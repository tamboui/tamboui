/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.pygments;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.style.Tags;
import dev.tamboui.text.Text;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Syntax-highlights source code by invoking the {@code pygmentize} CLI with the {@code raw} formatter.
 * It will first try `pygmentize`, then `uvx`, then `pipx` to find a working pygmentize command.
 * <p>
 * The raw formatter emits one token per line in the form:
 * {@code Token.Keyword\t'repr(token_text)'}.
 * This class parses that output and converts it to {@link Text} by applying {@link dev.tamboui.style.Tags}
 * on spans (e.g. {@code syntax-comment}, {@code syntax-string}, etc).
 * 
 */
public final class Pygments {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(2);
    private static final String DEFAULT_BIN = "pygmentize";
    private static final String UVX_BIN = "uvx";
    private static final String PIPX_BIN = "pipx";

    private static volatile Pygments CACHED;

    private volatile Invoker defaultInvoker;

    private Pygments() {
        // stateful utility
    }

    /**
     * Returns a cached {@link Pygments} instance.
     * <p>
     * The returned instance caches which invoker is usable (direct {@code pygmentize}, {@code uvx}, or {@code pipx})
     * the first time it needs to run Pygments.
     */
    public static Pygments pygments() {
        Pygments v = CACHED;
        if (v != null) {
            return v;
        }
        synchronized (Pygments.class) {
            if (CACHED == null) {
                CACHED = new Pygments();
            }
            return CACHED;
        }
    }

    public Text highlight(String filename, String source) {
        return highlight(filename, source, DEFAULT_TIMEOUT, DEFAULT_STYLE_RESOLVER);
    }

    public Text highlight(String filename, String source, TokenStyleResolver resolver) {
        return highlight(filename, source, DEFAULT_TIMEOUT, resolver);
    }

    public Text highlight(String filename, String source, Duration timeout, TokenStyleResolver resolver) {
        Result result = highlightWithInfo(filename, source, timeout, resolver);
        return result.text();
    }

    public Result highlightWithInfo(String filename, String source, Duration timeout) {
        return highlightWithInfo(filename, source, timeout, DEFAULT_STYLE_RESOLVER);
    }

    public Result highlightWithInfo(String filename, String source, Duration timeout, TokenStyleResolver resolver) {
        Objects.requireNonNull(resolver, "resolver");

        if (source == null || source.isEmpty()) {
            return new Result(Text.empty(), null, false, null);
        }
        if (filename == null || filename.trim().isEmpty()) {
            return new Result(Text.raw(source), null, false, "filename is required for lexer inference");
        }

        Invoker invoker = resolveInvoker();
        if (invoker == null) {
            return new Result(Text.raw(source), null, false, "pygmentize not available (tried pygmentize, uvx, pipx)");
        }

        String lexer;
        try {
            lexer = inferLexer(filename, invoker, timeout);
        } catch (IOException | InterruptedException e) {
            return new Result(Text.raw(source), null, false, e.getMessage());
        }

        if (lexer == null || lexer.isEmpty()) {
            return new Result(Text.raw(source), null, false, "no lexer inferred from filename");
        }

        String raw;
        try {
            raw = tokenizeToRaw(lexer, source, invoker, timeout);
        } catch (IOException | InterruptedException e) {
            return new Result(Text.raw(source), lexer, false, e.getMessage());
        }

        try {
            Text text = RawTokenParser.parse(raw, resolver);
            return new Result(text, lexer, true, null);
        } catch (RuntimeException e) {
            return new Result(Text.raw(source), lexer, false, "failed to parse pygmentize output: " + e.getMessage());
        }
    }

    public boolean isAvailable() {
        return resolveInvoker() != null;
    }

    private static String inferLexer(String filename, Invoker invoker, Duration timeout) throws IOException, InterruptedException {
        // First try extension mapping (fast, no process call)
        String lexer = inferLexerFromExtension(filename);
        if (lexer != null) {
            return lexer;
        }
        // Fallback to pygmentize inference (slower, but supports all file types)
        return inferLexerFromPygmentize(filename, invoker, timeout);
    }

    private static String inferLexerFromExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot >= filename.length() - 1) {
            return null;
        }
        String extension = filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
        return EXTENSION_TO_LEXER.get(extension);
    }

    private static String inferLexerFromPygmentize(String filename, Invoker invoker, Duration timeout) throws IOException, InterruptedException {
        // `pygmentize -N filename` prints a lexer name (or "text" / empty)
        ProcessResult pr = run(invoker.command("-N", filename), null, timeout, invoker.filterNotes());
        if (pr.exitCode != 0) {
            return null;
        }
        String out = pr.stdout.trim();
        if (out.isEmpty()) {
            return null;
        }
        // sometimes outputs like "text\n" or "java\n"
        String lexer = out.toLowerCase(Locale.ROOT);
        // Don't return "text" as it means no lexer was found
        return "text".equals(lexer) ? null : lexer;
    }

    private static final Map<String, String> EXTENSION_TO_LEXER = createExtensionMap();

    private static Map<String, String> createExtensionMap() {
        Map<String, String> map = new HashMap<>();
        // Common languages
        map.put("java", "java");
        map.put("py", "python");
        map.put("js", "javascript");
        map.put("jsx", "javascript");
        map.put("ts", "typescript");
        map.put("tsx", "typescript");
        map.put("rs", "rust");
        map.put("go", "go");
        map.put("c", "c");
        map.put("h", "c");
        map.put("cpp", "cpp");
        map.put("cc", "cpp");
        map.put("cxx", "cpp");
        map.put("hpp", "cpp");
        map.put("cs", "csharp");
        map.put("php", "php");
        map.put("rb", "ruby");
        map.put("swift", "swift");
        map.put("kt", "kotlin");
        map.put("scala", "scala");
        map.put("clj", "clojure");
        map.put("hs", "haskell");
        map.put("ml", "ocaml");
        map.put("fs", "fsharp");
        map.put("erl", "erlang");
        map.put("ex", "elixir");
        map.put("exs", "elixir");
        map.put("lua", "lua");
        map.put("pl", "perl");
        map.put("pm", "perl");
        map.put("r", "r");
        map.put("sh", "bash");
        map.put("bash", "bash");
        map.put("zsh", "bash");
        // Data formats
        map.put("json", "json");
        map.put("xml", "xml");
        map.put("html", "html");
        map.put("htm", "html");
        map.put("css", "css");
        map.put("yaml", "yaml");
        map.put("yml", "yaml");
        map.put("toml", "toml");
        map.put("ini", "ini");
        map.put("properties", "properties");
        map.put("sql", "sql");
        // Markup
        map.put("md", "markdown");
        map.put("markdown", "markdown");
        map.put("rst", "rst");
        map.put("tex", "latex");
        // Config/build files
        map.put("gradle", "groovy");
        map.put("groovy", "groovy");
        map.put("makefile", "makefile");
        map.put("mk", "makefile");
        map.put("cmake", "cmake");
        // Shell scripts
        map.put("ps1", "powershell");
        map.put("bat", "batch");
        map.put("cmd", "batch");
        return Collections.unmodifiableMap(map);
    }

    private static String tokenizeToRaw(String lexer, String source, Invoker invoker, Duration timeout) throws IOException, InterruptedException {
        List<String> cmd = invoker.command("-l", lexer, "-f", "raw");

        ProcessResult pr = run(cmd, source, timeout, invoker.filterNotes());
        if (pr.exitCode != 0) {
            throw new IOException("pygmentize failed: " + pr.stderr.trim());
        }
        return pr.stdout;
    }

    private static ProcessResult run(List<String> command, String stdin, Duration timeout, boolean filterNotes)
        throws IOException, InterruptedException {

        ProcessBuilder pb = new ProcessBuilder(command);
        Process p = pb.start();

        if (stdin != null) {
            try (OutputStream os = p.getOutputStream()) {
                os.write(stdin.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            p.getOutputStream().close();
        }

        boolean finished = p.waitFor(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new IOException("pygmentize timed out");
        }

        String out = readAll(p.getInputStream());
        String err = readAll(p.getErrorStream());
        if (filterNotes) {
            out = stripNoteLines(out);
            err = stripNoteLines(err);
        }
        return new ProcessResult(p.exitValue(), out, err);
    }

    private static String stripNoteLines(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        String[] lines = s.split("\n", -1);
        StringBuilder sb = new StringBuilder(s.length());
        for (String line : lines) {
            if (line.startsWith("NOTE:")) {
                continue;
            }
            sb.append(line).append('\n');
        }
        // Preserve the behavior of split(..., -1) which always leaves a trailing empty element if s ended with '\n'.
        // Since we always append '\n' above, remove one if the original didn't end with it.
        if (!s.endsWith("\n") && sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private Invoker resolveInvoker() {
        return resolveDefaultInvoker();
    }

    private Invoker resolveDefaultInvoker() {
        if(defaultInvoker != null) {
            return defaultInvoker;
        }
        synchronized (this) {
            Invoker detected = null;
            // Prefer a real pygmentize first.
            if (canRunDirect(DEFAULT_BIN, Duration.ofSeconds(2), "-V")) {
                detected = Invoker.direct(DEFAULT_BIN);
            } else if (canRunDirect(UVX_BIN, Duration.ofSeconds(2), "--version")) {
                // Fallback 1: uvx --from pygments pygmentize ...
                detected = Invoker.uvx();
            } else if (canRunDirect(PIPX_BIN, Duration.ofSeconds(2), "--version")) {
                // Fallback 2: pipx run --spec pygments pygmentize ...
                detected = Invoker.pipx();
            }
            defaultInvoker = detected;
            return detected;
        }
    }

    /**
     * can i run the command and get a success exit code?
     * @param bin the command to run
     * @param timeout the timeout
     * @param args the arguments to pass to the command (--version, -V, etc.) to ensure it returns a succcess
     * @return true if the command can be run and get a success exit code, false otherwise
     */
    private static boolean canRunDirect(String bin, Duration timeout,String... args) {
        try {
            List<String> cmd = new ArrayList<>(1 + args.length);
            cmd.add(bin);
            cmd.addAll(Arrays.asList(args));
            ProcessResult pr = run(cmd, null, timeout, false);
            if (pr.exitCode == 0) {
                return true;
            }
        } catch (Exception ignored) {
            // ignore
        }
        return false;
    }

    private static final class Invoker {
        private final List<String> prefix;
        private final boolean filterNotes;

        private Invoker(List<String> prefix, boolean filterNotes) {
            this.prefix = prefix;
            this.filterNotes = filterNotes;
        }

        static Invoker direct(String bin) {
            return new Invoker(Collections.singletonList(bin), false);
        }

        static Invoker uvx() {
            return new Invoker(Arrays.asList(UVX_BIN, "--from", "pygments", "pygmentize"), false);
        }

        static Invoker pipx() {
            // pipx may print "NOTE:" lines; strip them from stdout/stderr.
            return new Invoker(Arrays.asList(PIPX_BIN, "run", "--spec", "pygments", "pygmentize"), true);
        }

        List<String> command(String... args) {
            List<String> cmd = new ArrayList<>(prefix.size() + args.length);
            cmd.addAll(prefix);
            cmd.addAll(Arrays.asList(args));
            return cmd;
        }

        boolean filterNotes() {
            return filterNotes;
        }
    }

    private static String readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int read;
        while ((read = is.read(buf)) >= 0) {
            baos.write(buf, 0, read);
        }
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Optional mapping from token information to a style patch.
     */
    @FunctionalInterface
    public interface TokenStyleResolver {
        /**
         * @param tokenType full Pygments token type (e.g. {@code Token.Literal.String})
         * @return a style to use for the given token type
         */
        Style resolve(String tokenType);
    }

    public static final TokenStyleResolver DEFAULT_STYLE_RESOLVER = createDefaultStyleResolver();

    private static TokenStyleResolver createDefaultStyleResolver() {
        Map<String, Style> tokenStyles = new HashMap<>();
        // Token.Comment: "$text 60%"
        tokenStyles.put("Token.Comment", Style.EMPTY.gray().dim().withExtension(Tags.class, Tags.of("syntax-comment")));
        // Token.Error: "$text-error on $error-muted"
        tokenStyles.put("Token.Error", Style.EMPTY.red().bg(Color.rgb(0x2c, 0x1e, 0x1e)).withExtension(Tags.class, Tags.of("syntax-error"))); // $text-error on $error-muted
        // Token.Generic.Strong: "bold"
        tokenStyles.put("Token.Generic.Strong", Style.EMPTY.bold());
        // Token.Generic.Emph: "italic"
        tokenStyles.put("Token.Generic.Emph", Style.EMPTY.italic());
        // Token.Generic.Error: "$text-error on $error-muted"
        tokenStyles.put("Token.Generic.Error", Style.EMPTY.red().bg(Color.rgb(0x2c, 0x1e, 0x1e)).withExtension(Tags.class, Tags.of("syntax-error")));
        // Token.Generic.Heading: "$text-primary underline"
        tokenStyles.put("Token.Generic.Heading", Style.EMPTY.underlineColor(Color.WHITE).withExtension(Tags.class, Tags.of("syntax-heading")));
        // Token.Generic.Subheading: "$text-primary"
        tokenStyles.put("Token.Generic.Subheading", Style.EMPTY.fg(Color.WHITE).withExtension(Tags.class, Tags.of("syntax-subheading")));
        // Token.Keyword: "$text-accent"
        tokenStyles.put("Token.Keyword", Style.EMPTY.fg(Color.CYAN).withExtension(Tags.class, Tags.of("syntax-keyword")));
        // Token.Keyword.Constant: "bold $text-success 80%"
        tokenStyles.put("Token.Keyword.Constant", Style.EMPTY.bold().fg(Color.rgb(0xa9, 0xdc, 0x76)).withExtension(Tags.class, Tags.of("syntax-constant"))); // $text-success 80%
        // Token.Keyword.Namespace: "$text-error"
        tokenStyles.put("Token.Keyword.Namespace", Style.EMPTY.fg(Color.RED).withExtension(Tags.class, Tags.of("syntax-namespace")));
        // Token.Keyword.Type: "bold"
        tokenStyles.put("Token.Keyword.Type", Style.EMPTY.bold());
        // Token.Literal.Number: "$text-warning"
        tokenStyles.put("Token.Literal.Number", Style.EMPTY.fg(Color.YELLOW).withExtension(Tags.class, Tags.of("syntax-number")));
        // Token.Literal.String.Backtick: "$text 60%"
        tokenStyles.put("Token.Literal.String.Backtick", Style.EMPTY.fg(Color.GRAY).withExtension(Tags.class, Tags.of("syntax-string-backtick")));
        // Token.Literal.String: "$text-success 90%"
        tokenStyles.put("Token.Literal.String", Style.EMPTY.fg(Color.rgb(0x8d, 0xcf, 0x8c)).withExtension(Tags.class, Tags.of("syntax-string"))); // $text-success 90%
        // Token.Literal.String.Doc: "$text-success 80% italic"
        tokenStyles.put("Token.Literal.String.Doc", Style.EMPTY.fg(Color.rgb(0xa9, 0xdc, 0x76)).italic().withExtension(Tags.class, Tags.of("syntax-string-doc")));
        // Token.Literal.String.Double: "$text-success 90%"
        tokenStyles.put("Token.Literal.String.Double", Style.EMPTY.fg(Color.rgb(0x8d, 0xcf, 0x8c)).withExtension(Tags.class, Tags.of("syntax-string-double")));
        // Token.Name: "$text-primary"
        tokenStyles.put("Token.Name", Style.EMPTY.fg(Color.WHITE).withExtension(Tags.class, Tags.of("syntax-name")));
        // Token.Name.Attribute: "$text-warning"
        tokenStyles.put("Token.Name.Attribute", Style.EMPTY.fg(Color.YELLOW).withExtension(Tags.class, Tags.of("syntax-attribute")));
        // Token.Name.Builtin: "$text-accent"
        tokenStyles.put("Token.Name.Builtin", Style.EMPTY.fg(Color.CYAN).withExtension(Tags.class, Tags.of("syntax-builtin", "syntax-identifier")));
        // Token.Name.Builtin.Pseudo: "italic"
        tokenStyles.put("Token.Name.Builtin.Pseudo", Style.EMPTY.italic().withExtension(Tags.class, Tags.of("syntax-builtin-pseudo")));
        // Token.Name.Class: "$text-warning bold"
        tokenStyles.put("Token.Name.Class", Style.EMPTY.fg(Color.YELLOW).bold().withExtension(Tags.class, Tags.of("syntax-class")));
        // Token.Name.Constant: "$text-error"
        tokenStyles.put("Token.Name.Constant", Style.EMPTY.fg(Color.RED).withExtension(Tags.class, Tags.of("syntax-constant")));    
        // Token.Name.Decorator: "$text-primary bold"
        tokenStyles.put("Token.Name.Decorator", Style.EMPTY.fg(Color.WHITE).bold().withExtension(Tags.class, Tags.of("syntax-decorator")));
        // Token.Name.Function: "$text-warning underline"
        tokenStyles.put("Token.Name.Function", Style.EMPTY.fg(Color.YELLOW).underlineColor(Color.YELLOW).withExtension(Tags.class, Tags.of("syntax-function")));
        // Token.Name.Function.Magic: "$text-warning underline"
        tokenStyles.put("Token.Name.Function.Magic", Style.EMPTY.fg(Color.YELLOW).underlineColor(Color.YELLOW).withExtension(Tags.class, Tags.of("syntax-function-magic")));
        // Token.Name.Tag: "$text-primary bold"
        tokenStyles.put("Token.Name.Tag", Style.EMPTY.fg(Color.WHITE).bold().withExtension(Tags.class, Tags.of("syntax-tag")));
        // Token.Name.Variable: "$text-secondary"
        tokenStyles.put("Token.Name.Variable", Style.EMPTY.fg(Color.GRAY).withExtension(Tags.class, Tags.of("syntax-variable")));
        // Token.Name.Namespace
        tokenStyles.put("Token.Name.Namespace", Style.EMPTY.fg(Color.RED).withExtension(Tags.class, Tags.of("syntax-namespace")));
        // Token.Number: "$text-warning"
        tokenStyles.put("Token.Number", Style.EMPTY.fg(Color.YELLOW).withExtension(Tags.class, Tags.of("syntax-number")));
        // Token.Operator: "bold"
        tokenStyles.put("Token.Operator", Style.EMPTY.bold().withExtension(Tags.class, Tags.of("syntax-operator")));
        // Token.Operator.Word: "bold $text-error"
        tokenStyles.put("Token.Operator.Word", Style.EMPTY.bold().fg(Color.RED).withExtension(Tags.class, Tags.of("syntax-operator-word")));
        // Token.String: "$text-success"
        tokenStyles.put("Token.String", Style.EMPTY.fg(Color.GREEN).withExtension(Tags.class, Tags.of("syntax-string")));
        // Token.Whitespace: ""
        tokenStyles.put("Token.Text.Whitespace", Style.EMPTY.withExtension(Tags.class, Tags.of("syntax-whitespace")));
        // Token.Whitespace: ""
        tokenStyles.put("Token.Text.Punctuation", Style.EMPTY.withExtension(Tags.class, Tags.of("syntax-punctuation")));
        
        Map<String, Style> unmodifiableMap = Collections.unmodifiableMap(tokenStyles);
        return k -> {
            Style style = unmodifiableMap.get(k);
            return style != null ? style : Style.EMPTY;
        };
    }

    /**
     * Highlight result with extra info for UIs.
     */
    public static final class Result {
        private final Text text;
        private final String lexer;
        private final boolean highlighted;
        private final String message;

        private Result(Text text, String lexer, boolean highlighted, String message) {
            this.text = Objects.requireNonNull(text, "text");
            this.lexer = lexer;
            this.highlighted = highlighted;
            this.message = message;
        }

        public Text text() {
            return text;
        }

        public Optional<String> lexer() {
            return Optional.ofNullable(lexer);
        }

        public boolean highlighted() {
            return highlighted;
        }

        public Optional<String> message() {
            return Optional.ofNullable(message);
        }
    }

    private static final class ProcessResult {
        final int exitCode;
        final String stdout;
        final String stderr;

        ProcessResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout != null ? stdout : "";
            this.stderr = stderr != null ? stderr : "";
        }
    }
}


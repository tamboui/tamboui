/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;

/**
 * The built-in {@link SyntaxHighlighter}. It tokenizes a snippet using an
 * ordered set of regex {@link Grammar}s (inspired by highlight.js) and applies
 * the configured {@link SyntaxTheme} to turn each {@link TokenType} into a
 * {@link Style}. Every source line becomes a single {@link Line} holding one
 * {@link Span} per token, so widths, clipping and wrapping keep working
 * unchanged.
 *
 * <p>Use {@link #defaults()} for an instance covering the popular built-in
 * languages, or {@code builder()} to start from those and add/mark languages
 * missing from the defaults.
 *
 * <p>Multi-line constructs (block comments, triple-quoted strings) are handled
 * by the grammar's {@link Grammar.Rule#multiline} rules and are emitted as a
 * single token spanning the newlines that separate them, exactly like a
 * highlight.js {@code beginsWith}/{@code endsWith} pair.
 */
public final class RegexSyntaxHighlighter implements SyntaxHighlighter {

    private final Map<String, Grammar> byId;
    private final Map<String, Grammar> byAlias;

    private RegexSyntaxHighlighter(List<Grammar> grammars) {
        Map<String, Grammar> ids = new HashMap<>();
        Map<String, Grammar> aliases = new HashMap<>();
        for (Grammar g : grammars) {
            ids.put(g.id().toLowerCase(), g);
            for (String alias : g.aliases()) {
                aliases.put(alias.toLowerCase(), g);
            }
        }
        this.byId = ids;
        this.byAlias = aliases;
    }

    /**
     * Returns the highlighter covering the built-in languages: Java, Kotlin,
     * JavaScript, TypeScript, Python, JSON, XML/HTML, CSS, Bash/Shell, YAML,
     * SQL, Go and Rust, each with its common aliases.
     *
     * @return the default highlighter
     */
    public static RegexSyntaxHighlighter defaults() {
        return new Builder(false).withDefaults().build();
    }

    /**
     * Creates a new builder seeded with the built-in grammars.
     *
     * @return a builder populated with the default grammars
     */
    public static Builder builder() {
        return new Builder(true);
    }

    /**
     * Creates a highlighter containing only the supplied grammars.
     *
     * @param grammars the grammars to include
     * @return a new highlighter
     */
    public static RegexSyntaxHighlighter of(Grammar... grammars) {
        if (grammars == null || grammars.length == 0) {
            return new RegexSyntaxHighlighter(Collections.emptyList());
        }
        List<Grammar> list = new ArrayList<>(grammars.length);
        Collections.addAll(list, grammars);
        return new RegexSyntaxHighlighter(list);
    }

    /**
     * Returns the grammar for a language id or alias, or {@code null} if the
     * language is not recognized.
     *
     * @param language the language identifier or alias
     * @return the matched grammar, or {@code null}
     */
    public Grammar grammar(String language) {
        if (language == null || language.isEmpty()) {
            return null;
        }
        String key = language.toLowerCase().trim();
        Grammar g = byId.get(key);
        if (g == null) {
            g = byAlias.get(key);
        }
        return g;
    }

    @Override
    public List<Line> highlight(String code, String language, Style base, SyntaxTheme theme) {
        Grammar grammar = grammar(language);
        if (grammar == null) {
            return plain(code, base);
        }
        String trimmed = code.endsWith("\n") ? code.substring(0, code.length() - 1) : code;
        if (trimmed.isEmpty()) {
            return Collections.singletonList(Line.from(Span.styled("", base)));
        }
        List<Token> tokens = tokenize(grammar, trimmed);
        return toLines(tokens, base, theme);
    }

    private static List<Line> plain(String code, Style base) {
        String trimmed = code.endsWith("\n") ? code.substring(0, code.length() - 1) : code;
        if (trimmed.isEmpty()) {
            return Collections.singletonList(Line.from(Span.styled("", base)));
        }
        String[] lines = trimmed.split("\n", -1);
        List<Line> out = new ArrayList<>(lines.length);
        for (String line : lines) {
            out.add(Line.from(Span.styled(line, base)));
        }
        return out;
    }

    private static List<Token> tokenize(Grammar grammar, String code) {
        int length = code.length();
        List<Token> out = new ArrayList<>();
        List<Grammar.Rule> rules = grammar.rules();
        Matcher[] matchers = new Matcher[rules.size()];
        for (int r = 0; r < rules.size(); r++) {
            Grammar.Rule rule = rules.get(r);
            if (!rule.multiline()) {
                matchers[r] = rule.pattern().matcher(code);
            }
        }
        int i = 0;
        while (i < length) {
            boolean matched = false;
            for (int r = 0; r < rules.size(); r++) {
                Grammar.Rule rule = rules.get(r);
                if (rule.multiline()) {
                    if (code.startsWith(rule.open(), i)) {
                        int closeIdx = code.indexOf(rule.close(), i + rule.open().length());
                        int end = closeIdx < 0 ? length : closeIdx + rule.close().length();
                        out.add(new Token(code.substring(i, end), rule.type()));
                        i = end;
                        matched = true;
                        break;
                    }
                } else {
                    Matcher m = matchers[r];
                    m.region(i, length);
                    if (m.lookingAt()) {
                        out.add(new Token(m.group(), rule.type()));
                        i = m.end();
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) {
                // Accumulate a run of characters matched by no rule as a single
                // PLAIN token so it keeps the code-block base style.
                int start = i;
                while (i < length) {
                    boolean any = false;
                    for (int r = 0; r < rules.size(); r++) {
                        Grammar.Rule rule = rules.get(r);
                        if (rule.multiline()) {
                            if (code.startsWith(rule.open(), i)) {
                                any = true;
                                break;
                            }
                        } else {
                            Matcher m = matchers[r];
                            m.region(i, length);
                            if (m.lookingAt()) {
                                any = true;
                                break;
                            }
                        }
                    }
                    if (any) {
                        break;
                    }
                    i += Character.charCount(code.codePointAt(i));
                }
                out.add(new Token(code.substring(start, i), TokenType.PLAIN));
            }
        }
        return out;
    }

    private static List<Line> toLines(List<Token> tokens, Style base, SyntaxTheme theme) {
        List<Line> lines = new ArrayList<>();
        List<Span> current = new ArrayList<>();
        for (Token token : tokens) {
            String[] parts = token.text.split("\n", -1);
            for (int p = 0; p < parts.length; p++) {
                if (!parts[p].isEmpty()) {
                    Style style = theme.style(token.type, base);
                    current.add(Span.styled(parts[p], style));
                }
                if (p < parts.length - 1) {
                    lines.add(Line.from(current));
                    current = new ArrayList<>();
                }
            }
        }
        if (!current.isEmpty() || lines.isEmpty()) {
            lines.add(Line.from(current));
        }
        return lines;
    }

    private static final class Token {
        final String text;
        final TokenType type;

        Token(String text, TokenType type) {
            this.text = text;
            this.type = type;
        }
    }

    /** Builder for {@link RegexSyntaxHighlighter}. */
    public static final class Builder {

        private final List<Grammar> grammars = new ArrayList<>();

        private Builder(boolean seedDefaults) {
            if (seedDefaults) {
                withDefaults();
            }
        }

        /**
         * Adds a grammar to the highlighter.
         *
         * @param grammar the grammar to add
         * @return this builder
         */
        public Builder add(Grammar grammar) {
            grammars.add(grammar);
            return this;
        }

        private Builder withDefaults() {
            add(javaGrammar());
            add(kotlinGrammar());
            add(javascriptGrammar());
            add(typescriptGrammar());
            add(pythonGrammar());
            add(jsonGrammar());
            add(xmlGrammar());
            add(cssGrammar());
            add(bashGrammar());
            add(yamlGrammar());
            add(sqlGrammar());
            add(goGrammar());
            add(rustGrammar());
            return this;
        }

        /**
         * Builds the {@link RegexSyntaxHighlighter}.
         *
         * @return a new highlighter
         */
        public RegexSyntaxHighlighter build() {
            return new RegexSyntaxHighlighter(grammars);
        }
    }

    // Helpers shared by the built-in grammars.

    private static Grammar.Rule words(String csv) {
        String[] parts = csv.trim().split("\\s+");
        return Grammar.Rule.pattern(TokenType.KEYWORD,
            Pattern.compile("\\b(?:" + String.join("|", parts) + ")\\b"));
    }

    private static final String NUMBERS =
        "\\b(?:0[xX][0-9a-fA-F_]+|0[bB][01_]+|\\d[\\d_]*(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)\\b";

    private static Grammar.Rule lineComment(final String prefix) {
        return Grammar.Rule.pattern(TokenType.COMMENT,
            Pattern.compile(Pattern.quote(prefix) + "[^\\n]*"));
    }

    private static Grammar.Rule blockComment(final String open, final String close) {
        return Grammar.Rule.multiline(TokenType.COMMENT, open, close);
    }

    private static Grammar.Rule doubleString() {
        return Grammar.Rule.pattern(TokenType.STRING, Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\""));
    }

    private static Grammar.Rule singleString() {
        return Grammar.Rule.pattern(TokenType.STRING, Pattern.compile("'(?:\\\\.|[^'\\\\])*'"));
    }

    private static Grammar.Rule interpString() {
        return Grammar.Rule.pattern(TokenType.STRING, Pattern.compile("`(?:\\\\.|[^`\\\\])*`"));
    }

    private static Grammar.Rule multilineString(final String open, final String close) {
        return Grammar.Rule.multiline(TokenType.STRING, open, close);
    }

    private static void operators(Grammar.Builder b) {
        b.rule(Grammar.Rule.pattern(TokenType.OPERATOR, Pattern.compile(
            "&&|\\|\\||\\+\\+|--|==|!=|<=|>=|->|::|\\+=|-=|\\*=|/=|%=|&=|\\|=|\\^="
                + "|<<=|>>=|>>>=|\\?|\\.\\.|=>|[-+*/%&|^~!<>=?.:]")));
        b.rule(Grammar.Rule.pattern(TokenType.PUNCTUATION, Pattern.compile("[(){}\\[\\];,]")));
    }

    // ---- Java ----

    private static Grammar javaGrammar() {
        Grammar.Builder b = Grammar.builder("java");
        b.rule(blockComment("/*", "*/"));
        b.rule(lineComment("//"));
        b.rule(multilineString("\"\"\"", "\"\"\""));
        b.rule(doubleString());
        b.rule(singleString());
        b.rule(Grammar.Rule.pattern(TokenType.ANNOTATION,
            Pattern.compile("@[A-Za-z_][A-Za-z0-9_.]*")));
        b.rule(Grammar.Rule.pattern(TokenType.NUMBER, Pattern.compile(NUMBERS)));
        b.rule(Grammar.Rule.pattern(TokenType.CONSTANT, Pattern.compile("\\b[A-Z][A-Z0-9_]*\\b")));
        b.rule(Grammar.Rule.pattern(TokenType.TYPE, Pattern.compile("\\b[A-Z][a-zA-Z0-9_]*\\b")));
        b.rule(words("abstract assert boolean break byte case catch char class const continue default "
            + "do double else enum extends final finally float for goto if implements import instanceof "
            + "int interface long native new package private protected public return short static "
            + "strictfp super switch synchronized this throw throws transient try void volatile while "
            + "record sealed permits var yield true false null"));
        b.rule(Grammar.Rule.pattern(TokenType.FUNCTION, Pattern.compile("[A-Za-z_$][\\w$]*\\s*\\(")));
        operators(b);
        return b.build();
    }

    // ---- Kotlin ----

    private static Grammar kotlinGrammar() {
        Grammar.Builder b = Grammar.builder("kotlin").alias("kt");
        b.rule(blockComment("/*", "*/"));
        b.rule(lineComment("//"));
        b.rule(multilineString("\"\"\"", "\"\"\""));
        b.rule(doubleString());
        b.rule(singleString());
        b.rule(Grammar.Rule.pattern(TokenType.ANNOTATION,
            Pattern.compile("@[A-Za-z_][A-Za-z0-9_.]*")));
        b.rule(Grammar.Rule.pattern(TokenType.NUMBER, Pattern.compile(NUMBERS)));
        b.rule(Grammar.Rule.pattern(TokenType.CONSTANT, Pattern.compile("\\b[A-Z][A-Z0-9_]*\\b")));
        b.rule(Grammar.Rule.pattern(TokenType.TYPE, Pattern.compile("\\b[A-Z][a-zA-Z0-9_]*\\b")));
        b.rule(words("abstract actual annotation as by catch class companion const constructor "
            + "crossinline data delegate do dynamic enum expect external final fun get import in "
            + "infix init inline inner interface internal is lateinit noinline object open operator "
            + "out override package private protected public reified return sealed set super suspend "
            + "tailrec this throw try typealias val value var vararg when where while true false null"));
        b.rule(Grammar.Rule.pattern(TokenType.FUNCTION, Pattern.compile("[A-Za-z_$][\\w$]*\\s*\\(")));
        operators(b);
        return b.build();
    }

    // ---- JavaScript ----

    private static Grammar javascriptGrammar() {
        Grammar.Builder b = Grammar.builder("javascript").alias("js", "jsx", "node", "mjs", "cjs");
        b.rule(blockComment("/*", "*/"));
        b.rule(lineComment("//"));
        b.rule(interpString());
        b.rule(doubleString());
        b.rule(singleString());
        b.rule(blockComment("<!--", "-->"));
        b.rule(Grammar.Rule.pattern(TokenType.NUMBER, Pattern.compile(NUMBERS)));
        b.rule(Grammar.Rule.pattern(TokenType.CONSTANT, Pattern.compile("\\b[A-Z][A-Z0-9_]*\\b")));
        b.rule(words("break case catch class const continue debugger default delete do else export "
            + "extends finally for function if import in instanceof let new of return static super "
            + "switch this throw try typeof var void while with async await yield true false null "
            + "undefined"));
        b.rule(Grammar.Rule.pattern(TokenType.FUNCTION, Pattern.compile("[A-Za-z_$][\\w$]*\\s*\\(")));
        operators(b);
        return b.build();
    }

    // ---- TypeScript ----

    private static Grammar typescriptGrammar() {
        Grammar.Builder b = Grammar.builder("typescript").alias("ts", "tsx");
        Grammar js = javascriptGrammar();
        for (Grammar.Rule rule : js.rules()) {
            b.rule(rule);
        }
        b.rule(words("interface type namespace declare abstract implements readonly as enum"));
        return b.build();
    }

    // ---- Python ----

    private static Grammar pythonGrammar() {
        Grammar.Builder b = Grammar.builder("python").alias("py", "py3");
        b.rule(lineComment("#"));
        b.rule(multilineString("\"\"\"", "\"\"\""));
        b.rule(multilineString("'''", "'''"));
        b.rule(doubleString());
        b.rule(singleString());
        b.rule(Grammar.Rule.pattern(TokenType.NUMBER, Pattern.compile(NUMBERS)));
        b.rule(Grammar.Rule.pattern(TokenType.CONSTANT, Pattern.compile("\\b[A-Z][A-Z0-9_]*\\b")));
        b.rule(words("and as assert async await break class continue def del elif else except "
            + "finally for from global if import in is lambda nonlocal not or pass raise return "
            + "try while with yield True False None self"));
        b.rule(Grammar.Rule.pattern(TokenType.ANNOTATION,
            Pattern.compile("@[A-Za-z_][A-Za-z0-9_.]*")));
        b.rule(Grammar.Rule.pattern(TokenType.FUNCTION,
            Pattern.compile("[A-Za-z_][\\w]*\\s*\\(")));
        b.rule(Grammar.Rule.pattern(TokenType.OPERATOR,
            Pattern.compile("==|!=|<=|>=|->|\\+=|-=|=|[-+*/%&|^~<>:]")));
        b.rule(Grammar.Rule.pattern(TokenType.PUNCTUATION, Pattern.compile("[(){}\\[\\];,]")));
        return b.build();
    }

    // ---- JSON ----

    private static Grammar jsonGrammar() {
        Grammar.Builder b = Grammar.builder("json").alias("jsonc");
        b.rule(blockComment("/*", "*/"));
        b.rule(lineComment("//"));
        b.rule(doubleString());
        b.rule(Grammar.Rule.pattern(TokenType.NUMBER, Pattern.compile(NUMBERS)));
        b.rule(words("true false null"));
        b.rule(Grammar.Rule.pattern(TokenType.OPERATOR, Pattern.compile("[:,]")));
        b.rule(Grammar.Rule.pattern(TokenType.PUNCTUATION, Pattern.compile("[{}\\[\\]]")));
        return b.build();
    }

    // ---- XML / HTML ----

    private static Grammar xmlGrammar() {
        Grammar.Builder b = Grammar.builder("xml").alias("html", "htm", "svg", "xhtml");
        b.rule(blockComment("<!--", "-->"));
        b.rule(Grammar.Rule.pattern(TokenType.PUNCTUATION, Pattern.compile("[<>/]")));
        b.rule(Grammar.Rule.pattern(TokenType.TAG,
            Pattern.compile("[A-Za-z_][-A-Za-z0-9_.:]*")));
        b.rule(Grammar.Rule.pattern(TokenType.ATTRIBUTE,
            Pattern.compile("\\b[A-Za-z_:][-A-Za-z0-9_.:]*\\s*=")));
        b.rule(doubleString());
        b.rule(singleString());
        b.rule(Grammar.Rule.pattern(TokenType.PUNCTUATION, Pattern.compile("[={}]")));
        return b.build();
    }

    // ---- CSS ----

    private static Grammar cssGrammar() {
        Grammar.Builder b = Grammar.builder("css");
        b.rule(blockComment("/*", "*/"));
        b.rule(doubleString());
        b.rule(singleString());
        b.rule(Grammar.Rule.pattern(TokenType.NUMBER, Pattern.compile(NUMBERS)));
        b.rule(Grammar.Rule.pattern(TokenType.ATTRIBUTE,
            Pattern.compile("([-#]?[A-Za-z_][-A-Za-z0-9_]*)\\s*:")));
        b.rule(Grammar.Rule.pattern(TokenType.TYPE,
            Pattern.compile("#[A-Za-z_][-A-Za-z0-9_]*")));
        b.rule(words("and or not import media supports layer container at charset namespace "
            + "font face keyframes property page counter"));
        b.rule(Grammar.Rule.pattern(TokenType.PUNCTUATION, Pattern.compile("[{};:()]")));
        b.rule(Grammar.Rule.pattern(TokenType.OPERATOR, Pattern.compile("[,>+~*/=]")));
        return b.build();
    }

    // ---- Bash / Shell ----

    private static Grammar bashGrammar() {
        Grammar.Builder b = Grammar.builder("bash").alias("sh", "shell", "zsh");
        b.rule(lineComment("#"));
        b.rule(doubleString());
        b.rule(singleString());
        b.rule(interpString());
        b.rule(Grammar.Rule.pattern(TokenType.NUMBER, Pattern.compile(NUMBERS)));
        b.rule(words("if then else elif fi for while until do done case esac function select in "
            + "time coproc function break continue return exit set unset export local readonly "
            + "declare alias source echo eval exec trap wait true false null"));
        b.rule(Grammar.Rule.pattern(TokenType.PUNCTUATION, Pattern.compile("[{}();]")));
        b.rule(Grammar.Rule.pattern(TokenType.OPERATOR, Pattern.compile("[|&<>!=\\?$\\\\]")));
        return b.build();
    }

    // ---- YAML ----

    private static Grammar yamlGrammar() {
        Grammar.Builder b = Grammar.builder("yaml").alias("yml");
        b.rule(lineComment("#"));
        b.rule(doubleString());
        b.rule(singleString());
        b.rule(Grammar.Rule.pattern(TokenType.NUMBER, Pattern.compile(NUMBERS)));
        b.rule(words("true false null yes no on off NULL TRUE FALSE"));
        b.rule(Grammar.Rule.pattern(TokenType.KEYWORD,
            Pattern.compile("^\\s*[A-Za-z_][-A-Za-z0-9_.]*\\s*:", Pattern.MULTILINE)));
        b.rule(Grammar.Rule.pattern(TokenType.OPERATOR, Pattern.compile(":|[-]")));
        b.rule(Grammar.Rule.pattern(TokenType.PUNCTUATION, Pattern.compile("[\\-\\[\\]{},]")));
        return b.build();
    }

    // ---- SQL ----

    private static Grammar sqlGrammar() {
        Grammar.Builder b = Grammar.builder("sql");
        b.rule(lineComment("--"));
        b.rule(blockComment("/*", "*/"));
        b.rule(doubleString());
        b.rule(singleString());
        b.rule(Grammar.Rule.pattern(TokenType.NUMBER, Pattern.compile(NUMBERS)));
        b.rule(words("select insert update delete create drop alter table index view trigger "
            + "function procedure from where group by order having join inner left right outer "
            + "on as and or not null primary key foreign reference unique default check constraint "
            + "values set into distinct count sum avg min max between like in exists union all "
            + "case when then else end is asc desc limit offset returning with window partition "
            + "over true false"));
        b.rule(Grammar.Rule.pattern(TokenType.FUNCTION,
            Pattern.compile("[A-Za-z_][\\w]*\\s*\\(")));
        b.rule(Grammar.Rule.pattern(TokenType.OPERATOR, Pattern.compile("[-+*/%<>=!~?&|^,]")));
        b.rule(Grammar.Rule.pattern(TokenType.PUNCTUATION, Pattern.compile("[().]")));
        return b.build();
    }

    // ---- Go ----

    private static Grammar goGrammar() {
        Grammar.Builder b = Grammar.builder("go").alias("golang");
        b.rule(blockComment("/*", "*/"));
        b.rule(lineComment("//"));
        b.rule(interpString());
        b.rule(doubleString());
        b.rule(Grammar.Rule.pattern(TokenType.NUMBER, Pattern.compile(NUMBERS)));
        b.rule(Grammar.Rule.pattern(TokenType.CONSTANT, Pattern.compile("\\b[A-Z][A-Z0-9_]*\\b")));
        b.rule(words("break default func interface select case defer go map struct chan else goto "
            + "package switch const fallthrough if range type continue for import return var true "
            + "false iota nil go"));
        b.rule(Grammar.Rule.pattern(TokenType.TYPE, Pattern.compile("\\b[A-Z][a-zA-Z0-9_]*\\b")));
        b.rule(Grammar.Rule.pattern(TokenType.FUNCTION, Pattern.compile("[A-Za-z_][\\w]*\\s*\\(")));
        operators(b);
        return b.build();
    }

    // ---- Rust ----

    private static Grammar rustGrammar() {
        Grammar.Builder b = Grammar.builder("rust").alias("rs");
        b.rule(lineComment("//"));
        b.rule(blockComment("/*", "*/"));
        b.rule(doubleString());
        // Raw strings r"..." and byte strings b"..."
        b.rule(Grammar.Rule.pattern(TokenType.STRING,
            Pattern.compile("[br]*\"")));
        b.rule(Grammar.Rule.pattern(TokenType.NUMBER, Pattern.compile(NUMBERS)));
        b.rule(Grammar.Rule.pattern(TokenType.ANNOTATION,
            Pattern.compile("#!?\\[[a-z_]+")));
        b.rule(words("as break const continue crate dyn else enum extern fn for if impl in let loop "
            + "match mod move mut pub ref return self Self static struct super trait type unsafe use "
            + "where while async await true false"));
        b.rule(Grammar.Rule.pattern(TokenType.FUNCTION, Pattern.compile("[a-z_][\\w]*\\s*\\(")));
        operators(b);
        return b.build();
    }
}
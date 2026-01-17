/// usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17+

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Converts JavaDoc block comments to `///` Markdown line comments.
///
/// It also converts common lowercase HTML tags in documentation to Markdown equivalents,
/// preserving Java type parameters like {@code <T>} and {@code <B>}.
///
/// Usage:
///
/// - Dry-run (default): `jbang .jbang/ConvertJavadocs.java`
/// - Write changes: `jbang .jbang/ConvertJavadocs.java --write`
/// - Check mode (exit non-zero if changes are needed): `jbang .jbang/ConvertJavadocs.java --check`
/// - Limit paths: `jbang .jbang/ConvertJavadocs.java --write path/to/module`
public final class ConvertJavadocs {
    private static final Set<String> DEFAULT_EXCLUDED_DIRS =
            new HashSet<>(Arrays.asList("build", ".gradle", "out"));
    private static final Pattern HEADING_PATTERN =
            Pattern.compile("^<h([1-6])>(.*?)</h\\1>$");
    private static final Pattern PRE_START_PATTERN =
            Pattern.compile("^<pre>\\s*(\\{@code)?\\s*$");
    private static final Pattern LI_PATTERN =
            Pattern.compile("<li>(.*?)</li>");
    private static final Pattern BR_PATTERN =
            Pattern.compile("<br\\s*/?>");
    private static final Pattern P_OPEN_PATTERN =
            Pattern.compile("<p>");
    private static final Pattern P_CLOSE_PATTERN =
            Pattern.compile("</p>");
    private static final Pattern UL_OPEN_PATTERN =
            Pattern.compile("<ul>");
    private static final Pattern UL_CLOSE_PATTERN =
            Pattern.compile("</ul>");
    private static final Pattern OL_OPEN_PATTERN =
            Pattern.compile("<ol>");
    private static final Pattern OL_CLOSE_PATTERN =
            Pattern.compile("</ol>");

    /// Javadoc treats {@code ///} comments as Markdown. A list item like {@code - [c] - Foo} is parsed
    /// as a reference-style link and fails with {@code "reference not found"} unless a reference
    /// definition is present. We rewrite these "key binding" patterns to safe Markdown.
    private static final Pattern KEY_BINDING_LIST_ITEM_PATTERN =
            Pattern.compile("^(\\s*(?:[-*+]|\\d+\\.)\\s+)\\[([^\\s\\]]{1,16})](?=\\s*(?:[-–—:]|$))(.*)$");

    private static final String USAGE = String.join(
            "\n",
            "Usage: jbang .jbang/ConvertJavadocs.java [options] [paths...]",
            "",
            "Options:",
            "  --write              Apply changes to files",
            "  --check              Check only; exit non-zero if changes are needed (default)",
            "  --verbose            Print modified file paths",
            "  --exclude-dir <dir>  Skip directories by name (repeatable)",
            "  -h, --help           Show this help"
    );

    public static void main(String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        if (arguments.help) {
            System.out.println(USAGE);
            return;
        }

        List<Path> roots = arguments.paths.isEmpty()
                ? Collections.singletonList(Paths.get("."))
                : arguments.paths;
        Set<String> excludedDirs = new HashSet<>(DEFAULT_EXCLUDED_DIRS);
        excludedDirs.addAll(arguments.excludedDirs);

        List<Path> javaFiles = new ArrayList<>();
        for (Path root : roots) {
            collectJavaFiles(root, excludedDirs, javaFiles);
        }

        int changedFiles = 0;
        for (Path file : javaFiles) {
            ConversionResult result = convertFile(file);
            if (result.changed) {
                changedFiles++;
                if (arguments.verbose) {
                    System.out.println(result.path);
                }
                if (arguments.write) {
                    Files.write(result.path, result.content.getBytes(StandardCharsets.UTF_8));
                }
            }
        }

        System.out.printf(
                Locale.ROOT,
                "Scanned %d file(s), %d changed.%n",
                javaFiles.size(),
                changedFiles
        );

        if (!arguments.write && changedFiles > 0) {
            System.exit(1);
        }
    }

    private static void collectJavaFiles(Path root, Set<String> excludedDirs, List<Path> result)
            throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        if (Files.isRegularFile(root)) {
            if (root.toString().endsWith(".java") && !isExcluded(root, excludedDirs)) {
                result.add(root);
            }
            return;
        }
        Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE,
                new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (isExcluded(dir, excludedDirs)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (file.toString().endsWith(".java") && !isExcluded(file, excludedDirs)) {
                            result.add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
    }

    private static boolean isExcluded(Path path, Set<String> excludedDirs) {
        for (Path part : path.normalize()) {
            if (excludedDirs.contains(part.toString())) {
                return true;
            }
        }
        return false;
    }

    private static ConversionResult convertFile(Path file) throws IOException {
        String input = Files.readString(file, StandardCharsets.UTF_8);
        boolean endsWithNewline = input.endsWith("\n");
        List<String> lines = Arrays.asList(input.split("\n", -1));

        List<String> output = new ArrayList<>();
        List<String> docBlock = new ArrayList<>();
        boolean inDocBlock = false;
        String docIndent = "";

        int index = 0;
        while (index < lines.size()) {
            String line = lines.get(index);

            if (!inDocBlock && isDocBlockStart(line)) {
                inDocBlock = true;
                docBlock.clear();
                docIndent = leadingWhitespace(line);
                docBlock.add(line);
                if (line.contains("*/")) {
                    output.addAll(convertDocBlock(docBlock, docIndent));
                    inDocBlock = false;
                }
                index++;
                continue;
            }

            if (inDocBlock) {
                docBlock.add(line);
                if (line.contains("*/")) {
                    output.addAll(convertDocBlock(docBlock, docIndent));
                    inDocBlock = false;
                }
                index++;
                continue;
            }

            if (isLineDocStart(line)) {
                List<String> lineDocBlock = new ArrayList<>();
                String indent = leadingWhitespace(line);
                int start = index;
                while (start < lines.size() && isLineDocStart(lines.get(start))) {
                    lineDocBlock.add(stripLineDocPrefix(lines.get(start)));
                    start++;
                }
                boolean addSpaceAfterPrefix = detectLineDocPrefixSpace(lines, index, start);
                output.addAll(convertDocLines(lineDocBlock, indent, addSpaceAfterPrefix));
                index = start;
                continue;
            }

            output.add(line);
            index++;
        }

        String result = String.join("\n", output);
        if (endsWithNewline) {
            result = result + "\n";
        }
        return new ConversionResult(file, !result.equals(input), result);
    }

    private static boolean isDocBlockStart(String line) {
        int idx = line.indexOf("/**");
        return idx >= 0;
    }

    private static boolean isLineDocStart(String line) {
        String trimmed = line.stripLeading();
        return trimmed.startsWith("///");
    }

    /**
     * Detects whether an existing {@code ///} doc block uses {@code "/// "} (with a space)
     * or {@code "///"} (no space) as the prefix for content lines.
     */
    private static boolean detectLineDocPrefixSpace(List<String> lines, int startInclusive, int endExclusive) {
        for (int i = startInclusive; i < endExclusive; i++) {
            String trimmed = lines.get(i).stripLeading();
            if (!trimmed.startsWith("///")) {
                continue;
            }
            if (trimmed.equals("///")) {
                continue;
            }
            return trimmed.length() > 3 && trimmed.charAt(3) == ' ';
        }
        // If we can't infer, default to the canonical style with a space.
        return true;
    }

    private static String stripLineDocPrefix(String line) {
        String trimmed = line.stripLeading();
        String content = trimmed.substring(3);
        if (content.startsWith(" ")) {
            content = content.substring(1);
        }
        return content;
    }

    private static String leadingWhitespace(String line) {
        int index = 0;
        while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
            index++;
        }
        return line.substring(0, index);
    }

    private static List<String> convertDocBlock(List<String> blockLines, String indent) {
        String blockText = String.join("\n", blockLines);
        int start = blockText.indexOf("/**");
        int end = blockText.lastIndexOf("*/");
        if (start < 0 || end < 0 || end < start) {
            return blockLines;
        }
        String inner = blockText.substring(start + 3, end);
        List<String> rawLines = Arrays.asList(inner.split("\n", -1));
        List<String> cleaned = new ArrayList<>();
        for (String line : rawLines) {
            String trimmed = line.stripLeading();
            if (trimmed.startsWith("*")) {
                trimmed = trimmed.substring(1);
                if (trimmed.startsWith(" ")) {
                    trimmed = trimmed.substring(1);
                }
            }
            cleaned.add(trimmed);
        }
        return convertDocLines(cleaned, indent, true);
    }

    private static String sanitizeForJavadocMarkdown(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        // Don't rewrite link reference definitions like: [foo]: https://example.com
        if (content.startsWith("[") && content.contains("]:")) {
            int close = content.indexOf(']');
            if (close >= 0 && close + 1 < content.length() && content.charAt(close + 1) == ':') {
                return content;
            }
        }
        Matcher matcher = KEY_BINDING_LIST_ITEM_PATTERN.matcher(content);
        if (matcher.matches()) {
            String prefix = matcher.group(1);
            String key = matcher.group(2);
            String rest = matcher.group(3);
            return prefix + "`" + key + "`" + rest;
        }
        return content;
    }

    private static List<String> convertDocLines(List<String> rawLines, String indent) {
        return convertDocLines(rawLines, indent, true);
    }

    private static List<String> convertDocLines(List<String> rawLines, String indent, boolean addSpaceAfterPrefix) {
        List<String> trimmed = trimBlankEdges(rawLines);
        List<String> output = new ArrayList<>();
        String listMode = null;
        boolean inFence = false;

        for (String line : trimmed) {
            if (line == null) {
                output.add(docLine(indent, "", addSpaceAfterPrefix));
                continue;
            }

            String content = line;
            String lineTrimmed = content.trim();

            if (lineTrimmed.startsWith("```")) {
                output.add(docLine(indent, lineTrimmed, addSpaceAfterPrefix));
                inFence = !inFence;
                continue;
            }

            if (inFence) {
                int endIndex = content.indexOf("</pre>");
                if (endIndex >= 0) {
                    String before = content.substring(0, endIndex);
                    if (!before.isEmpty()) {
                            output.add(docLine(indent, before, addSpaceAfterPrefix));
                    }
                        output.add(docLine(indent, "```", addSpaceAfterPrefix));
                    inFence = false;
                    String after = content.substring(endIndex + "</pre>".length()).trim();
                    if (!after.isEmpty()) {
                            output.add(docLine(indent, after, addSpaceAfterPrefix));
                    }
                } else {
                        output.add(docLine(indent, content, addSpaceAfterPrefix));
                }
                continue;
            }

            if (lineTrimmed.isEmpty()) {
                output.add(docLine(indent, "", addSpaceAfterPrefix));
                continue;
            }

            Matcher heading = HEADING_PATTERN.matcher(lineTrimmed);
            if (heading.matches()) {
                int level = Integer.parseInt(heading.group(1));
                String title = heading.group(2).trim();
                output.add(docLine(indent, "#".repeat(level) + " " + title, addSpaceAfterPrefix));
                continue;
            }

            Matcher preStart = PRE_START_PATTERN.matcher(lineTrimmed);
            if (preStart.matches()) {
                output.add(docLine(indent, lineTrimmed.contains("{@code") ? "```java" : "```", addSpaceAfterPrefix));
                inFence = true;
                continue;
            }

            int inlinePre = lineTrimmed.indexOf("<pre>");
            if (inlinePre >= 0) {
                output.add(docLine(indent, lineTrimmed.contains("{@code") ? "```java" : "```", addSpaceAfterPrefix));
                inFence = true;
                continue;
            }

            if (lineTrimmed.contains("</pre>")) {
                String before = lineTrimmed.replace("</pre>", "").trim();
                if (!before.isEmpty()) {
                    output.add(docLine(indent, before, addSpaceAfterPrefix));
                }
                output.add(docLine(indent, "```", addSpaceAfterPrefix));
                continue;
            }

            List<String> segments = splitParagraphs(content);
            for (String segment : segments) {
                String segmentTrim = segment.trim();

                if (segmentTrim.isEmpty()) {
                    output.add(docLine(indent, "", addSpaceAfterPrefix));
                    continue;
                }

                if (UL_OPEN_PATTERN.matcher(segmentTrim).matches()) {
                    listMode = "ul";
                    output.add(docLine(indent, "", addSpaceAfterPrefix));
                    continue;
                }
                if (UL_CLOSE_PATTERN.matcher(segmentTrim).matches()) {
                    listMode = null;
                    output.add(docLine(indent, "", addSpaceAfterPrefix));
                    continue;
                }
                if (OL_OPEN_PATTERN.matcher(segmentTrim).matches()) {
                    listMode = "ol";
                    output.add(docLine(indent, "", addSpaceAfterPrefix));
                    continue;
                }
                if (OL_CLOSE_PATTERN.matcher(segmentTrim).matches()) {
                    listMode = null;
                    output.add(docLine(indent, "", addSpaceAfterPrefix));
                    continue;
                }

                Matcher liMatcher = LI_PATTERN.matcher(segment);
                boolean matchedLi = false;
                while (liMatcher.find()) {
                    matchedLi = true;
                    String item = applyInline(liMatcher.group(1).trim());
                    String prefix = "ol".equals(listMode) ? "1." : "-";
                    output.add(docLine(indent, item.isEmpty() ? prefix : prefix + " " + item, addSpaceAfterPrefix));
                }
                if (matchedLi) {
                    continue;
                }

                String cleaned = applyInline(segmentTrim.replace("</li>", ""));
                output.add(docLine(indent, cleaned, addSpaceAfterPrefix));
            }
        }

        return trimTrailingBlankLines(output, indent, addSpaceAfterPrefix);
    }

    private static List<String> splitParagraphs(String line) {
        String normalized = BR_PATTERN.matcher(line).replaceAll("\n");
        normalized = P_OPEN_PATTERN.matcher(normalized).replaceAll("\n\n");
        normalized = P_CLOSE_PATTERN.matcher(normalized).replaceAll("");
        return Arrays.asList(normalized.split("\n", -1));
    }

    private static String applyInline(String text) {
        String result = text;
        result = result.replace("<strong>", "**");
        result = result.replace("</strong>", "**");
        result = result.replace("<b>", "**");
        result = result.replace("</b>", "**");
        result = result.replace("<em>", "*");
        result = result.replace("</em>", "*");
        result = result.replace("<code>", "`");
        result = result.replace("</code>", "`");
        return result;
    }

    private static List<String> trimBlankEdges(List<String> rawLines) {
        int start = 0;
        int end = rawLines.size();
        while (start < end && rawLines.get(start).trim().isEmpty()) {
            start++;
        }
        while (end > start && rawLines.get(end - 1).trim().isEmpty()) {
            end--;
        }
        return rawLines.subList(start, end);
    }

    private static List<String> trimTrailingBlankLines(List<String> lines, String indent, boolean addSpaceAfterPrefix) {
        int end = lines.size();
        while (end > 0 && lines.get(end - 1).equals(docLine(indent, "", addSpaceAfterPrefix))) {
            end--;
        }
        return new ArrayList<>(lines.subList(0, end));
    }

    private static String docLine(String indent, String content, boolean addSpaceAfterPrefix) {
        if (content == null || content.isEmpty()) {
            return indent + "///";
        }
        return indent + "///" + (addSpaceAfterPrefix ? " " : "") + sanitizeForJavadocMarkdown(content);
    }

    private static final class Arguments {
        private final boolean write;
        private final boolean help;
        private final boolean verbose;
        private final List<Path> paths;
        private final List<String> excludedDirs;

        private Arguments(
                boolean write,
                boolean help,
                boolean verbose,
                List<Path> paths,
                List<String> excludedDirs
        ) {
            this.write = write;
            this.help = help;
            this.verbose = verbose;
            this.paths = paths;
            this.excludedDirs = excludedDirs;
        }

        private static Arguments parse(String[] args) {
            boolean write = false;
            boolean help = false;
            boolean verbose = false;
            List<Path> paths = new ArrayList<>();
            List<String> excludedDirs = new ArrayList<>();

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--write":
                        write = true;
                        break;
                    case "--check":
                        write = false;
                        break;
                    case "--verbose":
                        verbose = true;
                        break;
                    case "--exclude-dir":
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("Missing value for --exclude-dir");
                        }
                        excludedDirs.add(args[++i]);
                        break;
                    case "-h":
                    case "--help":
                        help = true;
                        break;
                    default:
                        paths.add(Paths.get(arg));
                        break;
                }
            }

            return new Arguments(write, help, verbose, paths, excludedDirs);
        }
    }

    private static final class ConversionResult {
        private final Path path;
        private final boolean changed;
        private final String content;

        private ConversionResult(Path path, boolean changed, String content) {
            this.path = path;
            this.changed = changed;
            this.content = content;
        }
    }
}
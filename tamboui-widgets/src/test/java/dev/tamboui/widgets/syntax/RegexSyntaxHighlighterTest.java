/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.syntax;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.style.Tags;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class RegexSyntaxHighlighterTest {

    private static final Style BASE = Style.EMPTY.fg(Color.GRAY);
    private static final SyntaxTheme THEME = SyntaxTheme.DEFAULTS;

    private static Color.Rgb rgb(TokenType type) {
        return THEME.style(type, BASE).fg().map(Color::toRgb).orElse(null);
    }

    private static String raw(Line line) {
        return line.rawContent();
    }

    private static Color.Rgb fgOf(Line line, String content) {
        for (Span span : line.spans()) {
            if (span.content().equals(content)) {
                return span.style().fg().map(Color::toRgb).orElse(null);
            }
        }
        return null;
    }

    private static List<Line> highlight(String code, String language) {
        return RegexSyntaxHighlighter.defaults().highlight(code, language, BASE, THEME);
    }

    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    @Test
    @DisplayName("returns plain lines when the language is unknown")
    void unknownLanguageIsPlain() {
        List<Line> lines = highlight("let x = 1;", "unknown-lang");
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).spans()).hasSize(1);
        assertThat(raw(lines.get(0))).isEqualTo("let x = 1;");
        assertThat(lines.get(0).spans().get(0).style().fg().map(Color::toRgb).get())
            .isEqualTo(Color.GRAY.toRgb());
    }

    @Test
    @DisplayName("highlights keywords, types, numbers, strings and comments in Java")
    void highlightsJava() {
        String code = "// a comment\n"
            + "int x = 42;\n"
            + "String name = \"hi\";\n"
            + "public String greet(String who) {}";
        List<Line> lines = highlight(code, "java");

        // Line 0 is a comment.
        assertThat(raw(lines.get(0))).isEqualTo("// a comment");

        // Line 1: "int" keyword, "42" number.
        assertThat(fgOf(lines.get(1), "int")).isEqualTo(rgb(TokenType.KEYWORD));
        assertThat(fgOf(lines.get(1), "42")).isEqualTo(rgb(TokenType.NUMBER));

        // Line 2: the string literal "hi" is a STRING token.
        assertThat(fgOf(lines.get(2), "\"hi\"")).isEqualTo(rgb(TokenType.STRING));

        // Capitalized type names are colored as TYPE.
        assertThat(fgOf(lines.get(3), "String")).isEqualTo(rgb(TokenType.TYPE));
    }

    @Test
    @DisplayName("default highlighters reuse compiled built-in grammars")
    void defaultHighlightersReuseBuiltInGrammars() {
        RegexSyntaxHighlighter first = RegexSyntaxHighlighter.defaults();
        RegexSyntaxHighlighter second = RegexSyntaxHighlighter.defaults();

        assertThat(first.grammar("java")).isSameAs(second.grammar("java"));
    }

    @Test
    @DisplayName("matches a language by id and alias, case-insensitively")
    void resolvesAliases() {
        RegexSyntaxHighlighter hl = RegexSyntaxHighlighter.defaults();
        assertThat(hl.grammar("java")).isNotNull();
        assertThat(hl.grammar("js")).isNotNull();
        assertThat(hl.grammar("py")).isNotNull();
        assertThat(hl.grammar("yml")).isNotNull();
        assertThat(hl.grammar("JS")).isNotNull();
        assertThat(hl.grammar("nope")).isNull();
    }

    @Test
    @DisplayName("handles multi-line block comments across lines")
    void multiLineCommentIsOneToken() {
        String code = "/* starts\nstill comment\nends */\ncode";
        List<Line> lines = highlight(code, "java");
        assertThat(fgOf(lines.get(0), "/* starts")).isEqualTo(rgb(TokenType.COMMENT));
        assertThat(fgOf(lines.get(1), "still comment")).isEqualTo(rgb(TokenType.COMMENT));
        assertThat(lines.get(2).rawContent()).isEqualTo("ends */");
        assertThat(fgOf(lines.get(2), "ends */")).isEqualTo(rgb(TokenType.COMMENT));
        assertThat(raw(lines.get(3))).isEqualTo("code");
    }

    @Test
    @DisplayName("produces correct line counts for a trailing newline")
    void trailingNewline() {
        List<Line> lines = highlight("a\nb\n", "java");
        assertThat(lines).hasSize(2);
        assertThat(raw(lines.get(0))).isEqualTo("a");
        assertThat(raw(lines.get(1))).isEqualTo("b");
    }

    @Test
    @DisplayName("a custom theme is honoured by the highlighter")
    void customThemeApplies() {
        Style override = Style.EMPTY.fg(Color.RED);
        SyntaxTheme theme = SyntaxTheme.builder().token(TokenType.KEYWORD, override).build();
        List<Line> lines = RegexSyntaxHighlighter.defaults().highlight("public", "java", BASE, theme);
        assertThat(fgOf(lines.get(0), "public")).isEqualTo(Color.RED.toRgb());
    }

    @Test
    @DisplayName("none() keeps each line as a single plain span")
    void noneHighlighterIsPlain() {
        List<Line> lines = SyntaxHighlighter.none().highlight("int x = 1;\nboom", "java", BASE, THEME);
        assertThat(lines).hasSize(2);
        for (Line line : lines) {
            assertThat(line.spans()).hasSize(1);
        }
    }

    @Test
    @DisplayName("comment modifier is italic")
    void commentIsItalic() {
        List<Line> lines = highlight("// note", "java");
        for (Span span : lines.get(0).spans()) {
            if (span.content().contains("note")) {
                assertThat(span.style().addModifiers()).contains(Modifier.ITALIC);
            }
        }
    }

    @Test
    @DisplayName("a zero-length custom rule is rejected without stalling")
    void zeroLengthCustomRuleIsRejected() {
        Grammar grammar = Grammar.builder("custom")
            .rule(Grammar.Rule.pattern(TokenType.KEYWORD, Pattern.compile("")))
            .build();
        RegexSyntaxHighlighter highlighter = RegexSyntaxHighlighter.of(grammar);

        IllegalArgumentException exception = assertTimeoutPreemptively(Duration.ofSeconds(1),
            () -> assertThrows(IllegalArgumentException.class,
                () -> highlighter.highlight("hello", "custom", BASE, THEME)));

        assertThat(exception).hasMessage(
            "Grammar rule KEYWORD matched an empty token at offset 0; rules must consume at least one character");
    }

    @Test
    @DisplayName("an empty literal is rejected when constructing a rule")
    void emptyLiteralIsRejected() {
        assertThatThrownBy(() -> Grammar.Rule.literal(TokenType.KEYWORD, ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Literal rule text must not be empty");
    }

    @Test
    @DisplayName("empty multiline delimiters are rejected when constructing a rule")
    void emptyMultilineDelimitersAreRejected() {
        assertThatThrownBy(() -> Grammar.Rule.multiline(TokenType.COMMENT, "", "*/"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Multiline rule open delimiter must not be empty");
        assertThatThrownBy(() -> Grammar.Rule.multiline(TokenType.COMMENT, "/*", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Multiline rule close delimiter must not be empty");
    }

    @Test
    @DisplayName("highlight survives an unclosed string of 20k characters")
    void highlightSurvivesUnclosedLongString() {
        // Streaming markdown routinely contains a not-yet-closed string literal
        // mid-stream. Alternation-under-star string rules recurse per character
        // in Java's regex engine, so this input must neither crash with
        // StackOverflowError nor take pathologically long.
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < 20000; i++) {
            sb.append('a');
        }
        String unclosed = sb.toString();

        assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                highlight(unclosed, "java"));
    }

    @Test
    @DisplayName("highlight survives escape-heavy input")
    void highlightSurvivesEscapeSpam() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append("\"a\\");
        }
        String escapeSpam = sb.toString();

        assertTimeoutPreemptively(Duration.ofSeconds(2), () ->
                highlight(escapeSpam, "js"));
    }

    @Test
    @DisplayName("escaped strings are still highlighted as strings")
    void escapedStringsAreHighlighted() {
        assertThat(fgOf(highlight("String s = \"a\\\"b\";", "java").get(0), "\"a\\\"b\""))
            .isEqualTo(rgb(TokenType.STRING));
        assertThat(fgOf(highlight("char c = '\\'';", "java").get(0), "'\\''"))
            .isEqualTo(rgb(TokenType.STRING));
    }

    @Test
    @DisplayName("an unclosed or escape-heavy string run does not stack overflow")
    void unclosedStringDoesNotStackOverflow() {
        for (String q : new String[] {"\"", "'", "`"}) {
            int n = 9_000;
            assertThatCode(() -> highlight(q + repeat('a', n), "python"))
                .doesNotThrowAnyException();
            assertThatCode(() -> highlight(q + repeat('\\', n), "python"))
                .doesNotThrowAnyException();
            assertThatCode(() -> highlight(q + repeat('a', n), "javascript"))
                .doesNotThrowAnyException();
            assertThatCode(() -> highlight(q + repeat('a', n), "sql"))
                .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("the maximum line length must be positive")
    void maximumLineLengthMustBePositive() {
        assertThatThrownBy(() -> RegexSyntaxHighlighter.builder().maxLineLength(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("maxLineLength must be greater than zero");
    }

    @Test
    @DisplayName("lines longer than the configured cap fall back to plain rendering")
    void overLongLinesFallBackToPlain() {
        int cap = 16;
        RegexSyntaxHighlighter hl =
            RegexSyntaxHighlighter.builder().maxLineLength(cap).build();
        List<Line> lines = hl.highlight(repeat('a', cap + 1) + "= 1", "java", BASE, THEME);
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).spans()).hasSize(1);
        assertThat(raw(lines.get(0))).isEqualTo(repeat('a', cap + 1) + "= 1");
    }

    @Test
    @DisplayName("function tokens exclude call punctuation")
    void functionTokensExcludeCallPunctuation() {
        List<Line> lines = highlight("run()", "java");

        assertThat(typeOf(lines, "run")).contains(TokenType.FUNCTION);
        assertThat(typeOf(lines, "(")).contains(TokenType.PUNCTUATION);
    }

    @Test
    @DisplayName("HTML distinguishes tag names, attributes, and text")
    void htmlDistinguishesTagNamesAttributesAndText() {
        List<Line> lines = highlight("<a href=\"target\">link</a>", "html");

        assertThat(lines.get(0).spans())
            .filteredOn(span -> span.content().equals("a"))
            .extracting(span -> span.style().extension(TokenType.class).orElse(null))
            .containsExactly(TokenType.TAG, TokenType.TAG);
        assertThat(typeOf(lines, "href")).contains(TokenType.ATTRIBUTE);
        assertThat(typeOf(lines, "link")).contains(TokenType.PLAIN);
    }

    @Test
    @DisplayName("properties keys are line-anchored: URL-heavy values stay plain")
    void propertiesKeysAreLineAnchored() {
        // 'http' sits before a ':' mid-line - without line anchoring it would
        // be mistaken for a key; Camel-style URI values make this common.
        List<Line> lines = highlight("uri=http://x:8080/y", "properties");

        assertThat(fgOf(lines.get(0), "uri")).isEqualTo(rgb(TokenType.ATTRIBUTE));
        for (Span span : lines.get(0).spans()) {
            if (span.content().contains("http")) {
                assertThat(span.style().fg().map(Color::toRgb).orElse(null))
                    .as("URL fragment must not be key-colored")
                    .isNotEqualTo(rgb(TokenType.ATTRIBUTE));
            }
        }
    }

    @Test
    @DisplayName("YAML keys are keys wherever they start, a value such as timer:tick is not")
    void yamlValueWithSchemeIsNotAKey() {
        List<Line> lines = highlight("- route:\n"
            + "    from:\n"
            + "      uri: timer:tick\n"
            + "      parameters: {period: 1000}\n"
            + "      steps:\n"
            + "        - to: http://localhost:8080/api\n"
            + "        - log: ${body}", "yaml");

        assertThat(keys(lines)).containsExactly(
            "route:", "from:", "uri:", "parameters:", "period:", "steps:", "to:", "log:");
    }

    @Test
    @DisplayName("long YAML values are highlighted in linear time")
    void longYamlValuesAreHighlightedQuickly() {
        StringBuilder sb = new StringBuilder();
        for (int line = 0; line < 20; line++) {
            sb.append("key").append(line).append(": ").append(repeat('x', 9_000)).append('\n');
        }
        String yaml = sb.toString();

        List<Line> lines = assertTimeoutPreemptively(Duration.ofSeconds(1), () -> highlight(yaml, "yaml"));

        assertThat(keys(lines)).hasSize(20);
    }

    @Test
    @DisplayName("^ in a rule matches at the start of a line, not wherever a token starts")
    void caretMatchesOnlyAtLineStart() {
        Grammar grammar = Grammar.builder("diff")
            .rule(Grammar.Rule.pattern(TokenType.STRING, Pattern.compile("^\\+[^\\n]*", Pattern.MULTILINE)))
            .rule(Grammar.Rule.pattern(TokenType.NUMBER, Pattern.compile("\\d+")))
            .build();

        List<Line> lines = RegexSyntaxHighlighter.of(grammar).highlight("kept 2 +3\n+added 4", "diff", BASE, THEME);

        assertThat(lines.get(0).spans()).extracting(Span::content).containsExactly("kept ", "2", " +", "3");
        assertThat(typeOf(lines, "+added 4")).contains(TokenType.STRING);
    }

    @Test
    @DisplayName("spans carry their token class as TokenType and Tags extensions")
    void spansCarryTokenClass() {
        List<Line> lines = highlight("class total = \"x\"; // done", "java");

        assertThat(typeOf(lines, "class")).contains(TokenType.KEYWORD);
        assertThat(typeOf(lines, "\"x\"")).contains(TokenType.STRING);
        assertThat(typeOf(lines, "// done")).contains(TokenType.COMMENT);
        // Untokenized text is tagged PLAIN, so consumers can distinguish
        // "not tokenized" from "no highlighter ran"
        assertThat(typeOf(lines, " total ")).contains(TokenType.PLAIN);

        // The same class also rides the established Tags mechanism, following
        // the "syntax-<type>" naming convention
        assertThat(tagsOf(lines, "class")).contains("syntax-keyword");
        assertThat(tagsOf(lines, "// done")).contains("syntax-comment");
    }

    private static List<String> keys(List<Line> lines) {
        List<String> keys = new ArrayList<>();
        for (Line line : lines) {
            for (Span span : line.spans()) {
                if (span.style().extension(TokenType.class).orElse(null) == TokenType.KEYWORD) {
                    keys.add(span.content().trim());
                }
            }
        }
        return keys;
    }

    private static java.util.Optional<TokenType> typeOf(List<Line> lines, String content) {
        for (Line line : lines) {
            for (Span span : line.spans()) {
                if (span.content().equals(content)) {
                    return span.style().extension(TokenType.class);
                }
            }
        }
        return java.util.Optional.empty();
    }

    private static java.util.Set<String> tagsOf(List<Line> lines, String content) {
        for (Line line : lines) {
            for (Span span : line.spans()) {
                if (span.content().equals(content)) {
                    return span.style().extension(Tags.class).map(Tags::values)
                        .orElse(java.util.Collections.emptySet());
                }
            }
        }
        return java.util.Collections.emptySet();
    }
}
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.syntax;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;

import static org.assertj.core.api.Assertions.assertThat;

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
}
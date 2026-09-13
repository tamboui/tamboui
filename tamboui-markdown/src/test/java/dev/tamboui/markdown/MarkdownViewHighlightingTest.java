/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.syntax.SyntaxHighlighter;
import dev.tamboui.widgets.syntax.SyntaxTheme;
import dev.tamboui.widgets.syntax.TokenType;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownViewHighlightingTest {

    private static Buffer render(MarkdownView view, int width, int height) {
        Rect area = new Rect(0, 0, width, height);
        Buffer buffer = Buffer.empty(area);
        view.render(area, buffer);
        return buffer;
    }

    @Test
    @DisplayName("highlights keywords in a java fenced code block")
    void highlightsJavaKeyword() {
        MarkdownView view = MarkdownView.builder()
            .source("```java\npublic class Demo {}\n```")
            .build();
        Buffer buffer = render(view, 30, 3);

        // Content starts at column 1 (rounded border at col 0).
        // "public" is a keyword and should carry the keyword color.
        Color.Rgb keywordRgb = SyntaxTheme.DEFAULTS.style(TokenType.KEYWORD, Style.EMPTY).fg()
            .map(Color::toRgb).orElse(null);
        assertThat(buffer.get(1, 1).style().fg().map(Color::toRgb)).isPresent();
        assertThat(buffer.get(1, 1).style().fg().map(Color::toRgb).get()).isEqualTo(keywordRgb);
        assertThat(buffer.get(1, 1).symbol()).isEqualTo("p");
    }

    @Test
    @DisplayName("keywords are not highlighted when a no-op highlighter is used")
    void noHighlightWhenNone() {
        MarkdownView view = MarkdownView.builder()
            .source("```java\npublic class Demo {}\n```")
            .syntaxHighlighter(SyntaxHighlighter.none())
            .build();
        Buffer buffer = render(view, 30, 3);

        // The code base style is gray (default code block), no per-token color.
        assertThat(buffer.get(1, 1).style().fg().map(Color::toRgb).get())
            .isEqualTo(Color.GRAY.toRgb());
    }

    @Test
    @DisplayName("a custom theme takes effect inside a code block")
    void customThemeTakesEffect() {
        SyntaxTheme theme = SyntaxTheme.builder().build();
        MarkdownView view = MarkdownView.builder()
            .source("```java\npublic class Demo {}\n```")
            .syntaxTheme(theme)
            .build();
        Buffer buffer = render(view, 30, 3);

        Color.Rgb keywordRgb = theme.style(TokenType.KEYWORD, Style.EMPTY).fg().map(Color::toRgb).orElse(null);
        assertThat(buffer.get(1, 1).style().fg().map(Color::toRgb).get()).isEqualTo(keywordRgb);
    }

    @Test
    @DisplayName("renders a code block with a base color that is patched by token styles")
    void baseCodeStylePatched() {
        dev.tamboui.markdown.MarkdownStyles styles = dev.tamboui.markdown.MarkdownStyles.builder()
            .codeBlock(dev.tamboui.style.Style.EMPTY.fg(Color.YELLOW))
            .build();
        MarkdownView view = MarkdownView.builder()
            .source("```java\nnote tweak\n```")
            .styles(styles)
            .build();
        Buffer buffer = render(view, 30, 3);

        // "note" is not a Java keyword and should keep the yellow base foreground.
        assertThat(buffer.get(1, 1).style().fg().map(Color::toRgb).get())
            .isEqualTo(Color.YELLOW.toRgb());
    }
}
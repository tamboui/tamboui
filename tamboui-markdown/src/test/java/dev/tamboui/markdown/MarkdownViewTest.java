/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownViewTest {

    private static Buffer renderInto(MarkdownView view, int width, int height) {
        Rect area = new Rect(0, 0, width, height);
        Buffer buffer = Buffer.empty(area);
        view.render(area, buffer);
        return buffer;
    }

    @Test
    @DisplayName("renders an H1 heading bolded with an underline rule")
    void rendersH1Heading() {
        MarkdownView view = MarkdownView.builder().source("# Hello").build();
        Buffer buffer = renderInto(view, 20, 3);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(0, 0).style().addModifiers()).contains(Modifier.BOLD);
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("═");
    }

    @Test
    @DisplayName("renders strong text as bold")
    void rendersStrong() {
        MarkdownView view = MarkdownView.builder().source("hello **world**").build();
        Buffer buffer = renderInto(view, 30, 1);

        assertThat(buffer.get(0, 0).style().addModifiers()).doesNotContain(Modifier.BOLD);
        assertThat(buffer.get(6, 0).symbol()).isEqualTo("w");
        assertThat(buffer.get(6, 0).style().addModifiers()).contains(Modifier.BOLD);
    }

    @Test
    @DisplayName("renders emphasis as italic")
    void rendersEmphasis() {
        MarkdownView view = MarkdownView.builder().source("plain *italic*").build();
        Buffer buffer = renderInto(view, 30, 1);

        assertThat(buffer.get(6, 0).symbol()).isEqualTo("i");
        assertThat(buffer.get(6, 0).style().addModifiers()).contains(Modifier.ITALIC);
    }

    @Test
    @DisplayName("renders strikethrough as crossed-out")
    void rendersStrikethrough() {
        MarkdownView view = MarkdownView.builder().source("~~gone~~").build();
        Buffer buffer = renderInto(view, 30, 1);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("g");
        assertThat(buffer.get(0, 0).style().addModifiers()).contains(Modifier.CROSSED_OUT);
    }

    @Test
    @DisplayName("renders inline code with the inline-code style")
    void rendersInlineCode() {
        MarkdownView view = MarkdownView.builder().source("see `foo` now").build();
        Buffer buffer = renderInto(view, 30, 1);

        assertThat(buffer.get(4, 0).symbol()).isEqualTo("f");
        assertThat(buffer.get(4, 0).style().addModifiers()).contains(Modifier.DIM);
    }

    @Test
    @DisplayName("renders a link with the OSC-8 hyperlink extension and underline")
    void rendersLink() {
        MarkdownView view = MarkdownView.builder()
            .source("see [docs](https://example.com) here")
            .build();
        Buffer buffer = renderInto(view, 40, 1);

        assertThat(buffer.get(4, 0).symbol()).isEqualTo("d");
        assertThat(buffer.get(4, 0).style().addModifiers()).contains(Modifier.UNDERLINED);
        assertThat(buffer.get(4, 0).style().hyperlink()).isPresent()
            .get()
            .extracting(h -> h.url())
            .isEqualTo("https://example.com");
    }

    @Test
    @DisplayName("renders a bullet list")
    void rendersBulletList() {
        MarkdownView view = MarkdownView.builder().source("- one\n- two").build();
        Buffer buffer = renderInto(view, 20, 2);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("•");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("o");
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("•");
        assertThat(buffer.get(2, 1).symbol()).isEqualTo("t");
    }

    @Test
    @DisplayName("renders an ordered list with sequential markers")
    void rendersOrderedList() {
        MarkdownView view = MarkdownView.builder().source("1. one\n2. two").build();
        Buffer buffer = renderInto(view, 20, 2);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("1");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo(".");
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("2");
    }

    @Test
    @DisplayName("renders a GFM task list with check glyphs")
    void rendersTaskList() {
        MarkdownView view = MarkdownView.builder()
            .source("- [x] done\n- [ ] todo")
            .build();
        Buffer buffer = renderInto(view, 30, 2);

        // After "• ", the task marker is "[x] " or "[ ] ".
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("[");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("x");
        assertThat(buffer.get(2, 1).symbol()).isEqualTo("[");
        assertThat(buffer.get(3, 1).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("renders a blockquote with a leading bar")
    void rendersBlockquote() {
        MarkdownView view = MarkdownView.builder().source("> quoted").build();
        Buffer buffer = renderInto(view, 20, 1);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("│");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("q");
        assertThat(buffer.get(2, 0).style().addModifiers()).contains(Modifier.DIM);
    }

    @Test
    @DisplayName("renders a thematic break as a row of horizontal-rule glyphs")
    void rendersThematicBreak() {
        MarkdownView view = MarkdownView.builder().source("---").build();
        Buffer buffer = renderInto(view, 6, 1);

        for (int x = 0; x < 6; x++) {
            assertThat(buffer.get(x, 0).symbol()).isEqualTo("─");
        }
    }

    @Test
    @DisplayName("renders a fenced code block inside a rounded border with the info string as title")
    void rendersFencedCodeBlock() {
        MarkdownView view = MarkdownView.builder()
            .source("```java\nint x = 1;\n```")
            .build();
        Buffer buffer = renderInto(view, 20, 4);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("╭");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("j");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("a");
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("╰");
        assertThat(buffer.get(1, 1).symbol()).isEqualTo("i");
    }

    @Test
    @DisplayName("renders an HTML block as dim escaped text")
    void rendersHtmlBlock() {
        MarkdownView view = MarkdownView.builder()
            .source("<div>hi</div>")
            .build();
        Buffer buffer = renderInto(view, 20, 1);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("<");
        assertThat(buffer.get(0, 0).style().addModifiers()).contains(Modifier.DIM);
    }

    @Test
    @DisplayName("renders a GFM table with header and rows")
    void rendersTable() {
        String md = "| Name | Age |\n| --- | --- |\n| Ada | 36 |\n| Bob | 47 |";
        MarkdownView view = MarkdownView.builder().source(md).build();
        Buffer buffer = renderInto(view, 20, 3);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("N");
        assertThat(buffer.get(0, 0).style().addModifiers()).contains(Modifier.BOLD);
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("A");
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("B");
    }

    @Test
    @DisplayName("scrolls past the first chunk when scroll is set")
    void scrollsContent() {
        // Layout produces: heading(1) + rule(1) + spacer(1) + paragraph(1) = 4 rows
        // before "body line two". Skip those four to reveal the second paragraph.
        MarkdownView view = MarkdownView.builder()
            .source("# Title\n\nbody line one\n\nbody line two")
            .scroll(6)
            .build();
        Buffer buffer = renderInto(view, 20, 1);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("b");
        assertThat(buffer.get(10, 0).symbol()).isEqualTo("t");
    }

    @Test
    @DisplayName("renders an image as bracketed alt text plus URL")
    void rendersImage() {
        MarkdownView view = MarkdownView.builder()
            .source("![cat](https://x/y.png)")
            .build();
        Buffer buffer = renderInto(view, 40, 1);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("[");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("i");
    }

    @Test
    @DisplayName("computeHeight returns 0 for non-positive widths")
    void computeHeightZero() {
        MarkdownView view = MarkdownView.builder().source("# Title").build();
        assertThat(view.computeHeight(0)).isZero();
        assertThat(view.computeHeight(-5)).isZero();
    }

    @Test
    @DisplayName("computeHeight reports height that scales with wrap width")
    void computeHeightWraps() {
        // Long single paragraph; narrower widths require more rows.
        String prose = "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu nu";
        MarkdownView view = MarkdownView.builder().source(prose).build();

        int wide = view.computeHeight(80);
        int narrow = view.computeHeight(20);

        assertThat(wide).isEqualTo(1);
        assertThat(narrow).isGreaterThan(wide);
    }

    @Test
    @DisplayName("computeHeight accounts for outer Block chrome")
    void computeHeightWithBlock() {
        MarkdownView plain = MarkdownView.builder().source("# Title").build();
        MarkdownView boxed = MarkdownView.builder()
            .source("# Title")
            .block(dev.tamboui.widgets.block.Block.bordered())
            .build();

        // ALL borders add 2 rows of vertical chrome.
        assertThat(boxed.computeHeight(20)).isEqualTo(plain.computeHeight(20) + 2);
    }

    @Test
    @DisplayName("survives streaming a partial document one character at a time")
    void survivesStreaming() {
        String full = "# Hello\n\nthis is **bold** and `code` and a [link](https://example.com).";
        for (int len = 1; len <= full.length(); len++) {
            MarkdownView view = MarkdownView.builder().source(full.substring(0, len)).build();
            renderInto(view, 30, 6);
        }
    }
}

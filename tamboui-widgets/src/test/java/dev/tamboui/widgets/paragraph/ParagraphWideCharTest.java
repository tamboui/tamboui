/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.paragraph;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class ParagraphWideCharTest {

    private static final String CODER = "\uD83D\uDC69\u200D\uD83D\uDCBB";
    private static final String FAMILY = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67";
    private static final Style RED = Style.EMPTY.fg(Color.RED);
    private static final Style BLUE = Style.EMPTY.fg(Color.BLUE);

    @Test
    @DisplayName("Clip mode clips CJK text at display width boundary")
    void clipCjkText() {
        Paragraph p = Paragraph.builder()
                .text(Text.from("世界你好"))  // 4 chars * 2 width = 8 display cols
                .overflow(Overflow.CLIP)
                .build();

        // Render in 5-wide area: only "世界" fits (4 cols), "你" would need 6
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 1));
        p.render(buffer.area(), buffer);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("世");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("界");
        // Column 4 should be empty (你 doesn't fit in remaining 1 column)
        assertThat(buffer.get(4, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Ellipsis mode truncates CJK text with ellipsis")
    void ellipsisCjkText() {
        Paragraph p = Paragraph.builder()
                .text(Text.from("世界你好啊"))  // 10 display cols
                .overflow(Overflow.ELLIPSIS)
                .build();

        // Render in 7-wide area: available = 7 - 3 (ellipsis) = 4 cols = "世界" + "..."
        Buffer buffer = Buffer.empty(new Rect(0, 0, 7, 1));
        p.render(buffer.area(), buffer);

        String line = extractLineText(buffer, 0);
        assertThat(line).isEqualTo("世界...");
    }

    @Test
    @DisplayName("Ellipsis start with CJK text")
    void ellipsisStartCjkText() {
        Paragraph p = Paragraph.builder()
                .text(Text.from("世界你好啊"))  // 10 display cols
                .overflow(Overflow.ELLIPSIS_START)
                .build();

        // Render in 7-wide area: available = 7 - 3 = 4 cols from end = "好啊"
        Buffer buffer = Buffer.empty(new Rect(0, 0, 7, 1));
        p.render(buffer.area(), buffer);

        String line = extractLineText(buffer, 0);
        assertThat(line).isEqualTo("...好啊");
    }

    @Test
    @DisplayName("Ellipsis modes keep a CJK character at the cut whole or leave it out, keeping span styles")
    void ellipsisCjkAtCutKeepsSpanStyles() {
        // 2 + 8 + 3 = 13 display cols
        Line line = Line.from(new Span("ab", BLUE), new Span("世界你好", RED), new Span("end", BLUE));

        // 界 does not fit in the column left before the ellipsis, which takes the style of the text it replaces
        assertRendersAs(line, Overflow.ELLIPSIS, 8, new Span("ab", BLUE), new Span("世...", RED));
        // 你 does not fit in the column left after the ellipsis
        assertRendersAs(line, Overflow.ELLIPSIS_START, 9, new Span("...好", RED), new Span("end", BLUE));
        // 好 does not fit in the column left after the ellipsis
        assertRendersAs(line, Overflow.ELLIPSIS_MIDDLE, 11,
                new Span("ab", BLUE), new Span("世...", RED), new Span("end", BLUE));
    }

    @Test
    @DisplayName("Ellipsis modes keep a ZWJ emoji at the cut whole or leave it out, keeping span styles")
    void ellipsisZwjEmojiAtCutKeepsSpanStyles() {
        // At each cut the emoji gets one column, and is left out, or exactly its two columns, and is kept whole
        Line end = Line.from(new Span("ab", BLUE), new Span(" " + CODER + " and more", RED));
        assertRendersAs(end, Overflow.ELLIPSIS, 7, new Span("ab", BLUE), new Span(" ...", RED));
        assertRendersAs(end, Overflow.ELLIPSIS, 8, new Span("ab", BLUE), new Span(" " + CODER + "...", RED));

        Line start = Line.from(new Span("more text " + FAMILY + " ", RED), new Span("end", BLUE));
        assertRendersAs(start, Overflow.ELLIPSIS_START, 8, new Span("... ", RED), new Span("end", BLUE));
        assertRendersAs(start, Overflow.ELLIPSIS_START, 9,
                new Span("..." + FAMILY + " ", RED), new Span("end", BLUE));

        Line middle = Line.from(
                new Span("ab", BLUE), new Span(" " + CODER + " text that is long ", RED), new Span("end", BLUE));
        assertRendersAs(middle, Overflow.ELLIPSIS_MIDDLE, 11,
                new Span("ab", BLUE), new Span(" ... ", RED), new Span("end", BLUE));
        assertRendersAs(middle, Overflow.ELLIPSIS_MIDDLE, 12,
                new Span("ab", BLUE), new Span(" " + CODER + "... ", RED), new Span("end", BLUE));
    }

    @Test
    @DisplayName("Wrap character mode wraps CJK text at display width boundary")
    void wrapCharacterCjkText() {
        Paragraph p = Paragraph.builder()
                .text(Text.from("世界你好"))  // 8 display cols
                .overflow(Overflow.WRAP_CHARACTER)
                .build();

        // Render in 5-wide area: first line "世界" (4 cols, 你 needs 2 more = 6 > 5)
        // Second line: "你好" (4 cols)
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 3));
        p.render(buffer.area(), buffer);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("世");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("界");
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("你");
        assertThat(buffer.get(2, 1).symbol()).isEqualTo("好");
    }

    @Test
    @DisplayName("Wrap word mode handles mixed ASCII and CJK")
    void wrapWordMixedContent() {
        Paragraph p = Paragraph.builder()
                .text(Text.from("Hello 世界"))  // 5 + 1 + 4 = 10 display cols
                .overflow(Overflow.WRAP_WORD)
                .build();

        // Render in 8-wide area: "Hello " fits (6 cols), then "世界" (4) doesn't fit on same line
        Buffer buffer = Buffer.empty(new Rect(0, 0, 8, 3));
        p.render(buffer.area(), buffer);

        // First line should have "Hello "
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
        // Second line should have "世界"
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("世");
    }

    @Test
    @DisplayName("Emoji in paragraph renders with correct width")
    void emojiInParagraph() {
        // 🔥 is U+1F525, width 2
        Paragraph p = Paragraph.builder()
                .text(Text.from("A\uD83D\uDD25B"))  // 1 + 2 + 1 = 4 display cols
                .overflow(Overflow.CLIP)
                .build();

        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        p.render(buffer.area(), buffer);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("\uD83D\uDD25");
        assertThat(buffer.get(2, 0).isContinuation()).isTrue();
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("B");
    }

    @Test
    @DisplayName("Clip does not break surrogate pairs")
    void clipDoesNotBreakSurrogatePairs() {
        // 🔥🎉 = 2 emoji, each width 2, total 4 cols
        Paragraph p = Paragraph.builder()
                .text(Text.from("\uD83D\uDD25\uD83C\uDF89"))
                .overflow(Overflow.CLIP)
                .build();

        // Width 3: only first emoji fits (width 2), second doesn't (would need 4 total)
        Buffer buffer = Buffer.empty(new Rect(0, 0, 3, 1));
        p.render(buffer.area(), buffer);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("\uD83D\uDD25");
        assertThat(buffer.get(1, 0).isContinuation()).isTrue();
        // Column 2 should not have a broken surrogate
        assertThat(buffer.get(2, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Wrap character with emoji wraps correctly")
    void wrapCharacterWithEmoji() {
        // 🔥🎉🚀 = 3 emoji, each width 2, total 6 cols
        Paragraph p = Paragraph.builder()
                .text(Text.from("\uD83D\uDD25\uD83C\uDF89\uD83D\uDE80"))
                .overflow(Overflow.WRAP_CHARACTER)
                .build();

        // Width 5: first line "🔥🎉" (4 cols, 🚀 needs 2 more = 6 > 5)
        // Second line: "🚀" (2 cols)
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 3));
        p.render(buffer.area(), buffer);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("\uD83D\uDD25");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("\uD83C\uDF89");
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("\uD83D\uDE80");
    }

    @Test
    @DisplayName("Span width accounts for CJK characters")
    void spanWidthWithCjk() {
        Span span = Span.raw("世界");
        assertThat(span.width()).isEqualTo(4);
    }

    @Test
    @DisplayName("Line width accounts for mixed content")
    void lineWidthMixed() {
        Line line = Line.from(Span.raw("Hi"), Span.raw("世界"));
        // "Hi" = 2, "世界" = 4, total = 6
        assertThat(line.width()).isEqualTo(6);
    }

    @Test
    @DisplayName("WRAP_WORD does not hang on a wide character wider than the line")
    void wrapWordWideCharWiderThanLine() {
        Paragraph p = Paragraph.builder()
                .text(Text.from("中文"))
                .overflow(Overflow.WRAP_WORD)
                .build();

        // each wide character takes a line of its own; it used to loop until out of memory
        Buffer buffer = Buffer.empty(new Rect(0, 0, 1, 4));
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> p.render(buffer.area(), buffer));
    }

    @Test
    @DisplayName("WRAP_WORD does not hang when the hanging indent leaves less room than a wide character")
    void wrapWordWideCharAfterHangingIndent() {
        Paragraph p = Paragraph.builder()
                .text(Text.from("   中文字"))
                .overflow(Overflow.WRAP_WORD)
                .build();

        Buffer buffer = Buffer.empty(new Rect(0, 0, 4, 6));
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> p.render(buffer.area(), buffer));
    }

    @Test
    @DisplayName("WRAP_CHARACTER keeps graphemes together at their display width")
    void wrapCharacterKeepsGraphemesWhole() {
        assertClusterStaysWhole("👩‍💻", Overflow.WRAP_CHARACTER);
        assertClusterStaysWhole("🇫🇷", Overflow.WRAP_CHARACTER);
    }

    @Test
    @DisplayName("WRAP_WORD keeps graphemes together at their display width")
    void wrapWordKeepsGraphemesWhole() {
        assertClusterStaysWhole("👩‍💻", Overflow.WRAP_WORD);
        assertClusterStaysWhole("🇫🇷", Overflow.WRAP_WORD);
    }

    private void assertClusterStaysWhole(String cluster, Overflow overflow) {
        Paragraph paragraph = Paragraph.builder()
                .text(Text.from(cluster + "X"))
                .overflow(overflow)
                .build();
        Buffer buffer = Buffer.empty(new Rect(0, 0, 2, 3));
        paragraph.render(buffer.area(), buffer);

        Buffer expected = Buffer.empty(buffer.area());
        expected.setString(0, 0, cluster, Style.EMPTY);
        expected.setString(0, 1, "X", Style.EMPTY);
        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    private static void assertRendersAs(Line line, Overflow overflow, int width, Span... expectedSpans) {
        Paragraph paragraph = Paragraph.builder()
                .text(Text.from(line))
                .overflow(overflow)
                .build();
        Rect area = new Rect(0, 0, width, 1);
        Buffer buffer = Buffer.empty(area);
        paragraph.render(area, buffer);

        Buffer expected = Buffer.empty(area);
        expected.setLine(0, 0, Line.from(expectedSpans));
        BufferAssertions.assertThat(buffer).as("%s in %d columns", overflow, width).isEqualTo(expected);
    }

    private String extractLineText(Buffer buffer, int y) {
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < buffer.area().width(); x++) {
            if (!buffer.get(x, y).isContinuation()) {
                String sym = buffer.get(x, y).symbol();
                if (!sym.equals(" ") || sb.length() > 0) {
                    sb.append(sym);
                }
            }
        }
        return sb.toString().trim();
    }
}

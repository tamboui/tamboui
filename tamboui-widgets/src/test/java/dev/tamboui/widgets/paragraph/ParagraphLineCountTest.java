/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.paragraph;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Padding;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Overflow;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.Borders;

import static org.assertj.core.api.Assertions.assertThat;

class ParagraphLineCountTest {

    private static final String FOX = "The quick brown fox jumps over the lazy dog";
    private static final String TECHNOLOGIST = "👩\u200D💻";
    private static final String FAMILY = "👨\u200D👩\u200D👧";

    @Test
    @DisplayName("with word wrap, a line counts the rows it wraps into")
    void wordWrap() {
        Paragraph paragraph = paragraph(FOX, Overflow.WRAP_WORD);

        assertThat(paragraph.lineCount(10)).isEqualTo(renderedRows(paragraph, 10)).isEqualTo(5);
        assertThat(paragraph.lineCount(43)).isEqualTo(renderedRows(paragraph, 43)).isEqualTo(1);
    }

    @Test
    @DisplayName("with character wrap, a line counts the rows it wraps into")
    void characterWrap() {
        Paragraph paragraph = paragraph("abcdefghijklmnopqrstuvwxyz", Overflow.WRAP_CHARACTER);

        assertThat(paragraph.lineCount(10)).isEqualTo(renderedRows(paragraph, 10)).isEqualTo(3);
    }

    @Test
    @DisplayName("rows continued under the indent of a wrapped line are counted")
    void hangingIndent() {
        // counted by words over the full width, this stack trace line takes 4 rows
        Paragraph words = paragraph(
            "    at org.apache.camel.processor.Pipeline.process(Pipeline.java:163)", Overflow.WRAP_WORD);
        Paragraph characters = paragraph("    abcdefghijklmnop", Overflow.WRAP_CHARACTER);

        assertThat(words.lineCount(24)).isEqualTo(renderedRows(words, 24)).isEqualTo(5);
        assertThat(characters.lineCount(10)).isEqualTo(renderedRows(characters, 10)).isEqualTo(3);
    }

    @Test
    @DisplayName("the breaks after -, / and \\ are counted")
    void breaksAfterPunctuation() {
        // counted by words, each of these takes one row less
        Paragraph dash = paragraph("send to order-processing-route now", Overflow.WRAP_WORD);
        Paragraph slash = paragraph("read file:/var/camel/inbox/orders", Overflow.WRAP_WORD);
        Paragraph backslash = paragraph("read C:\\camel\\data\\inbox\\orders", Overflow.WRAP_WORD);

        assertThat(dash.lineCount(13)).isEqualTo(renderedRows(dash, 13)).isEqualTo(4);
        assertThat(slash.lineCount(10)).isEqualTo(renderedRows(slash, 10)).isEqualTo(5);
        assertThat(backslash.lineCount(10)).isEqualTo(renderedRows(backslash, 10)).isEqualTo(5);
    }

    @Test
    @DisplayName("wide CJK characters are counted by their display width")
    void cjk() {
        Paragraph words = paragraph("世界 hello 你好 world", Overflow.WRAP_WORD);
        Paragraph characters = paragraph("日本語のテキストを折り返す", Overflow.WRAP_CHARACTER);

        assertThat(words.lineCount(6)).isEqualTo(renderedRows(words, 6)).isEqualTo(4);
        assertThat(characters.lineCount(7)).isEqualTo(renderedRows(characters, 7)).isEqualTo(5);
    }

    @Test
    @DisplayName("a ZWJ sequence is counted as one wide grapheme cluster")
    void zwjSequences() {
        Paragraph characters = paragraph(
            TECHNOLOGIST + FAMILY + TECHNOLOGIST + FAMILY + TECHNOLOGIST, Overflow.WRAP_CHARACTER);
        Paragraph words = paragraph(
            "team " + TECHNOLOGIST + TECHNOLOGIST + " and family " + FAMILY + FAMILY, Overflow.WRAP_WORD);

        assertThat(characters.lineCount(5)).isEqualTo(renderedRows(characters, 5)).isEqualTo(3);
        assertThat(words.lineCount(8)).isEqualTo(renderedRows(words, 8)).isEqualTo(5);
    }

    @Test
    @DisplayName("empty lines are counted as rows")
    void emptyLines() {
        Paragraph paragraph = paragraph("first line wraps here\n\nlast", Overflow.WRAP_WORD);

        assertThat(paragraph.lineCount(10)).isEqualTo(renderedRows(paragraph, 10)).isEqualTo(5);
    }

    @Test
    @DisplayName("without wrapping, each line takes one row")
    void noWrap() {
        String text = FOX + "\n" + FOX;

        assertThat(paragraph(text, Overflow.CLIP).lineCount(10)).isEqualTo(2);
        assertThat(paragraph(text, Overflow.ELLIPSIS).lineCount(10)).isEqualTo(2);
    }

    @Test
    @DisplayName("a block adds its rows and narrows the width the text wraps at")
    void block() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from(FOX))
            .overflow(Overflow.WRAP_WORD)
            .block(Block.builder().borders(Borders.ALL).title("Log").padding(Padding.uniform(1)).build())
            .build();

        int rows = paragraph.lineCount(14);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 14, rows));
        paragraph.render(buffer.area(), buffer);

        assertThat(rows).isEqualTo(9);
        assertThat(rows(buffer)).containsExactly(
            "┌Log─────────┐",
            "│            │",
            "│ The quick  │",
            "│ brown fox  │",
            "│ jumps      │",
            "│ over the   │",
            "│ lazy dog   │",
            "│            │",
            "└────────────┘");
    }

    @Test
    @DisplayName("a width that leaves no room for text counts only the block")
    void noRoomForText() {
        Paragraph bordered = Paragraph.builder().text(Text.from("abc")).block(Block.bordered()).build();

        assertThat(bordered.lineCount(2)).isEqualTo(2);
        assertThat(paragraph("abc", Overflow.WRAP_WORD).lineCount(0)).isZero();
    }

    private static Paragraph paragraph(String text, Overflow overflow) {
        return Paragraph.builder().text(Text.from(text)).overflow(overflow).build();
    }

    /** The rows a render into a tall area fills, up to the last row that is not blank. */
    private static int renderedRows(Paragraph paragraph, int width) {
        Buffer buffer = Buffer.empty(new Rect(0, 0, width, 50));
        paragraph.render(buffer.area(), buffer);
        List<String> rows = rows(buffer);
        int filled = 0;
        for (int y = 0; y < rows.size(); y++) {
            if (!rows.get(y).trim().isEmpty()) {
                filled = y + 1;
            }
        }
        return filled;
    }

    private static List<String> rows(Buffer buffer) {
        List<String> rows = new ArrayList<>();
        for (int y = 0; y < buffer.height(); y++) {
            StringBuilder sb = new StringBuilder();
            for (int x = 0; x < buffer.width(); x++) {
                sb.append(buffer.get(x, y).symbol());
            }
            rows.add(sb.toString());
        }
        return rows;
    }
}

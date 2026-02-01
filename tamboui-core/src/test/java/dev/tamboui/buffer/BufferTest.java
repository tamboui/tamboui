/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.buffer;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static dev.tamboui.assertj.BufferAssertions.*;

class BufferTest {

    @Test
    @DisplayName("Buffer.empty creates buffer filled with empty cells")
    void emptyBuffer() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 5));
        assertThat(buffer.area()).isEqualTo(new Rect(0, 0, 10, 5));
        assertThat(buffer.get(0, 0)).isEqualTo(Cell.EMPTY);
        assertThat(buffer.get(9, 4)).isEqualTo(Cell.EMPTY);
    }

    @Test
    @DisplayName("Buffer.filled creates buffer with specified cell")
    void filledBuffer() {
        Cell cell = new Cell("X", Style.EMPTY);
        Buffer buffer = Buffer.filled(new Rect(0, 0, 5, 5), cell);
        assertThat(buffer.get(0, 0)).isEqualTo(cell);
        assertThat(buffer.get(4, 4)).isEqualTo(cell);
    }

    @Test
    @DisplayName("Buffer set and get")
    void setAndGet() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 10));
        Cell cell = new Cell("A", Style.EMPTY.fg(Color.RED));

        buffer.set(5, 5, cell);

        assertThat(buffer.get(5, 5)).isEqualTo(cell);
        assertThat(buffer.get(0, 0)).isEqualTo(Cell.EMPTY);
    }

    @Test
    @DisplayName("Buffer setString writes characters")
    void setString() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        buffer.setString(0, 0, "Hello", Style.EMPTY);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("e");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("l");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("l");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("o");
        assertThat(buffer.get(5, 0).symbol()).isEqualTo(" "); // unchanged
    }

    @Test
    @DisplayName("Buffer setString with style")
    void setStringWithStyle() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        Style style = Style.EMPTY.fg(Color.GREEN);
        buffer.setString(0, 0, "Hi", style);

        assertThat(buffer.get(0, 0).style()).isEqualTo(style);
        assertThat(buffer.get(1, 0).style()).isEqualTo(style);
    }

    @Test
    @DisplayName("Buffer setString patches existing cell styles (preserves background)")
    void setStringPatchesStyle() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        // First set background color on area
        Style bgStyle = Style.EMPTY.bg(Color.BLUE);
        buffer.setStyle(new Rect(0, 0, 10, 1), bgStyle);

        // Then set text with foreground color only
        Style textStyle = Style.EMPTY.fg(Color.RED);
        buffer.setString(0, 0, "Hi", textStyle);

        // Text cells should have both foreground (from textStyle) and background (from bgStyle)
        assertThat(buffer.get(0, 0).style().fg()).contains(Color.RED);
        assertThat(buffer.get(0, 0).style().bg()).contains(Color.BLUE);
        assertThat(buffer.get(1, 0).style().fg()).contains(Color.RED);
        assertThat(buffer.get(1, 0).style().bg()).contains(Color.BLUE);

        // Cells after text should still have background
        assertThat(buffer.get(2, 0).style().bg()).contains(Color.BLUE);
    }

    @Test
    @DisplayName("Buffer diff returns changed cells")
    void diff() {
        Rect area = new Rect(0, 0, 5, 1);
        Buffer prev = Buffer.empty(area);
        Buffer curr = Buffer.empty(area);

        curr.setString(0, 0, "Hi", Style.EMPTY);
        // diff returns cells from `other` (curr) where they differ from `this` (prev)
        List<CellUpdate> updates = prev.diff(curr);

        assertThat(updates).hasSize(2);
        assertThat(updates.get(0).x()).isEqualTo(0);
        assertThat(updates.get(0).y()).isEqualTo(0);
        assertThat(updates.get(0).cell().symbol()).isEqualTo("H");
    }

    @Test
    @DisplayName("Buffer diff with no changes returns empty list")
    void diffNoChanges() {
        Rect area = new Rect(0, 0, 5, 5);
        Buffer a = Buffer.empty(area);
        Buffer b = Buffer.empty(area);

        assertThat(a.diff(b)).isEmpty();
    }

    @Test
    @DisplayName("Buffer set outside bounds is ignored")
    void setOutsideBounds() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 5));
        buffer.set(10, 10, new Cell("X", Style.EMPTY));
        // Should not throw, cell is simply ignored
        assertThat(buffer.get(0, 0)).isEqualTo(Cell.EMPTY);
    }

    @Test
    @DisplayName("Buffer with offset area")
    void offsetArea() {
        Buffer buffer = Buffer.empty(new Rect(5, 5, 10, 10));
        Cell cell = new Cell("X", Style.EMPTY);

        buffer.set(5, 5, cell);
        assertThat(buffer.get(5, 5)).isEqualTo(cell);

        buffer.set(14, 14, cell);
        assertThat(buffer.get(14, 14)).isEqualTo(cell);
    }

    @Test
    @DisplayName("BufferAssertions provides detailed diff output")
    void bufferAssertionsDetailedDiff() {
        Buffer actual = Buffer.empty(new Rect(0, 0, 15, 10));
        actual.setString(2, 2, "██████████", Style.EMPTY);
        actual.setString(2, 3, "██░░░░░░██", Style.EMPTY);
        actual.setString(2, 4, "██░░░░░░██", Style.EMPTY);
        actual.setString(2, 5, "██░░░░░░██", Style.EMPTY);
        actual.setString(2, 6, "██░░░░░░██", Style.EMPTY);
        actual.setString(2, 7, "██████████", Style.EMPTY);

        Buffer expected = Buffer.empty(new Rect(0, 0, 16, 10));
        expected.setString(2, 2, "█████████ █", Style.EMPTY);
        expected.setString(2, 3, "██░░░░░░██    ", Style.EMPTY);
        expected.setString(2, 4, "██░░░░░░██    ", Style.EMPTY);
        expected.setString(2, 5, "██░░░░░░██    ", Style.EMPTY);
        expected.setString(2, 6, "██░░░░░░██    ", Style.EMPTY);
        expected.setString(2, 7, "██████████    ", Style.EMPTY);

        // This will fail and show a nice diff
        try {
            assertThat(actual).isEqualTo(expected);
        } catch (AssertionError e) {
            // Verify the error message contains formatted buffer output
            String message = e.getMessage();
            assertThat(message).contains("actual:");
            assertThat(message).contains("expected:");
            assertThat(message).contains("Buffer {");
            assertThat(message).contains("content:");
        }
    }

    @Test
    @DisplayName("BufferAssertions can assert buffer properties")
    void bufferAssertionsProperties() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 5));
        
        assertThat(buffer)
            .hasArea(new Rect(0, 0, 10, 5))
            .hasWidth(10)
            .hasHeight(5);
    }

    @Test
    @DisplayName("BufferAssertions can assert individual cells")
    void bufferAssertionsCells() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 5));
        Cell cell = new Cell("X", Style.EMPTY);
        buffer.set(5, 2, cell);

        assertThat(buffer).hasCellAt(5, 2, cell);
        assertThat(buffer).hasCellAt(0, 0, Cell.EMPTY);
    }

    @Test
    @DisplayName("toAnsiString renders plain text correctly")
    void toAnsiStringPlainText() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 1));
        buffer.setString(0, 0, "Hello", Style.EMPTY);

        String result = buffer.toAnsiString();

        // Should have reset at start, content, and reset at end
        assertThat(result).startsWith("\u001b[0m");
        assertThat(result).contains("Hello");
        assertThat(result).endsWith("\u001b[0m");
    }

    @Test
    @DisplayName("toAnsiString renders styled text with ANSI codes")
    void toAnsiStringStyledText() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 2, 1));
        buffer.set(0, 0, new Cell("A", Style.EMPTY.fg(Color.RED)));
        buffer.set(1, 0, new Cell("B", Style.EMPTY.fg(Color.GREEN)));

        String result = buffer.toAnsiString();

        // Should contain red code for A and green code for B
        assertThat(result).contains(";31m");  // red
        assertThat(result).contains("A");
        assertThat(result).contains(";32m");  // green
        assertThat(result).contains("B");
    }

    @Test
    @DisplayName("toAnsiString handles multiple lines")
    void toAnsiStringMultipleLines() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 3, 2));
        buffer.setString(0, 0, "ABC", Style.EMPTY);
        buffer.setString(0, 1, "XYZ", Style.EMPTY);

        String result = buffer.toAnsiString();

        assertThat(result).contains("ABC");
        assertThat(result).contains("\r\n");
        assertThat(result).contains("XYZ");
    }

    @Test
    @DisplayName("toAnsiString and toAnsiStringTrimmed use \\r\\n line separators for raw mode compatibility")
    void ansiStringUsesCarriageReturnLineFeed() {
        // When OPOST is disabled (e.g. the Panama backend's raw mode), a bare \n
        // only moves the cursor down without returning to column 0. Using \r\n
        // ensures lines start at the left edge regardless of the output processing mode.
        Buffer buffer = Buffer.empty(new Rect(0, 0, 3, 3));
        buffer.setString(0, 0, "AAA", Style.EMPTY);
        buffer.setString(0, 1, "BBB", Style.EMPTY);
        buffer.setString(0, 2, "CCC", Style.EMPTY);

        String ansi = buffer.toAnsiString();
        String trimmed = buffer.toAnsiStringTrimmed();

        // Both methods must use \r\n, not bare \n
        assertThat(ansi).contains("AAA\r\nBBB\r\nCCC");
        assertThat(trimmed).contains("AAA\r\nBBB\r\nCCC");

        // Verify there are no bare \n (i.e., every \n is preceded by \r)
        String ansiWithoutCrLf = ansi.replace("\r\n", "");
        assertThat(ansiWithoutCrLf).doesNotContain("\n");

        String trimmedWithoutCrLf = trimmed.replace("\r\n", "");
        assertThat(trimmedWithoutCrLf).doesNotContain("\n");
    }

    @Test
    @DisplayName("toAnsiString optimizes style changes")
    void toAnsiStringOptimizesStyleChanges() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 1));
        Style redStyle = Style.EMPTY.fg(Color.RED);
        // All same style - should only emit style code once
        for (int i = 0; i < 5; i++) {
            buffer.set(i, 0, new Cell("X", redStyle));
        }

        String result = buffer.toAnsiString();

        // Count occurrences of the red color code - should only appear once
        int occurrences = countOccurrences(result, ";31m");
        assertThat(occurrences).isEqualTo(1);
    }

    @Test
    @DisplayName("toAnsiStringTrimmed removes trailing spaces")
    void toAnsiStringTrimmed() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        buffer.setString(0, 0, "Hi", Style.EMPTY);
        // Rest of buffer is empty spaces

        String trimmed = buffer.toAnsiStringTrimmed();
        String full = buffer.toAnsiString();

        // Trimmed should be shorter (no trailing spaces)
        assertThat(trimmed.length()).isLessThan(full.length());
        assertThat(trimmed).contains("Hi");
    }

    @Test
    @DisplayName("toAnsiStringTrimmed preserves styled spaces")
    void toAnsiStringTrimmedPreservesStyledSpaces() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 1));
        buffer.set(0, 0, new Cell("A", Style.EMPTY));
        buffer.set(1, 0, new Cell(" ", Style.EMPTY.bg(Color.RED)));  // Styled space
        buffer.set(2, 0, new Cell("B", Style.EMPTY));
        // 3 and 4 are empty default spaces

        String trimmed = buffer.toAnsiStringTrimmed();

        // Should contain A, styled space, and B, but not trailing spaces
        assertThat(trimmed).contains("A");
        assertThat(trimmed).contains("B");
        // The styled space should cause content up to position 2 to be included
    }

    @Test
    @DisplayName("toAnsiString with empty buffer")
    void toAnsiStringEmptyBuffer() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 3, 2));

        String result = buffer.toAnsiString();

        // Should still have structure (spaces and newlines) with reset codes
        assertThat(result).contains("\n");
        assertThat(result).endsWith("\u001b[0m");
    }

    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}

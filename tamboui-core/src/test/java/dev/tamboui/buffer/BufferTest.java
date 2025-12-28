/*
 * Copyright (c) 2025 TamboUI Contributors
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
}

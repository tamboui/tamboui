/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

import static org.assertj.core.api.Assertions.*;

class ClearTest {

    @Test
    @DisplayName("Clear resets cells to empty")
    void clearsToEmpty() {
        Rect area = new Rect(0, 0, 5, 3);
        Buffer buffer = Buffer.empty(area);

        // Fill buffer with content
        buffer.setString(0, 0, "Hello", Style.EMPTY);
        buffer.setString(0, 1, "World", Style.EMPTY);
        buffer.setString(0, 2, "Test!", Style.EMPTY);

        // Clear the area
        Clear.INSTANCE.render(area, buffer);

        // All cells should be empty
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 5; x++) {
                assertThat(buffer.get(x, y)).isEqualTo(Cell.EMPTY);
            }
        }
    }

    @Test
    @DisplayName("Clear resets styled cells")
    void clearsStyledCells() {
        Rect area = new Rect(0, 0, 5, 3);
        Buffer buffer = Buffer.empty(area);

        // Fill buffer with styled content
        Style style = Style.EMPTY.fg(Color.RED).bg(Color.BLUE);
        buffer.setString(0, 0, "Hello", style);

        // Clear the area
        Clear.clear().render(area, buffer);

        // Cells should have no style
        assertThat(buffer.get(0, 0).style()).isEqualTo(Style.EMPTY);
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Clear only affects specified area")
    void clearsOnlySpecifiedArea() {
        Rect fullArea = new Rect(0, 0, 10, 5);
        Rect clearArea = new Rect(2, 1, 3, 2);
        Buffer buffer = Buffer.empty(fullArea);

        // Fill entire buffer
        for (int y = 0; y < 5; y++) {
            buffer.setString(0, y, "XXXXXXXXXX", Style.EMPTY);
        }

        // Clear only a portion
        Clear.INSTANCE.render(clearArea, buffer);

        // Outside clear area should still have content
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("X");
        assertThat(buffer.get(1, 1).symbol()).isEqualTo("X");
        assertThat(buffer.get(5, 1).symbol()).isEqualTo("X");

        // Inside clear area should be empty
        assertThat(buffer.get(2, 1).symbol()).isEqualTo(" ");
        assertThat(buffer.get(3, 1).symbol()).isEqualTo(" ");
        assertThat(buffer.get(4, 1).symbol()).isEqualTo(" ");
        assertThat(buffer.get(2, 2).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Clear handles empty area")
    void handlesEmptyArea() {
        Rect area = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 5));

        // Should not throw
        Clear.INSTANCE.render(area, buffer);
    }

    @Test
    @DisplayName("Clear handles zero width")
    void handlesZeroWidth() {
        Rect area = new Rect(0, 0, 0, 5);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 5));

        // Fill buffer
        buffer.setString(0, 0, "Hello", Style.EMPTY);

        // Clear with zero width
        Clear.INSTANCE.render(area, buffer);

        // Content should remain
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
    }

    @Test
    @DisplayName("Clear handles zero height")
    void handlesZeroHeight() {
        Rect area = new Rect(0, 0, 5, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 5));

        // Fill buffer
        buffer.setString(0, 0, "Hello", Style.EMPTY);

        // Clear with zero height
        Clear.INSTANCE.render(area, buffer);

        // Content should remain
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
    }

    @Test
    @DisplayName("Singleton instance is reusable")
    void singletonIsReusable() {
        assertThat(Clear.INSTANCE).isSameAs(Clear.clear());
    }

    @Test
    @DisplayName("Clear works with offset area")
    void worksWithOffsetArea() {
        Rect fullArea = new Rect(0, 0, 10, 10);
        Rect clearArea = new Rect(5, 5, 3, 3);
        Buffer buffer = Buffer.empty(fullArea);

        // Fill buffer
        for (int y = 0; y < 10; y++) {
            buffer.setString(0, y, "XXXXXXXXXX", Style.EMPTY);
        }

        // Clear offset area
        Clear.INSTANCE.render(clearArea, buffer);

        // Check corners of clear area
        assertThat(buffer.get(5, 5).symbol()).isEqualTo(" ");
        assertThat(buffer.get(7, 5).symbol()).isEqualTo(" ");
        assertThat(buffer.get(5, 7).symbol()).isEqualTo(" ");
        assertThat(buffer.get(7, 7).symbol()).isEqualTo(" ");

        // Check outside clear area
        assertThat(buffer.get(4, 5).symbol()).isEqualTo("X");
        assertThat(buffer.get(8, 5).symbol()).isEqualTo("X");
    }
}

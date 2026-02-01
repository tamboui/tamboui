/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.flow;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.widget.Widget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link Flow} widget.
 */
class FlowTest {

    /**
     * Creates a simple widget that renders a single character at the top-left.
     */
    private static Widget charWidget(String ch) {
        return (area, buffer) -> {
            if (!area.isEmpty()) {
                buffer.setString(area.x(), area.y(), ch, Style.EMPTY);
            }
        };
    }

    /**
     * Creates a widget that fills its area with a repeating character.
     */
    private static Widget fillingWidget(String ch) {
        return (area, buffer) -> {
            for (int y = area.y(); y < area.bottom(); y++) {
                for (int x = area.x(); x < area.right(); x++) {
                    buffer.setString(x, y, ch, Style.EMPTY);
                }
            }
        };
    }

    @Test
    @DisplayName("items flow left-to-right")
    void itemsFlowLeftToRight() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);

        Flow.builder()
            .item(charWidget("A"), 5)
            .item(charWidget("B"), 5)
            .item(charWidget("C"), 5)
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(5, 0, "B")
            .hasSymbolAt(10, 0, "C");
    }

    @Test
    @DisplayName("items wrap to next line")
    void wrapping() {
        Rect area = new Rect(0, 0, 10, 2);
        Buffer buffer = Buffer.empty(area);

        // 3 items of width 4 in a 10-wide area: first 2 fit (4+4=8), 3rd wraps
        Flow.builder()
            .item(charWidget("A"), 4)
            .item(charWidget("B"), 4)
            .item(charWidget("C"), 4)
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(4, 0, "B")
            .hasSymbolAt(0, 1, "C");
    }

    @Test
    @DisplayName("horizontal spacing between items")
    void horizontalSpacing() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);

        Flow.builder()
            .item(charWidget("A"), 3)
            .item(charWidget("B"), 3)
            .horizontalSpacing(2)
            .build()
            .render(area, buffer);

        // A at x=0, B at x=3+2=5
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(5, 0, "B");
    }

    @Test
    @DisplayName("vertical spacing between rows")
    void verticalSpacing() {
        Rect area = new Rect(0, 0, 6, 4);
        Buffer buffer = Buffer.empty(area);

        // 3 items of width 4 in a 6-wide area: 1 per row
        Flow.builder()
            .item(charWidget("A"), 4)
            .item(charWidget("B"), 4)
            .verticalSpacing(1)
            .build()
            .render(area, buffer);

        // A at y=0, B at y=0+1+1=2
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(0, 2, "B");
    }

    @Test
    @DisplayName("row height from tallest item")
    void rowHeightFromTallest() {
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);

        // First row: item of height 1 and item of height 3
        // Second row starts at y=3
        Flow.builder()
            .item(charWidget("A"), 5, 1)
            .item(charWidget("B"), 5, 3)
            .item(charWidget("C"), 5, 1)
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(5, 0, "B")
            .hasSymbolAt(10, 0, "C");
    }

    @Test
    @DisplayName("area clipping stops rendering past bottom")
    void areaClipping() {
        Rect area = new Rect(0, 0, 5, 1);
        Buffer buffer = Buffer.empty(area);

        // Only room for first item (1 row high)
        Flow.builder()
            .item(charWidget("A"), 5)
            .item(charWidget("B"), 5)
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A");

        // B should not appear (no room on second row)
    }

    @Test
    @DisplayName("empty items does not render")
    void emptyItems() {
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        Flow.builder()
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer).isEqualTo(Buffer.empty(area));
    }

    @Test
    @DisplayName("empty area does not render")
    void emptyArea() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 5));

        Flow.builder()
            .item(charWidget("A"), 5)
            .build()
            .render(emptyArea, buffer);

        BufferAssertions.assertThat(buffer).isEqualTo(Buffer.empty(new Rect(0, 0, 10, 5)));
    }

    @Test
    @DisplayName("items from list")
    void itemsFromList() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);

        Flow.builder()
            .items(Arrays.asList(
                FlowItem.of(charWidget("A"), 5),
                FlowItem.of(charWidget("B"), 5)
            ))
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(5, 0, "B");
    }
}

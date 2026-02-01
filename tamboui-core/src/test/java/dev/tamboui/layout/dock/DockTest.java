/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.dock;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.widget.Widget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link Dock} widget.
 */
class DockTest {

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

    @Test
    @DisplayName("renders all 5 regions")
    void allFiveRegions() {
        Rect area = new Rect(0, 0, 30, 10);
        Buffer buffer = Buffer.empty(area);

        Dock.builder()
            .top(fillingWidget("T"))
            .bottom(fillingWidget("B"))
            .left(fillingWidget("L"))
            .right(fillingWidget("R"))
            .center(fillingWidget("C"))
            .topHeight(Constraint.length(2))
            .bottomHeight(Constraint.length(2))
            .leftWidth(Constraint.length(5))
            .rightWidth(Constraint.length(5))
            .build()
            .render(area, buffer);

        // Top region: y=0..1, full width
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "T")
            .hasSymbolAt(15, 1, "T");

        // Bottom region: y=8..9, full width
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 8, "B")
            .hasSymbolAt(15, 9, "B");

        // Left region: y=2..7, x=0..4
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 2, "L")
            .hasSymbolAt(4, 5, "L");

        // Right region: y=2..7, x=25..29
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(25, 2, "R")
            .hasSymbolAt(29, 5, "R");

        // Center region: y=2..7, x=5..24
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(5, 2, "C")
            .hasSymbolAt(20, 5, "C");
    }

    @Test
    @DisplayName("center only layout works")
    void centerOnly() {
        Rect area = new Rect(0, 0, 20, 10);
        Buffer buffer = Buffer.empty(area);

        Dock.builder()
            .center(fillingWidget("C"))
            .build()
            .render(area, buffer);

        // Center should fill entire area when no other regions
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "C")
            .hasSymbolAt(10, 5, "C")
            .hasSymbolAt(19, 9, "C");
    }

    @Test
    @DisplayName("top and center without sidebar")
    void topAndCenter() {
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);

        Dock.builder()
            .top(fillingWidget("T"))
            .center(fillingWidget("C"))
            .topHeight(Constraint.length(2))
            .build()
            .render(area, buffer);

        // Top: y=0..1
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "T")
            .hasSymbolAt(0, 1, "T");

        // Center: y=2..4
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 2, "C")
            .hasSymbolAt(0, 4, "C");
    }

    @Test
    @DisplayName("no header or footer renders left-center-right")
    void noHeaderOrFooter() {
        Rect area = new Rect(0, 0, 30, 5);
        Buffer buffer = Buffer.empty(area);

        Dock.builder()
            .left(fillingWidget("L"))
            .center(fillingWidget("C"))
            .right(fillingWidget("R"))
            .leftWidth(Constraint.length(5))
            .rightWidth(Constraint.length(5))
            .build()
            .render(area, buffer);

        // Left: x=0..4
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "L")
            .hasSymbolAt(4, 2, "L");

        // Center: x=5..24
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(5, 0, "C")
            .hasSymbolAt(20, 2, "C");

        // Right: x=25..29
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(25, 0, "R")
            .hasSymbolAt(29, 2, "R");
    }

    @Test
    @DisplayName("empty area does not render")
    void emptyArea() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 5));

        Dock.builder()
            .center(charWidget("A"))
            .build()
            .render(emptyArea, buffer);

        BufferAssertions.assertThat(buffer).isEqualTo(Buffer.empty(new Rect(0, 0, 10, 5)));
    }

    @Test
    @DisplayName("no regions does not render")
    void noRegions() {
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        Dock.builder()
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer).isEqualTo(Buffer.empty(area));
    }

    @Test
    @DisplayName("custom constraints change region sizes")
    void customConstraints() {
        Rect area = new Rect(0, 0, 20, 10);
        Buffer buffer = Buffer.empty(area);

        Dock.builder()
            .top(charWidget("T"))
            .center(charWidget("C"))
            .topHeight(Constraint.length(3))
            .build()
            .render(area, buffer);

        // Top at y=0
        BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "T");
        // Center at y=3 (after 3-row top)
        BufferAssertions.assertThat(buffer).hasSymbolAt(0, 3, "C");
    }
}

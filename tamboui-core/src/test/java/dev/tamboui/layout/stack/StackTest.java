/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.stack;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.ContentAlignment;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.widget.Widget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link Stack} widget.
 */
class StackTest {

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
    @DisplayName("last child overlaps earlier children")
    void lastChildOverlaps() {
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        Stack.builder()
            .children(fillingWidget("A"), fillingWidget("B"))
            .build()
            .render(area, buffer);

        // B should overwrite A everywhere
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "B")
            .hasSymbolAt(5, 2, "B")
            .hasSymbolAt(9, 4, "B");
    }

    @Test
    @DisplayName("single child renders correctly")
    void singleChild() {
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        Stack.builder()
            .children(charWidget("X"))
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "X");
    }

    @Test
    @DisplayName("empty children does not render")
    void emptyChildren() {
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        Stack.builder()
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer).isEqualTo(Buffer.empty(area));
    }

    @Test
    @DisplayName("empty area does not render")
    void emptyArea() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 5));

        Stack.builder()
            .children(charWidget("A"))
            .build()
            .render(emptyArea, buffer);

        BufferAssertions.assertThat(buffer).isEqualTo(Buffer.empty(new Rect(0, 0, 10, 5)));
    }

    @Test
    @DisplayName("STRETCH mode fills entire area")
    void stretchMode() {
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        Stack.builder()
            .children(fillingWidget("S"))
            .alignment(ContentAlignment.STRETCH)
            .build()
            .render(area, buffer);

        // All cells should be "S"
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "S")
            .hasSymbolAt(9, 4, "S");
    }

    @Test
    @DisplayName("default alignment is STRETCH")
    void defaultAlignmentIsStretch() {
        Stack stack = Stack.builder()
            .children(charWidget("A"))
            .build();

        assertThat(stack.alignment()).isEqualTo(ContentAlignment.STRETCH);
    }

    @Test
    @DisplayName("children from list")
    void childrenFromList() {
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        java.util.List<Widget> widgets = java.util.Arrays.asList(
            fillingWidget("A"), fillingWidget("B")
        );

        Stack.builder()
            .children(widgets)
            .build()
            .render(area, buffer);

        // B should overwrite A
        BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "B");
    }
}

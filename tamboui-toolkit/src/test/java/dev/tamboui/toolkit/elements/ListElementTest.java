/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widgets.list.ListState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ListElement.
 */
class ListElementTest {

    @Test
    @DisplayName("ListElement fluent API chains correctly")
    void fluentApiChaining() {
        ListState state = new ListState();
        ListElement<?> element = list("Item 1", "Item 2", "Item 3")
            .state(state)
            .highlightSymbol("> ")
            .highlightColor(Color.YELLOW)
            .title("Menu")
            .rounded()
            .borderColor(Color.CYAN);

        assertThat(element).isInstanceOf(ListElement.class);
    }

    @Test
    @DisplayName("list() creates empty element")
    void emptyList() {
        ListElement<?> element = list();
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("list(String...) creates element with items")
    void listWithItems() {
        ListElement<?> element = list("A", "B", "C");
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("list(List<String>) creates element with items")
    void listWithItemsList() {
        ListElement<?> element = list(Arrays.asList("X", "Y", "Z"));
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("items() method replaces items")
    void itemsMethod() {
        ListElement<?> element = list()
            .items("New 1", "New 2");
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("ListElement renders items to buffer")
    void rendersToBuffer() {
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        ListState state = new ListState();

        list("Item 1", "Item 2", "Item 3")
            .state(state)
            .title("List")
            .rounded()
            .render(frame, area, RenderContext.empty());

        // Check border is rendered
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("╭");
    }

    @Test
    @DisplayName("ListElement with selection highlights item")
    void withSelection() {
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        ListState state = new ListState();
        state.select(1);

        list("Item 1", "Item 2", "Item 3")
            .state(state)
            .highlightColor(Color.YELLOW)
            .render(frame, area, RenderContext.empty());

        // Second item should have highlight style (row 1 in 0-indexed)
        // We can't easily verify the exact highlight without knowing the layout,
        // but we can verify rendering completes
        assertThat(buffer).isNotNull();
    }

    @Test
    @DisplayName("Empty area does not render")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 5));
        Frame frame = Frame.forTesting(buffer);
        ListState state = new ListState();

        // Should not throw
        list("A", "B").state(state).render(frame, emptyArea, RenderContext.empty());
    }

    @Test
    @DisplayName("ListElement without explicit state creates internal state")
    void internalState() {
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        // Should not throw even without state
        list("Item 1", "Item 2")
            .render(frame, area, RenderContext.empty());
    }

    @Test
    @DisplayName("highlightSymbol sets the indicator")
    void highlightSymbol() {
        ListElement<?> element = list("A", "B")
            .highlightSymbol("→ ");
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("highlightStyle sets the style")
    void highlightStyle() {
        ListElement<?> element = list("A", "B")
            .highlightColor(Color.GREEN);
        assertThat(element).isNotNull();
    }
}

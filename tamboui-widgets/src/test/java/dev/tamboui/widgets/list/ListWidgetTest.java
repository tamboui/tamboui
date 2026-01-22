/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.list;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.text.Overflow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static dev.tamboui.assertj.BufferAssertions.assertThat;

class ListWidgetTest {

    @Test
    @DisplayName("ListWidget renders items")
    void rendersItems() {
        List<ListItem> items = Arrays.asList(
            ListItem.from("Item 1"),
            ListItem.from("Item 2"),
            ListItem.from("Item 3")
        );
        ListWidget list = ListWidget.builder()
            .items(items)
            .highlightSymbol("") // No highlight symbol for simple rendering
            .build();
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();

        list.render(area, buffer, state);

        Buffer expected = Buffer.empty(new Rect(0, 0, 20, 3));
        expected.setString(0, 0, "Item 1", Style.EMPTY);
        expected.setString(0, 1, "Item 2", Style.EMPTY);
        expected.setString(0, 2, "Item 3", Style.EMPTY);

        assertThat(buffer).isEqualTo(expected);

        assertThat(buffer)
            .hasCellAt(0, 0, new Cell("I", Style.EMPTY))
            .hasCellAt(0, 1, new Cell("I", Style.EMPTY))
            .hasCellAt(0, 2, new Cell("I", Style.EMPTY));
    }

    @Test
    @DisplayName("ListWidget with selection")
    void withSelection() {
        List<ListItem> items = Arrays.asList(
            ListItem.from("Item 1"),
            ListItem.from("Item 2")
        );
        Style highlightStyle = Style.EMPTY.fg(Color.YELLOW);
        ListWidget list = ListWidget.builder()
            .items(items)
            .highlightStyle(highlightStyle)
            .build();
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();
        state.select(1);

        list.render(area, buffer, state);

        // Second item should have highlight style (content starts after default symbol ">> ")
        assertThat(buffer.get(3, 1).style().fg()).contains(Color.YELLOW);
    }

    @Test
    @DisplayName("ListWidget with highlight symbol")
    void withHighlightSymbol() {
        List<ListItem> items = Arrays.asList(
            ListItem.from("Item 1"),
            ListItem.from("Item 2")
        );
        ListWidget list = ListWidget.builder()
            .items(items)
            .highlightSymbol("> ")
            .build();
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();
        state.select(0);

        list.render(area, buffer, state);

        // Selected item has reversed style (default highlightStyle)
        Style highlightStyle = Style.EMPTY.reversed();
        assertThat(buffer)
            .hasCellAt(0, 0, new Cell(">", Style.EMPTY))
            .hasCellAt(1, 0, new Cell(" ", Style.EMPTY))
            .hasCellAt(2, 0, new Cell("I", highlightStyle));
    }

    @Test
    @DisplayName("ListState selection navigation")
    void stateNavigation() {
        ListState state = new ListState();

        assertThat(state.selected()).isNull();

        state.select(0);
        assertThat(state.selected()).isEqualTo(0);

        state.selectNext(5);
        assertThat(state.selected()).isEqualTo(1);

        state.selectPrevious();
        assertThat(state.selected()).isEqualTo(0);

        // Should not go below 0
        state.selectPrevious();
        assertThat(state.selected()).isEqualTo(0);
    }

    @Test
    @DisplayName("ListState selectNext wraps or stops at end")
    void selectNextAtEnd() {
        ListState state = new ListState();
        state.select(4);

        state.selectNext(5);
        assertThat(state.selected()).isEqualTo(4); // stays at end
    }

    @Test
    @DisplayName("ListWidget with bottom-to-top direction")
    void withBottomToTopDirection() {
        List<ListItem> items = Arrays.asList(
            ListItem.from("Item 1"),
            ListItem.from("Item 2"),
            ListItem.from("Item 3")
        );
        ListWidget list = ListWidget.builder()
            .items(items)
            .direction(ListDirection.BOTTOM_TO_TOP)
            .highlightSymbol("") // No symbol for simple test
            .build();
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();
        state.select(0); // Select first item (should appear at bottom)

        list.render(area, buffer, state);

        // In bottom-to-top, first item should be at bottom (selected, so has reversed style)
        Style highlightStyle = Style.EMPTY.reversed();
        assertThat(buffer)
            .hasCellAt(0, 2, new Cell("I", highlightStyle)) // Item 1 at bottom (selected)
            .hasCellAt(0, 1, new Cell("I", Style.EMPTY)) // Item 2 in middle
            .hasCellAt(0, 0, new Cell("I", Style.EMPTY)); // Item 3 at top
    }

    @Test
    @DisplayName("ListWidget with repeat highlight symbol on multiline items")
    void withRepeatHighlightSymbol() {
        List<ListItem> items = Arrays.asList(
            ListItem.from(Text.from(
                Line.from("Line 1"),
                Line.from("Line 2"),
                Line.from("Line 3")
            ))
        );
        ListWidget list = ListWidget.builder()
            .items(items)
            .highlightSymbol("> ")
            .repeatHighlightSymbol(true)
            .build();
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();
        state.select(0);

        list.render(area, buffer, state);

        // All three lines should have the highlight symbol (symbol uses default style, not highlightStyle)
        dev.tamboui.assertj.BufferAssertions.assertThat(buffer)
            .hasCellAt(0, 0, new Cell(">", Style.EMPTY))
            .hasCellAt(0, 1, new Cell(">", Style.EMPTY))
            .hasCellAt(0, 2, new Cell(">", Style.EMPTY));
    }

    @Test
    @DisplayName("ListWidget without repeat highlight symbol shows only on first line")
    void withoutRepeatHighlightSymbol() {
        List<ListItem> items = Arrays.asList(
            ListItem.from(Text.from(
                Line.from("Line 1"),
                Line.from("Line 2"),
                Line.from("Line 3")
            ))
        );
        ListWidget list = ListWidget.builder()
            .items(items)
            .highlightSymbol("> ")
            .repeatHighlightSymbol(false) // Default
            .build();
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();
        state.select(0);

        list.render(area, buffer, state);

        // Only first line should have the highlight symbol (symbol uses default style)
        dev.tamboui.assertj.BufferAssertions.assertThat(buffer)
            .hasCellAt(0, 0, new Cell(">", Style.EMPTY));
        // Second and third lines should not have the symbol but should be indented (content starts after symbol space)
        // Content has reversed style (default highlightStyle)
        Style highlightStyle = Style.EMPTY.reversed();
        dev.tamboui.assertj.BufferAssertions.assertThat(buffer)
            .hasCellAt(2, 1, new Cell("L", highlightStyle)) // Line 2 content (indented, selected)
            .hasCellAt(2, 2, new Cell("L", highlightStyle)); // Line 3 content (indented, selected)
    }

    // ========== Overflow Mode Tests ==========

    @Test
    @DisplayName("CLIP overflow truncates text at boundary")
    void clipOverflow() {
        List<ListItem> items = Arrays.asList(
            ListItem.from("Hello World This Is Long")
        );
        ListWidget list = ListWidget.builder()
            .items(items)
            .overflow(Overflow.CLIP)
            .highlightSymbol("")
            .build();
        Rect area = new Rect(0, 0, 5, 1);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();

        list.render(area, buffer, state);

        // Should show "Hello" (5 chars)
        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Hello", Style.EMPTY);
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("ELLIPSIS overflow truncates with ellipsis at end")
    void ellipsisOverflow() {
        List<ListItem> items = Arrays.asList(
            ListItem.from("Hello World")
        );
        ListWidget list = ListWidget.builder()
            .items(items)
            .overflow(Overflow.ELLIPSIS)
            .highlightSymbol("")
            .build();
        Rect area = new Rect(0, 0, 8, 1);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();

        list.render(area, buffer, state);

        // Should show "Hello..." (5 chars + 3 dots = 8)
        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Hello...", Style.EMPTY);
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("ELLIPSIS_START overflow truncates with ellipsis at start")
    void ellipsisStartOverflow() {
        List<ListItem> items = Arrays.asList(
            ListItem.from("Hello World")
        );
        ListWidget list = ListWidget.builder()
            .items(items)
            .overflow(Overflow.ELLIPSIS_START)
            .highlightSymbol("")
            .build();
        Rect area = new Rect(0, 0, 8, 1);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();

        list.render(area, buffer, state);

        // Should show "...World" (3 dots + 5 chars = 8)
        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "...World", Style.EMPTY);
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("ELLIPSIS_MIDDLE overflow truncates with ellipsis in middle")
    void ellipsisMiddleOverflow() {
        List<ListItem> items = Arrays.asList(
            ListItem.from("Hello World")
        );
        ListWidget list = ListWidget.builder()
            .items(items)
            .overflow(Overflow.ELLIPSIS_MIDDLE)
            .highlightSymbol("")
            .build();
        Rect area = new Rect(0, 0, 8, 1);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();

        list.render(area, buffer, state);

        // Should show "Hel...ld" (3 left + 3 dots + 2 right = 8)
        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Hel...ld", Style.EMPTY);
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Overflow does not modify text that fits")
    void overflowTextFits() {
        List<ListItem> items = Arrays.asList(
            ListItem.from("Hi")
        );
        ListWidget list = ListWidget.builder()
            .items(items)
            .overflow(Overflow.ELLIPSIS)
            .highlightSymbol("")
            .build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();

        list.render(area, buffer, state);

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Hi", Style.EMPTY);
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Overflow works with highlight symbol")
    void overflowWithHighlightSymbol() {
        List<ListItem> items = Arrays.asList(
            ListItem.from("Hello World This Is Long")
        );
        ListWidget list = ListWidget.builder()
            .items(items)
            .overflow(Overflow.ELLIPSIS)
            .highlightSymbol("> ")
            .build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();
        state.select(0);

        list.render(area, buffer, state);

        // Symbol takes 2 chars, leaving 8 for content
        // Should show "> Hello..." (> + space + Hello + ...)
        // Note: The highlight style is only applied to the content area, not the symbol area
        // (see ListWidget line 237-238: "Only fill the content area, not the symbol area, so symbol keeps its own style")
        Style highlightStyle = Style.EMPTY.reversed();
        Buffer expected = Buffer.empty(area);
        // Only content area (x=2 onwards) gets highlight style
        expected.setStyle(new Rect(2, 0, 8, 1), highlightStyle);
        expected.setString(0, 0, "> ", Style.EMPTY);
        expected.setString(2, 0, "Hello...", highlightStyle);
        assertThat(buffer).isEqualTo(expected);
    }
}

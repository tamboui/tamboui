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
}

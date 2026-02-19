/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.list;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.common.SizedWidget;
import dev.tamboui.widgets.paragraph.Paragraph;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

class ListWidgetTest {

    @Test
    @DisplayName("ListWidget renders items")
    void rendersItems() {
        ListWidget list = ListWidget.builder()
            .items("Item 1", "Item 2", "Item 3")
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
    }

    @Test
    @DisplayName("ListWidget with selection applies full highlight style to selected row")
    void withSelection() {
        Style highlightStyle = Style.EMPTY.bg(Color.BLUE).bold();
        ListWidget list = ListWidget.builder()
            .items("Item 1", "Item 2")
            .highlightStyle(highlightStyle)
            .highlightSymbol("> ")
            .build();
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();
        state.select(1);

        list.render(area, buffer, state);

        Style bgBlueBold = Style.EMPTY.bg(Color.BLUE).bold();
        Buffer expected = Buffer.empty(area);
        // Row 0: unselected, content starts after symbol area (2 chars)
        expected.setString(2, 0, "Item 1", Style.EMPTY);
        // Row 1: selected — symbol with highlight, content rendered, then full highlight overlay
        expected.setString(0, 1, "> ", bgBlueBold);
        expected.setString(2, 1, "Item 2", Style.EMPTY);
        expected.setStyle(new Rect(0, 1, 20, 1), bgBlueBold); // full highlight on entire selected row

        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("ListWidget with highlight symbol applies full highlight to selected row")
    void withHighlightSymbol() {
        ListWidget list = ListWidget.builder()
            .items("Item 1", "Item 2")
            .highlightSymbol("> ")
            .build();
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();
        state.select(0);

        list.render(area, buffer, state);

        // Default highlightStyle is reversed() — applied to symbol AND as overlay on entire row
        Style reversed = Style.EMPTY.reversed();
        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "> ", reversed); // symbol with default highlight (reversed)
        expected.setString(2, 0, "Item 1", Style.EMPTY); // content rendered first
        expected.setStyle(new Rect(0, 0, 20, 1), reversed); // full highlight overlay on selected row
        expected.setString(2, 1, "Item 2", Style.EMPTY); // second item, no symbol, no highlight

        assertThat(buffer).isEqualTo(expected);
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
    @DisplayName("ListState selectNext stops at end")
    void selectNextAtEnd() {
        ListState state = new ListState();
        state.select(4);

        state.selectNext(5);
        assertThat(state.selected()).isEqualTo(4); // stays at end
    }

    @Test
    @DisplayName("ListWidget auto-scrolls to keep selected item visible")
    void autoScrollKeepsSelectedVisible() {
        ListWidget list = ListWidget.builder()
            .items("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")
            .highlightSymbol("")
            .scrollMode(ScrollMode.AUTO_SCROLL)
            .build();
        // Only 2 rows visible
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();
        state.select(3); // Select 4th item (index 3)

        list.render(area, buffer, state);

        // With 5 items of height 1 and viewport of 2, selecting index 3:
        // selectedTop=3, selectedBottom=4, offset adjusted to 4-2=2
        // Visible: Item 3 (index 2) at row 0, Item 4 (index 3, selected) at row 1
        // Default highlight (reversed) applied to selected row
        Style reversed = Style.EMPTY.reversed();
        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Item 3", Style.EMPTY);
        expected.setString(0, 1, "Item 4", Style.EMPTY);
        expected.setStyle(new Rect(0, 1, 20, 1), reversed); // highlight on selected row

        assertThat(buffer).isEqualTo(expected);
        assertThat(state.offset()).isEqualTo(2);
    }

    @Test
    @DisplayName("ListWidget renders arbitrary SizedWidget items")
    void withSizedWidgetItems() {
        SizedWidget item = SizedWidget.of(Paragraph.from("Custom widget"));
        ListWidget list = ListWidget.builder()
            .items(Arrays.asList(item))
            .highlightSymbol("")
            .build();
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();

        list.render(area, buffer, state);

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Custom widget", Style.EMPTY);

        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("ListState scrollBy adjusts offset")
    void scrollByAdjustsOffset() {
        ListState state = new ListState();
        assertThat(state.offset()).isEqualTo(0);

        state.scrollBy(5);
        assertThat(state.offset()).isEqualTo(5);

        state.scrollBy(-3);
        assertThat(state.offset()).isEqualTo(2);

        // Should not go below 0
        state.scrollBy(-10);
        assertThat(state.offset()).isEqualTo(0);
    }

    @Test
    @DisplayName("ListState sticky scroll tracking")
    void stickyScrollTracking() {
        ListState state = new ListState();
        assertThat(state.isUserScrolledAway()).isFalse();

        state.markUserScrolledAway();
        assertThat(state.isUserScrolledAway()).isTrue();

        state.resumeAutoScroll();
        assertThat(state.isUserScrolledAway()).isFalse();
    }

    @Test
    @DisplayName("Empty list renders nothing")
    void emptyListRendersNothing() {
        ListWidget list = ListWidget.builder()
            .highlightSymbol("")
            .build();
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);
        Buffer expected = Buffer.empty(area);
        ListState state = new ListState();

        list.render(area, buffer, state);

        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("ListState selectFirst resets offset")
    void selectFirstResetsOffset() {
        ListState state = new ListState();
        state.select(5);
        state.setOffset(10);

        state.selectFirst();

        assertThat(state.selected()).isEqualTo(0);
        assertThat(state.offset()).isEqualTo(0);
    }

    @Test
    @DisplayName("ListState selectLast sets to last item")
    void selectLastSetsToLastItem() {
        ListState state = new ListState();
        state.select(0);

        state.selectLast(10);

        assertThat(state.selected()).isEqualTo(9);
    }
}

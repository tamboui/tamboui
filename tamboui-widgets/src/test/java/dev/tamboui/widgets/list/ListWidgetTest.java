/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.list;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.common.ScrollBarPolicy;
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
    @DisplayName("ListWidget scrolls to selected item by default when selection exceeds viewport")
    void defaultScrollModeScrollsToSelected() {
        ListWidget list = ListWidget.builder()
            .items("Item 1", "Item 2", "Item 3", "Item 4", "Item 5")
            .highlightSymbol("")
            .build(); // no explicit scrollMode — default must scroll to selection
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();
        state.select(4); // Select last item, well beyond the 2-row viewport

        list.render(area, buffer, state);

        // offset adjusted so items 4 and 5 (indices 3 and 4) are visible
        Style reversed = Style.EMPTY.reversed();
        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Item 4", Style.EMPTY);
        expected.setString(0, 1, "Item 5", Style.EMPTY);
        expected.setStyle(new Rect(0, 1, 20, 1), reversed);

        assertThat(buffer).isEqualTo(expected);
        assertThat(state.offset()).isEqualTo(3);
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
    @DisplayName("A selected blank item keeps its row and gets the highlight")
    void selectedBlankItemKeepsItsRow() {
        ListWidget list = ListWidget.builder()
            .items("a", "", "b")
            .highlightSymbol("> ")
            .build();
        Rect area = new Rect(0, 0, 10, 3);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();
        state.select(1);

        list.render(area, buffer, state);

        Style reversed = Style.EMPTY.reversed();
        Buffer expected = Buffer.empty(area);
        expected.setString(2, 0, "a", Style.EMPTY);
        expected.setString(0, 1, "> ", reversed);
        expected.setStyle(new Rect(0, 1, 10, 1), reversed);
        expected.setString(2, 2, "b", Style.EMPTY);

        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Blank spacer items keep their rows")
    void blankSpacerItemsKeepTheirRows() {
        ListWidget list = ListWidget.builder()
            .items("Fruit", "", "Apple", "", "Banana")
            .highlightSymbol("")
            .build();
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        list.render(area, buffer, new ListState());

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Fruit", Style.EMPTY);
        expected.setString(0, 2, "Apple", Style.EMPTY);
        expected.setString(0, 4, "Banana", Style.EMPTY);

        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("ListState counts a blank item as a row when scrolling to the selection")
    void scrollToSelectedCountsBlankItem() {
        List<ListItem> items = Arrays.asList(ListItem.from("a"), ListItem.from(""), ListItem.from("b"));
        ListState state = new ListState();
        state.select(2);

        state.scrollToSelected(2, items);

        assertThat(state.offset()).isEqualTo(1);
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

    @Test
    @DisplayName("AS_NEEDED scrollbar does not cover multi-line items that fit by count")
    void asNeededScrollbarDoesNotCoverMultiLineItems() {
        SizedWidget item = SizedWidget.ofHeight(
            Paragraph.builder().text(Text.from("abc\ndef\nghi")).right().build(), 3);
        ListWidget list = ListWidget.builder()
            .items(Arrays.asList(item, item, item))
            .highlightSymbol("")
            .scrollBarPolicy(ScrollBarPolicy.AS_NEEDED)
            .build();
        // 3 items for 5 rows, but 9 lines in total
        Rect area = new Rect(0, 0, 8, 5);
        Buffer buffer = Buffer.empty(area);

        list.render(area, buffer, new ListState());

        // The right-aligned text ends in column 6, next to the scrollbar in column 7
        Buffer expected = Buffer.empty(area);
        expected.setString(4, 0, "abc", Style.EMPTY);
        expected.setString(4, 1, "def", Style.EMPTY);
        expected.setString(4, 2, "ghi", Style.EMPTY);
        expected.setString(4, 3, "abc", Style.EMPTY);
        expected.setString(4, 4, "def", Style.EMPTY);
        for (int y = 0; y < 5; y++) {
            expected.setString(7, y, y < 3 ? "█" : "│", Style.EMPTY);
        }

        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("ListWidget renders horizontally scrolled content")
    void horizontalScrollRendersOffsetContent() {
        ListWidget list = ListWidget.builder()
            .items("0123456789", "abcdefghij")
            .highlightSymbol("")
            .build();
        Rect area = new Rect(0, 0, 5, 2);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();
        state.setHorizontalOffset(3);

        list.render(area, buffer, state);

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "34567", Style.EMPTY);
        expected.setString(0, 1, "defgh", Style.EMPTY);

        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("ListWidget horizontal scroll preserves highlight symbol at left edge")
    void horizontalScrollPreservesHighlightSymbol() {
        Style highlightStyle = Style.EMPTY.reversed();
        ListWidget list = ListWidget.builder()
            .items("0123456789", "abcdefghij")
            .highlightSymbol("> ")
            .highlightStyle(highlightStyle)
            .build();
        Rect area = new Rect(0, 0, 7, 2);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();
        state.select(0);
        state.setHorizontalOffset(3);

        list.render(area, buffer, state);

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "> ", highlightStyle);
        expected.setString(2, 0, "34567", Style.EMPTY);
        expected.setStyle(new Rect(0, 0, 7, 1), highlightStyle);
        expected.setString(2, 1, "defgh", Style.EMPTY);

        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("ListWidget horizontal scroll handles wide characters cleanly at boundaries")
    void horizontalScrollWideCharacters() {
        // "A" (width 1, col 0) + "漢" (width 2, col 1-2) + "B" (width 1, col 3)
        ListWidget list = ListWidget.builder()
            .items("A漢B")
            .highlightSymbol("")
            .build();
        Rect area = new Rect(0, 0, 2, 1);
        Buffer buffer = Buffer.empty(area);
        ListState state = new ListState();

        // Offset 2 cuts through "漢" (continuation is at col 2, B is at col 3)
        // Col 0 in viewport should become space instead of an orphaned continuation cell
        state.setHorizontalOffset(2);
        list.render(area, buffer, state);

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, " ", Style.EMPTY);
        expected.setString(1, 0, "B", Style.EMPTY);
        assertThat(buffer).isEqualTo(expected);

        // Offset 0 with width 2: col 0 has "A", col 1 would have "漢" which needs 2 cols
        // Since it cannot fit in remaining 1 col, it should be replaced with space
        buffer = Buffer.empty(area);
        state.setHorizontalOffset(0);
        // Using horizontal offset > 0 triggers the column slicing path:
        // Text "XA漢B", offset 1: col 0 has "A", col 1 has "漢" which overflows right edge of 2-col viewport
        ListWidget listOverflow = ListWidget.builder()
            .items("XA漢B")
            .highlightSymbol("")
            .build();
        state.setHorizontalOffset(1);
        listOverflow.render(area, buffer, state);

        Buffer expectedOverflow = Buffer.empty(area);
        expectedOverflow.setString(0, 0, "A", Style.EMPTY);
        expectedOverflow.setString(1, 0, " ", Style.EMPTY);
        assertThat(buffer).isEqualTo(expectedOverflow);
    }

    @Test
    @DisplayName("ListState tracks and adjusts horizontal scroll offset")
    void listStateHorizontalScrollTracking() {
        ListState state = new ListState();
        assertThat(state.horizontalOffset()).isEqualTo(0);

        state.setHorizontalOffset(10);
        assertThat(state.horizontalOffset()).isEqualTo(10);

        state.setHorizontalOffset(-5);
        assertThat(state.horizontalOffset()).isEqualTo(0);

        state.scrollHorizontalBy(5);
        assertThat(state.horizontalOffset()).isEqualTo(5);

        state.scrollHorizontalBy(-2);
        assertThat(state.horizontalOffset()).isEqualTo(3);

        state.scrollHorizontalBy(-10);
        assertThat(state.horizontalOffset()).isEqualTo(0);
    }
}

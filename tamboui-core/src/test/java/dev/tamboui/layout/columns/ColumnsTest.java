/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.columns;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.widget.Widget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the {@link Columns} widget.
 */
class ColumnsTest {

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
    @DisplayName("renders children in a single row")
    void singleRow() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);

        Columns.builder()
            .children(charWidget("A"), charWidget("B"))
            .columnCount(2)
            .build()
            .render(area, buffer);

        // 2 columns in 20 wide = 10 per column
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B");
    }

    @Test
    @DisplayName("renders children in multiple rows")
    void multipleRows() {
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);

        Columns.builder()
            .children(charWidget("A"), charWidget("B"), charWidget("C"), charWidget("D"))
            .columnCount(2)
            .build()
            .render(area, buffer);

        // 2 cols of 10, 2 rows of 1
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(0, 1, "C")
            .hasSymbolAt(10, 1, "D");
    }

    @Test
    @DisplayName("spacing creates gaps between columns")
    void spacingBetweenColumns() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        Columns.builder()
            .children(fillingWidget("A"), fillingWidget("B"), fillingWidget("C"))
            .columnCount(3)
            .spacing(2)
            .build()
            .render(area, buffer);

        // 10 - 4 spacing = 6, 6/3 = 2 per col
        // col0: x=0,w=2; gap x=2,w=2; col1: x=4,w=2; gap x=6,w=2; col2: x=8,w=2
        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "AA", Style.EMPTY);
        expected.setString(4, 0, "BB", Style.EMPTY);
        expected.setString(8, 0, "CC", Style.EMPTY);

        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("column-first ordering fills top-to-bottom then left-to-right")
    void columnFirstOrdering() {
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);

        Columns.builder()
            .children(charWidget("A"), charWidget("B"), charWidget("C"), charWidget("D"))
            .columnCount(2)
            .order(ColumnOrder.COLUMN_FIRST)
            .build()
            .render(area, buffer);

        // column-first: col0=[A,B], col1=[C,D] -> row0=[A,C], row1=[B,D]
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "C")
            .hasSymbolAt(0, 1, "B")
            .hasSymbolAt(10, 1, "D");
    }

    @Test
    @DisplayName("row-first ordering fills left-to-right then top-to-bottom")
    void rowFirstOrdering() {
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);

        Columns.builder()
            .children(charWidget("A"), charWidget("B"), charWidget("C"), charWidget("D"))
            .columnCount(2)
            .order(ColumnOrder.ROW_FIRST)
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(0, 1, "C")
            .hasSymbolAt(10, 1, "D");
    }

    @Test
    @DisplayName("custom column widths are applied")
    void customColumnWidths() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);

        Columns.builder()
            .children(fillingWidget("A"), fillingWidget("B"))
            .columnCount(2)
            .columnWidths(Constraint.length(5), Constraint.fill())
            .build()
            .render(area, buffer);

        // col0: 5 wide, col1: 15 wide
        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "AAAAA", Style.EMPTY);
        for (int x = 5; x < 20; x++) {
            expected.setString(x, 0, "B", Style.EMPTY);
        }

        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("custom row heights are applied")
    void customRowHeights() {
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        Columns.builder()
            .children(fillingWidget("A"), fillingWidget("B"), fillingWidget("C"), fillingWidget("D"))
            .columnCount(2)
            .rowHeights(3, 2)
            .build()
            .render(area, buffer);

        // Row 0: height 3 (y=0..2), Row 1: height 2 (y=3..4)
        // Col0: x=0..4, Col1: x=5..9
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("A"); // last row of A
        assertThat(buffer.get(5, 0).symbol()).isEqualTo("B");
        assertThat(buffer.get(5, 2).symbol()).isEqualTo("B"); // last row of B
        assertThat(buffer.get(0, 3).symbol()).isEqualTo("C");
        assertThat(buffer.get(0, 4).symbol()).isEqualTo("C"); // last row of C
        assertThat(buffer.get(5, 3).symbol()).isEqualTo("D");
        assertThat(buffer.get(5, 4).symbol()).isEqualTo("D"); // last row of D
    }

    @Test
    @DisplayName("empty area does not render")
    void emptyArea() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 5));

        // Should not throw
        Columns.builder()
            .children(charWidget("A"))
            .columnCount(1)
            .build()
            .render(emptyArea, buffer);

        BufferAssertions.assertThat(buffer).isEqualTo(Buffer.empty(new Rect(0, 0, 10, 5)));
    }

    @Test
    @DisplayName("empty children does not render")
    void emptyChildren() {
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        Columns.builder()
            .columnCount(2)
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer).isEqualTo(Buffer.empty(area));
    }

    @Test
    @DisplayName("fewer children than grid cells leaves empty cells")
    void fewerChildrenThanCells() {
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);

        Columns.builder()
            .children(charWidget("A"), charWidget("B"), charWidget("C"))
            .columnCount(2)
            .build()
            .render(area, buffer);

        // 3 children in 2x2 grid: A(0,0), B(1,0), C(0,1), empty(1,1)
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(0, 1, "C");

        // Position (10,1) should remain empty (space)
        assertThat(buffer.get(10, 1).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("column count larger than children uses children count")
    void columnCountExceedsChildren() {
        Rect area = new Rect(0, 0, 30, 1);
        Buffer buffer = Buffer.empty(area);

        Columns.builder()
            .children(charWidget("A"), charWidget("B"))
            .columnCount(5)
            .build()
            .render(area, buffer);

        // Only 2 children, so effective cols = 2, 30/2 = 15 per col
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(15, 0, "B");
    }

    @Test
    @DisplayName("children accepts list")
    void childrenFromList() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);

        Columns.builder()
            .children(Arrays.asList(charWidget("X"), charWidget("Y")))
            .columnCount(2)
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "X")
            .hasSymbolAt(10, 0, "Y");
    }

    @Test
    @DisplayName("flex mode is applied")
    void flexMode() {
        // With Flex.START (default), content is packed to the left.
        // This test mainly verifies the flex parameter is passed through to Layout.
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);

        Columns.builder()
            .children(charWidget("A"))
            .columnCount(1)
            .flex(Flex.START)
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "A");
    }
}

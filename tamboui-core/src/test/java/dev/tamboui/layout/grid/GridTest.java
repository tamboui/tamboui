/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.grid;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.LayoutException;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.widget.Widget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the {@link Grid} widget.
 */
class GridTest {

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
    @DisplayName("renders children in a 2x2 grid")
    void basicGrid() {
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .children(charWidget("A"), charWidget("B"), charWidget("C"), charWidget("D"))
            .columnCount(2)
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(0, 1, "C")
            .hasSymbolAt(10, 1, "D");
    }

    @Test
    @DisplayName("renders children in a 3x3 grid")
    void threeByThree() {
        Rect area = new Rect(0, 0, 30, 3);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .children(charWidget("A"), charWidget("B"), charWidget("C"),
                      charWidget("D"), charWidget("E"), charWidget("F"),
                      charWidget("G"), charWidget("H"), charWidget("I"))
            .columnCount(3)
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(20, 0, "C")
            .hasSymbolAt(0, 1, "D")
            .hasSymbolAt(10, 1, "E")
            .hasSymbolAt(20, 1, "F")
            .hasSymbolAt(0, 2, "G")
            .hasSymbolAt(10, 2, "H")
            .hasSymbolAt(20, 2, "I");
    }

    @Test
    @DisplayName("horizontal gutter creates gaps between columns")
    void horizontalGutter() {
        Rect area = new Rect(0, 0, 21, 1);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .children(charWidget("A"), charWidget("B"))
            .columnCount(2)
            .horizontalGutter(1)
            .build()
            .render(area, buffer);

        // 21 - 1 gutter = 20, 20/2 = 10 per col
        // col0: x=0,w=10; gutter x=10; col1: x=11,w=10
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(11, 0, "B");
    }

    @Test
    @DisplayName("vertical gutter creates gaps between rows")
    void verticalGutter() {
        Rect area = new Rect(0, 0, 10, 3);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .children(charWidget("A"), charWidget("B"))
            .columnCount(1)
            .verticalGutter(1)
            .build()
            .render(area, buffer);

        // 2 rows with 1 vertical gutter: row0 y=0, gutter, row1 y=2
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(0, 2, "B");
    }

    @Test
    @DisplayName("asymmetric gutter")
    void asymmetricGutter() {
        Rect area = new Rect(0, 0, 22, 3);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .children(charWidget("A"), charWidget("B"), charWidget("C"), charWidget("D"))
            .columnCount(2)
            .horizontalGutter(2)
            .verticalGutter(1)
            .build()
            .render(area, buffer);

        // 22 - 2 gutter = 20, 20/2 = 10 per col
        // col0: x=0; col1: x=12
        // row0: y=0; row1: y=2
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(12, 0, "B")
            .hasSymbolAt(0, 2, "C")
            .hasSymbolAt(12, 2, "D");
    }

    @Test
    @DisplayName("column constraints control column widths")
    void columnConstraints() {
        Rect area = new Rect(0, 0, 30, 1);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .children(charWidget("A"), charWidget("B"))
            .columnCount(2)
            .columnConstraints(Constraint.length(10), Constraint.fill())
            .build()
            .render(area, buffer);

        // col0: 10 wide, col1: fill -> 20
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B");
    }

    @Test
    @DisplayName("column constraints cycle when fewer than columns")
    void columnConstraintsCycling() {
        Rect area = new Rect(0, 0, 30, 1);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .children(charWidget("A"), charWidget("B"), charWidget("C"))
            .columnCount(3)
            .columnConstraints(Constraint.length(8))
            .build()
            .render(area, buffer);

        // All 3 cols get length(8) via cycling
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(8, 0, "B")
            .hasSymbolAt(16, 0, "C");
    }

    @Test
    @DisplayName("row constraints control row heights")
    void rowConstraints() {
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .children(charWidget("A"), charWidget("B"), charWidget("C"), charWidget("D"))
            .columnCount(2)
            .rowConstraints(Constraint.length(2), Constraint.length(3))
            .build()
            .render(area, buffer);

        // row0: y=0,h=2; row1: y=2,h=3
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(0, 2, "C")
            .hasSymbolAt(10, 2, "D");
    }

    @Test
    @DisplayName("explicit row heights are applied")
    void explicitRowHeights() {
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .children(fillingWidget("A"), fillingWidget("B"))
            .columnCount(1)
            .rowHeights(3, 2)
            .build()
            .render(area, buffer);

        // Row 0: height 3, Row 1: height 2
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("A");
        assertThat(buffer.get(0, 3).symbol()).isEqualTo("B");
        assertThat(buffer.get(0, 4).symbol()).isEqualTo("B");
    }

    @Test
    @DisplayName("empty area does not render")
    void emptyArea() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 5));

        Grid.builder()
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

        Grid.builder()
            .children()
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

        Grid.builder()
            .children(charWidget("A"), charWidget("B"), charWidget("C"))
            .columnCount(2)
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(0, 1, "C");

        assertThat(buffer.get(10, 1).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("children accepts list")
    void childrenFromList() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
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
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .children(charWidget("A"))
            .columnCount(1)
            .flex(Flex.START)
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "A");
    }

    // ==================== Area-based Grid Tests ====================

    @Test
    @DisplayName("area-based grid renders widgets in named areas")
    void areaBasedGridRendersWidgets() {
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .gridAreas("A B", "C D")
            .area("A", charWidget("A"))
            .area("B", charWidget("B"))
            .area("C", charWidget("C"))
            .area("D", charWidget("D"))
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(0, 1, "C")
            .hasSymbolAt(10, 1, "D");
    }

    @Test
    @DisplayName("area-based grid with horizontal span")
    void areaBasedGridWithHorizontalSpan() {
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .gridAreas("A A", "B C")
            .area("A", fillingWidget("A"))
            .area("B", charWidget("B"))
            .area("C", charWidget("C"))
            .build()
            .render(area, buffer);

        // A spans both columns in row 0
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(10, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(19, 0).symbol()).isEqualTo("A");

        // B and C in row 1
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 1, "B")
            .hasSymbolAt(10, 1, "C");
    }

    @Test
    @DisplayName("area-based grid with vertical span")
    void areaBasedGridWithVerticalSpan() {
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .gridAreas("A B", "A C")
            .area("A", fillingWidget("A"))
            .area("B", charWidget("B"))
            .area("C", charWidget("C"))
            .build()
            .render(area, buffer);

        // A spans both rows in column 0
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("A");

        // B in row 0, C in row 1
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(10, 1, "C");
    }

    @Test
    @DisplayName("area-based grid with 2x2 span")
    void areaBasedGridWith2x2Span() {
        Rect area = new Rect(0, 0, 30, 3);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .gridAreas("A A B", "A A C", "D D D")
            .area("A", fillingWidget("A"))
            .area("B", charWidget("B"))
            .area("C", charWidget("C"))
            .area("D", fillingWidget("D"))
            .build()
            .render(area, buffer);

        // A spans 2x2 in top-left
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("A");
        assertThat(buffer.get(9, 1).symbol()).isEqualTo("A");

        // B and C in right column
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(20, 0, "B")
            .hasSymbolAt(20, 1, "C");

        // D spans bottom row
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("D");
        assertThat(buffer.get(15, 2).symbol()).isEqualTo("D");
        assertThat(buffer.get(29, 2).symbol()).isEqualTo("D");
    }

    @Test
    @DisplayName("area-based grid with gutters includes gutters in spans")
    void areaBasedGridWithGutters() {
        Rect area = new Rect(0, 0, 21, 3);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .gridAreas("A A", "B C")
            .area("A", fillingWidget("A"))
            .area("B", charWidget("B"))
            .area("C", charWidget("C"))
            .horizontalGutter(1)
            .verticalGutter(1)
            .build()
            .render(area, buffer);

        // A spans both columns plus the gutter between them
        // Width: 21 - 1 gutter = 20, 20/2 = 10 per col
        // A should fill: cols 0-9, gutter at 10, cols 11-20, so A gets 0-20
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(10, 0).symbol()).isEqualTo("A"); // gutter is inside A's span
        assertThat(buffer.get(20, 0).symbol()).isEqualTo("A");

        // Row 1 is gutter (empty)
        assertThat(buffer.get(0, 1).symbol()).isEqualTo(" ");

        // B and C in row 2
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 2, "B")
            .hasSymbolAt(11, 2, "C");
    }

    @Test
    @DisplayName("area-based grid with empty areas renders nothing")
    void areaBasedGridWithEmptyAreas() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .gridAreas("A B")
            .area("A", charWidget("A"))
            // B has no widget assigned - should be empty
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "A");
        assertThat(buffer.get(10, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("area-based grid with dot notation for empty cells")
    void areaBasedGridWithDotNotation() {
        Rect area = new Rect(0, 0, 30, 1);
        Buffer buffer = Buffer.empty(area);

        Grid.builder()
            .gridAreas("A . B")
            .area("A", charWidget("A"))
            .area("B", charWidget("B"))
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(20, 0, "B");
        // Middle column is empty (dot notation)
        assertThat(buffer.get(10, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("area-based grid throws on undefined area")
    void areaBasedGridThrowsOnUndefinedArea() {
        assertThatThrownBy(() ->
            Grid.builder()
                .gridAreas("A B")
                .area("A", charWidget("A"))
                .area("C", charWidget("C")) // C not defined in template
                .build())
            .isInstanceOf(LayoutException.class)
            .hasMessageContaining("undefined area 'C'");
    }

    @Test
    @DisplayName("children-based grid throws when too many children for fixed row count")
    void childrenGridThrowsOnTooManyChildren() {
        assertThatThrownBy(() ->
            Grid.builder()
                .children(charWidget("A"), charWidget("B"), charWidget("C"),
                          charWidget("D"), charWidget("E"), charWidget("F"))
                .columnCount(2)
                .rowCount(2) // 2x2 = 4 cells, but 6 children
                .build())
            .isInstanceOf(LayoutException.class)
            .hasMessageContaining("6 children")
            .hasMessageContaining("4 cells");
    }

    @Test
    @DisplayName("children-based grid allows many children when rowCount not set")
    void childrenGridAllowsManyChildrenWithoutRowCount() {
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);

        // 6 children with 2 columns = 3 rows auto-calculated
        Grid.builder()
            .children(charWidget("A"), charWidget("B"), charWidget("C"),
                      charWidget("D"), charWidget("E"), charWidget("F"))
            .columnCount(2)
            .build()
            .render(area, buffer);

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(0, 1, "C")
            .hasSymbolAt(10, 1, "D")
            .hasSymbolAt(0, 2, "E")
            .hasSymbolAt(10, 2, "F");
    }
}

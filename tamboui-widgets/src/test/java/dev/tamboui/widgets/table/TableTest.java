/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.table;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.style.TestStylePropertyResolver;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widgets.block.Block;

import static org.assertj.core.api.Assertions.*;

class TableTest {

    @Test
    @DisplayName("Table renders basic content")
    void rendersBasicContent() {
        Table table = Table.builder()
            .rows(Arrays.asList(
                Row.from("Alice", "30"),
                Row.from("Bob", "25")
            ))
            .widths(Constraint.length(10), Constraint.length(5))
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        // First row, first column
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("l");
        // Second row, first column
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("B");
    }

    @Test
    @DisplayName("Table renders header")
    void rendersHeader() {
        Table table = Table.builder()
            .header(Row.from("Name", "Age"))
            .rows(Arrays.asList(Row.from("Alice", "30")))
            .widths(Constraint.length(10), Constraint.length(5))
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        // Header in first row
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("N");
        // Data in second row
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("A");
    }

    @Test
    @DisplayName("Table renders with selection")
    void rendersWithSelection() {
        Style highlightStyle = Style.EMPTY.bg(Color.BLUE);
        Table table = Table.builder()
            .rows(Arrays.asList(
                Row.from("Alice", "30"),
                Row.from("Bob", "25")
            ))
            .widths(Constraint.length(10), Constraint.length(5))
            .highlightStyle(highlightStyle)
            .highlightSymbol("> ")
            .highlightSpacing(Table.HighlightSpacing.ALWAYS)
            .build();

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();
        state.select(1); // Select second row

        table.render(area, buffer, state);

        // Highlight symbol on selected row
        assertThat(buffer.get(0, 1).symbol()).isEqualTo(">");
        assertThat(buffer.get(1, 1).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Table with block")
    void withBlock() {
        Table table = Table.builder()
            .rows(Arrays.asList(Row.from("Data")))
            .widths(Constraint.length(10))
            .block(Block.bordered())
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 15, 5);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        // Block border
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("┌");
        // Data inside block
        assertThat(buffer.get(1, 1).symbol()).isEqualTo("D");
    }

    @Test
    @DisplayName("Table column spacing")
    void columnSpacing() {
        Table table = Table.builder()
            .rows(Arrays.asList(Row.from("A", "B")))
            .widths(Constraint.length(3), Constraint.length(3))
            .columnSpacing(2)
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        // First column at 0
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        // Second column at 3 + 2 spacing = 5
        assertThat(buffer.get(5, 0).symbol()).isEqualTo("B");
    }

    @Test
    @DisplayName("Table with footer")
    void withFooter() {
        Table table = Table.builder()
            .rows(Arrays.asList(Row.from("Data")))
            .footer(Row.from("Total: 1"))
            .widths(Constraint.length(15))
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        // Data row
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("D");
        // Footer at bottom
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("T");
    }

    @Test
    @DisplayName("Table rows accessor")
    void rowsAccessor() {
        List<Row> rows = Arrays.asList(Row.from("A"), Row.from("B"));
        Table table = Table.builder()
            .rows(rows)
            .widths(Constraint.length(5))
            .build();

        assertThat(table.rows()).hasSize(2);
    }

    @Test
    @DisplayName("Table with percentage constraints")
    void percentageConstraints() {
        Table table = Table.builder()
            .rows(Arrays.asList(Row.from("Left", "Right")))
            .widths(Constraint.percentage(50), Constraint.percentage(50))
            .highlightSymbol("")
            .columnSpacing(0)
            .build();

        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        // Both columns should render
        // With 50% + 50% and no spacing, columns are 10 + 10
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("L");
        // Second column starts at position 10
        assertThat(buffer.get(10, 0).symbol()).isEqualTo("R");
    }

    @Test
    @DisplayName("Table highlight spacing NEVER does not reserve space")
    void highlightSpacingNever() {
        Table table = Table.builder()
            .rows(Arrays.asList(Row.from("Data")))
            .widths(Constraint.length(10))
            .highlightSymbol(">> ")
            .highlightSpacing(Table.HighlightSpacing.NEVER)
            .build();

        Rect area = new Rect(0, 0, 15, 1);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();
        state.select(0);

        table.render(area, buffer, state);

        // No space reserved for symbol, data starts at 0
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("D");
    }

    @Test
    @DisplayName("Table highlight spacing ALWAYS reserves space")
    void highlightSpacingAlways() {
        Table table = Table.builder()
            .rows(Arrays.asList(Row.from("Data")))
            .widths(Constraint.length(10))
            .highlightSymbol(">> ")
            .highlightSpacing(Table.HighlightSpacing.ALWAYS)
            .build();

        Rect area = new Rect(0, 0, 15, 1);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();
        // No selection

        table.render(area, buffer, state);

        // Space reserved for symbol even without selection
        // Data starts after symbol width (3)
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("D");
    }

    @Test
    @DisplayName("Empty table renders nothing")
    void emptyTable() {
        Table table = Table.builder()
            .widths(Constraint.length(10))
            .build();

        Rect area = new Rect(0, 0, 15, 3);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        // Should not crash, buffer remains empty
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Table without widths renders nothing")
    void noWidths() {
        Table table = Table.builder()
            .rows(Arrays.asList(Row.from("Data")))
            .build();

        Rect area = new Rect(0, 0, 15, 3);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        // Without widths, nothing renders
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("viewportHeight returns full area height with no chrome")
    void viewportHeightNoChrome() {
        Table table = Table.builder()
            .rows(Arrays.asList(Row.from("Data")))
            .widths(Constraint.length(10))
            .build();

        Rect area = new Rect(0, 0, 20, 10);
        assertThat(table.viewportHeight(area)).isEqualTo(10);
    }

    @Test
    @DisplayName("viewportHeight subtracts block chrome")
    void viewportHeightWithBlock() {
        Table table = Table.builder()
            .rows(Arrays.asList(Row.from("Data")))
            .widths(Constraint.length(10))
            .block(Block.bordered())
            .build();

        Rect area = new Rect(0, 0, 20, 10);
        // Bordered block takes 2 rows (top + bottom border)
        assertThat(table.viewportHeight(area)).isEqualTo(8);
    }

    @Test
    @DisplayName("viewportHeight subtracts header height")
    void viewportHeightWithHeader() {
        Table table = Table.builder()
            .header(Row.from("Name", "Age"))
            .rows(Arrays.asList(Row.from("Alice", "30")))
            .widths(Constraint.length(10), Constraint.length(5))
            .build();

        Rect area = new Rect(0, 0, 20, 10);
        // Header takes 1 row
        assertThat(table.viewportHeight(area)).isEqualTo(9);
    }

    @Test
    @DisplayName("viewportHeight subtracts footer height")
    void viewportHeightWithFooter() {
        Table table = Table.builder()
            .rows(Arrays.asList(Row.from("Data")))
            .footer(Row.from("Total: 1"))
            .widths(Constraint.length(15))
            .build();

        Rect area = new Rect(0, 0, 20, 10);
        // Footer takes 1 row
        assertThat(table.viewportHeight(area)).isEqualTo(9);
    }

    @Test
    @DisplayName("viewportHeight subtracts block, header, and footer")
    void viewportHeightWithAll() {
        Table table = Table.builder()
            .header(Row.from("Name"))
            .rows(Arrays.asList(Row.from("Data")))
            .footer(Row.from("Total"))
            .widths(Constraint.length(10))
            .block(Block.bordered())
            .build();

        Rect area = new Rect(0, 0, 20, 10);
        // Block: 2, header: 1, footer: 1 -> 10 - 4 = 6
        assertThat(table.viewportHeight(area)).isEqualTo(6);
    }

    @Test
    @DisplayName("viewportHeight never returns negative")
    void viewportHeightNeverNegative() {
        Table table = Table.builder()
            .header(Row.from("Name"))
            .rows(Arrays.asList(Row.from("Data")))
            .footer(Row.from("Total"))
            .widths(Constraint.length(10))
            .block(Block.bordered())
            .build();

        // Area too small to fit any data rows
        Rect area = new Rect(0, 0, 20, 2);
        assertThat(table.viewportHeight(area)).isEqualTo(0);
    }

    @Test
    @DisplayName("viewportHeight with header bottom margin")
    void viewportHeightWithHeaderMargin() {
        Table table = Table.builder()
            .header(Row.from("Name").bottomMargin(1))
            .rows(Arrays.asList(Row.from("Data")))
            .widths(Constraint.length(10))
            .build();

        Rect area = new Rect(0, 0, 20, 10);
        // Header: height 1 + margin 1 = totalHeight 2
        assertThat(table.viewportHeight(area)).isEqualTo(8);
    }

    @Test
    @DisplayName("Table uses HIGHLIGHT_COLOR property from StylePropertyResolver")
    void usesHighlightColorProperty() {
        Table table = Table.builder()
                .rows(Arrays.asList(
                    Row.from("Alice", "30"),
                    Row.from("Bob", "25")
                ))
                .widths(Constraint.length(10), Constraint.length(5))
                .highlightSymbol("")
                .highlightSpacing(Table.HighlightSpacing.NEVER)
                .styleResolver(TestStylePropertyResolver.of("highlight-color", Color.BLUE))
                .build();

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();
        state.select(1); // Select second row

        table.render(area, buffer, state);

        // Second row "Bob" starts at y=1 and should have blue background
        BufferAssertions.assertThat(buffer).at(0, 1).hasBackground(Color.BLUE);
    }

    @Test
    @DisplayName("Table renders right-aligned and centered cells")
    void rendersAlignedCells() {
        Table table = Table.builder()
            .rows(Arrays.asList(
                Row.from(
                    Cell.from("Hi").rightAligned(),
                    Cell.from("Hey").centered()
                )
            ))
            .widths(Constraint.length(6), Constraint.length(7))
            .columnSpacing(0)
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 15, 1);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        // Column 0: width 6, text "Hi" (len 2) right aligned -> offset 6 - 2 = 4 -> index 4, 5
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(5, 0).symbol()).isEqualTo("i");

        // Column 1: starts at x=6, width 7, text "Hey" (len 3) centered -> offset (7 - 3)/2 = 2 -> index 6+2 = 8
        assertThat(buffer.get(6, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(7, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(8, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("e");
        assertThat(buffer.get(10, 0).symbol()).isEqualTo("y");
        assertThat(buffer.get(11, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(12, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Table renders with columnAlignments")
    void rendersColumnAlignments() {
        Table table = Table.builder()
            .rows(Arrays.asList(
                Row.from("A", "B")
            ))
            .widths(Constraint.length(5), Constraint.length(5))
            .columnAlignments(Alignment.LEFT, Alignment.RIGHT)
            .columnSpacing(0)
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        // Col 0: left aligned "A" at x=0
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo(" ");

        // Col 1: right aligned "B" at x=5 + (5 - 1) = 9
        assertThat(buffer.get(8, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("B");
    }

    @Test
    @DisplayName("Cell alignment takes precedence over column alignment")
    void cellAlignmentPrecedenceOverColumn() {
        Table table = Table.builder()
            .rows(Arrays.asList(
                Row.from(Cell.from("Hi").leftAligned())
            ))
            .widths(Constraint.length(6))
            .columnAlignments(Alignment.RIGHT)
            .columnSpacing(0)
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        // Cell is explicitly left aligned, overriding table column alignment (RIGHT)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("i");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Line alignment takes precedence over cell and column alignment")
    void lineAlignmentPrecedenceOverCellAndColumn() {
        Line line = Line.from(Span.raw("Hi")).alignment(Alignment.RIGHT);
        Cell cell = Cell.from(line).leftAligned(); // cell is leftAligned, line is rightAligned

        Table table = Table.builder()
            .rows(Arrays.asList(Row.from(cell)))
            .widths(Constraint.length(6))
            .columnAlignments(Alignment.CENTER)
            .columnSpacing(0)
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        // Line is right-aligned -> x=4, 5
        assertThat(buffer.get(3, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(5, 0).symbol()).isEqualTo("i");
    }

    @Test
    @DisplayName("Table truncates overflowing text regardless of alignment")
    void textOverflowTruncates() {
        Table table = Table.builder()
            .rows(Arrays.asList(
                Row.from(Cell.from("ABCDEFG").rightAligned())
            ))
            .widths(Constraint.length(4))
            .columnSpacing(0)
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        // Text width 7 > col width 4: startX stays 0, truncated to 4 chars "ABCD"
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("B");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("C");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("D");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo(" ");
    }
}

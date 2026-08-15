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
import dev.tamboui.text.Text;
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
    @DisplayName("Cell alignment LEFT places content at column start")
    void cellAlignmentLeft() {
        Table table = Table.builder()
            .rows(Arrays.asList(Row.from(Cell.from("Hi").alignment(Alignment.LEFT))))
            .widths(Constraint.length(10))
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        table.render(area, buffer, new TableState());

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("i");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Cell alignment CENTER centers content within column")
    void cellAlignmentCenter() {
        Table table = Table.builder()
            .rows(Arrays.asList(Row.from(Cell.from("Hi").alignment(Alignment.CENTER))))
            .widths(Constraint.length(10))
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        table.render(area, buffer, new TableState());

        // (10 - 2) / 2 = 4
        assertThat(buffer.get(3, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(5, 0).symbol()).isEqualTo("i");
        assertThat(buffer.get(6, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Cell alignment RIGHT places content at column end")
    void cellAlignmentRight() {
        Table table = Table.builder()
            .rows(Arrays.asList(Row.from(Cell.from("Hi").alignment(Alignment.RIGHT))))
            .widths(Constraint.length(10))
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        table.render(area, buffer, new TableState());

        assertThat(buffer.get(7, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(8, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("i");
    }

    @Test
    @DisplayName("Cell alignment RIGHT applies to second column with column spacing")
    void cellAlignmentRightInSecondColumn() {
        Table table = Table.builder()
            .rows(Arrays.asList(Row.from(
                Cell.from("A"),
                Cell.from("42").alignment(Alignment.RIGHT)
            )))
            .widths(Constraint.length(3), Constraint.length(5))
            .columnSpacing(1)
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 9, 1);
        Buffer buffer = Buffer.empty(area);

        table.render(area, buffer, new TableState());

        // First column left-aligned at x=0
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        // Second column starts at x=4 (3 + 1 spacing), width 5; "42" right-aligned at x=7,8
        assertThat(buffer.get(6, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(7, 0).symbol()).isEqualTo("4");
        assertThat(buffer.get(8, 0).symbol()).isEqualTo("2");
    }

    @Test
    @DisplayName("Cell alignment applies per-line for multi-line cells")
    void cellAlignmentMultiLine() {
        Table table = Table.builder()
            .rows(Arrays.asList(
                Row.from(Cell.from(Text.from("Hi\nThere"))
                    .alignment(Alignment.RIGHT))
                .height(2)
            ))
            .widths(Constraint.length(10))
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 10, 2);
        Buffer buffer = Buffer.empty(area);

        table.render(area, buffer, new TableState());

        // "Hi" right-aligned on row 0
        assertThat(buffer.get(8, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("i");
        // "There" right-aligned on row 1
        assertThat(buffer.get(5, 1).symbol()).isEqualTo("T");
        assertThat(buffer.get(9, 1).symbol()).isEqualTo("e");
    }

    @Test
    @DisplayName("Cell content wider than column is clipped from left edge regardless of alignment")
    void cellAlignmentClipsOverflowFromLeft() {
        Table table = Table.builder()
            .rows(Arrays.asList(Row.from(
                Cell.from("HelloWorld").alignment(Alignment.RIGHT)
            )))
            .widths(Constraint.length(5))
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 5, 1);
        Buffer buffer = Buffer.empty(area);

        table.render(area, buffer, new TableState());

        // Overflow: clipped from left edge, first 5 chars rendered
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("o");
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.style.TestStylePropertyResolver;
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
    @DisplayName("Selected multi-line row scrolled into view is drawn whole, inside the table")
    void selectedMultiLineRowScrolledIntoViewIsDrawnWhole() {
        Table table = Table.builder()
            .rows(Arrays.asList(
                Row.from("a0\nb0"),
                Row.from("a1\nb1"),
                Row.from("a2\nb2")
            ))
            .widths(Constraint.length(4))
            .build();

        // The table is drawn into the top 5 lines of a taller buffer
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 8));
        TableState state = new TableState();
        state.select(2);

        table.render(new Rect(0, 0, 10, 5), buffer, state);

        // Scrolled by one line: row 0 shows its second line, row 2 both of its lines
        BufferAssertions.assertThat(buffer).hasContent(
            "   b0     ",
            "   a1     ",
            "   b1     ",
            ">> a2     ",
            "   b2     ",
            "          ",
            "          ",
            "          ");
        // The highlight covers both lines of the selected row and stops at the table
        assertThat(buffer.get(3, 4).style().addModifiers()).contains(Modifier.REVERSED);
        assertThat(reversedCells(buffer, 5)).isEmpty();
    }

    @Test
    @DisplayName("Multi-line row above the footer is cut off at the footer")
    void multiLineRowAboveFooterIsCutOffAtFooter() {
        Table table = Table.builder()
            .rows(Arrays.asList(
                Row.from("a0\nb0", "c0\nd0"),
                Row.from("a1\nb1", "c1\nd1")
            ))
            .footer(Row.from("sum"))
            .widths(Constraint.length(3), Constraint.length(3))
            .build();

        Rect area = new Rect(0, 0, 8, 4);
        Buffer buffer = Buffer.empty(area);

        table.render(area, buffer, new TableState());

        BufferAssertions.assertThat(buffer).hasContent(
            "a0  c0  ",
            "b0  d0  ",
            "a1  c1  ",
            "sum     ");
    }

    @Test
    @DisplayName("Selected multi-line row above the footer stays out of the footer")
    void selectedMultiLineRowAboveFooterStaysOutOfFooter() {
        Table table = Table.builder()
            .rows(Arrays.asList(
                Row.from("a0\nb0", "c0\nd0"),
                Row.from("a1\nb1", "c1\nd1"),
                Row.from("a2\nb2", "c2\nd2")
            ))
            .footer(Row.from("sum"))
            .widths(Constraint.length(3), Constraint.length(3))
            .build();

        Rect area = new Rect(0, 0, 11, 4);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();
        state.select(2);

        table.render(area, buffer, state);

        BufferAssertions.assertThat(buffer).hasContent(
            "   b1  d1  ",
            ">> a2  c2  ",
            "   b2  d2  ",
            "   sum     ");
        // The highlight covers both lines of the selected row and stops at the footer
        assertThat(buffer.get(3, 2).style().addModifiers()).contains(Modifier.REVERSED);
        assertThat(reversedCells(buffer, 3)).isEmpty();
    }

    private static List<String> reversedCells(Buffer buffer, int fromY) {
        List<String> cells = new ArrayList<>();
        for (int y = fromY; y < buffer.height(); y++) {
            for (int x = 0; x < buffer.width(); x++) {
                if (buffer.get(x, y).style().addModifiers().contains(Modifier.REVERSED)) {
                    cells.add("(" + x + "," + y + ")");
                }
            }
        }
        return cells;
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
}

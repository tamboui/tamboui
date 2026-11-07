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
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
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
    @DisplayName("Table without widths auto-derives fill columns and renders (#305)")
    void noWidths() {
        Table table = Table.builder()
            .rows(Arrays.asList(Row.from("Data")))
            .build();

        Rect area = new Rect(0, 0, 15, 3);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("D");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("a");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("t");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("a");
    }

    @Test
    @DisplayName("Empty table without widths renders nothing")
    void emptyTableWithoutWidths() {
        Table table = Table.builder().build();

        Rect area = new Rect(0, 0, 15, 3);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Table with Constraint.fit() sizes columns to content width (#413)")
    void fitConstraintsSizeToContent() {
        Row header = Row.from("PID", "NAME", "STATUS");
        List<Row> rows = Arrays.asList(
            Row.from("1234", "java", "RUNNING"),
            Row.from("5678", "node", "IDLE")
        );
        Table table = Table.builder()
            .header(header)
            .rows(rows)
            .widths(Constraint.fit(), Constraint.fit(), Constraint.fit())
            .columnSpacing(1)
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 40, 3);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        // Col 0 = max("PID","1234","5678") = 4; col 1 at 4+1=5; col 2 at 5+4+1=10
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("P");
        assertThat(buffer.get(5, 0).symbol()).isEqualTo("N");
        assertThat(buffer.get(10, 0).symbol()).isEqualTo("S");
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("1");
        assertThat(buffer.get(5, 1).symbol()).isEqualTo("j");
        assertThat(buffer.get(10, 1).symbol()).isEqualTo("R");
    }

    @Test
    @DisplayName("Table with mixed Fit and Fill constraints pins Fit to content and lets Fill absorb")
    void mixedFitAndFillConstraints() {
        Row header = Row.from("ID", "DESCRIPTION");
        List<Row> rows = Arrays.asList(Row.from("20", "First item"));
        Table table = Table.builder()
            .header(header)
            .rows(rows)
            .widths(Constraint.fit(), Constraint.fill())
            .columnSpacing(1)
            .highlightSymbol("")
            .build();

        Rect area = new Rect(0, 0, 30, 2);
        Buffer buffer = Buffer.empty(area);
        TableState state = new TableState();

        table.render(area, buffer, state);

        // Col 0 max width = 2 ("ID"/"20"); col 1 starts at 2+1=3
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("I");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("D");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("D");
        assertThat(buffer.get(3, 1).symbol()).isEqualTo("F");
    }

    @Test
    @DisplayName("distributeContentWidths sizes to content when all columns fit")
    void distributeSizesToContentWhenAllFit() {
        assertThat(Table.distributeContentWidths(new int[] {8, 70}, 149)).containsExactly(8, 70);
    }

    @Test
    @DisplayName("distributeContentWidths keeps narrow columns and shares remainder among wide ones")
    void distributeNarrowKeepWideShare() {
        assertThat(Table.distributeContentWidths(new int[] {8, 70}, 59)).containsExactly(8, 51);
        assertThat(Table.distributeContentWidths(new int[] {10, 20, 80}, 60)).containsExactly(10, 20, 30);
    }

    @Test
    @DisplayName("distributeContentWidths shares remainder evenly, uneven going to earlier columns")
    void distributeRemainder() {
        assertThat(Table.distributeContentWidths(new int[] {5, 50, 50}, 55)).containsExactly(5, 25, 25);
        assertThat(Table.distributeContentWidths(new int[] {5, 50, 50}, 56)).containsExactly(5, 26, 25);
    }

    @Test
    @DisplayName("distributeContentWidths handles all-wide, zero/negative, and empty input")
    void distributeEdgeCases() {
        assertThat(Table.distributeContentWidths(new int[] {30, 40}, 20)).containsExactly(10, 10);
        assertThat(Table.distributeContentWidths(new int[] {10, 20}, 0)).containsExactly(0, 0);
        assertThat(Table.distributeContentWidths(new int[] {10, 20}, -5)).containsExactly(0, 0);
        assertThat(Table.distributeContentWidths(new int[0], 100)).isEmpty();
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

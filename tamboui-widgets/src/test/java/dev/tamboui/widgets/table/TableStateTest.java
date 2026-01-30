/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.table;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TableStateTest {

    @Test
    @DisplayName("Initial state has no selection")
    void initialState() {
        TableState state = new TableState();
        assertThat(state.selected()).isNull();
        assertThat(state.offset()).isEqualTo(0);
    }

    @Test
    @DisplayName("select sets the selected row")
    void select() {
        TableState state = new TableState();
        state.select(5);
        assertThat(state.selected()).isEqualTo(5);
    }

    @Test
    @DisplayName("select clamps to zero")
    void selectClampsToZero() {
        TableState state = new TableState();
        state.select(-10);
        assertThat(state.selected()).isEqualTo(0);
    }

    @Test
    @DisplayName("clearSelection removes selection")
    void clearSelection() {
        TableState state = new TableState();
        state.select(3);
        state.clearSelection();
        assertThat(state.selected()).isNull();
    }

    @Test
    @DisplayName("selectFirst selects first row")
    void selectFirst() {
        TableState state = new TableState();
        state.selectFirst();
        assertThat(state.selected()).isEqualTo(0);
    }

    @Test
    @DisplayName("selectLast selects last row")
    void selectLast() {
        TableState state = new TableState();
        state.selectLast(10);
        assertThat(state.selected()).isEqualTo(9);
    }

    @Test
    @DisplayName("selectLast does nothing with zero rows")
    void selectLastEmpty() {
        TableState state = new TableState();
        state.selectLast(0);
        assertThat(state.selected()).isNull();
    }

    @Test
    @DisplayName("selectNext moves to next row")
    void selectNext() {
        TableState state = new TableState();
        state.select(3);
        state.selectNext(10);
        assertThat(state.selected()).isEqualTo(4);
    }

    @Test
    @DisplayName("selectNext stops at last row")
    void selectNextAtEnd() {
        TableState state = new TableState();
        state.select(9);
        state.selectNext(10);
        assertThat(state.selected()).isEqualTo(9);
    }

    @Test
    @DisplayName("selectNext selects first when nothing selected")
    void selectNextFromNull() {
        TableState state = new TableState();
        state.selectNext(10);
        assertThat(state.selected()).isEqualTo(0);
    }

    @Test
    @DisplayName("selectPrevious moves to previous row")
    void selectPrevious() {
        TableState state = new TableState();
        state.select(5);
        state.selectPrevious();
        assertThat(state.selected()).isEqualTo(4);
    }

    @Test
    @DisplayName("selectPrevious stops at first row")
    void selectPreviousAtStart() {
        TableState state = new TableState();
        state.select(0);
        state.selectPrevious();
        assertThat(state.selected()).isEqualTo(0);
    }

    @Test
    @DisplayName("selectPrevious selects first when nothing selected")
    void selectPreviousFromNull() {
        TableState state = new TableState();
        state.selectPrevious();
        assertThat(state.selected()).isEqualTo(0);
    }

    @Test
    @DisplayName("setOffset sets scroll offset")
    void setOffset() {
        TableState state = new TableState();
        state.setOffset(10);
        assertThat(state.offset()).isEqualTo(10);
    }

    @Test
    @DisplayName("setOffset clamps to zero")
    void setOffsetClampsToZero() {
        TableState state = new TableState();
        state.setOffset(-5);
        assertThat(state.offset()).isEqualTo(0);
    }

    @Test
    @DisplayName("scrollToSelected adjusts offset to show selected row")
    void scrollToSelected() {
        TableState state = new TableState();
        List<Row> rows = Arrays.asList(Row.from("Row 1"), Row.from("Row 2"), Row.from("Row 3"),
                Row.from("Row 4"), Row.from("Row 5"));

        // Select row 4 (index 3)
        state.select(3);
        // Visible height is 2 rows
        state.scrollToSelected(2, rows);

        // Offset should be adjusted to show row 3 (each row is height 1)
        assertThat(state.offset()).isGreaterThanOrEqualTo(2);
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.buffer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

import static org.assertj.core.api.Assertions.*;

class CellTest {

    @Test
    @DisplayName("Cell EMPTY has space symbol and empty style")
    void emptyCell() {
        assertThat(Cell.EMPTY.symbol()).isEqualTo(" ");
        assertThat(Cell.EMPTY.style()).isEqualTo(Style.EMPTY);
    }

    @Test
    @DisplayName("Cell symbol creates new cell with new symbol")
    void symbol() {
        Cell cell = Cell.EMPTY.symbol("X");
        assertThat(cell.symbol()).isEqualTo("X");
        assertThat(cell.style()).isEqualTo(Style.EMPTY);
    }

    @Test
    @DisplayName("Cell style creates new cell with new style")
    void style() {
        Style style = Style.EMPTY.fg(Color.RED);
        Cell cell = Cell.EMPTY.style(style);
        assertThat(cell.symbol()).isEqualTo(" ");
        assertThat(cell.style()).isEqualTo(style);
    }

    @Test
    @DisplayName("Cell reset returns EMPTY")
    void reset() {
        Cell cell = new Cell("X", Style.EMPTY.fg(Color.RED));
        assertThat(cell.reset()).isEqualTo(Cell.EMPTY);
    }
}

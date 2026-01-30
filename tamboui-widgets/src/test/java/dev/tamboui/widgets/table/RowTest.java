/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.table;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.text.Text;

import static org.assertj.core.api.Assertions.*;

class RowTest {

    @Test
    @DisplayName("Row.from(Cell...) creates row with cells")
    void fromCells() {
        Row row = Row.from(Cell.from("A"), Cell.from("B"), Cell.from("C"));
        assertThat(row.cells()).hasSize(3);
        assertThat(row.height()).isEqualTo(1);
    }

    @Test
    @DisplayName("Row.from(String...) creates row from strings")
    void fromStrings() {
        Row row = Row.from("Name", "Age", "City");
        assertThat(row.cells()).hasSize(3);
        assertThat(row.cells().get(0).content().width()).isEqualTo(4);
    }

    @Test
    @DisplayName("Row.from(List<Cell>) creates row from list")
    void fromList() {
        List<Cell> cells = Arrays.asList(Cell.from("X"), Cell.from("Y"));
        Row row = Row.from(cells);
        assertThat(row.cells()).hasSize(2);
    }

    @Test
    @DisplayName("Row.empty creates empty row")
    void emptyRow() {
        Row row = Row.empty();
        assertThat(row.cells()).isEmpty();
        assertThat(row.height()).isEqualTo(1);
    }

    @Test
    @DisplayName("Row height is calculated from cell content")
    void heightFromContent() {
        Cell multiLineCell = Cell.from(Text.from("Line1\nLine2\nLine3"));
        Row row = Row.from(Cell.from("Single"), multiLineCell);
        assertThat(row.height()).isEqualTo(3);
    }

    @Test
    @DisplayName("Row.height sets explicit height")
    void explicitHeight() {
        Row row = Row.from("A", "B").height(5);
        assertThat(row.height()).isEqualTo(5);
    }

    @Test
    @DisplayName("Row.style sets row style")
    void withStyle() {
        Row row = Row.from("A", "B").style(Style.EMPTY.bold());
        assertThat(row.style().addModifiers()).contains(Modifier.BOLD);
    }

    @Test
    @DisplayName("Row.bottomMargin sets margin below row")
    void withBottomMargin() {
        Row row = Row.from("A", "B").bottomMargin(2);
        assertThat(row.bottomMargin()).isEqualTo(2);
        assertThat(row.totalHeight()).isEqualTo(3); // 1 height + 2 margin
    }

    @Test
    @DisplayName("Row height minimum is 1")
    void minimumHeight() {
        Row row = Row.from("A").height(0);
        assertThat(row.height()).isEqualTo(1);
    }

    @Test
    @DisplayName("Row bottom margin minimum is 0")
    void minimumMargin() {
        Row row = Row.from("A").bottomMargin(-5);
        assertThat(row.bottomMargin()).isEqualTo(0);
    }
}

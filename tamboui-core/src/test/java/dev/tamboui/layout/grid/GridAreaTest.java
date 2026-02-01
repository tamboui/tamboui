/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.grid;

import dev.tamboui.layout.LayoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the {@link GridArea} parser and validator.
 */
class GridAreaTest {

    @Test
    @DisplayName("parses simple 2x2 grid")
    void parsesSimple2x2Grid() {
        GridArea area = GridArea.parse("A B", "C D");

        assertThat(area.rows()).isEqualTo(2);
        assertThat(area.columns()).isEqualTo(2);
        assertThat(area.areaNames()).containsExactly("A", "B", "C", "D");
    }

    @Test
    @DisplayName("parses single cell grid")
    void parsesSingleCellGrid() {
        GridArea area = GridArea.parse("A");

        assertThat(area.rows()).isEqualTo(1);
        assertThat(area.columns()).isEqualTo(1);
        assertThat(area.areaNames()).containsExactly("A");

        GridArea.AreaBounds bounds = area.boundsFor("A");
        assertThat(bounds.row()).isEqualTo(0);
        assertThat(bounds.column()).isEqualTo(0);
        assertThat(bounds.rowSpan()).isEqualTo(1);
        assertThat(bounds.columnSpan()).isEqualTo(1);
    }

    @Test
    @DisplayName("parses horizontally spanning area")
    void parsesHorizontalSpan() {
        GridArea area = GridArea.parse("A A B", "C D D");

        assertThat(area.rows()).isEqualTo(2);
        assertThat(area.columns()).isEqualTo(3);

        GridArea.AreaBounds boundsA = area.boundsFor("A");
        assertThat(boundsA.row()).isEqualTo(0);
        assertThat(boundsA.column()).isEqualTo(0);
        assertThat(boundsA.rowSpan()).isEqualTo(1);
        assertThat(boundsA.columnSpan()).isEqualTo(2);

        GridArea.AreaBounds boundsD = area.boundsFor("D");
        assertThat(boundsD.row()).isEqualTo(1);
        assertThat(boundsD.column()).isEqualTo(1);
        assertThat(boundsD.rowSpan()).isEqualTo(1);
        assertThat(boundsD.columnSpan()).isEqualTo(2);
    }

    @Test
    @DisplayName("parses vertically spanning area")
    void parsesVerticalSpan() {
        GridArea area = GridArea.parse("A B", "A C");

        GridArea.AreaBounds boundsA = area.boundsFor("A");
        assertThat(boundsA.row()).isEqualTo(0);
        assertThat(boundsA.column()).isEqualTo(0);
        assertThat(boundsA.rowSpan()).isEqualTo(2);
        assertThat(boundsA.columnSpan()).isEqualTo(1);
    }

    @Test
    @DisplayName("parses 2x2 spanning area")
    void parses2x2Span() {
        GridArea area = GridArea.parse("A A B", "A A C", "D D D");

        GridArea.AreaBounds boundsA = area.boundsFor("A");
        assertThat(boundsA.row()).isEqualTo(0);
        assertThat(boundsA.column()).isEqualTo(0);
        assertThat(boundsA.rowSpan()).isEqualTo(2);
        assertThat(boundsA.columnSpan()).isEqualTo(2);
        assertThat(boundsA.endRow()).isEqualTo(2);
        assertThat(boundsA.endColumn()).isEqualTo(2);

        GridArea.AreaBounds boundsD = area.boundsFor("D");
        assertThat(boundsD.row()).isEqualTo(2);
        assertThat(boundsD.column()).isEqualTo(0);
        assertThat(boundsD.rowSpan()).isEqualTo(1);
        assertThat(boundsD.columnSpan()).isEqualTo(3);
    }

    @Test
    @DisplayName("parses empty cells with dot notation")
    void parsesEmptyCells() {
        GridArea area = GridArea.parse("A . B", ". . .");

        assertThat(area.rows()).isEqualTo(2);
        assertThat(area.columns()).isEqualTo(3);
        assertThat(area.areaNames()).containsExactly("A", "B");
        assertThat(area.areaCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("parses multi-character area names")
    void parsesMultiCharacterNames() {
        GridArea area = GridArea.parse("header header header",
                                       "nav main main",
                                       "footer footer footer");

        assertThat(area.areaNames()).containsExactly("header", "nav", "main", "footer");

        GridArea.AreaBounds header = area.boundsFor("header");
        assertThat(header.columnSpan()).isEqualTo(3);

        GridArea.AreaBounds main = area.boundsFor("main");
        assertThat(main.column()).isEqualTo(1);
        assertThat(main.columnSpan()).isEqualTo(2);
    }

    @Test
    @DisplayName("parses names with underscores and digits")
    void parsesNamesWithUnderscoresAndDigits() {
        GridArea area = GridArea.parse("area_1 area2");

        assertThat(area.areaNames()).containsExactly("area_1", "area2");
    }

    @Test
    @DisplayName("toTemplates returns original templates")
    void toTemplatesReturnsOriginal() {
        String[] templates = {"A A B", "C C D"};
        GridArea area = GridArea.parse(templates);

        assertThat(area.toTemplates()).containsExactly("A A B", "C C D");
    }

    @Test
    @DisplayName("throws on empty template")
    void throwsOnEmptyTemplate() {
        assertThatThrownBy(() -> GridArea.parse())
            .isInstanceOf(LayoutException.class)
            .hasMessageContaining("cannot be empty");
    }

    @Test
    @DisplayName("throws on null template array")
    void throwsOnNullTemplateArray() {
        assertThatThrownBy(() -> GridArea.parse((String[]) null))
            .isInstanceOf(LayoutException.class)
            .hasMessageContaining("cannot be empty");
    }

    @Test
    @DisplayName("throws on empty row template")
    void throwsOnEmptyRowTemplate() {
        assertThatThrownBy(() -> GridArea.parse("A B", ""))
            .isInstanceOf(LayoutException.class)
            .hasMessageContaining("Row 1 template cannot be empty");
    }

    @Test
    @DisplayName("throws on unequal column counts")
    void throwsOnUnequalColumnCounts() {
        assertThatThrownBy(() -> GridArea.parse("A B", "A B C"))
            .isInstanceOf(LayoutException.class)
            .hasMessageContaining("Row 1 has 3 columns but expected 2");
    }

    @Test
    @DisplayName("throws on invalid area name starting with digit")
    void throwsOnInvalidNameStartingWithDigit() {
        assertThatThrownBy(() -> GridArea.parse("1A B"))
            .isInstanceOf(LayoutException.class)
            .hasMessageContaining("Invalid area name '1A'")
            .hasMessageContaining("row 0, column 0");
    }

    @Test
    @DisplayName("throws on invalid area name with special characters")
    void throwsOnInvalidNameWithSpecialChars() {
        assertThatThrownBy(() -> GridArea.parse("A-B C"))
            .isInstanceOf(LayoutException.class)
            .hasMessageContaining("Invalid area name 'A-B'");
    }

    @Test
    @DisplayName("throws on non-rectangular area (L-shape)")
    void throwsOnNonRectangularArea() {
        // L-shape: 'A' at (0,0), (0,1), (1,0) - adjacent but not a rectangle
        assertThatThrownBy(() -> GridArea.parse("A A B", "A B B"))
            .isInstanceOf(LayoutException.class)
            .hasMessageContaining("Area 'A' does not form a rectangle");
    }

    @Test
    @DisplayName("throws on disconnected area (gap between occurrences)")
    void throwsOnDisconnectedArea() {
        // 'A' at rows 0 and 2, separated by row 1 - disconnected
        assertThatThrownBy(() -> GridArea.parse("A A B", "C C C", "A A D"))
            .isInstanceOf(LayoutException.class)
            .hasMessageContaining("Area 'A' is not contiguous")
            .hasMessageContaining("disconnected");
    }

    @Test
    @DisplayName("throws on disconnected area in same row")
    void throwsOnDisconnectedAreaSameRow() {
        // 'A' at cols 0 and 2, separated by col 1 - disconnected
        assertThatThrownBy(() -> GridArea.parse("A B A"))
            .isInstanceOf(LayoutException.class)
            .hasMessageContaining("Area 'A' is not contiguous")
            .hasMessageContaining("disconnected");
    }

    @Test
    @DisplayName("boundsFor returns null for unknown area")
    void boundsForReturnsNullForUnknown() {
        GridArea area = GridArea.parse("A B");

        assertThat(area.boundsFor("C")).isNull();
        assertThat(area.boundsFor("unknown")).isNull();
    }

    @Test
    @DisplayName("AreaBounds toString is readable")
    void areaBoundsToString() {
        GridArea area = GridArea.parse("A A", "A A");
        GridArea.AreaBounds bounds = area.boundsFor("A");

        assertThat(bounds.toString()).contains("row=0", "col=0", "rowSpan=2", "colSpan=2");
    }

    @Test
    @DisplayName("handles extra whitespace in templates")
    void handlesExtraWhitespace() {
        GridArea area = GridArea.parse("  A   B  ", " C    D ");

        assertThat(area.columns()).isEqualTo(2);
        assertThat(area.areaNames()).containsExactly("A", "B", "C", "D");
    }
}

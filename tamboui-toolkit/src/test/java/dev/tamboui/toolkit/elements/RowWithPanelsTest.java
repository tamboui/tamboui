/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Row/Column preferredSize respecting length constraints.
 */
class RowWithPanelsTest {

    @Test
    @DisplayName("Row preferredSize should respect length constraint")
    void rowPreferredSizeRespectsLengthConstraint() {
        // Regression test: Row.preferredSize() must respect .length() constraint
        Row r = row(
            panel(() -> text("A")).rounded().length(10),
            panel(() -> text("B")).rounded().length(10)
        ).length(4);  // Height constraint

        Size size = r.preferredSize(-1, -1, RenderContext.empty());

        // Row should return height=4 (from its .length(4) constraint),
        // not height=3 (max of panels' preferred heights)
        assertThat(size.height())
            .as("Row with .length(4) should have preferredHeight of 4")
            .isEqualTo(4);
    }

    @Test
    @DisplayName("Column preferredSize should respect length constraint")
    void columnPreferredSizeRespectsLengthConstraint() {
        // Regression test: Column.preferredSize() must respect .length() constraint
        Column c = column(
            text("A"),
            text("B")
        ).length(5);  // Height constraint

        Size size = c.preferredSize(-1, -1, RenderContext.empty());

        // Column should return height=5 (from its .length(5) constraint),
        // not height=2 (sum of children heights)
        assertThat(size.height())
            .as("Column with .length(5) should have preferredHeight of 5")
            .isEqualTo(5);
    }
}

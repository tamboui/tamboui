/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown.internal;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.widgets.table.Cell;
import dev.tamboui.widgets.table.Row;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownTableWidthsTest {

    @Test
    @DisplayName("columns that fit get the width of their widest cell")
    void columnsThatFitKeepTheirWidth() {
        Row header = Row.from(Cell.from("Field"), Cell.from("Value"));
        Row row = Row.from(Cell.from("Order ID"), Cell.from("ORD-1003"));
        assertThat(MarkdownLayout.columnWidths(header, Collections.singletonList(row), 2, 80))
            .containsExactly(8, 8);
    }

    @Test
    @DisplayName("when columns do not fit, the narrow ones keep their width and the wide ones share the rest")
    void wideColumnsShareWhatIsLeft() {
        Row row = Row.from(Cell.from("id"), Cell.from(repeat(70)), Cell.from(repeat(40)));
        int[] widths = MarkdownLayout.columnWidths(null, Collections.singletonList(row), 3, 60);
        // 60 minus 2 spaces between columns: "id" keeps 2, the other two share 56
        assertThat(widths).containsExactly(2, 28, 28);
        assertThat(Arrays.stream(widths).sum() + 2).isLessThanOrEqualTo(60);
    }

    private static String repeat(int n) {
        char[] c = new char[n];
        Arrays.fill(c, 'x');
        return new String(c);
    }
}

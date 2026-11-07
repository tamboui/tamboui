/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownLayoutTest {

    @Test
    @DisplayName("calculateColumnWidths sizes to content when all columns fit")
    void sizesToContentWhenAllFit() {
        int[] desired = {8, 70};
        int available = 149;

        int[] result = MarkdownLayout.calculateColumnWidths(desired, available);

        assertThat(result).containsExactly(8, 70);
    }

    @Test
    @DisplayName("calculateColumnWidths keeps narrow columns and shares remainder among wide columns")
    void narrowColumnsKeepWidthWideColumnsShareRemainder() {
        // desired: 8 and 70, available: 59 (from total width 60 with 1 spacing)
        int[] desired = {8, 70};
        int available = 59;

        int[] result = MarkdownLayout.calculateColumnWidths(desired, available);

        // narrow column (8) keeps its width; wide column (70) gets remainder: 59 - 8 = 51
        assertThat(result).containsExactly(8, 51);
    }

    @Test
    @DisplayName("calculateColumnWidths handles multiple narrow and wide columns")
    void multipleNarrowAndWideColumns() {
        // desired: [10, 20, 80], available: 60
        int[] desired = {10, 20, 80};
        int available = 60;

        int[] result = MarkdownLayout.calculateColumnWidths(desired, available);

        // narrow cols 10 and 20 keep width; wide col gets remainder: 60 - 30 = 30
        assertThat(result).containsExactly(10, 20, 30);
    }

    @Test
    @DisplayName("calculateColumnWidths shares remainder evenly among multiple wide columns")
    void multipleWideColumnsShareRemainder() {
        // desired: [5, 50, 50], available: 55
        int[] desired = {5, 50, 50};
        int available = 55;

        int[] result = MarkdownLayout.calculateColumnWidths(desired, available);

        // narrow col (5) keeps width; remaining 50 is shared: 25 each
        assertThat(result).containsExactly(5, 25, 25);
    }

    @Test
    @DisplayName("calculateColumnWidths distributes non-even remainder to earlier columns")
    void distributesNonEvenRemainder() {
        // desired: [5, 50, 50], available: 56
        int[] desired = {5, 50, 50};
        int available = 56;

        int[] result = MarkdownLayout.calculateColumnWidths(desired, available);

        // remaining 51: base 25 each, remainder 1 goes to earlier wide column
        assertThat(result).containsExactly(5, 26, 25);
    }

    @Test
    @DisplayName("calculateColumnWidths handles all columns being wide")
    void allColumnsWide() {
        int[] desired = {30, 40};
        int available = 20;

        int[] result = MarkdownLayout.calculateColumnWidths(desired, available);

        // 20 split between 2 wide columns: 10 each
        assertThat(result).containsExactly(10, 10);
    }

    @Test
    @DisplayName("calculateColumnWidths handles zero or negative available width")
    void zeroOrNegativeAvailableWidth() {
        int[] desired = {10, 20};

        assertThat(MarkdownLayout.calculateColumnWidths(desired, 0)).containsExactly(0, 0);
        assertThat(MarkdownLayout.calculateColumnWidths(desired, -5)).containsExactly(0, 0);
    }

    @Test
    @DisplayName("calculateColumnWidths handles empty input")
    void emptyInput() {
        assertThat(MarkdownLayout.calculateColumnWidths(new int[0], 100)).isEmpty();
    }
}

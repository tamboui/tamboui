/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.common;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScrollPadding}.
 */
class ScrollPaddingTest {

    private static int[] ones(int count) {
        int[] heights = new int[count];
        Arrays.fill(heights, 1);
        return heights;
    }

    @Test
    @DisplayName("Without padding, scrolls only when the selection leaves the viewport")
    void noPaddingScrollsAtEdge() {
        assertThat(ScrollPadding.computeOffset(0, ones(20), 9, 0, 10)).isEqualTo(0);
        assertThat(ScrollPadding.computeOffset(0, ones(20), 10, 0, 10)).isEqualTo(1);
        assertThat(ScrollPadding.computeOffset(5, ones(20), 5, 0, 10)).isEqualTo(5);
        assertThat(ScrollPadding.computeOffset(5, ones(20), 4, 0, 10)).isEqualTo(4);
    }

    @Test
    @DisplayName("Scrolls down before the selection reaches the last visible row")
    void paddingScrollsDownEarly() {
        assertThat(ScrollPadding.computeOffset(0, ones(20), 7, 2, 10)).isEqualTo(0);
        assertThat(ScrollPadding.computeOffset(0, ones(20), 8, 2, 10)).isEqualTo(1);
    }

    @Test
    @DisplayName("Scrolls up before the selection reaches the first visible row")
    void paddingScrollsUpEarly() {
        assertThat(ScrollPadding.computeOffset(5, ones(20), 7, 2, 10)).isEqualTo(5);
        assertThat(ScrollPadding.computeOffset(5, ones(20), 6, 2, 10)).isEqualTo(4);
    }

    @Test
    @DisplayName("Selection still reaches the first and last rows at the ends of the content")
    void paddingStopsAtContentEnds() {
        assertThat(ScrollPadding.computeOffset(0, ones(20), 0, 3, 10)).isEqualTo(0);
        assertThat(ScrollPadding.computeOffset(10, ones(20), 19, 3, 10)).isEqualTo(10);
        assertThat(ScrollPadding.computeOffset(0, ones(20), 19, 3, 10)).isEqualTo(10);
    }

    @Test
    @DisplayName("Padding too large for the viewport is reduced so the selection stays centred")
    void oversizedPaddingIsReduced() {
        assertThat(ScrollPadding.computeOffset(0, ones(20), 10, 100, 5)).isEqualTo(8);
        assertThat(ScrollPadding.computeOffset(8, ones(20), 11, 100, 5)).isEqualTo(9);
    }

    @Test
    @DisplayName("Padding counts items, not rows, for multi-line items")
    void paddingCountsItems() {
        int[] heights = {2, 2, 2, 2, 2, 2};
        assertThat(ScrollPadding.computeOffset(0, heights, 2, 1, 8)).isEqualTo(0);
        assertThat(ScrollPadding.computeOffset(0, heights, 3, 1, 8)).isEqualTo(2);
    }

    @Test
    @DisplayName("Content shorter than the viewport never scrolls")
    void shortContentNeverScrolls() {
        assertThat(ScrollPadding.computeOffset(3, ones(4), 3, 2, 10)).isEqualTo(0);
    }

    @Test
    @DisplayName("Rejects nonsense input by clamping")
    void clampsInvalidInput() {
        assertThat(ScrollPadding.computeOffset(5, new int[0], 0, 2, 10)).isEqualTo(0);
        assertThat(ScrollPadding.computeOffset(0, ones(20), 50, 0, 10)).isEqualTo(10);
        assertThat(ScrollPadding.computeOffset(5, ones(20), -3, 0, 10)).isEqualTo(0);
        assertThat(ScrollPadding.computeOffset(0, ones(20), 9, -2, 10)).isEqualTo(0);
        assertThat(ScrollPadding.computeOffset(-4, ones(20), 0, 0, 10)).isEqualTo(0);
    }

    @Test
    @DisplayName("Huge padding does not overflow and is reduced to fit the viewport")
    void hugePaddingDoesNotOverflow() {
        assertThat(ScrollPadding.computeOffset(0, ones(20), 1, Integer.MAX_VALUE, 5)).isEqualTo(0);
        assertThat(ScrollPadding.computeOffset(0, ones(20), 10, Integer.MAX_VALUE, 5)).isEqualTo(8);
        assertThat(ScrollPadding.computeOffset(0, ones(20), 19, Integer.MAX_VALUE, 5)).isEqualTo(15);
    }
}

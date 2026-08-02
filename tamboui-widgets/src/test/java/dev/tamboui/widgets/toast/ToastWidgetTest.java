/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

import static dev.tamboui.assertj.BufferAssertions.assertThat;

class ToastWidgetTest {

    @Test
    @DisplayName("Renders info toast with side rails and progress bar")
    void rendersInfoToast() {
        Rect area = new Rect(0, 0, 24, 5);
        Buffer buffer = Buffer.empty(area);

        ToastRenderSnapshot snapshot = new ToastRenderSnapshot(
                "t1",
                ToastType.INFO,
                "Saved",
                Arrays.asList("File written"),
                0.5,
                BorderMode.SIDE_RAILS,
                TitleLayout.COMPACT,
                TitleSeparator.DOT,
                TitleAlignment.START,
                true,
                ProgressStyle.FULL_BLOCK,
                Style.EMPTY.fg(Color.BLUE),
                Style.EMPTY.bold(),
                Style.EMPTY,
                Style.EMPTY.fg(Color.BLUE));

        ToastWidget.INSTANCE.render(area, buffer, snapshot);

        assertThat(buffer).at(0, 0).hasSymbol("│");
        assertThat(buffer).at(23, 0).hasSymbol("│");
        Assertions.assertThat(rowText(buffer, 1, 1, 22)).contains("Saved");
        Assertions.assertThat(buffer.get(1, 3).symbol()).isIn("█", "▌", "▋", "▊", "▉", "▏", "▎", "▍", " ");
    }

    @Test
    @DisplayName("Renders full border mode")
    void rendersFullBorder() {
        Rect area = new Rect(0, 0, 16, 4);
        Buffer buffer = Buffer.empty(area);

        ToastRenderSnapshot snapshot = new ToastRenderSnapshot(
                "t2",
                ToastType.ERROR,
                null,
                Arrays.asList("Failed"),
                1.0,
                BorderMode.FULL,
                TitleLayout.COMPACT,
                TitleSeparator.NONE,
                TitleAlignment.START,
                false,
                ProgressStyle.MINIMAL,
                Style.EMPTY.fg(Color.RED),
                Style.EMPTY,
                Style.EMPTY,
                Style.EMPTY.fg(Color.RED));

        ToastWidget.INSTANCE.render(area, buffer, snapshot);

        assertThat(buffer).at(0, 0).hasSymbol("┌");
        assertThat(buffer).at(15, 0).hasSymbol("┐");
    }

    private static String rowText(Buffer buffer, int x, int y, int width) {
        StringBuilder row = new StringBuilder();
        for (int col = 0; col < width; col++) {
            row.append(buffer.get(x + col, y).symbol());
        }
        return row.toString();
    }
}

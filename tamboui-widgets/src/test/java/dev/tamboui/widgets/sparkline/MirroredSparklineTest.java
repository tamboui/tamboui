/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.sparkline;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.block.Block;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MirroredSparklineTest {

    @Test
    @DisplayName("Centre row renders separator")
    void centreRowRendersSeparator() {
        // 3-row area: top series, centre separator, bottom series
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(8)
                .bottomData(8)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasSymbolAt(0, 1, "─");
    }

    @Test
    @DisplayName("Top series renders above centre, bottom empty when zero")
    void topSeriesRendersAboveCentre() {
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(8)
                .bottomData(0)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // row 0 = top full bar, row 1 = centre separator, row 2 = bottom empty
        assertThat(buffer).hasContent("█", "─", " ");
    }

    @Test
    @DisplayName("Bottom series renders below centre, top empty when zero")
    void bottomSeriesRendersBelowCentre() {
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(0)
                .bottomData(8)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // row 0 = top empty, row 1 = centre separator, row 2 = bottom full bar
        assertThat(buffer).hasContent(" ", "─", "█");
    }

    @Test
    @DisplayName("Top and bottom styles applied to correct rows")
    void stylesAppliedToCorrectRows() {
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(8)
                .bottomData(8)
                .topStyle(Style.EMPTY.fg(Color.GREEN))
                .bottomStyle(Style.EMPTY.fg(Color.BLUE))
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasForegroundAt(0, 0, Color.GREEN);
        assertThat(buffer).hasForegroundAt(0, 2, Color.BLUE);
    }

    @Test
    @DisplayName("Y-axis labels rendered when showYAxis is true")
    void yAxisLabelsRendered() {
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(8)
                .bottomData(8)
                .max(8)
                .showYAxis(true)
                .build();
        // 6 wide: 4 label cols + 2 data cols; 3 rows
        Rect area = new Rect(0, 0, 6, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Row 0: max label "   8" then bar; row 1: "   0" then separator; row 2: "   8" then bar
        assertThat(buffer).hasContent(
                "   8█ ",
                "   0─ ",
                "   8█ "
        );
    }

    @Test
    @DisplayName("Y-axis labels absent when showYAxis is false")
    void yAxisLabelsAbsentWhenDisabled() {
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(8)
                .bottomData(8)
                .max(8)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // x=0 is the bar column — should have a bar symbol, not a label character
        assertThat(buffer).hasContent("█", "─", "█");
    }

    @Test
    @DisplayName("X-axis labels rendered below chart body")
    void xAxisLabelsRendered() {
        // 8 data points, 8-wide area → labels don't overlap
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(0, 0, 0, 0, 0, 0, 0, 0)
                .bottomData(0, 0, 0, 0, 0, 0, 0, 0)
                .showYAxis(false)
                .xLabels("-7s", "now")
                .build();
        // height 4: 3 chart rows + 1 x-axis row
        Rect area = new Rect(0, 0, 8, 4);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // x-axis at y=3: "-7s" at col 0, "now" right-aligned ending at col 7
        assertThat(buffer).hasSymbolAt(0, 3, "-");
        assertThat(buffer).hasSymbolAt(1, 3, "7");
        assertThat(buffer).hasSymbolAt(2, 3, "s");
        assertThat(buffer).hasSymbolAt(5, 3, "n");
        assertThat(buffer).hasSymbolAt(7, 3, "w");
    }

    @Test
    @DisplayName("Block border wraps the chart")
    void blockBorderWrapsChart() {
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(8)
                .bottomData(8)
                .showYAxis(false)
                .block(Block.bordered())
                .build();
        Rect area = new Rect(0, 0, 5, 5);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer)
                .hasSymbolAt(0, 0, "┌")
                .hasSymbolAt(4, 0, "┐")
                .hasSymbolAt(0, 4, "└")
                .hasSymbolAt(4, 4, "┘");
    }

    @Test
    @DisplayName("Empty area does not throw")
    void emptyAreaDoesNotThrow() {
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(1, 2, 3)
                .bottomData(1, 2, 3)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 5));

        assertThatCode(() -> chart.render(area, buffer)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Explicit max scales both series to full bars")
    void explicitMaxScalesBothSeries() {
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(100)
                .bottomData(100)
                .max(100)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasContent("█", "─", "█");
    }

    @Test
    @DisplayName("autoMax clears explicit max and rescales from data")
    void autoMaxClearsExplicitMax() {
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(50)
                .bottomData(100)
                .max(200)
                .autoMax() // revert to data max = 100
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // bottom = 100 = max → full bar
        assertThat(buffer).hasSymbolAt(0, 2, "█");
    }

    @Test
    @DisplayName("topData from List accepted")
    void topDataFromList() {
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(Arrays.asList(8L))
                .bottomData(new long[0])
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasSymbolAt(0, 0, "█");
    }

    @Test
    @DisplayName("bottomData from List accepted")
    void bottomDataFromList() {
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(new long[0])
                .bottomData(Arrays.asList(8L))
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasSymbolAt(0, 2, "█");
    }

    @Test
    @DisplayName("from(long[], long[]) factory builds with defaults")
    void fromArrayFactory() {
        // Default includes showYAxis=true (4-char label) so area must be wide enough
        MirroredSparkline chart = MirroredSparkline.from(new long[]{8}, new long[]{8});
        Rect area = new Rect(0, 0, 6, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Bar appears at col 4 (after the 4-char Y-axis label)
        assertThat(buffer).hasSymbolAt(4, 0, "█");
        assertThat(buffer).hasSymbolAt(4, 1, "─");
        assertThat(buffer).hasSymbolAt(4, 2, "█");
    }

    @Test
    @DisplayName("from(List, List) factory builds with defaults")
    void fromListFactory() {
        // Default includes showYAxis=true (4-char label) so area must be wide enough
        MirroredSparkline chart = MirroredSparkline.from(Arrays.asList(8L), Arrays.asList(8L));
        Rect area = new Rect(0, 0, 6, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasSymbolAt(4, 0, "█");
        assertThat(buffer).hasSymbolAt(4, 1, "─");
        assertThat(buffer).hasSymbolAt(4, 2, "█");
    }

    @Test
    @DisplayName("RIGHT_TO_LEFT renders newest data at left column")
    void rightToLeftNewestDataAtLeft() {
        // data: [0, 8] — newest value is 8 (last element)
        // LEFT_TO_RIGHT:  col 0 = 0 (empty), col 1 = 8 (full)
        // RIGHT_TO_LEFT:  col 0 = 8 (full),  col 1 = 0 (empty)
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(0, 8)
                .bottomData(0, 8)
                .showYAxis(false)
                .direction(Sparkline.RenderDirection.RIGHT_TO_LEFT)
                .build();
        Rect area = new Rect(0, 0, 2, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Newest (8) at left, oldest (0) at right
        assertThat(buffer).hasSymbolAt(0, 0, "█"); // top full at left
        assertThat(buffer).hasSymbolAt(1, 0, " "); // top empty at right
        assertThat(buffer).hasSymbolAt(0, 2, "█"); // bottom full at left
        assertThat(buffer).hasSymbolAt(1, 2, " "); // bottom empty at right
    }

    @Test
    @DisplayName("RIGHT_TO_LEFT x-axis labels are mirrored")
    void rightToLeftXAxisLabels() {
        // 8 ticks; "-7s" should land on the right, "now" on the left
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(0, 0, 0, 0, 0, 0, 0, 0)
                .bottomData(0, 0, 0, 0, 0, 0, 0, 0)
                .showYAxis(false)
                .xLabels("-7s", "now")
                .direction(Sparkline.RenderDirection.RIGHT_TO_LEFT)
                .build();
        Rect area = new Rect(0, 0, 8, 4);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // "now" at col 0 (left), "-7s" right-aligned ending at col 7 (right)
        assertThat(buffer).hasSymbolAt(0, 3, "n");
        assertThat(buffer).hasSymbolAt(2, 3, "w");
        assertThat(buffer).hasSymbolAt(5, 3, "-");
        assertThat(buffer).hasSymbolAt(7, 3, "s");
    }
}

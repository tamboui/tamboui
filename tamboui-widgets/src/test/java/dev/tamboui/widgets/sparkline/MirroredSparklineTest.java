/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.sparkline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.block.Block;

import static org.assertj.core.api.Assertions.*;

class MirroredSparklineTest {

    @Test
    @DisplayName("Centre row renders separator")
    void centreRowRendersSeparator() {
        // 3-row area: top, centre, bottom
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(8)
                .bottomData(8)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer.get(0, 1).symbol()).isEqualTo("─");
    }

    @Test
    @DisplayName("Top series renders above centre")
    void topSeriesRendersAboveCentre() {
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(8)
                .bottomData(0)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // row 0 is above centre (row 1) — full bar for value == max
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("█");
        // row 2 (below centre) — no bottom data
        assertThat(buffer.get(0, 2).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Bottom series renders below centre")
    void bottomSeriesRendersBelowCentre() {
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(0)
                .bottomData(8)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // row 0 (above centre) — no top data
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
        // row 2 (below centre) — full bar
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("Top and bottom styles applied to correct rows")
    void stylesAppliedToCorrectRows() {
        Style topStyle = Style.EMPTY.fg(Color.GREEN);
        Style bottomStyle = Style.EMPTY.fg(Color.BLUE);

        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(8)
                .bottomData(8)
                .topStyle(topStyle)
                .bottomStyle(bottomStyle)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.GREEN);
        assertThat(buffer.get(0, 2).style().fg()).contains(Color.BLUE);
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
        // Wide enough for label (4 chars) + at least 1 data column
        Rect area = new Rect(0, 0, 6, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Row 0: max label "   8" at x=0..3
        String topLabel = ""
                + buffer.get(0, 0).symbol()
                + buffer.get(1, 0).symbol()
                + buffer.get(2, 0).symbol()
                + buffer.get(3, 0).symbol();
        assertThat(topLabel).isEqualTo("   8");

        // Centre row: "   0"
        String centreLabel = ""
                + buffer.get(0, 1).symbol()
                + buffer.get(1, 1).symbol()
                + buffer.get(2, 1).symbol()
                + buffer.get(3, 1).symbol();
        assertThat(centreLabel).isEqualTo("   0");
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
        Rect area = new Rect(0, 0, 6, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // x=0 should be the bar column, not a label — expect a bar symbol
        assertThat(buffer.get(0, 0).symbol()).isNotEqualTo(" "); // has a bar
        // and no "8" label at position 3
        assertThat(buffer.get(3, 0).symbol()).isNotEqualTo("8");
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

        // x-axis is at y=3; "-7s" starts at col 0
        assertThat(buffer.get(0, 3).symbol()).isEqualTo("-");
        assertThat(buffer.get(1, 3).symbol()).isEqualTo("7");
        assertThat(buffer.get(2, 3).symbol()).isEqualTo("s");
        // "now" right-aligned ending at last tick (col 7): n=5, o=6, w=7
        assertThat(buffer.get(5, 3).symbol()).isEqualTo("n");
        assertThat(buffer.get(7, 3).symbol()).isEqualTo("w");
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

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("┌");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("┐");
        assertThat(buffer.get(0, 4).symbol()).isEqualTo("└");
        assertThat(buffer.get(4, 4).symbol()).isEqualTo("┘");
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
    @DisplayName("Explicit max scales both series")
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

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("█"); // top full
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("█"); // bottom full
    }

    @Test
    @DisplayName("autoMax clears explicit max")
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
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("topData from List accepted")
    void topDataFromList() {
        java.util.List<Long> data = java.util.Arrays.asList(8L);
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(data)
                .bottomData(new long[0])
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("bottomData from List accepted")
    void bottomDataFromList() {
        java.util.List<Long> data = java.util.Arrays.asList(8L);
        MirroredSparkline chart = MirroredSparkline.builder()
                .topData(new long[0])
                .bottomData(data)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer.get(0, 2).symbol()).isEqualTo("█");
    }
}

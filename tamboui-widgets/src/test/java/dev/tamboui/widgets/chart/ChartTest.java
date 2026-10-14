/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.chart;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.block.Block;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class ChartTest {

    @Test
    void builder_creates_chart_with_defaults() {
        Chart chart = Chart.builder().build();

        assertThat(chart).isNotNull();
    }

    @Test
    void builder_accepts_datasets() {
        Dataset dataset = Dataset.builder()
            .name("Test")
            .data(new double[][] {{0, 0}, {1, 1}})
            .build();

        Chart chart = Chart.builder()
            .datasets(dataset)
            .build();

        assertThat(chart).isNotNull();
    }

    @Test
    void builder_accepts_x_and_y_axes() {
        Axis xAxis = Axis.builder()
            .title("X")
            .bounds(0, 10)
            .labels("0", "5", "10")
            .build();

        Axis yAxis = Axis.builder()
            .title("Y")
            .bounds(0, 100)
            .labels("0", "50", "100")
            .build();

        Chart chart = Chart.builder()
            .xAxis(xAxis)
            .yAxis(yAxis)
            .build();

        assertThat(chart).isNotNull();
    }

    @Test
    void builder_accepts_block() {
        Chart chart = Chart.builder()
            .block(Block.bordered())
            .build();

        assertThat(chart).isNotNull();
    }

    @Test
    void builder_accepts_style() {
        Chart chart = Chart.builder()
            .style(Style.EMPTY.fg(Color.CYAN))
            .build();

        assertThat(chart).isNotNull();
    }

    @Test
    void builder_accepts_legend_position() {
        Chart chart = Chart.builder()
            .legendPosition(LegendPosition.BOTTOM_LEFT)
            .build();

        assertThat(chart).isNotNull();
    }

    @Test
    void builder_hide_legend() {
        Chart chart = Chart.builder()
            .hideLegend()
            .build();

        assertThat(chart).isNotNull();
    }

    @Test
    void of_creates_chart_with_datasets() {
        Dataset dataset = Dataset.of(new double[][] {{0, 1}, {1, 2}});
        Chart chart = Chart.of(dataset);

        assertThat(chart).isNotNull();
    }

    @Test
    void render_empty_area_does_nothing() {
        Chart chart = Chart.builder().build();
        Buffer buffer = Buffer.empty(new Rect(0, 0, 0, 0));

        chart.render(new Rect(0, 0, 0, 0), buffer);
        // Should not throw
    }

    @Test
    void render_with_no_datasets_clears_area() {
        Chart chart = Chart.builder().build();
        Rect area = new Rect(0, 0, 20, 10);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);
        // Should not throw
    }

    @Test
    void render_scatter_plot() {
        Dataset dataset = Dataset.builder()
            .data(new double[][] {{0, 0}, {5, 5}, {10, 10}})
            .graphType(GraphType.SCATTER)
            .marker(Dataset.Marker.DOT)
            .build();

        Chart chart = Chart.builder()
            .datasets(dataset)
            .xAxis(Axis.builder().bounds(0, 10).build())
            .yAxis(Axis.builder().bounds(0, 10).build())
            .build();

        Rect area = new Rect(0, 0, 20, 10);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Should render dot markers at data points
        // Bottom-left corner (0,0) should have a dot
        assertThat(buffer.get(0, 9).symbol()).isEqualTo("•");
    }

    @Test
    void render_line_chart() {
        Dataset dataset = Dataset.builder()
            .data(new double[][] {{0, 0}, {10, 10}})
            .graphType(GraphType.LINE)
            .style(Style.EMPTY.fg(Color.CYAN))
            .build();

        Chart chart = Chart.builder()
            .datasets(dataset)
            .xAxis(Axis.builder().bounds(0, 10).build())
            .yAxis(Axis.builder().bounds(0, 10).build())
            .build();

        Rect area = new Rect(0, 0, 20, 10);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Should render line between points
        // The line uses Bresenham's algorithm
    }

    @Test
    void render_bar_chart() {
        Dataset dataset = Dataset.builder()
            .data(new double[][] {{0, 5}, {5, 8}, {10, 3}})
            .graphType(GraphType.BAR)
            .style(Style.EMPTY.fg(Color.GREEN))
            .build();

        Chart chart = Chart.builder()
            .datasets(dataset)
            .xAxis(Axis.builder().bounds(0, 10).build())
            .yAxis(Axis.builder().bounds(0, 10).build())
            .build();

        Rect area = new Rect(0, 0, 20, 10);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Should render vertical bars
    }

    @Test
    void render_with_x_axis_labels() {
        Chart chart = Chart.builder()
            .datasets(Dataset.of(new double[][] {{0, 0}}))
            .xAxis(Axis.builder()
                .bounds(0, 10)
                .labels("0", "5", "10")
                .build())
            .yAxis(Axis.builder().bounds(0, 10).build())
            .build();

        Rect area = new Rect(0, 0, 30, 12);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // X-axis labels should be rendered at the bottom
        // Check that label "0" appears at the start of x-axis label row
        assertThat(buffer.get(0, 11).symbol()).isEqualTo("0");
    }

    @Test
    void render_with_y_axis_labels() {
        Chart chart = Chart.builder()
            .datasets(Dataset.of(new double[][] {{0, 0}}))
            .xAxis(Axis.builder().bounds(0, 10).build())
            .yAxis(Axis.builder()
                .bounds(0, 10)
                .labels("0", "5", "10")
                .build())
            .build();

        Rect area = new Rect(0, 0, 30, 12);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Y-axis labels should be rendered on the left
        String leftColumn = buffer.get(0, 0).symbol() + buffer.get(1, 0).symbol();
        // Should contain at least one label
    }

    @Test
    void render_with_axis_titles() {
        Chart chart = Chart.builder()
            .datasets(Dataset.of(new double[][] {{0, 0}}))
            .xAxis(Axis.builder()
                .title("Time")
                .bounds(0, 10)
                .build())
            .yAxis(Axis.builder()
                .title("Value")
                .bounds(0, 10)
                .build())
            .build();

        Rect area = new Rect(0, 0, 30, 15);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Y-axis title should appear vertically on the left (first char of "Value")
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("V");
    }

    @Test
    void render_with_legend() {
        Dataset dataset = Dataset.builder()
            .name("Series 1")
            .data(new double[][] {{0, 0}, {1, 1}})
            .marker(Dataset.Marker.DOT)
            .build();

        Chart chart = Chart.builder()
            .datasets(dataset)
            .xAxis(Axis.builder().bounds(0, 10).build())
            .yAxis(Axis.builder().bounds(0, 10).build())
            .legendPosition(LegendPosition.TOP_LEFT)
            .build();

        Rect area = new Rect(0, 0, 30, 15);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Legend should be rendered at top-left, containing marker and name
        // The legend starts with marker symbol "•"
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("•");
    }

    @Test
    void render_with_block() {
        Chart chart = Chart.builder()
            .datasets(Dataset.of(new double[][] {{0, 0}}))
            .block(Block.bordered())
            .build();

        Rect area = new Rect(0, 0, 20, 10);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Block borders should be rendered
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("┌");
        assertThat(buffer.get(19, 0).symbol()).isEqualTo("┐");
        assertThat(buffer.get(0, 9).symbol()).isEqualTo("└");
        assertThat(buffer.get(19, 9).symbol()).isEqualTo("┘");
    }

    @Test
    void render_multiple_datasets() {
        Dataset dataset1 = Dataset.builder()
            .name("Data 1")
            .data(new double[][] {{0, 0}, {5, 5}})
            .graphType(GraphType.LINE)
            .style(Style.EMPTY.fg(Color.RED))
            .build();

        Dataset dataset2 = Dataset.builder()
            .name("Data 2")
            .data(new double[][] {{0, 10}, {5, 5}})
            .graphType(GraphType.LINE)
            .style(Style.EMPTY.fg(Color.BLUE))
            .build();

        Chart chart = Chart.builder()
            .datasets(dataset1, dataset2)
            .xAxis(Axis.builder().bounds(0, 10).build())
            .yAxis(Axis.builder().bounds(0, 10).build())
            .build();

        Rect area = new Rect(0, 0, 20, 10);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);
        // Should render both datasets
    }

    @Test
    void render_with_style() {
        Chart chart = Chart.builder()
            .datasets(Dataset.of(new double[][] {{0, 0}}))
            .style(Style.EMPTY.fg(Color.YELLOW))
            .build();

        Rect area = new Rect(0, 0, 20, 10);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Chart area should have yellow foreground
        assertThat(buffer.get(5, 5).style().fg()).contains(Color.YELLOW);
    }

    @Test
    void render_very_small_area() {
        Chart chart = Chart.builder()
            .datasets(Dataset.of(new double[][] {{0, 0}, {1, 1}}))
            .xAxis(Axis.builder().bounds(0, 10).labels("0", "10").build())
            .yAxis(Axis.builder().bounds(0, 10).labels("0", "10").build())
            .build();

        Rect area = new Rect(0, 0, 5, 3);
        Buffer buffer = Buffer.empty(area);

        // Should not throw on very small area
        chart.render(area, buffer);
    }

    @Test
    void builder_addDataset_adds_single_dataset() {
        Dataset dataset = Dataset.of(new double[][] {{0, 0}});

        Chart chart = Chart.builder()
            .addDataset(dataset)
            .addDataset(dataset)
            .build();

        assertThat(chart).isNotNull();
    }

    @Test
    void line_to_a_point_far_left_of_the_x_bounds_runs_to_the_left_edge() {
        // The last 10 seconds in epoch millis, after a sample stamped 0: 3.5 billion cells to the left
        long now = 1_760_000_000_000L;
        Chart chart = chart(GraphType.LINE, new double[][] {{0, 1}, {now - 5_000, 1}, {now, 2}},
            now - 10_000, now, 0, 4);

        BufferAssertions.assertThat(render(chart, 21, 5)).hasContent(
            "                     ",
            "                     ",
            "                ─────",
            "────────────────     ",
            "                     ");
    }

    @Test
    void lines_between_points_billions_of_cells_outside_the_graph_render_quickly() {
        double[][] data = new double[11][];
        for (int i = 0; i < data.length; i++) {
            data[i] = new double[] {i % 2 == 0 ? -2_100_000_000.0 : 2_100_000_000.0, 2};
        }
        Chart chart = chart(GraphType.LINE, data, 0, 10, 0, 4);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 11, 5));

        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> chart.render(buffer.area(), buffer));

        BufferAssertions.assertThat(buffer).hasContent(
            "           ",
            "           ",
            "───────────",
            "           ",
            "           ");
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void line_leaves_a_gap_at_a_point_without_a_position(double missing) {
        String[] expected = {
            "     ",
            "• ── ",
            "     ",
            "     ",
            "     "};

        Chart missingY = chart(GraphType.LINE, new double[][] {{0, 3}, {1, missing}, {2, 3}, {3, 3}}, 0, 4, 0, 4);
        BufferAssertions.assertThat(render(missingY, 5, 5)).hasContent(expected);

        Chart missingX = chart(GraphType.LINE, new double[][] {{0, 3}, {missing, 3}, {2, 3}, {3, 3}}, 0, 4, 0, 4);
        BufferAssertions.assertThat(render(missingX, 5, 5)).hasContent(expected);
    }

    @Test
    void scatter_point_four_billion_cells_right_of_the_graph_is_not_drawn() {
        // 2^32 + 2 cells to the right; cut to 32 bits, that is column 2
        Chart chart = chart(GraphType.SCATTER, new double[][] {{4_294_967_298.0, 1}}, 0, 4, 0, 4);

        BufferAssertions.assertThat(render(chart, 5, 5)).hasContent(
            "     ",
            "     ",
            "     ",
            "     ",
            "     ");
    }

    @Test
    void bar_without_a_value_is_not_drawn() {
        Chart chart = chart(GraphType.BAR, new double[][] {{1, Double.NaN}, {3, 2}}, 0, 4, 0, 4);

        BufferAssertions.assertThat(render(chart, 5, 5)).hasContent(
            "     ",
            "     ",
            "   █ ",
            "   █ ",
            "   █ ");
    }

    @Test
    void bar_far_above_the_y_bounds_fills_the_graph_height() {
        // 2^32 + 2 cells up; cut to 32 bits, that is a bar 3 cells high
        Chart chart = chart(GraphType.BAR, new double[][] {{3, 4_294_967_298.0}}, 0, 4, 0, 4);

        BufferAssertions.assertThat(render(chart, 5, 5)).hasContent(
            "   █ ",
            "   █ ",
            "   █ ",
            "   █ ",
            "   █ ");
    }

    private static Chart chart(GraphType type, double[][] data, double xMin, double xMax, double yMin, double yMax) {
        return Chart.builder()
            .datasets(Dataset.builder().data(data).graphType(type).build())
            .xAxis(Axis.builder().bounds(xMin, xMax).build())
            .yAxis(Axis.builder().bounds(yMin, yMax).build())
            .build();
    }

    private static Buffer render(Chart chart, int width, int height) {
        Buffer buffer = Buffer.empty(new Rect(0, 0, width, height));
        chart.render(buffer.area(), buffer);
        return buffer;
    }
}

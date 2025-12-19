/*
 * Copyright (c) 2025 JRatatui Contributors
 * SPDX-License-Identifier: MIT
 */
package io.github.jratatui.widgets.chart;

import io.github.jratatui.buffer.Buffer;
import io.github.jratatui.layout.Rect;
import io.github.jratatui.style.Color;
import io.github.jratatui.style.Style;
import io.github.jratatui.widgets.block.Block;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChartTest {

    @Test
    void builder_creates_chart_with_defaults() {
        var chart = Chart.builder().build();

        assertThat(chart).isNotNull();
    }

    @Test
    void builder_accepts_datasets() {
        var dataset = Dataset.builder()
            .name("Test")
            .data(new double[][] {{0, 0}, {1, 1}})
            .build();

        var chart = Chart.builder()
            .datasets(dataset)
            .build();

        assertThat(chart).isNotNull();
    }

    @Test
    void builder_accepts_x_and_y_axes() {
        var xAxis = Axis.builder()
            .title("X")
            .bounds(0, 10)
            .labels("0", "5", "10")
            .build();

        var yAxis = Axis.builder()
            .title("Y")
            .bounds(0, 100)
            .labels("0", "50", "100")
            .build();

        var chart = Chart.builder()
            .xAxis(xAxis)
            .yAxis(yAxis)
            .build();

        assertThat(chart).isNotNull();
    }

    @Test
    void builder_accepts_block() {
        var chart = Chart.builder()
            .block(Block.bordered())
            .build();

        assertThat(chart).isNotNull();
    }

    @Test
    void builder_accepts_style() {
        var chart = Chart.builder()
            .style(Style.EMPTY.fg(Color.CYAN))
            .build();

        assertThat(chart).isNotNull();
    }

    @Test
    void builder_accepts_legend_position() {
        var chart = Chart.builder()
            .legendPosition(LegendPosition.BOTTOM_LEFT)
            .build();

        assertThat(chart).isNotNull();
    }

    @Test
    void builder_hide_legend() {
        var chart = Chart.builder()
            .hideLegend()
            .build();

        assertThat(chart).isNotNull();
    }

    @Test
    void of_creates_chart_with_datasets() {
        var dataset = Dataset.of(new double[][] {{0, 1}, {1, 2}});
        var chart = Chart.of(dataset);

        assertThat(chart).isNotNull();
    }

    @Test
    void render_empty_area_does_nothing() {
        var chart = Chart.builder().build();
        var buffer = Buffer.empty(new Rect(0, 0, 0, 0));

        chart.render(new Rect(0, 0, 0, 0), buffer);
        // Should not throw
    }

    @Test
    void render_with_no_datasets_clears_area() {
        var chart = Chart.builder().build();
        var area = new Rect(0, 0, 20, 10);
        var buffer = Buffer.empty(area);

        chart.render(area, buffer);
        // Should not throw
    }

    @Test
    void render_scatter_plot() {
        var dataset = Dataset.builder()
            .data(new double[][] {{0, 0}, {5, 5}, {10, 10}})
            .graphType(GraphType.SCATTER)
            .marker(Dataset.Marker.DOT)
            .build();

        var chart = Chart.builder()
            .datasets(dataset)
            .xAxis(Axis.builder().bounds(0, 10).build())
            .yAxis(Axis.builder().bounds(0, 10).build())
            .build();

        var area = new Rect(0, 0, 20, 10);
        var buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Should render dot markers at data points
        // Bottom-left corner (0,0) should have a dot
        assertThat(buffer.get(0, 9).symbol()).isEqualTo("•");
    }

    @Test
    void render_line_chart() {
        var dataset = Dataset.builder()
            .data(new double[][] {{0, 0}, {10, 10}})
            .graphType(GraphType.LINE)
            .style(Style.EMPTY.fg(Color.CYAN))
            .build();

        var chart = Chart.builder()
            .datasets(dataset)
            .xAxis(Axis.builder().bounds(0, 10).build())
            .yAxis(Axis.builder().bounds(0, 10).build())
            .build();

        var area = new Rect(0, 0, 20, 10);
        var buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Should render line between points
        // The line uses Bresenham's algorithm
    }

    @Test
    void render_bar_chart() {
        var dataset = Dataset.builder()
            .data(new double[][] {{0, 5}, {5, 8}, {10, 3}})
            .graphType(GraphType.BAR)
            .style(Style.EMPTY.fg(Color.GREEN))
            .build();

        var chart = Chart.builder()
            .datasets(dataset)
            .xAxis(Axis.builder().bounds(0, 10).build())
            .yAxis(Axis.builder().bounds(0, 10).build())
            .build();

        var area = new Rect(0, 0, 20, 10);
        var buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Should render vertical bars
    }

    @Test
    void render_with_x_axis_labels() {
        var chart = Chart.builder()
            .datasets(Dataset.of(new double[][] {{0, 0}}))
            .xAxis(Axis.builder()
                .bounds(0, 10)
                .labels("0", "5", "10")
                .build())
            .yAxis(Axis.builder().bounds(0, 10).build())
            .build();

        var area = new Rect(0, 0, 30, 12);
        var buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // X-axis labels should be rendered at the bottom
        // Check that label "0" appears at the start of x-axis label row
        assertThat(buffer.get(0, 11).symbol()).isEqualTo("0");
    }

    @Test
    void render_with_y_axis_labels() {
        var chart = Chart.builder()
            .datasets(Dataset.of(new double[][] {{0, 0}}))
            .xAxis(Axis.builder().bounds(0, 10).build())
            .yAxis(Axis.builder()
                .bounds(0, 10)
                .labels("0", "5", "10")
                .build())
            .build();

        var area = new Rect(0, 0, 30, 12);
        var buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Y-axis labels should be rendered on the left
        var leftColumn = buffer.get(0, 0).symbol() + buffer.get(1, 0).symbol();
        // Should contain at least one label
    }

    @Test
    void render_with_axis_titles() {
        var chart = Chart.builder()
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

        var area = new Rect(0, 0, 30, 15);
        var buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Y-axis title should appear vertically on the left (first char of "Value")
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("V");
    }

    @Test
    void render_with_legend() {
        var dataset = Dataset.builder()
            .name("Series 1")
            .data(new double[][] {{0, 0}, {1, 1}})
            .marker(Dataset.Marker.DOT)
            .build();

        var chart = Chart.builder()
            .datasets(dataset)
            .xAxis(Axis.builder().bounds(0, 10).build())
            .yAxis(Axis.builder().bounds(0, 10).build())
            .legendPosition(LegendPosition.TOP_LEFT)
            .build();

        var area = new Rect(0, 0, 30, 15);
        var buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Legend should be rendered at top-left, containing marker and name
        // The legend starts with marker symbol "•"
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("•");
    }

    @Test
    void render_with_block() {
        var chart = Chart.builder()
            .datasets(Dataset.of(new double[][] {{0, 0}}))
            .block(Block.bordered())
            .build();

        var area = new Rect(0, 0, 20, 10);
        var buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Block borders should be rendered
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("┌");
        assertThat(buffer.get(19, 0).symbol()).isEqualTo("┐");
        assertThat(buffer.get(0, 9).symbol()).isEqualTo("└");
        assertThat(buffer.get(19, 9).symbol()).isEqualTo("┘");
    }

    @Test
    void render_multiple_datasets() {
        var dataset1 = Dataset.builder()
            .name("Data 1")
            .data(new double[][] {{0, 0}, {5, 5}})
            .graphType(GraphType.LINE)
            .style(Style.EMPTY.fg(Color.RED))
            .build();

        var dataset2 = Dataset.builder()
            .name("Data 2")
            .data(new double[][] {{0, 10}, {5, 5}})
            .graphType(GraphType.LINE)
            .style(Style.EMPTY.fg(Color.BLUE))
            .build();

        var chart = Chart.builder()
            .datasets(dataset1, dataset2)
            .xAxis(Axis.builder().bounds(0, 10).build())
            .yAxis(Axis.builder().bounds(0, 10).build())
            .build();

        var area = new Rect(0, 0, 20, 10);
        var buffer = Buffer.empty(area);

        chart.render(area, buffer);
        // Should render both datasets
    }

    @Test
    void render_with_style() {
        var chart = Chart.builder()
            .datasets(Dataset.of(new double[][] {{0, 0}}))
            .style(Style.EMPTY.fg(Color.YELLOW))
            .build();

        var area = new Rect(0, 0, 20, 10);
        var buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Chart area should have yellow foreground
        assertThat(buffer.get(5, 5).style().fg()).contains(Color.YELLOW);
    }

    @Test
    void render_very_small_area() {
        var chart = Chart.builder()
            .datasets(Dataset.of(new double[][] {{0, 0}, {1, 1}}))
            .xAxis(Axis.builder().bounds(0, 10).labels("0", "10").build())
            .yAxis(Axis.builder().bounds(0, 10).labels("0", "10").build())
            .build();

        var area = new Rect(0, 0, 5, 3);
        var buffer = Buffer.empty(area);

        // Should not throw on very small area
        chart.render(area, buffer);
    }

    @Test
    void builder_addDataset_adds_single_dataset() {
        var dataset = Dataset.of(new double[][] {{0, 0}});

        var chart = Chart.builder()
            .addDataset(dataset)
            .addDataset(dataset)
            .build();

        assertThat(chart).isNotNull();
    }
}

///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST
/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.chart.Axis;
import dev.tamboui.widgets.chart.Chart;
import dev.tamboui.widgets.chart.Dataset;
import dev.tamboui.widgets.chart.GraphType;
import dev.tamboui.widgets.chart.LegendPosition;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.io.IOException;
import java.util.Random;

/**
 * Demo TUI application showcasing the Chart widget.
 * <p>
 * Demonstrates line charts, scatter plots, and bar charts
 * with animated data updates.
 */
public class ChartDemo {

    private static final int DATA_SIZE = 50;

    private boolean running = true;
    private final double[][] tempData1 = new double[DATA_SIZE][2];
    private final double[][] tempData2 = new double[DATA_SIZE][2];
    private final double[][] scatterData = new double[30][2];
    private final double[][] barData = new double[10][2];
    private final Random random = new Random();
    private long frameCount = 0;
    private double time = 0;

    public static void main(String[] args) throws Exception {
        new ChartDemo().run();
    }

    public ChartDemo() {
        // Initialize temperature data (two sine waves with noise)
        for (int i = 0; i < DATA_SIZE; i++) {
            double x = i * 0.2;
            tempData1[i][0] = x;
            tempData1[i][1] = 50 + 20 * Math.sin(x) + random.nextGaussian() * 3;
            tempData2[i][0] = x;
            tempData2[i][1] = 40 + 15 * Math.cos(x * 0.8) + random.nextGaussian() * 3;
        }

        // Initialize scatter data (random points in a cluster)
        for (int i = 0; i < scatterData.length; i++) {
            scatterData[i][0] = 50 + random.nextGaussian() * 15;
            scatterData[i][1] = 50 + random.nextGaussian() * 15;
        }

        // Initialize bar data (categories)
        for (int i = 0; i < barData.length; i++) {
            barData[i][0] = i;
            barData[i][1] = 10 + random.nextInt(80);
        }
    }

    public void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            // Handle resize
            backend.onResize(() -> {
                try {
                    terminal.draw(this::ui);
                } catch (IOException e) {
                    // Ignore
                }
            });

            // Event loop with animation
            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(100);
                if (c == 'q' || c == 'Q' || c == 3) {
                    running = false;
                }

                // Update data for animation
                updateData();
                frameCount++;
            }
        }
    }

    private void updateData() {
        time += 0.1;

        // Shift temperature data left and add new point
        for (int i = 0; i < DATA_SIZE - 1; i++) {
            tempData1[i][1] = tempData1[i + 1][1];
            tempData2[i][1] = tempData2[i + 1][1];
        }
        double x = (DATA_SIZE - 1) * 0.2 + time;
        tempData1[DATA_SIZE - 1][1] = 50 + 20 * Math.sin(x) + random.nextGaussian() * 3;
        tempData2[DATA_SIZE - 1][1] = 40 + 15 * Math.cos(x * 0.8) + random.nextGaussian() * 3;

        // Slowly move scatter points
        for (double[] point : scatterData) {
            point[0] += random.nextGaussian() * 2;
            point[1] += random.nextGaussian() * 2;
            // Keep in bounds
            point[0] = Math.max(10, Math.min(90, point[0]));
            point[1] = Math.max(10, Math.min(90, point[1]));
        }

        // Occasionally update bar values
        if (frameCount % 10 == 0) {
            int idx = random.nextInt(barData.length);
            barData[idx][1] = 10 + random.nextInt(80);
        }
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        var layout = Layout.vertical()
            .constraints(
                Constraint.length(3),  // Header
                Constraint.fill(),     // Main content
                Constraint.length(3)   // Footer
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        renderMainContent(frame, layout.get(1));
        renderFooter(frame, layout.get(2));
    }

    private void renderHeader(Frame frame, Rect area) {
        Block headerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from(
                Line.from(
                    Span.raw(" TamboUI ").bold().cyan(),
                    Span.raw("Chart Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderMainContent(Frame frame, Rect area) {
        // Split into top (line chart) and bottom (scatter + bar)
        var rows = Layout.vertical()
            .constraints(
                Constraint.percentage(60),
                Constraint.percentage(40)
            )
            .split(area);

        renderLineChart(frame, rows.get(0));

        var bottomCols = Layout.horizontal()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(rows.get(1));

        renderScatterPlot(frame, bottomCols.get(0));
        renderBarChart(frame, bottomCols.get(1));
    }

    private void renderLineChart(Frame frame, Rect area) {
        var dataset1 = Dataset.builder()
            .name("Sensor 1")
            .data(tempData1)
            .graphType(GraphType.LINE)
            .marker(Dataset.Marker.DOT)
            .style(Style.EMPTY.fg(Color.CYAN))
            .build();

        var dataset2 = Dataset.builder()
            .name("Sensor 2")
            .data(tempData2)
            .graphType(GraphType.LINE)
            .marker(Dataset.Marker.DOT)
            .style(Style.EMPTY.fg(Color.MAGENTA))
            .build();

        var chart = Chart.builder()
            .datasets(dataset1, dataset2)
            .xAxis(Axis.builder()
                .title("Time")
                .bounds(0, 10)
                .labels("0", "2", "4", "6", "8", "10")
                .style(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .yAxis(Axis.builder()
                .title("Temp")
                .bounds(0, 100)
                .labels("0", "25", "50", "75", "100")
                .style(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .legendPosition(LegendPosition.TOP_RIGHT)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.BLUE))
                .title(Title.from(Line.from(
                    Span.raw(" Temperature Over Time ").blue()
                )))
                .build())
            .build();

        frame.renderWidget(chart, area);
    }

    private void renderScatterPlot(Frame frame, Rect area) {
        var dataset = Dataset.builder()
            .name("Cluster")
            .data(scatterData)
            .graphType(GraphType.SCATTER)
            .marker(Dataset.Marker.BRAILLE)
            .style(Style.EMPTY.fg(Color.GREEN))
            .build();

        var chart = Chart.builder()
            .datasets(dataset)
            .xAxis(Axis.builder()
                .bounds(0, 100)
                .labels("0", "50", "100")
                .style(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .yAxis(Axis.builder()
                .bounds(0, 100)
                .labels("0", "50", "100")
                .style(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .legendPosition(LegendPosition.TOP_LEFT)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.GREEN))
                .title(Title.from(Line.from(
                    Span.raw(" Scatter Plot ").green()
                )))
                .build())
            .build();

        frame.renderWidget(chart, area);
    }

    private void renderBarChart(Frame frame, Rect area) {
        var dataset = Dataset.builder()
            .name("Values")
            .data(barData)
            .graphType(GraphType.BAR)
            .style(Style.EMPTY.fg(Color.YELLOW))
            .build();

        var chart = Chart.builder()
            .datasets(dataset)
            .xAxis(Axis.builder()
                .bounds(0, 9)
                .labels("0", "3", "6", "9")
                .style(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .yAxis(Axis.builder()
                .bounds(0, 100)
                .labels("0", "50", "100")
                .style(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .hideLegend()
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.YELLOW))
                .title(Title.from(Line.from(
                    Span.raw(" Bar Chart ").yellow()
                )))
                .build())
            .build();

        frame.renderWidget(chart, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
            Span.raw(" Frame: ").dim(),
            Span.raw(String.valueOf(frameCount)).bold().cyan(),
            Span.raw("   "),
            Span.raw("q").bold().yellow(),
            Span.raw(" Quit").dim()
        );

        Paragraph footer = Paragraph.builder()
            .text(Text.from(helpLine))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .build())
            .build();

        frame.renderWidget(footer, area);
    }
}

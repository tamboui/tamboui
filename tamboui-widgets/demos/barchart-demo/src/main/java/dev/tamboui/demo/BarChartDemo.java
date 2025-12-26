///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline:LATEST

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Direction;
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
import dev.tamboui.widgets.barchart.Bar;
import dev.tamboui.widgets.barchart.BarChart;
import dev.tamboui.widgets.barchart.BarGroup;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.io.IOException;
import java.util.Random;

/**
 * Demo TUI application showcasing the BarChart widget.
 */
public class BarChartDemo {

    private boolean running = true;
    private final Random random = new Random();
    private int selectedChart = 0;
    private long frameCount = 0;

    // Sales data
    private final long[] q1Sales = {120, 150, 180};
    private final long[] q2Sales = {140, 160, 200};
    private final long[] q3Sales = {110, 170, 190};
    private final long[] q4Sales = {160, 180, 220};

    public static void main(String[] args) throws Exception {
        new BarChartDemo().run();
    }

    public void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            backend.onResize(() -> {
                try {
                    terminal.draw(this::ui);
                } catch (IOException e) {
                    // Ignore
                }
            });

            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(100);
                if (c == -2 || c == -1) {
                    continue;
                }

                handleInput(c);
                frameCount++;
            }
        }
    }

    private void handleInput(int c) {
        switch (c) {
            case 'q', 'Q', 3 -> running = false;
            case '1' -> selectedChart = 0;
            case '2' -> selectedChart = 1;
            case '3' -> selectedChart = 2;
            case 'r', 'R' -> randomizeData();
        }
    }

    private void randomizeData() {
        for (int i = 0; i < 3; i++) {
            q1Sales[i] = 100 + random.nextInt(150);
            q2Sales[i] = 100 + random.nextInt(150);
            q3Sales[i] = 100 + random.nextInt(150);
            q4Sales[i] = 100 + random.nextInt(150);
        }
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        var layout = Layout.vertical()
            .constraints(
                Constraint.length(3),  // Header
                Constraint.fill(),     // Chart
                Constraint.length(3)   // Footer
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        renderChart(frame, layout.get(1));
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
                    Span.raw("BarChart Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderChart(Frame frame, Rect area) {
        switch (selectedChart) {
            case 0 -> renderVerticalGroupedChart(frame, area);
            case 1 -> renderHorizontalChart(frame, area);
            case 2 -> renderSimpleChart(frame, area);
        }
    }

    private void renderVerticalGroupedChart(Frame frame, Rect area) {
        var chart = BarChart.builder()
            .data(
                BarGroup.builder()
                    .label("Q1")
                    .addBar(Bar.builder().value(q1Sales[0]).label("Jan").style(Style.EMPTY.fg(Color.RED)).build())
                    .addBar(Bar.builder().value(q1Sales[1]).label("Feb").style(Style.EMPTY.fg(Color.GREEN)).build())
                    .addBar(Bar.builder().value(q1Sales[2]).label("Mar").style(Style.EMPTY.fg(Color.BLUE)).build())
                    .build(),
                BarGroup.builder()
                    .label("Q2")
                    .addBar(Bar.builder().value(q2Sales[0]).label("Apr").style(Style.EMPTY.fg(Color.RED)).build())
                    .addBar(Bar.builder().value(q2Sales[1]).label("May").style(Style.EMPTY.fg(Color.GREEN)).build())
                    .addBar(Bar.builder().value(q2Sales[2]).label("Jun").style(Style.EMPTY.fg(Color.BLUE)).build())
                    .build(),
                BarGroup.builder()
                    .label("Q3")
                    .addBar(Bar.builder().value(q3Sales[0]).label("Jul").style(Style.EMPTY.fg(Color.RED)).build())
                    .addBar(Bar.builder().value(q3Sales[1]).label("Aug").style(Style.EMPTY.fg(Color.GREEN)).build())
                    .addBar(Bar.builder().value(q3Sales[2]).label("Sep").style(Style.EMPTY.fg(Color.BLUE)).build())
                    .build(),
                BarGroup.builder()
                    .label("Q4")
                    .addBar(Bar.builder().value(q4Sales[0]).label("Oct").style(Style.EMPTY.fg(Color.RED)).build())
                    .addBar(Bar.builder().value(q4Sales[1]).label("Nov").style(Style.EMPTY.fg(Color.GREEN)).build())
                    .addBar(Bar.builder().value(q4Sales[2]).label("Dec").style(Style.EMPTY.fg(Color.BLUE)).build())
                    .build()
            )
            .barWidth(3)
            .barGap(1)
            .groupGap(2)
            .max(250)
            .valueStyle(Style.EMPTY.fg(Color.WHITE))
            .labelStyle(Style.EMPTY.fg(Color.YELLOW))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.GREEN))
                .title(Title.from(Line.from(
                    Span.raw(" Quarterly Sales (Vertical Grouped) ").green()
                )))
                .build())
            .build();

        frame.renderWidget(chart, area);
    }

    private void renderHorizontalChart(Frame frame, Rect area) {
        var chart = BarChart.builder()
            .data(
                BarGroup.of(
                    Bar.builder().value(q1Sales[0] + q1Sales[1] + q1Sales[2]).label("Q1").style(Style.EMPTY.fg(Color.CYAN)).build(),
                    Bar.builder().value(q2Sales[0] + q2Sales[1] + q2Sales[2]).label("Q2").style(Style.EMPTY.fg(Color.YELLOW)).build(),
                    Bar.builder().value(q3Sales[0] + q3Sales[1] + q3Sales[2]).label("Q3").style(Style.EMPTY.fg(Color.MAGENTA)).build(),
                    Bar.builder().value(q4Sales[0] + q4Sales[1] + q4Sales[2]).label("Q4").style(Style.EMPTY.fg(Color.GREEN)).build()
                )
            )
            .direction(Direction.HORIZONTAL)
            .barWidth(2)
            .barGap(1)
            .max(700)
            .valueStyle(Style.EMPTY.fg(Color.WHITE))
            .labelStyle(Style.EMPTY.fg(Color.YELLOW))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.MAGENTA))
                .title(Title.from(Line.from(
                    Span.raw(" Quarterly Totals (Horizontal) ").magenta()
                )))
                .build())
            .build();

        frame.renderWidget(chart, area);
    }

    private void renderSimpleChart(Frame frame, Rect area) {
        var chart = BarChart.builder()
            .data(BarGroup.of(
                Bar.of(q1Sales[0], "Jan"),
                Bar.of(q1Sales[1], "Feb"),
                Bar.of(q1Sales[2], "Mar"),
                Bar.of(q2Sales[0], "Apr"),
                Bar.of(q2Sales[1], "May"),
                Bar.of(q2Sales[2], "Jun")
            ))
            .barWidth(5)
            .barGap(1)
            .max(250)
            .barStyle(Style.EMPTY.fg(Color.CYAN))
            .valueStyle(Style.EMPTY.fg(Color.WHITE))
            .labelStyle(Style.EMPTY.fg(Color.YELLOW))
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .title(Title.from(Line.from(
                    Span.raw(" Monthly Sales (Simple) ").cyan()
                )))
                .build())
            .build();

        frame.renderWidget(chart, area);
    }

    private void renderFooter(Frame frame, Rect area) {
        String chartName = switch (selectedChart) {
            case 0 -> "Vertical Grouped";
            case 1 -> "Horizontal";
            case 2 -> "Simple";
            default -> "Unknown";
        };

        Line helpLine = Line.from(
            Span.raw(" Chart: ").dim(),
            Span.raw(chartName).bold().cyan(),
            Span.raw("   "),
            Span.raw("1-3").bold().yellow(),
            Span.raw(" Switch  ").dim(),
            Span.raw("r").bold().yellow(),
            Span.raw(" Randomize  ").dim(),
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

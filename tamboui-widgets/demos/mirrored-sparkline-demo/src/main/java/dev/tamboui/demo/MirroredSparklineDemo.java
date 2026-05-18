///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-widgets:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.util.Random;

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
import dev.tamboui.widgets.paragraph.Paragraph;
import dev.tamboui.widgets.sparkline.MirroredSparkline;
import dev.tamboui.widgets.sparkline.Sparkline;

/**
 * Demo TUI application showcasing the MirroredSparkline widget.
 * <p>
 * Demonstrates a dual-series mirrored chart displaying network IN/OUT and
 * disk read/write rates, with Y-axis labels, X-axis timestamps, and animated
 * data updates.
 */
public class MirroredSparklineDemo {

    private static final int DATA_SIZE = 60;

    private boolean running = true;
    private final long[] netIn = new long[DATA_SIZE];
    private final long[] netOut = new long[DATA_SIZE];
    private final long[] diskRead = new long[DATA_SIZE];
    private final long[] diskWrite = new long[DATA_SIZE];
    private final Random random = new Random();
    private long frameCount = 0;

    /**
     * Demo entry point.
     *
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new MirroredSparklineDemo().run();
    }

    private MirroredSparklineDemo() {
        for (int i = 0; i < DATA_SIZE; i++) {
            netIn[i] = 20 + random.nextInt(60);
            netOut[i] = 10 + random.nextInt(40);
            diskRead[i] = 5 + random.nextInt(50);
            diskWrite[i] = 5 + random.nextInt(30);
        }
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
    public void run() throws Exception {
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            backend.onResize(() -> terminal.draw(this::ui));

            while (running) {
                terminal.draw(this::ui);

                int c = backend.read(100);
                if (c == 'q' || c == 'Q' || c == 3) {
                    running = false;
                }

                updateData();
                frameCount++;
            }
        }
    }

    private void updateData() {
        System.arraycopy(netIn, 1, netIn, 0, DATA_SIZE - 1);
        System.arraycopy(netOut, 1, netOut, 0, DATA_SIZE - 1);
        System.arraycopy(diskRead, 1, diskRead, 0, DATA_SIZE - 1);
        System.arraycopy(diskWrite, 1, diskWrite, 0, DATA_SIZE - 1);

        netIn[DATA_SIZE - 1] = clamp(netIn[DATA_SIZE - 2] + random.nextInt(21) - 10, 0, 100);
        netOut[DATA_SIZE - 1] = clamp(netOut[DATA_SIZE - 2] + random.nextInt(21) - 10, 0, 100);
        diskRead[DATA_SIZE - 1] = clamp(diskRead[DATA_SIZE - 2] + random.nextInt(15) - 7, 0, 80);
        diskWrite[DATA_SIZE - 1] = clamp(diskWrite[DATA_SIZE - 2] + random.nextInt(11) - 5, 0, 60);
    }

    private long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private void ui(Frame frame) {
        Rect area = frame.area();

        var layout = Layout.vertical()
            .constraints(
                Constraint.length(3),
                Constraint.fill(),
                Constraint.length(3)
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        renderCharts(frame, layout.get(1));
        renderFooter(frame, layout.get(2));
    }

    private void renderHeader(Frame frame, Rect area) {
        Block block = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from(
                Line.from(
                    Span.raw(" TamboUI ").bold().cyan(),
                    Span.raw("MirroredSparkline Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(block, area);
    }

    private void renderCharts(Frame frame, Rect area) {
        var rows = Layout.vertical()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(area);

        renderNetworkChart(frame, rows.get(0));
        renderDiskChart(frame, rows.get(1));
    }

    private void renderNetworkChart(Frame frame, Rect area) {
        long curIn = netIn[DATA_SIZE - 1];
        long curOut = netOut[DATA_SIZE - 1];

        MirroredSparkline chart = MirroredSparkline.builder()
            .topData(netIn)
            .bottomData(netOut)
            .topForeground(Color.GREEN)
            .bottomForeground(Color.BLUE)
            .max(100)
            .showYAxis(true)
            .xLabels("-60s", "-45s", "-30s", "-15s", "now")
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .title(Title.from(Line.from(
                    Span.raw(" Network ").bold().cyan(),
                    Span.raw("IN ").green(),
                    Span.raw(String.format("%d MB/s", curIn)).bold().green(),
                    Span.raw("  OUT ").blue(),
                    Span.raw(String.format("%d MB/s ", curOut)).bold().blue()
                )))
                .build())
            .build();

        frame.renderWidget(chart, area);
    }

    private void renderDiskChart(Frame frame, Rect area) {
        long curRead = diskRead[DATA_SIZE - 1];
        long curWrite = diskWrite[DATA_SIZE - 1];

        MirroredSparkline chart = MirroredSparkline.builder()
            .topData(diskRead)
            .bottomData(diskWrite)
            .topForeground(Color.YELLOW)
            .bottomForeground(Color.MAGENTA)
            .max(100)
            .showYAxis(true)
            .barSet(Sparkline.BarSet.NINE_LEVELS)
            .direction(Sparkline.RenderDirection.LEFT_TO_RIGHT)
            .xLabels("-60s", "-45s", "-30s", "-15s", "now")
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.YELLOW))
                .title(Title.from(Line.from(
                    Span.raw(" Disk ").bold().yellow(),
                    Span.raw("READ ").yellow(),
                    Span.raw(String.format("%d MB/s", curRead)).bold().yellow(),
                    Span.raw("  WRITE ").magenta(),
                    Span.raw(String.format("%d MB/s ", curWrite)).bold().magenta()
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

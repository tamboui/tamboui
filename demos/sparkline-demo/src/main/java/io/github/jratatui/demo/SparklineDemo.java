/*
 * Copyright (c) 2025 JRatatui Contributors
 * SPDX-License-Identifier: MIT
 */
package io.github.jratatui.demo;

import io.github.jratatui.backend.jline.JLineBackend;
import io.github.jratatui.layout.Constraint;
import io.github.jratatui.layout.Layout;
import io.github.jratatui.layout.Rect;
import io.github.jratatui.style.Color;
import io.github.jratatui.style.Style;
import io.github.jratatui.terminal.Frame;
import io.github.jratatui.terminal.Terminal;
import io.github.jratatui.text.Line;
import io.github.jratatui.text.Span;
import io.github.jratatui.text.Text;
import io.github.jratatui.widgets.block.Block;
import io.github.jratatui.widgets.block.BorderType;
import io.github.jratatui.widgets.block.Borders;
import io.github.jratatui.widgets.block.Title;
import io.github.jratatui.widgets.paragraph.Paragraph;
import io.github.jratatui.widgets.sparkline.Sparkline;
import org.jline.terminal.Terminal.Signal;
import org.jline.utils.NonBlockingReader;

import java.io.IOException;
import java.util.Random;

/**
 * Demo TUI application showcasing the Sparkline widget.
 * <p>
 * Demonstrates sparklines with different styles, bar sets,
 * and animated data updates.
 */
public class SparklineDemo {

    private static final int DATA_SIZE = 60;

    private boolean running = true;
    private final long[] cpuData = new long[DATA_SIZE];
    private final long[] memoryData = new long[DATA_SIZE];
    private final long[] networkData = new long[DATA_SIZE];
    private final long[] diskData = new long[DATA_SIZE];
    private final Random random = new Random();
    private long frameCount = 0;

    public static void main(String[] args) throws Exception {
        new SparklineDemo().run();
    }

    public SparklineDemo() {
        // Initialize with some random data
        for (int i = 0; i < DATA_SIZE; i++) {
            cpuData[i] = 30 + random.nextInt(40);
            memoryData[i] = 50 + random.nextInt(30);
            networkData[i] = random.nextInt(100);
            diskData[i] = random.nextInt(50);
        }
    }

    public void run() throws Exception {
        try (JLineBackend backend = new JLineBackend()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<JLineBackend> terminal = new Terminal<>(backend);

            // Handle resize
            backend.jlineTerminal().handle(Signal.WINCH, signal -> {
                try {
                    terminal.draw(this::ui);
                } catch (IOException e) {
                    // Ignore
                }
            });

            NonBlockingReader reader = backend.jlineTerminal().reader();

            // Event loop with animation
            while (running) {
                terminal.draw(this::ui);

                int c = reader.read(100);
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
        // Shift data left
        System.arraycopy(cpuData, 1, cpuData, 0, DATA_SIZE - 1);
        System.arraycopy(memoryData, 1, memoryData, 0, DATA_SIZE - 1);
        System.arraycopy(networkData, 1, networkData, 0, DATA_SIZE - 1);
        System.arraycopy(diskData, 1, diskData, 0, DATA_SIZE - 1);

        // Add new values with some variation
        cpuData[DATA_SIZE - 1] = clamp(cpuData[DATA_SIZE - 2] + random.nextInt(21) - 10, 10, 90);
        memoryData[DATA_SIZE - 1] = clamp(memoryData[DATA_SIZE - 2] + random.nextInt(11) - 5, 40, 90);
        networkData[DATA_SIZE - 1] = clamp(networkData[DATA_SIZE - 2] + random.nextInt(31) - 15, 0, 100);
        diskData[DATA_SIZE - 1] = clamp(diskData[DATA_SIZE - 2] + random.nextInt(11) - 5, 0, 50);
    }

    private long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
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
                    Span.raw(" JRatatui ").bold().cyan(),
                    Span.raw("Sparkline Demo ").yellow()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    private void renderMainContent(Frame frame, Rect area) {
        // Split into 2x2 grid
        var rows = Layout.vertical()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(area);

        var topCols = Layout.horizontal()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(rows.get(0));

        var bottomCols = Layout.horizontal()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(rows.get(1));

        renderCpuSparkline(frame, topCols.get(0));
        renderMemorySparkline(frame, topCols.get(1));
        renderNetworkSparkline(frame, bottomCols.get(0));
        renderDiskSparkline(frame, bottomCols.get(1));
    }

    private void renderCpuSparkline(Frame frame, Rect area) {
        long current = cpuData[DATA_SIZE - 1];
        String label = String.format("CPU Usage: %d%%", current);

        Sparkline sparkline = Sparkline.builder()
            .data(cpuData)
            .max(100)
            .style(Style.EMPTY.fg(Color.GREEN))
            .barSet(Sparkline.BarSet.NINE_LEVELS)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.GREEN))
                .title(Title.from(Line.from(
                    Span.raw(" " + label + " ").green()
                )))
                .build())
            .build();

        frame.renderWidget(sparkline, area);
    }

    private void renderMemorySparkline(Frame frame, Rect area) {
        long current = memoryData[DATA_SIZE - 1];
        String label = String.format("Memory: %d%%", current);

        Sparkline sparkline = Sparkline.builder()
            .data(memoryData)
            .max(100)
            .style(Style.EMPTY.fg(Color.YELLOW))
            .barSet(Sparkline.BarSet.NINE_LEVELS)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.YELLOW))
                .title(Title.from(Line.from(
                    Span.raw(" " + label + " ").yellow()
                )))
                .build())
            .build();

        frame.renderWidget(sparkline, area);
    }

    private void renderNetworkSparkline(Frame frame, Rect area) {
        long current = networkData[DATA_SIZE - 1];
        String label = String.format("Network I/O: %d MB/s", current);

        Sparkline sparkline = Sparkline.builder()
            .data(networkData)
            .max(100)
            .style(Style.EMPTY.fg(Color.CYAN))
            .barSet(Sparkline.BarSet.THREE_LEVELS)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .title(Title.from(Line.from(
                    Span.raw(" " + label + " ").cyan()
                )))
                .build())
            .build();

        frame.renderWidget(sparkline, area);
    }

    private void renderDiskSparkline(Frame frame, Rect area) {
        long current = diskData[DATA_SIZE - 1];
        String label = String.format("Disk I/O: %d MB/s", current);

        Sparkline sparkline = Sparkline.builder()
            .data(diskData)
            .max(100)
            .style(Style.EMPTY.fg(Color.MAGENTA))
            .direction(Sparkline.RenderDirection.RIGHT_TO_LEFT)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.MAGENTA))
                .title(Title.from(Line.from(
                    Span.raw(" " + label + " (RTL) ").magenta()
                )))
                .build())
            .build();

        frame.renderWidget(sparkline, area);
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

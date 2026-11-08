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
import dev.tamboui.style.ColorGradient;
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
import dev.tamboui.widgets.sparkline.Sparkline;

/**
 * Demo TUI application showcasing the Sparkline widget.
 * <p>
 * Demonstrates sparklines at different heights — from compact single-row
 * to tall multi-row — with toggleable Y/X axis labels, bar sets,
 * render direction, and animated data updates.
 * <p>
 * Keys:
 * <ul>
 *   <li>{@code Tab} — toggle compact/expanded layout</li>
 *   <li>{@code +/-} — adjust height in compact mode</li>
 *   <li>{@code y} — toggle Y-axis labels</li>
 *   <li>{@code x} — toggle X-axis labels</li>
 *   <li>{@code q} — quit</li>
 * </ul>
 */
public class SparklineDemo {

    private static final int DATA_SIZE = 200;
    private static final String[] X_LABELS = {"-60s", "-30s", "now"};
    private static final int MIN_HEIGHT = 3;
    private static final int MAX_HEIGHT = 20;
    private static final ColorGradient CPU_GRADIENT =
            ColorGradient.of(Color.GREEN, Color.YELLOW, Color.RED);

    private boolean running = true;
    private boolean compact = true;
    private int compactHeight = 5;
    private boolean showYAxis;
    private boolean showXAxis;
    private final long[] cpuData = new long[DATA_SIZE];
    private final long[] memoryData = new long[DATA_SIZE];
    private final long[] networkData = new long[DATA_SIZE];
    private final long[] diskData = new long[DATA_SIZE];
    private final Random random = new Random();
    private long frameCount = 0;

    /**
     * Demo entry point.
     *
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new SparklineDemo().run();
    }

    private SparklineDemo() {
        for (int i = 0; i < DATA_SIZE; i++) {
            cpuData[i] = 30 + random.nextInt(40);
            memoryData[i] = 50 + random.nextInt(30);
            networkData[i] = random.nextInt(100);
            diskData[i] = random.nextInt(50);
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
                } else if (c == 9) { // Tab
                    compact = !compact;
                } else if (c == '+' || c == '=') {
                    compactHeight = Math.min(MAX_HEIGHT, compactHeight + 1);
                } else if (c == '-' || c == '_') {
                    compactHeight = Math.max(MIN_HEIGHT, compactHeight - 1);
                } else if (c == 'y' || c == 'Y') {
                    showYAxis = !showYAxis;
                } else if (c == 'x' || c == 'X') {
                    showXAxis = !showXAxis;
                }

                updateData();
                frameCount++;
            }
        }
    }

    private void updateData() {
        System.arraycopy(cpuData, 1, cpuData, 0, DATA_SIZE - 1);
        System.arraycopy(memoryData, 1, memoryData, 0, DATA_SIZE - 1);
        System.arraycopy(networkData, 1, networkData, 0, DATA_SIZE - 1);
        System.arraycopy(diskData, 1, diskData, 0, DATA_SIZE - 1);

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
                Constraint.length(3),
                Constraint.fill(),
                Constraint.length(3)
            )
            .split(area);

        renderHeader(frame, layout.get(0));
        if (compact) {
            renderCompactLayout(frame, layout.get(1));
        } else {
            renderExpandedLayout(frame, layout.get(1));
        }
        renderFooter(frame, layout.get(2));
    }

    private void renderHeader(Frame frame, Rect area) {
        String mode = compact ? "Compact (" + compactHeight + " rows)" : "Expanded";
        Block headerBlock = Block.builder()
            .borders(Borders.ALL)
            .borderType(BorderType.ROUNDED)
            .borderStyle(Style.EMPTY.fg(Color.CYAN))
            .title(Title.from(
                Line.from(
                    Span.raw(" TamboUI ").bold().cyan(),
                    Span.raw("Sparkline Demo ").yellow(),
                    Span.raw("— " + mode + " ").dim()
                )
            ).centered())
            .build();

        frame.renderWidget(headerBlock, area);
    }

    // --- Compact layout: 4 sparklines stacked at fixed height ---

    private void renderCompactLayout(Frame frame, Rect area) {
        var rows = Layout.vertical()
            .constraints(
                Constraint.length(compactHeight),
                Constraint.length(compactHeight),
                Constraint.length(compactHeight),
                Constraint.length(compactHeight),
                Constraint.fill()
            )
            .split(area);

        renderSparkline(frame, rows.get(0), "CPU (Gradient)", cpuData, Color.GREEN, CPU_GRADIENT,
                Sparkline.BarSet.NINE_LEVELS, Sparkline.RenderDirection.LEFT_TO_RIGHT);
        renderSparkline(frame, rows.get(1), "Memory", memoryData, Color.YELLOW, null,
                Sparkline.BarSet.NINE_LEVELS, Sparkline.RenderDirection.LEFT_TO_RIGHT);
        renderSparkline(frame, rows.get(2), "Network", networkData, Color.CYAN, null,
                Sparkline.BarSet.THREE_LEVELS, Sparkline.RenderDirection.LEFT_TO_RIGHT);
        renderSparkline(frame, rows.get(3), "Disk (RTL)", diskData, Color.MAGENTA, null,
                Sparkline.BarSet.NINE_LEVELS, Sparkline.RenderDirection.RIGHT_TO_LEFT);
    }

    // --- Expanded layout: 2x2 grid filling the space ---

    private void renderExpandedLayout(Frame frame, Rect area) {
        var rows = Layout.vertical()
            .constraints(
                Constraint.percentage(50),
                Constraint.percentage(50)
            )
            .split(area);

        var topCols = Layout.horizontal()
            .constraints(Constraint.percentage(50), Constraint.percentage(50))
            .split(rows.get(0));

        var bottomCols = Layout.horizontal()
            .constraints(Constraint.percentage(50), Constraint.percentage(50))
            .split(rows.get(1));

        renderSparkline(frame, topCols.get(0), "CPU (Gradient)", cpuData, Color.GREEN, CPU_GRADIENT,
                Sparkline.BarSet.NINE_LEVELS, Sparkline.RenderDirection.LEFT_TO_RIGHT);
        renderSparkline(frame, topCols.get(1), "Memory", memoryData, Color.YELLOW, null,
                Sparkline.BarSet.NINE_LEVELS, Sparkline.RenderDirection.LEFT_TO_RIGHT);
        renderSparkline(frame, bottomCols.get(0), "Network", networkData, Color.CYAN, null,
                Sparkline.BarSet.THREE_LEVELS, Sparkline.RenderDirection.LEFT_TO_RIGHT);
        renderSparkline(frame, bottomCols.get(1), "Disk (RTL)", diskData, Color.MAGENTA, null,
                Sparkline.BarSet.NINE_LEVELS, Sparkline.RenderDirection.RIGHT_TO_LEFT);
    }

    private void renderSparkline(Frame frame, Rect area, String name, long[] data,
            Color color, ColorGradient gradient, Sparkline.BarSet barSet, Sparkline.RenderDirection dir) {
        long current = data[DATA_SIZE - 1];
        String label = String.format(" %s: %d%% ", name, current);
        String[] labels = dir == Sparkline.RenderDirection.RIGHT_TO_LEFT
                ? new String[]{"now", "-30s", "-60s"}
                : X_LABELS;

        Sparkline.Builder builder = Sparkline.builder()
            .data(data)
            .max(100)
            .style(Style.EMPTY.fg(color))
            .barSet(barSet)
            .direction(dir)
            .showYAxis(showYAxis)
            .xLabels(showXAxis ? labels : null)
            .block(Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(color))
                .title(Title.from(Line.from(
                    Span.styled(label, Style.EMPTY.fg(color))
                )))
                .build());

        if (gradient != null) {
            builder.gradient(gradient);
        }

        frame.renderWidget(builder.build(), area);
    }

    private void renderFooter(Frame frame, Rect area) {
        Line helpLine = Line.from(
            Span.raw(" Frame: ").dim(),
            Span.raw(String.valueOf(frameCount)).bold().cyan(),
            Span.raw("  "),
            Span.raw("Tab").bold().yellow(),
            Span.raw(" Layout").dim(),
            Span.raw("  "),
            Span.raw("+/-").bold().yellow(),
            Span.raw(" Height").dim(),
            Span.raw("  "),
            Span.raw("y").bold().yellow(),
            Span.raw(" Y-axis").dim(),
            Span.raw(showYAxis ? " ✓" : "  ").green(),
            Span.raw("  "),
            Span.raw("x").bold().yellow(),
            Span.raw(" X-axis").dim(),
            Span.raw(showXAxis ? " ✓" : "  ").green(),
            Span.raw("  "),
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

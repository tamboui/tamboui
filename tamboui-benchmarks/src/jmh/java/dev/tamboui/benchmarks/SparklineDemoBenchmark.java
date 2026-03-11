/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.benchmarks;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import dev.tamboui.buffer.DiffResult;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.AbstractBackend;
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
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmark that replays the Sparkline Demo rendering loop at max FPS.
 * <p>
 * Measures the full rendering pipeline: widget rendering → buffer diff → backend draw.
 * Each iteration = one frame (updateData + terminal.draw).
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SparklineDemoBenchmark {

    private static final int DATA_SIZE = 60;

    @Param({"80x24", "160x50", "240x67"})
    String terminalSize;

    private Terminal<MetricsBackend> terminal;
    private MetricsBackend backend;
    private final long[] cpuData = new long[DATA_SIZE];
    private final long[] memoryData = new long[DATA_SIZE];
    private final long[] networkData = new long[DATA_SIZE];
    private final long[] diskData = new long[DATA_SIZE];
    private final Random random = new Random(42);
    private long frameCount;

    @Setup(Level.Trial)
    public void setup() {
        String[] parts = terminalSize.split("x");
        int width = Integer.parseInt(parts[0]);
        int height = Integer.parseInt(parts[1]);

        backend = new MetricsBackend(width, height);
        terminal = new Terminal<>(backend);

        // Initialize data (same as SparklineDemo)
        for (int i = 0; i < DATA_SIZE; i++) {
            cpuData[i] = 30 + random.nextInt(40);
            memoryData[i] = 50 + random.nextInt(30);
            networkData[i] = random.nextInt(100);
            diskData[i] = random.nextInt(50);
        }
        frameCount = 0;

        // First frame (full redraw) — warm up the double buffer
        terminal.draw(this::ui);
        backend.reset();
    }

    @Benchmark
    public void sparklineFrame(Blackhole bh) {
        updateData();
        frameCount++;
        backend.reset();
        terminal.draw(this::ui);
        bh.consume(backend.bytesWritten);
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

        List<Rect> layout = Layout.vertical()
                .constraints(
                        Constraint.length(3),
                        Constraint.fill(),
                        Constraint.length(3)
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
                                Span.raw("Sparkline Demo ").yellow()
                        )
                ).centered())
                .build();
        frame.renderWidget(headerBlock, area);
    }

    private void renderMainContent(Frame frame, Rect area) {
        List<Rect> rows = Layout.vertical()
                .constraints(Constraint.percentage(50), Constraint.percentage(50))
                .split(area);
        List<Rect> topCols = Layout.horizontal()
                .constraints(Constraint.percentage(50), Constraint.percentage(50))
                .split(rows.get(0));
        List<Rect> bottomCols = Layout.horizontal()
                .constraints(Constraint.percentage(50), Constraint.percentage(50))
                .split(rows.get(1));

        renderSparkline(frame, topCols.get(0), "CPU Usage", cpuData, Color.GREEN, Sparkline.BarSet.NINE_LEVELS, Sparkline.RenderDirection.LEFT_TO_RIGHT);
        renderSparkline(frame, topCols.get(1), "Memory", memoryData, Color.YELLOW, Sparkline.BarSet.NINE_LEVELS, Sparkline.RenderDirection.LEFT_TO_RIGHT);
        renderSparkline(frame, bottomCols.get(0), "Network I/O", networkData, Color.CYAN, Sparkline.BarSet.THREE_LEVELS, Sparkline.RenderDirection.LEFT_TO_RIGHT);
        renderSparkline(frame, bottomCols.get(1), "Disk I/O", diskData, Color.MAGENTA, Sparkline.BarSet.NINE_LEVELS, Sparkline.RenderDirection.RIGHT_TO_LEFT);
    }

    private void renderSparkline(Frame frame, Rect area, String name, long[] data,
                                  Color color, Sparkline.BarSet barSet, Sparkline.RenderDirection dir) {
        long current = data[DATA_SIZE - 1];
        String label = " " + name + ": " + current + "% ";
        Sparkline sparkline = Sparkline.builder()
                .data(data)
                .max(100)
                .style(Style.EMPTY.fg(color))
                .barSet(barSet)
                .direction(dir)
                .block(Block.builder()
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderStyle(Style.EMPTY.fg(color))
                        .title(Title.from(Line.from(Span.raw(label).style(Style.EMPTY.fg(color)))))
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

    /**
     * Backend that counts bytes and cursor moves for metrics.
     */
    static class MetricsBackend extends AbstractBackend {
        int bytesWritten;
        int writeCount;
        int cursorMoves;
        private final int width;
        private final int height;

        MetricsBackend(int width, int height) {
            this.width = width;
            this.height = height;
        }

        void reset() {
            bytesWritten = 0;
            writeCount = 0;
            cursorMoves = 0;
        }

        @Override
        public void writeRaw(String data) {
            bytesWritten += data.length();
            writeCount++;
            // Count CUP sequences (cursor moves)
            int idx = 0;
            while ((idx = data.indexOf("\u001b[", idx)) >= 0) {
                int end = data.indexOf('H', idx);
                if (end > idx && data.indexOf(';', idx) > idx) {
                    cursorMoves++;
                }
                idx += 2;
            }
        }

        @Override
        public void writeRaw(CharSequence data) {
            writeRaw(data.toString());
        }

        @Override
        public void writeRaw(byte[] data) {
            bytesWritten += data.length;
            writeCount++;
        }

        @Override
        public void flush() { }

        @Override
        public Size size() { return new Size(width, height); }

        @Override
        public void clear() { }

        @Override
        public void showCursor() { }

        @Override
        public void hideCursor() { }

        @Override
        public Position getCursorPosition() { return new Position(0, 0); }

        @Override
        public void enableRawMode() { }

        @Override
        public void disableRawMode() { }

        @Override
        public void enterAlternateScreen() { }

        @Override
        public void leaveAlternateScreen() { }

        @Override
        public int read(int timeoutMs) { return -1; }

        @Override
        public int peek(int timeoutMs) { return -1; }

        @Override
        public void onResize(Runnable handler) { }

        @Override
        public void close() { }
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.benchmarks;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.AbstractBackend;
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
 * Sparkline rendering loop for profiling and FPS measurement.
 * <p>
 * Two modes:
 * <ul>
 *   <li><b>Real terminal</b> (default): uses BackendFactory for end-to-end measurement
 *       including actual terminal I/O. Press 'q' to quit.</li>
 *   <li><b>Null backend</b> ({@code --null}): uses a no-op backend with volatile sink
 *       to prevent JIT dead-code elimination. Runs a fixed number of frames.</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>
 *   # Real terminal (end-to-end, press q to quit):
 *   java -cp ... SparklineProfiler
 *
 *   # Null backend (profiling, fixed frames):
 *   java -cp ... SparklineProfiler --null [width] [height] [frames]
 * </pre>
 */
public class SparklineProfiler {

    private static final int DATA_SIZE = 60;

    private final long[] cpuData = new long[DATA_SIZE];
    private final long[] memoryData = new long[DATA_SIZE];
    private final long[] networkData = new long[DATA_SIZE];
    private final long[] diskData = new long[DATA_SIZE];
    private final Random random = new Random(42);
    private long frameCount;
    private double currentFps;
    private double currentUsPerFrame;

    public static void main(String[] args) throws Exception {
        SparklineProfiler profiler = new SparklineProfiler();
        profiler.initData();

        if (args.length > 0 && "--null".equals(args[0])) {
            int width = args.length > 1 ? Integer.parseInt(args[1]) : 160;
            int height = args.length > 2 ? Integer.parseInt(args[2]) : 50;
            int frames = args.length > 3 ? Integer.parseInt(args[3]) : 500_000;
            profiler.runNull(width, height, frames);
        } else {
            profiler.runReal();
        }
    }

    private void initData() {
        for (int i = 0; i < DATA_SIZE; i++) {
            cpuData[i] = 30 + random.nextInt(40);
            memoryData[i] = 50 + random.nextInt(30);
            networkData[i] = random.nextInt(100);
            diskData[i] = random.nextInt(50);
        }
    }

    private void runReal() throws Exception {
        int durationSeconds = 10;
        try (Backend backend = BackendFactory.create()) {
            backend.enableRawMode();
            backend.enterAlternateScreen();
            backend.hideCursor();

            Terminal<Backend> terminal = new Terminal<>(backend);

            // Warmup
            for (int i = 0; i < 500; i++) {
                updateData();
                frameCount++;
                terminal.draw(this::ui);
            }

            // Measure for fixed duration
            frameCount = 0;
            long start = System.nanoTime();
            long deadline = start + durationSeconds * 1_000_000_000L;
            long reportInterval = 500_000_000L;
            long lastReport = start;
            long framesSinceReport = 0;

            while (System.nanoTime() < deadline) {
                updateData();
                frameCount++;
                terminal.draw(this::ui);
                framesSinceReport++;

                long now = System.nanoTime();
                if (now - lastReport >= reportInterval) {
                    currentFps = framesSinceReport * 1_000_000_000.0 / (now - lastReport);
                    currentUsPerFrame = (now - lastReport) / 1000.0 / framesSinceReport;
                    lastReport = now;
                    framesSinceReport = 0;
                }
            }

            long elapsed = System.nanoTime() - start;
            backend.leaveAlternateScreen();
            backend.showCursor();
            backend.disableRawMode();

            double usPerFrame = elapsed / 1000.0 / frameCount;
            double fps = 1_000_000.0 / usPerFrame;
            System.out.printf("Total: %d frames in %ds, %.1f us/frame, %.0f FPS%n",
                    frameCount, durationSeconds, usPerFrame, fps);
        }
    }

    private void runNull(int width, int height, int frames) {
        SinkBackend backend = new SinkBackend(width, height);
        Terminal<SinkBackend> terminal = new Terminal<>(backend);

        System.out.printf("Warming up %dx%d...%n", width, height);
        for (int i = 0; i < 10_000; i++) {
            updateData();
            frameCount++;
            terminal.draw(this::ui);
        }

        System.out.printf("Running %d frames at %dx%d — attach profiler now...%n", frames, width, height);
        long start = System.nanoTime();
        for (int i = 0; i < frames; i++) {
            updateData();
            frameCount++;
            terminal.draw(this::ui);
        }
        long elapsed = System.nanoTime() - start;

        double usPerFrame = elapsed / 1000.0 / frames;
        double fps = 1_000_000.0 / usPerFrame;
        System.out.printf("Done: %.1f us/frame, %.0f FPS (sink=%d)%n", usPerFrame, fps, backend.sink);
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
                .constraints(Constraint.length(3), Constraint.fill(), Constraint.length(3))
                .split(area);
        renderHeader(frame, layout.get(0));
        renderMainContent(frame, layout.get(1));
        renderFooter(frame, layout.get(2));
    }

    private void renderHeader(Frame frame, Rect area) {
        frame.renderWidget(Block.builder()
                .borders(Borders.ALL).borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.CYAN))
                .title(Title.from(Line.from(
                        Span.raw(" TamboUI ").bold().cyan(),
                        Span.raw("Sparkline Demo ").yellow()
                )).centered()).build(), area);
    }

    private void renderMainContent(Frame frame, Rect area) {
        List<Rect> rows = Layout.vertical()
                .constraints(Constraint.percentage(50), Constraint.percentage(50)).split(area);
        List<Rect> topCols = Layout.horizontal()
                .constraints(Constraint.percentage(50), Constraint.percentage(50)).split(rows.get(0));
        List<Rect> bottomCols = Layout.horizontal()
                .constraints(Constraint.percentage(50), Constraint.percentage(50)).split(rows.get(1));

        renderSparkline(frame, topCols.get(0), "CPU", cpuData, Color.GREEN, Sparkline.BarSet.NINE_LEVELS, Sparkline.RenderDirection.LEFT_TO_RIGHT);
        renderSparkline(frame, topCols.get(1), "Mem", memoryData, Color.YELLOW, Sparkline.BarSet.NINE_LEVELS, Sparkline.RenderDirection.LEFT_TO_RIGHT);
        renderSparkline(frame, bottomCols.get(0), "Net", networkData, Color.CYAN, Sparkline.BarSet.THREE_LEVELS, Sparkline.RenderDirection.LEFT_TO_RIGHT);
        renderSparkline(frame, bottomCols.get(1), "Disk", diskData, Color.MAGENTA, Sparkline.BarSet.NINE_LEVELS, Sparkline.RenderDirection.RIGHT_TO_LEFT);
    }

    private void renderSparkline(Frame frame, Rect area, String name, long[] data,
                                  Color color, Sparkline.BarSet barSet, Sparkline.RenderDirection dir) {
        long current = data[DATA_SIZE - 1];
        frame.renderWidget(Sparkline.builder()
                .data(data).max(100).style(Style.EMPTY.fg(color)).barSet(barSet).direction(dir)
                .block(Block.builder().borders(Borders.ALL).borderType(BorderType.ROUNDED)
                        .borderStyle(Style.EMPTY.fg(color))
                        .title(Title.from(Line.from(Span.raw(" " + name + ": " + current + "% ")
                                .style(Style.EMPTY.fg(color))))).build())
                .build(), area);
    }

    private void renderFooter(Frame frame, Rect area) {
        frame.renderWidget(Paragraph.builder()
                .text(Text.from(Line.from(
                        Span.raw(" Frame: ").dim(),
                        Span.raw(String.valueOf(frameCount)).bold().cyan(),
                        Span.raw("   "),
                        Span.raw((long) currentFps + " FPS").bold().green(),
                        Span.raw("  " + (long) (currentUsPerFrame * 10) / 10.0 + " us/frame").dim())))
                .block(Block.builder().borders(Borders.ALL).borderType(BorderType.ROUNDED)
                        .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY)).build())
                .build(), area);
    }

    /**
     * No-op backend with volatile sink to prevent JIT dead-code elimination.
     */
    static class SinkBackend extends AbstractBackend {
        volatile int sink;
        private final int width, height;

        SinkBackend(int w, int h) { this.width = w; this.height = h; }

        @Override public void writeRaw(String data) { sink += data.length(); }
        @Override public void writeRaw(CharSequence data) { sink += data.length(); }
        @Override public void writeRaw(byte[] data) { sink += data.length; }
        @Override public void flush() { }
        @Override public Size size() { return new Size(width, height); }
        @Override public void clear() { }
        @Override public void showCursor() { }
        @Override public void hideCursor() { }
        @Override public Position getCursorPosition() { return new Position(0, 0); }
        @Override public void enableRawMode() { }
        @Override public void disableRawMode() { }
        @Override public void enterAlternateScreen() { }
        @Override public void leaveAlternateScreen() { }
        @Override public int read(int t) { return -1; }
        @Override public int peek(int t) { return -1; }
        @Override public void onResize(Runnable h) { }
        @Override public void close() { }
    }
}

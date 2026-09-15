/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.benchmarks;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.buffer.DiffResult;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

/**
 * Benchmark measuring the realistic rendering pipeline as Terminal.draw() does it:
 * <ol>
 *   <li>clear() current buffer</li>
 *   <li>Render borders via buffer.set() (like Block widget)</li>
 *   <li>Render text content via setString() (like Paragraph/List widgets)</li>
 *   <li>diff() previous vs current</li>
 *   <li>Iterate and consume diff results</li>
 *   <li>Swap buffers</li>
 * </ol>
 * <p>
 * Simulates a typical TUI frame: bordered panel with text content inside,
 * plus a status bar at the bottom. This matches how real apps combine
 * Block (buffer.set with Unicode border chars) and text widgets (setString with ASCII).
 * <p>
 * Run with: ./gradlew :tamboui-benchmarks:jmh -Pjmh.includes='.*RenderPipeline.*'
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class RenderPipelineBenchmark {

    @Param({"80x24", "160x50", "240x67"})
    private String terminalSize;

    @Param({"5", "50"})
    private int changePercentage;

    private Buffer bufferA;
    private Buffer bufferB;
    private DiffResult diffResult;
    private int width;
    private int height;

    // Border cells (pre-built like Block widget does with BorderSet)
    private Cell borderHorizontal;
    private Cell borderVertical;
    private Cell borderTopLeft;
    private Cell borderTopRight;
    private Cell borderBottomLeft;
    private Cell borderBottomRight;

    // Inner content area (inside border)
    private int innerLeft;
    private int innerTop;
    private int innerWidth;
    private int innerHeight;

    // Pre-built text lines for inner content
    private String[] baseContentLines;
    private String[] mutatedContentLines;
    private Style contentStyle;
    private Style statusStyle;
    private String statusLine;

    @Setup(Level.Trial)
    public void setup() {
        String[] parts = terminalSize.split("x");
        width = Integer.parseInt(parts[0]);
        height = Integer.parseInt(parts[1]);
        Rect area = Rect.of(width, height);

        bufferA = Buffer.empty(area);
        bufferB = Buffer.empty(area);
        diffResult = new DiffResult(area.area());

        // Border style (like Block widget with PLAIN borders)
        Style borderStyle = Style.EMPTY.fg(Color.WHITE);
        borderHorizontal = new Cell("─", borderStyle);
        borderVertical = new Cell("│", borderStyle);
        borderTopLeft = new Cell("┌", borderStyle);
        borderTopRight = new Cell("┐", borderStyle);
        borderBottomLeft = new Cell("└", borderStyle);
        borderBottomRight = new Cell("┘", borderStyle);

        // Inner area = inside the border (leave 1 row at bottom for status bar)
        innerLeft = 1;
        innerTop = 1;
        innerWidth = width - 2;
        innerHeight = height - 3; // -2 for top/bottom border, -1 for status bar

        // Content style (some lines styled, some plain — like a list widget)
        contentStyle = Style.EMPTY;
        statusStyle = Style.EMPTY.fg(Color.GREEN);

        // Build base content lines (fill inner area with text)
        baseContentLines = new String[innerHeight];
        StringBuilder sb = new StringBuilder(innerWidth);
        for (int x = 0; x < innerWidth; x++) {
            sb.append((char) ('a' + (x % 26)));
        }
        String baseLine = sb.toString();
        for (int y = 0; y < innerHeight; y++) {
            baseContentLines[y] = baseLine;
        }

        // Build mutated lines: changePercentage of rows get different content
        int mutatedRows = Math.max(1, (innerHeight * changePercentage) / 100);
        mutatedContentLines = new String[innerHeight];
        for (int y = 0; y < innerHeight; y++) {
            if (y < mutatedRows) {
                StringBuilder msb = new StringBuilder(innerWidth);
                for (int x = 0; x < innerWidth; x++) {
                    msb.append((char) ('A' + ((x + 3) % 26)));
                }
                mutatedContentLines[y] = msb.toString();
            } else {
                mutatedContentLines[y] = baseLine;
            }
        }

        // Status bar line
        StringBuilder statusSb = new StringBuilder(width);
        String statusText = " Status: OK | Lines: " + innerHeight + " | Cols: " + innerWidth + " ";
        statusSb.append(statusText);
        for (int x = statusText.length(); x < width; x++) {
            statusSb.append(' ');
        }
        statusLine = statusSb.toString();

        // Prime bufferA with base frame
        renderFrame(bufferA, baseContentLines);

        // Validate: render mutated frame into bufferB and check diff
        bufferB.clear();
        renderFrame(bufferB, mutatedContentLines);
        bufferA.diff(bufferB, diffResult);
        int diffSize = diffResult.size();
        diffResult.clear();

        // Reset bufferB for benchmark
        bufferB.clear();

        // Re-prime bufferA
        bufferA.clear();
        renderFrame(bufferA, baseContentLines);

        System.out.println("\n=== RenderPipeline: " + terminalSize
                + " (border=" + (2 * width + 2 * (height - 2) - 4 + 4) + " cells"
                + ", inner=" + (innerWidth * innerHeight) + " cells"
                + ", status=" + width + " cells)"
                + ", " + changePercentage + "% change, " + diffSize + " diffs ===\n");
    }

    private void renderFrame(Buffer buffer, String[] contentLines) {
        // 1. Render borders (like Block widget does via buffer.set)
        // Top border
        buffer.set(0, 0, borderTopLeft);
        for (int x = 1; x < width - 1; x++) {
            buffer.set(x, 0, borderHorizontal);
        }
        buffer.set(width - 1, 0, borderTopRight);

        // Side borders
        int bottomBorderY = height - 2; // row above status bar
        for (int y = 1; y < bottomBorderY; y++) {
            buffer.set(0, y, borderVertical);
            buffer.set(width - 1, y, borderVertical);
        }

        // Bottom border (above status bar)
        buffer.set(0, bottomBorderY, borderBottomLeft);
        for (int x = 1; x < width - 1; x++) {
            buffer.set(x, bottomBorderY, borderHorizontal);
        }
        buffer.set(width - 1, bottomBorderY, borderBottomRight);

        // 2. Render text content inside border (like Paragraph/List via setString)
        for (int y = 0; y < innerHeight; y++) {
            buffer.setString(innerLeft, innerTop + y, contentLines[y], contentStyle);
        }

        // 3. Render status bar (like a styled text line via setString)
        buffer.setString(0, height - 1, statusLine, statusStyle);
    }

    /**
     * Realistic Terminal.draw() simulation:
     * clear + border rendering + text rendering + diff + consume + swap.
     */
    @Benchmark
    public void renderPipeline(Blackhole blackhole) {
        // 1. Clear current buffer (like Terminal.draw does)
        bufferB.clear();

        // 2. Render frame: borders via set() + text via setString()
        renderFrame(bufferB, mutatedContentLines);

        // 3. Diff previous vs current
        bufferA.diff(bufferB, diffResult);

        // 4. Consume diff (simulating backend.draw): coordinates once per run, then cells
        for (int run = 0, runs = diffResult.runCount(); run < runs; run++) {
            int start = diffResult.runStart(run);
            int end = start + diffResult.runLength(run);
            blackhole.consume(diffResult.xOf(start));
            blackhole.consume(diffResult.yOf(start));
            for (int i = start; i < end; i++) {
                blackhole.consume(diffResult.cellAt(i));
            }
        }

        // 5. Clear diff and swap buffers
        diffResult.clear();
        Buffer temp = bufferA;
        bufferA = bufferB;
        bufferB = temp;
    }
}

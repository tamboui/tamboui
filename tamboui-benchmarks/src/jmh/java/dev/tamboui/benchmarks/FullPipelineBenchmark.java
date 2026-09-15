/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.benchmarks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
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
import dev.tamboui.buffer.DiffResult;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;

/**
 * Benchmark measuring the full rendering pipeline:
 * 1. Diff two buffers (zero-allocation Data-Oriented Design)
 * 2. Iterate and consume results (cache-friendly linear access)
 * <p>
 * This measures the complete cost of the DoD rendering pipeline using DiffResult.
 * <p>
 * Run with: ./gradlew :tamboui-benchmarks:jmh -Pjmh.includes='.*FullPipeline.*'
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class FullPipelineBenchmark {

    @Param({"80x24", "160x50", "240x67", "320x100"})
    private String terminalSize;

    @Param({"5", "20", "50"})
    private int changePercentage;

    private Buffer prevBuffer;
    private Buffer currBuffer;
    private DiffResult reusableDiffResult;
    private int width;
    private int height;
    private int totalCells;

    @Setup(Level.Trial)
    public void setup() {
        String[] parts = terminalSize.split("x");
        width = Integer.parseInt(parts[0]);
        height = Integer.parseInt(parts[1]);
        Rect area = Rect.of(width, height);
        totalCells = area.area();

        int numToMutate = (totalCells * changePercentage) / 100;

        // Create prevBuffer filled with 'a'
        prevBuffer = Buffer.empty(area);
        for (int y = 0; y < height; y++) {
            StringBuilder line = new StringBuilder(width);
            for (int x = 0; x < width; x++) {
                line.append('a');
            }
            prevBuffer.setString(0, y, line.toString(), Style.EMPTY);
        }

        // Create currBuffer with 'a', then mutate X% cells to 'x'
        currBuffer = Buffer.empty(area);
        for (int y = 0; y < height; y++) {
            StringBuilder line = new StringBuilder(width);
            for (int x = 0; x < width; x++) {
                line.append('a');
            }
            currBuffer.setString(0, y, line.toString(), Style.EMPTY);
        }

        Random rand = new Random(42);
        List<Integer> allIndices = new ArrayList<>(totalCells);
        for (int i = 0; i < totalCells; i++) {
            allIndices.add(i);
        }
        Collections.shuffle(allIndices, rand);

        for (int i = 0; i < numToMutate; i++) {
            int index = allIndices.get(i);
            int x = index % width;
            int y = index / width;
            currBuffer.setString(x, y, "x", Style.EMPTY);
        }

        // Validate
        reusableDiffResult = new DiffResult(totalCells);
        prevBuffer.diff(currBuffer, reusableDiffResult);
        int actualDiffSize = reusableDiffResult.size();
        if (actualDiffSize != numToMutate) {
            throw new IllegalStateException("Expected " + numToMutate + " diffs but got " + actualDiffSize);
        }
        reusableDiffResult.clear();

        System.out.println("\n=== Benchmark: " + terminalSize + " (" + totalCells + " cells), "
                + changePercentage + "% change (" + numToMutate + " cells), validated ===\n");
    }

    @Benchmark
    public void fullPipeline(Blackhole blackhole) {
        prevBuffer.diff(currBuffer, reusableDiffResult);

        // Consume the diff the way a backend does: coordinates once per run, cells in sequence.
        for (int run = 0, runs = reusableDiffResult.runCount(); run < runs; run++) {
            int start = reusableDiffResult.runStart(run);
            int end = start + reusableDiffResult.runLength(run);
            blackhole.consume(reusableDiffResult.xOf(start));
            blackhole.consume(reusableDiffResult.yOf(start));
            for (int i = start; i < end; i++) {
                blackhole.consume(reusableDiffResult.cellAt(i));
            }
        }

        reusableDiffResult.clear();
    }
}

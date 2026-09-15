/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.buffer;

import java.util.Arrays;

import dev.tamboui.layout.Rect;

/**
 * A reusable, allocation-free container for buffer diff results.
 * <p>
 * Changed cells are stored as <em>runs</em>: pairs of {@code (startIndex, length)} over the
 * row-major cell index of the target buffer. Consecutive changed cells collapse into one run,
 * so a backend can position the cursor once per run and stream the cells; the cells themselves
 * are read straight from the target buffer, so nothing per cell is copied or referenced here.
 * <p>
 * Typical usage pattern:
 * <pre>{@code
 * DiffResult diff = new DiffResult(1920);  // Pre-size for 80x24 terminal
 * while (running) {
 *     previousBuffer.diff(currentBuffer, diff);
 *     backend.draw(diff);
 *     diff.clear();
 * }
 * }</pre>
 *
 * @see Buffer#diff(Buffer, DiffResult)
 */
public final class DiffResult {

    private int[] runs;     // (start, length) pairs
    private int runCount;
    private int cellCount;

    // Target buffer bound by Buffer.diff(); cells are read from here.
    private Cell[] source;
    private int srcX, srcY, srcWidth;

    /**
     * Creates a new diff result with the default initial capacity (256 updates).
     */
    public DiffResult() {
        this(256);
    }

    /**
     * Creates a new diff result sized for the given number of changed cells.
     * <p>
     * The worst case (every other cell changed) needs one run per two cells, so sizing this
     * to the terminal cell count guarantees no reallocation.
     *
     * @param initialCapacity the expected maximum number of changed cells per frame
     */
    public DiffResult(int initialCapacity) {
        this.runs = new int[Math.max(2, initialCapacity + 2)];
    }

    /**
     * Binds this result to the buffer whose cells the runs refer to, discarding any
     * previous content. Called by {@link Buffer#diff(Buffer, DiffResult)} before the
     * first run is appended.
     *
     * @param content the target buffer's cells, in row-major order
     * @param area the target buffer's area
     */
    void bind(Cell[] content, Rect area) {
        this.source = content;
        this.srcX = area.x();
        this.srcY = area.y();
        this.srcWidth = area.width();
        this.runCount = 0;
        this.cellCount = 0;
    }

    /**
     * Appends a run of {@code length} adjacent changed cells starting at row-major
     * {@code start}. Runs must be appended in increasing order and must not cross a row.
     *
     * @param start the row-major index of the run's first cell
     * @param length the number of cells in the run
     */
    void addRun(int start, int length) {
        int pos = runCount << 1;
        if (pos + 2 > runs.length) {
            runs = Arrays.copyOf(runs, Math.max(pos + 2, runs.length + (runs.length >> 1)));
        }
        runs[pos] = start;
        runs[pos + 1] = length;
        runCount++;
        cellCount += length;
    }

    /**
     * Clears this diff result and releases the reference to the target buffer.
     */
    public void clear() {
        runCount = 0;
        cellCount = 0;
        source = null;
    }

    /**
     * Returns the number of changed cells across all runs.
     *
     * @return the number of changed cells
     */
    public int size() {
        return cellCount;
    }

    /**
     * Returns whether this diff result holds no changed cells.
     *
     * @return true if there are no changed cells
     */
    public boolean isEmpty() {
        return cellCount == 0;
    }

    /**
     * Returns the number of runs of horizontally adjacent changed cells.
     *
     * @return the number of runs
     */
    public int runCount() {
        return runCount;
    }

    /**
     * Returns the row-major index in the target buffer at which the given run starts.
     *
     * @param run the run index (0 to {@link #runCount()}-1)
     * @return the row-major index of the run's first cell
     */
    public int runStart(int run) {
        return runs[run << 1];
    }

    /**
     * Returns the number of cells in the given run.
     *
     * @param run the run index (0 to {@link #runCount()}-1)
     * @return the number of cells in the run
     */
    public int runLength(int run) {
        return runs[(run << 1) + 1];
    }

    /**
     * Returns the cell at the given row-major index of the target buffer.
     *
     * @param index a row-major index, as returned by {@link #runStart(int)}
     * @return the cell at that index
     */
    public Cell cellAt(int index) {
        return source[index];
    }

    /**
     * Returns the x coordinate of the given row-major index in the target buffer.
     *
     * @param index a row-major index, as returned by {@link #runStart(int)}
     * @return the x coordinate
     */
    public int xOf(int index) {
        return srcX + index % srcWidth;
    }

    /**
     * Returns the y coordinate of the given row-major index in the target buffer.
     *
     * @param index a row-major index, as returned by {@link #runStart(int)}
     * @return the y coordinate
     */
    public int yOf(int index) {
        return srcY + index / srcWidth;
    }

    /**
     * Returns the x coordinate of the i-th changed cell.
     * <p>
     * This is an index-based view over the runs and costs O({@link #runCount()}) per call.
     * Backends should iterate runs with {@link #runStart(int)}/{@link #runLength(int)} instead.
     *
     * @param i the index of the changed cell (0 to {@link #size()}-1)
     * @return the x coordinate
     */
    public int getX(int i) {
        return xOf(indexOf(i));
    }

    /**
     * Returns the y coordinate of the i-th changed cell.
     * <p>
     * This is an index-based view over the runs and costs O({@link #runCount()}) per call.
     * Backends should iterate runs with {@link #runStart(int)}/{@link #runLength(int)} instead.
     *
     * @param i the index of the changed cell (0 to {@link #size()}-1)
     * @return the y coordinate
     */
    public int getY(int i) {
        return yOf(indexOf(i));
    }

    /**
     * Returns the i-th changed cell.
     * <p>
     * This is an index-based view over the runs and costs O({@link #runCount()}) per call.
     * Backends should iterate runs with {@link #runStart(int)}/{@link #runLength(int)} instead.
     *
     * @param i the index of the changed cell (0 to {@link #size()}-1)
     * @return the cell
     */
    public Cell getCell(int i) {
        return source[indexOf(i)];
    }

    private int indexOf(int i) {
        if (i < 0 || i >= cellCount) {
            throw new IndexOutOfBoundsException("Index: " + i + ", changed cells: " + cellCount);
        }
        int consumed = 0;
        for (int run = 0; run < runCount; run++) {
            int length = runs[(run << 1) + 1];
            if (i < consumed + length) {
                return runs[run << 1] + (i - consumed);
            }
            consumed += length;
        }
        throw new IllegalStateException("Run lengths do not add up to " + cellCount);
    }

    /**
     * Returns the number of runs this result can hold without reallocating.
     *
     * @return the capacity in runs
     */
    public int capacity() {
        return runs.length >> 1;
    }

    @Override
    public String toString() {
        return String.format("DiffResult[cells=%d, runs=%d, capacity=%d]", cellCount, runCount, capacity());
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.IOException;

import dev.tamboui.buffer.DiffResult;
import dev.tamboui.error.RuntimeIOException;
import dev.tamboui.layout.Position;

/**
 * Base class for terminal backends that produce ANSI output.
 * <p>
 * Provides final implementation of {@link #draw(DiffResult)} and
 * {@link #setCursorPosition(Position)} so that all concrete backends
 * share a single, consistent rendering path through {@link AnsiCellWriter}.
 * <p>
 * Subclasses must implement the raw I/O primitives ({@link #writeRaw(String)},
 * {@link #flush()}, etc.) but cannot override the drawing or cursor-positioning
 * logic.
 *
 * @see AnsiCellWriter
 */
public abstract class AbstractBackend implements Backend {

    /** Reusable buffer for cursor escape sequences – avoids per-call allocation. */
    private final StringBuilder cursorBuf = new StringBuilder(16);

    /**
     * Creates a new abstract backend.
     */
    protected AbstractBackend() {
    }

    /**
     * Draws the given cell updates to the terminal.
     * <p>
     * Iterates the runs of horizontally adjacent changed cells in {@link DiffResult},
     * positions the cursor once per run, and writes the run's cells using
     * {@link AnsiCellWriter}. Because a run never crosses a row, the terminal's own
     * cursor advance carries the writer through the run and no per-cell cursor move
     * is ever emitted.
     * <p>
     * Cursor moves use the field-level {@code cursorBuf} StringBuilder and
     * {@link #writeRaw(CharSequence)} to avoid a {@code toString()} allocation.
     * <p>
     * Output is sent via {@link #writeRaw(CharSequence)}.
     *
     * @param diff the diff result containing the runs of changed cells
     * @throws IOException if drawing fails
     */
    @Override
    public final void draw(DiffResult diff) throws IOException {
        try (AnsiCellWriter cellWriter = new AnsiCellWriter(s -> {
            try {
                writeRaw(s);
            } catch (IOException e) {
                throw new RuntimeIOException("Failed to write cell data", e);
            }
        })) {
            // One cursor move per run of adjacent changed cells on a row; the terminal
            // auto-advances inside the run (2-wide chars advance by two, their
            // CONTINUATION cells are skipped by the writer).
            for (int r = 0, n = diff.runCount(); r < n; r++) {
                int p = diff.runStart(r);
                int end = p + diff.runLength(r);
                // A run may start on a CONTINUATION whose wide char is unchanged: nothing to
                // draw there, position on the first real cell instead.
                while (p < end && diff.cellAt(p).isContinuation()) {
                    p++;
                }
                if (p == end) {
                    continue;
                }
                // ANSI CUP: \e[row;colH  (1-based)
                cursorBuf.setLength(0);
                cursorBuf.append("\u001b[");
                cursorBuf.append(diff.yOf(p) + 1);
                cursorBuf.append(';');
                cursorBuf.append(diff.xOf(p) + 1);
                cursorBuf.append('H');
                writeRaw(cursorBuf);
                for (; p < end; p++) {
                    cellWriter.writeCell(diff.cellAt(p));
                }
            }
        }
    }

    /**
     * Sets the cursor to the given position and flushes.
     *
     * @param position the position to set the cursor to
     * @throws IOException if the operation fails
     */
    @Override
    public final void setCursorPosition(Position position) throws IOException {
        // ANSI uses 1-based coordinates
        writeRaw("\u001b[" + (position.y() + 1) + ";" + (position.x() + 1) + "H");
        flush();
    }
}

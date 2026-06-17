/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.DiffResult;
import dev.tamboui.error.RuntimeIOException;
import dev.tamboui.jfr.TerminalDrawEvent;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.widget.RawOutputCapable;
import dev.tamboui.widget.RawOutputContext;

/**
 * The main terminal abstraction. Manages the rendering lifecycle and
 * buffer management for efficient updates.
 *
 * @param <B> the backend type
 */
public final class Terminal<B extends Backend> implements AutoCloseable {

    // Kitty graphics protocol: delete all images from the graphics layer.
    // Non-Kitty terminals safely ignore this APC sequence.
    // d=A (uppercase) deletes all images AND frees their stored data. The lowercase d=a
    // only removes placements while leaving the image bytes in terminal memory, which would
    // leak terminal-side memory whenever images are dismissed.
    // q=2 suppresses the terminal's OK/error reply; without it the reply is read back as input
    // (e.g. stray digits landing in the focused field).
    private static final byte[] KITTY_DELETE_ALL =
            "\033_Ga=d,d=A,q=2\033\\".getBytes(StandardCharsets.US_ASCII);

    private final B backend;
    private final RawOutput rawOutput;
    private final DiffResult diffResult;
    private Buffer currentBuffer;
    private Buffer previousBuffer;
    private boolean hiddenCursor;
    private boolean previousFrameHadRawOutput;
    private List<Rect> previousRawOutputAreas = Collections.emptyList();

    /**
     * Creates a new terminal instance with the given backend.
     *
     * @param backend the backend to use for terminal operations
     * @throws RuntimeIOException if initialization fails
     */
    public Terminal(B backend) {
        this.backend = backend;
        this.hiddenCursor = false;
        this.rawOutput = createRawOutputStream(backend);

        try {
            Size size = backend.size();
            Rect area = Rect.of(size.width(), size.height());
            this.currentBuffer = Buffer.empty(area);
            this.previousBuffer = Buffer.empty(area);
            // Pre-allocate diff result with capacity for entire terminal
            // (worst case: every cell changes). This ensures zero reallocation.
            this.diffResult = new DiffResult(area.area());
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to initialize terminal: " + e.getMessage(), e);
        }
    }

    private static RawOutput createRawOutputStream(Backend backend) {
        return new RawOutput(backend);
    }

    /**
     * The raw output stream handed to {@link RawOutputCapable} widgets.
     * <p>
     * Beyond writing bytes to the backend, it exposes a {@link RawOutputContext#generation()
     * generation} counter that the terminal increments whenever the screen is cleared
     * (via {@link #clear()} or a resize). Raw-output widgets such as the native image
     * protocols use it to know when their cached "already transmitted" state is stale and
     * must be redrawn.
     */
    private static final class RawOutput extends OutputStream implements RawOutputContext {

        private final Backend backend;
        private long generation;

        // While buffering, widget writes are collected here instead of sent immediately, so the
        // frame's raw output (native images) can be flushed AFTER the cell diff. Cell-based image
        // protocols (iTerm2, Sixel) otherwise get holes punched by the diff that clears the
        // previous frame's text from the image's cells during the same draw.
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private boolean buffering;

        RawOutput(Backend backend) {
            this.backend = backend;
        }

        /** Increments the screen generation, signalling that the screen was cleared. */
        void bumpGeneration() {
            generation++;
        }

        @Override
        public long generation() {
            return generation;
        }

        /** Begins collecting writes instead of sending them. */
        void startBuffering() {
            buffering = true;
        }

        /** Stops collecting; subsequent writes are sent directly again. */
        void stopBuffering() {
            buffering = false;
        }

        /** Sends any collected bytes to the backend (called after the cell diff). */
        void drainBuffer() throws IOException {
            if (buffer.size() > 0) {
                backend.writeRaw(buffer.toByteArray());
                buffer.reset();
            }
        }

        @Override
        public void write(int b) throws IOException {
            if (buffering) {
                buffer.write(b);
            } else {
                backend.writeRaw(new byte[]{(byte) b});
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (buffering) {
                buffer.write(b, off, len);
            } else if (off == 0 && len == b.length) {
                backend.writeRaw(b);
            } else {
                byte[] slice = new byte[len];
                System.arraycopy(b, off, slice, 0, len);
                backend.writeRaw(slice);
            }
        }

        @Override
        public void flush() throws IOException {
            // While buffering, a widget's flush() must not push its raw output to the terminal yet;
            // the buffer is drained and flushed by the terminal after the cell diff.
            if (!buffering) {
                backend.flush();
            }
        }
    }

    /**
     * Returns the backend.
     *
     * @return the backend instance
     */
    public B backend() {
        return backend;
    }

    /**
     * Returns the current terminal size.
     *
     * @return the terminal size
     * @throws RuntimeIOException if the size cannot be determined
     */
    public Size size() {
        try {
            return backend.size();
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to get terminal size: " + e.getMessage(), e);
        }
    }

    /**
     * Draws a frame using the provided rendering function.
     * This is the main rendering entry point.
     *
     * @param renderer the function that renders to the frame
     * @return a completed frame containing the rendered buffer
     * @throws RuntimeIOException if drawing fails
     */
    public CompletedFrame draw(Consumer<Frame> renderer) {
        TerminalDrawEvent trace = null;
        if (TerminalDrawEvent.enabled()) {
            trace = new TerminalDrawEvent();
            trace.begin();
        }
        try {
            try {
                // Handle resize if needed
                Size size = backend.size();
                Rect area = Rect.of(size.width(), size.height());

                if (!area.equals(currentBuffer.area())) {
                    resize(area);
                }

                // Clear current buffer for new frame
                currentBuffer.clear();

                // Create frame and render. Buffer the raw output (native images) widgets emit, so it
                // is flushed AFTER the cell diff below. Otherwise the diff that clears the previous
                // frame's text from a cell-based image's cells (iTerm2, Sixel) would punch a hole in
                // the image emitted during this same draw. Kitty is unaffected (separate graphics
                // layer), but deferring is harmless for it.
                Frame frame = new Frame(currentBuffer, rawOutput);
                rawOutput.startBuffering();
                try {
                    renderer.accept(frame);
                } finally {
                    rawOutput.stopBuffering();
                }

                // Clear stale raw-output areas directly, before the diff: these wipe vacated image
                // regions (spaces / Kitty delete-all) and must not overwrite the new cell content
                // the diff is about to draw there.
                cleanupRawOutput(frame.rawOutputAreas());

                // Calculate diff and draw (zero-allocation DoD variant)
                previousBuffer.diff(currentBuffer, diffResult);
                if (!diffResult.isEmpty()) {
                    backend.draw(diffResult);
                }
                diffResult.clear();  // Clear after use to release Cell refs for GC

                // Flush the buffered raw output (native images) on top of the freshly drawn cells.
                rawOutput.drainBuffer();

                // Handle cursor
                if (frame.isCursorVisible()) {
                    frame.cursorPosition().ifPresent(pos -> {
                        try {
                            backend.setCursorPosition(pos);
                            if (hiddenCursor) {
                                backend.showCursor();
                                hiddenCursor = false;
                            }
                        } catch (IOException e) {
                            throw new RuntimeIOException(
                                    String.format("Failed to set cursor position to %s: %s", pos, e.getMessage()), e);
                        }
                    });
                } else if (!hiddenCursor) {
                    try {
                        backend.hideCursor();
                        hiddenCursor = true;
                    } catch (IOException e) {
                        throw new RuntimeIOException("Failed to hide cursor: " + e.getMessage(), e);
                    }
                }

                // Flush output
                backend.flush();

                // Swap buffers
                Buffer temp = previousBuffer;
                previousBuffer = currentBuffer;
                currentBuffer = temp;

                previousFrameHadRawOutput = frame.hadRawOutput();
                previousRawOutputAreas = frame.rawOutputAreas();

                return new CompletedFrame(previousBuffer, area);
            } catch (IOException e) {
                throw new RuntimeIOException("Failed to draw frame: " + e.getMessage(), e);
            }
        } finally {
            if (trace != null) {
                trace.commit();
            }
        }
    }

    /**
     * Resizes the terminal buffers.
     *
     * @param area the new terminal area
     * @throws RuntimeIOException if resizing fails
     */
    private void resize(Rect area) {
        currentBuffer = Buffer.empty(area);
        previousBuffer = Buffer.empty(area);
        try {
            cleanupRawOutput(Collections.emptyList());
            previousFrameHadRawOutput = false;
            previousRawOutputAreas = Collections.emptyList();
            backend.clear();
            // The screen was wiped: invalidate raw-output widgets' cached "already drawn" state.
            rawOutput.bumpGeneration();
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to clear terminal during resize: " + e.getMessage(), e);
        }
    }

    /**
     * Cleans up raw output artifacts from previous frames.
     * <p>
     * When raw-output widgets (Kitty/iTerm2/Sixel) are removed or moved between
     * frames, their artifacts must be explicitly cleared:
     * <ul>
     *   <li>Kitty: images live on a separate graphics layer, so a delete-all
     *       command is sent when all images are removed.</li>
     *   <li>iTerm2/Sixel: images occupy terminal cells, so stale areas are
     *       overwritten with spaces.</li>
     * </ul>
     *
     * @param currentAreas the raw output areas rendered in the current frame
     *                     (empty list when no images are present)
     * @throws IOException if writing cleanup sequences fails
     */
    private void cleanupRawOutput(List<Rect> currentAreas) throws IOException {
        if (!previousFrameHadRawOutput) {
            return;
        }

        List<Rect> staleAreas;
        if (currentAreas.isEmpty()) {
            rawOutput.write(KITTY_DELETE_ALL);
            staleAreas = previousRawOutputAreas;
        } else {
            staleAreas = new ArrayList<>();
            for (Rect prev : previousRawOutputAreas) {
                if (!currentAreas.contains(prev)) {
                    staleAreas.add(prev);
                }
            }
        }

        if (!staleAreas.isEmpty()) {
            int maxWidth = 0;
            for (Rect area : staleAreas) {
                if (area.width() > maxWidth) {
                    maxWidth = area.width();
                }
            }
            byte[] spaces = new byte[maxWidth];
            Arrays.fill(spaces, (byte) ' ');

            for (Rect imageArea : staleAreas) {
                for (int y = imageArea.y(); y < imageArea.y() + imageArea.height(); y++) {
                    String move = String.format("\033[%d;%dH", y + 1, imageArea.x() + 1);
                    rawOutput.write(move.getBytes(StandardCharsets.US_ASCII));
                    rawOutput.write(spaces, 0, imageArea.width());
                }
            }
        }
    }

    /**
     * Clears the terminal and resets the buffers.
     * <p>
     * Any images drawn by native protocols (Kitty, iTerm2, Sixel) are removed from the screen
     * as part of the clear, and the screen {@link RawOutputContext#generation() generation} is
     * advanced so those widgets retransmit their image on the next {@link #draw(Consumer) draw}
     * instead of assuming it is still visible. Callers therefore do not need to take any special
     * action to make images survive a {@code clear()} — the following frame redraws them.
     *
     * @throws RuntimeIOException if clearing fails
     */
    public void clear() {
        try {
            cleanupRawOutput(Collections.emptyList());
            previousFrameHadRawOutput = false;
            previousRawOutputAreas = Collections.emptyList();
            backend.clear();
            // The screen was wiped: invalidate raw-output widgets' cached "already drawn" state.
            rawOutput.bumpGeneration();
            Rect area = currentBuffer.area();
            currentBuffer = Buffer.empty(area);
            previousBuffer = Buffer.empty(area);
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to clear terminal: " + e.getMessage(), e);
        }
    }

    /**
     * Shows the cursor.
     *
     * @throws RuntimeIOException if showing the cursor fails
     */
    public void showCursor() {
        try {
            backend.showCursor();
            hiddenCursor = false;
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to show cursor: " + e.getMessage(), e);
        }
    }

    /**
     * Hides the cursor.
     *
     * @throws RuntimeIOException if hiding the cursor fails
     */
    public void hideCursor() {
        try {
            backend.hideCursor();
            hiddenCursor = true;
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to hide cursor: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the current terminal area.
     *
     * @return the current terminal area
     */
    public Rect area() {
        return currentBuffer.area();
    }

    /**
     * Closes the terminal and releases resources.
     *
     * @throws RuntimeIOException if closing fails
     */
    @Override
    public void close() {
        try {
            cleanupRawOutput(Collections.emptyList());
            if (hiddenCursor) {
                backend.showCursor();
            }
            backend.close();
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to close terminal: " + e.getMessage(), e);
        }
    }
}

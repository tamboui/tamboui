/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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

/**
 * The main terminal abstraction. Manages the rendering lifecycle and
 * buffer management for efficient updates.
 *
 * @param <B> the backend type
 */
public final class Terminal<B extends Backend> implements AutoCloseable {

    private static final byte[] KITTY_DELETE_ALL =
            "\033_Ga=d,d=a\033\\".getBytes(StandardCharsets.US_ASCII);

    private final B backend;
    private final OutputStream rawOutput;
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

    private static OutputStream createRawOutputStream(Backend backend) {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                backend.writeRaw(new byte[]{(byte) b});
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if (off == 0 && len == b.length) {
                    backend.writeRaw(b);
                } else {
                    byte[] slice = new byte[len];
                    System.arraycopy(b, off, slice, 0, len);
                    backend.writeRaw(slice);
                }
            }

            @Override
            public void flush() throws IOException {
                backend.flush();
            }
        };
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

                // Create frame and render
                Frame frame = new Frame(currentBuffer, rawOutput);
                renderer.accept(frame);

                // Clean up images when raw-output widgets are no longer rendered.
                // Kitty: delete-all removes the separate graphics layer.
                // iTerm2/Sixel: overwrite previous image areas with spaces.
                if (previousFrameHadRawOutput && !frame.hadRawOutput()) {
                    rawOutput.write(KITTY_DELETE_ALL);
                    for (Rect imageArea : previousRawOutputAreas) {
                        for (int y = imageArea.y(); y < imageArea.y() + imageArea.height(); y++) {
                            String move = String.format("\033[%d;%dH", y + 1, imageArea.x() + 1);
                            rawOutput.write(move.getBytes(StandardCharsets.US_ASCII));
                            byte[] spaces = new byte[imageArea.width()];
                            Arrays.fill(spaces, (byte) ' ');
                            rawOutput.write(spaces);
                        }
                    }
                }

                // Calculate diff and draw (zero-allocation DoD variant)
                previousBuffer.diff(currentBuffer, diffResult);
                if (!diffResult.isEmpty()) {
                    backend.draw(diffResult);
                }
                diffResult.clear();  // Clear after use to release Cell refs for GC

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
            if (previousFrameHadRawOutput) {
                rawOutput.write(KITTY_DELETE_ALL);
                previousFrameHadRawOutput = false;
                previousRawOutputAreas = Collections.emptyList();
            }
            backend.clear();
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to clear terminal during resize: " + e.getMessage(), e);
        }
    }

    /**
     * Clears the terminal and resets the buffers.
     *
     * @throws RuntimeIOException if clearing fails
     */
    public void clear() {
        try {
            backend.clear();
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
            if (hiddenCursor) {
                backend.showCursor();
            }
            backend.close();
        } catch (IOException e) {
            throw new RuntimeIOException("Failed to close terminal: " + e.getMessage(), e);
        }
    }
}

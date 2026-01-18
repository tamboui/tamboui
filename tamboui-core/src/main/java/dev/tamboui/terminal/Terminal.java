/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.CellUpdate;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;

/// The main terminal abstraction. Manages the rendering lifecycle and
/// buffer management for efficient updates.
///
/// @param <B> the backend type
public final class Terminal<B extends Backend> implements AutoCloseable {

    private final B backend;
    private final OutputStream rawOutput;
    private Buffer currentBuffer;
    private Buffer previousBuffer;
    private boolean hiddenCursor;

    /// Creates a new terminal instance with the given backend.
    ///
    /// @param backend the backend to use for terminal operations
    /// @throws IOException if initialization fails
    public Terminal(B backend) throws IOException {
        this.backend = backend;
        this.hiddenCursor = false;
        this.rawOutput = createRawOutputStream(backend);

        Size size = backend.size();
        Rect area = Rect.of(size.width(), size.height());
        this.currentBuffer = Buffer.empty(area);
        this.previousBuffer = Buffer.empty(area);
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

    /// Returns the backend.
    ///
    /// @return the backend instance
    public B backend() {
        return backend;
    }

    /// Returns the current terminal size.
    ///
    /// @return the terminal size
    /// @throws IOException if the size cannot be determined
    public Size size() throws IOException {
        return backend.size();
    }

    /// Draws a frame using the provided rendering function.
    /// This is the main rendering entry point.
    ///
    /// @param renderer the function that renders to the frame
    /// @return a completed frame containing the rendered buffer
    /// @throws IOException if drawing fails
    public CompletedFrame draw(Consumer<Frame> renderer) throws IOException {
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

        // Calculate diff and draw
        List<CellUpdate> updates = previousBuffer.diff(currentBuffer);
        if (!updates.isEmpty()) {
            backend.draw(updates);
        }

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
                    throw new RuntimeException("Failed to set cursor position", e);
                }
            });
        } else if (!hiddenCursor) {
            backend.hideCursor();
            hiddenCursor = true;
        }

        // Flush output
        backend.flush();

        // Swap buffers
        Buffer temp = previousBuffer;
        previousBuffer = currentBuffer;
        currentBuffer = temp;

        return new CompletedFrame(previousBuffer, area);
    }

    /// Resizes the terminal buffers.
    ///
    /// @param area the new terminal area
    /// @throws IOException if resizing fails
    private void resize(Rect area) throws IOException {
        currentBuffer = Buffer.empty(area);
        previousBuffer = Buffer.empty(area);
        backend.clear();
    }

    /// Clears the terminal and resets the buffers.
    ///
    /// @throws IOException if clearing fails
    public void clear() throws IOException {
        backend.clear();
        Rect area = currentBuffer.area();
        currentBuffer = Buffer.empty(area);
        previousBuffer = Buffer.empty(area);
    }

    /// Shows the cursor.
    ///
    /// @throws IOException if showing the cursor fails
    public void showCursor() throws IOException {
        backend.showCursor();
        hiddenCursor = false;
    }

    /// Hides the cursor.
    ///
    /// @throws IOException if hiding the cursor fails
    public void hideCursor() throws IOException {
        backend.hideCursor();
        hiddenCursor = true;
    }

    /// Returns the current terminal area.
    ///
    /// @return the current terminal area
    public Rect area() {
        return currentBuffer.area();
    }

    /// Closes the terminal and releases resources.
    ///
    /// @throws IOException if closing fails
    @Override
    public void close() throws IOException {
        if (hiddenCursor) {
            backend.showCursor();
        }
        backend.close();
    }
}


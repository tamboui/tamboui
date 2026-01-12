/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.pilot;

import dev.tamboui.buffer.CellUpdate;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;
import dev.tamboui.terminal.Backend;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A headless backend implementation for testing TUI applications.
 * <p>
 * This backend doesn't interact with a real terminal, making it suitable
 * for automated testing. It maintains a configurable terminal size and
 * provides a queue for injecting events programmatically.
 */
public final class TestBackend implements Backend {

    private final Size size;
    private final BlockingQueue<Integer> inputQueue;
    private volatile Runnable resizeHandler;
    private volatile boolean closed;

    /**
     * Creates a test backend with the default size (80x24).
     */
    public TestBackend() {
        this(new Size(80, 24));
    }

    /**
     * Creates a test backend with the specified size.
     *
     * @param size the terminal size
     */
    public TestBackend(Size size) {
        this.size = size;
        this.inputQueue = new LinkedBlockingQueue<>();
        this.closed = false;
    }

    /**
     * Injects a character into the input queue for testing.
     *
     * @param c the character to inject
     */
    public void injectChar(char c) {
        if (!closed) {
            inputQueue.offer((int) c);
        }
    }

    /**
     * Injects a key code into the input queue for testing.
     * The key code is encoded as a negative value to distinguish it from characters.
     *
     * @param keyCode the key code to inject
     */
    public void injectKey(int keyCode) {
        if (!closed) {
            // Use negative values to encode key codes (offset by -1000 to avoid conflicts)
            inputQueue.offer(-(keyCode + 1000));
        }
    }

    /**
     * Simulates a terminal resize.
     *
     * @param newSize the new terminal size
     */
    public void simulateResize(Size newSize) {
        if (resizeHandler != null) {
            resizeHandler.run();
        }
    }

    @Override
    public void draw(Iterable<CellUpdate> updates) throws IOException {
        // No-op for testing
    }

    @Override
    public void flush() throws IOException {
        // No-op for testing
    }

    @Override
    public void clear() throws IOException {
        // No-op for testing
    }

    @Override
    public Size size() throws IOException {
        return size;
    }

    @Override
    public void showCursor() throws IOException {
        // No-op for testing
    }

    @Override
    public void hideCursor() throws IOException {
        // No-op for testing
    }

    @Override
    public Position getCursorPosition() throws IOException {
        return new Position(0, 0);
    }

    @Override
    public void setCursorPosition(Position position) throws IOException {
        // No-op for testing
    }

    @Override
    public void enterAlternateScreen() throws IOException {
        // No-op for testing
    }

    @Override
    public void leaveAlternateScreen() throws IOException {
        // No-op for testing
    }

    @Override
    public void enableRawMode() throws IOException {
        // No-op for testing
    }

    @Override
    public void disableRawMode() throws IOException {
        // No-op for testing
    }

    @Override
    public void enableMouseCapture() throws IOException {
        // No-op for testing
    }

    @Override
    public void disableMouseCapture() throws IOException {
        // No-op for testing
    }

    @Override
    public void onResize(Runnable handler) {
        this.resizeHandler = handler;
    }

    @Override
    public int read(int timeoutMs) throws IOException {
        if (closed) {
            return -1; // EOF
        }
        try {
            Integer value = inputQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
            if (value == null) {
                return -2; // Timeout
            }
            return value;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -2; // Timeout
        }
    }

    @Override
    public int peek(int timeoutMs) throws IOException {
        if (closed) {
            return -1; // EOF
        }
        Integer value = inputQueue.peek();
        if (value == null) {
            return -2; // Timeout
        }
        return value;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        inputQueue.clear();
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import dev.tamboui.buffer.CellUpdate;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Size;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Backend interface for terminal operations.
 * Implementations handle the actual terminal I/O.
 */
public interface Backend extends AutoCloseable {

    /**
     * Draws the given cell updates to the terminal.
     *
     * @param updates the cell updates to draw
     * @throws IOException if drawing fails
     */
    void draw(Iterable<CellUpdate> updates) throws IOException;

    /**
     * Flushes any buffered output to the terminal.
     *
     * @throws IOException if flushing fails
     */
    void flush() throws IOException;

    /**
     * Clears the terminal screen.
     *
     * @throws IOException if clearing fails
     */
    void clear() throws IOException;

    /**
     * Returns the current terminal size.
     *
     * @return the terminal size
     * @throws IOException if the size cannot be determined
     */
    Size size() throws IOException;

    /**
     * Shows the cursor.
     *
     * @throws IOException if showing the cursor fails
     */
    void showCursor() throws IOException;

    /**
     * Hides the cursor.
     *
     * @throws IOException if hiding the cursor fails
     */
    void hideCursor() throws IOException;

    /**
     * Gets the current cursor position.
     *
     * @return the current cursor position
     * @throws IOException if the position cannot be determined
     */
    Position getCursorPosition() throws IOException;

    /**
     * Sets the cursor position.
     *
     * @param position the position to set the cursor to
     * @throws IOException if setting the cursor position fails
     */
    void setCursorPosition(Position position) throws IOException;

    /**
     * Enters the alternate screen buffer.
     *
     * @throws IOException if entering alternate screen fails
     */
    void enterAlternateScreen() throws IOException;

    /**
     * Leaves the alternate screen buffer.
     *
     * @throws IOException if leaving alternate screen fails
     */
    void leaveAlternateScreen() throws IOException;

    /**
     * Enables raw mode (disables line buffering and echo).
     *
     * @throws IOException if enabling raw mode fails
     */
    void enableRawMode() throws IOException;

    /**
     * Disables raw mode.
     *
     * @throws IOException if disabling raw mode fails
     */
    void disableRawMode() throws IOException;

    /**
     * Enables mouse capture.
     *
     * @throws IOException if enabling mouse capture fails
     */
    default void enableMouseCapture() throws IOException {
        // Optional: not all backends support mouse
    }

    /**
     * Disables mouse capture.
     *
     * @throws IOException if disabling mouse capture fails
     */
    default void disableMouseCapture() throws IOException {
        // Optional: not all backends support mouse
    }

    /**
     * Scrolls the screen up by the given number of lines.
     *
     * @param lines the number of lines to scroll up
     * @throws IOException if scrolling fails
     */
    default void scrollUp(int lines) throws IOException {
        // Optional
    }

    /**
     * Scrolls the screen down by the given number of lines.
     *
     * @param lines the number of lines to scroll down
     * @throws IOException if scrolling fails
     */
    default void scrollDown(int lines) throws IOException {
        // Optional
    }

    /**
     * Writes raw bytes directly to the terminal output.
     * <p>
     * This is used for native image protocols (Sixel, Kitty, iTerm2) that
     * require sending escape sequences with binary data.
     *
     * @param data the raw bytes to write
     * @throws IOException if writing fails
     */
    default void writeRaw(byte[] data) throws IOException {
        // Optional: not all backends support raw output
        throw new UnsupportedOperationException("Raw output not supported by this backend");
    }

    /**
     * Writes a raw string directly to the terminal output.
     * <p>
     * This is a convenience method for protocols that work with ASCII escape sequences.
     *
     * @param data the string to write
     * @throws IOException if writing fails
     */
    default void writeRaw(String data) throws IOException {
        writeRaw(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Registers a handler to be called when the terminal is resized.
     *
     * @param handler the handler to call on resize
     */
    void onResize(Runnable handler);

    /**
     * Reads a single character from the terminal input with timeout.
     *
     * @param timeoutMs timeout in milliseconds
     * @return the character read, -1 for EOF, or -2 for timeout
     * @throws IOException if an I/O error occurs
     */
    int read(int timeoutMs) throws IOException;

    /**
     * Peeks at the next character without consuming it.
     *
     * @param timeoutMs timeout in milliseconds
     * @return the character peeked, -1 for EOF, or -2 for timeout
     * @throws IOException if an I/O error occurs
     */
    int peek(int timeoutMs) throws IOException;

    /**
     * Closes this backend and releases any resources.
     *
     * @throws IOException if closing fails
     */
    @Override
    void close() throws IOException;
}

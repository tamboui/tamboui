/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.backend.panama;

import dev.tamboui.layout.Size;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Platform-independent terminal operations interface.
 * <p>
 * This interface abstracts the low-level terminal operations that differ
 * between Unix (Linux/macOS) and Windows platforms.
 */
public interface PlatformTerminal extends AutoCloseable {

    /**
     * Enables raw mode on the terminal.
     * <p>
     * Raw mode disables line buffering, echo, and special character processing,
     * allowing direct character-by-character input.
     *
     * @throws IOException if raw mode cannot be enabled
     */
    void enableRawMode() throws IOException;

    /**
     * Disables raw mode and restores original terminal attributes.
     *
     * @throws IOException if raw mode cannot be disabled
     */
    void disableRawMode() throws IOException;

    /**
     * Gets the current terminal size.
     *
     * @return the terminal size
     * @throws IOException if the size cannot be determined
     */
    Size getSize() throws IOException;

    /**
     * Reads a single character from the terminal with timeout.
     *
     * @param timeoutMs timeout in milliseconds (-1 for infinite, 0 for non-blocking)
     * @return the character read, -1 for EOF, or -2 for timeout
     * @throws IOException if reading fails
     */
    int read(int timeoutMs) throws IOException;

    /**
     * Peeks at the next character without consuming it.
     *
     * @param timeoutMs timeout in milliseconds
     * @return the character peeked, -1 for EOF, or -2 for timeout
     * @throws IOException if reading fails
     */
    int peek(int timeoutMs) throws IOException;

    /**
     * Writes data to the terminal.
     *
     * @param data the data to write
     * @throws IOException if writing fails
     */
    void write(byte[] data) throws IOException;

    /**
     * Writes a portion of a byte array to the terminal.
     * <p>
     * This method allows writing from a reusable buffer without
     * creating intermediate byte array copies.
     *
     * @param buffer the byte array containing data
     * @param offset the start offset in the buffer
     * @param length the number of bytes to write
     * @throws IOException if writing fails
     */
    void write(byte[] buffer, int offset, int length) throws IOException;

    /**
     * Writes a string to the terminal.
     *
     * @param s the string to write
     * @throws IOException if writing fails
     */
    void write(String s) throws IOException;

    /**
     * Returns the charset used for terminal I/O.
     *
     * @return the terminal charset
     */
    Charset getCharset();

    /**
     * Checks if raw mode is currently enabled.
     *
     * @return true if raw mode is enabled
     */
    boolean isRawModeEnabled();

    /**
     * Registers a handler to be called when the terminal is resized.
     *
     * @param handler the handler to call on resize, or null to remove
     */
    void onResize(Runnable handler);

    /**
     * Closes the terminal and releases resources.
     *
     * @throws IOException if closing fails
     */
    @Override
    void close() throws IOException;
}

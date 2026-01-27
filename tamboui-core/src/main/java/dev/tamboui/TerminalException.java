/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui;

/**
 * Base exception for TamboUI framework errors.
 * <p>
 * This exception serves as the base class for all TamboUI-specific exceptions.
 * It provides a consistent exception hierarchy for the framework.
 * <p>
 * For terminal I/O errors, use {@link TerminalIOException}.
 *
 * @see TerminalIOException
 */
public class TerminalException extends RuntimeException {

    /**
     * Creates a new terminal exception with the given message.
     *
     * @param message the error message
     */
    public TerminalException(String message) {
        super(message);
    }

    /**
     * Creates a new terminal exception with the given message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public TerminalException(String message, Throwable cause) {
        super(message, cause);
    }
}

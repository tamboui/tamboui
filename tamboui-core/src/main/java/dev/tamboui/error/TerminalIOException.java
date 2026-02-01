/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.error;

import java.io.IOException;

import dev.tamboui.error.TamboUIException;

/**
 * Exception thrown when a terminal I/O operation fails.
 * <p>
 * This exception is used for errors that occur during terminal I/O operations,
 * such as reading from or writing to the terminal, setting terminal attributes,
 * or querying terminal state.
 *
 * @see TamboUIException
 */
public class TerminalIOException extends TamboUIException {

    /**
     * Creates a new terminal I/O exception with the given message.
     *
     * @param message the error message
     */
    public TerminalIOException(String message) {
        super(message);
    }

    /**
     * Creates a new terminal I/O exception with the given message and cause.
     *
     * @param message the error message
     * @param cause   the underlying IOException
     */
    public TerminalIOException(String message, Throwable cause) {
        super(message, cause);
    }

   
}

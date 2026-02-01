/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.error;

import dev.tamboui.error.TamboUIException;

/**
 * Exception thrown when a terminal I/O operation fails.
 * <p>
 * This exception is used for errors that occur during terminal I/O operations,
 * such as reading from or writing to the terminal, setting terminal attributes,
 * or querying terminal state.
 *
 * NOTE: This is similar to java.io.UncheckedExcpetion, but does not share name to avoid
 * ambiguity
 *
 * @see TamboUIException
 */
public class RuntimeIOException extends TamboUIException {

    /**
     * Creates a new runtime/unchecked IO exception with the given message.
     *
     * @param message the error message
     */
    public RuntimeIOException(String message) {
        super(message);
    }

    /**
     * Creates a new runtime/unchecked I/O exception with the given message and cause.
     *
     * @param message the error message
     * @param cause   the underlying IOException
     */
    public RuntimeIOException(String message, Throwable cause) {
        super(message, cause);
    }

   
}

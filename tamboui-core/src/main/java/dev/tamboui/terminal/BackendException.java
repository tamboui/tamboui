package dev.tamboui.terminal;

import dev.tamboui.error.TamboUIException;

/**
 * Exception for backend errors.
 * <p>
 * This exception is used for errors that occur during backend operations,
 * such as creating a backend instance, calling native methods, or other backend-specific errors
 * that are not I/O related.
 * <p>
 * For I/O related errors, backend methods throw {@link java.io.IOException} directly,
 * or {@link dev.tamboui.error.RuntimeIOException} when wrapped as a runtime exception.
 */
public class BackendException extends TamboUIException {

    /**
     * Creates a new backend exception with the given message.
     *
     * @param message the error message
     */
    public BackendException(String message) {
        super(message);
    }

    /**
     * Creates a new backend exception with the given message and cause.
     *
     * @param message the error message
     * @param cause   the underlying exception
     */
    public BackendException(String message, Throwable cause) {
        super(message, cause);
    }

}

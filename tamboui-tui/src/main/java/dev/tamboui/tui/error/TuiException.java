package dev.tamboui.tui.error;

import dev.tamboui.error.TamboUIException;

/**
 * Exception for TUI framework related errors.
 */
public class TuiException extends TamboUIException {

    /**
     * Creates a new TUI exception with the given message.
     * @param message the error message
     */
    public TuiException(String message) {
        super(message);
    }

    /**
     * Creates a new TUI exception with the given message and cause.
     * @param message the error message
     * @param cause the cause of the exception
     */
    public TuiException(String message, Throwable cause) {
        super(message, cause);
    }
}

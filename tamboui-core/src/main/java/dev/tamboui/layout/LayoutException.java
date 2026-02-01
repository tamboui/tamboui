package dev.tamboui.layout;

import dev.tamboui.error.TamboUIException;

/**
 * Exception for layout related errors.
 */
public class LayoutException extends TamboUIException {

    /**
     * Creates a new layout exception with the given message.
     *
     * @param message the error message
     */
    public LayoutException(String message) {
        super(message);
    }

    /**
     * Creates a new layout exception with the given message and cause.
     *
     * @param message the error message
     * @param cause   the underlying exception
     */
    public LayoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

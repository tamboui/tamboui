package dev.tamboui.tui;

import dev.tamboui.TamboUIException;

/**
 * Exception for TUI framework related errors.
 */
public class TuiException extends TamboUIException {

    public TuiException(String message) {
        super(message);
    }

    public TuiException(String message, Throwable cause) {
        super(message, cause);
    }
}

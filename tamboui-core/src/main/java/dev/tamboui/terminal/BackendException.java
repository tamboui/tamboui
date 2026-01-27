package dev.tamboui.terminal;

import dev.tamboui.TamboUIException;

/**
 * Exception for backend errors.
 * <p>
 * This exception is used for errors that occur during backend operations,
 * such as creating a backend instance, calling native methods, or other backend-specific errors
 * that are not I/O related.
 * 
 * For IO related errors, use {@link TerminalIOException}.
 * <p>.
 */
public class BackendException extends TamboUIException {

    public BackendException(String message) {
        super(message);
    }

    public BackendException(String message, Throwable cause) {
        super(message, cause);
    }

}

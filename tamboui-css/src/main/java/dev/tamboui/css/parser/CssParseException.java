/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when CSS parsing or strict validation fails.
 */
public final class CssParseException extends RuntimeException {

    /** The source position where the parse error occurred. */
    private final Token.Position position;

    /** The diagnostics associated with this parse failure, if any. */
    private final List<CssDiagnostic> diagnostics;

    /**
     * Creates a new parse exception with a message and position.
     *
     * @param message  the error message
     * @param position the position in the source where the error occurred
     */
    public CssParseException(String message, Token.Position position) {
        this(message, position, Collections.emptyList());
    }

    /**
     * Creates a new parse exception with a message, position, and diagnostics list.
     *
     * @param message     the error message
     * @param position    the position in the source where the error occurred
     * @param diagnostics the list of diagnostics that caused this validation failure
     */
    public CssParseException(String message, Token.Position position, List<CssDiagnostic> diagnostics) {
        super(formatMessage(message, position));
        this.position = position != null ? position : Token.Position.UNKNOWN;
        this.diagnostics = diagnostics != null
                ? Collections.unmodifiableList(new ArrayList<>(diagnostics))
                : Collections.emptyList();
    }

    /**
     * Creates a new parse exception with a message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public CssParseException(String message, Throwable cause) {
        super(message, cause);
        this.position = Token.Position.UNKNOWN;
        this.diagnostics = Collections.emptyList();
    }

    /**
     * Returns the position in the source where the error occurred.
     *
     * @return the error position
     */
    public Token.Position getPosition() {
        return position;
    }

    /**
     * Returns the position in the source where the error occurred.
     *
     * @return the error position
     */
    public Token.Position position() {
        return position;
    }

    /**
     * Returns the list of diagnostics associated with this exception, if any.
     *
     * @return an unmodifiable list of diagnostics
     */
    public List<CssDiagnostic> diagnostics() {
        return diagnostics;
    }

    private static String formatMessage(String message, Token.Position position) {
        if (position != null && position.line() > 0) {
            return String.format("CSS parse error at line %d, column %d: %s",
                    position.line(), position.column(), message);
        }
        return "CSS parse error: " + message;
    }
}

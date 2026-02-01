/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.parser;

/**
 * Exception thrown when CSS parsing fails.
 */
public final class CssParseException extends RuntimeException {

    /** The source position where the parse error occurred. */
    private final Token.Position position;

    /**
     * Creates a new parse exception with a message and position.
     *
     * @param message  the error message
     * @param position the position in the source where the error occurred
     */
    public CssParseException(String message, Token.Position position) {
        super(formatMessage(message, position));
        this.position = position;
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
    }

    /**
     * Returns the position in the source where the error occurred.
     *
     * @return the error position
     */
    public Token.Position getPosition() {
        return position;
    }

    private static String formatMessage(String message, Token.Position position) {
        if (position.line() > 0) {
            return String.format("CSS parse error at line %d, column %d: %s",
                    position.line(), position.column(), message);
        }
        return "CSS parse error: " + message;
    }
}

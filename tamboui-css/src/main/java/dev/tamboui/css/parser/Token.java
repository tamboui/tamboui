/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.parser;

import java.util.Objects;

/**
 * Represents a token in the CSS lexer output.
 * <p>
 * Tokens are the building blocks produced by the lexer that the parser
 * uses to construct the CSS AST.
 */
public abstract class Token {

    private final Position position;
    private boolean precededByWhitespace;

    /**
     * Creates a token at the given source position.
     *
     * @param position the position in the source
     */
    protected Token(Position position) {
        this.position = position;
        this.precededByWhitespace = false;
    }

    /**
     * Returns the position of this token in the source.
     *
     * @return the source position
     */
    public Position position() {
        return position;
    }

    /**
     * Returns whether this token was preceded by whitespace in the source.
     * This is used to distinguish between compound selectors (Panel.class)
     * and descendant selectors (Panel Button).
     *
     * @return {@code true} if preceded by whitespace
     */
    public boolean precededByWhitespace() {
        return precededByWhitespace;
    }

    /**
     * Sets whether this token was preceded by whitespace.
     */
    void setPrecededByWhitespace(boolean value) {
        this.precededByWhitespace = value;
    }

    /**
     * Position in source code (line and column, 1-based).
     */
    public static final class Position {
        /** Position representing an unknown source location. */
        public static final Position UNKNOWN = new Position(0, 0);

        private final int line;
        private final int column;

        /**
         * Creates a position with the given line and column.
         *
         * @param line   the 1-based line number
         * @param column the 1-based column number
         */
        public Position(int line, int column) {
            this.line = line;
            this.column = column;
        }

        /**
         * Returns the 1-based line number.
         *
         * @return the line number
         */
        public int line() {
            return line;
        }

        /**
         * Returns the 1-based column number.
         *
         * @return the column number
         */
        public int column() {
            return column;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Position)) {
                return false;
            }
            Position position = (Position) o;
            return line == position.line && column == position.column;
        }

        @Override
        public int hashCode() {
            return Objects.hash(line, column);
        }

        @Override
        public String toString() {
            return "Position{line=" + line + ", column=" + column + "}";
        }
    }

    /**
     * An identifier token (e.g., "color", "Panel", "bold").
     */
    public static final class Ident extends Token {
        private final String value;

        /**
         * Creates an identifier token.
         *
         * @param value    the identifier value
         * @param position the source position
         */
        public Ident(String value, Position position) {
            super(position);
            this.value = value;
        }

        /**
         * Returns the identifier value.
         *
         * @return the identifier string
         */
        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return "Ident{" + value + "}";
        }
    }

    /**
     * A string token (e.g., "hello world" or 'hello world').
     */
    public static final class StringToken extends Token {
        private final String value;

        /**
         * Creates a string token.
         *
         * @param value    the string value (without quotes)
         * @param position the source position
         */
        public StringToken(String value, Position position) {
            super(position);
            this.value = value;
        }

        /**
         * Returns the string value (without quotes).
         *
         * @return the string value
         */
        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return "StringToken{" + value + "}";
        }
    }

    /**
     * A number token (e.g., "42", "3.14", "50%").
     */
    public static final class Number extends Token {
        private final String value;
        private final boolean isPercentage;

        /**
         * Creates a number token.
         *
         * @param value        the numeric value as a string
         * @param isPercentage whether the number is followed by a percent sign
         * @param position     the source position
         */
        public Number(String value, boolean isPercentage, Position position) {
            super(position);
            this.value = value;
            this.isPercentage = isPercentage;
        }

        /**
         * Returns the numeric value as a string.
         *
         * @return the numeric value
         */
        public String value() {
            return value;
        }

        /**
         * Returns whether this number is a percentage value.
         *
         * @return {@code true} if this is a percentage
         */
        public boolean isPercentage() {
            return isPercentage;
        }

        @Override
        public String toString() {
            return "Number{" + value + (isPercentage ? "%" : "") + "}";
        }
    }

    /**
     * A hash token for colors or IDs (e.g., "#ff0000", "#sidebar").
     */
    public static final class Hash extends Token {
        private final String value;

        /**
         * Creates a hash token.
         *
         * @param value    the hash value (without the leading #)
         * @param position the source position
         */
        public Hash(String value, Position position) {
            super(position);
            this.value = value;
        }

        /**
         * Returns the hash value (without the leading #).
         *
         * @return the hash value
         */
        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return "Hash{#" + value + "}";
        }
    }

    /**
     * A variable reference (e.g., "$primary").
     */
    public static final class Variable extends Token {
        private final String name;

        /**
         * Creates a variable reference token.
         *
         * @param name     the variable name (without the leading $)
         * @param position the source position
         */
        public Variable(String name, Position position) {
            super(position);
            this.name = name;
        }

        /**
         * Returns the variable name (without the leading $).
         *
         * @return the variable name
         */
        public String name() {
            return name;
        }

        @Override
        public String toString() {
            return "Variable{$" + name + "}";
        }
    }

    /**
     * A delimiter character (e.g., ".", "&gt;", "*", "&amp;").
     */
    public static final class Delim extends Token {
        private final char value;

        /**
         * Creates a delimiter token.
         *
         * @param value    the delimiter character
         * @param position the source position
         */
        public Delim(char value, Position position) {
            super(position);
            this.value = value;
        }

        /**
         * Returns the delimiter character.
         *
         * @return the delimiter character
         */
        public char value() {
            return value;
        }

        @Override
        public String toString() {
            return "Delim{" + value + "}";
        }
    }

    /**
     * A colon token ":".
     */
    public static final class Colon extends Token {
        /**
         * Creates a colon token.
         *
         * @param position the source position
         */
        public Colon(Position position) {
            super(position);
        }

        @Override
        public String toString() {
            return "Colon";
        }
    }

    /**
     * A semicolon token ";".
     */
    public static final class Semicolon extends Token {
        /**
         * Creates a semicolon token.
         *
         * @param position the source position
         */
        public Semicolon(Position position) {
            super(position);
        }

        @Override
        public String toString() {
            return "Semicolon";
        }
    }

    /**
     * An opening brace "{".
     */
    public static final class OpenBrace extends Token {
        /**
         * Creates an opening brace token.
         *
         * @param position the source position
         */
        public OpenBrace(Position position) {
            super(position);
        }

        @Override
        public String toString() {
            return "OpenBrace";
        }
    }

    /**
     * A closing brace "}".
     */
    public static final class CloseBrace extends Token {
        /**
         * Creates a closing brace token.
         *
         * @param position the source position
         */
        public CloseBrace(Position position) {
            super(position);
        }

        @Override
        public String toString() {
            return "CloseBrace";
        }
    }

    /**
     * An opening parenthesis "(".
     */
    public static final class OpenParen extends Token {
        /**
         * Creates an opening parenthesis token.
         *
         * @param position the source position
         */
        public OpenParen(Position position) {
            super(position);
        }

        @Override
        public String toString() {
            return "OpenParen";
        }
    }

    /**
     * A closing parenthesis ")".
     */
    public static final class CloseParen extends Token {
        /**
         * Creates a closing parenthesis token.
         *
         * @param position the source position
         */
        public CloseParen(Position position) {
            super(position);
        }

        @Override
        public String toString() {
            return "CloseParen";
        }
    }

    /**
     * A comma ",".
     */
    public static final class Comma extends Token {
        /**
         * Creates a comma token.
         *
         * @param position the source position
         */
        public Comma(Position position) {
            super(position);
        }

        @Override
        public String toString() {
            return "Comma";
        }
    }

    /**
     * An opening bracket "[".
     */
    public static final class OpenBracket extends Token {
        /**
         * Creates an opening bracket token.
         *
         * @param position the source position
         */
        public OpenBracket(Position position) {
            super(position);
        }

        @Override
        public String toString() {
            return "OpenBracket";
        }
    }

    /**
     * A closing bracket "]".
     */
    public static final class CloseBracket extends Token {
        /**
         * Creates a closing bracket token.
         *
         * @param position the source position
         */
        public CloseBracket(Position position) {
            super(position);
        }

        @Override
        public String toString() {
            return "CloseBracket";
        }
    }

    /**
     * Whitespace (spaces, tabs, newlines).
     * Usually filtered out before parsing but useful for preserving formatting.
     */
    public static final class Whitespace extends Token {
        private final String value;

        /**
         * Creates a whitespace token.
         *
         * @param value    the whitespace characters
         * @param position the source position
         */
        public Whitespace(String value, Position position) {
            super(position);
            this.value = value;
        }

        /**
         * Returns the whitespace characters.
         *
         * @return the whitespace string
         */
        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return "Whitespace";
        }
    }

    /**
     * End of file marker.
     */
    public static final class EOF extends Token {
        /**
         * Creates an end-of-file token.
         *
         * @param position the source position
         */
        public EOF(Position position) {
            super(position);
        }

        @Override
        public String toString() {
            return "EOF";
        }
    }
}

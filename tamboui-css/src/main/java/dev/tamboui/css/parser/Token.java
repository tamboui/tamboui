/*
 * Copyright (c) 2025 TamboUI Contributors
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

    protected Token(Position position) {
        this.position = position;
        this.precededByWhitespace = false;
    }

    /**
     * Returns the position of this token in the source.
     */
    public Position position() {
        return position;
    }

    /**
     * Returns whether this token was preceded by whitespace in the source.
     * This is used to distinguish between compound selectors (Panel.class)
     * and descendant selectors (Panel Button).
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
        public static final Position UNKNOWN = new Position(0, 0);

        private final int line;
        private final int column;

        public Position(int line, int column) {
            this.line = line;
            this.column = column;
        }

        public int line() {
            return line;
        }

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

        public Ident(String value, Position position) {
            super(position);
            this.value = value;
        }

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

        public StringToken(String value, Position position) {
            super(position);
            this.value = value;
        }

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

        public Number(String value, boolean isPercentage, Position position) {
            super(position);
            this.value = value;
            this.isPercentage = isPercentage;
        }

        public String value() {
            return value;
        }

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

        public Hash(String value, Position position) {
            super(position);
            this.value = value;
        }

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

        public Variable(String name, Position position) {
            super(position);
            this.name = name;
        }

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

        public Delim(char value, Position position) {
            super(position);
            this.value = value;
        }

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

        public Whitespace(String value, Position position) {
            super(position);
            this.value = value;
        }

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
        public EOF(Position position) {
            super(position);
        }

        @Override
        public String toString() {
            return "EOF";
        }
    }
}

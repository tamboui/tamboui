/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Lexer for Textual-style CSS.
 * <p>
 * Tokenizes CSS input into a stream of tokens for the parser.
 * Supports standard CSS syntax plus Textual extensions like variables ($name).
 */
public final class CssLexer {

    private final String input;
    private int pos;
    private int line;
    private int column;

    /**
     * Creates a new lexer for the given CSS input.
     *
     * @param input the CSS source code
     */
    public CssLexer(String input) {
        this.input = input;
        this.pos = 0;
        this.line = 1;
        this.column = 1;
    }

    /**
     * Tokenizes the entire input and returns a list of tokens.
     * <p>
     * Whitespace tokens are included in the output. The list always
     * ends with an EOF token.
     *
     * @return the list of tokens
     * @throws CssParseException if the input contains invalid syntax
     */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();

        while (!isAtEnd()) {
            tokens.add(nextToken());
        }

        tokens.add(new Token.EOF(currentPosition()));
        return tokens;
    }

    /**
     * Tokenizes the input, filtering out whitespace tokens.
     * Each token has its {@code precededByWhitespace} flag set appropriately.
     *
     * @return the list of non-whitespace tokens
     * @throws CssParseException if the input contains invalid syntax
     */
    public List<Token> tokenizeFiltered() {
        List<Token> tokens = new ArrayList<>();
        boolean lastWasWhitespace = false;

        while (!isAtEnd()) {
            Token token = nextToken();
            if (token instanceof Token.Whitespace) {
                lastWasWhitespace = true;
            } else {
                token.setPrecededByWhitespace(lastWasWhitespace);
                tokens.add(token);
                lastWasWhitespace = false;
            }
        }

        Token eof = new Token.EOF(currentPosition());
        eof.setPrecededByWhitespace(lastWasWhitespace);
        tokens.add(eof);
        return tokens;
    }

    private Token nextToken() {
        char c = peek();

        // Skip comments
        if (c == '/' && peekNext() == '*') {
            skipComment();
            if (isAtEnd()) {
                return new Token.EOF(currentPosition());
            }
            c = peek();
        }

        Token.Position startPos = currentPosition();

        // Whitespace
        if (Character.isWhitespace(c)) {
            return readWhitespace(startPos);
        }

        // Variable ($name) - only if followed by identifier character
        // Otherwise $ is a delimiter (e.g., in $= attribute selector operator)
        if (c == '$' && isIdentStart(peekNext())) {
            return readVariable(startPos);
        }

        // Hash (#id or #color)
        if (c == '#') {
            return readHash(startPos);
        }

        // String
        if (c == '"' || c == '\'') {
            return readString(startPos);
        }

        // Number
        if (Character.isDigit(c) || (c == '-' && Character.isDigit(peekNext()))) {
            return readNumber(startPos);
        }

        // Identifier
        if (isIdentStart(c)) {
            return readIdent(startPos);
        }

        // Single-character tokens
        advance();
        switch (c) {
            case '{':
                return new Token.OpenBrace(startPos);
            case '}':
                return new Token.CloseBrace(startPos);
            case '(':
                return new Token.OpenParen(startPos);
            case ')':
                return new Token.CloseParen(startPos);
            case '[':
                return new Token.OpenBracket(startPos);
            case ']':
                return new Token.CloseBracket(startPos);
            case ':':
                return new Token.Colon(startPos);
            case ';':
                return new Token.Semicolon(startPos);
            case ',':
                return new Token.Comma(startPos);
            case '.':
            case '>':
            case '*':
            case '&':
            case '!':
            case '=':
            case '^':
            case '$':
            case '~':
                return new Token.Delim(c, startPos);
            default:
                throw new CssParseException("Unexpected character: " + c, startPos);
        }
    }

    private Token.Whitespace readWhitespace(Token.Position startPos) {
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && Character.isWhitespace(peek())) {
            sb.append(advance());
        }
        return new Token.Whitespace(sb.toString(), startPos);
    }

    private Token.Variable readVariable(Token.Position startPos) {
        advance(); // consume '$'
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && isIdentChar(peek())) {
            sb.append(advance());
        }
        if (sb.length() == 0) {
            throw new CssParseException("Expected variable name after $", startPos);
        }
        return new Token.Variable(sb.toString(), startPos);
    }

    private Token.Hash readHash(Token.Position startPos) {
        advance(); // consume '#'
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && isIdentChar(peek())) {
            sb.append(advance());
        }
        if (sb.length() == 0) {
            throw new CssParseException("Expected identifier after #", startPos);
        }
        return new Token.Hash(sb.toString(), startPos);
    }

    private Token.StringToken readString(Token.Position startPos) {
        char quote = advance(); // consume opening quote
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && peek() != quote) {
            char c = advance();
            if (c == '\\' && !isAtEnd()) {
                sb.append(advance()); // escape sequence
            } else {
                sb.append(c);
            }
        }
        if (isAtEnd()) {
            throw new CssParseException("Unterminated string", startPos);
        }
        advance(); // consume closing quote
        return new Token.StringToken(sb.toString(), startPos);
    }

    private Token.Number readNumber(Token.Position startPos) {
        StringBuilder sb = new StringBuilder();
        if (peek() == '-') {
            sb.append(advance());
        }
        while (!isAtEnd() && Character.isDigit(peek())) {
            sb.append(advance());
        }
        // Decimal part
        if (!isAtEnd() && peek() == '.' && Character.isDigit(peekNext())) {
            sb.append(advance()); // consume '.'
            while (!isAtEnd() && Character.isDigit(peek())) {
                sb.append(advance());
            }
        }
        // Percentage
        boolean isPercentage = false;
        if (!isAtEnd() && peek() == '%') {
            isPercentage = true;
            advance();
        }
        return new Token.Number(sb.toString(), isPercentage, startPos);
    }

    private Token.Ident readIdent(Token.Position startPos) {
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && isIdentChar(peek())) {
            sb.append(advance());
        }
        return new Token.Ident(sb.toString(), startPos);
    }

    private void skipComment() {
        advance(); // consume '/'
        advance(); // consume '*'
        while (!isAtEnd()) {
            if (peek() == '*' && peekNext() == '/') {
                advance(); // consume '*'
                advance(); // consume '/'
                return;
            }
            advance();
        }
        // Unterminated comment - silently end at EOF
    }

    private boolean isIdentStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '-';
    }

    private boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }

    private boolean isAtEnd() {
        return pos >= input.length();
    }

    private char peek() {
        return isAtEnd() ? '\0' : input.charAt(pos);
    }

    private char peekNext() {
        return pos + 1 >= input.length() ? '\0' : input.charAt(pos + 1);
    }

    private char advance() {
        char c = input.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private Token.Position currentPosition() {
        return new Token.Position(line, column);
    }
}

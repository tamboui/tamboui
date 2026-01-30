/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.selector;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses CSS selector strings into {@link Selector} objects.
 * <p>
 * This parser supports the full CSS selector syntax used by the tamboui CSS
 * module:
 * <ul>
 * <li>{@code *} - Universal selector</li>
 * <li>{@code Type} - Type selector</li>
 * <li>{@code #id} - ID selector</li>
 * <li>{@code .class} - Class selector</li>
 * <li>{@code :pseudo} - Pseudo-class selector</li>
 * <li>{@code [attr]} - Attribute existence selector</li>
 * <li>{@code [attr=value]} - Attribute equals selector</li>
 * <li>{@code [attr^=value]} - Attribute starts-with selector</li>
 * <li>{@code [attr$=value]} - Attribute ends-with selector</li>
 * <li>{@code [attr*=value]} - Attribute contains selector</li>
 * <li>{@code A B} - Descendant combinator</li>
 * <li>{@code A > B} - Child combinator</li>
 * <li>{@code A.class#id} - Compound selectors</li>
 * </ul>
 * <p>
 * <b>Usage:</b>
 * 
 * <pre>{@code
 * Selector selector = SelectorParser.parse(".primary.large");
 * boolean matches = selector.matches(element, state, ancestors);
 * }</pre>
 */
public final class SelectorParser {

    private final String input;
    private int pos;

    private SelectorParser(String input) {
        this.input = input;
        this.pos = 0;
    }

    /**
     * Parses a CSS selector string into a Selector object.
     *
     * @param selector
     *            the CSS selector string
     * @return the parsed Selector
     * @throws IllegalArgumentException
     *             if the selector is invalid
     */
    public static Selector parse(String selector) {
        if (selector == null || selector.isEmpty()) {
            throw new IllegalArgumentException("Selector cannot be null or empty");
        }
        return new SelectorParser(selector.trim()).parseSelector();
    }

    private Selector parseSelector() {
        Selector left = parseCompoundSelector();

        while (pos < input.length()) {
            skipWhitespace();
            if (pos >= input.length()) {
                break;
            }

            char c = input.charAt(pos);
            if (c == '>') {
                // Child combinator
                pos++;
                skipWhitespace();
                Selector right = parseCompoundSelector();
                left = new ChildSelector(left, right);
            } else if (isSimpleSelectorStart(c)) {
                // Descendant combinator (whitespace was already consumed)
                Selector right = parseCompoundSelector();
                left = new DescendantSelector(left, right);
            } else {
                break;
            }
        }

        return left;
    }

    private Selector parseCompoundSelector() {
        skipWhitespace();
        List<Selector> parts = new ArrayList<>();

        while (pos < input.length()) {
            char c = input.charAt(pos);

            if (c == '*') {
                pos++;
                parts.add(UniversalSelector.INSTANCE);
            } else if (c == '#') {
                parts.add(parseIdSelector());
            } else if (c == '.') {
                parts.add(parseClassSelector());
            } else if (c == ':') {
                parts.add(parsePseudoClassSelector());
            } else if (c == '[') {
                parts.add(parseAttributeSelector());
            } else if (Character.isLetter(c)) {
                parts.add(parseTypeSelector());
            } else {
                break;
            }
        }

        if (parts.isEmpty()) {
            throw new IllegalArgumentException("Expected selector at position " + pos);
        }

        return parts.size() == 1 ? parts.get(0) : new CompoundSelector(parts);
    }

    private Selector parseTypeSelector() {
        int start = pos;
        while (pos < input.length() && isIdentChar(input.charAt(pos))) {
            pos++;
        }
        return new TypeSelector(input.substring(start, pos));
    }

    private Selector parseIdSelector() {
        pos++; // consume '#'
        int start = pos;
        while (pos < input.length() && isIdentChar(input.charAt(pos))) {
            pos++;
        }
        if (start == pos) {
            throw new IllegalArgumentException("Expected ID name after '#' at position " + start);
        }
        return new IdSelector(input.substring(start, pos));
    }

    private Selector parseClassSelector() {
        pos++; // consume '.'
        int start = pos;
        while (pos < input.length() && isIdentChar(input.charAt(pos))) {
            pos++;
        }
        if (start == pos) {
            throw new IllegalArgumentException(
                    "Expected class name after '.' at position " + start);
        }
        return new ClassSelector(input.substring(start, pos));
    }

    private Selector parsePseudoClassSelector() {
        pos++; // consume ':'
        int start = pos;
        while (pos < input.length()
                && (isIdentChar(input.charAt(pos)) || input.charAt(pos) == '-')) {
            pos++;
        }
        if (start == pos) {
            throw new IllegalArgumentException(
                    "Expected pseudo-class name after ':' at position " + start);
        }
        String name = input.substring(start, pos);

        // Handle functional pseudo-classes like :nth-child(even)
        if (pos < input.length() && input.charAt(pos) == '(') {
            pos++; // consume '('
            int argStart = pos;
            int depth = 1;
            while (pos < input.length() && depth > 0) {
                char c = input.charAt(pos);
                if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                }
                if (depth > 0) {
                    pos++;
                }
            }
            String args = input.substring(argStart, pos).trim();
            if (pos < input.length() && input.charAt(pos) == ')') {
                pos++; // consume ')'
            }
            name = name + "(" + args + ")";
        }

        return new PseudoClassSelector(name);
    }

    private Selector parseAttributeSelector() {
        pos++; // consume '['
        skipWhitespace();

        // Parse attribute name
        int nameStart = pos;
        while (pos < input.length() && isIdentChar(input.charAt(pos))) {
            pos++;
        }
        if (nameStart == pos) {
            throw new IllegalArgumentException(
                    "Expected attribute name after '[' at position " + nameStart);
        }
        String attrName = input.substring(nameStart, pos);

        skipWhitespace();

        // Check for operator
        if (pos < input.length() && input.charAt(pos) == ']') {
            pos++; // consume ']'
            return new AttributeSelector(attrName);
        }

        // Parse operator
        AttributeSelector.Operator operator;
        char c = input.charAt(pos);
        if (c == '=') {
            operator = AttributeSelector.Operator.EQUALS;
            pos++;
        } else if (c == '^' && pos + 1 < input.length() && input.charAt(pos + 1) == '=') {
            operator = AttributeSelector.Operator.STARTS_WITH;
            pos += 2;
        } else if (c == '$' && pos + 1 < input.length() && input.charAt(pos + 1) == '=') {
            operator = AttributeSelector.Operator.ENDS_WITH;
            pos += 2;
        } else if (c == '*' && pos + 1 < input.length() && input.charAt(pos + 1) == '=') {
            operator = AttributeSelector.Operator.CONTAINS;
            pos += 2;
        } else {
            throw new IllegalArgumentException("Expected attribute operator at position " + pos);
        }

        skipWhitespace();

        // Parse value (string or ident)
        String value;
        c = input.charAt(pos);
        if (c == '"' || c == '\'') {
            value = parseString(c);
        } else {
            int valueStart = pos;
            while (pos < input.length() && input.charAt(pos) != ']'
                    && !Character.isWhitespace(input.charAt(pos))) {
                pos++;
            }
            value = input.substring(valueStart, pos);
        }

        skipWhitespace();

        if (pos >= input.length() || input.charAt(pos) != ']') {
            throw new IllegalArgumentException("Expected ']' at position " + pos);
        }
        pos++; // consume ']'

        return new AttributeSelector(attrName, operator, value);
    }

    private String parseString(char quote) {
        pos++; // consume opening quote
        StringBuilder sb = new StringBuilder();
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == quote) {
                pos++; // consume closing quote
                return sb.toString();
            } else if (c == '\\' && pos + 1 < input.length()) {
                pos++;
                sb.append(input.charAt(pos));
            } else {
                sb.append(c);
            }
            pos++;
        }
        throw new IllegalArgumentException("Unterminated string");
    }

    private void skipWhitespace() {
        while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
            pos++;
        }
    }

    private boolean isIdentChar(char c) {
        return Character.isLetterOrDigit(c) || c == '-' || c == '_';
    }

    private boolean isSimpleSelectorStart(char c) {
        return c == '*' || c == '#' || c == '.' || c == ':' || c == '[' || Character.isLetter(c);
    }
}

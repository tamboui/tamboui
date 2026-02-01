/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.parser;

import dev.tamboui.css.model.PropertyValue;
import dev.tamboui.css.model.Rule;
import dev.tamboui.css.model.Stylesheet;
import dev.tamboui.css.selector.*;

import java.util.*;

/**
 * Recursive descent parser for Textual-style CSS.
 * <p>
 * Parses CSS input into a Stylesheet containing variables and rules.
 * Supports:
 * <ul>
 *   <li>Variables: {@code $name: value;}</li>
 *   <li>Type selectors: {@code Panel { ... }}</li>
 *   <li>ID selectors: {@code #sidebar { ... }}</li>
 *   <li>Class selectors: {@code .error { ... }}</li>
 *   <li>Universal selector: {@code * { ... }}</li>
 *   <li>Pseudo-classes: {@code :focus, :hover, :disabled}</li>
 *   <li>Compound selectors: {@code Panel.primary#sidebar}</li>
 *   <li>Descendant combinator: {@code Panel Button { ... }}</li>
 *   <li>Child combinator: {@code Panel > Button { ... }}</li>
 *   <li>Nested rules: {@code Panel { &:focus { ... } }}</li>
 *   <li>Selector lists: {@code .foo, .bar { ... }}</li>
 *   <li>Attribute selectors: {@code Panel[title="Test"] { ... }}</li>
 * </ul>
 */
public final class CssParser {

    private final List<Token> tokens;
    private int pos;
    private int ruleOrder;

    private CssParser(List<Token> tokens) {
        this.tokens = tokens;
        this.pos = 0;
        this.ruleOrder = 0;
    }

    /**
     * Parses CSS input into a stylesheet.
     *
     * @param css the CSS source code
     * @return the parsed stylesheet
     * @throws CssParseException if parsing fails
     */
    public static Stylesheet parse(String css) {
        CssLexer lexer = new CssLexer(css);
        List<Token> tokens = lexer.tokenizeFiltered();
        CssParser parser = new CssParser(tokens);
        return parser.parseStylesheet();
    }

    private Stylesheet parseStylesheet() {
        Map<String, String> variables = new LinkedHashMap<>();
        List<Rule> rules = new ArrayList<>();

        while (!isAtEnd()) {
            if (check(Token.Variable.class)) {
                parseVariable(variables);
            } else {
                rules.addAll(parseRule(Collections.<Selector>emptyList()));
            }
        }

        return new Stylesheet(variables, rules);
    }

    private void parseVariable(Map<String, String> variables) {
        Token.Variable varToken = consume(Token.Variable.class, "Expected variable");
        consume(Token.Colon.class, "Expected ':' after variable name");

        StringBuilder value = new StringBuilder();
        boolean first = true;
        while (!check(Token.Semicolon.class) && !isAtEnd()) {
            Token t = advance();
            // Preserve whitespace between value tokens
            if (!first && t.precededByWhitespace()) {
                value.append(" ");
            }
            value.append(tokenToString(t));
            first = false;
        }
        consume(Token.Semicolon.class, "Expected ';' after variable value");

        variables.put(varToken.name(), value.toString().trim());
    }

    private List<Rule> parseRule(List<Selector> parentSelectors) {
        List<Selector> selectors = parseSelectorList();

        // Apply parent context for nested rules with &
        if (!parentSelectors.isEmpty()) {
            List<Selector> combined = new ArrayList<>();
            for (Selector selector : selectors) {
                combined.add(combineWithParent(parentSelectors, selector));
            }
            selectors = combined;
        }

        consume(Token.OpenBrace.class, "Expected '{' after selector");

        Map<String, PropertyValue> declarations = new LinkedHashMap<>();
        List<Rule> nestedRules = new ArrayList<>();

        while (!check(Token.CloseBrace.class) && !isAtEnd()) {
            // Check for nested rule (starts with &, selector, or nested block)
            if (check(Token.Delim.class) && peekDelim() == '&') {
                // Nested rule with &
                nestedRules.addAll(parseRule(selectors));
            } else if (isStartOfSelector()) {
                // Could be nested rule or property - look ahead
                if (lookaheadIsNestedRule()) {
                    nestedRules.addAll(parseRule(selectors));
                } else {
                    parseDeclaration(declarations);
                }
            } else if (check(Token.Ident.class)) {
                parseDeclaration(declarations);
            } else {
                throw error("Unexpected token in rule body");
            }
        }

        consume(Token.CloseBrace.class, "Expected '}' after declarations");

        List<Rule> result = new ArrayList<>();
        if (!declarations.isEmpty()) {
            // All selectors in a selector list share the same source order
            int order = ruleOrder++;
            for (Selector selector : selectors) {
                result.add(new Rule(selector, declarations, order));
            }
        }
        result.addAll(nestedRules);

        return result;
    }

    private List<Selector> parseSelectorList() {
        List<Selector> selectors = new ArrayList<>();
        selectors.add(parseSelector());
        while (check(Token.Comma.class)) {
            advance(); // consume ','
            selectors.add(parseSelector());
        }
        return selectors;
    }

    private void parseDeclaration(Map<String, PropertyValue> declarations) {
        Token.Ident property = consume(Token.Ident.class, "Expected property name");
        consume(Token.Colon.class, "Expected ':' after property name");

        StringBuilder value = new StringBuilder();
        boolean important = false;
        boolean first = true;

        while (!check(Token.Semicolon.class) && !check(Token.CloseBrace.class) && !isAtEnd()) {
            Token token = peek();
            if (token instanceof Token.Delim && ((Token.Delim) token).value() == '!') {
                advance();
                Token next = advance();
                if (next instanceof Token.Ident && ((Token.Ident) next).value().equals("important")) {
                    important = true;
                } else {
                    throw error("Expected 'important' after '!'");
                }
            } else {
                Token t = advance();
                // Preserve whitespace between value tokens
                if (!first && t.precededByWhitespace()) {
                    value.append(" ");
                }
                value.append(tokenToString(t));
                first = false;
            }
        }

        if (check(Token.Semicolon.class)) {
            advance();
        }

        String trimmedValue = value.toString().trim();
        declarations.put(property.value(),
                new PropertyValue(trimmedValue, important));
    }

    private Selector parseSelector() {
        Selector left = parseCompoundSelector();

        while (true) {
            if (check(Token.Delim.class) && peekDelim() == '>') {
                advance(); // consume '>'
                Selector right = parseCompoundSelector();
                left = new ChildSelector(left, right);
            } else if (isStartOfSimpleSelector() && peek().precededByWhitespace()) {
                // Descendant combinator (whitespace between selectors)
                Selector right = parseCompoundSelector();
                left = new DescendantSelector(left, right);
            } else {
                break;
            }
        }

        return left;
    }

    private Selector parseCompoundSelector() {
        List<Selector> parts = new ArrayList<>();

        // Handle & at start of selector
        if (check(Token.Delim.class) && peekDelim() == '&') {
            advance(); // consume '&'
            // & by itself will be handled by parent combination
            // Continue parsing additional parts (like :focus)
        }

        // Parse first simple selector
        if (isStartOfSimpleSelector()) {
            parts.add(parseSimpleSelector());
        }

        // Parse additional simple selectors that are NOT preceded by whitespace
        // (whitespace indicates descendant combinator, not compound selector)
        while (isStartOfSimpleSelector() && !peek().precededByWhitespace()) {
            parts.add(parseSimpleSelector());
        }

        if (parts.isEmpty()) {
            throw error("Expected selector");
        }

        return parts.size() == 1 ? parts.get(0) : new CompoundSelector(parts);
    }

    private Selector parseSimpleSelector() {
        Token token = peek();

        if (token instanceof Token.Ident) {
            advance();
            return new TypeSelector(((Token.Ident) token).value());
        }

        if (token instanceof Token.Hash) {
            advance();
            return new IdSelector(((Token.Hash) token).value());
        }

        if (token instanceof Token.Delim) {
            Token.Delim delim = (Token.Delim) token;
            if (delim.value() == '.') {
                advance();
                Token.Ident className = consume(Token.Ident.class, "Expected class name after '.'");
                return new ClassSelector(className.value());
            }
            if (delim.value() == '*') {
                advance();
                return UniversalSelector.INSTANCE;
            }
        }

        if (token instanceof Token.Colon) {
            advance();
            Token.Ident pseudoClass = consume(Token.Ident.class, "Expected pseudo-class name after ':'");
            String name = pseudoClass.value();

            // Handle functional pseudo-classes like :nth-child(even)
            if (check(Token.OpenParen.class)) {
                advance(); // consume '('
                StringBuilder args = new StringBuilder();
                while (!check(Token.CloseParen.class) && !isAtEnd()) {
                    Token t = advance();
                    args.append(tokenToString(t));
                }
                consume(Token.CloseParen.class, "Expected ')' after pseudo-class arguments");
                name = name + "(" + args.toString() + ")";
            }

            return new PseudoClassSelector(name);
        }

        if (token instanceof Token.OpenBracket) {
            return parseAttributeSelector();
        }

        throw error("Expected selector");
    }

    private Selector parseAttributeSelector() {
        advance(); // consume '['
        Token.Ident attrName = consume(Token.Ident.class, "Expected attribute name");

        // Check for operator
        if (check(Token.CloseBracket.class)) {
            // [attr] - existence check
            advance();
            return new AttributeSelector(attrName.value());
        }

        // Parse operator: =, ^=, $=, *=
        AttributeSelector.Operator operator;
        if (check(Token.Delim.class)) {
            char c = peekDelim();
            if (c == '=') {
                advance();
                operator = AttributeSelector.Operator.EQUALS;
            } else if (c == '^') {
                advance();
                consume(Token.Delim.class, "Expected '=' after '^'");
                operator = AttributeSelector.Operator.STARTS_WITH;
            } else if (c == '$') {
                advance();
                consume(Token.Delim.class, "Expected '=' after '$'");
                operator = AttributeSelector.Operator.ENDS_WITH;
            } else if (c == '*') {
                advance();
                consume(Token.Delim.class, "Expected '=' after '*'");
                operator = AttributeSelector.Operator.CONTAINS;
            } else {
                throw error("Expected attribute selector operator");
            }
        } else {
            throw error("Expected attribute selector operator or ']'");
        }

        // Parse value (string or ident)
        String value;
        if (check(Token.StringToken.class)) {
            value = ((Token.StringToken) advance()).value();
        } else if (check(Token.Ident.class)) {
            value = ((Token.Ident) advance()).value();
        } else {
            throw error("Expected attribute value");
        }

        consume(Token.CloseBracket.class, "Expected ']' after attribute value");
        return new AttributeSelector(attrName.value(), operator, value);
    }

    private boolean isStartOfSelector() {
        Token token = peek();
        if (token instanceof Token.Ident) {
            return true;
        }
        if (token instanceof Token.Hash) {
            return true;
        }
        if (token instanceof Token.Colon) {
            return true;
        }
        if (token instanceof Token.OpenBracket) {
            return true;
        }
        if (token instanceof Token.Delim) {
            char c = ((Token.Delim) token).value();
            return c == '.' || c == '*' || c == '&';
        }
        return false;
    }

    private boolean isStartOfSimpleSelector() {
        Token token = peek();
        if (token instanceof Token.Ident) {
            return true;
        }
        if (token instanceof Token.Hash) {
            return true;
        }
        if (token instanceof Token.Colon) {
            return true;
        }
        if (token instanceof Token.OpenBracket) {
            return true;
        }
        if (token instanceof Token.Delim) {
            char c = ((Token.Delim) token).value();
            return c == '.' || c == '*';
        }
        return false;
    }

    private boolean lookaheadIsNestedRule() {
        // Save position
        int savedPos = pos;

        try {
            // Skip potential selector
            while (isStartOfSimpleSelector() ||
                   (check(Token.Delim.class) && peekDelim() == '>')) {
                // Skip attribute selectors entirely
                if (check(Token.OpenBracket.class)) {
                    advance(); // consume '['
                    while (!check(Token.CloseBracket.class) && !isAtEnd()) {
                        advance();
                    }
                    if (check(Token.CloseBracket.class)) {
                        advance(); // consume ']'
                    }
                } else {
                    advance();
                }
            }
            // If we hit '{', it's a nested rule
            return check(Token.OpenBrace.class);
        } finally {
            pos = savedPos;
        }
    }

    private Selector combineWithParent(List<Selector> parentSelectors, Selector childSelector) {
        if (parentSelectors.isEmpty()) {
            return childSelector;
        }
        // For now, simple descendant combination with first parent
        Selector parent = parentSelectors.get(0);

        // If child is class selectors or pseudo-classes (like &:focus or &.foo), combine as compound
        if (childSelector instanceof PseudoClassSelector ||
            childSelector instanceof ClassSelector ||
            (childSelector instanceof CompoundSelector &&
             allPseudoOrClass((CompoundSelector) childSelector))) {
            if (parent instanceof CompoundSelector) {
                List<Selector> combined = new ArrayList<>(((CompoundSelector) parent).parts());
                if (childSelector instanceof CompoundSelector) {
                    combined.addAll(((CompoundSelector) childSelector).parts());
                } else {
                    combined.add(childSelector);
                }
                return new CompoundSelector(combined);
            } else {
                List<Selector> combined = new ArrayList<>();
                combined.add(parent);
                if (childSelector instanceof CompoundSelector) {
                    combined.addAll(((CompoundSelector) childSelector).parts());
                } else {
                    combined.add(childSelector);
                }
                return new CompoundSelector(combined);
            }
        }

        return new DescendantSelector(parent, childSelector);
    }

    private boolean allPseudoOrClass(CompoundSelector cs) {
        for (Selector s : cs.parts()) {
            if (!(s instanceof PseudoClassSelector) && !(s instanceof ClassSelector)) {
                return false;
            }
        }
        return true;
    }

    private String tokenToString(Token token) {
        if (token instanceof Token.Ident) {
            return ((Token.Ident) token).value();
        }
        if (token instanceof Token.StringToken) {
            return "\"" + ((Token.StringToken) token).value() + "\"";
        }
        if (token instanceof Token.Number) {
            Token.Number num = (Token.Number) token;
            return num.value() + (num.isPercentage() ? "%" : "");
        }
        if (token instanceof Token.Hash) {
            return "#" + ((Token.Hash) token).value();
        }
        if (token instanceof Token.Variable) {
            return "$" + ((Token.Variable) token).name();
        }
        if (token instanceof Token.Delim) {
            return String.valueOf(((Token.Delim) token).value());
        }
        if (token instanceof Token.Colon) {
            return ":";
        }
        if (token instanceof Token.Comma) {
            return ",";
        }
        if (token instanceof Token.OpenParen) {
            return "(";
        }
        if (token instanceof Token.CloseParen) {
            return ")";
        }
        return "";
    }

    private char peekDelim() {
        Token token = peek();
        if (token instanceof Token.Delim) {
            return ((Token.Delim) token).value();
        }
        return '\0';
    }

    private boolean isAtEnd() {
        return peek() instanceof Token.EOF;
    }

    private Token peek() {
        return tokens.get(pos);
    }

    private Token advance() {
        if (!isAtEnd()) {
            pos++;
        }
        return tokens.get(pos - 1);
    }

    private boolean check(Class<? extends Token> type) {
        return type.isInstance(peek());
    }

    @SuppressWarnings("unchecked")
    private <T extends Token> T consume(Class<T> type, String message) {
        if (check(type)) {
            return (T) advance();
        }
        throw error(message);
    }

    private CssParseException error(String message) {
        Token token = peek();
        return new CssParseException(message + " (got " + token.getClass().getSimpleName() + ")",
                token.position());
    }
}

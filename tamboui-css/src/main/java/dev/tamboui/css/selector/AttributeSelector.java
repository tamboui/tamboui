/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.selector;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An attribute selector that matches elements by their style attributes.
 * <p>
 * Supports the following operators:
 * <ul>
 *   <li>{@code [attr]} - attribute exists</li>
 *   <li>{@code [attr=value]} - exact match</li>
 *   <li>{@code [attr^=value]} - starts with</li>
 *   <li>{@code [attr$=value]} - ends with</li>
 *   <li>{@code [attr*=value]} - contains</li>
 * </ul>
 * <p>
 * Example: {@code Panel[title="Test Tree"]} matches Panels with title="Test Tree".
 */
public final class AttributeSelector implements Selector {

    /**
     * The operator for attribute matching.
     */
    public enum Operator {
        /** Attribute exists */
        EXISTS,
        /** Exact match */
        EQUALS,
        /** Starts with */
        STARTS_WITH,
        /** Ends with */
        ENDS_WITH,
        /** Contains */
        CONTAINS
    }

    private final String attribute;
    private final Operator operator;
    private final String value;

    /**
     * Creates an attribute existence selector.
     *
     * @param attribute the attribute name
     */
    public AttributeSelector(String attribute) {
        this.attribute = Objects.requireNonNull(attribute);
        this.operator = Operator.EXISTS;
        this.value = null;
    }

    /**
     * Creates an attribute selector with an operator and value.
     *
     * @param attribute the attribute name
     * @param operator the matching operator
     * @param value the value to match against
     */
    public AttributeSelector(String attribute, Operator operator, String value) {
        this.attribute = Objects.requireNonNull(attribute);
        this.operator = Objects.requireNonNull(operator);
        this.value = value;
    }

    /**
     * Returns the attribute name.
     *
     * @return the attribute name
     */
    public String attribute() {
        return attribute;
    }

    /**
     * Returns the matching operator.
     *
     * @return the matching operator
     */
    public Operator operator() {
        return operator;
    }

    /**
     * Returns the value to match against (null for EXISTS operator).
     *
     * @return the value to match against, or {@code null} for the EXISTS operator
     */
    public String value() {
        return value;
    }

    @Override
    public int specificity() {
        return 10; // Same as class selector (0, 1, 0)
    }

    @Override
    public boolean matches(Styleable element, PseudoClassState state, List<Styleable> ancestors) {
        Map<String, String> attrs = element.styleAttributes();
        String attrValue = attrs.get(attribute);

        switch (operator) {
            case EXISTS:
                return attrValue != null;
            case EQUALS:
                return value.equals(attrValue);
            case STARTS_WITH:
                return attrValue != null && attrValue.startsWith(value);
            case ENDS_WITH:
                return attrValue != null && attrValue.endsWith(value);
            case CONTAINS:
                return attrValue != null && attrValue.contains(value);
            default:
                return false;
        }
    }

    @Override
    public String toCss() {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(attribute);
        switch (operator) {
            case EXISTS:
                break;
            case EQUALS:
                sb.append('=').append('"').append(value).append('"');
                break;
            case STARTS_WITH:
                sb.append("^=").append('"').append(value).append('"');
                break;
            case ENDS_WITH:
                sb.append("$=").append('"').append(value).append('"');
                break;
            case CONTAINS:
                sb.append("*=").append('"').append(value).append('"');
                break;
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AttributeSelector)) {
            return false;
        }
        AttributeSelector that = (AttributeSelector) o;
        return attribute.equals(that.attribute) &&
               operator == that.operator &&
               Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute, operator, value);
    }

    @Override
    public String toString() {
        return "AttributeSelector{" + toCss() + "}";
    }
}

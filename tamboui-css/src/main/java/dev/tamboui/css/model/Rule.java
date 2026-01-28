/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.model;

import dev.tamboui.css.selector.Selector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a CSS rule consisting of a selector and declarations.
 * <p>
 * Example: {@code .error { color: red; text-style: bold; }}
 */
public final class Rule {

    private final Selector selector;
    private final Map<String, PropertyValue> declarations;
    private final int sourceOrder;

    /**
     * Creates a new rule.
     *
     * @param selector     the selector that determines which elements this rule applies to
     * @param declarations the property declarations (property name -> value)
     * @param sourceOrder  the order in which this rule appeared in the stylesheet
     */
    public Rule(Selector selector, Map<String, PropertyValue> declarations, int sourceOrder) {
        this.selector = Objects.requireNonNull(selector);
        this.declarations = Collections.unmodifiableMap(new LinkedHashMap<>(declarations));
        this.sourceOrder = sourceOrder;
    }

    /**
     * Returns the selector that determines which elements this rule applies to.
     *
     * @return the selector
     */
    public Selector selector() {
        return selector;
    }

    /**
     * Returns the property declarations of this rule.
     *
     * @return an unmodifiable map of property name to value
     */
    public Map<String, PropertyValue> declarations() {
        return declarations;
    }

    /**
     * Returns the order in which this rule appeared in the stylesheet.
     *
     * @return the source order index
     */
    public int sourceOrder() {
        return sourceOrder;
    }

    /**
     * Returns the specificity of this rule's selector.
     *
     * @return the specificity value
     */
    public int specificity() {
        return selector.specificity();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rule)) {
            return false;
        }
        Rule rule = (Rule) o;
        return sourceOrder == rule.sourceOrder &&
                selector.equals(rule.selector) &&
                declarations.equals(rule.declarations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selector, declarations, sourceOrder);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(selector.toCss()).append(" {\n");
        for (Map.Entry<String, PropertyValue> entry : declarations.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append(";\n");
        }
        sb.append("}");
        return sb.toString();
    }
}

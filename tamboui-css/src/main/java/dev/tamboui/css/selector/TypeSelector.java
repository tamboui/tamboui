/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.selector;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;

import java.util.List;
import java.util.Objects;

/**
 * A type selector that matches elements by their type name.
 * <p>
 * Example: {@code Panel { ... }} matches all Panel elements.
 * <p>
 * Type selectors also match subclasses. For example, if {@code MyPanel extends Panel},
 * the selector {@code Panel { ... }} will match both Panel and MyPanel elements.
 */
public final class TypeSelector implements Selector {

    private final String typeName;

    /**
     * Creates a type selector for the given type name.
     *
     * @param typeName the element type name
     */
    public TypeSelector(String typeName) {
        this.typeName = Objects.requireNonNull(typeName);
    }

    /**
     * Returns the type name this selector matches.
     *
     * @return the element type name
     */
    public String typeName() {
        return typeName;
    }

    @Override
    public int specificity() {
        return 1; // (0, 0, 1)
    }

    @Override
    public boolean matches(Styleable element, PseudoClassState state, List<Styleable> ancestors) {
        return Styleable.styleTypesOf(element).contains(typeName);
    }

    @Override
    public String toCss() {
        return typeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TypeSelector)) {
            return false;
        }
        TypeSelector that = (TypeSelector) o;
        return typeName.equals(that.typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    @Override
    public String toString() {
        return "TypeSelector{" + typeName + "}";
    }
}

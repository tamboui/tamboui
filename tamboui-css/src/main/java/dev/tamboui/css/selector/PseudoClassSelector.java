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
 * A pseudo-class selector that matches elements in a particular state.
 * <p>
 * Example: {@code :focus { ... }} matches focused elements.
 * <p>
 * Supported pseudo-classes:
 * <ul>
 *   <li>{@code :focus} - element has keyboard focus</li>
 *   <li>{@code :hover} - mouse is over element</li>
 *   <li>{@code :disabled} - element is disabled</li>
 *   <li>{@code :active} - element is being activated (clicked)</li>
 *   <li>{@code :first-child} - element is first child of parent</li>
 *   <li>{@code :last-child} - element is last child of parent</li>
 * </ul>
 */
public final class PseudoClassSelector implements Selector {

    private final String pseudoClass;

    /**
     * Creates a pseudo-class selector for the given pseudo-class name.
     *
     * @param pseudoClass the pseudo-class name (without the leading colon)
     */
    public PseudoClassSelector(String pseudoClass) {
        this.pseudoClass = Objects.requireNonNull(pseudoClass);
    }

    /**
     * Returns the pseudo-class name (without the leading colon).
     *
     * @return the pseudo-class name
     */
    public String pseudoClass() {
        return pseudoClass;
    }

    @Override
    public int specificity() {
        return 10; // (0, 1, 0) - same as class selectors
    }

    @Override
    public boolean matches(Styleable element, PseudoClassState state, List<Styleable> ancestors) {
        return state.has(pseudoClass);
    }

    @Override
    public String toCss() {
        return ":" + pseudoClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PseudoClassSelector)) {
            return false;
        }
        PseudoClassSelector that = (PseudoClassSelector) o;
        return pseudoClass.equals(that.pseudoClass);
    }

    @Override
    public int hashCode() {
        return pseudoClass.hashCode();
    }

    @Override
    public String toString() {
        return "PseudoClassSelector{" + pseudoClass + "}";
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.selector;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.css.cascade.PseudoClassStateProvider;

import java.util.List;
import java.util.Objects;

/**
 * A descendant combinator selector that matches nested elements.
 * <p>
 * Example: {@code Panel Button { ... }} matches any Button that is
 * a descendant (child, grandchild, etc.) of a Panel.
 */
public final class DescendantSelector implements Selector {

    private final Selector ancestor;
    private final Selector descendant;

    /**
     * Creates a descendant combinator selector.
     *
     * @param ancestor   the ancestor selector
     * @param descendant the descendant selector
     */
    public DescendantSelector(Selector ancestor, Selector descendant) {
        this.ancestor = Objects.requireNonNull(ancestor);
        this.descendant = Objects.requireNonNull(descendant);
    }

    /**
     * Returns the ancestor selector.
     *
     * @return the ancestor selector
     */
    public Selector ancestor() {
        return ancestor;
    }

    /**
     * Returns the descendant selector.
     *
     * @return the descendant selector
     */
    public Selector descendant() {
        return descendant;
    }

    @Override
    public int specificity() {
        return ancestor.specificity() + descendant.specificity();
    }

    @Override
    public boolean matches(Styleable element, PseudoClassState state, List<Styleable> ancestors) {
        // First, the descendant selector must match the element
        if (!descendant.matches(element, state, ancestors)) {
            return false;
        }

        // Then, check if any ancestor matches the ancestor selector
        // Note: This uses NONE for ancestors, so pseudo-classes on ancestors won't match.
        // Use matches(element, stateProvider, ancestors) for proper pseudo-class support.
        for (int i = ancestors.size() - 1; i >= 0; i--) {
            Styleable ancestorElement = ancestors.get(i);
            List<Styleable> ancestorAncestors = ancestors.subList(0, i);
            if (ancestor.matches(ancestorElement, PseudoClassState.NONE, ancestorAncestors)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean matches(Styleable element, PseudoClassStateProvider stateProvider, List<Styleable> ancestors) {
        // First, the descendant selector must match the element
        if (!descendant.matches(element, stateProvider, ancestors)) {
            return false;
        }

        // Then, check if any ancestor matches the ancestor selector
        // Use the state provider to get the correct pseudo-class state for each ancestor
        for (int i = ancestors.size() - 1; i >= 0; i--) {
            Styleable ancestorElement = ancestors.get(i);
            List<Styleable> ancestorAncestors = ancestors.subList(0, i);
            if (ancestor.matches(ancestorElement, stateProvider, ancestorAncestors)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toCss() {
        return ancestor.toCss() + " " + descendant.toCss();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DescendantSelector)) {
            return false;
        }
        DescendantSelector that = (DescendantSelector) o;
        return ancestor.equals(that.ancestor) && descendant.equals(that.descendant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ancestor, descendant);
    }

    @Override
    public String toString() {
        return "DescendantSelector{" + toCss() + "}";
    }
}

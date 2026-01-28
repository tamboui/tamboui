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
 * A child combinator selector that matches direct children.
 * <p>
 * Example: {@code Panel > Button { ... }} matches a Button that is
 * a direct child of a Panel (not a grandchild or deeper).
 */
public final class ChildSelector implements Selector {

    private final Selector parent;
    private final Selector child;

    /**
     * Creates a child combinator selector.
     *
     * @param parent the parent selector
     * @param child  the child selector
     */
    public ChildSelector(Selector parent, Selector child) {
        this.parent = Objects.requireNonNull(parent);
        this.child = Objects.requireNonNull(child);
    }

    /**
     * Returns the parent selector.
     *
     * @return the parent selector
     */
    public Selector parent() {
        return parent;
    }

    /**
     * Returns the child selector.
     *
     * @return the child selector
     */
    public Selector child() {
        return child;
    }

    @Override
    public int specificity() {
        return parent.specificity() + child.specificity();
    }

    @Override
    public boolean matches(Styleable element, PseudoClassState state, List<Styleable> ancestors) {
        // First, the child selector must match the element
        if (!child.matches(element, state, ancestors)) {
            return false;
        }

        // Then, check if the immediate parent matches the parent selector
        // Note: This uses NONE for the parent, so pseudo-classes on parent won't match.
        // Use matches(element, stateProvider, ancestors) for proper pseudo-class support.
        if (ancestors.isEmpty()) {
            return false;
        }

        Styleable parentElement = ancestors.get(ancestors.size() - 1);
        List<Styleable> parentAncestors = ancestors.subList(0, ancestors.size() - 1);
        return parent.matches(parentElement, PseudoClassState.NONE, parentAncestors);
    }

    @Override
    public boolean matches(Styleable element, PseudoClassStateProvider stateProvider, List<Styleable> ancestors) {
        // First, the child selector must match the element
        if (!child.matches(element, stateProvider, ancestors)) {
            return false;
        }

        // Then, check if the immediate parent matches the parent selector
        // Use the state provider to get the correct pseudo-class state for the parent
        if (ancestors.isEmpty()) {
            return false;
        }

        Styleable parentElement = ancestors.get(ancestors.size() - 1);
        List<Styleable> parentAncestors = ancestors.subList(0, ancestors.size() - 1);
        return parent.matches(parentElement, stateProvider, parentAncestors);
    }

    @Override
    public String toCss() {
        return parent.toCss() + " > " + child.toCss();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChildSelector)) {
            return false;
        }
        ChildSelector that = (ChildSelector) o;
        return parent.equals(that.parent) && child.equals(that.child);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, child);
    }

    @Override
    public String toString() {
        return "ChildSelector{" + toCss() + "}";
    }
}

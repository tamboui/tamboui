/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.selector;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;

import java.util.List;
import java.util.Objects;

/// A child combinator selector that matches direct children.
///
///
///
/// Example: {@code Panel > Button { ... }} matches a Button that is
/// a direct child of a Panel (not a grandchild or deeper).
public final class ChildSelector implements Selector {

    private final Selector parent;
    private final Selector child;

    public ChildSelector(Selector parent, Selector child) {
        this.parent = Objects.requireNonNull(parent);
        this.child = Objects.requireNonNull(child);
    }

    public Selector parent() {
        return parent;
    }

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
        if (ancestors.isEmpty()) {
            return false;
        }

        Styleable parentElement = ancestors.get(ancestors.size() - 1);
        List<Styleable> parentAncestors = ancestors.subList(0, ancestors.size() - 1);
        return parent.matches(parentElement, PseudoClassState.NONE, parentAncestors);
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


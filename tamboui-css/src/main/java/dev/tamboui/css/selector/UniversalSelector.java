/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.selector;

import java.util.List;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;

/**
 * A universal selector that matches all elements.
 * <p>
 * Example: {@code * { ... }} matches every element.
 */
public final class UniversalSelector implements Selector {

    /**
     * Singleton instance since the selector is stateless.
     */
    public static final UniversalSelector INSTANCE = new UniversalSelector();

    private UniversalSelector() {
    }

    @Override
    public int specificity() {
        return 0; // (0, 0, 0)
    }

    @Override
    public boolean matches(Styleable element, PseudoClassState state, List<Styleable> ancestors) {
        return true;
    }

    @Override
    public String toCss() {
        return "*";
    }

    @Override
    public String toString() {
        return "UniversalSelector";
    }
}

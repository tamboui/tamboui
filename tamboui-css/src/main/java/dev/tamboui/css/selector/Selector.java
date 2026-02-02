/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.selector;

import java.util.List;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.css.cascade.PseudoClassStateProvider;

/**
 * Represents a CSS selector that matches elements.
 * <p>
 * Selectors are used to determine which CSS rules apply to which elements.
 * The specificity of a selector determines its priority when multiple
 * rules match the same element.
 */
public interface Selector {

    /**
     * Returns the specificity of this selector.
     * <p>
     * Specificity is used for cascade resolution: higher specificity wins.
     * The value is computed as: (id * 100) + (class/pseudo * 10) + type
     *
     * @return the specificity value
     */
    int specificity();

    /**
     * Tests whether this selector matches the given element.
     *
     * @param element   the element to test
     * @param state     the current pseudo-class state (focus, hover, etc.)
     * @param ancestors the ancestor chain from root to parent (not including element)
     * @return true if this selector matches the element
     */
    boolean matches(Styleable element, PseudoClassState state, List<Styleable> ancestors);

    /**
     * Tests whether this selector matches the given element, using a state provider
     * for dynamic pseudo-class resolution.
     * <p>
     * This method is needed for descendant selectors like {@code #parent:focus .child}
     * where the :focus pseudo-class must be evaluated on the ancestor element.
     * <p>
     * The default implementation delegates to {@link #matches(Styleable, PseudoClassState, List)}
     * using the state from the provider for the target element. Combinator selectors
     * (like DescendantSelector) override this to properly evaluate ancestor states.
     *
     * @param element       the element to test
     * @param stateProvider provides pseudo-class state for any element
     * @param ancestors     the ancestor chain from root to parent (not including element)
     * @return true if this selector matches the element
     */
    default boolean matches(Styleable element, PseudoClassStateProvider stateProvider, List<Styleable> ancestors) {
        return matches(element, stateProvider.stateFor(element), ancestors);
    }

    /**
     * Returns a CSS string representation of this selector.
     *
     * @return the selector as CSS text
     */
    String toCss();
}

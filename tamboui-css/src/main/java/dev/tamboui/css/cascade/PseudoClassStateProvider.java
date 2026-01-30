/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.cascade;

import dev.tamboui.css.Styleable;

/**
 * Provides the pseudo-class state for any element during selector matching.
 * <p>
 * This functional interface allows computing dynamic pseudo-class state for
 * elements in the tree, which is needed for descendant selectors like
 * {@code #parent:focus .child} where the :focus state must be checked on the
 * ancestor element.
 */
@FunctionalInterface
public interface PseudoClassStateProvider {

    /**
     * Returns the pseudo-class state for the given element.
     *
     * @param element
     *            the element to get state for
     * @return the pseudo-class state for the element
     */
    PseudoClassState stateFor(Styleable element);
}

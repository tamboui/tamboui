/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.selector;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;

import java.util.List;

/// Represents a CSS selector that matches elements.
///
///
///
/// Selectors are used to determine which CSS rules apply to which elements.
/// The specificity of a selector determines its priority when multiple
/// rules match the same element.
public interface Selector {

    /// Returns the specificity of this selector.
    ///
    ///
    ///
    /// Specificity is used for cascade resolution: higher specificity wins.
    /// The value is computed as: (id * 100) + (class/pseudo * 10) + type
    ///
    /// @return the specificity value
    int specificity();

    /// Tests whether this selector matches the given element.
    ///
    /// @param element   the element to test
    /// @param state     the current pseudo-class state (focus, hover, etc.)
    /// @param ancestors the ancestor chain from root to parent (not including element)
    /// @return true if this selector matches the element
    boolean matches(Styleable element, PseudoClassState state, List<Styleable> ancestors);

    /// Returns a CSS string representation of this selector.
    ///
    /// @return the selector as CSS text
    String toCss();
}


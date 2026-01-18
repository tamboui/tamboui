/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/// Interface for elements that can be styled via CSS.
///
///
///
/// Elements implementing this interface can be targeted by CSS selectors
/// and have CSS rules applied to them during rendering.
public interface Styleable {

    /// Returns the style type name used for type selectors.
    ///
    ///
    ///
    /// For example, a Panel element would return "Panel" to match
    /// the CSS selector {@code Panel { ... }}.
    ///
    /// @return the style type name (never null)
    default String styleType() {
        return getClass().getSimpleName();
    }

    /// Returns the element ID for ID selectors.
    ///
    ///
    ///
    /// For example, an element with ID "sidebar" would be matched
    /// by the CSS selector {@code #sidebar { ... }}.
    ///
    /// @return the element ID, or empty if not set
    Optional<String> cssId();

    /// Returns the CSS classes assigned to this element.
    ///
    ///
    ///
    /// For example, an element with class "primary" would be matched
    /// by the CSS selector {@code .primary { ... }}.
    ///
    /// @return an unmodifiable set of CSS class names (never null, may be empty)
    Set<String> cssClasses();

    /// Returns the parent element in the style hierarchy.
    ///
    ///
    ///
    /// Used for descendant and child combinator selectors like
    /// {@code Panel Button { ... }} or {@code Panel > Button { ... }}.
    ///
    /// @return the parent element, or empty if this is a root element
    Optional<Styleable> cssParent();

    /// Returns the style attributes for attribute selector matching.
    ///
    ///
    ///
    /// Attribute selectors like {@code Panel[title="Test"]} match elements
    /// based on their style attributes. This method returns a map of
    /// attribute names to their values.
    ///
    ///
    ///
    /// Common attributes include:
    ///
    /// - {@code title} - the element's title
    /// - {@code label} - the element's label
    /// - {@code placeholder} - the element's placeholder text
    ///
    ///
    /// @return an unmodifiable map of attribute names to values (never null, may be empty)
    default Map<String, String> styleAttributes() {
        return Collections.emptyMap();
    }

    /// Builds the list of style types for an element, ordered by precedence.
    ///
    ///
    ///
    /// The list contains the element's type and all parent types in the class
    /// hierarchy that implement {@link Styleable}. Types are ordered with lower
    /// precedence first (parent types) and higher precedence last (most specific type).
    ///
    ///
    ///
    /// For example, if {@code MyPanel extends Panel} and both implement Styleable,
    /// calling {@code styleTypes(myPanel)} returns {@code ["Panel", "MyPanel"]}.
    ///
    /// @param element the element to get style types for
    /// @return an ordered list of style type names
    static List<String> styleTypesOf(Styleable element) {
        List<String> types = new ArrayList<>();

        // Walk the superclass hierarchy for inherited types
        Class<?> clazz = element.getClass().getSuperclass();
        while (clazz != null && Styleable.class.isAssignableFrom(clazz)) {
            types.add(0, clazz.getSimpleName());
            clazz = clazz.getSuperclass();
        }

        // Add the element's declared type last (highest precedence)
        types.add(element.styleType());

        return Collections.unmodifiableList(types);
    }
}


/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.model;

import java.util.Objects;

/// Represents a CSS property value as parsed from the stylesheet.
///
///
///
/// The raw value is stored as a string and converted to the appropriate
/// type when applied to elements.
public final class PropertyValue {

    private final String raw;
    private final boolean important;

    public PropertyValue(String raw, boolean important) {
        this.raw = Objects.requireNonNull(raw);
        this.important = important;
    }

    /// Creates a regular (non-important) property value.
    ///
    /// @param raw the raw value string
    /// @return the property value
    public static PropertyValue of(String raw) {
        return new PropertyValue(raw, false);
    }

    /// Creates an important (!important) property value.
    ///
    /// @param raw the raw value string
    /// @return the property value
    public static PropertyValue important(String raw) {
        return new PropertyValue(raw, true);
    }

    public String raw() {
        return raw;
    }

    public boolean important() {
        return important;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PropertyValue)) {
            return false;
        }
        PropertyValue that = (PropertyValue) o;
        return important == that.important && raw.equals(that.raw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw, important);
    }

    @Override
    public String toString() {
        return important ? raw + " !important" : raw;
    }
}


/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.model;

import java.util.Objects;

/**
 * Represents a CSS property value as parsed from the stylesheet.
 * <p>
 * The raw value is stored as a string and converted to the appropriate
 * type when applied to elements.
 */
public final class PropertyValue {

    /**
     * The keyword used when a child wants to explicitly inherit a value from its parent.
     */
    public static final String INHERIT_KEYWORD = "inherit";

    private final String raw;
    private final boolean important;

    /**
     * Creates a property value.
     *
     * @param raw       the raw value string
     * @param important whether the value has !important
     */
    public PropertyValue(String raw, boolean important) {
        this.raw = Objects.requireNonNull(raw);
        this.important = important;
    }

    /**
     * Creates a regular (non-important) property value.
     *
     * @param raw the raw value string
     * @return the property value
     */
    public static PropertyValue of(String raw) {
        return new PropertyValue(raw, false);
    }

    /**
     * Creates an important (!important) property value.
     *
     * @param raw the raw value string
     * @return the property value
     */
    public static PropertyValue important(String raw) {
        return new PropertyValue(raw, true);
    }

    /**
     * Returns the raw value string.
     *
     * @return the raw value
     */
    public String raw() {
        return raw;
    }

    /**
     * Returns whether this value has the {@code !important} flag.
     *
     * @return {@code true} if important
     */
    public boolean important() {
        return important;
    }

    /**
     * Returns true if this value is the special "inherit" keyword,
     * indicating the child wants to explicitly inherit from parent.
     *
     * @return true if the raw value is "inherit"
     */
    public boolean isInherit() {
        return INHERIT_KEYWORD.equalsIgnoreCase(raw);
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
        return important == that.important
                && raw.equals(that.raw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw, important);
    }

    @Override
    public String toString() {
        if (important) {
            return raw + " !important";
        }
        return raw;
    }
}

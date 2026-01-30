/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Objects;
import java.util.Optional;

/**
 * A typed definition for a style property, bundling the property name with its
 * converter, inheritance behavior, and optional default value.
 * <p>
 * PropertyDefinitions are typically defined as constants in property registry
 * classes:
 * 
 * <pre>{@code
 * public static final PropertyDefinition<Color> COLOR = PropertyDefinition
 *         .builder("color", ColorConverter.INSTANCE).inheritable().build();
 *
 * public static final PropertyDefinition<Color> BACKGROUND = PropertyDefinition.of("background",
 *         ColorConverter.INSTANCE); // Non-inheritable
 * }</pre>
 * <p>
 * They can then be used with {@link StylePropertyResolver} to retrieve typed
 * values:
 * 
 * <pre>{@code
 * Color color = resolver.get(BORDER_COLOR).orElse(Color.WHITE);
 * }</pre>
 *
 * @param <T>
 *            the type of value this property definition represents
 */
public final class PropertyDefinition<T> {

    private final String name;
    private final PropertyConverter<T> converter;
    private final boolean inheritable;
    private final T defaultValue;

    private PropertyDefinition(String name, PropertyConverter<T> converter, boolean inheritable,
            T defaultValue) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.converter = Objects.requireNonNull(converter, "converter must not be null");
        this.inheritable = inheritable;
        this.defaultValue = defaultValue;
    }

    /**
     * Creates a new property definition with the given name and converter. The
     * property will be non-inheritable with no default value.
     *
     * @param name
     *            the property name (e.g., "background", "padding")
     * @param converter
     *            the converter for parsing string values
     * @param <T>
     *            the type of value this property represents
     * @return the property definition
     */
    public static <T> PropertyDefinition<T> of(String name, PropertyConverter<T> converter) {
        return new PropertyDefinition<>(name, converter, false, null);
    }

    /**
     * Creates a new builder for a property definition.
     *
     * @param name
     *            the property name (e.g., "color", "border-type")
     * @param converter
     *            the converter for parsing string values
     * @param <T>
     *            the type of value this property represents
     * @return a builder for the property definition
     */
    public static <T> Builder<T> builder(String name, PropertyConverter<T> converter) {
        return new Builder<>(name, converter);
    }

    /**
     * Returns the property name.
     *
     * @return the property name
     */
    public String name() {
        return name;
    }

    /**
     * Returns whether this property is inherited from parent elements.
     * <p>
     * Per CSS semantics, inheritable properties (like color, text-style) are passed
     * from parent to child elements if not explicitly set.
     *
     * @return true if this property inherits from parent
     */
    public boolean isInheritable() {
        return inheritable;
    }

    /**
     * Returns the default value for this property, or null if none.
     *
     * @return the default value, or null
     */
    public T defaultValue() {
        return defaultValue;
    }

    /**
     * Converts a string value to the target type using this definition's converter.
     *
     * @param value
     *            the string value to convert
     * @return the converted value, or empty if conversion fails
     */
    public Optional<T> convert(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return converter.convert(value);
    }

    /**
     * Returns the converter for this property.
     *
     * @return the property converter
     */
    public PropertyConverter<T> converter() {
        return converter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PropertyDefinition<?> that = (PropertyDefinition<?>) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "PropertyDefinition[" + name + (inheritable ? ", inheritable" : "") + "]";
    }

    /**
     * Builder for PropertyDefinition.
     *
     * @param <T>
     *            the type of value the property definition represents
     */
    public static final class Builder<T> {
        private final String name;
        private final PropertyConverter<T> converter;
        private boolean inheritable;
        private T defaultValue;

        private Builder(String name, PropertyConverter<T> converter) {
            this.name = name;
            this.converter = converter;
        }

        /**
         * Marks this property as inheritable. Inheritable properties are passed from
         * parent to child elements if not explicitly set on the child.
         *
         * @return this builder
         */
        public Builder<T> inheritable() {
            this.inheritable = true;
            return this;
        }

        /**
         * Sets the default value for this property.
         *
         * @param defaultValue
         *            the default value
         * @return this builder
         */
        public Builder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        /**
         * Builds the property definition.
         *
         * @return the property definition
         */
        public PropertyDefinition<T> build() {
            return new PropertyDefinition<>(name, converter, inheritable, defaultValue);
        }
    }
}

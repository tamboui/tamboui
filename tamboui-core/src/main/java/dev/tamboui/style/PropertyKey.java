/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Objects;
import java.util.Optional;

/**
 * A typed key for a style property, bundling the property name with its converter.
 * <p>
 * PropertyKeys are typically defined as constants in widgets or style utility classes:
 * <pre>{@code
 * public static final PropertyKey<Color> BORDER_COLOR =
 *     PropertyKey.of("border-color", ColorConverter.INSTANCE);
 * }</pre>
 * <p>
 * They can then be used with {@link StylePropertyResolver} to retrieve typed values:
 * <pre>{@code
 * Color color = resolver.get(BORDER_COLOR).orElse(Color.WHITE);
 * }</pre>
 *
 * @param <T> the type of value this property key represents
 */
public final class PropertyKey<T> {

    private final String name;
    private final PropertyConverter<T> converter;

    private PropertyKey(String name, PropertyConverter<T> converter) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.converter = Objects.requireNonNull(converter, "converter must not be null");
    }

    /**
     * Creates a new property key with the given name and converter.
     *
     * @param name      the property name (e.g., "border-color", "text-overflow")
     * @param converter the converter for parsing string values
     * @param <T>       the type of value this property represents
     * @return the property key
     */
    public static <T> PropertyKey<T> of(String name, PropertyConverter<T> converter) {
        return new PropertyKey<>(name, converter);
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
     * Converts a string value to the target type using this key's converter.
     *
     * @param value the string value to convert
     * @return the converted value, or empty if conversion fails
     */
    public Optional<T> convert(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return converter.convert(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PropertyKey<?> that = (PropertyKey<?>) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "PropertyKey[" + name + "]";
    }
}

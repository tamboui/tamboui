/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Optional;

/**
 * A functional interface for converting string values to typed property values.
 * <p>
 * Converters are used by {@link PropertyKey} to parse string representations
 * into strongly-typed values.
 *
 * @param <T> the type of value this converter produces
 */
@FunctionalInterface
public interface PropertyConverter<T> {

    /**
     * Converts a string value to the target type.
     *
     * @param value the string value to convert
     * @return the converted value, or empty if conversion fails
     */
    Optional<T> convert(String value);
}

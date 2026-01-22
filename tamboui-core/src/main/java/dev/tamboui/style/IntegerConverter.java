/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Optional;

/**
 * Converts string values to Integer objects.
 * <p>
 * Handles standard integer formats including negative values.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Standalone usage
 * Optional<Integer> value = IntegerConverter.INSTANCE.convert("42");
 *
 * // With PropertyKey
 * PropertyKey<Integer> COUNT = PropertyKey.of("count", IntegerConverter.INSTANCE);
 * }</pre>
 */
public final class IntegerConverter implements PropertyConverter<Integer> {

    /**
     * Singleton instance of the integer converter.
     */
    public static final IntegerConverter INSTANCE = new IntegerConverter();

    private IntegerConverter() {
    }

    @Override
    public Optional<Integer> convert(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Optional;

/**
 * Converts string values to Double objects.
 * <p>
 * Handles standard decimal number formats including negative values
 * and scientific notation.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Standalone usage
 * Optional<Double> value = DoubleConverter.INSTANCE.convert("1.5");
 *
 * // With PropertyKey
 * PropertyKey<Double> SPEED = PropertyKey.of("speed", DoubleConverter.INSTANCE);
 * }</pre>
 */
public final class DoubleConverter implements PropertyConverter<Double> {

    /**
     * Singleton instance of the double converter.
     */
    public static final DoubleConverter INSTANCE = new DoubleConverter();

    private DoubleConverter() {
    }

    @Override
    public Optional<Double> convert(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Double.parseDouble(value.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}

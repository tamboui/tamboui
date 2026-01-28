/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import java.util.Map;
import java.util.Optional;

/**
 * Converts CSS integer values.
 * <p>
 * Supports plain integer values like "0", "5", "10".
 */
public final class IntegerConverter implements PropertyConverter<Integer> {

    /** Singleton instance. */
    public static final IntegerConverter INSTANCE = new IntegerConverter();

    private IntegerConverter() {
    }

    @Override
    public Optional<Integer> convert(String value, Map<String, String> variables) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String resolved = PropertyConverter.resolveVariables(value.trim(), variables);

        try {
            return Optional.of(Integer.parseInt(resolved));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}

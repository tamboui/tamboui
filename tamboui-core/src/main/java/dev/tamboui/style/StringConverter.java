/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Optional;

/**
 * Identity converter for String properties. Returns the input value unchanged
 * (after trimming).
 */
public final class StringConverter implements PropertyConverter<String> {

    /**
     * Singleton instance.
     */
    public static final StringConverter INSTANCE = new StringConverter();

    private StringConverter() {
    }

    @Override
    public Optional<String> convert(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
    }
}

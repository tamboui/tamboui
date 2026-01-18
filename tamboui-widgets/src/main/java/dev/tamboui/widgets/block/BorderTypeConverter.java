/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.block;

import dev.tamboui.style.PropertyConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/// Converts string values to {@link BorderType} enum values.
///
///
///
/// Supported values (case-insensitive, hyphens or underscores):
///
/// - {@code plain} - standard box drawing characters
/// - {@code rounded} - rounded corners
/// - {@code double} - double-line borders
/// - {@code thick} - thick/bold borders
/// - {@code light-double-dashed} - light double-dashed borders
/// - {@code heavy-double-dashed} - heavy double-dashed borders
/// - {@code light-triple-dashed} - light triple-dashed borders
/// - {@code heavy-triple-dashed} - heavy triple-dashed borders
/// - {@code light-quadruple-dashed} - light quadruple-dashed borders
/// - {@code heavy-quadruple-dashed} - heavy quadruple-dashed borders
/// - {@code quadrant-inside} - quadrant block inside style
/// - {@code quadrant-outside} - quadrant block outside style
public final class BorderTypeConverter implements PropertyConverter<BorderType> {

    /// Singleton instance of the border type converter.
    public static final BorderTypeConverter INSTANCE = new BorderTypeConverter();

    private static final Map<String, BorderType> VALUES = new HashMap<>();

    static {
        for (BorderType type : BorderType.values()) {
            // Add lowercase with hyphens (e.g., "light-double-dashed")
            String hyphenated = type.name().toLowerCase().replace('_', '-');
            VALUES.put(hyphenated, type);
            // Also support underscores (e.g., "light_double_dashed")
            VALUES.put(type.name().toLowerCase(), type);
        }
    }

    private BorderTypeConverter() {
    }

    @Override
    public Optional<BorderType> convert(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(VALUES.get(value.trim().toLowerCase()));
    }
}


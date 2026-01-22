/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.block;

import dev.tamboui.style.PropertyConverter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Converts string values to {@link BorderType} enum values.
 * <p>
 * Supported values (case-insensitive, hyphens or underscores):
 * <ul>
 *   <li>{@code plain} - standard box drawing characters</li>
 *   <li>{@code rounded} - rounded corners</li>
 *   <li>{@code double} - double-line borders</li>
 *   <li>{@code thick} - thick/bold borders</li>
 *   <li>{@code light-double-dashed} - light double-dashed borders</li>
 *   <li>{@code heavy-double-dashed} - heavy double-dashed borders</li>
 *   <li>{@code light-triple-dashed} - light triple-dashed borders</li>
 *   <li>{@code heavy-triple-dashed} - heavy triple-dashed borders</li>
 *   <li>{@code light-quadruple-dashed} - light quadruple-dashed borders</li>
 *   <li>{@code heavy-quadruple-dashed} - heavy quadruple-dashed borders</li>
 *   <li>{@code quadrant-inside} - quadrant block inside style</li>
 *   <li>{@code quadrant-outside} - quadrant block outside style</li>
 * </ul>
 */
public final class BorderTypeConverter implements PropertyConverter<BorderType> {

    /**
     * Singleton instance of the border type converter.
     */
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

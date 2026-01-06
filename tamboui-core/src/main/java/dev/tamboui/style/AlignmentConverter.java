/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import dev.tamboui.layout.Alignment;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Converts string values to {@link Alignment} enum values.
 * <p>
 * Supported values (case-insensitive):
 * <ul>
 *   <li>{@code left} - left alignment</li>
 *   <li>{@code center} - center alignment</li>
 *   <li>{@code right} - right alignment</li>
 * </ul>
 */
public final class AlignmentConverter implements PropertyConverter<Alignment> {

    /**
     * Singleton instance of the alignment converter.
     */
    public static final AlignmentConverter INSTANCE = new AlignmentConverter();

    private static final Map<String, Alignment> VALUES = new HashMap<>();

    static {
        VALUES.put("left", Alignment.LEFT);
        VALUES.put("center", Alignment.CENTER);
        VALUES.put("right", Alignment.RIGHT);
    }

    private AlignmentConverter() {
    }

    @Override
    public Optional<Alignment> convert(String value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(VALUES.get(value.trim().toLowerCase()));
    }
}

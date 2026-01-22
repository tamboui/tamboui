/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import dev.tamboui.layout.Alignment;

import java.util.Map;
import java.util.Optional;

/**
 * Converts CSS text-align values to Alignment enum.
 * <p>
 * Supports the following values:
 * <ul>
 *   <li>{@code "left"} - left alignment</li>
 *   <li>{@code "center"} - center alignment</li>
 *   <li>{@code "right"} - right alignment</li>
 * </ul>
 */
public final class AlignmentConverter implements PropertyConverter<Alignment> {

    @Override
    public Optional<Alignment> convert(String value, Map<String, String> variables) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String resolved = PropertyConverter.resolveVariables(value.trim(), variables).toLowerCase();

        switch (resolved) {
            case "left":
                return Optional.of(Alignment.LEFT);
            case "center":
                return Optional.of(Alignment.CENTER);
            case "right":
                return Optional.of(Alignment.RIGHT);
            default:
                return Optional.empty();
        }
    }
}

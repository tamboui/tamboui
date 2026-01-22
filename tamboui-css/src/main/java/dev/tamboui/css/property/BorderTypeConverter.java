/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import dev.tamboui.widgets.block.BorderType;

import java.util.Map;
import java.util.Optional;

/**
 * Converts CSS border-type values to BorderType enum.
 * <p>
 * CSS values are derived from enum names by converting to lowercase and
 * replacing underscores with hyphens. For example:
 * <ul>
 *   <li>{@code NONE} becomes {@code "none"}</li>
 *   <li>{@code PLAIN} becomes {@code "plain"}</li>
 *   <li>{@code LIGHT_DOUBLE_DASHED} becomes {@code "light-double-dashed"}</li>
 * </ul>
 */
public final class BorderTypeConverter implements PropertyConverter<BorderType> {

    @Override
    public Optional<BorderType> convert(String value, Map<String, String> variables) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String resolved = PropertyConverter.resolveVariables(value.trim(), variables).toLowerCase();

        for (BorderType type : BorderType.values()) {
            String cssValue = type.name().toLowerCase().replace('_', '-');
            if (cssValue.equals(resolved)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}

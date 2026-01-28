/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import dev.tamboui.layout.Direction;

import java.util.Map;
import java.util.Optional;

/**
 * Converts CSS direction values to Direction enum.
 * <p>
 * Supports the following values:
 * <ul>
 *   <li>{@code "horizontal"} or {@code "row"} - horizontal layout</li>
 *   <li>{@code "vertical"} or {@code "column"} - vertical layout</li>
 * </ul>
 */
public final class DirectionConverter implements PropertyConverter<Direction> {

    /** Singleton instance. */
    public static final DirectionConverter INSTANCE = new DirectionConverter();

    private DirectionConverter() {
    }

    @Override
    public Optional<Direction> convert(String value, Map<String, String> variables) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String resolved = PropertyConverter.resolveVariables(value.trim(), variables).toLowerCase();

        switch (resolved) {
            case "horizontal":
            case "row":
                return Optional.of(Direction.HORIZONTAL);
            case "vertical":
            case "column":
                return Optional.of(Direction.VERTICAL);
            default:
                return Optional.empty();
        }
    }
}

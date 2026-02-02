/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

import java.util.Optional;

import dev.tamboui.style.PropertyConverter;

/**
 * Converts string values to {@link Direction} enum values.
 * <p>
 * Supported values (case-insensitive):
 * <ul>
 *   <li>{@code horizontal} or {@code row} - horizontal layout</li>
 *   <li>{@code vertical} or {@code column} - vertical layout</li>
 * </ul>
 */
public final class DirectionConverter implements PropertyConverter<Direction> {

    /**
     * Singleton instance of the direction converter.
     */
    public static final DirectionConverter INSTANCE = new DirectionConverter();

    private DirectionConverter() {
    }

    @Override
    public Optional<Direction> convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalized = value.trim().toLowerCase();

        switch (normalized) {
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

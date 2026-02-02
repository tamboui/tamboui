/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

import java.util.Optional;

import dev.tamboui.style.PropertyConverter;

/**
 * Converts string values to {@link Flex} enum values.
 * <p>
 * Supported values (case-insensitive):
 * <ul>
 *   <li>{@code start} - items aligned at start</li>
 *   <li>{@code center} - items centered</li>
 *   <li>{@code end} - items aligned at end</li>
 *   <li>{@code space-between} - items distributed with space between</li>
 *   <li>{@code space-around} - items distributed with space around</li>
 *   <li>{@code space-evenly} - items distributed with equal space</li>
 * </ul>
 */
public final class FlexConverter implements PropertyConverter<Flex> {

    /**
     * Singleton instance of the flex converter.
     */
    public static final FlexConverter INSTANCE = new FlexConverter();

    private FlexConverter() {
    }

    @Override
    public Optional<Flex> convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalized = value.trim().toLowerCase();

        switch (normalized) {
            case "start":
                return Optional.of(Flex.START);
            case "center":
                return Optional.of(Flex.CENTER);
            case "end":
                return Optional.of(Flex.END);
            case "space-between":
                return Optional.of(Flex.SPACE_BETWEEN);
            case "space-around":
                return Optional.of(Flex.SPACE_AROUND);
            case "space-evenly":
                return Optional.of(Flex.SPACE_EVENLY);
            default:
                return Optional.empty();
        }
    }
}

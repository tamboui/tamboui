/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import dev.tamboui.layout.Flex;

import java.util.Map;
import java.util.Optional;

/**
 * Converts CSS flex values to Flex enum.
 * <p>
 * Supports the following values:
 * <ul>
 *   <li>{@code "start"} - items aligned at start</li>
 *   <li>{@code "center"} - items centered</li>
 *   <li>{@code "end"} - items aligned at end</li>
 *   <li>{@code "space-between"} - items distributed with space between</li>
 *   <li>{@code "space-around"} - items distributed with space around</li>
 *   <li>{@code "space-evenly"} - items distributed with equal space</li>
 * </ul>
 */
public final class FlexConverter implements PropertyConverter<Flex> {

    /** Singleton instance. */
    public static final FlexConverter INSTANCE = new FlexConverter();

    private FlexConverter() {
    }

    @Override
    public Optional<Flex> convert(String value, Map<String, String> variables) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String resolved = PropertyConverter.resolveVariables(value.trim(), variables).toLowerCase();

        switch (resolved) {
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

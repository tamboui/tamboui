/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import dev.tamboui.layout.Padding;

import java.util.Map;
import java.util.Optional;

/**
 * Converts CSS padding/margin values to Padding objects.
 * <p>
 * Supports the following formats:
 * <ul>
 *   <li>{@code "1"} - uniform padding on all sides</li>
 *   <li>{@code "1 2"} - vertical (top/bottom) and horizontal (left/right)</li>
 *   <li>{@code "1 2 3 4"} - top, right, bottom, left (CSS order)</li>
 * </ul>
 */
public final class SpacingConverter implements PropertyConverter<Padding> {

    /** Singleton instance. */
    public static final SpacingConverter INSTANCE = new SpacingConverter();

    private SpacingConverter() {
    }

    @Override
    public Optional<Padding> convert(String value, Map<String, String> variables) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String resolved = PropertyConverter.resolveVariables(value.trim(), variables);
        String[] parts = resolved.split("\\s+");

        try {
            switch (parts.length) {
                case 1: {
                    int all = Integer.parseInt(parts[0]);
                    return Optional.of(Padding.uniform(all));
                }
                case 2: {
                    int vertical = Integer.parseInt(parts[0]);
                    int horizontal = Integer.parseInt(parts[1]);
                    return Optional.of(Padding.symmetric(vertical, horizontal));
                }
                case 4: {
                    int top = Integer.parseInt(parts[0]);
                    int right = Integer.parseInt(parts[1]);
                    int bottom = Integer.parseInt(parts[2]);
                    int left = Integer.parseInt(parts[3]);
                    return Optional.of(new Padding(top, right, bottom, left));
                }
                default:
                    return Optional.empty();
            }
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}

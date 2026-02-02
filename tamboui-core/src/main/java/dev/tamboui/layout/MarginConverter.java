/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

import java.util.Optional;

import dev.tamboui.style.PropertyConverter;

/**
 * Converts string values to {@link Margin} objects.
 * <p>
 * Supports the following formats:
 * <ul>
 *   <li>{@code "1"} - uniform margin on all sides</li>
 *   <li>{@code "1 2"} - vertical (top/bottom) and horizontal (left/right)</li>
 *   <li>{@code "1 2 3 4"} - top, right, bottom, left (CSS order)</li>
 * </ul>
 */
public final class MarginConverter implements PropertyConverter<Margin> {

    /**
     * Singleton instance of the margin converter.
     */
    public static final MarginConverter INSTANCE = new MarginConverter();

    private MarginConverter() {
    }

    @Override
    public Optional<Margin> convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String[] parts = value.trim().split("\\s+");

        try {
            switch (parts.length) {
                case 1: {
                    int all = Integer.parseInt(parts[0]);
                    return Optional.of(Margin.uniform(all));
                }
                case 2: {
                    int vertical = Integer.parseInt(parts[0]);
                    int horizontal = Integer.parseInt(parts[1]);
                    return Optional.of(Margin.symmetric(vertical, horizontal));
                }
                case 4: {
                    int top = Integer.parseInt(parts[0]);
                    int right = Integer.parseInt(parts[1]);
                    int bottom = Integer.parseInt(parts[2]);
                    int left = Integer.parseInt(parts[3]);
                    return Optional.of(new Margin(top, right, bottom, left));
                }
                default:
                    return Optional.empty();
            }
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}

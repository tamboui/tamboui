/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.style.PropertyConverter;

import java.util.Optional;

/**
 * Converts CSS string values to {@link Gutter} objects.
 * <p>
 * Supported formats:
 * <ul>
 *   <li>{@code "2"} — uniform gutter of 2</li>
 *   <li>{@code "1 2"} — horizontal 1, vertical 2</li>
 * </ul>
 */
public final class GutterConverter implements PropertyConverter<Gutter> {

    /**
     * Singleton instance.
     */
    public static final GutterConverter INSTANCE = new GutterConverter();

    private GutterConverter() {
    }

    @Override
    public Optional<Gutter> convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String[] parts = value.trim().split("\\s+");
        try {
            if (parts.length == 1) {
                int v = Integer.parseInt(parts[0]);
                if (v < 0) {
                    return Optional.empty();
                }
                return Optional.of(Gutter.uniform(v));
            } else if (parts.length == 2) {
                int h = Integer.parseInt(parts[0]);
                int vert = Integer.parseInt(parts[1]);
                if (h < 0 || vert < 0) {
                    return Optional.empty();
                }
                return Optional.of(Gutter.of(h, vert));
            }
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        return Optional.empty();
    }
}

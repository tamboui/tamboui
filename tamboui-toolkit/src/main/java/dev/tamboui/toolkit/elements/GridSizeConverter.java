/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.style.PropertyConverter;

import java.util.Optional;

/**
 * Converts CSS string values to {@link GridSize} objects.
 * <p>
 * Supported formats:
 * <ul>
 *   <li>{@code "3"} — 3 columns, auto rows</li>
 *   <li>{@code "3 4"} — 3 columns, 4 rows</li>
 * </ul>
 */
public final class GridSizeConverter implements PropertyConverter<GridSize> {

    /**
     * Singleton instance.
     */
    public static final GridSizeConverter INSTANCE = new GridSizeConverter();

    private GridSizeConverter() {
    }

    @Override
    public Optional<GridSize> convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String[] parts = value.trim().split("\\s+");
        try {
            if (parts.length == 1) {
                int columns = Integer.parseInt(parts[0]);
                if (columns < 1) {
                    return Optional.empty();
                }
                return Optional.of(GridSize.columns(columns));
            } else if (parts.length == 2) {
                int columns = Integer.parseInt(parts[0]);
                int rows = Integer.parseInt(parts[1]);
                if (columns < 1 || rows < 0) {
                    return Optional.empty();
                }
                return Optional.of(GridSize.of(columns, rows));
            }
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        return Optional.empty();
    }
}

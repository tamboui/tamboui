/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.style.PropertyConverter;
import dev.tamboui.layout.columns.ColumnOrder;

import java.util.Optional;

/**
 * Converts string values to {@link ColumnOrder} enum values.
 * <p>
 * Supported values (case-insensitive):
 * <ul>
 *   <li>{@code row-first} — items fill left-to-right, then top-to-bottom</li>
 *   <li>{@code column-first} — items fill top-to-bottom, then left-to-right</li>
 * </ul>
 */
public final class ColumnOrderConverter implements PropertyConverter<ColumnOrder> {

    /**
     * Singleton instance of the column order converter.
     */
    public static final ColumnOrderConverter INSTANCE = new ColumnOrderConverter();

    private ColumnOrderConverter() {
    }

    @Override
    public Optional<ColumnOrder> convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String normalized = value.trim().toLowerCase();

        switch (normalized) {
            case "row-first":
                return Optional.of(ColumnOrder.ROW_FIRST);
            case "column-first":
                return Optional.of(ColumnOrder.COLUMN_FIRST);
            default:
                return Optional.empty();
        }
    }
}
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.columns;

/**
 * Controls the ordering of children within a {@link Columns} layout.
 *
 * <ul>
 *   <li>{@link #ROW_FIRST} — items fill left-to-right, then top-to-bottom (like reading text)</li>
 *   <li>{@link #COLUMN_FIRST} — items fill top-to-bottom, then left-to-right (like newspaper columns)</li>
 * </ul>
 */
public enum ColumnOrder {

    /**
     * Items fill left-to-right, then top-to-bottom.
     */
    ROW_FIRST,

    /**
     * Items fill top-to-bottom, then left-to-right.
     */
    COLUMN_FIRST;

    /**
     * Resolves the child index for a given grid position based on this ordering mode.
     *
     * @param row  the row index (0-based)
     * @param col  the column index (0-based)
     * @param rows the total number of rows
     * @param cols the total number of columns
     * @return the child index
     */
    public int resolveIndex(int row, int col, int rows, int cols) {
        if (this == COLUMN_FIRST) {
            return col * rows + row;
        }
        return row * cols + col;
    }
}

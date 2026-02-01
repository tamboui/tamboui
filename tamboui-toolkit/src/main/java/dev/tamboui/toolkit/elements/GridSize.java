/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

/**
 * Immutable value object holding grid dimensions (columns and rows).
 * <p>
 * A rows value of 0 means the row count is auto-derived from the number of children.
 */
public final class GridSize {

    private final int columns;
    private final int rows;

    private GridSize(int columns, int rows) {
        if (columns < 1) {
            throw new IllegalArgumentException("columns must be at least 1: " + columns);
        }
        if (rows < 0) {
            throw new IllegalArgumentException("rows cannot be negative: " + rows);
        }
        this.columns = columns;
        this.rows = rows;
    }

    /**
     * Creates a grid size with explicit columns and rows.
     *
     * @param columns the number of columns (must be &gt;= 1)
     * @param rows    the number of rows (must be &gt;= 0; 0 means auto)
     * @return a new grid size
     */
    public static GridSize of(int columns, int rows) {
        return new GridSize(columns, rows);
    }

    /**
     * Creates a grid size with explicit columns and auto rows.
     *
     * @param columns the number of columns (must be &gt;= 1)
     * @return a new grid size with auto row count
     */
    public static GridSize columns(int columns) {
        return new GridSize(columns, 0);
    }

    /**
     * Returns the number of columns.
     *
     * @return the column count
     */
    public int columns() {
        return columns;
    }

    /**
     * Returns the number of rows (0 means auto-derive from children count).
     *
     * @return the row count, or 0 for auto
     */
    public int rows() {
        return rows;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GridSize)) {
            return false;
        }
        GridSize gridSize = (GridSize) o;
        return columns == gridSize.columns && rows == gridSize.rows;
    }

    @Override
    public int hashCode() {
        int result = columns;
        result = 31 * result + rows;
        return result;
    }

    @Override
    public String toString() {
        return "GridSize[columns=" + columns + ", rows=" + rows + "]";
    }
}

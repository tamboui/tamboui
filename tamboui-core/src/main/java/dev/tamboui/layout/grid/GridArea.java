/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.grid;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import dev.tamboui.layout.LayoutException;

/**
 * Parses and validates CSS grid-template-areas style layout definitions.
 * <p>
 * Area names must be alphanumeric identifiers starting with a letter. Use "."
 * for empty cells. Named areas must form contiguous rectangles.
 * 
 * <pre>{@code
 * GridArea areas = GridArea.parse("A A B", "A A C", "D D D");
 * // A spans columns 0-1, rows 0-1
 * // B spans column 2, row 0
 * // C spans column 2, row 1
 * // D spans columns 0-2, row 2
 * }</pre>
 */
public final class GridArea {

    /**
     * Represents an empty cell in the area template.
     */
    public static final String EMPTY_CELL = ".";

    private final int rows;
    private final int columns;
    private final Map<String, AreaBounds> areas;
    private final String[][] grid;

    private GridArea(int rows, int columns, Map<String, AreaBounds> areas, String[][] grid) {
        this.rows = rows;
        this.columns = columns;
        this.areas = Collections.unmodifiableMap(areas);
        this.grid = grid;
    }

    /**
     * Parses area template strings into a GridArea.
     * <p>
     * Each string represents a row; tokens are space-separated area names. All rows
     * must have the same number of tokens (columns). Named areas must form
     * contiguous rectangles. Use "." for empty cells.
     *
     * @param rowTemplates
     *            the row templates (e.g., "A A B", "A A C")
     * @return the parsed GridArea
     * @throws LayoutException
     *             if the template is invalid
     */
    public static GridArea parse(String... rowTemplates) {
        if (rowTemplates == null || rowTemplates.length == 0) {
            throw new LayoutException("Grid area template cannot be empty");
        }

        // Parse into 2D grid
        String[][] grid = new String[rowTemplates.length][];
        int columnCount = -1;

        for (int row = 0; row < rowTemplates.length; row++) {
            String template = rowTemplates[row];
            if (template == null || template.trim().isEmpty()) {
                throw new LayoutException("Row " + row + " template cannot be empty");
            }
            String[] tokens = template.trim().split("\\s+");

            if (columnCount == -1) {
                columnCount = tokens.length;
            } else if (tokens.length != columnCount) {
                throw new LayoutException(String.format(
                        "Row %d has %d columns but expected %d (all rows must have equal columns)",
                        row, tokens.length, columnCount));
            }

            grid[row] = tokens;
        }

        // Validate area names and collect bounds
        Map<String, AreaBounds> areas = collectAndValidateAreas(grid, rowTemplates.length,
                columnCount);

        return new GridArea(rowTemplates.length, columnCount, areas, grid);
    }

    private static Map<String, AreaBounds> collectAndValidateAreas(String[][] grid, int rows,
            int cols) {

        Map<String, AreaBounds> areas = new LinkedHashMap<>();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                String name = grid[row][col];

                if (EMPTY_CELL.equals(name)) {
                    continue;
                }

                // Validate name format (alphanumeric, starting with letter)
                if (!isValidAreaName(name)) {
                    throw new LayoutException(String.format(
                            "Invalid area name '%s' at row %d, column %d. "
                                    + "Names must be alphanumeric and start with a letter.",
                            name, row, col));
                }

                if (!areas.containsKey(name)) {
                    // First occurrence - find the full rectangular extent
                    AreaBounds bounds = findAreaBounds(grid, name, row, col, rows, cols);
                    areas.put(name, bounds);
                }
            }
        }

        return areas;
    }

    private static AreaBounds findAreaBounds(String[][] grid, String name, int startRow,
            int startCol, int totalRows, int totalCols) {

        // Find the extent of this area
        int endRow = startRow;
        int endCol = startCol;

        // Expand right
        while (endCol + 1 < totalCols && name.equals(grid[startRow][endCol + 1])) {
            endCol++;
        }

        // Expand down
        while (endRow + 1 < totalRows) {
            boolean fullRow = true;
            for (int c = startCol; c <= endCol; c++) {
                if (!name.equals(grid[endRow + 1][c])) {
                    fullRow = false;
                    break;
                }
            }
            if (fullRow) {
                endRow++;
            } else {
                break;
            }
        }

        // Validate: all cells in the bounds must have this name
        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                if (!name.equals(grid[r][c])) {
                    throw new LayoutException(String.format(
                            "Area '%s' is not a contiguous rectangle. "
                                    + "Cell at row %d, column %d has '%s' instead.",
                            name, r, c, grid[r][c]));
                }
            }
        }

        // Validate: no other cells outside bounds have this name
        for (int r = 0; r < totalRows; r++) {
            for (int c = 0; c < totalCols; c++) {
                if (name.equals(grid[r][c])) {
                    boolean inBounds = r >= startRow && r <= endRow && c >= startCol && c <= endCol;
                    if (!inBounds) {
                        // Check if this cell is adjacent to the bounds (non-rectangular)
                        // or completely separated (disconnected)
                        boolean isAdjacent = isAdjacentToBounds(r, c, startRow, endRow, startCol,
                                endCol);
                        if (isAdjacent) {
                            throw new LayoutException(String.format(
                                    "Area '%s' does not form a rectangle. "
                                            + "Cell at row %d, column %d extends beyond the rectangular bounds.",
                                    name, r, c));
                        } else {
                            throw new LayoutException(String.format("Area '%s' is not contiguous. "
                                    + "Found at row %d, column %d which is disconnected from the main area.",
                                    name, r, c));
                        }
                    }
                }
            }
        }

        return new AreaBounds(startRow, startCol, endRow - startRow + 1, endCol - startCol + 1);
    }

    /**
     * Checks if a cell at (r, c) is adjacent to the rectangular bounds. Adjacent
     * means the cell is directly next to the bounds (touching an edge).
     */
    private static boolean isAdjacentToBounds(int r, int c, int startRow, int endRow, int startCol,
            int endCol) {
        // Cell is adjacent if it's within 1 row/column of the bounds
        boolean rowAdjacent = r >= startRow - 1 && r <= endRow + 1;
        boolean colAdjacent = c >= startCol - 1 && c <= endCol + 1;

        // Must be adjacent in both dimensions, but at least one must be touching
        boolean touchesRow = r >= startRow && r <= endRow;
        boolean touchesCol = c >= startCol && c <= endCol;

        // Adjacent if: within range on both axes AND touching on at least one axis
        return rowAdjacent && colAdjacent && (touchesRow || touchesCol);
    }

    private static boolean isValidAreaName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        char first = name.charAt(0);
        if (!Character.isLetter(first)) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the number of rows in the grid template.
     *
     * @return the row count
     */
    public int rows() {
        return rows;
    }

    /**
     * Returns the number of columns in the grid template.
     *
     * @return the column count
     */
    public int columns() {
        return columns;
    }

    /**
     * Returns the bounds for a named area.
     *
     * @param areaName
     *            the area name
     * @return the bounds, or null if not found
     */
    public AreaBounds boundsFor(String areaName) {
        return areas.get(areaName);
    }

    /**
     * Returns all defined area names in declaration order.
     *
     * @return the set of area names
     */
    public Set<String> areaNames() {
        return areas.keySet();
    }

    /**
     * Returns the number of named areas.
     *
     * @return the area count
     */
    public int areaCount() {
        return areas.size();
    }

    /**
     * Returns the row templates that were used to create this GridArea.
     * <p>
     * Useful for passing to Grid.Builder.gridAreas().
     *
     * @return array of row template strings
     */
    public String[] toTemplates() {
        String[] templates = new String[rows];
        for (int r = 0; r < rows; r++) {
            templates[r] = String.join(" ", grid[r]);
        }
        return templates;
    }

    /**
     * Bounds of a named area within the grid.
     */
    public static final class AreaBounds {
        private final int row;
        private final int column;
        private final int rowSpan;
        private final int columnSpan;

        /**
         * Creates area bounds.
         *
         * @param row
         *            the start row (0-based)
         * @param column
         *            the start column (0-based)
         * @param rowSpan
         *            the number of rows spanned
         * @param columnSpan
         *            the number of columns spanned
         */
        public AreaBounds(int row, int column, int rowSpan, int columnSpan) {
            this.row = row;
            this.column = column;
            this.rowSpan = rowSpan;
            this.columnSpan = columnSpan;
        }

        /**
         * Returns the start row (0-based).
         *
         * @return the start row
         */
        public int row() {
            return row;
        }

        /**
         * Returns the start column (0-based).
         *
         * @return the start column
         */
        public int column() {
            return column;
        }

        /**
         * Returns the number of rows spanned.
         *
         * @return the row span
         */
        public int rowSpan() {
            return rowSpan;
        }

        /**
         * Returns the number of columns spanned.
         *
         * @return the column span
         */
        public int columnSpan() {
            return columnSpan;
        }

        /**
         * Returns the end row (exclusive).
         *
         * @return row + rowSpan
         */
        public int endRow() {
            return row + rowSpan;
        }

        /**
         * Returns the end column (exclusive).
         *
         * @return column + columnSpan
         */
        public int endColumn() {
            return column + columnSpan;
        }

        @Override
        public String toString() {
            return String.format("AreaBounds[row=%d, col=%d, rowSpan=%d, colSpan=%d]", row, column,
                    rowSpan, columnSpan);
        }
    }
}

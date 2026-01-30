/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.table;

import java.util.List;

/**
 * State for a {@link Table} widget.
 * <p>
 * Tracks the currently selected row and manages scrolling offset for tables
 * that don't fit in the display area.
 *
 * <pre>{@code
 * TableState state = new TableState();
 * state.select(0); // Select first row
 *
 * // In event handling:
 * state.selectNext(table.rows().size());
 * state.selectPrevious();
 * }</pre>
 */
public final class TableState {

    private Integer selected;
    private int offset;

    /**
     * Creates a new table state with no selection.
     */
    public TableState() {
        this.selected = null;
        this.offset = 0;
    }

    /**
     * Returns the index of the currently selected row, or null if nothing is
     * selected.
     *
     * @return the selected row index, or null if nothing is selected
     */
    public Integer selected() {
        return selected;
    }

    /**
     * Returns the scroll offset.
     *
     * @return the scroll offset
     */
    public int offset() {
        return offset;
    }

    /**
     * Selects the row at the given index.
     *
     * @param index
     *            the row index to select
     */
    public void select(int index) {
        this.selected = Math.max(0, index);
    }

    /**
     * Clears the selection.
     */
    public void clearSelection() {
        this.selected = null;
    }

    /**
     * Selects the first row.
     */
    public void selectFirst() {
        this.selected = 0;
    }

    /**
     * Selects the last row.
     *
     * @param rowCount
     *            the total number of rows
     */
    public void selectLast(int rowCount) {
        if (rowCount > 0) {
            this.selected = rowCount - 1;
        }
    }

    /**
     * Selects the next row.
     *
     * @param rowCount
     *            the total number of rows
     */
    public void selectNext(int rowCount) {
        if (selected == null) {
            if (rowCount > 0) {
                selected = 0;
            }
        } else if (selected < rowCount - 1) {
            selected++;
        }
    }

    /**
     * Selects the previous row.
     */
    public void selectPrevious() {
        if (selected == null) {
            selected = 0;
        } else if (selected > 0) {
            selected--;
        }
    }

    /**
     * Scrolls to make the selected row visible.
     *
     * @param visibleRows
     *            the number of rows visible in the display area
     * @param rows
     *            the list of rows
     */
    public void scrollToSelected(int visibleRows, List<Row> rows) {
        if (selected == null || rows.isEmpty()) {
            return;
        }

        int selectedRow = selected;

        // Calculate the offset needed to show the selected row
        int rowsBefore = 0;
        for (int i = 0; i < selectedRow && i < rows.size(); i++) {
            rowsBefore += rows.get(i).totalHeight();
        }

        // Scroll up if selected is above visible area
        if (rowsBefore < offset) {
            offset = rowsBefore;
        }

        // Scroll down if selected is below visible area
        int selectedHeight = selectedRow < rows.size() ? rows.get(selectedRow).totalHeight() : 1;
        if (rowsBefore + selectedHeight > offset + visibleRows) {
            offset = rowsBefore + selectedHeight - visibleRows;
        }

        offset = Math.max(0, offset);
    }

    /**
     * Sets the scroll offset directly.
     *
     * @param offset
     *            the scroll offset to set
     */
    public void setOffset(int offset) {
        this.offset = Math.max(0, offset);
    }
}

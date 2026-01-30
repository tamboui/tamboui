/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.list;

import java.util.List;

/**
 * State for a List widget, tracking selection and scroll offset.
 */
public final class ListState {

    private Integer selected;
    private int offset;

    /**
     * Creates a new list state with no selection.
     */
    public ListState() {
        this.selected = null;
        this.offset = 0;
    }

    /**
     * Returns the index of the currently selected item, or null if nothing is
     * selected.
     *
     * @return the selected item index, or null if nothing is selected
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
     * Selects the item at the given index.
     *
     * @param index
     *            the item index to select
     */
    public void select(Integer index) {
        this.selected = index;
    }

    /**
     * Selects the first item.
     */
    public void selectFirst() {
        this.selected = 0;
    }

    /**
     * Selects the last item.
     *
     * @param itemCount
     *            the total number of items
     */
    public void selectLast(int itemCount) {
        if (itemCount > 0) {
            this.selected = itemCount - 1;
        }
    }

    /**
     * Selects the next item.
     *
     * @param itemCount
     *            the total number of items
     */
    public void selectNext(int itemCount) {
        if (itemCount == 0) {
            return;
        }
        if (selected == null) {
            selected = 0;
        } else if (selected < itemCount - 1) {
            selected++;
        }
    }

    /**
     * Selects the previous item.
     */
    public void selectPrevious() {
        if (selected == null) {
            return;
        }
        if (selected > 0) {
            selected--;
        }
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

    /**
     * Scrolls the list to ensure the selected item is visible.
     *
     * @param visibleHeight
     *            the visible height in the display area
     * @param items
     *            the list of items
     */
    public void scrollToSelected(int visibleHeight, List<ListItem> items) {
        if (selected == null || items.isEmpty()) {
            return;
        }

        // Calculate the top position of the selected item
        int selectedTop = 0;
        for (int i = 0; i < selected && i < items.size(); i++) {
            selectedTop += items.get(i).height();
        }
        int selectedBottom = selectedTop + items.get(selected).height();

        // Adjust offset if needed
        if (selectedTop < offset) {
            offset = selectedTop;
        } else if (selectedBottom > offset + visibleHeight) {
            offset = selectedBottom - visibleHeight;
        }
    }

    /**
     * Scrolls the list to show the last items without changing selection.
     * <p>
     * This is useful for chat messages, logs, or other content where you want to
     * always show the most recent items without needing to select them.
     *
     * @param visibleHeight
     *            the visible height in the display area
     * @param items
     *            the list of items
     */
    public void scrollToEnd(int visibleHeight, List<ListItem> items) {
        if (items.isEmpty()) {
            offset = 0;
            return;
        }

        // Calculate total content height
        int totalHeight = 0;
        for (ListItem item : items) {
            totalHeight += item.height();
        }

        // Set offset to show the last items
        if (totalHeight > visibleHeight) {
            offset = totalHeight - visibleHeight;
        } else {
            offset = 0;
        }
    }
}

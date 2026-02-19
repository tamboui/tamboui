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

    // Sticky scroll state
    private boolean userScrolledAway;
    private int lastDataSize;

    /**
     * Creates a new list state with no selection.
     */
    public ListState() {
        this.selected = null;
        this.offset = 0;
    }

    /**
     * Returns the index of the currently selected item, or null if nothing is selected.
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
     * @param index the item index to select
     */
    public void select(Integer index) {
        this.selected = index;
    }

    /**
     * Selects the first item.
     */
    public void selectFirst() {
        this.selected = 0;
        this.offset = 0;
    }

    /**
     * Selects the last item.
     *
     * @param itemCount the total number of items
     */
    public void selectLast(int itemCount) {
        if (itemCount > 0) {
            this.selected = itemCount - 1;
        }
    }

    /**
     * Selects the next item.
     *
     * @param itemCount the total number of items
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
     * @param offset the scroll offset to set
     */
    public void setOffset(int offset) {
        this.offset = Math.max(0, offset);
    }

    /**
     * Adjusts the scroll offset by a delta.
     *
     * @param delta the amount to scroll (positive = down, negative = up)
     */
    public void scrollBy(int delta) {
        this.offset = Math.max(0, this.offset + delta);
    }

    /**
     * Returns whether the user has scrolled away from the bottom (for sticky scroll).
     *
     * @return true if the user has scrolled away
     */
    public boolean isUserScrolledAway() {
        return userScrolledAway;
    }

    /**
     * Marks that the user has scrolled away from the bottom.
     */
    public void markUserScrolledAway() {
        this.userScrolledAway = true;
    }

    /**
     * Resumes auto-scrolling to the bottom (for sticky scroll).
     * <p>
     * The scroll offset will be set to the maximum during the next render.
     */
    public void resumeAutoScroll() {
        this.userScrolledAway = false;
    }

    /**
     * Scrolls the list to ensure the selected item is visible.
     *
     * @param visibleHeight the visible height in the display area
     * @param items the list of items
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
     * Scrolls the list to ensure the selected item is visible,
     * using pre-computed item heights.
     *
     * @param visibleHeight the visible height in the display area
     * @param itemHeights array of item heights
     */
    public void scrollToSelected(int visibleHeight, int[] itemHeights) {
        if (selected == null || itemHeights.length == 0) {
            return;
        }

        int sel = Math.min(selected, itemHeights.length - 1);

        int selectedTop = 0;
        for (int i = 0; i < sel; i++) {
            selectedTop += itemHeights[i];
        }
        int selectedBottom = selectedTop + itemHeights[sel];

        if (selectedTop < offset) {
            offset = selectedTop;
        } else if (selectedBottom > offset + visibleHeight) {
            offset = selectedBottom - visibleHeight;
        }

        offset = Math.max(0, offset);
    }

    /**
     * Applies sticky scroll logic.
     * <p>
     * Auto-scrolls to the end when new items are added, but pauses
     * when the user has scrolled away. Resumes when the user scrolls
     * back to the bottom.
     *
     * @param totalItems the total number of items
     * @param totalHeight the total content height in rows
     * @param visibleHeight the visible viewport height
     */
    public void applyStickyScroll(int totalItems, int totalHeight, int visibleHeight) {
        int maxScroll = Math.max(0, totalHeight - visibleHeight);

        boolean newItemsAdded = totalItems > lastDataSize;
        lastDataSize = totalItems;

        // Clamp
        offset = Math.min(offset, maxScroll);
        offset = Math.max(0, offset);

        // Check if at bottom
        boolean atBottom = maxScroll > 0 && offset >= maxScroll;

        if (atBottom) {
            userScrolledAway = false;
        } else if (newItemsAdded && !userScrolledAway) {
            offset = maxScroll;
        }

        if (!userScrolledAway) {
            offset = maxScroll;
        }
    }

    /**
     * Applies scroll-to-end mode (unconditional pin to bottom).
     *
     * @param totalHeight the total content height in rows
     * @param visibleHeight the visible viewport height
     */
    public void applyScrollToEnd(int totalHeight, int visibleHeight) {
        offset = Math.max(0, totalHeight - visibleHeight);
    }

    /**
     * Scrolls the list to show the last items without changing selection.
     *
     * @param visibleHeight the visible height in the display area
     * @param items the list of items
     */
    public void scrollToEnd(int visibleHeight, List<ListItem> items) {
        if (items.isEmpty()) {
            offset = 0;
            return;
        }

        int totalHeight = 0;
        for (ListItem item : items) {
            totalHeight += item.height();
        }

        if (totalHeight > visibleHeight) {
            offset = totalHeight - visibleHeight;
        } else {
            offset = 0;
        }
    }
}

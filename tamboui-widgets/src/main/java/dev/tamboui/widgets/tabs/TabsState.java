/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.tabs;

/**
 * State for a {@link Tabs} widget.
 * <p>
 * Tracks the currently selected tab index.
 *
 * <pre>{@code
 * TabsState state = new TabsState();
 * state.select(0); // Select first tab
 *
 * // In event handling:
 * state.selectNext(tabs.size());
 * state.selectPrevious(tabs.size());
 * }</pre>
 */
public final class TabsState {

    private Integer selected;

    /**
     * Creates a new tabs state with no selection.
     */
    public TabsState() {
        this.selected = null;
    }

    /**
     * Creates a new tabs state with the given selection.
     *
     * @param selected
     *            the initial selected tab index
     */
    public TabsState(int selected) {
        this.selected = Math.max(0, selected);
    }

    /**
     * Returns the index of the currently selected tab, or null if nothing is
     * selected.
     *
     * @return the selected tab index, or null if nothing is selected
     */
    public Integer selected() {
        return selected;
    }

    /**
     * Selects the tab at the given index.
     *
     * @param index
     *            the tab index to select
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
     * Selects the first tab.
     */
    public void selectFirst() {
        this.selected = 0;
    }

    /**
     * Selects the last tab.
     *
     * @param tabCount
     *            the total number of tabs
     */
    public void selectLast(int tabCount) {
        if (tabCount > 0) {
            this.selected = tabCount - 1;
        }
    }

    /**
     * Selects the next tab, wrapping to the first if at the end.
     *
     * @param tabCount
     *            the total number of tabs
     */
    public void selectNext(int tabCount) {
        if (tabCount == 0) {
            return;
        }
        if (selected == null) {
            selected = 0;
        } else {
            selected = (selected + 1) % tabCount;
        }
    }

    /**
     * Selects the previous tab, wrapping to the last if at the beginning.
     *
     * @param tabCount
     *            the total number of tabs
     */
    public void selectPrevious(int tabCount) {
        if (tabCount == 0) {
            return;
        }
        if (selected == null) {
            selected = tabCount - 1;
        } else {
            selected = (selected - 1 + tabCount) % tabCount;
        }
    }
}

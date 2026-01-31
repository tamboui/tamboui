/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.select;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * State for the {@link Select} widget, tracking options and selected index.
 *
 * <pre>{@code
 * // Create with options
 * SelectState state = new SelectState("Option A", "Option B", "Option C");
 *
 * // Navigate
 * state.selectNext();      // Move to next option (wraps)
 * state.selectPrevious();  // Move to previous option (wraps)
 * state.selectIndex(2);    // Jump to specific index
 *
 * // Get current selection
 * String value = state.selectedValue();
 * int index = state.selectedIndex();
 * }</pre>
 *
 * @see Select
 */
public final class SelectState {

    private final List<String> options;
    private int selectedIndex;

    /**
     * Creates a new state with the given options.
     * The first option is selected by default.
     *
     * @param options the available options
     */
    public SelectState(String... options) {
        this(Arrays.asList(options), 0);
    }

    /**
     * Creates a new state with the given options and initial selection.
     *
     * @param options the available options
     * @param selectedIndex the initially selected index
     */
    public SelectState(List<String> options, int selectedIndex) {
        this.options = new ArrayList<>(options);
        this.selectedIndex = options.isEmpty() ? -1 : Math.max(0, Math.min(selectedIndex, options.size() - 1));
    }

    /**
     * Returns the list of available options.
     *
     * @return unmodifiable list of options
     */
    public List<String> options() {
        return Collections.unmodifiableList(options);
    }

    /**
     * Returns the number of options.
     *
     * @return the option count
     */
    public int optionCount() {
        return options.size();
    }

    /**
     * Returns the currently selected index.
     *
     * @return the selected index, or -1 if no options
     */
    public int selectedIndex() {
        return selectedIndex;
    }

    /**
     * Returns the currently selected value.
     *
     * @return the selected value, or empty string if no options
     */
    public String selectedValue() {
        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            return options.get(selectedIndex);
        }
        return "";
    }

    /**
     * Alias for {@link #selectedValue()} for compatibility with form state patterns.
     *
     * @return the selected value
     */
    public String value() {
        return selectedValue();
    }

    /**
     * Sets the selected index.
     *
     * @param index the index to select
     */
    public void selectIndex(int index) {
        if (options.isEmpty()) {
            return;
        }
        this.selectedIndex = Math.max(0, Math.min(index, options.size() - 1));
    }

    /**
     * Selects the next option, wrapping to the first if at the end.
     */
    public void selectNext() {
        if (options.isEmpty()) {
            return;
        }
        selectedIndex = (selectedIndex + 1) % options.size();
    }

    /**
     * Selects the previous option, wrapping to the last if at the beginning.
     */
    public void selectPrevious() {
        if (options.isEmpty()) {
            return;
        }
        selectedIndex = (selectedIndex - 1 + options.size()) % options.size();
    }

    /**
     * Selects the first option.
     */
    public void selectFirst() {
        if (!options.isEmpty()) {
            selectedIndex = 0;
        }
    }

    /**
     * Selects the last option.
     */
    public void selectLast() {
        if (!options.isEmpty()) {
            selectedIndex = options.size() - 1;
        }
    }

    /**
     * Replaces the available options.
     * Selection is reset to the first option.
     *
     * @param newOptions the new options
     */
    public void setOptions(List<String> newOptions) {
        options.clear();
        options.addAll(newOptions);
        selectedIndex = options.isEmpty() ? -1 : 0;
    }

    /**
     * Replaces the available options.
     * Selection is reset to the first option.
     *
     * @param newOptions the new options
     */
    public void setOptions(String... newOptions) {
        setOptions(Arrays.asList(newOptions));
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.form;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * State for a select form field (dropdown selection).
 * <p>
 * Example usage:
 * <pre>{@code
 * SelectFieldState countryState = new SelectFieldState(
 *     Arrays.asList("USA", "UK", "Germany", "France"),
 *     0  // Initially select "USA"
 * );
 *
 * // Get selected value
 * String selected = countryState.selectedValue();  // "USA"
 *
 * // Change selection
 * countryState.selectIndex(2);  // Selects "Germany"
 * }</pre>
 */
public final class SelectFieldState {

    private final List<String> options;
    private int selectedIndex;

    /**
     * Creates a new select field state with the given options and initial selection.
     *
     * @param options the available options
     * @param initialIndex the initially selected index (0-based)
     * @throws NullPointerException if options is null
     * @throws IllegalArgumentException if options is empty or initialIndex is out of bounds
     */
    public SelectFieldState(List<String> options, int initialIndex) {
        Objects.requireNonNull(options, "Options must not be null");
        if (options.isEmpty()) {
            throw new IllegalArgumentException("Options must not be empty");
        }
        if (initialIndex < 0 || initialIndex >= options.size()) {
            throw new IllegalArgumentException("Initial index out of bounds: " + initialIndex);
        }
        this.options = new ArrayList<>(options);
        this.selectedIndex = initialIndex;
    }

    /**
     * Creates a new select field state with the given options, selecting the first option.
     *
     * @param options the available options
     * @throws NullPointerException if options is null
     * @throws IllegalArgumentException if options is empty
     */
    public SelectFieldState(List<String> options) {
        this(options, 0);
    }

    /**
     * Creates a new select field state with the given options, selecting the first option.
     *
     * @param options the available options
     * @throws IllegalArgumentException if no options are provided
     */
    public SelectFieldState(String... options) {
        this(Arrays.asList(options), 0);
    }

    /**
     * Returns the list of available options.
     *
     * @return an unmodifiable list of options
     */
    public List<String> options() {
        return Collections.unmodifiableList(options);
    }

    /**
     * Returns the currently selected index.
     *
     * @return the selected index (0-based)
     */
    public int selectedIndex() {
        return selectedIndex;
    }

    /**
     * Returns the currently selected value.
     *
     * @return the selected option value
     */
    public String selectedValue() {
        return options.get(selectedIndex);
    }

    /**
     * Selects the option at the given index.
     *
     * @param index the index to select (0-based)
     * @throws IllegalArgumentException if index is out of bounds
     */
    public void selectIndex(int index) {
        if (index < 0 || index >= options.size()) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }
        this.selectedIndex = index;
    }

    /**
     * Selects the next option, wrapping to the first if at the end.
     */
    public void selectNext() {
        selectedIndex = (selectedIndex + 1) % options.size();
    }

    /**
     * Selects the previous option, wrapping to the last if at the beginning.
     */
    public void selectPrevious() {
        selectedIndex = (selectedIndex - 1 + options.size()) % options.size();
    }

    /**
     * Returns the number of available options.
     *
     * @return the number of options
     */
    public int size() {
        return options.size();
    }
}

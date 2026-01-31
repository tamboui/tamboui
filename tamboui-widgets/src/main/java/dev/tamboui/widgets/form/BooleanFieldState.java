/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.form;

/**
 * State for a boolean form field (checkbox or toggle).
 * <p>
 * Example usage:
 * <pre>{@code
 * BooleanFieldState newsletterState = new BooleanFieldState(true);
 *
 * // Toggle the value
 * newsletterState.toggle();
 *
 * // Check the current value
 * if (newsletterState.value()) {
 *     // ...
 * }
 * }</pre>
 */
public final class BooleanFieldState {

    private boolean value;

    /**
     * Creates a new boolean field state with the given initial value.
     *
     * @param initialValue the initial value
     */
    public BooleanFieldState(boolean initialValue) {
        this.value = initialValue;
    }

    /**
     * Creates a new boolean field state with false as the initial value.
     */
    public BooleanFieldState() {
        this(false);
    }

    /**
     * Returns the current value.
     *
     * @return the current boolean value
     */
    public boolean value() {
        return value;
    }

    /**
     * Sets the value.
     *
     * @param value the new value
     */
    public void setValue(boolean value) {
        this.value = value;
    }

    /**
     * Toggles the value (true becomes false, false becomes true).
     */
    public void toggle() {
        this.value = !this.value;
    }
}

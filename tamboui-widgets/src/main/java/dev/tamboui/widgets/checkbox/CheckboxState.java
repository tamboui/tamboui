/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.checkbox;

/**
 * State for the {@link Checkbox} widget, tracking checked/unchecked status.
 *
 * <pre>{@code
 * CheckboxState state = new CheckboxState();        // unchecked
 * CheckboxState state = new CheckboxState(true);   // checked
 *
 * // Toggle the state
 * state.toggle();
 *
 * // Check current value
 * if (state.isChecked()) { ... }
 * }</pre>
 *
 * @see Checkbox
 */
public final class CheckboxState {

    private boolean checked;

    /**
     * Creates a new unchecked state.
     */
    public CheckboxState() {
        this.checked = false;
    }

    /**
     * Creates a new state with the given initial value.
     *
     * @param checked the initial checked state
     */
    public CheckboxState(boolean checked) {
        this.checked = checked;
    }

    /**
     * Returns whether the checkbox is checked.
     *
     * @return true if checked
     */
    public boolean isChecked() {
        return checked;
    }

    /**
     * Alias for {@link #isChecked()} for compatibility with form state patterns.
     *
     * @return true if checked
     */
    public boolean value() {
        return checked;
    }

    /**
     * Sets the checked state.
     *
     * @param checked the new checked state
     */
    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    /**
     * Alias for {@link #setChecked(boolean)} for compatibility with form state patterns.
     *
     * @param value the new value
     */
    public void setValue(boolean value) {
        this.checked = value;
    }

    /**
     * Toggles the checked state and returns the new value.
     *
     * @return the new checked state after toggling
     */
    public boolean toggle() {
        checked = !checked;
        return checked;
    }
}

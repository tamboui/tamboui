/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toggle;

/**
 * State for the {@link Toggle} widget, tracking on/off status.
 *
 * <pre>{@code
 * ToggleState state = new ToggleState();        // off
 * ToggleState state = new ToggleState(true);   // on
 *
 * // Toggle the state
 * state.toggle();
 *
 * // Check current value
 * if (state.isOn()) { ... }
 * }</pre>
 *
 * @see Toggle
 */
public final class ToggleState {

    private boolean on;

    /**
     * Creates a new state in the off position.
     */
    public ToggleState() {
        this.on = false;
    }

    /**
     * Creates a new state with the given initial value.
     *
     * @param on the initial on/off state
     */
    public ToggleState(boolean on) {
        this.on = on;
    }

    /**
     * Returns whether the toggle is on.
     *
     * @return true if on
     */
    public boolean isOn() {
        return on;
    }

    /**
     * Alias for {@link #isOn()} for compatibility with form state patterns.
     *
     * @return true if on
     */
    public boolean value() {
        return on;
    }

    /**
     * Sets the on/off state.
     *
     * @param on the new state
     */
    public void setOn(boolean on) {
        this.on = on;
    }

    /**
     * Alias for {@link #setOn(boolean)} for compatibility with form state patterns.
     *
     * @param value the new value
     */
    public void setValue(boolean value) {
        this.on = value;
    }

    /**
     * Toggles the on/off state and returns the new value.
     *
     * @return the new state after toggling
     */
    public boolean toggle() {
        on = !on;
        return on;
    }
}

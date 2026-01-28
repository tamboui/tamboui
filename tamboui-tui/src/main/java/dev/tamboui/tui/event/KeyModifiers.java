/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

/**
 * Represents keyboard modifier keys (Ctrl, Alt, Shift).
 */
public final class KeyModifiers {

    /** No modifiers pressed. */
    public static final KeyModifiers NONE = new KeyModifiers(false, false, false);

    /** Only Ctrl pressed. */
    public static final KeyModifiers CTRL = new KeyModifiers(true, false, false);

    /** Only Alt pressed. */
    public static final KeyModifiers ALT = new KeyModifiers(false, true, false);

    /** Only Shift pressed. */
    public static final KeyModifiers SHIFT = new KeyModifiers(false, false, true);

    private final boolean ctrl;
    private final boolean alt;
    private final boolean shift;

    /**
     * Creates a new KeyModifiers instance with the specified modifier flags.
     *
     * @param ctrl  true if Ctrl is pressed
     * @param alt   true if Alt is pressed
     * @param shift true if Shift is pressed
     */
    public KeyModifiers(boolean ctrl, boolean alt, boolean shift) {
        this.ctrl = ctrl;
        this.alt = alt;
        this.shift = shift;
    }

    /**
     * Creates modifiers with the specified flags.
     *
     * @param ctrl  true if Ctrl is pressed
     * @param alt   true if Alt is pressed
     * @param shift true if Shift is pressed
     * @return the key modifiers instance
     */
    public static KeyModifiers of(boolean ctrl, boolean alt, boolean shift) {
        if (!ctrl && !alt && !shift) {
            return NONE;
        }
        return new KeyModifiers(ctrl, alt, shift);
    }

    /**
     * Returns whether the Ctrl modifier is active.
     *
     * @return true if Ctrl is pressed
     */
    public boolean ctrl() {
        return ctrl;
    }

    /**
     * Returns whether the Alt modifier is active.
     *
     * @return true if Alt is pressed
     */
    public boolean alt() {
        return alt;
    }

    /**
     * Returns whether the Shift modifier is active.
     *
     * @return true if Shift is pressed
     */
    public boolean shift() {
        return shift;
    }

    /**
     * Returns true if no modifiers are pressed.
     *
     * @return true if no modifiers are active
     */
    public boolean isEmpty() {
        return !ctrl && !alt && !shift;
    }

    /**
     * Returns true if any modifier is pressed.
     *
     * @return true if at least one modifier is active
     */
    public boolean hasAny() {
        return ctrl || alt || shift;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KeyModifiers)) {
            return false;
        }
        KeyModifiers that = (KeyModifiers) o;
        return ctrl == that.ctrl && alt == that.alt && shift == that.shift;
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(ctrl);
        result = 31 * result + Boolean.hashCode(alt);
        result = 31 * result + Boolean.hashCode(shift);
        return result;
    }

    @Override
    public String toString() {
        return String.format("KeyModifiers[ctrl=%s, alt=%s, shift=%s]", ctrl, alt, shift);
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

/**
 * Represents keyboard lock and keypad state associated with a key event.
 * <p>
 * Distinguishes lock keys (Caps Lock, Num Lock) and keypad keys from standard
 * keyboard navigation and typing.
 */
public final class KeyEventState {

    /** No special key state (no locks active, not keypad). */
    public static final KeyEventState NONE = new KeyEventState(false, false, false);

    private final boolean capsLock;
    private final boolean numLock;
    private final boolean keypad;

    /**
     * Creates a new KeyEventState instance with the specified flags.
     *
     * @param capsLock true if Caps Lock was active
     * @param numLock  true if Num Lock was active
     * @param keypad   true if the key was on the numeric keypad
     */
    public KeyEventState(boolean capsLock, boolean numLock, boolean keypad) {
        this.capsLock = capsLock;
        this.numLock = numLock;
        this.keypad = keypad;
    }

    /**
     * Creates a KeyEventState instance with the specified flags.
     *
     * @param capsLock true if Caps Lock was active
     * @param numLock  true if Num Lock was active
     * @param keypad   true if the key was on the numeric keypad
     * @return the key event state instance
     */
    public static KeyEventState of(boolean capsLock, boolean numLock, boolean keypad) {
        if (!capsLock && !numLock && !keypad) {
            return NONE;
        }
        return new KeyEventState(capsLock, numLock, keypad);
    }

    /**
     * Returns true if Caps Lock was active.
     *
     * @return true if Caps Lock is active
     */
    public boolean capsLock() {
        return capsLock;
    }

    /**
     * Returns true if Num Lock was active.
     *
     * @return true if Num Lock is active
     */
    public boolean numLock() {
        return numLock;
    }

    /**
     * Returns true if the key was pressed on the numeric keypad.
     *
     * @return true if keypad key
     */
    public boolean keypad() {
        return keypad;
    }

    /**
     * Returns true if no special lock or keypad state is active.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return !capsLock && !numLock && !keypad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KeyEventState)) {
            return false;
        }
        KeyEventState that = (KeyEventState) o;
        return capsLock == that.capsLock
                && numLock == that.numLock
                && keypad == that.keypad;
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(capsLock);
        result = 31 * result + Boolean.hashCode(numLock);
        result = 31 * result + Boolean.hashCode(keypad);
        return result;
    }

    @Override
    public String toString() {
        return String.format("KeyEventState[capsLock=%s, numLock=%s, keypad=%s]", capsLock, numLock, keypad);
    }
}

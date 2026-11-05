/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

/**
 * Represents the kind of a keyboard event (press, repeat, or release).
 */
public enum KeyEventKind {

    /** The key was pressed down. */
    PRESS,

    /** The key was repeated while being held down. */
    REPEAT,

    /** The key was released. */
    RELEASE;

    /**
     * Returns true if this is a {@link #PRESS} event.
     *
     * @return true if press
     */
    public boolean isPress() {
        return this == PRESS;
    }

    /**
     * Returns true if this is a {@link #REPEAT} event.
     *
     * @return true if repeat
     */
    public boolean isRepeat() {
        return this == REPEAT;
    }

    /**
     * Returns true if this is a {@link #RELEASE} event.
     *
     * @return true if release
     */
    public boolean isRelease() {
        return this == RELEASE;
    }

    /**
     * Returns true if this is either a {@link #PRESS} or {@link #REPEAT} event.
     *
     * @return true if press or repeat
     */
    public boolean isPressOrRepeat() {
        return this == PRESS || this == REPEAT;
    }
}

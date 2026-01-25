/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

/**
 * Represents keyboard key codes for special keys.
 * <p>
 * Regular printable characters use {@link #CHAR} and the character
 * can be retrieved from {@link KeyEvent#character()}.
 */
public enum KeyCode {
    // Control keys
    ENTER,
    ESCAPE,
    BACKSPACE,
    TAB,
    DELETE,
    INSERT,

    // Arrow keys
    UP,
    DOWN,
    LEFT,
    RIGHT,

    // Navigation keys
    HOME,
    END,
    PAGE_UP,
    PAGE_DOWN,

    // Function keys
    F1, F2, F3, F4, F5, F6, F7, F8, F9, F10, F11, F12,

    // Special
    /** Regular printable character - check {@link KeyEvent#character()} */
    CHAR,

    /** Unknown or unrecognized key */
    UNKNOWN
}

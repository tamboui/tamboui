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
    /** Enter key. */
    ENTER,
    /** Escape key. */
    ESCAPE,
    /** Backspace key. */
    BACKSPACE,
    /** Tab key. */
    TAB,
    /** Delete key. */
    DELETE,
    /** Insert key. */
    INSERT,

    // Arrow keys
    /** Up arrow key. */
    UP,
    /** Down arrow key. */
    DOWN,
    /** Left arrow key. */
    LEFT,
    /** Right arrow key. */
    RIGHT,

    // Navigation keys
    /** Home key. */
    HOME,
    /** End key. */
    END,
    /** Page Up key. */
    PAGE_UP,
    /** Page Down key. */
    PAGE_DOWN,

    // Function keys
    /** F1 function key. */
    F1,
    /** F2 function key. */
    F2,
    /** F3 function key. */
    F3,
    /** F4 function key. */
    F4,
    /** F5 function key. */
    F5,
    /** F6 function key. */
    F6,
    /** F7 function key. */
    F7,
    /** F8 function key. */
    F8,
    /** F9 function key. */
    F9,
    /** F10 function key. */
    F10,
    /** F11 function key. */
    F11,
    /** F12 function key. */
    F12,

    // Special
    /** Regular printable character - check {@link KeyEvent#character()} */
    CHAR,

    /** Unknown or unrecognized key */
    UNKNOWN
}

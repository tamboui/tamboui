/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

/**
 * Represents keyboard key codes for special keys.
 * <p>
 * Regular printable characters use {@link #CHAR} and the character
 * can be retrieved from {@link KeyEvent#character()} or {@link KeyEvent#string()}.
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

    // Function keys (F1–F12)
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

    // Extended function keys (F13–F35)
    /** F13 function key. */
    F13,
    /** F14 function key. */
    F14,
    /** F15 function key. */
    F15,
    /** F16 function key. */
    F16,
    /** F17 function key. */
    F17,
    /** F18 function key. */
    F18,
    /** F19 function key. */
    F19,
    /** F20 function key. */
    F20,
    /** F21 function key. */
    F21,
    /** F22 function key. */
    F22,
    /** F23 function key. */
    F23,
    /** F24 function key. */
    F24,
    /** F25 function key. */
    F25,
    /** F26 function key. */
    F26,
    /** F27 function key. */
    F27,
    /** F28 function key. */
    F28,
    /** F29 function key. */
    F29,
    /** F30 function key. */
    F30,
    /** F31 function key. */
    F31,
    /** F32 function key. */
    F32,
    /** F33 function key. */
    F33,
    /** F34 function key. */
    F34,
    /** F35 function key. */
    F35,

    // Lock keys
    /** Caps Lock key. */
    CAPS_LOCK,
    /** Scroll Lock key. */
    SCROLL_LOCK,
    /** Num Lock key. */
    NUM_LOCK,

    // System and miscellaneous keys
    /** Print Screen key. */
    PRINT_SCREEN,
    /** Pause key. */
    PAUSE,
    /** Menu key. */
    MENU,

    // Keypad keys
    /** Keypad 0 key. */
    KP_0,
    /** Keypad 1 key. */
    KP_1,
    /** Keypad 2 key. */
    KP_2,
    /** Keypad 3 key. */
    KP_3,
    /** Keypad 4 key. */
    KP_4,
    /** Keypad 5 key. */
    KP_5,
    /** Keypad 6 key. */
    KP_6,
    /** Keypad 7 key. */
    KP_7,
    /** Keypad 8 key. */
    KP_8,
    /** Keypad 9 key. */
    KP_9,
    /** Keypad Decimal separator key. */
    KP_DECIMAL,
    /** Keypad Divide (/) key. */
    KP_DIVIDE,
    /** Keypad Multiply (*) key. */
    KP_MULTIPLY,
    /** Keypad Subtract (-) key. */
    KP_SUBTRACT,
    /** Keypad Add (+) key. */
    KP_ADD,
    /** Keypad Enter key. */
    KP_ENTER,
    /** Keypad Equal (=) key. */
    KP_EQUAL,
    /** Keypad Separator (comma) key. */
    KP_SEPARATOR,
    /** Keypad Left arrow key. */
    KP_LEFT,
    /** Keypad Right arrow key. */
    KP_RIGHT,
    /** Keypad Up arrow key. */
    KP_UP,
    /** Keypad Down arrow key. */
    KP_DOWN,
    /** Keypad Page Up key. */
    KP_PAGE_UP,
    /** Keypad Page Down key. */
    KP_PAGE_DOWN,
    /** Keypad Home key. */
    KP_HOME,
    /** Keypad End key. */
    KP_END,
    /** Keypad Insert key. */
    KP_INSERT,
    /** Keypad Delete key. */
    KP_DELETE,
    /** Keypad Begin / 5 with NumLock off key. */
    KP_BEGIN,

    // Media keys
    /** Media Play key. */
    MEDIA_PLAY,
    /** Media Pause key. */
    MEDIA_PAUSE,
    /** Media Play/Pause toggle key. */
    MEDIA_PLAY_PAUSE,
    /** Media Reverse key. */
    MEDIA_REVERSE,
    /** Media Stop key. */
    MEDIA_STOP,
    /** Media Fast Forward key. */
    MEDIA_FAST_FORWARD,
    /** Media Rewind key. */
    MEDIA_REWIND,
    /** Media Next Track key. */
    MEDIA_TRACK_NEXT,
    /** Media Previous Track key. */
    MEDIA_TRACK_PREVIOUS,
    /** Media Record key. */
    MEDIA_RECORD,
    /** Lower Volume key. */
    LOWER_VOLUME,
    /** Raise Volume key. */
    RAISE_VOLUME,
    /** Mute Volume key. */
    MUTE_VOLUME,

    // Modifier keys (reported when all-keys mode is active)
    /** Left Shift modifier key. */
    LEFT_SHIFT,
    /** Left Control modifier key. */
    LEFT_CONTROL,
    /** Left Alt modifier key. */
    LEFT_ALT,
    /** Left Super (Command/Windows) key. */
    LEFT_SUPER,
    /** Left Hyper key. */
    LEFT_HYPER,
    /** Left Meta key. */
    LEFT_META,
    /** Right Shift modifier key. */
    RIGHT_SHIFT,
    /** Right Control modifier key. */
    RIGHT_CONTROL,
    /** Right Alt modifier key. */
    RIGHT_ALT,
    /** Right Super (Command/Windows) key. */
    RIGHT_SUPER,
    /** Right Hyper key. */
    RIGHT_HYPER,
    /** Right Meta key. */
    RIGHT_META,
    /** ISO Level 3 Shift key. */
    ISO_LEVEL3_SHIFT,
    /** ISO Level 5 Shift key. */
    ISO_LEVEL5_SHIFT,

    // Special
    /** Regular printable character - check {@link KeyEvent#character()} or {@link KeyEvent#string()}. */
    CHAR,

    /** Unknown or unrecognized key. */
    UNKNOWN
}

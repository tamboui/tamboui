/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

/**
 * Text style modifiers (SGR attributes).
 */
public enum Modifier {
    BOLD(1),
    DIM(2),
    ITALIC(3),
    UNDERLINED(4),
    SLOW_BLINK(5),
    RAPID_BLINK(6),
    REVERSED(7),
    HIDDEN(8),
    CROSSED_OUT(9);

    private final int code;

    Modifier(int code) {
        this.code = code;
    }

    /**
     * Returns the ANSI SGR code to enable this modifier.
     */
    public int code() {
        return code;
    }

    /**
     * Returns the ANSI SGR code to disable this modifier.
     */
    public int resetCode() {
        switch (this) {
            case BOLD:
            case DIM:
                return 22;  // Both reset by 22
            case ITALIC:
                return 23;
            case UNDERLINED:
                return 24;
            case SLOW_BLINK:
            case RAPID_BLINK:
                return 25;
            case REVERSED:
                return 27;
            case HIDDEN:
                return 28;
            case CROSSED_OUT:
                return 29;
            default:
                return 0;
        }
    }
}

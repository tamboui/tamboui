/*
 * Copyright (c) 2025 Glimt Contributors
 * SPDX-License-Identifier: MIT
 */
package ink.glimt.style;

/**
 * Standard ANSI 16 colors (8 normal + 8 bright).
 */
public enum AnsiColor {
    BLACK(0),
    RED(1),
    GREEN(2),
    YELLOW(3),
    BLUE(4),
    MAGENTA(5),
    CYAN(6),
    WHITE(7),
    BRIGHT_BLACK(8),
    BRIGHT_RED(9),
    BRIGHT_GREEN(10),
    BRIGHT_YELLOW(11),
    BRIGHT_BLUE(12),
    BRIGHT_MAGENTA(13),
    BRIGHT_CYAN(14),
    BRIGHT_WHITE(15);

    private final int code;

    AnsiColor(int code) {
        this.code = code;
    }

    /**
     * Returns the ANSI palette index (0-15).
     */
    public int code() {
        return code;
    }

    /**
     * Returns the ANSI SGR code for foreground.
     */
    public int fgCode() {
        return code < 8 ? 30 + code : 90 + (code - 8);
    }

    /**
     * Returns the ANSI SGR code for background.
     */
    public int bgCode() {
        return code < 8 ? 40 + code : 100 + (code - 8);
    }
}

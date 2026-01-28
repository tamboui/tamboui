/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

/**
 * Standard ANSI 16 colors (8 normal + 8 bright).
 */
public enum AnsiColor {
    /** Black (palette index 0). */
    BLACK(0),
    /** Red (palette index 1). */
    RED(1),
    /** Green (palette index 2). */
    GREEN(2),
    /** Yellow (palette index 3). */
    YELLOW(3),
    /** Blue (palette index 4). */
    BLUE(4),
    /** Magenta (palette index 5). */
    MAGENTA(5),
    /** Cyan (palette index 6). */
    CYAN(6),
    /** White (palette index 7). */
    WHITE(7),
    /** Bright black (palette index 8). */
    BRIGHT_BLACK(8),
    /** Bright red (palette index 9). */
    BRIGHT_RED(9),
    /** Bright green (palette index 10). */
    BRIGHT_GREEN(10),
    /** Bright yellow (palette index 11). */
    BRIGHT_YELLOW(11),
    /** Bright blue (palette index 12). */
    BRIGHT_BLUE(12),
    /** Bright magenta (palette index 13). */
    BRIGHT_MAGENTA(13),
    /** Bright cyan (palette index 14). */
    BRIGHT_CYAN(14),
    /** Bright white (palette index 15). */
    BRIGHT_WHITE(15);

    private final int code;

    AnsiColor(int code) {
        this.code = code;
    }

    /**
     * Returns the ANSI palette index (0-15).
     *
     * @return the palette index
     */
    public int code() {
        return code;
    }

    /**
     * Returns the ANSI SGR code for foreground.
     *
     * @return the foreground SGR code
     */
    public int fgCode() {
        return code < 8 ? 30 + code : 90 + (code - 8);
    }

    /**
     * Returns the ANSI SGR code for background.
     *
     * @return the background SGR code
     */
    public int bgCode() {
        return code < 8 ? 40 + code : 100 + (code - 8);
    }
}

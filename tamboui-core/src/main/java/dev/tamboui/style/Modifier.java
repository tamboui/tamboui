/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

/**
 * Text style modifiers (SGR attributes).
 */
public enum Modifier {
    /** Normal (default) text style. */
    NORMAL(0),
    /** Bold text. */
    BOLD(1),
    /** Dim (faint) text. */
    DIM(2),
    /** Italic text. */
    ITALIC(3),
    /** Underlined text. */
    UNDERLINED(4),
    /** Slow blink text. */
    SLOW_BLINK(5),
    /** Rapid blink text. */
    RAPID_BLINK(6),
    /** Reversed (inverse) video text. */
    REVERSED(7),
    /** Hidden (invisible) text. */
    HIDDEN(8),
    /** Crossed-out (strikethrough) text. */
    CROSSED_OUT(9);

    private final int code;

    Modifier(int code) {
        this.code = code;
    }

    /**
     * Returns the ANSI SGR code to enable this modifier.
     *
     * @return the SGR code
     */
    public int code() {
        return code;
    }

    /**
     * Returns the implicit style name for this modifier.
     * <p>
     * Maps each modifier to the conventional style name used
     * for style resolution (e.g., BOLD → "bold", CROSSED_OUT → "strikethrough").
     *
     * @return the style name
     */
    public String implicitStyleName() {
        return name().toLowerCase().replace('_', '-');
    }

}

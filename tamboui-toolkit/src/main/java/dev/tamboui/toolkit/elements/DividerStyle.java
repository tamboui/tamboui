/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

/**
 * Visual line style for {@link DividerElement}.
 * <p>
 * Each style defines the characters used for the left end, middle fill,
 * and right end of the divider.
 */
public enum DividerStyle {

    /**
     * Single-line style: {@code ─}
     */
    SINGLE("─", "─", "─"),

    /**
     * Double-line style: {@code ═}
     */
    DOUBLE("═", "═", "═"),

    /**
     * Bold/heavy style: {@code ━}
     */
    BOLD("━", "━", "━"),

    /**
     * Dotted style: {@code ·}
     */
    DOTTED("·", "·", "·"),

    /**
     * Dashed style: {@code -}
     */
    DASHED("-", "-", "-"),

    /**
     * Heavy style: {@code █}
     */
    HEAVY("█", "█", "█"),

    /**
     * Rounded style: {@code ─} with round corners
     */
    ROUNDED("╭", "─", "╮");

    private final String left;
    private final String line;
    private final String right;

    DividerStyle(String left, String line, String right) {
        this.left = left;
        this.line = line;
        this.right = right;
    }

    /**
     * Returns the left-end cap character.
     *
     * @return the left cap string
     */
    public String left() {
        return left;
    }

    /**
     * Returns the repeating fill character.
     *
     * @return the fill string
     */
    public String line() {
        return line;
    }

    /**
     * Returns the right-end cap character.
     *
     * @return the right cap string
     */
    public String right() {
        return right;
    }
}

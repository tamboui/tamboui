/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

/**
 * Visual line style for {@link DividerElement}.
 * <p>
 * Each style defines the character used to fill the divider.
 */
public enum DividerStyle {

    /**
     * Single-line style: {@code ─}
     */
    SINGLE("─"),

    /**
     * Double-line style: {@code ═}
     */
    DOUBLE("═"),

    /**
     * Bold/heavy style: {@code ━}
     */
    BOLD("━"),

    /**
     * Dotted style: {@code ·}
     */
    DOTTED("·"),

    /**
     * Dashed style: {@code -}
     */
    DASHED("-"),

    /**
     * Heavy style: {@code █}
     */
    HEAVY("█"),

    /**
     * Rounded style: {@code ─}
     */
    ROUNDED("─");

    private final String line;

    DividerStyle(String line) {
        this.line = line;
    }

    /**
     * Returns the repeating fill character.
     *
     * @return the fill string
     */
    public String line() {
        return line;
    }

}

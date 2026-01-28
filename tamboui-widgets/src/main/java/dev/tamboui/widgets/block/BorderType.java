/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.block;

/**
 * Type of border to draw.
 */
public enum BorderType {
    /**
     * No border - renders nothing but still reserves space if borders are enabled.
     */
    NONE(null),
    /** Plain border using single-line box-drawing characters. */
    PLAIN(new BorderSet("─", "─", "│", "│", "┌", "┐", "└", "┘")),
    /** Rounded border using single-line box-drawing characters with rounded corners. */
    ROUNDED(new BorderSet("─", "─", "│", "│", "╭", "╮", "╰", "╯")),
    /** Double-line border using double-line box-drawing characters. */
    DOUBLE(new BorderSet("═", "═", "║", "║", "╔", "╗", "╚", "╝")),
    /** Thick border using heavy box-drawing characters. */
    THICK(new BorderSet("━", "━", "┃", "┃", "┏", "┓", "┗", "┛")),
    /** Light double-dashed border. */
    LIGHT_DOUBLE_DASHED(new BorderSet("╌", "╌", "╎", "╎", "┌", "┐", "└", "┘")),
    /** Heavy double-dashed border. */
    HEAVY_DOUBLE_DASHED(new BorderSet("╍", "╍", "╏", "╏", "┏", "┓", "┗", "┛")),
    /** Light triple-dashed border. */
    LIGHT_TRIPLE_DASHED(new BorderSet("┄", "┄", "┆", "┆", "┌", "┐", "└", "┘")),
    /** Heavy triple-dashed border. */
    HEAVY_TRIPLE_DASHED(new BorderSet("┅", "┅", "┇", "┇", "┏", "┓", "┗", "┛")),
    /** Light quadruple-dashed border. */
    LIGHT_QUADRUPLE_DASHED(new BorderSet("┈", "┈", "┊", "┊", "┌", "┐", "└", "┘")),
    /** Heavy quadruple-dashed border. */
    HEAVY_QUADRUPLE_DASHED(new BorderSet("┉", "┉", "┋", "┋", "┏", "┓", "┗", "┛")),
    /** Quadrant border rendered inside the content area. */
    QUADRANT_INSIDE(new BorderSet("▄", "▀", "▐",
     "▌", "▗", "▖", "▝", "▘")),
    /** Quadrant border rendered outside the content area. */
    QUADRANT_OUTSIDE(new BorderSet("▀", "▄", "▌", "▐", "▛", "▜", "▙", "▟"));

    private final BorderSet set;

    BorderType(BorderSet set) {
        this.set = set;
    }

    /**
     * Returns the border characters for this type.
     *
     * @return the border character set, or {@code null} for {@link #NONE}
     */
    public BorderSet set() {
        return set;
    }
}

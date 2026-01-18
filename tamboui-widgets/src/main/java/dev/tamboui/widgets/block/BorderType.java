/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.block;

/// Type of border to draw.
public enum BorderType {
    /// No border - renders nothing but still reserves space if borders are enabled.
    NONE(null),
    PLAIN(new BorderSet("─", "─", "│", "│", "┌", "┐", "└", "┘")),
    ROUNDED(new BorderSet("─", "─", "│", "│", "╭", "╮", "╰", "╯")),
    DOUBLE(new BorderSet("═", "═", "║", "║", "╔", "╗", "╚", "╝")),
    THICK(new BorderSet("━", "━", "┃", "┃", "┏", "┓", "┗", "┛")),
    LIGHT_DOUBLE_DASHED(new BorderSet("╌", "╌", "╎", "╎", "┌", "┐", "└", "┘")),
    HEAVY_DOUBLE_DASHED(new BorderSet("╍", "╍", "╏", "╏", "┏", "┓", "┗", "┛")),
    LIGHT_TRIPLE_DASHED(new BorderSet("┄", "┄", "┆", "┆", "┌", "┐", "└", "┘")),
    HEAVY_TRIPLE_DASHED(new BorderSet("┅", "┅", "┇", "┇", "┏", "┓", "┗", "┛")),
    LIGHT_QUADRUPLE_DASHED(new BorderSet("┈", "┈", "┊", "┊", "┌", "┐", "└", "┘")),
    HEAVY_QUADRUPLE_DASHED(new BorderSet("┉", "┉", "┋", "┋", "┏", "┓", "┗", "┛")),
    QUADRANT_INSIDE(new BorderSet("▄", "▀", "▐",
     "▌", "▗", "▖", "▝", "▘")),
    QUADRANT_OUTSIDE(new BorderSet("▀", "▄", "▌", "▐", "▛", "▜", "▙", "▟"));

    private final BorderSet set;

    BorderType(BorderSet set) {
        this.set = set;
    }

    /// Returns the border characters for this type.
    public BorderSet set() {
        return set;
    }
}


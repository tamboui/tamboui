/*
 * Copyright (c) 2025 Glimt Contributors
 * SPDX-License-Identifier: MIT
 */
package ink.glimt.widgets.block;

/**
 * Type of border to draw.
 */
public enum BorderType {
    PLAIN(new BorderSet("─", "│", "┌", "┐", "└", "┘")),
    ROUNDED(new BorderSet("─", "│", "╭", "╮", "╰", "╯")),
    DOUBLE(new BorderSet("═", "║", "╔", "╗", "╚", "╝")),
    THICK(new BorderSet("━", "┃", "┏", "┓", "┗", "┛")),
    QUADRANT_INSIDE(new BorderSet("▀", "▌", "▘", "▝", "▖", "▗")),
    QUADRANT_OUTSIDE(new BorderSet("▄", "▐", "▛", "▜", "▙", "▟"));

    private final BorderSet set;

    BorderType(BorderSet set) {
        this.set = set;
    }

    /**
     * Returns the border characters for this type.
     */
    public BorderSet set() {
        return set;
    }
}

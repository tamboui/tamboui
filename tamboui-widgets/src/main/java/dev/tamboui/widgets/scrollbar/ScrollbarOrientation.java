/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.scrollbar;

/**
 * Orientation and position of a {@link Scrollbar}.
 * <p>
 * Determines whether the scrollbar is vertical or horizontal,
 * and which edge of the content area it is placed on.
 */
public enum ScrollbarOrientation {
    /**
     * Vertical scrollbar on the right side of the content area.
     * This is the default orientation.
     */
    VERTICAL_RIGHT,

    /**
     * Vertical scrollbar on the left side of the content area.
     */
    VERTICAL_LEFT,

    /**
     * Horizontal scrollbar at the bottom of the content area.
     */
    HORIZONTAL_BOTTOM,

    /**
     * Horizontal scrollbar at the top of the content area.
     */
    HORIZONTAL_TOP;

    /**
     * Returns true if this orientation is vertical.
     *
     * @return true if vertical
     */
    public boolean isVertical() {
        return this == VERTICAL_RIGHT || this == VERTICAL_LEFT;
    }

    /**
     * Returns true if this orientation is horizontal.
     *
     * @return true if horizontal
     */
    public boolean isHorizontal() {
        return this == HORIZONTAL_BOTTOM || this == HORIZONTAL_TOP;
    }
}

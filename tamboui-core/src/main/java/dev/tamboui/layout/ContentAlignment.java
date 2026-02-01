/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

/**
 * Defines 2D alignment for positioning a child within a container area.
 * <p>
 * Used by layout widgets like Stack to control how children are aligned
 * within their parent area.
 * <p>
 * There are 9 positional values (3x3 grid) plus {@link #STRETCH} which
 * fills the entire container.
 */
public enum ContentAlignment {
    /** Top-left corner. */
    TOP_LEFT,
    /** Top center. */
    TOP_CENTER,
    /** Top-right corner. */
    TOP_RIGHT,
    /** Center-left. */
    CENTER_LEFT,
    /** Center (both horizontal and vertical). */
    CENTER,
    /** Center-right. */
    CENTER_RIGHT,
    /** Bottom-left corner. */
    BOTTOM_LEFT,
    /** Bottom center. */
    BOTTOM_CENTER,
    /** Bottom-right corner. */
    BOTTOM_RIGHT,
    /** Stretch to fill the entire container. */
    STRETCH;

    /**
     * Computes the aligned sub-rect for a child within a container.
     * <p>
     * For {@link #STRETCH}, returns the full container rect.
     * For positional values, computes the position of the child
     * (with the given width and height) within the container.
     *
     * @param container   the container area
     * @param childWidth  the child width
     * @param childHeight the child height
     * @return the aligned rectangle for the child
     */
    public Rect align(Rect container, int childWidth, int childHeight) {
        if (this == STRETCH) {
            return container;
        }

        int w = Math.min(childWidth, container.width());
        int h = Math.min(childHeight, container.height());

        int x;
        int y;

        // Horizontal alignment
        switch (this) {
            case TOP_LEFT:
            case CENTER_LEFT:
            case BOTTOM_LEFT:
                x = container.x();
                break;
            case TOP_CENTER:
            case CENTER:
            case BOTTOM_CENTER:
                x = container.x() + (container.width() - w) / 2;
                break;
            default:
                // TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT
                x = container.x() + container.width() - w;
                break;
        }

        // Vertical alignment
        switch (this) {
            case TOP_LEFT:
            case TOP_CENTER:
            case TOP_RIGHT:
                y = container.y();
                break;
            case CENTER_LEFT:
            case CENTER:
            case CENTER_RIGHT:
                y = container.y() + (container.height() - h) / 2;
                break;
            default:
                // BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
                y = container.y() + container.height() - h;
                break;
        }

        return new Rect(x, y, w, h);
    }
}

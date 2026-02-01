/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

/**
 * Represents a terminal window resize event.
 * <p>
 * This event is triggered when the terminal window size changes,
 * typically due to the user resizing the terminal window.
 */
public final class ResizeEvent implements Event {

    private final int width;
    private final int height;

    /**
     * Creates a resize event with the given dimensions.
     *
     * @param width  the new terminal width in columns
     * @param height the new terminal height in rows
     */
    public ResizeEvent(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Creates a resize event with the given dimensions.
     *
     * @param width  the new terminal width in columns
     * @param height the new terminal height in rows
     * @return a new resize event
     */
    public static ResizeEvent of(int width, int height) {
        return new ResizeEvent(width, height);
    }

    /**
     * Returns the new terminal width in columns.
     *
     * @return the width
     */
    public int width() {
        return width;
    }

    /**
     * Returns the new terminal height in rows.
     *
     * @return the height
     */
    public int height() {
        return height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResizeEvent)) {
            return false;
        }
        ResizeEvent that = (ResizeEvent) o;
        return width == that.width && height == that.height;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(width);
        result = 31 * result + Integer.hashCode(height);
        return result;
    }

    @Override
    public String toString() {
        return String.format("ResizeEvent[width=%d, height=%d]", width, height);
    }
}

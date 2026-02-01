/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

/**
 * A size representing width and height dimensions.
 */
public final class Size {

    /** A size with zero width and height. */
    public static final Size ZERO = new Size(0, 0);

    private final int width;
    private final int height;
    private final int cachedHashCode;

    /**
     * Creates a size.
     *
     * @param width  the width
     * @param height the height
     */
    public Size(int width, int height) {
        this.width = width;
        this.height = height;
        this.cachedHashCode = 31 * width + height;
    }

    /**
     * Returns the width.
     *
     * @return the width
     */
    public int width() {
        return width;
    }

    /**
     * Returns the height.
     *
     * @return the height
     */
    public int height() {
        return height;
    }

    /**
     * Returns the area (width * height).
     *
     * @return the area
     */
    public int area() {
        return width * height;
    }

    /**
     * Returns true if either dimension is zero.
     *
     * @return true if width or height is zero
     */
    public boolean isEmpty() {
        return width == 0 || height == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Size)) {
            return false;
        }
        Size size = (Size) o;
        if (cachedHashCode != size.cachedHashCode) {
            return false;
        }
        return width == size.width && height == size.height;
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    @Override
    public String toString() {
        return String.format("Size[width=%d, height=%d]", width, height);
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

/**
 * Represents the preferred dimensions of an element.
 * <p>
 * This immutable value class bundles width and height together with these semantics:
 * <ul>
 *   <li>Positive values = known size (element should use this exact size)</li>
 *   <li>Zero = explicitly zero-sized (element should collapse)</li>
 *   <li>Negative (-1) = unknown size (element should fill available space)</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * // Element with known dimensions
 * return PreferredSize.of(10, 5);
 *
 * // Element that should fill horizontally but has known height
 * return PreferredSize.heightOnly(3);
 *
 * // Element that should fill both dimensions
 * return PreferredSize.UNKNOWN;
 *
 * // Element that should collapse (zero-sized)
 * return PreferredSize.ZERO;
 * }</pre>
 */
public final class Size {

    /** Unknown size - element should fill available space in both dimensions. */
    public static final Size UNKNOWN = new Size(-1, -1);

    /** Explicitly zero-sized - element should collapse in both dimensions. */
    public static final Size ZERO = new Size(0, 0);

    private final int width;
    private final int height;

    /**
     * Creates a new preferred size with the given dimensions.
     *
     * @param width the preferred width (-1 for unknown, 0 for collapse, positive for known)
     * @param height the preferred height (-1 for unknown, 0 for collapse, positive for known)
     */
    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /**
     * Returns the preferred width.
     *
     * @return the width (-1 for unknown, 0 for collapse, positive for known)
     */
    public int width() {
        return width;
    }

    /**
     * Returns the preferred height.
     *
     * @return the height (-1 for unknown, 0 for collapse, positive for known)
     */
    public int height() {
        return height;
    }

    /**
     * Returns true if the width is known (non-negative).
     *
     * @return true if width is 0 or positive
     */
    public boolean hasKnownWidth() {
        return width >= 0;
    }

    /**
     * Returns true if the height is known (non-negative).
     *
     * @return true if height is 0 or positive
     */
    public boolean hasKnownHeight() {
        return height >= 0;
    }

    /**
     * Returns the width if known, otherwise returns the default value.
     *
     * @param defaultValue the value to return if width is unknown
     * @return the width if known, or defaultValue if unknown
     */
    public int widthOr(int defaultValue) {
        return width >= 0 ? width : defaultValue;
    }

    /**
     * Returns the height if known, otherwise returns the default value.
     *
     * @param defaultValue the value to return if height is unknown
     * @return the height if known, or defaultValue if unknown
     */
    public int heightOr(int defaultValue) {
        return height >= 0 ? height : defaultValue;
    }

    /**
     * Creates a size with the given dimensions.
     *
     * @param width the preferred width
     * @param height the preferred height
     * @return a new PreferredSize instance
     */
    public static Size of(int width, int height) {
        if (width == -1 && height == -1) {
            return UNKNOWN;
        }
        if (width == 0 && height == 0) {
            return ZERO;
        }
        return new Size(width, height);
    }

    /**
     * Creates a size with known width and unknown height (fill vertically).
     *
     * @param width the known width
     * @return a new PreferredSize with unknown height
     */
    public static Size widthOnly(int width) {
        return new Size(width, -1);
    }

    /**
     * Creates a size with known height and unknown width (fill horizontally).
     *
     * @param height the known height
     * @return a new PreferredSize with unknown width
     */
    public static Size heightOnly(int height) {
        return new Size(-1, height);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Size)) {
            return false;
        }
        Size that = (Size) o;
        return width == that.width && height == that.height;
    }

    @Override
    public int hashCode() {
        return 31 * width + height;
    }

    @Override
    public String toString() {
        String w = width >= 0 ? String.valueOf(width) : "?";
        String h = height >= 0 ? String.valueOf(height) : "?";
        return "PreferredSize[" + w + "x" + h + "]";
    }
}

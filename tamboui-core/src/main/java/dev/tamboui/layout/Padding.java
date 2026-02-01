/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

/**
 * Padding inside a block.
 */
public final class Padding {

    /** A padding with zero on all sides. */
    public static final Padding NONE = new Padding(0, 0, 0, 0);

    private final int top;
    private final int right;
    private final int bottom;
    private final int left;

    /**
     * Creates a padding with the given values.
     *
     * @param top    top padding
     * @param right  right padding
     * @param bottom bottom padding
     * @param left   left padding
     */
    public Padding(int top, int right, int bottom, int left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    /**
     * Returns a uniform padding on all sides.
     *
     * @param value padding in cells
     * @return a padding with equal values on all sides
     */
    public static Padding uniform(int value) {
        return new Padding(value, value, value, value);
    }

    /**
     * Returns a padding with symmetric vertical and horizontal values.
     *
     * @param vertical   top/bottom padding
     * @param horizontal left/right padding
     * @return a padding with the given symmetric values
     */
    public static Padding symmetric(int vertical, int horizontal) {
        return new Padding(vertical, horizontal, vertical, horizontal);
    }

    /**
     * Returns a padding applied horizontally only.
     *
     * @param value left/right padding
     * @return a padding with horizontal values set
     */
    public static Padding horizontal(int value) {
        return new Padding(0, value, 0, value);
    }

    /**
     * Returns a padding applied vertically only.
     *
     * @param value top/bottom padding
     * @return a padding with vertical values set
     */
    public static Padding vertical(int value) {
        return new Padding(value, 0, value, 0);
    }

    /**
     * Returns the sum of left and right padding.
     *
     * @return total horizontal padding
     */
    public int horizontalTotal() {
        return left + right;
    }

    /**
     * Returns the sum of top and bottom padding.
     *
     * @return total vertical padding
     */
    public int verticalTotal() {
        return top + bottom;
    }

    /**
     * Returns the top padding.
     *
     * @return top padding
     */
    public int top() {
        return top;
    }

    /**
     * Returns the right padding.
     *
     * @return right padding
     */
    public int right() {
        return right;
    }

    /**
     * Returns the bottom padding.
     *
     * @return bottom padding
     */
    public int bottom() {
        return bottom;
    }

    /**
     * Returns the left padding.
     *
     * @return left padding
     */
    public int left() {
        return left;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Padding)) {
            return false;
        }
        Padding padding = (Padding) o;
        return top == padding.top
            && right == padding.right
            && bottom == padding.bottom
            && left == padding.left;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(top);
        result = 31 * result + Integer.hashCode(right);
        result = 31 * result + Integer.hashCode(bottom);
        result = 31 * result + Integer.hashCode(left);
        return result;
    }

    @Override
    public String toString() {
        return String.format("Padding[top=%d, right=%d, bottom=%d, left=%d]", top, right, bottom, left);
    }
}

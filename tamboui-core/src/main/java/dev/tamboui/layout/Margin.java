/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

/**
 * Margin values for top, right, bottom, and left sides.
 */
public final class Margin {

    /** A margin with zero on all sides. */
    public static final Margin NONE = new Margin(0, 0, 0, 0);

    private final int top;
    private final int right;
    private final int bottom;
    private final int left;
    private final int cachedHashCode;

    /**
     * Creates a margin.
     *
     * @param top    top margin
     * @param right  right margin
     * @param bottom bottom margin
     * @param left   left margin
     */
    public Margin(int top, int right, int bottom, int left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
        this.cachedHashCode = computeHashCode();
    }

    private int computeHashCode() {
        int result = top;
        result = 31 * result + right;
        result = 31 * result + bottom;
        result = 31 * result + left;
        return result;
    }

    /**
     * Returns a uniform margin on all sides.
     *
     * @param value margin in cells
     * @return a margin with equal values on all sides
     */
    public static Margin uniform(int value) {
        return new Margin(value, value, value, value);
    }

    /**
     * Returns a margin with vertical and horizontal values.
     *
     * @param vertical    top/bottom margin
     * @param horizontal  left/right margin
     * @return a margin with the given symmetric values
     */
    public static Margin symmetric(int vertical, int horizontal) {
        return new Margin(vertical, horizontal, vertical, horizontal);
    }

    /**
     * Returns a margin applied horizontally only.
     *
     * @param value left/right margin
     * @return a margin with horizontal values set
     */
    public static Margin horizontal(int value) {
        return new Margin(0, value, 0, value);
    }

    /**
     * Returns a margin applied vertically only.
     *
     * @param value top/bottom margin
     * @return a margin with vertical values set
     */
    public static Margin vertical(int value) {
        return new Margin(value, 0, value, 0);
    }

    /**
     * Returns the top margin.
     *
     * @return top margin
     */
    public int top() {
        return top;
    }

    /**
     * Returns the right margin.
     *
     * @return right margin
     */
    public int right() {
        return right;
    }

    /**
     * Returns the bottom margin.
     *
     * @return bottom margin
     */
    public int bottom() {
        return bottom;
    }

    /**
     * Returns the left margin.
     *
     * @return left margin
     */
    public int left() {
        return left;
    }

    /**
     * Returns the sum of left and right margins.
     *
     * @return total horizontal margin
     */
    public int horizontalTotal() {
        return left + right;
    }

    /**
     * Returns the sum of top and bottom margins.
     *
     * @return total vertical margin
     */
    public int verticalTotal() {
        return top + bottom;
    }

    /**
     * Returns the inner area after applying this margin.
     * <p>
     * The returned rect is inset by the margin values on each side.
     * If the margin would result in negative dimensions, returns an empty rect.
     *
     * @param area the outer area
     * @return the inner area with margin applied
     */
    public Rect inner(Rect area) {
        int newX = area.x() + left;
        int newY = area.y() + top;
        int newWidth = Math.max(0, area.width() - horizontalTotal());
        int newHeight = Math.max(0, area.height() - verticalTotal());
        return new Rect(newX, newY, newWidth, newHeight);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Margin)) {
            return false;
        }
        Margin margin = (Margin) o;
        if (cachedHashCode != margin.cachedHashCode) {
            return false;
        }
        return top == margin.top
            && right == margin.right
            && bottom == margin.bottom
            && left == margin.left;
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    @Override
    public String toString() {
        return String.format("Margin[top=%d, right=%d, bottom=%d, left=%d]", top, right, bottom, left);
    }
}

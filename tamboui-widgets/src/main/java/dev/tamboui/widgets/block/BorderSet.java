/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.block;

/**
 * Characters used to draw a border.
 * Supports asymmetric borders where top/bottom horizontal and left/right vertical
 * characters can differ (e.g., for QUADRANT border types).
 */
public final class BorderSet {
    private final String topHorizontal;
    private final String bottomHorizontal;
    private final String leftVertical;
    private final String rightVertical;
    private final String topLeft;
    private final String topRight;
    private final String bottomLeft;
    private final String bottomRight;

    /**
     * Creates a border set with separate characters for each side.
     */
    public BorderSet(
        String topHorizontal,
        String bottomHorizontal,
        String leftVertical,
        String rightVertical,
        String topLeft,
        String topRight,
        String bottomLeft,
        String bottomRight
    ) {
        this.topHorizontal = topHorizontal;
        this.bottomHorizontal = bottomHorizontal;
        this.leftVertical = leftVertical;
        this.rightVertical = rightVertical;
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomLeft = bottomLeft;
        this.bottomRight = bottomRight;
    }

    public String topHorizontal() {
        return topHorizontal;
    }

    public String bottomHorizontal() {
        return bottomHorizontal;
    }

    public String leftVertical() {
        return leftVertical;
    }

    public String rightVertical() {
        return rightVertical;
    }

    public String topLeft() {
        return topLeft;
    }

    public String topRight() {
        return topRight;
    }

    public String bottomLeft() {
        return bottomLeft;
    }

    public String bottomRight() {
        return bottomRight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BorderSet)) {
            return false;
        }
        BorderSet that = (BorderSet) o;
        return topHorizontal.equals(that.topHorizontal)
            && bottomHorizontal.equals(that.bottomHorizontal)
            && leftVertical.equals(that.leftVertical)
            && rightVertical.equals(that.rightVertical)
            && topLeft.equals(that.topLeft)
            && topRight.equals(that.topRight)
            && bottomLeft.equals(that.bottomLeft)
            && bottomRight.equals(that.bottomRight);
    }

    @Override
    public int hashCode() {
        int result = topHorizontal.hashCode();
        result = 31 * result + bottomHorizontal.hashCode();
        result = 31 * result + leftVertical.hashCode();
        result = 31 * result + rightVertical.hashCode();
        result = 31 * result + topLeft.hashCode();
        result = 31 * result + topRight.hashCode();
        result = 31 * result + bottomLeft.hashCode();
        result = 31 * result + bottomRight.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format(
            "BorderSet[topHorizontal=%s, bottomHorizontal=%s, leftVertical=%s, rightVertical=%s, topLeft=%s, topRight=%s, bottomLeft=%s, bottomRight=%s]",
            topHorizontal, bottomHorizontal, leftVertical, rightVertical, topLeft, topRight, bottomLeft, bottomRight);
    }
}

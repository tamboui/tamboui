/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

/**
 * Padding inside a block.
 */
public final class Padding {

    public static final Padding NONE = new Padding(0, 0, 0, 0);

    private final int top;
    private final int right;
    private final int bottom;
    private final int left;

    public Padding(int top, int right, int bottom, int left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    public static Padding uniform(int value) {
        return new Padding(value, value, value, value);
    }

    public static Padding symmetric(int vertical, int horizontal) {
        return new Padding(vertical, horizontal, vertical, horizontal);
    }

    public static Padding horizontal(int value) {
        return new Padding(0, value, 0, value);
    }

    public static Padding vertical(int value) {
        return new Padding(value, 0, value, 0);
    }

    public int horizontalTotal() {
        return left + right;
    }

    public int verticalTotal() {
        return top + bottom;
    }

    public int top() {
        return top;
    }

    public int right() {
        return right;
    }

    public int bottom() {
        return bottom;
    }

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

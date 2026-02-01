/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

/**
 * Immutable value object holding horizontal and vertical gutter spacing
 * for grid layouts.
 */
public final class Gutter {

    private final int horizontal;
    private final int vertical;

    private Gutter(int horizontal, int vertical) {
        if (horizontal < 0) {
            throw new IllegalArgumentException("horizontal gutter cannot be negative: " + horizontal);
        }
        if (vertical < 0) {
            throw new IllegalArgumentException("vertical gutter cannot be negative: " + vertical);
        }
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    /**
     * Creates a gutter with different horizontal and vertical spacing.
     *
     * @param horizontal the horizontal gutter between columns
     * @param vertical   the vertical gutter between rows
     * @return a new gutter
     */
    public static Gutter of(int horizontal, int vertical) {
        return new Gutter(horizontal, vertical);
    }

    /**
     * Creates a gutter with uniform spacing in both directions.
     *
     * @param value the gutter value for both horizontal and vertical
     * @return a new uniform gutter
     */
    public static Gutter uniform(int value) {
        return new Gutter(value, value);
    }

    /**
     * Returns the horizontal gutter (between columns).
     *
     * @return the horizontal gutter
     */
    public int horizontal() {
        return horizontal;
    }

    /**
     * Returns the vertical gutter (between rows).
     *
     * @return the vertical gutter
     */
    public int vertical() {
        return vertical;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Gutter)) {
            return false;
        }
        Gutter gutter = (Gutter) o;
        return horizontal == gutter.horizontal && vertical == gutter.vertical;
    }

    @Override
    public int hashCode() {
        int result = horizontal;
        result = 31 * result + vertical;
        return result;
    }

    @Override
    public String toString() {
        return "Gutter[horizontal=" + horizontal + ", vertical=" + vertical + "]";
    }
}

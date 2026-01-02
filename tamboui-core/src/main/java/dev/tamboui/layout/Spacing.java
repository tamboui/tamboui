/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

/**
 * Represents the spacing between segments in a layout.
 * <p>
 * Can represent either positive spacing (space between segments) or negative spacing
 * (overlap between segments).
 */
public abstract class Spacing {

    /**
     * Creates spacing from an integer value.
     * Negative values create overlap, non-negative values create space.
     */
    public static Spacing from(int value) {
        if (value < 0) {
            return new Overlap(-value);
        } else {
            return new Space(value);
        }
    }

    /**
     * Creates spacing with a fixed number of cells between segments.
     */
    public static Spacing space(int value) {
        return new Space(value);
    }

    /**
     * Creates overlap with a fixed number of overlapping cells.
     */
    public static Spacing overlap(int value) {
        return new Overlap(value);
    }

    /**
     * Returns the spacing value as an integer.
     * Positive for space, negative for overlap.
     */
    public abstract int value();

    /**
     * Represents positive spacing between segments.
     */
    public static final class Space extends Spacing {
        private final int value;

        private Space(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("Space value must be non-negative: " + value);
            }
            this.value = value;
        }

        @Override
        public int value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Space)) {
                return false;
            }
            Space space = (Space) o;
            return value == space.value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }

        @Override
        public String toString() {
            return "Spacing.Space(" + value + ")";
        }
    }

    /**
     * Represents negative spacing, causing overlap between segments.
     */
    public static final class Overlap extends Spacing {
        private final int value;

        private Overlap(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("Overlap value must be non-negative: " + value);
            }
            this.value = value;
        }

        @Override
        public int value() {
            return -value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Overlap)) {
                return false;
            }
            Overlap overlap = (Overlap) o;
            return value == overlap.value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(-value);
        }

        @Override
        public String toString() {
            return "Spacing.Overlap(" + value + ")";
        }
    }
}


/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

/**
 * Constraints for layout space allocation.
 */
public interface Constraint {

    /**
     * Fixed size in cells.
     */
    final class Length implements Constraint {
        private final int value;

        /**
         * Creates a fixed length constraint.
         *
         * @param value size in cells (>= 0)
         */
        public Length(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("Length cannot be negative: " + value);
            }
            this.value = value;
        }

        public int value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Length)) {
                return false;
            }
            Length length = (Length) o;
            return value == length.value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }

        @Override
        public String toString() {
            return String.format("Length[value=%d]", value);
        }
    }

    /**
     * Percentage of available space (0-100).
     */
    final class Percentage implements Constraint {
        private final int value;

        /**
         * Creates a percentage constraint.
         *
         * @param value percentage 0-100
         */
        public Percentage(int value) {
            if (value < 0 || value > 100) {
                throw new IllegalArgumentException("Percentage must be between 0 and 100: " + value);
            }
            this.value = value;
        }

        public int value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Percentage)) {
                return false;
            }
            Percentage that = (Percentage) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }

        @Override
        public String toString() {
            return String.format("Percentage[value=%d]", value);
        }
    }

    /**
     * Ratio of available space (numerator/denominator).
     */
    final class Ratio implements Constraint {
        private final int numerator;
        private final int denominator;

        /**
         * Creates a ratio constraint.
         *
         * @param numerator   numerator (>=0)
         * @param denominator denominator (>0)
         */
        public Ratio(int numerator, int denominator) {
            if (denominator <= 0) {
                throw new IllegalArgumentException("Denominator must be positive: " + denominator);
            }
            if (numerator < 0) {
                throw new IllegalArgumentException("Numerator cannot be negative: " + numerator);
            }
            this.numerator = numerator;
            this.denominator = denominator;
        }

        public int numerator() {
            return numerator;
        }

        public int denominator() {
            return denominator;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Ratio)) {
                return false;
            }
            Ratio ratio = (Ratio) o;
            return numerator == ratio.numerator && denominator == ratio.denominator;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(numerator);
            result = 31 * result + Integer.hashCode(denominator);
            return result;
        }

        @Override
        public String toString() {
            return String.format("Ratio[numerator=%d, denominator=%d]", numerator, denominator);
        }
    }

    /**
     * Minimum size in cells.
     */
    final class Min implements Constraint {
        private final int value;

        /**
         * Creates a minimum size constraint.
         *
         * @param value minimum size (>=0)
         */
        public Min(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("Min cannot be negative: " + value);
            }
            this.value = value;
        }

        public int value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Min)) {
                return false;
            }
            Min min = (Min) o;
            return value == min.value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }

        @Override
        public String toString() {
            return String.format("Min[value=%d]", value);
        }
    }

    /**
     * Maximum size in cells.
     */
    final class Max implements Constraint {
        private final int value;

        /**
         * Creates a maximum size constraint.
         *
         * @param value maximum size (>=0)
         */
        public Max(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("Max cannot be negative: " + value);
            }
            this.value = value;
        }

        public int value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Max)) {
                return false;
            }
            Max max = (Max) o;
            return value == max.value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }

        @Override
        public String toString() {
            return String.format("Max[value=%d]", value);
        }
    }

    /**
     * Fill remaining space with given weight.
     */
    final class Fill implements Constraint {
        private final int weight;

        /**
         * Creates a fill constraint.
         *
         * @param weight weight for distributing remaining space (>=0)
         */
        public Fill(int weight) {
            if (weight < 0) {
                throw new IllegalArgumentException("Fill weight must be non-negative: " + weight);
            }
            this.weight = weight;
        }

        /**
         * Creates a fill constraint with weight 1.
         */
        public Fill() {
            this(1);
        }

        public int weight() {
            return weight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Fill)) {
                return false;
            }
            Fill fill = (Fill) o;
            return weight == fill.weight;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(weight);
        }

        @Override
        public String toString() {
            return String.format("Fill[weight=%d]", weight);
        }
    }

    // Convenience factory methods
    /**
     * Creates a fixed length constraint.
     *
     * @param value length in cells
     * @return length constraint
     */
    static Constraint length(int value) {
        return new Length(value);
    }

    /**
     * Creates a percentage constraint.
     *
     * @param value percentage 0-100
     * @return percentage constraint
     */
    static Constraint percentage(int value) {
        return new Percentage(value);
    }

    /**
     * Creates a ratio constraint.
     *
     * @param numerator   numerator
     * @param denominator denominator
     * @return ratio constraint
     */
    static Constraint ratio(int numerator, int denominator) {
        return new Ratio(numerator, denominator);
    }

    /**
     * Creates a minimum size constraint.
     *
     * @param value minimum size
     * @return min constraint
     */
    static Constraint min(int value) {
        return new Min(value);
    }

    /**
     * Creates a maximum size constraint.
     *
     * @param value maximum size
     * @return max constraint
     */
    static Constraint max(int value) {
        return new Max(value);
    }

    /**
     * Creates a fill constraint with a custom weight.
     *
     * @param weight fill weight
     * @return fill constraint
     */
    static Constraint fill(int weight) {
        return new Fill(weight);
    }

    /**
     * Creates a fill constraint with weight 1.
     *
     * @return fill constraint
     */
    static Constraint fill() {
        return new Fill(1);
    }
}

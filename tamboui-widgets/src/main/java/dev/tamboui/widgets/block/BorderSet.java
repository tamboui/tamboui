/*
 * Copyright TamboUI Contributors
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
     * Each character is sanitized to be at most a single character (or empty).
     * Multi-character strings are truncated to the first character.
     *
     * @param topHorizontal the top horizontal border character
     * @param bottomHorizontal the bottom horizontal border character
     * @param leftVertical the left vertical border character
     * @param rightVertical the right vertical border character
     * @param topLeft the top-left corner character
     * @param topRight the top-right corner character
     * @param bottomLeft the bottom-left corner character
     * @param bottomRight the bottom-right corner character
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
        this.topHorizontal = sanitize(topHorizontal);
        this.bottomHorizontal = sanitize(bottomHorizontal);
        this.leftVertical = sanitize(leftVertical);
        this.rightVertical = sanitize(rightVertical);
        this.topLeft = sanitize(topLeft);
        this.topRight = sanitize(topRight);
        this.bottomLeft = sanitize(bottomLeft);
        this.bottomRight = sanitize(bottomRight);
    }

    /**
     * Sanitizes a border character to be at most a single character.
     * Empty strings are preserved (they indicate "don't render").
     * Multi-character strings are truncated to the first character.
     */
    private static String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        // Take only the first character (handles multi-char inputs like "<<")
        return value.substring(0, 1);
    }

    /**
     * Returns the top horizontal border character.
     *
     * @return the top horizontal character
     */
    public String topHorizontal() {
        return topHorizontal;
    }

    /**
     * Returns the bottom horizontal border character.
     *
     * @return the bottom horizontal character
     */
    public String bottomHorizontal() {
        return bottomHorizontal;
    }

    /**
     * Returns the left vertical border character.
     *
     * @return the left vertical character
     */
    public String leftVertical() {
        return leftVertical;
    }

    /**
     * Returns the right vertical border character.
     *
     * @return the right vertical character
     */
    public String rightVertical() {
        return rightVertical;
    }

    /**
     * Returns the top-left corner character.
     *
     * @return the top-left corner character
     */
    public String topLeft() {
        return topLeft;
    }

    /**
     * Returns the top-right corner character.
     *
     * @return the top-right corner character
     */
    public String topRight() {
        return topRight;
    }

    /**
     * Returns the bottom-left corner character.
     *
     * @return the bottom-left corner character
     */
    public String bottomLeft() {
        return bottomLeft;
    }

    /**
     * Returns the bottom-right corner character.
     *
     * @return the bottom-right corner character
     */
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

    /**
     * Creates a new builder for BorderSet.
     * All characters default to empty strings.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for BorderSet.
     * All characters default to empty strings, allowing partial border definitions.
     */
    public static final class Builder {
        private String topHorizontal = "";
        private String bottomHorizontal = "";
        private String leftVertical = "";
        private String rightVertical = "";
        private String topLeft = "";
        private String topRight = "";
        private String bottomLeft = "";
        private String bottomRight = "";

        private Builder() {
        }

        /**
         * Sets the top horizontal border character.
         *
         * @param c the character
         * @return this builder
         */
        public Builder topHorizontal(String c) {
            this.topHorizontal = c;
            return this;
        }

        /**
         * Sets the bottom horizontal border character.
         *
         * @param c the character
         * @return this builder
         */
        public Builder bottomHorizontal(String c) {
            this.bottomHorizontal = c;
            return this;
        }

        /**
         * Sets both horizontal border characters (top and bottom).
         *
         * @param c the character
         * @return this builder
         */
        public Builder horizontal(String c) {
            this.topHorizontal = c;
            this.bottomHorizontal = c;
            return this;
        }

        /**
         * Sets the left vertical border character.
         *
         * @param c the character
         * @return this builder
         */
        public Builder leftVertical(String c) {
            this.leftVertical = c;
            return this;
        }

        /**
         * Sets the right vertical border character.
         *
         * @param c the character
         * @return this builder
         */
        public Builder rightVertical(String c) {
            this.rightVertical = c;
            return this;
        }

        /**
         * Sets both vertical border characters (left and right).
         *
         * @param c the character
         * @return this builder
         */
        public Builder vertical(String c) {
            this.leftVertical = c;
            this.rightVertical = c;
            return this;
        }

        /**
         * Sets the top-left corner character.
         *
         * @param c the character
         * @return this builder
         */
        public Builder topLeft(String c) {
            this.topLeft = c;
            return this;
        }

        /**
         * Sets the top-right corner character.
         *
         * @param c the character
         * @return this builder
         */
        public Builder topRight(String c) {
            this.topRight = c;
            return this;
        }

        /**
         * Sets the bottom-left corner character.
         *
         * @param c the character
         * @return this builder
         */
        public Builder bottomLeft(String c) {
            this.bottomLeft = c;
            return this;
        }

        /**
         * Sets the bottom-right corner character.
         *
         * @param c the character
         * @return this builder
         */
        public Builder bottomRight(String c) {
            this.bottomRight = c;
            return this;
        }

        /**
         * Builds the BorderSet.
         *
         * @return the configured BorderSet
         */
        public BorderSet build() {
            return new BorderSet(
                topHorizontal, bottomHorizontal,
                leftVertical, rightVertical,
                topLeft, topRight, bottomLeft, bottomRight
            );
        }
    }
}

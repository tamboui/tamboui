/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.ratty;

import java.util.Objects;

/**
 * Options for placing a 3D object in terminal cell space.
 * <p>
 * Supports positioning, sizing, animation, transforms, and styling.
 *
 * @see dev.tamboui.ratty.protocol.RattyProtocol#place(int, PlaceOptions)
 */
public final class PlaceOptions {

    private final int row;
    private final int col;
    private final int width;
    private final int height;
    private final Boolean animate;
    private final Float scale;
    private final Float depth;
    private final String color;
    private final Float brightness;
    private final Float px;
    private final Float py;
    private final Float pz;
    private final Float rx;
    private final Float ry;
    private final Float rz;
    private final Float sx;
    private final Float sy;
    private final Float sz;

    private PlaceOptions(Builder builder) {
        this.row = builder.row;
        this.col = builder.col;
        this.width = builder.width;
        this.height = builder.height;
        this.animate = builder.animate;
        this.scale = builder.scale;
        this.depth = builder.depth;
        this.color = builder.color;
        this.brightness = builder.brightness;
        this.px = builder.px;
        this.py = builder.py;
        this.pz = builder.pz;
        this.rx = builder.rx;
        this.ry = builder.ry;
        this.rz = builder.rz;
        this.sx = builder.sx;
        this.sy = builder.sy;
        this.sz = builder.sz;
    }

    /**
     * Creates a new builder with required position and size.
     *
     * @param row    anchor row (center of placement)
     * @param col    anchor column (center of placement)
     * @param width  width in terminal cells
     * @param height height in terminal cells
     * @return a new builder
     */
    public static Builder builder(int row, int col, int width, int height) {
        return new Builder(row, col, width, height);
    }

    /**
     * Returns the anchor row.
     *
     * @return the row
     */
    public int row() {
        return row;
    }

    /**
     * Returns the anchor column.
     *
     * @return the column
     */
    public int col() {
        return col;
    }

    /**
     * Returns the width in cells.
     *
     * @return the width
     */
    public int width() {
        return width;
    }

    /**
     * Returns the height in cells.
     *
     * @return the height
     */
    public int height() {
        return height;
    }

    /**
     * Returns whether animation is enabled.
     *
     * @return true if animated, null if not specified
     */
    public Boolean animate() {
        return animate;
    }

    /**
     * Returns the scale factor.
     *
     * @return the scale, or null if not specified
     */
    public Float scale() {
        return scale;
    }

    /**
     * Returns the depth (z-offset).
     *
     * @return the depth, or null if not specified
     */
    public Float depth() {
        return depth;
    }

    /**
     * Returns the color as RRGGBB hex string.
     *
     * @return the color, or null if not specified
     */
    public String color() {
        return color;
    }

    /**
     * Returns the brightness multiplier.
     *
     * @return the brightness, or null if not specified
     */
    public Float brightness() {
        return brightness;
    }

    /**
     * Returns the X translation offset.
     *
     * @return px, or null if not specified
     */
    public Float px() {
        return px;
    }

    /**
     * Returns the Y translation offset.
     *
     * @return py, or null if not specified
     */
    public Float py() {
        return py;
    }

    /**
     * Returns the Z translation offset.
     *
     * @return pz, or null if not specified
     */
    public Float pz() {
        return pz;
    }

    /**
     * Returns the X rotation in degrees.
     *
     * @return rx, or null if not specified
     */
    public Float rx() {
        return rx;
    }

    /**
     * Returns the Y rotation in degrees.
     *
     * @return ry, or null if not specified
     */
    public Float ry() {
        return ry;
    }

    /**
     * Returns the Z rotation in degrees.
     *
     * @return rz, or null if not specified
     */
    public Float rz() {
        return rz;
    }

    /**
     * Returns the X scale factor.
     *
     * @return sx, or null if not specified
     */
    public Float sx() {
        return sx;
    }

    /**
     * Returns the Y scale factor.
     *
     * @return sy, or null if not specified
     */
    public Float sy() {
        return sy;
    }

    /**
     * Returns the Z scale factor.
     *
     * @return sz, or null if not specified
     */
    public Float sz() {
        return sz;
    }

    /**
     * Builder for {@link PlaceOptions}.
     */
    public static final class Builder {
        private final int row;
        private final int col;
        private final int width;
        private final int height;
        private Boolean animate;
        private Float scale;
        private Float depth;
        private String color;
        private Float brightness;
        private Float px;
        private Float py;
        private Float pz;
        private Float rx;
        private Float ry;
        private Float rz;
        private Float sx;
        private Float sy;
        private Float sz;

        private Builder(int row, int col, int width, int height) {
            this.row = row;
            this.col = col;
            this.width = width;
            this.height = height;
        }

        /**
         * Enables or disables default animation.
         *
         * @param animate true to enable animation
         * @return this builder
         */
        public Builder animate(boolean animate) {
            this.animate = animate;
            return this;
        }

        /**
         * Sets the uniform scale factor.
         *
         * @param scale the scale factor
         * @return this builder
         */
        public Builder scale(float scale) {
            this.scale = scale;
            return this;
        }

        /**
         * Sets the depth (z-offset).
         *
         * @param depth the depth
         * @return this builder
         */
        public Builder depth(float depth) {
            this.depth = depth;
            return this;
        }

        /**
         * Sets the color tint as RGB hex (e.g., "ff8844").
         *
         * @param color the color hex string (without '#')
         * @return this builder
         */
        public Builder color(String color) {
            this.color = Objects.requireNonNull(color, "color");
            return this;
        }

        /**
         * Sets the brightness multiplier.
         *
         * @param brightness the brightness (1.0 = normal)
         * @return this builder
         */
        public Builder brightness(float brightness) {
            this.brightness = brightness;
            return this;
        }

        /**
         * Sets the translation offset.
         *
         * @param px X offset
         * @param py Y offset
         * @param pz Z offset
         * @return this builder
         */
        public Builder translate(float px, float py, float pz) {
            this.px = px;
            this.py = py;
            this.pz = pz;
            return this;
        }

        /**
         * Sets the rotation in degrees.
         *
         * @param rx X rotation
         * @param ry Y rotation
         * @param rz Z rotation
         * @return this builder
         */
        public Builder rotate(float rx, float ry, float rz) {
            this.rx = rx;
            this.ry = ry;
            this.rz = rz;
            return this;
        }

        /**
         * Sets Y-axis rotation in degrees (most common for spinning objects).
         *
         * @param ry Y rotation
         * @return this builder
         */
        public Builder rotateY(float ry) {
            this.ry = ry;
            return this;
        }

        /**
         * Sets non-uniform scale factors.
         *
         * @param sx X scale
         * @param sy Y scale
         * @param sz Z scale
         * @return this builder
         */
        public Builder scaleNonUniform(float sx, float sy, float sz) {
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            return this;
        }

        /**
         * Builds the placement options.
         *
         * @return the options
         */
        public PlaceOptions build() {
            return new PlaceOptions(this);
        }
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Objects;

/**
 * Represents the width of an element.
 * <p>
 * Supports the following values:
 * <ul>
 *   <li>{@code fill} - element fills the available container width</li>
 *   <li>{@code fit} - element width fits its content</li>
 *   <li>Percentage values like {@code 50%} or {@code 0.5}</li>
 *   <li>Fixed character counts like {@code 20}</li>
 * </ul>
 */
public final class Width {

    /**
     * Width that fills the available container space.
     */
    public static final Width FILL = new Width(Type.FILL, 0);

    /**
     * Width that fits the content.
     */
    public static final Width FIT = new Width(Type.FIT, 0);

    private final Type type;
    private final double value;

    private Width(Type type, double value) {
        this.type = type;
        this.value = value;
    }

    /**
     * Creates a percentage-based width.
     *
     * @param percent the percentage (0.0 to 1.0, or 0 to 100)
     * @return a percentage width
     */
    public static Width percent(double percent) {
        // Normalize: if > 1, assume it's 0-100 scale
        double normalized = percent > 1 ? percent / 100.0 : percent;
        return new Width(Type.PERCENT, normalized);
    }

    /**
     * Creates a fixed character width.
     *
     * @param chars the number of characters
     * @return a fixed width
     */
    public static Width fixed(int chars) {
        return new Width(Type.FIXED, chars);
    }

    /**
     * Returns true if this width fills the container.
     *
     * @return true if fill width
     */
    public boolean isFill() {
        return type == Type.FILL;
    }

    /**
     * Returns true if this width fits the content.
     *
     * @return true if fit width
     */
    public boolean isFit() {
        return type == Type.FIT;
    }

    /**
     * Returns true if this width is a percentage.
     *
     * @return true if percentage width
     */
    public boolean isPercent() {
        return type == Type.PERCENT;
    }

    /**
     * Returns true if this width is a fixed character count.
     *
     * @return true if fixed width
     */
    public boolean isFixed() {
        return type == Type.FIXED;
    }

    /**
     * Returns the percentage value (0.0 to 1.0).
     * Only meaningful if {@link #isPercent()} returns true.
     *
     * @return the percentage value
     */
    public double percent() {
        return value;
    }

    /**
     * Returns the fixed character count.
     * Only meaningful if {@link #isFixed()} returns true.
     *
     * @return the character count
     */
    public int chars() {
        return (int) value;
    }

    /**
     * Computes the actual width in characters given a container width.
     *
     * @param containerWidth the available container width
     * @param contentWidth the content width (used for FIT)
     * @return the computed width in characters
     */
    public int compute(int containerWidth, int contentWidth) {
        switch (type) {
            case FILL:
                return containerWidth;
            case FIT:
                return contentWidth;
            case PERCENT:
                return (int) Math.round(containerWidth * value);
            case FIXED:
                return (int) value;
            default:
                return contentWidth;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Width width = (Width) o;
        return Double.compare(width.value, value) == 0 && type == width.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        switch (type) {
            case FILL:
                return "fill";
            case FIT:
                return "fit";
            case PERCENT:
                return (int) (value * 100) + "%";
            case FIXED:
                return (int) value + "ch";
            default:
                return "unknown";
        }
    }

    private enum Type {
        FILL,
        FIT,
        PERCENT,
        FIXED
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas.shapes;

import dev.tamboui.style.Color;
import dev.tamboui.widgets.canvas.Painter;
import dev.tamboui.widgets.canvas.Shape;

/**
 * A rectangle defined by position and size.
 * <p>
 * The rectangle is positioned from its bottom-left corner in
 * canvas coordinate space (mathematical coordinates).
 *
 * <pre>{@code
 * // Rectangle at (10, 20) with width 30 and height 15
 * context.draw(new Rectangle(10, 20, 30, 15, Color.BLUE));
 * }</pre>
 *
 * @see Shape
 */
public final class Rectangle implements Shape {

    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final Color color;

    /**
     * Creates a rectangle at the given position with the given dimensions and color.
     *
     * @param x      the x coordinate of the bottom-left corner
     * @param y      the y coordinate of the bottom-left corner
     * @param width  the rectangle width
     * @param height the rectangle height
     * @param color  the rectangle color
     */
    public Rectangle(double x, double y, double width, double height, Color color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
    }

    /**
     * Creates a rectangle at (x, y) with the given dimensions and color.
     *
     * @param x      the x coordinate of the bottom-left corner
     * @param y      the y coordinate of the bottom-left corner
     * @param width  the rectangle width
     * @param height the rectangle height
     * @param color  the rectangle color
     * @return a new Rectangle
     */
    public static Rectangle of(double x, double y, double width, double height, Color color) {
        return new Rectangle(x, y, width, height, color);
    }

    @Override
    public void draw(Painter painter) {
        double right = x + width;
        double top = y + height;

        // Bottom, top, left and right edges
        Line.drawSegment(painter, x, y, right, y, color);
        Line.drawSegment(painter, x, top, right, top, color);
        Line.drawSegment(painter, x, y, x, top, color);
        Line.drawSegment(painter, right, y, right, top, color);
    }

    /**
     * Returns the x coordinate.
     *
     * @return the x coordinate
     */
    public double x() {
        return x;
    }

    /**
     * Returns the y coordinate.
     *
     * @return the y coordinate
     */
    public double y() {
        return y;
    }

    /**
     * Returns the width.
     *
     * @return the width
     */
    public double width() {
        return width;
    }

    /**
     * Returns the height.
     *
     * @return the height
     */
    public double height() {
        return height;
    }

    /**
     * Returns the color.
     *
     * @return the color
     */
    public Color color() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rectangle)) {
            return false;
        }
        Rectangle rectangle = (Rectangle) o;
        return Double.compare(rectangle.x, x) == 0
            && Double.compare(rectangle.y, y) == 0
            && Double.compare(rectangle.width, width) == 0
            && Double.compare(rectangle.height, height) == 0
            && color.equals(rectangle.color);
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        result = 31 * result + Double.hashCode(width);
        result = 31 * result + Double.hashCode(height);
        result = 31 * result + color.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format(
            "Rectangle[x=%s, y=%s, width=%s, height=%s, color=%s]",
            x, y, width, height, color);
    }
}

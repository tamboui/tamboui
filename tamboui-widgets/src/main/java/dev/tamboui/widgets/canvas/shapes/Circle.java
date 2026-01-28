/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas.shapes;

import dev.tamboui.style.Color;
import dev.tamboui.widgets.canvas.Painter;
import dev.tamboui.widgets.canvas.Shape;

/**
 * A circle defined by center and radius.
 *
 * <pre>{@code
 * // Circle centered at (50, 50) with radius 20
 * context.draw(new Circle(50, 50, 20, Color.GREEN));
 * }</pre>
 *
 * @see Shape
 */
public final class Circle implements Shape {

    private final double x;
    private final double y;
    private final double radius;
    private final Color color;

    /**
     * Creates a circle centered at (x, y) with the given radius and color.
     *
     * @param x      the x coordinate of the center
     * @param y      the y coordinate of the center
     * @param radius the radius of the circle
     * @param color  the color to draw with
     */
    public Circle(double x, double y, double radius, Color color) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = color;
    }

    /**
     * Creates a circle centered at (x, y) with the given radius and color.
     *
     * @param x      the x coordinate of the center
     * @param y      the y coordinate of the center
     * @param radius the radius of the circle
     * @param color  the color to draw with
     * @return a new circle
     */
    public static Circle of(double x, double y, double radius, Color color) {
        return new Circle(x, y, radius, color);
    }

    @Override
    public void draw(Painter painter) {
        if (radius <= 0) {
            return;
        }

        // Draw circle using Midpoint circle algorithm (Bresenham's for circles)
        // Sample points around the circumference
        double step = 0.5 / radius;  // Smaller step for larger circles
        for (double angle = 0; angle < 2 * Math.PI; angle += step) {
            double px = x + radius * Math.cos(angle);
            double py = y + radius * Math.sin(angle);
            painter.getPoint(px, py).ifPresent(p ->
                painter.paint(p.x(), p.y(), color));
        }

        // Ensure we close the circle by drawing the final point
        painter.getPoint(x + radius, y).ifPresent(p ->
            painter.paint(p.x(), p.y(), color));
    }

    /**
     * Returns the x coordinate of the center.
     *
     * @return the x coordinate
     */
    public double x() {
        return x;
    }

    /**
     * Returns the y coordinate of the center.
     *
     * @return the y coordinate
     */
    public double y() {
        return y;
    }

    /**
     * Returns the radius of the circle.
     *
     * @return the radius
     */
    public double radius() {
        return radius;
    }

    /**
     * Returns the color of the circle.
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
        if (!(o instanceof Circle)) {
            return false;
        }
        Circle circle = (Circle) o;
        return Double.compare(circle.x, x) == 0
            && Double.compare(circle.y, y) == 0
            && Double.compare(circle.radius, radius) == 0
            && color.equals(circle.color);
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(x);
        result = 31 * result + Double.hashCode(y);
        result = 31 * result + Double.hashCode(radius);
        result = 31 * result + color.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("Circle[x=%s, y=%s, radius=%s, color=%s]", x, y, radius, color);
    }
}

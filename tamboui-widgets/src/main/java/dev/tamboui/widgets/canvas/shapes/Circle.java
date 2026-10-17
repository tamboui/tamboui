/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas.shapes;

import java.util.Arrays;
import java.util.Optional;

import dev.tamboui.style.Color;
import dev.tamboui.widgets.canvas.Painter;
import dev.tamboui.widgets.canvas.Shape;

/**
 * A circle defined by center and radius.
 * <p>
 * The circle is drawn one grid point at a time, however large or small it is
 * on the canvas. Only its part inside the canvas bounds is visited.
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
        if (!(radius > 0) || !Double.isFinite(radius) || !Double.isFinite(x) || !Double.isFinite(y)) {
            return;
        }
        double[] xBounds = painter.xBounds();
        double[] yBounds = painter.yBounds();
        double xRange = xBounds[1] - xBounds[0];
        double yRange = yBounds[1] - yBounds[0];
        if (!(xRange > 0) || !(yRange > 0)) {
            return;
        }
        // The radius in grid points along each axis
        double radiusX = radius / xRange * (painter.gridWidth() - 1);
        double radiusY = radius / yRange * (painter.gridHeight() - 1);
        if (Double.isInfinite(radiusX) || Double.isInfinite(radiusY)) {
            return;
        }

        // Cut the circle at its quarters, and where it crosses an edge of the bounds: each arc
        // is then inside or outside the bounds, and only the arcs inside are drawn
        double[] angles = new double[13];
        int count = 0;
        for (int quarter = 0; quarter <= 4; quarter++) {
            angles[count++] = quarter * Math.PI / 2;
        }
        count = addCrossings(angles, count, (xBounds[0] - x) / radius, true);
        count = addCrossings(angles, count, (xBounds[1] - x) / radius, true);
        count = addCrossings(angles, count, (yBounds[0] - y) / radius, false);
        count = addCrossings(angles, count, (yBounds[1] - y) / radius, false);
        Arrays.sort(angles, 0, count);

        for (int i = 0; i + 1 < count; i++) {
            double from = angles[i];
            double to = angles[i + 1];
            if (to > from && pointAt(painter, (from + to) / 2).isPresent()) {
                drawArc(painter, from, to, radiusX, radiusY);
            }
        }
    }

    /**
     * Paints an arc within a quarter of the circle, with samples at most half a grid point
     * apart so that they land on the same or neighboring grid points. Within a quarter, the
     * arc moves fastest on the grid at one of its ends.
     */
    private void drawArc(Painter painter, double from, double to, double radiusX, double radiusY) {
        double speed = Math.max(Math.hypot(radiusX * Math.sin(from), radiusY * Math.cos(from)),
            Math.hypot(radiusX * Math.sin(to), radiusY * Math.cos(to)));
        int steps = (int) Math.max(1, Math.ceil(2 * speed * (to - from)));
        for (int k = 0; k < steps; k++) {
            paintAt(painter, from + (to - from) * k / steps);
        }
        paintAt(painter, to);
    }

    private void paintAt(Painter painter, double angle) {
        pointAt(painter, angle).ifPresent(p -> painter.paint(p.x(), p.y(), color));
    }

    private Optional<Painter.GridPoint> pointAt(Painter painter, double angle) {
        return painter.getPoint(x + radius * Math.cos(angle), y + radius * Math.sin(angle));
    }

    /**
     * Adds the two angles, in [0, 2&pi;], where the circle crosses the line
     * {@code offset * radius} away from its center: a vertical line, or a horizontal one.
     */
    private static int addCrossings(double[] angles, int count, double offset, boolean vertical) {
        if (!(offset > -1 && offset < 1)) {
            return count;
        }
        if (vertical) {
            double angle = Math.acos(offset);
            angles[count++] = angle;
            angles[count++] = 2 * Math.PI - angle;
        } else {
            double angle = Math.asin(offset);
            angles[count++] = angle < 0 ? angle + 2 * Math.PI : angle;
            angles[count++] = Math.PI - angle;
        }
        return count;
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

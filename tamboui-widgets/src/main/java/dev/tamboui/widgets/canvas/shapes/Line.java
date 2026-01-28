/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas.shapes;

import dev.tamboui.style.Color;
import dev.tamboui.widgets.canvas.Painter;
import dev.tamboui.widgets.canvas.Shape;

import java.util.Optional;

/**
 * A line segment between two points.
 * <p>
 * Draws a line from (x1, y1) to (x2, y2) using Bresenham's algorithm.
 *
 * <pre>{@code
 * context.draw(new Line(0, 0, 10, 10, Color.RED));
 * }</pre>
 *
 * @see Shape
 */
public final class Line implements Shape {

    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;
    private final Color color;

    /**
     * Creates a line segment between two points.
     *
     * @param x1 the x coordinate of the start point
     * @param y1 the y coordinate of the start point
     * @param x2 the x coordinate of the end point
     * @param y2 the y coordinate of the end point
     * @param color the line color
     */
    public Line(double x1, double y1, double x2, double y2, Color color) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.color = color;
    }

    /**
     * Creates a line from (x1, y1) to (x2, y2) with the given color.
     *
     * @param x1 the x coordinate of the start point
     * @param y1 the y coordinate of the start point
     * @param x2 the x coordinate of the end point
     * @param y2 the y coordinate of the end point
     * @param color the line color
     * @return a new line
     */
    public static Line of(double x1, double y1, double x2, double y2, Color color) {
        return new Line(x1, y1, x2, y2, color);
    }

    @Override
    public void draw(Painter painter) {
        Optional<Painter.GridPoint> p1 = painter.getPoint(x1, y1);
        Optional<Painter.GridPoint> p2 = painter.getPoint(x2, y2);

        if (!p1.isPresent() || !p2.isPresent()) {
            // Fall back to drawing what we can if points are partially visible
            drawWithClipping(painter);
            return;
        }

        drawBresenham(painter, p1.get().x(), p1.get().y(), p2.get().x(), p2.get().y());
    }

    private void drawWithClipping(Painter painter) {
        // Sample points along the line and draw visible ones
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);

        if (length == 0) {
            painter.getPoint(x1, y1).ifPresent(p ->
                painter.paint(p.x(), p.y(), color));
            return;
        }

        // Step size - smaller for longer lines
        double step = 0.5;
        int steps = (int) Math.ceil(length / step);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = x1 + t * dx;
            double y = y1 + t * dy;
            painter.getPoint(x, y).ifPresent(p ->
                painter.paint(p.x(), p.y(), color));
        }
    }

    private void drawBresenham(Painter painter, int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            painter.paint(x0, y0, color);

            if (x0 == x1 && y0 == y1) {
                break;
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    /**
     * Returns the x coordinate of the start point.
     *
     * @return the x1 coordinate
     */
    public double x1() {
        return x1;
    }

    /**
     * Returns the y coordinate of the start point.
     *
     * @return the y1 coordinate
     */
    public double y1() {
        return y1;
    }

    /**
     * Returns the x coordinate of the end point.
     *
     * @return the x2 coordinate
     */
    public double x2() {
        return x2;
    }

    /**
     * Returns the y coordinate of the end point.
     *
     * @return the y2 coordinate
     */
    public double y2() {
        return y2;
    }

    /**
     * Returns the line color.
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
        if (!(o instanceof Line)) {
            return false;
        }
        Line line = (Line) o;
        return Double.compare(line.x1, x1) == 0
            && Double.compare(line.y1, y1) == 0
            && Double.compare(line.x2, x2) == 0
            && Double.compare(line.y2, y2) == 0
            && color.equals(line.color);
    }

    @Override
    public int hashCode() {
        int result = Double.hashCode(x1);
        result = 31 * result + Double.hashCode(y1);
        result = 31 * result + Double.hashCode(x2);
        result = 31 * result + Double.hashCode(y2);
        result = 31 * result + color.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("Line[x1=%s, y1=%s, x2=%s, y2=%s, color=%s]", x1, y1, x2, y2, color);
    }
}

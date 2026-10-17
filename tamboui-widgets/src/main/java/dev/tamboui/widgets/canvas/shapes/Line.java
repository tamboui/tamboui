/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas.shapes;

import java.util.Optional;

import dev.tamboui.style.Color;
import dev.tamboui.widgets.canvas.Painter;
import dev.tamboui.widgets.canvas.Shape;

/**
 * A line segment between two points.
 * <p>
 * Draws a line from (x1, y1) to (x2, y2) using Bresenham's algorithm, one grid
 * point per step. The part of the line outside the canvas bounds is cut off.
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
        drawSegment(painter, x1, y1, x2, y2, color);
    }

    /**
     * Draws the part of the segment from (x1, y1) to (x2, y2) that is inside the canvas
     * bounds, one grid point per step. A segment with a NaN or infinite coordinate is not drawn.
     */
    static void drawSegment(Painter painter, double x1, double y1, double x2, double y2, Color color) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (!Double.isFinite(x1) || !Double.isFinite(y1) || !Double.isFinite(dx) || !Double.isFinite(dy)) {
            return;
        }
        double[] xBounds = painter.xBounds();
        double[] yBounds = painter.yBounds();
        // Liang-Barsky: narrow the segment, from t = 0 at (x1, y1) to t = 1 at (x2, y2),
        // to its part on the inner side of each edge of the bounds
        double[] range = {0, 1};
        if (!clipEdge(-dx, x1 - xBounds[0], range) || !clipEdge(dx, xBounds[1] - x1, range)
                || !clipEdge(-dy, y1 - yBounds[0], range) || !clipEdge(dy, yBounds[1] - y1, range)) {
            return;
        }
        // An unclipped end keeps its exact coordinates; a clipped one is kept inside the bounds
        // despite rounding, so that getPoint maps it to the edge of the grid
        Optional<Painter.GridPoint> start = painter.getPoint(
            clamp(x1 + range[0] * dx, xBounds), clamp(y1 + range[0] * dy, yBounds));
        Optional<Painter.GridPoint> end = painter.getPoint(
            clamp(x2 - (1 - range[1]) * dx, xBounds), clamp(y2 - (1 - range[1]) * dy, yBounds));
        if (start.isPresent() && end.isPresent()) {
            drawBresenham(painter, start.get().x(), start.get().y(), end.get().x(), end.get().y(), color);
        }
    }

    private static boolean clipEdge(double p, double q, double[] range) {
        if (p == 0) {
            // Parallel to the edge: all inside or all outside
            return q >= 0;
        }
        double t = q / p;
        if (p < 0) {
            range[0] = Math.max(range[0], t);
        } else {
            range[1] = Math.min(range[1], t);
        }
        return range[0] <= range[1];
    }

    private static double clamp(double value, double[] bounds) {
        return Math.max(bounds[0], Math.min(bounds[1], value));
    }

    private static void drawBresenham(Painter painter, int x0, int y0, int x1, int y1, Color color) {
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

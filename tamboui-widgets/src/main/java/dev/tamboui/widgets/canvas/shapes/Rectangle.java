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
        // Draw four edges using lines
        double x1 = x;
        double y1 = y;
        double x2 = x + width;
        double y2 = y + height;

        // Bottom edge
        drawLine(painter, x1, y1, x2, y1);
        // Top edge
        drawLine(painter, x1, y2, x2, y2);
        // Left edge
        drawLine(painter, x1, y1, x1, y2);
        // Right edge
        drawLine(painter, x2, y1, x2, y2);
    }

    private void drawLine(Painter painter, double x1, double y1, double x2, double y2) {
        Optional<Painter.GridPoint> p1 = painter.getPoint(x1, y1);
        Optional<Painter.GridPoint> p2 = painter.getPoint(x2, y2);

        if (p1.isPresent() && p2.isPresent()) {
            drawBresenham(painter, p1.get().x(), p1.get().y(), p2.get().x(), p2.get().y());
        } else {
            // Sample along line for partial visibility
            double dx = x2 - x1;
            double dy = y2 - y1;
            double length = Math.sqrt(dx * dx + dy * dy);
            if (length == 0) {
                painter.getPoint(x1, y1).ifPresent(p ->
                    painter.paint(p.x(), p.y(), color));
                return;
            }
            int steps = Math.max(1, (int) Math.ceil(length / 0.5));
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                painter.getPoint(x1 + t * dx, y1 + t * dy).ifPresent(p ->
                    painter.paint(p.x(), p.y(), color));
            }
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

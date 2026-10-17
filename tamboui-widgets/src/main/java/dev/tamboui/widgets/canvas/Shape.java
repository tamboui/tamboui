/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas;

/**
 * A shape that can be drawn on a {@link Canvas}.
 * <p>
 * Implement this interface to create custom shapes that can be
 * rendered using the Canvas widget's drawing infrastructure.
 *
 * <pre>{@code
 * public class Cross implements Shape {
 *     private final double x, y, size;
 *     private final Color color;
 *
 *     @Override
 *     public void draw(Painter painter) {
 *         new Line(x - size, y, x + size, y, color).draw(painter);
 *         new Line(x, y - size, x, y + size, color).draw(painter);
 *     }
 * }
 * }</pre>
 * <p>
 * A shape that paints its own points should space them by grid point, from
 * {@link Painter#gridWidth()} and {@link Painter#xBounds()}, rather than by a fixed
 * distance in canvas coordinates: that leaves gaps when the bounds are small, and
 * takes very long when they are large.
 *
 * @see Canvas
 * @see Painter
 */
@FunctionalInterface
public interface Shape {

    /**
     * Draws this shape using the provided painter.
     * <p>
     * The painter provides methods to convert canvas coordinates
     * to grid positions and to paint colored points.
     *
     * @param painter the painter to use for drawing
     */
    void draw(Painter painter);
}

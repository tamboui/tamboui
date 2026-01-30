/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas;

/**
 * A shape that can be drawn on a {@link Canvas}.
 * <p>
 * Implement this interface to create custom shapes that can be rendered using
 * the Canvas widget's drawing infrastructure.
 *
 * <pre>{@code
 * public class Cross implements Shape {
 *     private final double x, y, size;
 *     private final Color color;
 *
 *     @Override
 *     public void draw(Painter painter) {
 *         // Draw horizontal line
 *         for (double dx = -size; dx <= size; dx += 0.5) {
 *             painter.getPoint(x + dx, y).ifPresent(p -> painter.paint(p.x(), p.y(), color));
 *         }
 *         // Draw vertical line
 *         for (double dy = -size; dy <= size; dy += 0.5) {
 *             painter.getPoint(x, y + dy).ifPresent(p -> painter.paint(p.x(), p.y(), color));
 *         }
 *     }
 * }
 * }</pre>
 *
 * @see Canvas
 * @see Painter
 */
@FunctionalInterface
public interface Shape {

    /**
     * Draws this shape using the provided painter.
     * <p>
     * The painter provides methods to convert canvas coordinates to grid positions
     * and to paint colored points.
     *
     * @param painter
     *            the painter to use for drawing
     */
    void draw(Painter painter);
}

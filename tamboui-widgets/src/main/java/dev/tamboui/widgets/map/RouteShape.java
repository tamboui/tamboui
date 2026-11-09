/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.map;

import java.util.Objects;

import dev.tamboui.style.Color;
import dev.tamboui.widgets.canvas.Painter;
import dev.tamboui.widgets.canvas.Shape;

/**
 * A geographic polyline (a route) that composes with other Canvas shapes and layers.
 * <p>
 * Points are longitude/latitude pairs, longitude first, matching the rest of the map API.
 * Consecutive points are joined by straight segments in the equirectangular projection, taking
 * the shorter way around the globe and wrapping across the antimeridian, like {@link CoastlineShape}.
 * Configure the Canvas with the same bounds you want to see; segments are clipped to them.
 *
 * <pre>{@code
 * ctx.draw(new RouteShape(new double[][] {
 *     {10.49, 55.28},   // Pederstrup
 *     {-1.55, 47.22}    // Nantes
 * }, Color.MAGENTA));
 * }</pre>
 */
public final class RouteShape implements Shape {

    private final double[][] points;
    private final Color color;

    /**
     * Creates an immutable route.
     *
     * @param points longitude/latitude pairs in degrees, longitude first
     * @param color  the route color
     */
    public RouteShape(double[][] points, Color color) {
        Objects.requireNonNull(points, "points");
        this.color = Objects.requireNonNull(color, "color");
        this.points = new double[points.length][];
        for (int i = 0; i < points.length; i++) {
            double[] point = points[i];
            if (point == null || point.length != 2
                    || !Double.isFinite(point[0]) || !Double.isFinite(point[1])
                    || point[0] < -180 || point[0] > 180 || point[1] < -90 || point[1] > 90) {
                throw new IllegalArgumentException("Invalid route point at index " + i);
            }
            this.points[i] = new double[] {point[0], point[1]};
        }
    }

    @Override
    public void draw(Painter painter) {
        for (int i = 1; i < points.length; i++) {
            GeoSegments.drawWrapped(painter, points[i - 1][0], points[i - 1][1], points[i][0], points[i][1], color);
        }
    }
}

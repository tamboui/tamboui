/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.map;

import java.util.Arrays;
import java.util.Objects;

import dev.tamboui.style.Color;
import dev.tamboui.widgets.canvas.Painter;
import dev.tamboui.widgets.canvas.Shape;

/**
 * A filled and/or outlined geographic polygon (a region) that composes with other Canvas
 * shapes and layers.
 * <p>
 * The polygon is a ring of longitude/latitude points, longitude first; the last point is
 * joined back to the first automatically. The interior is filled by scanning each grid row,
 * and the border is drawn as a wrapped polyline like {@link RouteShape}. Supply a fill color,
 * an outline color, or both. Configure the Canvas with the bounds you want to see.
 * <p>
 * The polygon must not cross the antimeridian; split it into east and west rings if it does,
 * the same way longitude is unwrapped elsewhere in the map API.
 *
 * <pre>{@code
 * ctx.draw(new RegionShape(new double[][] {
 *     {8, 54}, {16, 54}, {16, 58}, {8, 58}
 * }, Color.rgb(0, 40, 0), Color.GREEN));
 * }</pre>
 */
public final class RegionShape implements Shape {

    private final double[] ring;
    private final Color fillColor;
    private final Color outlineColor;

    /**
     * Creates an immutable region.
     *
     * @param points       longitude/latitude pairs in degrees forming the ring, longitude first
     * @param fillColor    the interior color, or null for no fill
     * @param outlineColor the border color, or null for no outline
     */
    public RegionShape(double[][] points, Color fillColor, Color outlineColor) {
        Objects.requireNonNull(points, "points");
        this.fillColor = fillColor;
        this.outlineColor = outlineColor;
        this.ring = new double[points.length * 2];
        for (int i = 0; i < points.length; i++) {
            double[] point = points[i];
            if (point == null || point.length != 2
                    || !Double.isFinite(point[0]) || !Double.isFinite(point[1])
                    || point[0] < -180 || point[0] > 180 || point[1] < -90 || point[1] > 90) {
                throw new IllegalArgumentException("Invalid region point at index " + i);
            }
            this.ring[i * 2] = point[0];
            this.ring[i * 2 + 1] = point[1];
        }
    }

    @Override
    public void draw(Painter painter) {
        if (fillColor != null && ring.length >= 6) {
            fillRing(painter, ring, fillColor);
        }
        if (outlineColor != null) {
            int n = ring.length / 2;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                GeoSegments.drawWrapped(painter, ring[i * 2], ring[i * 2 + 1],
                        ring[j * 2], ring[j * 2 + 1], outlineColor);
            }
        }
    }

    /**
     * Fills a closed ring of flattened longitude/latitude pairs by scanning each grid row.
     * The ring is closed implicitly from the last point back to the first.
     *
     * @param painter the canvas painter
     * @param ring    flattened longitude/latitude pairs, longitude first
     * @param color   the fill color
     */
    static void fillRing(Painter painter, double[] ring, Color color) {
        double[] xBounds = painter.xBounds();
        double[] yBounds = painter.yBounds();
        int gridWidth = painter.gridWidth();
        int gridHeight = painter.gridHeight();
        double xRange = xBounds[1] - xBounds[0];
        double yRange = yBounds[1] - yBounds[0];
        int n = ring.length / 2;
        if (xRange == 0 || yRange == 0 || gridWidth <= 1 || gridHeight <= 1 || n < 3) {
            return;
        }
        double[] crossings = new double[n];
        for (int gy = 0; gy < gridHeight; gy++) {
            double lat = yBounds[1] - (double) gy / (gridHeight - 1) * yRange;
            int count = 0;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                double y0 = ring[i * 2 + 1];
                double y1 = ring[j * 2 + 1];
                // Half-open edge test so shared vertices are counted once.
                if ((y0 <= lat && y1 > lat) || (y1 <= lat && y0 > lat)) {
                    double t = (lat - y0) / (y1 - y0);
                    crossings[count++] = ring[i * 2] + t * (ring[j * 2] - ring[i * 2]);
                }
            }
            if (count < 2) {
                continue;
            }
            Arrays.sort(crossings, 0, count);
            for (int k = 0; k + 1 < count; k += 2) {
                int gx0 = gridColumn(crossings[k], xBounds[0], xRange, gridWidth);
                int gx1 = gridColumn(crossings[k + 1], xBounds[0], xRange, gridWidth);
                for (int gx = Math.max(0, gx0); gx <= Math.min(gridWidth - 1, gx1); gx++) {
                    painter.paint(gx, gy, color);
                }
            }
        }
    }

    private static int gridColumn(double longitude, double min, double range, int gridWidth) {
        return (int) Math.round((longitude - min) / range * (gridWidth - 1));
    }
}

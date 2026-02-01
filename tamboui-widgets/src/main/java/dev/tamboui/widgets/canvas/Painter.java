/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas;

import dev.tamboui.style.Color;

import java.util.Optional;

/**
 * Provides an interface for drawing on a {@link Canvas} grid.
 * <p>
 * The Painter handles coordinate transformation from canvas space
 * (floating-point coordinates with configurable bounds) to grid space
 * (integer coordinates within the terminal area).
 * <p>
 * Canvas coordinates use a mathematical coordinate system with the
 * origin at the lower-left corner, while grid coordinates use the
 * upper-left corner as origin (matching terminal conventions).
 *
 * @see Shape
 * @see Context
 */
public class Painter {

    private final Context context;

    /**
     * Creates a painter that draws on the given context.
     *
     * @param context the canvas context to draw on
     */
    public Painter(Context context) {
        this.context = context;
    }

    /**
     * Converts canvas coordinates to grid coordinates.
     * <p>
     * The canvas uses a mathematical coordinate system with the origin
     * at the lower-left corner. This method transforms those coordinates
     * to grid positions suitable for terminal rendering.
     *
     * @param x the x coordinate in canvas space
     * @param y the y coordinate in canvas space
     * @return the grid position, or empty if outside canvas bounds
     */
    public Optional<GridPoint> getPoint(double x, double y) {
        double[] xBounds = context.xBounds();
        double[] yBounds = context.yBounds();

        // Check if point is within bounds
        if (x < xBounds[0] || x > xBounds[1] || y < yBounds[0] || y > yBounds[1]) {
            return Optional.empty();
        }

        // Get grid dimensions based on marker resolution
        int gridWidth = context.gridWidth();
        int gridHeight = context.gridHeight();

        // Transform canvas coordinates to grid coordinates
        double xRange = xBounds[1] - xBounds[0];
        double yRange = yBounds[1] - yBounds[0];

        if (xRange == 0 || yRange == 0) {
            return Optional.empty();
        }

        int gridX = (int) Math.round((x - xBounds[0]) / xRange * (gridWidth - 1));
        // Flip y-axis (canvas: bottom-left origin, grid: top-left origin)
        int gridY = (int) Math.round((yBounds[1] - y) / yRange * (gridHeight - 1));

        // Clamp to valid range
        gridX = Math.max(0, Math.min(gridWidth - 1, gridX));
        gridY = Math.max(0, Math.min(gridHeight - 1, gridY));

        return Optional.of(new GridPoint(gridX, gridY));
    }

    /**
     * Paints a colored point at the specified grid coordinates.
     *
     * @param x     the x grid coordinate
     * @param y     the y grid coordinate
     * @param color the color to paint
     */
    public void paint(int x, int y, Color color) {
        context.paint(x, y, color);
    }

    /**
     * A point in grid space (integer coordinates).
     */
    public static final class GridPoint {
        private final int x;
        private final int y;

        /**
         * Creates a new grid point.
         *
         * @param x the x coordinate
         * @param y the y coordinate
         */
        public GridPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        /**
         * Returns the x coordinate.
         *
         * @return the x coordinate
         */
        public int x() {
            return x;
        }

        /**
         * Returns the y coordinate.
         *
         * @return the y coordinate
         */
        public int y() {
            return y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof GridPoint)) {
                return false;
            }
            GridPoint gridPoint = (GridPoint) o;
            return x == gridPoint.x && y == gridPoint.y;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(x);
            result = 31 * result + Integer.hashCode(y);
            return result;
        }

        @Override
        public String toString() {
            return String.format("GridPoint[x=%d, y=%d]", x, y);
        }
    }
}

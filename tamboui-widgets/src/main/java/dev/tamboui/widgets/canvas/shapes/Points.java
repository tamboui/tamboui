/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.canvas.shapes;

import dev.tamboui.style.Color;
import dev.tamboui.widgets.canvas.Painter;
import dev.tamboui.widgets.canvas.Shape;

/**
 * A collection of points (scatter plot).
 *
 * <pre>{@code
 * double[][] coords = {{10, 20}, {30, 40}, {50, 60}};
 * context.draw(new Points(coords, Color.YELLOW));
 * }</pre>
 *
 * @see Shape
 */
public final class Points implements Shape {

    private final double[][] coords;
    private final Color color;

    /**
     * Creates a points shape from coordinate pairs and a color.
     *
     * @param coords the coordinate pairs (each sub-array is [x, y])
     * @param color  the color for all points
     */
    public Points(double[][] coords, Color color) {
        this.coords = coords;
        this.color = color;
    }

    /**
     * Creates a points shape from coordinate pairs and a color.
     *
     * @param coords the coordinate pairs (each sub-array is [x, y])
     * @param color  the color for all points
     * @return a new Points shape
     */
    public static Points of(double[][] coords, Color color) {
        return new Points(coords, color);
    }

    /**
     * Creates a points shape from x and y arrays.
     *
     * @param x     array of x coordinates
     * @param y     array of y coordinates
     * @param color the color for all points
     * @return a new Points shape
     * @throws IllegalArgumentException if arrays have different lengths
     */
    public static Points of(double[] x, double[] y, Color color) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("x and y arrays must have the same length");
        }
        double[][] coords = new double[x.length][2];
        for (int i = 0; i < x.length; i++) {
            coords[i][0] = x[i];
            coords[i][1] = y[i];
        }
        return new Points(coords, color);
    }

    @Override
    public void draw(Painter painter) {
        if (coords == null) {
            return;
        }
        for (double[] coord : coords) {
            if (coord != null && coord.length >= 2) {
                painter.getPoint(coord[0], coord[1]).ifPresent(p ->
                    painter.paint(p.x(), p.y(), color));
            }
        }
    }

    /**
     * Returns the coordinate pairs.
     *
     * @return the coordinate pairs
     */
    public double[][] coords() {
        return coords;
    }

    /**
     * Returns the color for all points.
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
        if (!(o instanceof Points)) {
            return false;
        }
        Points points = (Points) o;
        if (color == null ? points.color != null : !color.equals(points.color)) {
            return false;
        }
        if (coords == points.coords) {
            return true;
        }
        if (coords == null || points.coords == null || coords.length != points.coords.length) {
            return false;
        }
        for (int i = 0; i < coords.length; i++) {
            double[] a = coords[i];
            double[] b = points.coords[i];
            if (a == b) {
                continue;
            }
            if (a == null || b == null || a.length != b.length) {
                return false;
            }
            for (int j = 0; j < a.length; j++) {
                if (Double.compare(a[j], b[j]) != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = color != null ? color.hashCode() : 0;
        if (coords != null) {
            for (double[] arr : coords) {
                if (arr != null) {
                    for (double v : arr) {
                        long bits = Double.doubleToLongBits(v);
                        result = 31 * result + (int) (bits ^ (bits >>> 32));
                    }
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("Points[color=%s, count=%d]", color, coords != null ? coords.length : 0);
    }
}

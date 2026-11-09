/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.map;

/**
 * Immutable equirectangular bounds in degrees. East may exceed 180 to cross
 * the antimeridian (for example 170 to 190). Longitude increases rightwards.
 */
public final class MapViewport {
    /** The whole world. */
    public static final MapViewport WORLD = new MapViewport(-180, 180, -90, 90);
    private final double west;
    private final double east;
    private final double south;
    private final double north;

    /**
     * Creates bounds with a longitude span of at most 360 degrees.
     * @param west western longitude in [-180, 180)
     * @param east eastern longitude, greater than west and at most west + 360
     * @param south southern latitude in [-90, 90)
     * @param north northern latitude, greater than south and at most 90
     */
    public MapViewport(double west, double east, double south, double north) {
        if (!Double.isFinite(west) || !Double.isFinite(east)
                || !Double.isFinite(south) || !Double.isFinite(north)
                || west < -180 || west >= 180 || east <= west || east - west > 360
                || south < -90 || north > 90 || north <= south) {
            throw new IllegalArgumentException("Invalid geographic bounds");
        }
        this.west = west;
        this.east = east;
        this.south = south;
        this.north = north;
    }

    /**
     * Returns the western longitude bound.
     *
     * @return western longitude
     */
    public double west() {
        return west;
    }

    /**
     * Returns the eastern longitude bound.
     *
     * @return eastern longitude
     */
    public double east() {
        return east;
    }

    /**
     * Returns the southern latitude bound.
     *
     * @return southern latitude
     */
    public double south() {
        return south;
    }

    /**
     * Returns the northern latitude bound.
     *
     * @return northern latitude
     */
    public double north() {
        return north;
    }

    double unwrap(double longitude) {
        double result = longitude;
        if (result < west) {
            result += 360;
        }
        return result;
    }
}

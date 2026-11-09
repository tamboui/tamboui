/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.map;

import java.util.Objects;

import dev.tamboui.style.Color;

/**
 * An immutable geographic point rendered as a one-cell dot above coastlines, with an
 * optional name drawn beside it.
 */
public final class MapMarker {
    private final double longitude;
    private final double latitude;
    private final Color color;
    private final String label;

    /**
     * Creates an unlabeled marker, using longitude before latitude throughout the map API.
     * @param longitude longitude in [-180, 180]
     * @param latitude latitude in [-90, 90]
     * @param color marker foreground color
     */
    public MapMarker(double longitude, double latitude, Color color) {
        this(longitude, latitude, color, null);
    }

    /**
     * Creates a marker with an optional name drawn beside the dot.
     * @param longitude longitude in [-180, 180]
     * @param latitude latitude in [-90, 90]
     * @param color marker foreground color
     * @param label name drawn beside the marker, or null for none
     */
    public MapMarker(double longitude, double latitude, Color color, String label) {
        if (!Double.isFinite(longitude) || !Double.isFinite(latitude)
                || longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Invalid marker coordinates");
        }
        this.longitude = longitude;
        this.latitude = latitude;
        this.color = Objects.requireNonNull(color, "color");
        this.label = label;
    }

    /**
     * Returns the marker longitude.
     *
     * @return longitude in degrees
     */
    public double longitude() {
        return longitude;
    }

    /**
     * Returns the marker latitude.
     *
     * @return latitude in degrees
     */
    public double latitude() {
        return latitude;
    }

    /**
     * Returns the marker foreground color.
     *
     * @return marker foreground color
     */
    public Color color() {
        return color;
    }

    /**
     * Returns the name drawn beside the marker, if any.
     *
     * @return the label, or null if the marker is unlabeled
     */
    public String label() {
        return label;
    }
}

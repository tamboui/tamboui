/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.map;

import java.util.Objects;

/**
 * Mutable view state for a map: the current {@link MapViewport}.
 * <p>
 * This is the map counterpart of {@code ListState} and {@code TreeState}: the stateless map widget
 * renders it, an interactive element's pan and zoom gestures mutate it, and application code (for
 * example a search or a live-data provider) can mutate it to fly to a result and read it to react to
 * panning. Supply one explicitly to keep the viewport across element rebuilds, or let the element
 * create its own.
 * <p>
 * Mutating methods normalize the bounds: longitude is wrapped into {@code [-180, 180)} (a full
 * 360-degree span may rotate, so east can exceed 180), the span is capped at the whole globe, and
 * latitude is clamped to the poles.
 */
public final class MapState {

    private MapViewport viewport;

    /**
     * Creates state focused on the whole world.
     */
    public MapState() {
        this(MapViewport.WORLD);
    }

    /**
     * Creates state focused on the given viewport.
     *
     * @param initial the initial viewport
     */
    public MapState(MapViewport initial) {
        this.viewport = Objects.requireNonNull(initial, "initial");
    }

    /**
     * Returns the current viewport.
     *
     * @return the current viewport
     */
    public MapViewport viewport() {
        return viewport;
    }

    /**
     * Sets the viewport (for example to fly to a search result).
     *
     * @param viewport the new viewport
     */
    public void viewport(MapViewport viewport) {
        this.viewport = Objects.requireNonNull(viewport, "viewport");
    }

    /**
     * Resets the viewport to the whole world.
     */
    public void reset() {
        this.viewport = MapViewport.WORLD;
    }

    /**
     * Sets the viewport from raw bounds, normalizing longitude, capping the span at the globe and
     * clamping latitude to the poles. Interactive elements compute bounds from gestures and call this.
     *
     * @param west  western longitude in degrees
     * @param east  eastern longitude in degrees
     * @param south southern latitude in degrees
     * @param north northern latitude in degrees
     */
    public void set(double west, double east, double south, double north) {
        double lonSpan = east - west;
        double latSpan = north - south;
        if (lonSpan > 360) {
            // Cap the zoom-out span but keep the center, so a full-world view can still rotate.
            double center = (west + east) / 2;
            west = center - 180;
            east = center + 180;
        }
        // Normalize the western longitude into [-180, 180); a full 360 span may rotate (east > 180).
        while (west < -180) {
            west += 360;
            east += 360;
        }
        while (west >= 180) {
            west -= 360;
            east -= 360;
        }
        if (latSpan >= 180) {
            south = -90;
            north = 90;
        } else if (north > 90) {
            north = 90;
            south = north - latSpan;
        } else if (south < -90) {
            south = -90;
            north = south + latSpan;
        }
        this.viewport = new MapViewport(west, east, south, north);
    }
}

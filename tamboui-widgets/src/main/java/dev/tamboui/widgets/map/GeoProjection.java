/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.map;

import java.util.Objects;

import dev.tamboui.layout.Rect;

/**
 * Immutable equirectangular (plate carrée) projection of a {@link MapViewport} onto a
 * terminal rectangle, correcting for the proportions of a terminal cell.
 * <p>
 * A character cell is about twice as tall as it is wide, so filling the whole rectangle with
 * a fixed viewport would stretch the map horizontally. Instead, the requested viewport is
 * treated as the region that must stay visible and is expanded on its shorter axis to match
 * the rectangle's aspect ratio (see {@link #effectiveViewport()}). The map then fills the
 * rectangle with no distortion and no wasted margin, showing a little more geography. Only
 * when the expansion would exceed the whole globe (360 by 180 degrees) does the map area
 * letterbox. It is plate carrée, not Mercator, so it is for overviews, not local navigation.
 * <p>
 * The projection converts geographic coordinates to buffer columns and rows and back, so the
 * same transform drives rendering, marker placement, mouse hit-testing and pan/zoom.
 */
public final class GeoProjection {

    /** Terminal cell height divided by width; a cell is about twice as tall as wide. */
    private static final double CELL_ASPECT = 2.0;

    private final MapViewport effectiveViewport;
    private final Rect mapArea;

    /**
     * Creates a projection of the viewport onto the given area.
     *
     * @param viewport the geographic bounds that must stay visible
     * @param area     the terminal rectangle to project into
     */
    public GeoProjection(MapViewport viewport, Rect area) {
        Objects.requireNonNull(viewport, "viewport");
        Objects.requireNonNull(area, "area");
        if (area.isEmpty()) {
            this.effectiveViewport = viewport;
            this.mapArea = area;
        } else {
            this.effectiveViewport = expandToArea(viewport, area);
            this.mapArea = letterbox(effectiveViewport, area);
        }
    }

    private static MapViewport expandToArea(MapViewport viewport, Rect area) {
        double lonSpan = viewport.east() - viewport.west();
        double latSpan = viewport.north() - viewport.south();
        double centerLon = (viewport.west() + viewport.east()) / 2;
        double centerLat = (viewport.south() + viewport.north()) / 2;
        // Column:row ratio equals lonSpan/latSpan * CELL_ASPECT; match it to the area.
        double k = (double) area.width() / area.height() / CELL_ASPECT;
        double effLon;
        double effLat;
        if (lonSpan / latSpan < k) {
            effLat = latSpan;
            effLon = latSpan * k;
        } else {
            effLon = lonSpan;
            effLat = lonSpan / k;
        }
        effLon = Math.min(effLon, 360);
        effLat = Math.min(effLat, 180);
        double north = centerLat + effLat / 2;
        double south = centerLat - effLat / 2;
        if (north > 90) {
            north = 90;
            south = 90 - effLat;
        } else if (south < -90) {
            south = -90;
            north = -90 + effLat;
        }
        double west = centerLon - effLon / 2;
        double east = west + effLon;
        while (west < -180) {
            west += 360;
            east += 360;
        }
        while (west >= 180) {
            west -= 360;
            east -= 360;
        }
        return new MapViewport(west, east, south, north);
    }

    private static Rect letterbox(MapViewport viewport, Rect area) {
        double lonSpan = viewport.east() - viewport.west();
        double latSpan = viewport.north() - viewport.south();
        double target = lonSpan / latSpan * CELL_ASPECT;
        int w = area.width();
        int h = area.height();
        int mapWidth;
        int mapHeight;
        if ((double) w / h > target) {
            mapHeight = h;
            mapWidth = Math.max(1, Math.min(w, (int) Math.round(h * target)));
        } else {
            mapWidth = w;
            mapHeight = Math.max(1, Math.min(h, (int) Math.round(w / target)));
        }
        int x = area.x() + (w - mapWidth) / 2;
        int y = area.y() + (h - mapHeight) / 2;
        return new Rect(x, y, mapWidth, mapHeight);
    }

    /**
     * Returns the viewport actually shown: the requested one expanded to the area's aspect.
     *
     * @return the effective geographic bounds
     */
    public MapViewport effectiveViewport() {
        return effectiveViewport;
    }

    /**
     * Returns the sub-rectangle the map is drawn into. It equals the supplied area unless the
     * viewport spans the whole globe and cannot fill it without distortion.
     *
     * @return the map area inside the supplied area
     */
    public Rect mapArea() {
        return mapArea;
    }

    /**
     * Returns whether a coordinate falls within the effective viewport.
     *
     * @param longitude longitude in degrees
     * @param latitude  latitude in degrees
     * @return true if inside the shown bounds
     */
    public boolean contains(double longitude, double latitude) {
        double x = effectiveViewport.unwrap(longitude);
        return x >= effectiveViewport.west() && x <= effectiveViewport.east()
                && latitude >= effectiveViewport.south() && latitude <= effectiveViewport.north();
    }

    /**
     * Converts a longitude to a buffer column within {@link #mapArea()}.
     *
     * @param longitude longitude in degrees
     * @return the buffer column
     */
    public int columnOf(double longitude) {
        double x = effectiveViewport.unwrap(longitude);
        double fraction = (x - effectiveViewport.west()) / (effectiveViewport.east() - effectiveViewport.west());
        return mapArea.x() + (int) Math.round(fraction * Math.max(0, mapArea.width() - 1));
    }

    /**
     * Converts a latitude to a buffer row within {@link #mapArea()}.
     *
     * @param latitude latitude in degrees
     * @return the buffer row
     */
    public int rowOf(double latitude) {
        double fraction = (effectiveViewport.north() - latitude)
                / (effectiveViewport.north() - effectiveViewport.south());
        return mapArea.y() + (int) Math.round(fraction * Math.max(0, mapArea.height() - 1));
    }

    /**
     * Converts a buffer column to a longitude.
     *
     * @param column the buffer column
     * @return longitude in degrees, in the effective viewport's unwrapped range
     */
    public double longitudeAt(int column) {
        double fraction = mapArea.width() <= 1 ? 0 : (column - mapArea.x()) / (double) (mapArea.width() - 1);
        return effectiveViewport.west() + fraction * (effectiveViewport.east() - effectiveViewport.west());
    }

    /**
     * Converts a buffer row to a latitude.
     *
     * @param row the buffer row
     * @return latitude in degrees
     */
    public double latitudeAt(int row) {
        double fraction = mapArea.height() <= 1 ? 0 : (row - mapArea.y()) / (double) (mapArea.height() - 1);
        return effectiveViewport.north() - fraction * (effectiveViewport.north() - effectiveViewport.south());
    }

    /**
     * Returns the longitude spanned by one column, for converting a horizontal drag to a pan.
     *
     * @return degrees of longitude per column
     */
    public double degreesPerColumn() {
        return (effectiveViewport.east() - effectiveViewport.west()) / Math.max(1, mapArea.width() - 1);
    }

    /**
     * Returns the latitude spanned by one row, for converting a vertical drag to a pan.
     *
     * @return degrees of latitude per row
     */
    public double degreesPerRow() {
        return (effectiveViewport.north() - effectiveViewport.south()) / Math.max(1, mapArea.height() - 1);
    }
}

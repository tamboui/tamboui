/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.canvas.Canvas;
import dev.tamboui.widgets.canvas.Marker;

/**
 * Stateless offline world map using bundled coastlines and Canvas Braille rendering.
 * Bounds use an equirectangular projection stretched to the supplied rectangle.
 * Each render depends only on immutable configuration; input handling, navigation,
 * search and asynchronous data loading belong to an Element or application.
 * Adds no dependencies beyond the existing widgets module and core.
 */
public final class WorldMap implements Widget {
    private final MapViewport viewport;
    private final List<MapMarker> markers;
    private final List<RegionShape> regions;
    private final List<RouteShape> routes;
    private final Color coastlineColor;
    private final Color landColor;
    private final Color tintWater;
    private final Color tintLand;
    private final CoastlineData coastlineData;

    private WorldMap(Builder builder) {
        viewport = builder.viewport;
        markers = Collections.unmodifiableList(new ArrayList<>(builder.markers));
        regions = Collections.unmodifiableList(new ArrayList<>(builder.regions));
        routes = Collections.unmodifiableList(new ArrayList<>(builder.routes));
        coastlineColor = builder.coastlineColor;
        landColor = builder.landColor;
        tintWater = builder.tintWater;
        tintLand = builder.tintLand;
        coastlineData = builder.coastlineData;
    }

    /**
     * Creates a builder for an offline world map.
     *
     * @return a builder for an offline whole-world map
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty()) {
            return;
        }
        buffer.fill(area, Cell.EMPTY);
        GeoProjection projection = new GeoProjection(viewport, area);
        Rect mapArea = projection.mapArea();
        if (mapArea.isEmpty()) {
            return;
        }
        MapViewport shown = projection.effectiveViewport();
        // Raster background pass: smooth land/sea shading beneath the vector Canvas.
        if (tintWater != null && tintLand != null) {
            LandTint.render(buffer, mapArea, shown, tintWater, tintLand, coastlineData);
        }
        Canvas.builder().xBounds(shown.west(), shown.east())
                .yBounds(shown.south(), shown.north()).marker(Marker.BRAILLE)
                .paint(context -> {
                    if (landColor != null) {
                        context.draw(new LandShape(coastlineData, landColor));
                    }
                    for (RegionShape region : regions) {
                        context.draw(region);
                    }
                    context.draw(new CoastlineShape(coastlineData, coastlineColor));
                    for (RouteShape route : routes) {
                        context.draw(route);
                    }
                }).build().render(mapArea, buffer);
        int rightEdge = area.x() + area.width();
        for (MapMarker marker : markers) {
            if (!projection.contains(marker.longitude(), marker.latitude())) {
                continue;
            }
            int column = projection.columnOf(marker.longitude());
            int row = projection.rowOf(marker.latitude());
            buffer.setString(column, row, "●", Style.EMPTY.fg(marker.color()));
            String label = marker.label();
            if (label != null && column + 1 < rightEdge) {
                // Clip the name to the widget's right edge so whatever fits is shown.
                String shownLabel = CharWidth.of(label) > rightEdge - (column + 1)
                        ? CharWidth.substringByWidth(label, rightEdge - (column + 1)) : label;
                if (!shownLabel.isEmpty()) {
                    buffer.setString(column + 1, row, shownLabel, Style.EMPTY.fg(marker.color()));
                }
            }
        }
    }

    /** Builder that snapshots configuration when building a widget. */
    public static final class Builder {
        private MapViewport viewport = MapViewport.WORLD;
        private final List<MapMarker> markers = new ArrayList<>();
        private final List<RegionShape> regions = new ArrayList<>();
        private final List<RouteShape> routes = new ArrayList<>();
        private Color coastlineColor = Color.WHITE;
        private Color landColor;
        private Color tintWater;
        private Color tintLand;
        private CoastlineData coastlineData = CoastlineData.bundled();

        private Builder() { }

        /**
         * Sets supplied view bounds; navigation is owned by the caller.
         * @param viewport geographic bounds
         * @return this builder
         */
        public Builder viewport(MapViewport viewport) {
            this.viewport = Objects.requireNonNull(viewport, "viewport");
            return this;
        }

        /**
         * Appends a marker. Later markers win when occupying the same cell.
         * @param marker immutable geographic marker
         * @return this builder
         */
        public Builder marker(MapMarker marker) {
            markers.add(Objects.requireNonNull(marker, "marker"));
            return this;
        }

        /**
         * Appends a route drawn above the coastline and below markers.
         * @param route immutable geographic polyline
         * @return this builder
         */
        public Builder route(RouteShape route) {
            routes.add(Objects.requireNonNull(route, "route"));
            return this;
        }

        /**
         * Appends a region drawn below the coastline.
         * @param region immutable geographic polygon
         * @return this builder
         */
        public Builder region(RegionShape region) {
            regions.add(Objects.requireNonNull(region, "region"));
            return this;
        }

        /**
         * Fills the bundled landmasses below the coastline, or clears the fill when null.
         * @param color land color, or null for no land fill
         * @return this builder
         */
        public Builder landColor(Color color) {
            landColor = color;
            return this;
        }

        /**
         * Enables smooth raster land/sea shading in the cell backgrounds, or clears it when both
         * colors are null. This is the prettier alternative to the braille {@link #landColor}.
         * @param water sea color
         * @param land land color
         * @return this builder
         */
        public Builder tint(Color water, Color land) {
            tintWater = water;
            tintLand = land;
            return this;
        }

        /**
         * Sets the coastline foreground.
         * @param color coastline color
         * @return this builder
         */
        public Builder coastlineColor(Color color) {
            coastlineColor = Objects.requireNonNull(color, "color");
            return this;
        }

        /**
         * Sets the coastline geometry (bundled by default); used for the coastline, land fill and tint.
         * @param data coastline polylines
         * @return this builder
         */
        public Builder coastlineData(CoastlineData data) {
            coastlineData = Objects.requireNonNull(data, "data");
            return this;
        }

        /**
         * Builds the immutable map widget.
         *
         * @return an immutable map widget
         */
        public WorldMap build() {
            return new WorldMap(this);
        }
    }
}

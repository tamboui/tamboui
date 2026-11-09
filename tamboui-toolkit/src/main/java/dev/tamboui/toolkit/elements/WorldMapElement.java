/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.DragHandler;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.map.CoastlineData;
import dev.tamboui.widgets.map.GeoProjection;
import dev.tamboui.widgets.map.MapMarker;
import dev.tamboui.widgets.map.MapState;
import dev.tamboui.widgets.map.MapViewport;
import dev.tamboui.widgets.map.RegionShape;
import dev.tamboui.widgets.map.RouteShape;
import dev.tamboui.widgets.map.WorldMap;

/**
 * Interactive offline world map element with keyboard and mouse pan and zoom.
 * <p>
 * The element owns a mutable {@link MapState} (the viewport); rendering is delegated to the stateless
 * {@link WorldMap} widget, which letterboxes to a correct aspect ratio. Arrow keys (or
 * {@code h j k l}) pan, {@code +}/{@code -} zoom, {@code 0} resets to the whole world. The
 * mouse wheel zooms toward the cursor and dragging with the left button pans.
 *
 * <pre>{@code
 * worldMap()
 *     .viewport(new MapViewport(-6, 16, 44, 58))
 *     .marker(new MapMarker(10.55, 55.37, Color.YELLOW))
 *     .title("Europe")
 *     .rounded();
 * }</pre>
 */
public final class WorldMapElement extends StyledElement<WorldMapElement> {

    private static final double ZOOM_IN = 0.8;
    private static final double ZOOM_OUT = 1.25;
    private static final double PAN_FRACTION = 0.1;
    private static final double MIN_LON_SPAN = 0.5;
    private static final double MIN_LAT_SPAN = 0.25;

    private MapState state = new MapState();
    private final List<MapMarker> markers = new ArrayList<>();
    private final List<RegionShape> regions = new ArrayList<>();
    private final List<RouteShape> routes = new ArrayList<>();
    private Color coastlineColor = Color.WHITE;
    private Color landColor;
    private Color tintWater;
    private Color tintLand;
    private CoastlineData coastlineData = CoastlineData.bundled();
    private boolean landVisible = true;
    private String title;
    private BorderType borderType;
    private Color borderColor;

    private Rect lastContentRect = Rect.ZERO;
    private MapViewport dragStartViewport;
    private double dragDegreesPerColumn;
    private double dragDegreesPerRow;

    /**
     * Creates an interactive world map element focused on the whole world.
     */
    public WorldMapElement() {
        this.focusable = true;
        onDrag(new PanDragHandler());
    }

    /**
     * Sets the initial geographic viewport on the current state.
     *
     * @param viewport the geographic bounds
     * @return this element
     */
    public WorldMapElement viewport(MapViewport viewport) {
        state.viewport(viewport);
        return this;
    }

    /**
     * Returns the current viewport, which pan and zoom mutate.
     *
     * @return the current geographic bounds
     */
    public MapViewport viewport() {
        return state.viewport();
    }

    /**
     * Sets the view state, so the viewport survives element rebuilds and can be mutated by
     * application code (for example a search flying to a result).
     *
     * @param state the map view state
     * @return this element
     */
    public WorldMapElement state(MapState state) {
        this.state = Objects.requireNonNull(state, "state");
        return this;
    }

    /**
     * Returns the mutable view state holding the viewport.
     *
     * @return the map view state
     */
    public MapState state() {
        return state;
    }

    /**
     * Appends a geographic marker.
     *
     * @param marker the marker
     * @return this element
     */
    public WorldMapElement marker(MapMarker marker) {
        markers.add(Objects.requireNonNull(marker, "marker"));
        return this;
    }

    /**
     * Appends a route drawn above the coastline and below markers.
     *
     * @param route the route polyline
     * @return this element
     */
    public WorldMapElement route(RouteShape route) {
        routes.add(Objects.requireNonNull(route, "route"));
        return this;
    }

    /**
     * Appends a region drawn below the coastline.
     *
     * @param region the region polygon
     * @return this element
     */
    public WorldMapElement region(RegionShape region) {
        regions.add(Objects.requireNonNull(region, "region"));
        return this;
    }

    /**
     * Sets the coastline color.
     *
     * @param color the coastline color
     * @return this element
     */
    public WorldMapElement coastlineColor(Color color) {
        this.coastlineColor = Objects.requireNonNull(color, "color");
        return this;
    }

    /**
     * Sets the coastline geometry (bundled by default).
     *
     * @param data the coastline polylines
     * @return this element
     */
    public WorldMapElement coastlineData(CoastlineData data) {
        this.coastlineData = Objects.requireNonNull(data, "data");
        return this;
    }

    /**
     * Sets the land fill color, enabling a land/sea shading that the {@code r} key toggles.
     *
     * @param color the land color
     * @return this element
     */
    public WorldMapElement landColor(Color color) {
        this.landColor = Objects.requireNonNull(color, "color");
        return this;
    }

    /**
     * Enables smooth raster land/sea shading, the prettier alternative to {@link #landColor}.
     * The {@code r} key toggles it.
     *
     * @param water the sea color
     * @param land  the land color
     * @return this element
     */
    public WorldMapElement tint(Color water, Color land) {
        this.tintWater = Objects.requireNonNull(water, "water");
        this.tintLand = Objects.requireNonNull(land, "land");
        return this;
    }

    /**
     * Sets the title, which also draws a border.
     *
     * @param title the title
     * @return this element
     */
    public WorldMapElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     *
     * @return this element
     */
    public WorldMapElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this element
     */
    public WorldMapElement borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        return Size.of(Math.min(availableWidth, 80), Math.min(availableHeight, 24));
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            lastContentRect = Rect.ZERO;
            return;
        }
        Rect content = area;
        if (title != null || borderType != null) {
            Block.Builder blockBuilder = Block.builder()
                    .borders(Borders.ALL)
                    .styleResolver(styleResolver(context));
            if (title != null) {
                blockBuilder.title(Title.from(title));
            }
            if (borderType != null) {
                blockBuilder.borderType(borderType);
            }
            if (borderColor != null) {
                blockBuilder.borderColor(borderColor);
            }
            Block block = blockBuilder.build();
            content = block.inner(area);
            frame.renderWidget(block, area);
        }
        lastContentRect = content;
        WorldMap.Builder mapBuilder = WorldMap.builder().viewport(state.viewport())
                .coastlineColor(coastlineColor).coastlineData(coastlineData);
        if (landVisible) {
            if (tintWater != null && tintLand != null) {
                mapBuilder.tint(tintWater, tintLand);
            } else if (landColor != null) {
                mapBuilder.landColor(landColor);
            }
        }
        for (RegionShape region : regions) {
            mapBuilder.region(region);
        }
        for (RouteShape route : routes) {
            mapBuilder.route(route);
        }
        for (MapMarker marker : markers) {
            mapBuilder.marker(marker);
        }
        frame.renderWidget(mapBuilder.build(), content);
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        if (!focused) {
            return EventResult.UNHANDLED;
        }
        if (event.isLeft() || event.isCharIgnoreCase('h')) {
            pan(-PAN_FRACTION, 0);
            return EventResult.HANDLED;
        }
        if (event.isRight() || event.isCharIgnoreCase('l')) {
            pan(PAN_FRACTION, 0);
            return EventResult.HANDLED;
        }
        if (event.isUp() || event.isCharIgnoreCase('k')) {
            pan(0, PAN_FRACTION);
            return EventResult.HANDLED;
        }
        if (event.isDown() || event.isCharIgnoreCase('j')) {
            pan(0, -PAN_FRACTION);
            return EventResult.HANDLED;
        }
        if (event.isChar('+') || event.isChar('=')) {
            zoomAtCenter(ZOOM_IN);
            return EventResult.HANDLED;
        }
        if (event.isChar('-') || event.isChar('_')) {
            zoomAtCenter(ZOOM_OUT);
            return EventResult.HANDLED;
        }
        if (event.isChar('0')) {
            state.reset();
            return EventResult.HANDLED;
        }
        if (event.isCharIgnoreCase('r') && (landColor != null || tintLand != null)) {
            landVisible = !landVisible;
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    @Override
    public EventResult handleMouseEvent(MouseEvent event) {
        if (event.kind() == MouseEventKind.SCROLL_UP) {
            zoomAt(ZOOM_IN, event.x(), event.y());
            return EventResult.HANDLED;
        }
        if (event.kind() == MouseEventKind.SCROLL_DOWN) {
            zoomAt(ZOOM_OUT, event.x(), event.y());
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private void pan(double lonFraction, double latFraction) {
        MapViewport shown = new GeoProjection(state.viewport(), lastContentRect).effectiveViewport();
        double dLon = lonFraction * (shown.east() - shown.west());
        double dLat = latFraction * (shown.north() - shown.south());
        state.set(shown.west() + dLon, shown.east() + dLon,
                shown.south() + dLat, shown.north() + dLat);
    }

    private void zoomAtCenter(double factor) {
        zoomAt(factor, lastContentRect.x() + lastContentRect.width() / 2,
                lastContentRect.y() + lastContentRect.height() / 2);
    }

    private void zoomAt(double factor, int screenX, int screenY) {
        GeoProjection projection = new GeoProjection(state.viewport(), lastContentRect);
        MapViewport shown = projection.effectiveViewport();
        double anchorLon = projection.longitudeAt(screenX);
        double anchorLat = projection.latitudeAt(screenY);
        Rect mapArea = projection.mapArea();
        double fracX = mapArea.width() <= 1 ? 0.5 : (screenX - mapArea.x()) / (double) (mapArea.width() - 1);
        double fracY = mapArea.height() <= 1 ? 0.5 : (screenY - mapArea.y()) / (double) (mapArea.height() - 1);
        fracX = Math.max(0, Math.min(1, fracX));
        fracY = Math.max(0, Math.min(1, fracY));
        double lonSpan = clamp((shown.east() - shown.west()) * factor, MIN_LON_SPAN, 360);
        double latSpan = clamp((shown.north() - shown.south()) * factor, MIN_LAT_SPAN, 180);
        double west = anchorLon - fracX * lonSpan;
        double north = anchorLat + fracY * latSpan;
        state.set(west, west + lonSpan, north - latSpan, north);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private final class PanDragHandler implements DragHandler {
        @Override
        public void onDragStart(int startX, int startY) {
            GeoProjection projection = new GeoProjection(state.viewport(), lastContentRect);
            dragStartViewport = projection.effectiveViewport();
            dragDegreesPerColumn = projection.degreesPerColumn();
            dragDegreesPerRow = projection.degreesPerRow();
        }

        @Override
        public void onDrag(int currentX, int currentY, int deltaX, int deltaY) {
            if (dragStartViewport == null) {
                return;
            }
            double dLon = -deltaX * dragDegreesPerColumn;
            double dLat = deltaY * dragDegreesPerRow;
            state.set(dragStartViewport.west() + dLon, dragStartViewport.east() + dLon,
                    dragStartViewport.south() + dLat, dragStartViewport.north() + dLat);
        }

        @Override
        public void onDragEnd(int endX, int endY) {
            dragStartViewport = null;
        }
    }
}

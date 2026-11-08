///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import java.time.Duration;

import dev.tamboui.style.Color;
import dev.tamboui.toolkit.app.ToolkitApp;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.elements.WorldMapElement;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.widgets.map.MapMarker;
import dev.tamboui.widgets.map.MapViewport;
import dev.tamboui.widgets.map.RouteShape;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.worldMap;

/**
 * Interactive offline world map demo, zoomed on Europe with two hometown markers.
 * <p>
 * Arrow keys (or {@code h j k l}) pan, {@code +}/{@code -} zoom, {@code 0} resets, the mouse
 * wheel zooms toward the cursor and dragging with the left button pans.
 */
public final class WorldMapDemo extends ToolkitApp {

    private static final MapViewport EUROPE = new MapViewport(-6, 16, 44, 58);

    private final WorldMapElement map = worldMap(EUROPE)
            .coastlineColor(Color.rgb(140, 200, 255))
            .tint(Color.rgb(12, 28, 54), Color.rgb(34, 92, 48))
            .route(new RouteShape(new double[][] {{10.48604, 55.28339}, {-1.55, 47.22}}, Color.MAGENTA))
            .marker(new MapMarker(10.48604, 55.28339, Color.YELLOW, "Pederstrup"))
            .marker(new MapMarker(-1.55, 47.22, Color.RED, "Nantes"))
            .title("WorldMap — Pederstrup & Nantes")
            .rounded()
            .id("map")
            .fill();

    /**
     * Demo entry point.
     *
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new WorldMapDemo().run();
    }

    private WorldMapDemo() {
    }

    @Override
    protected TuiConfig configure() {
        return TuiConfig.builder()
                .tickRate(Duration.ofMillis(100))
                .mouseCapture(true)
                .build();
    }

    @Override
    protected Element render() {
        return column(
                map,
                text(" arrows/hjkl pan   +/- zoom   0 reset   r land   wheel zoom   drag pan   q quit").dim());
    }
}

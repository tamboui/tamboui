/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.map;

import java.util.Objects;

import dev.tamboui.style.Color;
import dev.tamboui.widgets.canvas.Painter;
import dev.tamboui.widgets.canvas.Shape;

/**
 * Bundled world coastlines that compose with other Canvas shapes and layers.
 * <p>
 * The shape reads its clipping bounds from the Canvas it is drawn on, so configure
 * the Canvas with the longitude and latitude bounds you want to see. Each coastline
 * segment is drawn as a line clipped to those bounds and repeated across the antimeridian,
 * so a seam-crossing viewport (for example 170 to 190) draws continuously. Rendering mode is selected by the Canvas, so Braille, dot and
 * block markers all work without changing this shape. This shape never clears the canvas.
 *
 * <pre>{@code
 * MapViewport view = MapViewport.WORLD;
 * Canvas.builder().xBounds(view.west(), view.east())
 *     .yBounds(view.south(), view.north())
 *     .paint(ctx -> {
 *         ctx.draw(new CoastlineShape(Color.CYAN));
 *         ctx.layer();
 *         ctx.draw(new Points(new double[][] {{10.39, 55.40}}, Color.YELLOW));
 *     }).build();
 * }</pre>
 */
public final class CoastlineShape implements Shape {
    private final CoastlineData data;
    private final Color color;

    /**
     * Creates an immutable coastline shape using the bundled coastline.
     *
     * @param color coastline foreground color
     */
    public CoastlineShape(Color color) {
        this(CoastlineData.bundled(), color);
    }

    /**
     * Creates an immutable coastline shape from the given data.
     *
     * @param data  the coastline polylines
     * @param color coastline foreground color
     */
    public CoastlineShape(CoastlineData data, Color color) {
        this.data = Objects.requireNonNull(data, "data");
        this.color = Objects.requireNonNull(color, "color");
    }

    @Override
    public void draw(Painter painter) {
        for (double[] line : data.linesInternal()) {
            for (int i = 2; i < line.length; i += 2) {
                GeoSegments.drawWrapped(painter, line[i - 2], line[i - 1], line[i], line[i + 1], color);
            }
        }
    }
}

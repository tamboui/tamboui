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
 * Fills the bundled landmasses, giving a land/sea shading to pair with {@link CoastlineShape}.
 * <p>
 * The fill is derived from the same bundled coastline: each coastline feature is treated as a
 * ring and filled, closing it implicitly from its last point back to its first. Continents that
 * Natural Earth splits at the antimeridian close along the dateline, which can leave a small fill
 * artifact at the very edge of a whole-world view. Draw it below the coastline. This shape never
 * clears the canvas and adds no dependencies or data beyond the bundled coastline.
 *
 * <pre>{@code
 * ctx.draw(new LandShape(Color.rgb(0, 40, 0)));   // dark green land
 * ctx.draw(new CoastlineShape(Color.CYAN));        // coastline on top
 * }</pre>
 */
public final class LandShape implements Shape {

    private final CoastlineData data;
    private final Color color;

    /**
     * Creates an immutable land fill from the bundled coastline.
     *
     * @param color the land color
     */
    public LandShape(Color color) {
        this(CoastlineData.bundled(), color);
    }

    /**
     * Creates an immutable land fill from the given data.
     *
     * @param data  the coastline polylines
     * @param color the land color
     */
    public LandShape(CoastlineData data, Color color) {
        this.data = Objects.requireNonNull(data, "data");
        this.color = Objects.requireNonNull(color, "color");
    }

    @Override
    public void draw(Painter painter) {
        for (double[] line : data.linesInternal()) {
            RegionShape.fillRing(painter, line, color);
        }
    }
}

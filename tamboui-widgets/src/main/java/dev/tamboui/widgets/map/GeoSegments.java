/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.map;

import dev.tamboui.style.Color;
import dev.tamboui.widgets.canvas.Painter;
import dev.tamboui.widgets.canvas.shapes.Line;

/**
 * Shared drawing of geographic line segments, used by coastlines and routes so they wrap
 * across the antimeridian consistently.
 */
final class GeoSegments {

    private GeoSegments() {
    }

    /**
     * Draws one geographic segment, taking the shorter way around the globe and repeating it
     * across the seam so a viewport crossing the antimeridian still draws it continuously.
     * {@link Line} clips each copy to the canvas bounds, so copies outside it cost nothing.
     *
     * @param painter the canvas painter
     * @param x1      start longitude in degrees
     * @param y1      start latitude in degrees
     * @param x2      end longitude in degrees
     * @param y2      end latitude in degrees
     * @param color   the segment color
     */
    static void drawWrapped(Painter painter, double x1, double y1, double x2, double y2, Color color) {
        if (x2 - x1 > 180) {
            x2 -= 360;
        }
        if (x1 - x2 > 180) {
            x2 += 360;
        }
        for (int shift = -360; shift <= 720; shift += 360) {
            new Line(x1 + shift, y1, x2 + shift, y2, color).draw(painter);
        }
    }
}

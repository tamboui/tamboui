/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.map;

import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.widgets.canvas.Canvas;
import dev.tamboui.widgets.canvas.Marker;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LandShapeTest {

    private static int painted(Buffer buffer, Rect area) {
        int count = 0;
        for (int y = 0; y < area.height(); y++) {
            for (int x = 0; x < area.width(); x++) {
                if (!buffer.get(x, y).symbol().equals(" ")) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void landFillPaintsMoreThanTheCoastlineAlone() {
        Rect area = new Rect(0, 0, 60, 18);
        MapViewport view = new MapViewport(-6, 16, 44, 58);
        Buffer coast = Buffer.empty(area);
        Buffer land = Buffer.empty(area);
        canvas(view).paint(ctx -> ctx.draw(new CoastlineShape(Color.CYAN))).build().render(area, coast);
        canvas(view).paint(ctx -> ctx.draw(new LandShape(Color.GREEN))).build().render(area, land);
        assertTrue(painted(land, area) > painted(coast, area), "filled land covers more cells than the coastline");
    }

    @Test
    void rejectsNullColor() {
        assertThrows(NullPointerException.class, () -> new LandShape(null));
    }

    private static Canvas.Builder canvas(MapViewport view) {
        return Canvas.builder().xBounds(view.west(), view.east())
                .yBounds(view.south(), view.north()).marker(Marker.BRAILLE);
    }
}

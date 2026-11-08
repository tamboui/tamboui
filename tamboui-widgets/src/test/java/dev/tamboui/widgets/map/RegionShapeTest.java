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

class RegionShapeTest {

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
    void fillCoversMostOfAFullViewportRectangle() {
        Rect area = new Rect(0, 0, 20, 10);
        Buffer buffer = Buffer.empty(area);
        // A rectangle covering the whole viewport should fill (nearly) every cell.
        Canvas.builder().xBounds(0, 10).yBounds(0, 10).marker(Marker.BRAILLE)
                .paint(ctx -> ctx.draw(new RegionShape(
                        new double[][] {{0, 0}, {10, 0}, {10, 10}, {0, 10}}, Color.GREEN, null)))
                .build().render(area, buffer);
        assertTrue(painted(buffer, area) >= area.width() * area.height() - area.width(),
                "a full-viewport region fills nearly every cell");
    }

    @Test
    void outlineOnlyLeavesTheInteriorEmpty() {
        Rect area = new Rect(0, 0, 20, 10);
        Buffer fill = Buffer.empty(area);
        Buffer outline = Buffer.empty(area);
        double[][] ring = {{2, 2}, {8, 2}, {8, 8}, {2, 8}};
        Canvas.builder().xBounds(0, 10).yBounds(0, 10).marker(Marker.BRAILLE)
                .paint(ctx -> ctx.draw(new RegionShape(ring, Color.GREEN, null))).build().render(area, fill);
        Canvas.builder().xBounds(0, 10).yBounds(0, 10).marker(Marker.BRAILLE)
                .paint(ctx -> ctx.draw(new RegionShape(ring, null, Color.GREEN))).build().render(area, outline);
        assertTrue(painted(outline, area) < painted(fill, area), "an outline paints fewer cells than a fill");
        assertTrue(painted(outline, area) > 0, "an outline still draws");
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(NullPointerException.class, () -> new RegionShape(null, Color.GREEN, null));
        assertThrows(IllegalArgumentException.class,
                () -> new RegionShape(new double[][] {{0, 0, 0}}, Color.GREEN, null));
        assertThrows(IllegalArgumentException.class,
                () -> new RegionShape(new double[][] {{0, 91}}, Color.GREEN, null));
    }
}

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

class RouteShapeTest {

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
    void drawsAConnectedPolyline() {
        Rect area = new Rect(0, 0, 40, 20);
        Buffer buffer = Buffer.empty(area);
        Canvas.builder().xBounds(-10, 20).yBounds(40, 60).marker(Marker.BRAILLE)
                .paint(ctx -> ctx.draw(new RouteShape(new double[][] {{-5, 45}, {5, 55}, {15, 50}}, Color.RED)))
                .build().render(area, buffer);
        assertTrue(painted(buffer, area) > 5, "a route should draw a visible line");
    }

    @Test
    void wrapsAcrossTheAntimeridian() {
        Rect area = new Rect(0, 0, 40, 20);
        Buffer buffer = Buffer.empty(area);
        // A viewport straddling 180 degrees; a route from 175E to -175E is short across the seam.
        Canvas.builder().xBounds(170, 190).yBounds(-10, 10).marker(Marker.BRAILLE)
                .paint(ctx -> ctx.draw(new RouteShape(new double[][] {{175, 0}, {-175, 0}}, Color.RED)))
                .build().render(area, buffer);
        assertTrue(painted(buffer, area) > 3, "a seam-crossing route should draw continuously");
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(NullPointerException.class, () -> new RouteShape(null, Color.RED));
        assertThrows(NullPointerException.class, () -> new RouteShape(new double[][] {{0, 0}}, null));
        assertThrows(IllegalArgumentException.class, () -> new RouteShape(new double[][] {{0, 0, 0}}, Color.RED));
        assertThrows(IllegalArgumentException.class, () -> new RouteShape(new double[][] {{200, 0}}, Color.RED));
        assertThrows(IllegalArgumentException.class,
                () -> new RouteShape(new double[][] {{Double.NaN, 0}}, Color.RED));
    }
}

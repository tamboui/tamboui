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
import dev.tamboui.widgets.canvas.shapes.Points;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoastlineShapeTest {
    @Test
    void convenienceWidgetMatchesShapeForWorldRegionAndSeam() {
        Rect area = new Rect(0, 0, 80, 24);
        for (MapViewport view : new MapViewport[] {MapViewport.WORLD,
                new MapViewport(7, 16, 54, 58), new MapViewport(170, 190, -50, 70)}) {
            Buffer direct = Buffer.empty(area);
            Buffer wrapped = Buffer.empty(area);
            // WorldMap expands the viewport to fill the area; draw the shape over the same bounds and rect.
            GeoProjection projection = new GeoProjection(view, area);
            MapViewport shown = projection.effectiveViewport();
            canvas(shown, Marker.BRAILLE).paint(ctx -> ctx.draw(new CoastlineShape(Color.CYAN)))
                    .build().render(projection.mapArea(), direct);
            WorldMap.builder().viewport(view).coastlineColor(Color.CYAN).build().render(area, wrapped);
            assertThat(direct).isEqualTo(wrapped);
        }
    }

    @Test
    void shapeUsesRasterizationModeChosenByCanvas() {
        Rect area = new Rect(0, 0, 80, 24);
        for (Marker marker : Marker.values()) {
            Buffer buffer = Buffer.empty(area);
            canvas(MapViewport.WORLD, marker)
                    .paint(ctx -> ctx.draw(new CoastlineShape(Color.CYAN)))
                    .build().render(area, buffer);
            int painted = 0;
            for (int y = 0; y < area.height(); y++) {
                for (int x = 0; x < area.width(); x++) {
                    String symbol = buffer.get(x, y).symbol();
                    if (!symbol.equals(" ")) {
                        painted++;
                        if (marker == Marker.BRAILLE) {
                            assertTrue(symbol.codePointAt(0) > 0x2800 && symbol.codePointAt(0) <= 0x28ff);
                        } else if (marker == Marker.DOT) {
                            assertThat(buffer).hasSymbolAt(x, y, "•");
                        }
                    }
                }
            }
            assertTrue(painted > 100);
        }
    }

    @Test
    void shapePreservesPreviousLayerAndAllowsLaterOverlay() {
        Rect area = new Rect(0, 0, 5, 3);
        MapViewport ocean = new MapViewport(0, 10, -50, -40);
        Buffer buffer = Buffer.empty(area);
        canvas(ocean, Marker.DOT).paint(ctx -> {
            ctx.draw(new Points(new double[][] {{0, -40}}, Color.RED));
            ctx.layer();
            ctx.draw(new CoastlineShape(Color.CYAN));
            ctx.layer();
            ctx.draw(new Points(new double[][] {{10, -50}}, Color.YELLOW));
        }).build().render(area, buffer);
        assertThat(buffer).hasContent("•    ", "     ", "    •");
    }

    @Test
    void nullConfigurationIsRejected() {
        assertThrows(NullPointerException.class, () -> new CoastlineShape(null));
    }

    @Test
    void segmentCrossingBoundsWithBothEndpointsOutsideStillDraws() {
        Rect area = new Rect(0, 0, 10, 5);
        // A tight viewport over the North Sea near Denmark that a coastline segment
        // crosses with both endpoints beyond the bounds; Line must clip it, not skip it.
        MapViewport tight = new MapViewport(8, 9, 56, 57);
        Buffer buffer = Buffer.empty(area);
        canvas(tight, Marker.BRAILLE).paint(ctx -> ctx.draw(new CoastlineShape(Color.CYAN)))
                .build().render(area, buffer);
        int painted = 0;
        for (int y = 0; y < area.height(); y++) {
            for (int x = 0; x < area.width(); x++) {
                if (!buffer.get(x, y).symbol().equals(" ")) {
                    painted++;
                }
            }
        }
        assertTrue(painted > 0, "a coastline crossing the viewport should draw");
    }

    @Test
    void repeatedRenderIsStable() {
        Rect area = new Rect(0, 0, 40, 12);
        CoastlineShape shape = new CoastlineShape(Color.CYAN);
        Buffer first = Buffer.empty(area);
        Buffer second = Buffer.empty(area);
        canvas(MapViewport.WORLD, Marker.BRAILLE).paint(ctx -> ctx.draw(shape)).build().render(area, first);
        canvas(MapViewport.WORLD, Marker.BRAILLE).paint(ctx -> ctx.draw(shape)).build().render(area, second);
        assertThat(first).isEqualTo(second);
    }

    private static Canvas.Builder canvas(MapViewport view, Marker marker) {
        return Canvas.builder().xBounds(view.west(), view.east())
                .yBounds(view.south(), view.north()).marker(marker);
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.map;

import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldMapTest {
    private static final Rect AREA = new Rect(0, 0, 80, 24);

    @Test
    void oceanViewportHasExactMarkerPlacement() {
        Rect area = new Rect(0, 0, 5, 3);
        Buffer buffer = Buffer.empty(area);
        WorldMap.builder().viewport(new MapViewport(0, 10, -50, -40))
                .marker(new MapMarker(5, -45, Color.RED)).build().render(area, buffer);
        assertThat(buffer).hasContent("     ", "  ●  ", "     ");
    }

    @Test
    void bundledGeometryIsCompleteAndGeographic() {
        double[][] lines = CoastlineData.bundled().lines();
        assertEquals(1429, lines.length);
        int points = 0;
        for (double[] line : lines) {
            assertTrue(line.length >= 4);
            points += line.length / 2;
            for (int i = 0; i < line.length; i += 2) {
                assertTrue(Math.abs(line[i]) <= 180);
                assertTrue(Math.abs(line[i + 1]) <= 90);
            }
        }
        assertEquals(60416, points);
    }

    @Test
    void markerLabelIsDrawnBesideTheDotAndCulledWhenOffView() {
        MapViewport view = new MapViewport(-6, 16, 44, 58);
        GeoProjection projection = new GeoProjection(view, AREA);
        Buffer buffer = Buffer.empty(AREA);
        WorldMap.builder().viewport(view)
                .marker(new MapMarker(10.48604, 55.28339, Color.YELLOW, "Pederstrup"))
                .marker(new MapMarker(-100, 10, Color.RED, "Hidden")).build().render(AREA, buffer);
        int dot = projection.columnOf(10.48604);
        int row = projection.rowOf(55.28339);
        assertEquals("\u25cf", buffer.get(dot, row).symbol());
        assertEquals("P", buffer.get(dot + 1, row).symbol());
        // The off-view marker contributes neither a dot nor a label.
        StringBuilder all = new StringBuilder();
        for (int y = 0; y < AREA.height(); y++) {
            for (int x = 0; x < AREA.width(); x++) {
                all.append(buffer.get(x, y).symbol());
            }
        }
        assertTrue(all.indexOf("H") < 0, "a culled marker's label is not drawn");
    }

    @Test
    void worldRendersBrailleAndIsRepeatable() {
        WorldMap map = WorldMap.builder().build();
        Buffer first = Buffer.empty(AREA);
        Buffer second = Buffer.empty(AREA);
        map.render(AREA, first);
        map.render(AREA, second);
        int dots = 0;
        for (int y = 0; y < AREA.height(); y++) {
            for (int x = 0; x < AREA.width(); x++) {
                String symbol = first.get(x, y).symbol();
                assertEquals(symbol, second.get(x, y).symbol());
                if (!symbol.equals(" ")) {
                    assertTrue(symbol.codePointAt(0) > 0x2800 && symbol.codePointAt(0) <= 0x28ff);
                    dots++;
                }
            }
        }
        assertTrue(dots > 200);
    }

    @Test
    void markersUseLongitudeLatitudeOrderAndLastWins() {
        Buffer buffer = Buffer.empty(AREA);
        WorldMap.builder().marker(new MapMarker(0, 0, Color.RED))
                .marker(new MapMarker(0, 0, Color.GREEN)).build().render(AREA, buffer);
        assertEquals("●", buffer.get(40, 12).symbol());
        assertEquals(Style.EMPTY.fg(Color.GREEN), buffer.get(40, 12).style());
    }

    @Test
    void regionalBoundsCullMarkersInsteadOfClamping() {
        Buffer buffer = Buffer.empty(AREA);
        WorldMap.builder().viewport(new MapViewport(8, 16, 54, 58))
                .marker(new MapMarker(12, 56, Color.RED))
                .marker(new MapMarker(-74, 40, Color.RED)).build().render(AREA, buffer);
        assertEquals(1, countMarkers(buffer, AREA));
        assertEquals("●", buffer.get(40, 12).symbol());
    }

    @Test
    void antimeridianViewportWrapsMarkers() {
        MapViewport view = new MapViewport(170, 190, -10, 10);
        GeoProjection projection = new GeoProjection(view, AREA);
        Buffer buffer = Buffer.empty(AREA);
        WorldMap.builder().viewport(view)
                .marker(new MapMarker(-175, 0, Color.RED))
                .marker(new MapMarker(175, 0, Color.RED))
                .marker(new MapMarker(0, 0, Color.RED)).build().render(AREA, buffer);
        assertEquals("●", buffer.get(projection.columnOf(-175), projection.rowOf(0)).symbol());
        assertEquals("●", buffer.get(projection.columnOf(175), projection.rowOf(0)).symbol());
        assertEquals(2, countMarkers(buffer, AREA));
    }

    @Test
    void builderChangesDoNotMutateBuiltWidget() {
        WorldMap.Builder builder = WorldMap.builder();
        WorldMap map = builder.build();
        builder.marker(new MapMarker(0, 0, Color.RED)).viewport(new MapViewport(0, 10, 0, 10));
        Buffer buffer = Buffer.empty(AREA);
        map.render(AREA, buffer);
        assertEquals(0, countMarkers(buffer, AREA));
    }

    @Test
    void renderingClearsOldMarkersAndRespectsOffsetArea() {
        Rect outer = new Rect(0, 0, 12, 8);
        Rect inner = new Rect(2, 2, 8, 4);
        Buffer buffer = Buffer.empty(outer);
        buffer.setString(0, 0, "X", Style.EMPTY);
        WorldMap.builder().marker(new MapMarker(0, 0, Color.RED)).build().render(inner, buffer);
        assertEquals(1, countMarkers(buffer, outer));
        WorldMap.builder().build().render(inner, buffer);
        assertEquals(0, countMarkers(buffer, outer));
        assertEquals("X", buffer.get(0, 0).symbol());
        for (int x = 0; x < 12; x++) {
            assertEquals(" ", buffer.get(x, 7).symbol());
        }
    }

    @Test
    void emptyAndSingleCellAreasAreSafe() {
        WorldMap map = WorldMap.builder().marker(new MapMarker(180, 90, Color.RED)).build();
        Rect empty = new Rect(0, 0, 0, 0);
        map.render(empty, Buffer.empty(empty));
        Rect one = new Rect(4, 3, 1, 1);
        Buffer buffer = Buffer.empty(one);
        map.render(one, buffer);
        assertEquals("●", buffer.get(4, 3).symbol());
    }

    @Test
    void boundsIncludeBothEdges() {
        GeoProjection projection = new GeoProjection(MapViewport.WORLD, AREA);
        Buffer buffer = Buffer.empty(AREA);
        WorldMap.builder().marker(new MapMarker(-180, 90, Color.RED))
                .marker(new MapMarker(180, -90, Color.RED)).build().render(AREA, buffer);
        assertEquals("●", buffer.get(projection.columnOf(-180), projection.rowOf(90)).symbol());
        assertEquals("●", buffer.get(projection.columnOf(180), projection.rowOf(-90)).symbol());
        // The aspect-correct world map is letterboxed vertically, not stretched to the full height.
        assertTrue(projection.mapArea().height() < AREA.height());
    }

    @Test
    void invalidCoordinatesAndNullsAreRejected() {
        for (double invalid : new double[] {Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new MapViewport(invalid, 180, -90, 90));
            assertThrows(IllegalArgumentException.class, () -> new MapViewport(-180, invalid, -90, 90));
            assertThrows(IllegalArgumentException.class, () -> new MapViewport(-180, 180, invalid, 90));
            assertThrows(IllegalArgumentException.class, () -> new MapViewport(-180, 180, -90, invalid));
            assertThrows(IllegalArgumentException.class, () -> new MapMarker(invalid, 0, Color.RED));
            assertThrows(IllegalArgumentException.class, () -> new MapMarker(0, invalid, Color.RED));
        }
        assertThrows(IllegalArgumentException.class, () -> new MapViewport(0, 0, -90, 90));
        assertThrows(IllegalArgumentException.class, () -> new MapViewport(-180, 181, -90, 90));
        assertThrows(IllegalArgumentException.class, () -> new MapViewport(-181, 0, -90, 90));
        assertThrows(IllegalArgumentException.class, () -> new MapViewport(0, 10, 50, 40));
        assertThrows(IllegalArgumentException.class, () -> new MapViewport(0, 10, -91, 90));
        assertThrows(IllegalArgumentException.class, () -> new MapMarker(181, 0, Color.RED));
        assertThrows(IllegalArgumentException.class, () -> new MapMarker(0, 91, Color.RED));
        assertThrows(NullPointerException.class, () -> new MapMarker(0, 0, null));
        assertThrows(NullPointerException.class, () -> WorldMap.builder().viewport(null));
        assertThrows(NullPointerException.class, () -> WorldMap.builder().marker(null));
        assertThrows(NullPointerException.class, () -> WorldMap.builder().coastlineColor(null));
    }

    private static int countMarkers(Buffer buffer, Rect area) {
        int count = 0;
        for (int y = area.y(); y < area.y() + area.height(); y++) {
            for (int x = area.x(); x < area.x() + area.width(); x++) {
                if (buffer.get(x, y).symbol().equals("●")) {
                    count++;
                }
            }
        }
        return count;
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.map;

import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Rect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoProjectionTest {

    @Test
    void wholeWorldCannotFillAndLetterboxesToFourToOne() {
        // The globe is capped at 360x180 degrees, so it is the one case that still letterboxes.
        GeoProjection projection = new GeoProjection(MapViewport.WORLD, new Rect(0, 0, 80, 24));
        Rect mapArea = projection.mapArea();
        assertEquals(80, mapArea.width());
        assertEquals(20, mapArea.height());
        assertEquals(0, mapArea.x());
        assertEquals(2, mapArea.y());
    }

    @Test
    void viewportExpandsToFillTheWholeArea() {
        MapViewport requested = new MapViewport(0, 10, -10, 10);
        GeoProjection projection = new GeoProjection(requested, new Rect(0, 0, 80, 24));
        assertEquals(new Rect(0, 0, 80, 24), projection.mapArea(), "the map fills the whole area");
        MapViewport shown = projection.effectiveViewport();
        assertTrue(shown.east() - shown.west() > 10, "longitude is expanded to fill the width");
        assertEquals(-10, shown.south(), 1e-9);
        assertEquals(10, shown.north(), 1e-9);
        assertEquals(5, (shown.west() + shown.east()) / 2, 1e-9);
    }

    @Test
    void geoToScreenRoundTripsWithinOneDegree() {
        MapViewport view = new MapViewport(-6, 16, 44, 58);
        GeoProjection projection = new GeoProjection(view, new Rect(0, 0, 80, 24));
        for (double lon = -6; lon <= 16; lon += 2) {
            for (double lat = 44; lat <= 58; lat += 2) {
                int column = projection.columnOf(lon);
                int row = projection.rowOf(lat);
                double backLon = projection.longitudeAt(column);
                double backLat = projection.latitudeAt(row);
                assertTrue(Math.abs(backLon - lon) <= projection.degreesPerColumn());
                assertTrue(Math.abs(backLat - lat) <= projection.degreesPerRow());
            }
        }
    }

    @Test
    void containsCullsOutsideViewportAndWrapsLongitude() {
        GeoProjection projection = new GeoProjection(new MapViewport(170, 190, -10, 10), new Rect(0, 0, 80, 24));
        assertTrue(projection.contains(175, 0));
        assertTrue(projection.contains(-175, 0), "185 degrees east wraps into the viewport");
        assertFalse(projection.contains(0, 0));
        assertFalse(projection.contains(175, 80));
    }

    @Test
    void emptyAreaProducesEmptyMapArea() {
        GeoProjection projection = new GeoProjection(MapViewport.WORLD, new Rect(0, 0, 0, 0));
        assertTrue(projection.mapArea().isEmpty());
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapStateTest {

    @Test
    void defaultsToWholeWorld() {
        assertEquals(MapViewport.WORLD.west(), new MapState().viewport().west(), 1e-9);
    }

    @Test
    void flyToAndReset() {
        MapState state = new MapState();
        MapViewport europe = new MapViewport(-6, 16, 44, 58);
        state.viewport(europe);
        assertEquals(-6, state.viewport().west(), 1e-9);
        state.reset();
        assertEquals(-180, state.viewport().west(), 1e-9);
        assertThrows(NullPointerException.class, () -> state.viewport(null));
    }

    @Test
    void setNormalizesLongitudeAcrossTheAntimeridian() {
        MapState state = new MapState(new MapViewport(170, 190, -10, 10));
        state.set(185, 205, -10, 10);
        assertTrue(state.viewport().west() >= -180 && state.viewport().west() < 180,
                "western longitude is normalized");
        assertEquals(20, state.viewport().east() - state.viewport().west(), 1e-9, "span preserved");
    }

    @Test
    void setClampsLatitudeToPoleAndKeepsSpan() {
        MapState state = new MapState();
        state.set(0, 20, 80, 100);
        assertTrue(state.viewport().north() <= 90);
        assertEquals(20, state.viewport().north() - state.viewport().south(), 1e-9);
    }

    @Test
    void setCapsZoomOutButKeepsCenterSoItCanRotate() {
        MapState state = new MapState();
        state.set(-100, 300, -90, 90);   // 400-degree span, centered on 100E
        assertEquals(360, state.viewport().east() - state.viewport().west(), 1e-9, "capped at the globe");
        assertTrue(state.viewport().west() != -180 || state.viewport().east() != 180,
                "center kept, so the world view can rotate");
    }
}

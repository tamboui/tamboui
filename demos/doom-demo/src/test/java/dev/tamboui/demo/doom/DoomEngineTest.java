/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.doom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoomEngineTest {

    private static final String[] TEST_MAP = {
            "#####",
            "#...#",
            "#...#",
            "#...#",
            "#####"
    };

    @Test
    void castRayHitsEastWall() {
        var engine = new DoomDemo.DoomEngine(TEST_MAP, 2.5, 2.5, 0.0);
        var hit = engine.castRay(0.0, 10.0);

        assertEquals(1.5, hit.distance(), 0.05);
        assertTrue(hit.verticalSide());
    }

    @Test
    void castRayHitsNorthWall() {
        var engine = new DoomDemo.DoomEngine(TEST_MAP, 2.5, 2.5, 0.0);
        var hit = engine.castRay(-Math.PI / 2.0, 10.0);

        assertEquals(1.5, hit.distance(), 0.05);
        assertFalse(hit.verticalSide());
    }

    @Test
    void moveStopsAtWall() {
        var engine = new DoomDemo.DoomEngine(TEST_MAP, 2.5, 2.5, 0.0);
        boolean moved = engine.move(2.5, 0.0);

        assertFalse(moved);
        assertEquals(2.5, engine.playerX(), 0.01);
    }

    @Test
    void moveWithinEmptySpace() {
        var engine = new DoomDemo.DoomEngine(TEST_MAP, 2.5, 2.5, 0.0);
        boolean moved = engine.move(0.4, 0.0);

        assertTrue(moved);
        assertEquals(2.9, engine.playerX(), 0.01);
    }
}

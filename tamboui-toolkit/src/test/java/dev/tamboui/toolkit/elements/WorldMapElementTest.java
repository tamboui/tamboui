/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.event.DragHandler;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.widgets.map.MapViewport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldMapElementTest extends AbstractElementTest {

    private static final Rect AREA = new Rect(0, 0, 80, 24);

    private static WorldMapElement rendered(MapViewport viewport) {
        WorldMapElement element = new WorldMapElement().viewport(viewport);
        Buffer buffer = Buffer.empty(AREA);
        element.render(Frame.forTesting(buffer), AREA, DefaultRenderContext.createEmpty());
        return element;
    }

    private static double lonSpan(WorldMapElement element) {
        return element.viewport().east() - element.viewport().west();
    }

    @Test
    void arrowKeysPan() {
        WorldMapElement element = rendered(new MapViewport(-6, 16, 44, 58));
        double before = element.viewport().west();
        assertEquals(EventResult.HANDLED, element.handleKeyEvent(KeyEvent.ofKey(KeyCode.RIGHT), true));
        assertTrue(element.viewport().west() > before, "panning right increases western longitude");
    }

    @Test
    void panningIsIgnoredWhenNotFocused() {
        WorldMapElement element = rendered(new MapViewport(-6, 16, 44, 58));
        double before = element.viewport().west();
        assertEquals(EventResult.UNHANDLED, element.handleKeyEvent(KeyEvent.ofKey(KeyCode.RIGHT), false));
        assertEquals(before, element.viewport().west());
    }

    @Test
    void plusZoomsInAndMinusZoomsOut() {
        WorldMapElement element = rendered(new MapViewport(-6, 16, 44, 58));
        double start = lonSpan(element);
        element.handleKeyEvent(KeyEvent.ofChar('+'), true);
        double zoomedIn = lonSpan(element);
        assertTrue(zoomedIn < start, "+ zooms in");
        element.handleKeyEvent(KeyEvent.ofChar('-'), true);
        assertTrue(lonSpan(element) > zoomedIn, "- zooms out");
    }

    @Test
    void fullyZoomedOutPanRotatesLongitude() {
        WorldMapElement element = rendered(MapViewport.WORLD);
        element.handleKeyEvent(KeyEvent.ofKey(KeyCode.RIGHT), true);
        MapViewport view = element.viewport();
        assertEquals(360, view.east() - view.west(), 1e-9, "the whole-world span stays 360");
        assertTrue(view.west() != -180 || view.east() != 180, "panning rotates the centered longitude");
    }

    @Test
    void zeroResetsToWholeWorld() {
        WorldMapElement element = rendered(new MapViewport(-6, 16, 44, 58));
        element.handleKeyEvent(KeyEvent.ofChar('0'), true);
        assertEquals(-180, element.viewport().west());
        assertEquals(180, element.viewport().east());
    }

    @Test
    void wheelZoomsTowardCursor() {
        WorldMapElement element = rendered(new MapViewport(-6, 16, 44, 58));
        double start = lonSpan(element);
        assertEquals(EventResult.HANDLED, element.handleMouseEvent(MouseEvent.scrollUp(20, 10)));
        assertTrue(lonSpan(element) < start, "scrolling up zooms in");
        assertEquals(EventResult.HANDLED, element.handleMouseEvent(MouseEvent.scrollDown(20, 10)));
    }

    @Test
    void dragPans() {
        WorldMapElement element = rendered(new MapViewport(-6, 16, 44, 58));
        double before = element.viewport().west();
        DragHandler drag = element.dragHandler();
        drag.onDragStart(40, 12);
        drag.onDrag(50, 12, 10, 0);
        assertTrue(element.viewport().west() < before, "dragging right pans the map west");
        drag.onDragEnd(50, 12);
    }

    @Test
    void panningClampsLatitudeToPole() {
        WorldMapElement element = rendered(new MapViewport(-20, 20, 80, 88));
        // The first pan settles on the aspect-filled span; measure it, then keep panning up.
        element.handleKeyEvent(KeyEvent.ofKey(KeyCode.UP), true);
        double span = element.viewport().north() - element.viewport().south();
        for (int i = 0; i < 20; i++) {
            element.handleKeyEvent(KeyEvent.ofKey(KeyCode.UP), true);
        }
        assertTrue(element.viewport().north() <= 90, "cannot pan past the north pole");
        assertTrue(element.viewport().south() >= -90);
        assertEquals(span, element.viewport().north() - element.viewport().south(), 1e-9,
                "panning keeps the latitude span constant");
    }

    @Test
    void panningKeepsLongitudeNormalized() {
        WorldMapElement element = rendered(new MapViewport(170, 190, -10, 10));
        for (int i = 0; i < 10; i++) {
            element.handleKeyEvent(KeyEvent.ofKey(KeyCode.RIGHT), true);
        }
        assertTrue(element.viewport().west() >= -180 && element.viewport().west() < 180,
                "western longitude stays normalized while crossing the antimeridian");
    }

    @Test
    void landToggleFlipsOnlyWhenLandColorIsSet() {
        WorldMapElement withLand = rendered(new MapViewport(-6, 16, 44, 58)).landColor(Color.GREEN);
        assertEquals(EventResult.HANDLED, withLand.handleKeyEvent(KeyEvent.ofChar('r'), true));
        WorldMapElement noLand = rendered(new MapViewport(-6, 16, 44, 58));
        assertEquals(EventResult.UNHANDLED, noLand.handleKeyEvent(KeyEvent.ofChar('r'), true));
    }

    @Test
    void draggableAndFocusable() {
        WorldMapElement element = new WorldMapElement();
        assertTrue(element.isFocusable());
        assertTrue(element.isDraggable());
    }
}

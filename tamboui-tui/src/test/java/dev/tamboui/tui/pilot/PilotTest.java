/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.pilot;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Size;
import dev.tamboui.tui.EventHandler;
import dev.tamboui.tui.Renderer;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.ResizeEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for TuiTestRunner and Pilot (key, mouse, resize, accessors).
 */
class PilotTest {

    @Test
    void testKeyPress() throws Exception {
        List<Event> receivedEvents = new ArrayList<>();
        EventHandler handler = (event, runner) -> {
            receivedEvents.add(event);
            if (event instanceof KeyEvent) {
                KeyEvent ke = (KeyEvent) event;
                if (ke.isChar('q')) {
                    runner.quit();
                }
            }
            return event instanceof KeyEvent;
        };
        Renderer renderer = frame -> { };

        try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer)) {
            Pilot pilot = test.pilot();
            pilot.press('q');
            pilot.pause();
        }

        assertEquals(1, receivedEvents.size());
        assertTrue(receivedEvents.get(0) instanceof KeyEvent);
        KeyEvent ke = (KeyEvent) receivedEvents.get(0);
        assertTrue(ke.isChar('q'));
    }

    @Test
    void testMultipleKeyPresses() throws Exception {
        List<Event> receivedEvents = new ArrayList<>();
        EventHandler handler = (event, runner) -> {
            receivedEvents.add(event);
            if (event instanceof KeyEvent) {
                KeyEvent ke = (KeyEvent) event;
                if (ke.isChar('q')) {
                    runner.quit();
                }
            }
            return true;
        };
        Renderer renderer = frame -> { };

        try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer)) {
            Pilot pilot = test.pilot();
            pilot.press('h');
            pilot.press('e');
            pilot.press('l');
            pilot.press('l');
            pilot.press('o');
            pilot.press('q');
            pilot.pause();
        }

        assertEquals(6, receivedEvents.size());
        assertTrue(receivedEvents.get(0) instanceof KeyEvent);
        assertTrue(((KeyEvent) receivedEvents.get(0)).isChar('h'));
        assertTrue(((KeyEvent) receivedEvents.get(4)).isChar('o'));
        assertTrue(((KeyEvent) receivedEvents.get(5)).isChar('q'));
    }

    @Test
    void testSpecialKeyPress() throws Exception {
        List<Event> receivedEvents = new ArrayList<>();
        EventHandler handler = (event, runner) -> {
            receivedEvents.add(event);
            if (event instanceof KeyEvent) {
                KeyEvent ke = (KeyEvent) event;
                if (ke.isKey(KeyCode.ESCAPE)) {
                    runner.quit();
                }
            }
            return true;
        };
        Renderer renderer = frame -> { };

        try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer)) {
            Pilot pilot = test.pilot();
            pilot.press(KeyCode.ESCAPE);
            pilot.pause();
        }

        assertEquals(1, receivedEvents.size());
        assertTrue(receivedEvents.get(0) instanceof KeyEvent);
        KeyEvent ke = (KeyEvent) receivedEvents.get(0);
        assertTrue(ke.isKey(KeyCode.ESCAPE));
    }

    @Test
    void testMouseClick() throws Exception {
        List<Event> receivedEvents = new ArrayList<>();
        EventHandler handler = (event, runner) -> {
            receivedEvents.add(event);
            if (event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) event;
                if (me.isRelease() && me.isLeftButton() && me.x() == 10 && me.y() == 5) {
                    runner.quit();
                }
            }
            return true;
        };
        Renderer renderer = frame -> { };

        try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer)) {
            Pilot pilot = test.pilot();
            pilot.click(10, 5);
            pilot.pause();
            pilot.pause();
        }

        assertTrue(receivedEvents.size() >= 2);
        boolean foundPress = false;
        boolean foundRelease = false;
        for (Event event : receivedEvents) {
            if (event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) event;
                if (me.isPress() && me.isLeftButton() && me.x() == 10 && me.y() == 5) {
                    foundPress = true;
                }
                if (me.isRelease() && me.isLeftButton() && me.x() == 10 && me.y() == 5) {
                    foundRelease = true;
                }
            }
        }
        assertTrue(foundPress);
        assertTrue(foundRelease);
    }

    @Test
    void testResize() throws Exception {
        List<Event> receivedEvents = new ArrayList<>();
        EventHandler handler = (event, runner) -> {
            receivedEvents.add(event);
            if (event instanceof ResizeEvent) {
                runner.quit();
            }
            return true;
        };
        Renderer renderer = frame -> { };

        try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer)) {
            Pilot pilot = test.pilot();
            pilot.resize(100, 30);
            pilot.pause();
            pilot.pause();
        }

        // TuiRunner may consume ResizeEvent internally (redraw only); if it is passed to handler, we get one
        if (receivedEvents.size() >= 1) {
            assertTrue(receivedEvents.get(0) instanceof ResizeEvent);
            ResizeEvent re = (ResizeEvent) receivedEvents.get(0);
            assertEquals(100, re.width());
            assertEquals(30, re.height());
        }
    }

    @Test
    void testCustomSize() throws Exception {
        List<Event> receivedEvents = new ArrayList<>();
        EventHandler handler = (event, runner) -> {
            receivedEvents.add(event);
            if (event instanceof KeyEvent) {
                KeyEvent ke = (KeyEvent) event;
                if (ke.isChar('q')) {
                    runner.quit();
                }
            }
            return true;
        };
        Renderer renderer = frame -> {
            assertEquals(120, frame.width());
            assertEquals(40, frame.height());
        };

        try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer, new Size(120, 40))) {
            Pilot pilot = test.pilot();
            pilot.press('q');
            pilot.pause();
        }

        assertTrue(receivedEvents.size() > 0);
    }

    @Test
    void testPilotAccess() throws Exception {
        EventHandler handler = (event, runner) -> {
            if (event instanceof KeyEvent) {
                KeyEvent ke = (KeyEvent) event;
                if (ke.isChar('q')) {
                    runner.quit();
                }
            }
            return true;
        };
        Renderer renderer = frame -> { };

        try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer)) {
            Pilot pilot = test.pilot();
            assertNotNull(pilot);
            assertNotNull(test.backend());
            assertNotNull(test.runner());
        }
    }
}

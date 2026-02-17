/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.pilot;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.tui.EventHandler;
import dev.tamboui.tui.Renderer;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Example usage of the Pilot testing API (counter app, mouse, custom size, special keys).
 * Methods can be run as tests or used as documentation.
 */
class PilotExample {

    /**
     * Example: testing a simple counter application.
     */
    @org.junit.jupiter.api.Test
    void testCounterApp() throws Exception {
        int[] counter = { 0 };

        EventHandler handler = (event, runner) -> {
            if (event instanceof KeyEvent) {
                KeyEvent ke = (KeyEvent) event;
                if (ke.isChar('+')) {
                    counter[0]++;
                    return true;
                } else if (ke.isChar('-')) {
                    counter[0]--;
                    return true;
                } else if (ke.isChar('q')) {
                    runner.quit();
                    return false;
                }
            }
            return false;
        };

        Renderer renderer = frame -> {
            Rect area = frame.area();
            String text = "Counter: " + counter[0];
            int x = (area.width() - text.length()) / 2;
            int y = area.height() / 2;
            frame.buffer().setString(x, y, text, Style.EMPTY);
        };

        try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer)) {
            Pilot pilot = test.pilot();

            pilot.press('+');
            pilot.pause();
            assertEquals(1, counter[0]);

            pilot.press('+');
            pilot.pause();
            assertEquals(2, counter[0]);

            pilot.press('-');
            pilot.pause();
            assertEquals(1, counter[0]);

            pilot.press('q');
            pilot.pause();
        }
    }

    /**
     * Example: testing mouse interactions.
     */
    @org.junit.jupiter.api.Test
    void testMouseInteractions() throws Exception {
        boolean[] clicked = { false };
        int[] clickX = { 0 };
        int[] clickY = { 0 };

        EventHandler handler = (event, runner) -> {
            if (event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) event;
                if (me.isPress() && me.isLeftButton()) {
                    clicked[0] = true;
                    clickX[0] = me.x();
                    clickY[0] = me.y();
                    runner.quit();
                    return true;
                }
            } else if (event instanceof KeyEvent) {
                KeyEvent ke = (KeyEvent) event;
                if (ke.isChar('q')) {
                    runner.quit();
                    return false;
                }
            }
            return false;
        };

        Renderer renderer = frame -> { };

        try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer)) {
            Pilot pilot = test.pilot();
            pilot.click(10, 5);
            pilot.pause();

            assertTrue(clicked[0]);
            assertEquals(10, clickX[0]);
            assertEquals(5, clickY[0]);
        }
    }

    /**
     * Example: testing with custom terminal size.
     */
    @org.junit.jupiter.api.Test
    void testCustomSize() throws Exception {
        EventHandler handler = (event, runner) -> {
            if (event instanceof KeyEvent) {
                KeyEvent ke = (KeyEvent) event;
                if (ke.isChar('q')) {
                    runner.quit();
                }
            }
            return true;
        };

        Renderer renderer = frame -> {
            assertEquals(100, frame.width());
            assertEquals(50, frame.height());
        };

        try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer, new dev.tamboui.layout.Size(100, 50))) {
            Pilot pilot = test.pilot();
            pilot.press('q');
            pilot.pause();
        }
    }

    /**
     * Example: testing special keys.
     */
    @org.junit.jupiter.api.Test
    void testSpecialKeys() throws Exception {
        KeyCode[] lastKey = { null };

        EventHandler handler = (event, runner) -> {
            if (event instanceof KeyEvent) {
                KeyEvent ke = (KeyEvent) event;
                lastKey[0] = ke.code();
                if (ke.isKey(KeyCode.ESCAPE)) {
                    runner.quit();
                }
            }
            return true;
        };

        Renderer renderer = frame -> { };

        try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer)) {
            Pilot pilot = test.pilot();

            pilot.press(KeyCode.UP);
            pilot.pause();
            assertEquals(KeyCode.UP, lastKey[0]);

            pilot.press(KeyCode.DOWN);
            pilot.pause();
            assertEquals(KeyCode.DOWN, lastKey[0]);

            pilot.press(KeyCode.ESCAPE);
            pilot.pause();
        }
    }
}

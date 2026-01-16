/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.pilot;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.tui.EventHandler;
import dev.tamboui.tui.Renderer;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.pilot.Pilot;
import dev.tamboui.tui.pilot.TuiTestRunner;

/**
 * Example demonstrating how to use the Pilot testing system for TamboUI applications.
 * <p>
 * This example shows how to test a simple TUI application by simulating user interactions
 * like key presses and mouse clicks.
 */
public class PilotExample {

    /**
     * Example: Testing a simple counter application.
     */
    public static void testCounterApp() throws Exception {
        // Application state
        int[] counter = {0};

        // Event handler that responds to key presses
        EventHandler handler = (event, runner) -> {
            if (event instanceof KeyEvent) {
                KeyEvent ke = (KeyEvent) event;
                if (ke.isChar('+')) {
                    counter[0]++;
                    return true; // Redraw
                } else if (ke.isChar('-')) {
                    counter[0]--;
                    return true; // Redraw
                } else if (ke.isChar('q')) {
                    runner.quit();
                    return false;
                }
            }
            return false;
        };

        // Renderer that displays the counter
        Renderer renderer = frame -> {
            Rect area = frame.area();
            String text = "Counter: " + counter[0];
            int x = (area.width() - text.length()) / 2;
            int y = area.height() / 2;
            frame.buffer().setString(x, y, text, Style.EMPTY);
        };

        // Run the test
        try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer)) {
            Pilot pilot = test.pilot();

            // Test incrementing
            pilot.press('+');
            pilot.pause();
            assert counter[0] == 1 : "Counter should be 1";

            // Test incrementing again
            pilot.press('+');
            pilot.pause();
            assert counter[0] == 2 : "Counter should be 2";

            // Test decrementing
            pilot.press('-');
            pilot.pause();
            assert counter[0] == 1 : "Counter should be 1";

            // Quit the application
            pilot.press('q');
            pilot.pause();
        }
    }

    /**
     * Example: Testing mouse interactions.
     */
    public static void testMouseInteractions() throws Exception {
        boolean[] clicked = {false};
        int[] clickX = {0};
        int[] clickY = {0};

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

        Renderer renderer = frame -> {
            // Simple renderer
        };

        try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer)) {
            Pilot pilot = test.pilot();

            // Simulate a mouse click at position (10, 5)
            pilot.click(10, 5);
            pilot.pause();

            assert clicked[0] : "Click should have been registered";
            assert clickX[0] == 10 : "X coordinate should be 10";
            assert clickY[0] == 5 : "Y coordinate should be 5";
        }
    }

    /**
     * Example: Testing with custom terminal size.
     */
    public static void testCustomSize() throws Exception {
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
            // Verify the frame has the expected size
            assert frame.width() == 100 : "Width should be 100";
            assert frame.height() == 50 : "Height should be 50";
        };

        // Create test runner with custom size
        try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer, new dev.tamboui.layout.Size(100, 50))) {
            Pilot pilot = test.pilot();
            pilot.press('q');
            pilot.pause();
        }
    }

    /**
     * Example: Testing special keys.
     */
    public static void testSpecialKeys() throws Exception {
        KeyCode[] lastKey = {null};

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

        Renderer renderer = frame -> {
        };

        try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer)) {
            Pilot pilot = test.pilot();

            // Test arrow keys
            pilot.press(KeyCode.UP);
            pilot.pause();
            assert lastKey[0] == KeyCode.UP : "Last key should be UP";

            pilot.press(KeyCode.DOWN);
            pilot.pause();
            assert lastKey[0] == KeyCode.DOWN : "Last key should be DOWN";

            pilot.press(KeyCode.LEFT);
            pilot.pause();
            assert lastKey[0] == KeyCode.LEFT : "Last key should be LEFT";

            pilot.press(KeyCode.RIGHT);
            pilot.pause();
            assert lastKey[0] == KeyCode.RIGHT : "Last key should be RIGHT";

            // Test escape key to quit
            pilot.press(KeyCode.ESCAPE);
            pilot.pause();
        }
    }
}

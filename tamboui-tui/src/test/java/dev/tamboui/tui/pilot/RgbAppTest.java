/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.pilot;

import dev.tamboui.toolkit.app.ToolkitTestRunner;
import dev.tamboui.tui.pilot.Pilot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the RGB color switcher app, demonstrating how to use the Pilot testing system
 * with the Toolkit DSL.
 * <p>
 * This is similar to Textual's test_rgb.py example, showing a cleaner testing approach
 * using Toolkit with widget selection by ID.
 */
class RgbAppTest {

    @Test
    void testKeys() throws Exception {
        RgbAppExample app = new RgbAppExample();

        try (ToolkitTestRunner test = ToolkitTestRunner.runTest(app::render)) {
            // Set up style engine and bindings
            test.runner().styleEngine(app.styleEngine());
            // Add global handler for keyboard shortcuts
            test.runner().eventRouter().addGlobalHandler(app.actionHandler());
            Pilot pilot = test.pilot();

            // Test pressing the R key
            pilot.press('r');
            pilot.pause();
            assertEquals(RgbAppExample.BackgroundColor.RED, app.getCurrentColor());

            // Test pressing the G key
            pilot.press('g');
            pilot.pause();
            assertEquals(RgbAppExample.BackgroundColor.GREEN, app.getCurrentColor());

            // Test pressing the B key
            pilot.press('b');
            pilot.pause();
            assertEquals(RgbAppExample.BackgroundColor.BLUE, app.getCurrentColor());

            // Test pressing the X key (no binding, so no change)
            pilot.press('x');
            pilot.pause();
            assertEquals(RgbAppExample.BackgroundColor.BLUE, app.getCurrentColor());

            // Quit
            pilot.press('q');
            pilot.pause();
        }
    }

    @Test
    void testButtons() throws Exception {
        RgbAppExample app = new RgbAppExample();

        try (ToolkitTestRunner test = ToolkitTestRunner.runTest(app::render)) {
            // Set up style engine
            test.runner().styleEngine(app.styleEngine());
            Pilot pilot = test.pilot();

            // Test clicking the "Red" button by ID (no # prefix needed)
            pilot.click("red-button");
            pilot.pause();
            assertEquals(RgbAppExample.BackgroundColor.RED, app.getCurrentColor());

            // Test clicking the "Green" button by ID
            pilot.click("green-button");
            pilot.pause();
            assertEquals(RgbAppExample.BackgroundColor.GREEN, app.getCurrentColor());

            // Test clicking the "Blue" button by ID
            pilot.click("blue-button");
            pilot.pause();
            assertEquals(RgbAppExample.BackgroundColor.BLUE, app.getCurrentColor());

            // Quit
            pilot.press('q');
            pilot.pause();
        }
    }

    @Test
    void testMultipleKeys() throws Exception {
        RgbAppExample app = new RgbAppExample();

        try (ToolkitTestRunner test = ToolkitTestRunner.runTest(app::render)) {
            // Set up style engine and bindings
            test.runner().styleEngine(app.styleEngine());
            test.runner().eventRouter().addGlobalHandler(app.actionHandler());
            Pilot pilot = test.pilot();

            // Simulate typing "rgb" quickly
            pilot.press("r", "g", "b");
            pilot.pause();

            // Should end up on blue (last key pressed)
            assertEquals(RgbAppExample.BackgroundColor.BLUE, app.getCurrentColor());

            pilot.press('q');
            pilot.pause();
        }
    }

    @Test
    void testCustomSize() throws Exception {
        RgbAppExample app = new RgbAppExample();

        try (ToolkitTestRunner test = ToolkitTestRunner.runTest(app::render, new dev.tamboui.layout.Size(100, 50))) {
            // Set up style engine and bindings
            test.runner().styleEngine(app.styleEngine());
            test.runner().eventRouter().addGlobalHandler(app.actionHandler());
            Pilot pilot = test.pilot();

            pilot.press('r');
            pilot.pause();
            assertEquals(RgbAppExample.BackgroundColor.RED, app.getCurrentColor());

            pilot.press('q');
            pilot.pause();
        }
    }
}

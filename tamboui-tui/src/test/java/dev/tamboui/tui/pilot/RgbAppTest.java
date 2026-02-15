/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.pilot;

import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Size;
import dev.tamboui.toolkit.app.ToolkitTestRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the RGB color switcher app, demonstrating Pilot with ToolkitTestRunner
 * and widget selection by ID.
 */
class RgbAppTest {

    @Test
    void testKeys() throws Exception {
        RgbAppExample app = new RgbAppExample();

        try (ToolkitTestRunner test = ToolkitTestRunner.runTest(app::render)) {
            test.runner().styleEngine(app.styleEngine());
            test.runner().eventRouter().addGlobalHandler(app.actionHandler());
            Pilot pilot = test.pilot();

            pilot.press('r');
            pilot.pause();
            assertEquals(RgbAppExample.BackgroundColor.RED, app.getCurrentColor());

            pilot.press('g');
            pilot.pause();
            assertEquals(RgbAppExample.BackgroundColor.GREEN, app.getCurrentColor());

            pilot.press('b');
            pilot.pause();
            assertEquals(RgbAppExample.BackgroundColor.BLUE, app.getCurrentColor());

            pilot.press('x');
            pilot.pause();
            assertEquals(RgbAppExample.BackgroundColor.BLUE, app.getCurrentColor());

            pilot.press('q');
            pilot.pause();
        }
    }

    @Test
    void testButtons() throws Exception {
        RgbAppExample app = new RgbAppExample();

        try (ToolkitTestRunner test = ToolkitTestRunner.runTest(app::render)) {
            test.runner().styleEngine(app.styleEngine());
            Pilot pilot = test.pilot();

            pilot.click("red-button");
            pilot.pause();
            assertEquals(RgbAppExample.BackgroundColor.RED, app.getCurrentColor());

            pilot.click("green-button");
            pilot.pause();
            assertEquals(RgbAppExample.BackgroundColor.GREEN, app.getCurrentColor());

            pilot.click("blue-button");
            pilot.pause();
            assertEquals(RgbAppExample.BackgroundColor.BLUE, app.getCurrentColor());

            pilot.press('q');
            pilot.pause();
        }
    }

    @Test
    void testMultipleKeys() throws Exception {
        RgbAppExample app = new RgbAppExample();

        try (ToolkitTestRunner test = ToolkitTestRunner.runTest(app::render)) {
            test.runner().styleEngine(app.styleEngine());
            test.runner().eventRouter().addGlobalHandler(app.actionHandler());
            Pilot pilot = test.pilot();

            pilot.press("r", "g", "b");
            pilot.pause();
            assertEquals(RgbAppExample.BackgroundColor.BLUE, app.getCurrentColor());

            pilot.press('q');
            pilot.pause();
        }
    }

    @Test
    void testCustomSize() throws Exception {
        RgbAppExample app = new RgbAppExample();

        try (ToolkitTestRunner test = ToolkitTestRunner.runTest(app::render, new Size(100, 50))) {
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

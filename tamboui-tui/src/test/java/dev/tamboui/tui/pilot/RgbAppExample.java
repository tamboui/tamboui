
/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.pilot;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.bindings.ActionHandler;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.KeyTrigger;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.toolkit.app.ToolkitRunner;

import static dev.tamboui.toolkit.Toolkit.*;

import java.io.IOException;

/**
 * Example RGB color switcher app for testing demonstrations, using the Toolkit DSL
 * with bindings API and CSS styling.
 * <p>
 * This app displays three buttons for Red, Green, and Blue.
 * Pressing 'r', 'g', or 'b' keys, or clicking on the corresponding button,
 * changes the background color of the terminal.
 * <p>
 * This is similar to Textual's RGB app example, demonstrating:
 * <ul>
 *   <li>Clean Toolkit DSL usage</li>
 *   <li>Bindings API for keyboard shortcuts</li>
 *   <li>CSS styling for visual appearance</li>
 *   <li>Widget selection by ID for testing</li>
 * </ul>
 */
public class RgbAppExample {

    /**
     * The current background color.
     */
    public enum BackgroundColor {
        DEFAULT,
        RED,
        GREEN,
        BLUE
    }

    private BackgroundColor currentColor = BackgroundColor.DEFAULT;
    private ActionHandler actionHandler;
    private StyleEngine styleEngine;
    private Bindings bindings;

    /**
     * Creates a new RGB app instance.
     */
    public RgbAppExample() {
        // Create bindings for r, g, b keys
        // Note: Actions.CLICK is already bound to Mouse.Left.Press in standard bindings
        this.bindings = BindingSets.standard()
                .toBuilder()
                .bind("setRed", KeyTrigger.ch('r'))
                .bind("setGreen", KeyTrigger.ch('g'))
                .bind("setBlue", KeyTrigger.ch('b'))
                .build();

        // Create action handler for keyboard shortcuts
        actionHandler = new ActionHandler(bindings)
                .on("setRed", (e) -> color("RED"))
                .on("setGreen", (e) -> color("GREEN"))
                .on("setBlue", (e) -> color("BLUE"));

        // Load CSS from resource file
        styleEngine = StyleEngine.create();
        try {
            styleEngine.loadStylesheet("rgb-app", "/rgb-app.tcss");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        styleEngine.setActiveStylesheet("rgb-app");
    }

    /**
     * Renders the RGB app using the Toolkit DSL.
     *
     * @return the root element
     */
    public Element render() {
        // Determine CSS class based on current color
        String colorClass = "";
        if (currentColor != BackgroundColor.DEFAULT) {
            colorClass = currentColor.name().toLowerCase();
        }

        return panel()
            .id("root")
            .addClass(colorClass)
            .fill()
            .onAction(actionHandler)
            .add(
                column(
                    text("RGB Color Switcher").addClass("title"),
                    spacer(),
                    text("Red").id("red-button")
                        .focusable()
                        .onAction(new ActionHandler(bindings)
                                .on(Actions.CLICK, (e) -> color("RED"))),
                    text("Green").id("green-button")
                        .focusable()
                        .onAction(new ActionHandler(bindings)
                                .on(Actions.CLICK, (e) -> color("GREEN"))),
                    text("Blue").id("blue-button")
                        .focusable()
                        .onAction(new ActionHandler(bindings)
                                .on(Actions.CLICK, (e) -> color("BLUE"))),
                    spacer()
                )
            );
    }

    /**
     * Returns the current background color.
     *
     * @return the current color
     */
    public BackgroundColor getCurrentColor() {
        return currentColor;
    }

    /**
     * Returns the style engine for this app.
     *
     * @return the style engine
     */
    public StyleEngine styleEngine() {
        return styleEngine;
    }

    /**
     * Returns the bindings used by this app.
     *
     * @return the bindings
     */
    public Bindings bindings() {
        return bindings;
    }

    /**
     * Returns the action handler for keyboard shortcuts.
     *
     * @return the action handler
     */
    public ActionHandler actionHandler() {
        return actionHandler;
    }

    private void color(String color) {
        currentColor = BackgroundColor.valueOf(color);
    }

    /**
     * Main method to run the RGB app as a standalone application.
     *
     * @param args command line arguments (unused)
     * @throws Exception if an error occurs
     */
    public static void main(String[] args) throws Exception {
        RgbAppExample app = new RgbAppExample();

        TuiConfig config = TuiConfig.builder()
                .mouseCapture(true)
                .noTick()
                .build();

        try (ToolkitRunner runner = ToolkitRunner.builder()
                .config(config)
                .bindings(app.bindings())
                .styleEngine(app.styleEngine())
                .build()) {

            // Register global action handler for keyboard shortcuts
            runner.eventRouter().addGlobalHandler(app.actionHandler());

            runner.run(app::render);
        }
    }
}

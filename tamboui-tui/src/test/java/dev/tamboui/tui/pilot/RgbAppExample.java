/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
//FILES rgb-app.tcss=../../../../../resources/rgb-app.tcss
package dev.tamboui.tui.pilot;

import java.io.IOException;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.bindings.ActionHandler;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.bindings.KeyTrigger;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Example RGB color switcher app for testing demonstrations, using the Toolkit DSL
 * with bindings API and CSS styling.
 * <p>
 * Pressing 'r', 'g', or 'b' keys, or clicking on the corresponding button,
 * changes the background color. Used by {@link RgbAppTest}.
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
        this.bindings = BindingSets.standard()
                .toBuilder()
                .bind(KeyTrigger.ch('r'), "setRed")
                .bind(KeyTrigger.ch('g'), "setGreen")
                .bind(KeyTrigger.ch('b'), "setBlue")
                .build();

        actionHandler = new ActionHandler(bindings)
                .on("setRed", (e) -> color("RED"))
                .on("setGreen", (e) -> color("GREEN"))
                .on("setBlue", (e) -> color("BLUE"));

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
     */
    public BackgroundColor getCurrentColor() {
        return currentColor;
    }

    /**
     * Returns the style engine for this app.
     */
    public StyleEngine styleEngine() {
        return styleEngine;
    }

    /**
     * Returns the action handler for keyboard shortcuts.
     */
    public ActionHandler actionHandler() {
        return actionHandler;
    }

    private void color(String color) {
        currentColor = BackgroundColor.valueOf(color);
    }

    /**
     * Runs the RGB app as a standalone application.
     * Press r/g/b or click the buttons to change background color; q to quit.
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
                .bindings(app.bindings)
                .styleEngine(app.styleEngine())
                .build()) {

            runner.eventRouter().addGlobalHandler(app.actionHandler());
            runner.run(app::render);
        }
    }
}

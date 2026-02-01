///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST
/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.toolkit.elements.ListElement;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;

import static dev.tamboui.toolkit.Toolkit.*;

/**
 * CSS Demo showcasing live theme switching.
 * <p>
 * Features demonstrated:
 * <ul>
 *   <li>Loading CSS stylesheets from resources</li>
 *   <li>Live theme switching with 't' key</li>
 *   <li>CSS classes on elements</li>
 *   <li>Pseudo-class states (:focus) - Tab to navigate, see border change</li>
 *   <li>List elements with selection highlighting</li>
 *   <li>Combining CSS with programmatic styles</li>
 * </ul>
 */
public class CssDemo implements Element {

    private String currentTheme = "dark";
    private final StyleEngine styleEngine;
    private final List<String> listItems = List.of(
        "Dashboard",
        "Settings",
        "Profile",
        "Messages",
        "Notifications"
    );
    private final ListElement<?> navList;

    private CssDemo() {
        styleEngine = StyleEngine.create();
        try {
            // Load both themes
            styleEngine.loadStylesheet("dark", "/themes/dark.tcss");
            styleEngine.loadStylesheet("light", "/themes/light.tcss");
            styleEngine.setActiveStylesheet(currentTheme);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load CSS themes", e);
        }
        // Create navigation list
        navList = list(listItems)
            .id("nav-list")
            .title("Navigation")
            .rounded()
            .autoScroll();
    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        var demo = new CssDemo();
        demo.run();
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
     public void run() throws Exception {
        var config = TuiConfig.builder()
            .mouseCapture(true)
            .tickRate(Duration.ofMillis(100))
            .build();

        try (var runner = ToolkitRunner.create(config)) {
            runner.styleEngine(styleEngine);
            runner.run(() -> this);
        }
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        dock()
            // Header
            .top(panel(() -> row(
                text(" CSS Demo ").addClass("header"),
                spacer(),
                text(" Theme: ").addClass("dim"),
                text(currentTheme.toUpperCase()).id("theme-indicator"),
                text(" [t] Toggle ").addClass("dim"),
                text(" [Tab] Focus ").addClass("dim"),
                text(" [q] Quit ").addClass("dim")
            )).rounded().addClass("status"))

            // Left sidebar with navigation list
            .left(navList, Constraint.length(20))

            // Center panel - Style Classes (focusable)
            .center(panel(() -> column(
                text("Primary Action").addClass("primary"),
                text("Secondary Info").addClass("secondary"),
                text("Warning Message").addClass("warning"),
                text("Error Message").addClass("error"),
                text("Success Message").addClass("success"),
                text("Info Message").addClass("info")
            )).id("styles-panel").focusable().title("Style Classes").rounded())

            // Right panel - About (focusable)
            .right(panel(() -> column(
                text("This demo shows live CSS styling."),
                spacer(1),
                text("Try these features:"),
                text("  [t] Toggle dark/light theme"),
                text("  [Tab] Cycle focus between panels"),
                text("  [Up/Down] Navigate the list"),
                spacer(1),
                text("Notice how:").addClass("info"),
                text("  - Focused panels have colored borders"),
                text("  - List selection is highlighted"),
                text("  - Styles come from CSS files")
            )).id("about-panel").focusable().title("About").rounded())

            // Footer
            .bottom(panel(() -> row(
                text("Programmatic ").bold().cyan(),
                text("+ CSS ").addClass("primary"),
                text("= Powerful Styling").addClass("success")
            )).rounded())
        .render(frame, area, context);
    }

    @Override
    public Constraint constraint() {
        return Constraint.fill();
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        if (event.isCharIgnoreCase('t')) {
            toggleTheme();
            return EventResult.HANDLED;
        }
        // List navigation
        if (event.isUp()) {
            navList.selectPrevious();
            return EventResult.HANDLED;
        }
        if (event.isDown()) {
            navList.selectNext(listItems.size());
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    private void toggleTheme() {
        currentTheme = currentTheme.equals("dark") ? "light" : "dark";
        styleEngine.setActiveStylesheet(currentTheme);
    }
}

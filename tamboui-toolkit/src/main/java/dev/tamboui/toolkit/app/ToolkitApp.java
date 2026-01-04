/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.app;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.TuiConfig;

/**
 * Base class for DSL-based TUI applications.
 * <p>
 * Extend this class to create a TUI application. State is managed
 * through instance fields, and events are handled by elements via
 * their {@code onKeyEvent()} and {@code onMouseEvent()} handlers.
 *
 * <pre>{@code
 * import static toolkit.dev.tamboui.Toolkit.*;
 *
 * public class CounterApp extends ToolkitApp {
 *     private int count = 0;
 *
 *     @Override
 *     protected Element render() {
 *         return panel("Counter",
 *             text("Count: " + count).bold().cyan(),
 *             text("[Up/Down] to change").dim()
 *         )
 *         .rounded()
 *         .id("main")
 *         .focusable()
 *         .onKeyEvent(event -> {
 *             if (event.isUp()) {
 *                 count++;
 *                 return EventResult.HANDLED;
 *             }
 *             if (event.isDown()) {
 *                 count--;
 *                 return EventResult.HANDLED;
 *             }
 *             return EventResult.UNHANDLED;
 *         });
 *     }
 *
 *     public static void main(String[] args) throws Exception {
 *         new CounterApp().run();
 *     }
 * }
 * }</pre>
 */
public abstract class ToolkitApp {

    private ToolkitRunner runner;

    /**
     * Returns the TUI configuration for this application.
     * Override to customize.
     *
     * @return the configuration
     */
    protected TuiConfig configure() {
        return TuiConfig.defaults();
    }

    /**
     * Renders the application UI.
     * Called each frame to get the current state.
     * <p>
     * Add event handlers to elements using {@code onKeyEvent()} and
     * {@code onMouseEvent()} methods.
     *
     * @return the root element to render
     */
    protected abstract Element render();

    /**
     * Called when the application starts, before the first render.
     * Override to perform initialization.
     */
    protected void onStart() {
    }

    /**
     * Called when the application is about to stop.
     * Override to perform cleanup.
     */
    protected void onStop() {
    }

    /**
     * Runs the application.
     *
     * @throws Exception if an error occurs
     */
    public void run() throws Exception {
        try (ToolkitRunner r = ToolkitRunner.create(configure())) {
            this.runner = r;
            onStart();
            r.run(this::render);
        } finally {
            onStop();
            this.runner = null;
        }
    }

    /**
     * Returns the current runner, or null if not running.
     */
    protected ToolkitRunner runner() {
        return runner;
    }

    /**
     * Signals the application to quit.
     */
    protected void quit() {
        if (runner != null) {
            runner.quit();
        }
    }
}

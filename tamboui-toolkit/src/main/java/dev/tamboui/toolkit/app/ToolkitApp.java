/*
 * Copyright TamboUI Contributors
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

    /** Creates a new ToolkitApp. */
    protected ToolkitApp() {
    }

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
     * Creates a {@link ToolkitRunner.Builder} configured for this application.
     * <p>
     * Override this method if you need to customize runner configuration beyond what is
     * provided by {@link #configure()} (such as setting a custom style engine,
     * post-render processors, or fault-tolerant rendering).
     * <p>
     * By default, this configures the runner with {@link #configure()}, registers this
     * application instance as {@link ToolkitRunner.Builder#app(Object)}, and enables
     * {@link ToolkitRunner.Builder#withAutoBindingRegistration()} so that any methods
     * annotated with {@link dev.tamboui.annotations.bindings.OnAction @OnAction} on this
     * class are automatically registered as global action handlers.
     *
     * @return a new preconfigured runner builder
     */
    protected ToolkitRunner.Builder createRunnerBuilder() {
        return ToolkitRunner.builder()
                .config(configure())
                .app(this)
                .withAutoBindingRegistration();
    }

    /**
     * Runs the application.
     *
     * @throws Exception if an error occurs
     */
    public void run() throws Exception {
        try (ToolkitRunner r = createRunnerBuilder().build()) {
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
     *
     * @return the runner, or null
     */
    protected ToolkitRunner runner() {
        return runner;
    }

    /**
     * Sets the terminal window title.
     * <p>
     * The first call saves the current title on the terminal's title stack;
     * it is restored automatically when the application exits.
     *
     * @param title the window title to set
     * @throws dev.tamboui.error.RuntimeIOException if the operation fails
     */
    protected void setWindowTitle(String title) {
        if (runner != null) {
            runner.setWindowTitle(title);
        }
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

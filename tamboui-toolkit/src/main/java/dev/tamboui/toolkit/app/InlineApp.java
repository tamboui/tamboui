/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.app;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.InlineTuiConfig;

/**
 * Base class for inline DSL-based TUI applications.
 * <p>
 * Extend this class to create an inline TUI application that stays within the
 * normal terminal flow (no alternate screen). State is managed through instance
 * fields, and events are handled by elements via their handlers.
 *
 * <pre>{@code
 * import static dev.tamboui.toolkit.Toolkit.*;
 *
 * public class ProgressApp extends InlineApp {
 *     private double progress = 0.0;
 *
 *     @Override
 *     protected Element render() {
 *         return column(
 *             waveText("Installing packages...").cyan(),
 *             gauge(progress).label(String.format("%.0f%%", progress * 100)),
 *             text("Press q to cancel").dim()
 *         );
 *     }
 *
 *     @Override
 *     protected void onStart() {
 *         runner().schedule(() -> {
 *             runner().runOnRenderThread(() -> {
 *                 progress += 0.01;
 *                 if (progress >= 1.0) {
 *                     println(text("Installation complete!").green());
 *                     quit();
 *                 }
 *             });
 *         }, Duration.ofMillis(50));
 *     }
 *
 *     public static void main(String[] args) throws Exception {
 *         new ProgressApp().run();
 *     }
 * }
 * }</pre>
 *
 * @see InlineToolkitRunner
 * @see ToolkitApp
 */
public abstract class InlineApp {

    /**
     * Creates a new inline application.
     */
    protected InlineApp() {
    }

    private InlineToolkitRunner runner;

    /**
     * Returns the initial height of the inline display in lines.
     * <p>
     * The display will automatically grow beyond this if the content
     * requires more space. Override this method to specify a starting height.
     * The default is 1 line.
     *
     * @return the initial display height
     */
    protected int height() {
        return 1;
    }

    /**
     * Renders the application UI.
     * <p>
     * Called each frame to get the current state.
     * Add event handlers to elements using {@code onKeyEvent()} and
     * {@code onMouseEvent()} methods.
     *
     * @return the root element to render
     */
    protected abstract Element render();

    /**
     * Returns the TUI configuration for this application.
     * <p>
     * Override to customize tick rate, bindings, or other settings.
     *
     * @param height the display height
     * @return the configuration
     */
    protected InlineTuiConfig configure(int height) {
        return InlineTuiConfig.defaults(height);
    }

    /**
     * Called when the application starts, before the first render.
     * <p>
     * Override to perform initialization, start background tasks, or schedule work.
     */
    protected void onStart() {
    }

    /**
     * Called when the application is about to stop.
     * <p>
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
        int h = height();
        InlineTuiConfig config = configure(h);

        try (InlineToolkitRunner r = InlineToolkitRunner.create(config)) {
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
     * @return the runner
     */
    protected InlineToolkitRunner runner() {
        return runner;
    }

    /**
     * Prints an element above the viewport as a single line.
     *
     * @param element the element to print
     */
    protected void println(Element element) {
        if (runner != null) {
            runner.println(element);
        }
    }

    /**
     * Prints a plain text message above the viewport.
     *
     * @param message the message to print
     */
    protected void println(String message) {
        if (runner != null) {
            runner.println(message);
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

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.app;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.toolkit.component.ComponentTree;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.event.EventRouter;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.TickEvent;

import java.util.function.Supplier;

/**
 * Runner for DSL applications with automatic event routing.
 * <p>
 * Events are routed to elements based on focus and position.
 * Elements handle their own events via handlers set with {@code onKeyEvent()}
 * and {@code onMouseEvent()}.
 *
 * <pre>{@code
 * import static toolkit.dev.tamboui.Toolkit.*;
 *
 * try (var runner = ToolkitRunner.create()) {
 *     int[] count = {0};  // State in lambda scope
 *
 *     runner.run(() ->
 *         panel("Counter",
 *             text("Count: " + count[0]).bold().cyan()
 *         )
 *         .rounded()
 *         .id("counter")
 *         .focusable()
 *         .onKeyEvent(event -> {
 *             if (event.isUp()) {
 *                 count[0]++;
 *                 return EventResult.HANDLED;
 *             }
 *             return EventResult.UNHANDLED;
 *         })
 *     );
 * }
 * }</pre>
 */
public final class ToolkitRunner implements AutoCloseable {

    private final TuiRunner tuiRunner;
    private final FocusManager focusManager;
    private final ComponentTree componentTree;
    private final EventRouter eventRouter;
    private final DefaultRenderContext renderContext;

    private ToolkitRunner(TuiRunner tuiRunner) {
        this.tuiRunner = tuiRunner;
        this.focusManager = new FocusManager();
        this.componentTree = new ComponentTree();
        this.eventRouter = new EventRouter(focusManager);
        this.renderContext = new DefaultRenderContext(focusManager, componentTree, eventRouter);
    }

    /**
     * Creates a ToolkitRunner with default configuration.
     *
     * @return a new ToolkitRunner
     * @throws Exception if terminal initialization fails
     */
    public static ToolkitRunner create() throws Exception {
        return create(TuiConfig.defaults());
    }

    /**
     * Creates a ToolkitRunner with the specified configuration.
     *
     * @param config the configuration to use
     * @return a new ToolkitRunner
     * @throws Exception if terminal initialization fails
     */
    public static ToolkitRunner create(TuiConfig config) throws Exception {
        TuiRunner tuiRunner = TuiRunner.create(config);
        return new ToolkitRunner(tuiRunner);
    }

    /**
     * Runs the application with the given element supplier.
     * The supplier is called each frame to get the current UI state.
     * <p>
     * Events are routed to elements based on their handlers.
     * Press 'q' or Ctrl+C to quit (when no element consumes the event).
     *
     * @param elementSupplier provides the root element for each render
     * @throws Exception if an error occurs during execution
     */
    public void run(Supplier<Element> elementSupplier) throws Exception {
        tuiRunner.run(
            (event, runner) -> handleEvent(event),
            frame -> {
                // Clear state before each render
                focusManager.clearFocusables();
                componentTree.clear();
                eventRouter.clear();

                // Get the current element tree
                Element root = elementSupplier.get();

                // Render the element tree and register root for events
                if (root != null) {
                    root.render(frame, frame.area(), renderContext);
                    renderContext.registerElement(root, frame.area());
                }
            }
        );
    }

    private boolean handleEvent(Event event) {
        // Tick events always trigger a redraw for animations
        if (event instanceof TickEvent) {
            return true;
        }

        // Route to elements first - they handle their own events
        EventResult result = eventRouter.route(event);

        // If event was handled by an element, don't check for quit
        if (result.isHandled()) {
            return true;
        }

        // Handle quit only if event wasn't consumed by an element
        if (event instanceof KeyEvent && ((KeyEvent) event).isQuit()) {
            quit();
            return false;
        }

        return false;
    }

    /**
     * Signals the runner to stop.
     */
    public void quit() {
        tuiRunner.quit();
    }

    /**
     * Returns whether the runner is still running.
     */
    public boolean isRunning() {
        return tuiRunner.isRunning();
    }

    /**
     * Returns the focus manager.
     */
    public FocusManager focusManager() {
        return focusManager;
    }

    /**
     * Returns the event router.
     */
    public EventRouter eventRouter() {
        return eventRouter;
    }

    /**
     * Sets the style engine for CSS styling.
     * <p>
     * When set, elements will have their CSS styles resolved during rendering.
     *
     * @param styleEngine the style engine to use, or null to disable CSS styling
     * @return this runner for chaining
     */
    public ToolkitRunner styleEngine(StyleEngine styleEngine) {
        renderContext.setStyleEngine(styleEngine);
        return this;
    }

    /**
     * Returns the underlying TuiRunner.
     */
    public TuiRunner tuiRunner() {
        return tuiRunner;
    }

    @Override
    public void close() {
        tuiRunner.close();
    }
}

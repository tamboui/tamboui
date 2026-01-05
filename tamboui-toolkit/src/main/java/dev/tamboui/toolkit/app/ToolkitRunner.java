/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.app;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.event.EventRouter;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.bindings.ActionHandler;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.bindings.BindingSets;
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
    private final EventRouter eventRouter;
    private final DefaultRenderContext renderContext;

    private ToolkitRunner(TuiRunner tuiRunner) {
        this.tuiRunner = tuiRunner;
        this.focusManager = new FocusManager();
        this.eventRouter = new EventRouter(focusManager);
        this.renderContext = new DefaultRenderContext(focusManager, eventRouter);
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
                eventRouter.clear();

                // Get the current element tree
                Element root = elementSupplier.get();

                // Render the element tree and register root for events
                if (root != null) {
                    root.render(frame, frame.area(), renderContext);
                    renderContext.registerElement(root, frame.area());
                }

                // Auto-focus first focusable element if nothing is focused
                if (focusManager.focusedId() == null && !focusManager.focusOrder().isEmpty()) {
                    focusManager.setFocus(focusManager.focusOrder().get(0));
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

    /**
     * Creates a builder for configuring a ToolkitRunner.
     * <p>
     * The builder provides a fluent API for configuring bindings,
     * style engine, and automatic registration of action handlers.
     *
     * <pre>{@code
     * try (var runner = ToolkitRunner.builder()
     *         .bindings(BindingSets.vim())
     *         .app(this)
     *         .withAutoBindingRegistration()
     *         .build()) {
     *
     *     runner.run(() -> ...);
     * }
     * }</pre>
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ToolkitRunner} instances.
     * <p>
     * Provides a fluent API for configuring the toolkit application including
     * bindings, style engine, and auto-registration of annotated action handlers.
     */
    public static final class Builder {
        private TuiConfig config = TuiConfig.defaults();
        private Bindings bindings = BindingSets.defaults();
        private StyleEngine styleEngine;
        private Object app;
        private boolean autoBindingRegistration;

        private Builder() {
        }

        /**
         * Sets the application object containing {@code @OnAction} annotated methods.
         * <p>
         * This object will be used for automatic action handler registration
         * when {@link #withAutoBindingRegistration()} is called.
         *
         * @param app the application object
         * @return this builder
         */
        public Builder app(Object app) {
            this.app = app;
            return this;
        }

        /**
         * Sets the TUI configuration.
         *
         * @param config the configuration
         * @return this builder
         */
        public Builder config(TuiConfig config) {
            this.config = config;
            return this;
        }

        /**
         * Sets the bindings to use for action matching.
         *
         * @param bindings the bindings
         * @return this builder
         */
        public Builder bindings(Bindings bindings) {
            this.bindings = bindings;
            return this;
        }

        /**
         * Sets the style engine for CSS styling.
         *
         * @param styleEngine the style engine
         * @return this builder
         */
        public Builder styleEngine(StyleEngine styleEngine) {
            this.styleEngine = styleEngine;
            return this;
        }

        /**
         * Enables automatic registration of action handlers.
         * <p>
         * Discovers and registers handlers for methods annotated with
         * {@code @OnAction} on the application object set via {@link #app(Object)}.
         *
         * @return this builder
         */
        public Builder withAutoBindingRegistration() {
            this.autoBindingRegistration = true;
            return this;
        }

        /**
         * Builds and returns a configured ToolkitRunner.
         *
         * @return a new ToolkitRunner
         * @throws Exception if terminal initialization fails
         */
        public ToolkitRunner build() throws Exception {
            ToolkitRunner runner = ToolkitRunner.create(config);

            // Set bindings on render context for Component auto-registration
            runner.renderContext.setBindings(bindings);

            if (styleEngine != null) {
                runner.styleEngine(styleEngine);
            }

            // Register global action handlers from annotated app object
            if (autoBindingRegistration && app != null) {
                ActionHandler globalHandler = new ActionHandler(bindings)
                        .registerAnnotated(app);
                runner.eventRouter().addGlobalHandler(globalHandler);
            }

            return runner;
        }
    }
}

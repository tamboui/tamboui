/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.app;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.style.StyledAreaRegistry;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.ElementRegistry;
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

import java.io.OutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

    private static final PrintStream NULL_OUTPUT = new PrintStream(new OutputStream() {
        @Override
        public void write(int b) {
            // Discard all output
        }
    });

    private final TuiRunner tuiRunner;
    private final FocusManager focusManager;
    private final EventRouter eventRouter;
    private final ElementRegistry elementRegistry;
    private final StyledAreaRegistry styledAreaRegistry;
    private final DefaultRenderContext renderContext;
    private final ScheduledExecutorService scheduler;
    private final boolean faultTolerant;
    private final List<ToolkitPostRenderProcessor> postRenderProcessors;
    private volatile Duration lastElapsed = Duration.ZERO;

    private ToolkitRunner(TuiRunner tuiRunner,
                          boolean faultTolerant,
                          PrintStream errorOutput,
                          List<ToolkitPostRenderProcessor> toolkitPostRenderProcessors) {
        this.tuiRunner = tuiRunner;
        this.focusManager = new FocusManager();
        this.elementRegistry = new ElementRegistry();
        this.styledAreaRegistry = StyledAreaRegistry.create();
        this.eventRouter = new EventRouter(focusManager, elementRegistry);
        this.renderContext = new DefaultRenderContext(focusManager, eventRouter);
        this.renderContext.setFaultTolerant(faultTolerant);
        this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "toolkit-scheduler");
            t.setDaemon(true);
            return t;
        });
        this.faultTolerant = faultTolerant;
        this.postRenderProcessors = toolkitPostRenderProcessors;
    }

    private ToolkitRunner(TuiRunner tuiRunner) {
        this(tuiRunner, false, NULL_OUTPUT, Collections.emptyList());
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
     * <p>
     * If fault-tolerant rendering is enabled (via builder), individual element
     * render failures are caught and replaced with error placeholders, allowing
     * the rest of the UI to continue rendering.
     *
     * @param elementSupplier provides the root element for each render
     * @throws Exception if an error occurs during execution
     */
    public void run(Supplier<Element> elementSupplier) throws Exception {
        tuiRunner.run(
            (event, runner) -> handleEvent(event),
            frame -> {
                // All rendering now happens on render thread - no lock needed
                // Clear state before each render
                focusManager.clearFocusables();
                eventRouter.clear();
                elementRegistry.clear();
                styledAreaRegistry.clear();

                // Configure frame with styled area registry for auto-registration
                frame.setStyledAreaRegistry(styledAreaRegistry);

                // Get the current element tree
                Element root = elementSupplier.get();

                // Render the element tree and register root for events
                if (root != null) {
                    root.render(frame, frame.area(), renderContext);
                    renderContext.registerElement(root, frame.area());
                }

                // Auto-focus first focusable element if nothing is focused or focus is stale
                String currentFocus = focusManager.focusedId();
                List<String> focusOrder = focusManager.focusOrder();
                if (!focusOrder.isEmpty()) {
                    if (currentFocus == null || !focusOrder.contains(currentFocus)) {
                        focusManager.setFocus(focusOrder.get(0));
                    }
                }

                // Apply post-render processors (e.g., effects, overlays)
                for (ToolkitPostRenderProcessor processor : postRenderProcessors) {
                    processor.process(frame, elementRegistry, styledAreaRegistry, focusManager, lastElapsed);
                }
            }
        );
    }

    /**
     * Returns whether fault-tolerant rendering is enabled.
     *
     * @return true if fault-tolerant rendering is enabled
     */
    public boolean isFaultTolerant() {
        return faultTolerant;
    }

    private boolean handleEvent(Event event) {
        // Tick events trigger a redraw for animations
        if (event instanceof TickEvent) {
            lastElapsed = ((TickEvent) event).elapsed();
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
     *
     * @return true if the runner is running
     */
    public boolean isRunning() {
        return tuiRunner.isRunning();
    }

    /**
     * Schedules an action to run after a delay.
     * <p>
     * The action runs on the scheduler thread. If the action modifies UI state,
     * use {@link #runOnRenderThread(Runnable)} to ensure thread safety:
     *
     * <pre>{@code
     * runner.schedule(() -> {
     *     runner.runOnRenderThread(() -> {
     *         message = "Delayed message!";
     *     });
     * }, Duration.ofSeconds(2));
     * }</pre>
     *
     * @param action the action to run
     * @param delay the delay before running
     * @return a handle that can be used to cancel the scheduled action
     */
    public ScheduledAction schedule(Runnable action, Duration delay) {
        ScheduledFuture<?> future = scheduler.schedule(action, delay.toMillis(), TimeUnit.MILLISECONDS);
        return new ScheduledAction(future);
    }

    /**
     * Schedules an action to run repeatedly at a fixed interval.
     * <p>
     * The action runs on the scheduler thread. If the action modifies UI state,
     * use {@link #runOnRenderThread(Runnable)} to ensure thread safety.
     *
     * <pre>{@code
     * var repeating = runner.scheduleRepeating(() -> {
     *     runner.runOnRenderThread(() -> counter++);
     * }, Duration.ofMillis(100));
     *
     * // Later, to stop:
     * repeating.cancel();
     * }</pre>
     *
     * @param action the action to run
     * @param interval the interval between runs
     * @return a handle that can be used to cancel the scheduled action
     */
    public ScheduledAction scheduleRepeating(Runnable action, Duration interval) {
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                action, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
        return new ScheduledAction(future);
    }

    /**
     * Schedules an action to run repeatedly with a fixed delay between runs.
     * <p>
     * Unlike {@link #scheduleRepeating}, this waits for each execution to complete
     * before scheduling the next one. This is useful when the action's duration
     * is unpredictable and you want consistent spacing between runs.
     * <p>
     * The action runs on the scheduler thread. If the action modifies UI state,
     * use {@link #runOnRenderThread(Runnable)} to ensure thread safety.
     *
     * @param action the action to run
     * @param delay the delay between the end of one run and the start of the next
     * @return a handle that can be used to cancel the scheduled action
     */
    public ScheduledAction scheduleWithFixedDelay(Runnable action, Duration delay) {
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
                action, delay.toMillis(), delay.toMillis(), TimeUnit.MILLISECONDS);
        return new ScheduledAction(future);
    }

    /**
     * A handle to a scheduled action that can be cancelled.
     */
    public static final class ScheduledAction {
        private final ScheduledFuture<?> future;

        ScheduledAction(ScheduledFuture<?> future) {
            this.future = future;
        }

        /**
         * Cancels the scheduled action.
         * <p>
         * If the action is currently running, it will complete but won't
         * run again (for repeating actions).
         */
        public void cancel() {
            future.cancel(false);
        }

        /**
         * Returns whether this action has been cancelled.
         *
         * @return true if cancelled
         */
        public boolean isCancelled() {
            return future.isCancelled();
        }

        /**
         * Returns whether this action has completed.
         * <p>
         * For repeating actions, this only returns true if the action
         * was cancelled or encountered an error.
         *
         * @return true if completed
         */
        public boolean isDone() {
            return future.isDone();
        }
    }

    /**
     * Returns the focus manager.
     *
     * @return the focus manager
     */
    public FocusManager focusManager() {
        return focusManager;
    }

    /**
     * Returns the event router.
     *
     * @return the event router
     */
    public EventRouter eventRouter() {
        return eventRouter;
    }

    /**
     * Executes an action on the render thread.
     * <p>
     * Delegates to {@link TuiRunner#runOnRenderThread(Runnable)}.
     *
     * @param action the action to execute on the render thread
     */
    public void runOnRenderThread(Runnable action) {
        tuiRunner.runOnRenderThread(action);
    }

    /**
     * Returns whether the current thread is the render thread.
     *
     * @return true if called from the render thread
     */
    public boolean isRenderThread() {
        return tuiRunner.isRenderThread();
    }

    /**
     * Returns the element registry for ID-based area lookups.
     * <p>
     * The registry is populated during rendering and can be used
     * by effect systems to target elements by ID.
     *
     * @return the element registry
     */
    public ElementRegistry elementRegistry() {
        return elementRegistry;
    }

    /**
     * Returns the styled area registry for styled span lookups.
     * <p>
     * The registry is populated during rendering when styled content with Tags
     * is written to the buffer. It can be used by effect systems to target
     * styled spans using CSS-like selectors.
     *
     * @return the styled area registry
     */
    public StyledAreaRegistry styledAreaRegistry() {
        return styledAreaRegistry;
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
     *
     * @return the TuiRunner
     */
    public TuiRunner tuiRunner() {
        return tuiRunner;
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
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
        private boolean faultTolerant;
        private PrintStream errorOutput = NULL_OUTPUT;
        private final List<ToolkitPostRenderProcessor> toolkitPostRenderProcessors = new ArrayList<>();

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
         * Enables or disables fault-tolerant rendering.
         * <p>
         * When enabled, individual element render failures are caught and
         * replaced with error placeholders, allowing the rest of the UI to
         * continue rendering. When disabled (default), render exceptions
         * propagate to the TuiRunner's error handler.
         *
         * @param enabled true to enable fault-tolerant rendering
         * @return this builder
         */
        public Builder faultTolerant(boolean enabled) {
            this.faultTolerant = enabled;
            return this;
        }

        /**
         * Sets the output stream for error logging.
         * <p>
         * Defaults to a null output stream that discards all output.
         * In fault-tolerant mode, errors are displayed via placeholders
         * rather than logged to avoid flooding the terminal.
         *
         * @param errorOutput the error output stream, or null to discard output
         * @return this builder
         */
        public Builder errorOutput(PrintStream errorOutput) {
            this.errorOutput = errorOutput != null ? errorOutput : NULL_OUTPUT;
            return this;
        }

        /**
         * Adds a post-render processor.
         * <p>
         * Post-render processors are called after each frame is rendered,
         * allowing for effects, overlays, or other post-processing.
         *
         * @param processor the processor to add
         * @return this builder
         */
        public Builder postRenderProcessor(ToolkitPostRenderProcessor processor) {
            this.toolkitPostRenderProcessors.add(processor);
            return this;
        }

        /**
         * Builds and returns a configured ToolkitRunner.
         *
         * @return a new ToolkitRunner
         * @throws Exception if terminal initialization fails
         */
        public ToolkitRunner build() throws Exception {
            TuiRunner tuiRunner = TuiRunner.create(config);
            ToolkitRunner runner = new ToolkitRunner(tuiRunner, faultTolerant, errorOutput, toolkitPostRenderProcessors);

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

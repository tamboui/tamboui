/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.app;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Text;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.event.EventRouter;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.tui.InlineTuiConfig;
import dev.tamboui.tui.InlineTuiRunner;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.TickEvent;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Element-based inline runner - no manual rendering.
 * <p>
 * InlineToolkitRunner combines the element-based DSL of {@link ToolkitRunner}
 * with the inline display semantics of {@link InlineTuiRunner}. It provides:
 * <ul>
 *   <li>Element-based rendering (no manual widget composition)</li>
 *   <li>Automatic event routing to elements</li>
 *   <li>Focus management</li>
 *   <li>CSS styling support</li>
 *   <li>Tick events for animations (WaveText, etc.)</li>
 *   <li>Inline display (no alternate screen)</li>
 * </ul>
 *
 * <pre>{@code
 * import static dev.tamboui.toolkit.Toolkit.*;
 *
 * try (var runner = InlineToolkitRunner.create(4)) {
 *     double[] progress = {0.0};
 *
 *     runner.run(() -> column(
 *         waveText("Installing...").cyan(),
 *         gauge(progress[0]).label(String.format("%.0f%%", progress[0] * 100))
 *     ));
 * }
 * }</pre>
 *
 * @see InlineTuiRunner
 * @see ToolkitRunner
 */
public final class InlineToolkitRunner implements AutoCloseable {

    private final InlineTuiRunner tuiRunner;
    private final FocusManager focusManager;
    private final EventRouter eventRouter;
    private final ElementRegistry elementRegistry;
    private final DefaultRenderContext renderContext;
    private final ScheduledExecutorService scheduler;
    private volatile Duration lastElapsed = Duration.ZERO;

    private InlineToolkitRunner(InlineTuiRunner tuiRunner) {
        this.tuiRunner = tuiRunner;
        this.focusManager = new FocusManager();
        this.elementRegistry = new ElementRegistry();
        this.eventRouter = new EventRouter(focusManager, elementRegistry);
        this.renderContext = new DefaultRenderContext(focusManager, eventRouter);
        this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "inline-toolkit-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Creates an InlineToolkitRunner with an initial height of 1.
     *
     * @return a new InlineToolkitRunner
     * @throws Exception if terminal initialization fails
     */
    public static InlineToolkitRunner create() throws Exception {
        return create(InlineTuiConfig.defaults(1));
    }

    /**
     * Creates an InlineToolkitRunner with the specified height.
     *
     * @param height the number of lines for the inline display
     * @return a new InlineToolkitRunner
     * @throws Exception if terminal initialization fails
     */
    public static InlineToolkitRunner create(int height) throws Exception {
        return create(InlineTuiConfig.defaults(height));
    }

    /**
     * Creates an InlineToolkitRunner with the specified configuration.
     *
     * @param config the configuration to use
     * @return a new InlineToolkitRunner
     * @throws Exception if terminal initialization fails
     */
    public static InlineToolkitRunner create(InlineTuiConfig config) throws Exception {
        InlineTuiRunner tuiRunner = InlineTuiRunner.create(config);
        return new InlineToolkitRunner(tuiRunner);
    }

    /**
     * Returns a builder for configuring an InlineToolkitRunner.
     *
     * @param height the number of lines for the inline display
     * @return a new builder
     */
    public static Builder builder(int height) {
        return new Builder(height);
    }

    /**
     * Runs the application with the given element supplier.
     * <p>
     * The supplier is called each frame to get the current UI state.
     * Events are routed to elements based on their handlers.
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
                elementRegistry.clear();

                // Get the current element tree
                Element root = elementSupplier.get();

                // Calculate and set content height for dynamic resizing.
                // A preferredHeight of 0 means the element doesn't report a known
                // height (e.g. TableElement), so keep the configured viewport height.
                if (root != null) {
                    int preferredHeight = root.preferredHeight(frame.area().width(), renderContext);
                    if (preferredHeight > 0) {
                        tuiRunner.setContentHeight(preferredHeight);
                    }
                }

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
            }
        );
    }

    private boolean handleEvent(Event event) {
        // Tick events trigger a redraw for animations
        if (event instanceof TickEvent) {
            lastElapsed = ((TickEvent) event).elapsed();
            return true;
        }

        // Route to elements first
        EventResult result = eventRouter.route(event);

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
     * Prints an element above the viewport.
     * <p>
     * The element is rendered to a temporary buffer sized to the element's preferred height
     * and converted to an ANSI string.
     *
     * @param element the element to print
     */
    public void println(Element element) {
        int width = tuiRunner.width();
        int height = Math.max(1, element.preferredHeight(width, RenderContext.empty()));
        Buffer buf = Buffer.empty(Rect.of(width, height));
        Frame frame = Frame.forTesting(buf);
        element.render(frame, frame.area(), RenderContext.empty());
        tuiRunner.println(buf.toAnsiStringTrimmed());
    }

    /**
     * Prints a plain text message above the viewport.
     *
     * @param message the message to print
     */
    public void println(String message) {
        tuiRunner.println(message);
    }

    /**
     * Prints styled text above the viewport.
     *
     * @param text the styled text to print
     */
    public void println(Text text) {
        tuiRunner.println(text);
    }

    /**
     * Schedules an action to run after a delay.
     * <p>
     * The action runs on the scheduler thread. If the action modifies UI state,
     * use {@link #runOnRenderThread(Runnable)} to ensure thread safety.
     *
     * @param action the action to run
     * @param delay the delay before running
     * @return a handle that can be used to cancel the scheduled action
     */
    public ToolkitRunner.ScheduledAction schedule(Runnable action, Duration delay) {
        ScheduledFuture<?> future = scheduler.schedule(action, delay.toMillis(), TimeUnit.MILLISECONDS);
        return new ToolkitRunner.ScheduledAction(future);
    }

    /**
     * Schedules an action to run repeatedly at a fixed interval.
     *
     * @param action the action to run
     * @param interval the interval between runs
     * @return a handle that can be used to cancel the scheduled action
     */
    public ToolkitRunner.ScheduledAction scheduleRepeating(Runnable action, Duration interval) {
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                action, interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
        return new ToolkitRunner.ScheduledAction(future);
    }

    /**
     * Executes an action on the render thread.
     *
     * @param action the action to execute
     */
    public void runOnRenderThread(Runnable action) {
        tuiRunner.runOnRenderThread(action);
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
     * @return true if running
     */
    public boolean isRunning() {
        return tuiRunner.isRunning();
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
     * Returns the element registry.
     *
     * @return the element registry
     */
    public ElementRegistry elementRegistry() {
        return elementRegistry;
    }

    /**
     * Sets the style engine for CSS styling.
     *
     * @param styleEngine the style engine to use, or null to disable CSS styling
     * @return this runner for chaining
     */
    public InlineToolkitRunner styleEngine(StyleEngine styleEngine) {
        renderContext.setStyleEngine(styleEngine);
        return this;
    }

    /**
     * Returns the underlying InlineTuiRunner.
     *
     * @return the InlineTuiRunner
     */
    public InlineTuiRunner tuiRunner() {
        return tuiRunner;
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        tuiRunner.close();
    }

    /**
     * Builder for {@link InlineToolkitRunner} instances.
     */
    public static final class Builder {
        private final int height;
        private Duration tickRate = Duration.ofMillis(InlineTuiConfig.DEFAULT_TICK_RATE);
        private Duration pollTimeout = Duration.ofMillis(InlineTuiConfig.DEFAULT_POLL_TIMEOUT);
        private boolean clearOnClose = false;
        private Bindings bindings = BindingSets.defaults();
        private StyleEngine styleEngine;

        private Builder(int height) {
            this.height = height;
        }

        /**
         * Sets the tick interval for animations.
         *
         * @param tickRate the tick interval
         * @return this builder
         */
        public Builder tickRate(Duration tickRate) {
            this.tickRate = tickRate;
            return this;
        }

        /**
         * Disables tick events.
         *
         * @return this builder
         */
        public Builder noTick() {
            this.tickRate = null;
            return this;
        }

        /**
         * Sets the poll timeout.
         *
         * @param pollTimeout the poll timeout
         * @return this builder
         */
        public Builder pollTimeout(Duration pollTimeout) {
            this.pollTimeout = pollTimeout;
            return this;
        }

        /**
         * Configures whether to clear the display when closed.
         *
         * @param clearOnClose true to clear on close
         * @return this builder
         */
        public Builder clearOnClose(boolean clearOnClose) {
            this.clearOnClose = clearOnClose;
            return this;
        }

        /**
         * Sets the key bindings.
         *
         * @param bindings the bindings to use
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
         * Builds and returns a configured InlineToolkitRunner.
         *
         * @return a new InlineToolkitRunner
         * @throws Exception if terminal initialization fails
         */
        public InlineToolkitRunner build() throws Exception {
            InlineTuiConfig config = InlineTuiConfig.builder(height)
                    .tickRate(tickRate)
                    .pollTimeout(pollTimeout)
                    .clearOnClose(clearOnClose)
                    .bindings(bindings)
                    .build();

            InlineToolkitRunner runner = InlineToolkitRunner.create(config);

            if (styleEngine != null) {
                runner.styleEngine(styleEngine);
            }

            runner.renderContext.setBindings(bindings);

            return runner;
        }
    }
}

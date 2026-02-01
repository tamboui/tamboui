/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.error.TamboUIException;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.tui.bindings.ActionHandler;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.error.ErrorAction;
import dev.tamboui.tui.error.ErrorContext;
import dev.tamboui.tui.error.RenderError;
import dev.tamboui.tui.error.RenderErrorHandler;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.ResizeEvent;
import dev.tamboui.tui.event.TickEvent;
import dev.tamboui.tui.event.UiRunnable;
import dev.tamboui.tui.overlay.DebugOverlay;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Main entry point for running TUI applications.
 * <p>
 * TuiRunner handles the terminal lifecycle (raw mode, alternate screen, cursor),
 * event parsing, and the main event loop.
 *
 * <pre>{@code
 * try (var tui = TuiRunner.create()) {
 *     tui.run(
 *         (event, runner) -> {
 *             if (event instanceof KeyEvent && ((KeyEvent) event).isQuit()) {
 *                 runner.quit();
 *                 return false;
 *             }
 *             return handleEvent(event);
 *         },
 *         frame -> renderUI(frame)
 *     );
 * }
 * }</pre>
 *
 * @see TuiConfig
 * @see EventHandler
 * @see Renderer
 */
public final class TuiRunner implements AutoCloseable {

    private final Backend backend;
    private final Terminal<Backend> terminal;
    private final TuiConfig config;
    private final BlockingQueue<Event> eventQueue;
    private final AtomicBoolean running;
    private final AtomicBoolean cleanedUp;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong frameCount;
    private final Thread shutdownHook;
    private final RenderErrorHandler errorHandler;
    private final PrintStream errorOutput;
    private final AtomicReference<Instant> lastTick;
    private final AtomicReference<Instant> nextTickTime;
    private final AtomicReference<Size> lastSize;
    private final AtomicBoolean resizePending;
    private final AtomicReference<Renderer> activeRenderer;
    private final TerminalInputReader inputReader;
    private final DebugOverlay debugOverlay;
    private final List<PostRenderProcessor> postRenderProcessors;
    private volatile RenderError lastError;
    private volatile boolean inErrorState;
    private volatile int errorScroll;

    private TuiRunner(Backend backend, Terminal<Backend> terminal, TuiConfig config) {
        this.backend = backend;
        this.terminal = terminal;
        this.config = config;
        this.eventQueue = new LinkedBlockingQueue<>();
        this.running = new AtomicBoolean(true);
        this.cleanedUp = new AtomicBoolean(false);
        this.resizePending = new AtomicBoolean(false);
        this.activeRenderer = new AtomicReference<>();
        this.frameCount = new AtomicLong(0);
        this.lastTick = new AtomicReference<>(Instant.now());
        this.nextTickTime = new AtomicReference<>(
            config.tickRate() != null ? Instant.now().plus(config.tickRate()) : null);
        this.errorHandler = config.errorHandler();
        this.errorOutput = config.errorOutput();

        // Initialize last known size
        Size initialSize;
        try {
            initialSize = backend.size();
        } catch (IOException e) {
            initialSize = new Size(80, 24); // Fallback default
        }
        this.lastSize = new AtomicReference<>(initialSize);

        // Set up resize handler - just sets the flag, scheduler will handle redraw
        backend.onResize(() -> {
            try {
                Size newSize = backend.size();
                if (!newSize.equals(lastSize.get())) {
                    lastSize.set(newSize);
                    resizePending.set(true);
                }
            } catch (IOException e) {
                // Ignore resize errors
            }
        });

        // Set up scheduler for ticks and/or resize checks
        Duration schedulerPeriod = computeSchedulerPeriod(config);
        if (schedulerPeriod != null) {
            this.scheduler = new ScheduledThreadPoolExecutor(1, r -> {
                Thread t = new Thread(r, "tui-scheduler");
                t.setDaemon(true);
                return t;
            });
            long periodMs = schedulerPeriod.toMillis();
            scheduler.scheduleAtFixedRate(this::schedulerCallback, periodMs, periodMs, TimeUnit.MILLISECONDS);
        } else {
            this.scheduler = null;
        }

        // Create and start the input reader thread
        this.inputReader = new TerminalInputReader(backend, eventQueue, config.bindings(), running, config.pollTimeout());
        this.inputReader.start();

        // Create debug overlay
        this.debugOverlay = new DebugOverlay(backend.getClass().getSimpleName(), config.pollTimeout(), config.tickRate());

        // Store post-render processors
        this.postRenderProcessors = config.postRenderProcessors();

        // Register shutdown hook if enabled
        if (config.shutdownHook()) {
            this.shutdownHook = new Thread(this::cleanup, "tui-shutdown-hook");
            Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        } else {
            this.shutdownHook = null;
        }
    }

    /**
     * Creates a TuiRunner with default configuration.
     *
     * @return a new TuiRunner
     * @throws Exception if terminal initialization fails
     */
    public static TuiRunner create() throws Exception {
        return create(TuiConfig.defaults());
    }

    /**
     * Creates a TuiRunner with the specified configuration.
     *
     * @param config the configuration to use
     * @return a new TuiRunner
     * @throws Exception if terminal initialization fails
     */
    public static TuiRunner create(TuiConfig config) throws Exception {
        Backend backend = BackendFactory.create();

        try {
            if (config.rawMode()) {
                backend.enableRawMode();
            }
            if (config.alternateScreen()) {
                backend.enterAlternateScreen();
            }
            if (config.hideCursor()) {
                backend.hideCursor();
            }
            if (config.mouseCapture()) {
                backend.enableMouseCapture();
            }

            Terminal<Backend> terminal = new Terminal<>(backend);
            return new TuiRunner(backend, terminal, config);
        } catch (Exception e) {
            backend.close();
            throw e;
        }
    }

    /**
     * Runs the main event loop with the given handler and renderer.
     * <p>
     * Exceptions thrown during rendering are caught and handled according to
     * the configured {@link RenderErrorHandler}. By default, errors are displayed
     * in the UI and the application waits for user dismissal before quitting.
     *
     * @param handler  the event handler
     * @param renderer the UI renderer
     * @throws Exception if an error occurs during execution that cannot be handled
     */
    public void run(EventHandler handler, Renderer renderer) throws Exception {
        // Mark this thread as the render thread
        RenderThread.setRenderThread(Thread.currentThread());

        try {
            // Wrap renderer to add post-render processors and FPS overlay
            Renderer wrappedRenderer = frame -> {
                debugOverlay.recordFrame();
                renderer.render(frame);

                // Call post-render processors
                for (PostRenderProcessor processor : postRenderProcessors) {
                    processor.process(frame);
                }

                // FPS overlay is always last
                if (debugOverlay.isVisible()) {
                    debugOverlay.render(frame, frame.area());
                }
            };

            // Store renderer for scheduler-triggered redraws (e.g., on resize)
            this.activeRenderer.set(wrappedRenderer);

            // Initial draw
            safeRender(wrappedRenderer);

            while (running.get()) {
                if (inErrorState) {
                    handleErrorModeEvents();
                    continue;
                }

                Event event = pollEvent(config.pollTimeout());
                if (event != null) {
                    // Handle UiRunnable events (scheduled work from other threads)
                    if (event instanceof UiRunnable) {
                        try {
                            ((UiRunnable) event).run();
                        } catch (Throwable t) {
                            handleRenderError(t);
                        }
                        continue;
                    }

                    // Handle resize events by forcing a redraw
                    if (event instanceof ResizeEvent) {
                        safeRender(wrappedRenderer);
                        continue;
                    }

                    // Handle debug overlay toggle
                    if (config.bindings().matches(event, Actions.TOGGLE_DEBUG_OVERLAY)) {
                        debugOverlay.toggle();
                        safeRender(wrappedRenderer);
                        continue;
                    }

                    boolean shouldRedraw;
                    try {
                        shouldRedraw = handler.handle(event, this);
                    } catch (Throwable t) {
                        handleRenderError(t);
                        continue;
                    }
                    if (shouldRedraw && running.get() && !inErrorState) {
                        safeRender(wrappedRenderer);
                    }
                }
            }
        } finally {
            // Clear render thread reference
            RenderThread.clearRenderThread();
        }
    }

    private void safeRender(Renderer renderer) {
        RenderThread.checkRenderThread();
        try {
            terminal.draw(renderer::render);
        } catch (Throwable t) {
            handleRenderError(t);
        }
    }

    private void handleRenderError(Throwable t) {
        lastError = RenderError.from(t);
        errorScroll = 0;

        ErrorContext context = new ErrorContext() {
            @Override
            public PrintStream errorOutput() {
                return errorOutput;
            }

            @Override
            public void quit() {
                TuiRunner.this.quit();
            }
        };

        ErrorAction action = errorHandler.handle(lastError, context);

        switch (action) {
            case DISPLAY_AND_QUIT:
                inErrorState = true;
                renderErrorDisplay();
                break;
            case QUIT_IMMEDIATELY:
                quit();
                break;
            case SUPPRESS:
                // Continue - error was logged by handler
                break;
        }
    }

    private void handleErrorModeEvents() {
        Event event = pollEvent(config.pollTimeout());
        if (event == null) {
            return;
        }

        if (event instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) event;

            // Quit on 'q' or Escape
            if (keyEvent.isQuit() || keyEvent.code() == KeyCode.ESCAPE) {
                quit();
                return;
            }

            // Scroll handling
            if (keyEvent.code() == KeyCode.UP || keyEvent.code() == KeyCode.CHAR && keyEvent.character() == 'k') {
                if (errorScroll > 0) {
                    errorScroll--;
                    renderErrorDisplay();
                }
            } else if (keyEvent.code() == KeyCode.DOWN || keyEvent.code() == KeyCode.CHAR && keyEvent.character() == 'j') {
                errorScroll++;
                renderErrorDisplay();
            } else if (keyEvent.code() == KeyCode.PAGE_UP) {
                errorScroll = Math.max(0, errorScroll - 10);
                renderErrorDisplay();
            } else if (keyEvent.code() == KeyCode.PAGE_DOWN) {
                errorScroll += 10;
                renderErrorDisplay();
            } else if (keyEvent.code() == KeyCode.HOME) {
                errorScroll = 0;
                renderErrorDisplay();
            }
        } else if (event instanceof ResizeEvent) {
            renderErrorDisplay();
        }
    }

    private void renderErrorDisplay() {
        if (lastError == null) {
            return;
        }

        RenderThread.checkRenderThread();
        try {
            terminal.draw(frame -> {
                Rect area = frame.area();
                Buffer buffer = frame.buffer();

                // Build the error message content
                List<Line> lines = new ArrayList<Line>();
                lines.add(Line.from(new Span(lastError.fullExceptionType(), Style.EMPTY.fg(Color.RED).bold())));
                lines.add(Line.from(Span.raw("")));
                lines.add(Line.from(new Span("Message: ", Style.EMPTY.bold()), Span.raw(lastError.message())));
                lines.add(Line.from(Span.raw("")));
                lines.add(Line.from(new Span("Stack trace:", Style.EMPTY.bold())));

                // Add stack trace lines
                String[] stackLines = lastError.formattedStackTrace().split("\n");
                for (String stackLine : stackLines) {
                    lines.add(Line.from(Span.raw(stackLine)));
                }

                // Create the block with border
                Block block = Block.builder()
                        .title(" ERROR ")
                        .titleBottom(" Press 'q' to quit, arrows to scroll ")
                        .borders(Borders.ALL)
                        .borderType(BorderType.ROUNDED)
                        .borderColor(Color.RED)
                        .build();

                // Render the block
                block.render(area, buffer);
                Rect inner = block.inner(area);

                if (inner.isEmpty()) {
                    return;
                }

                // Calculate visible lines based on scroll
                int visibleHeight = inner.height();
                int maxScroll = Math.max(0, lines.size() - visibleHeight);
                int actualScroll = Math.min(errorScroll, maxScroll);
                errorScroll = actualScroll;

                // Render visible lines
                for (int i = 0; i < visibleHeight && (actualScroll + i) < lines.size(); i++) {
                    Line line = lines.get(actualScroll + i);
                    buffer.setLine(inner.left(), inner.top() + i, line);
                }
            });
        } catch (TamboUIException e) {
            // Failed to render error display - write to error output
            errorOutput.println("Failed to render error display: " + e.getMessage());
            lastError.cause().printStackTrace(errorOutput);
            quit();
        }
    }

    /**
     * Polls for the next event with the specified timeout.
     * <p>
     * Events are read from the unified event queue, which receives events
     * from both the dedicated input reader thread (keyboard/mouse) and
     * the scheduler thread (ticks/resize).
     * <p>
     * Input events (key/mouse) are prioritized over tick events to ensure
     * UI responsiveness even when rendering is slow.
     *
     * @param timeout the maximum time to wait
     * @return the next event, or null if timeout expires
     */
    public Event pollEvent(Duration timeout) {
        try {
            // First, check if there are any input events (non-tick) waiting
            // This ensures keyboard/mouse responsiveness even with slow renders
            Event event = findInputEvent();
            if (event != null) {
                return event;
            }

            // No input events, wait for any event
            return eventQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Searches the queue for an input event (non-tick), removing and returning it.
     * This allows input events to jump ahead of queued tick events.
     */
    private Event findInputEvent() {
        // Drain queue to find input events, re-queue ticks
        List<Event> ticks = new ArrayList<>();
        Event inputEvent = null;

        Event e;
        while ((e = eventQueue.poll()) != null) {
            if (e instanceof TickEvent) {
                ticks.add(e);
            } else {
                inputEvent = e;
                break;
            }
        }

        // Re-queue tick events (they'll be processed later)
        for (Event tick : ticks) {
            eventQueue.offer(tick);
        }

        return inputEvent;
    }

    /**
     * Polls for the next event without blocking.
     *
     * @return the next event, or null if none available
     */
    public Event pollEvent() {
        return pollEvent(Duration.ZERO);
    }

    /**
     * Draws the UI using the given renderer.
     *
     * @param renderer the render function
     * @throws TamboUIException if a terminal error occurs
     */
    public void draw(Consumer<Frame> renderer) {
        terminal.draw(renderer);
    }

    /**
     * Returns true if the runner is still running.
     *
     * @return true if the runner has not been stopped
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Signals the runner to stop.
     */
    public void quit() {
        running.set(false);
    }

    /**
     * Executes an action on the render thread.
     * <p>
     * If called from the render thread, the action is executed immediately.
     * If called from another thread, the action is queued for execution
     * on the render thread.
     * <p>
     * This is the primary API for safely updating UI state from background threads:
     * <pre>{@code
     * // From a background thread:
     * runner.runOnRenderThread(() -> {
     *     updateState();
     *     // UI will redraw on next event
     * });
     * }</pre>
     *
     * @param action the action to execute on the render thread
     */
    public void runOnRenderThread(Runnable action) {
        if (RenderThread.isRenderThread()) {
            action.run();
        } else {
            eventQueue.offer(new UiRunnable(action));
        }
    }

    /**
     * Queues an action to be executed on the render thread.
     * <p>
     * Unlike {@link #runOnRenderThread(Runnable)}, this method always queues
     * the action even if called from the render thread. This is useful when
     * you want to defer execution until after the current event handling
     * completes.
     *
     * @param action the action to execute on the render thread
     */
    public void runLater(Runnable action) {
        eventQueue.offer(new UiRunnable(action));
    }

    /**
     * Returns whether the current thread is the render thread.
     *
     * @return true if called from the render thread
     */
    public boolean isRenderThread() {
        return RenderThread.isRenderThread();
    }

    /**
     * Returns the underlying terminal.
     *
     * @return the terminal instance
     */
    public Terminal<Backend> terminal() {
        return terminal;
    }

    /**
     * Returns the underlying backend.
     *
     * @return the backend instance
     */
    public Backend backend() {
        return backend;
    }

    /**
     * Computes the scheduler period based on tick rate and resize grace period.
     *
     * @param config the TUI configuration
     * @return the scheduler period, or null if no scheduling is needed
     */
    private static Duration computeSchedulerPeriod(TuiConfig config) {
        Duration tickRate = config.tickRate();
        Duration resizeGrace = config.resizeGracePeriod();

        if (tickRate == null && resizeGrace == null) {
            return null;
        }
        if (tickRate == null) {
            return resizeGrace;
        }
        if (resizeGrace == null) {
            return tickRate;
        }
        // Return the minimum of the two
        return tickRate.compareTo(resizeGrace) <= 0 ? tickRate : resizeGrace;
    }

    /**
     * Scheduler callback that handles both tick events and resize-triggered redraws.
     * <p>
     * When a resize is detected, this posts a ResizeEvent to the event queue
     * to be processed on the render thread.
     */
    private void schedulerCallback() {
        if (!running.get()) {
            return;
        }

        // Check for pending resize and post event to trigger redraw on render thread
        if (resizePending.getAndSet(false)) {
            try {
                Size newSize = backend.size();
                eventQueue.offer(ResizeEvent.of(newSize.width(), newSize.height()));
            } catch (IOException e) {
                // Ignore resize errors
            }
        }

        // Generate tick event if ticks are enabled AND it's time for the next tick
        if (config.ticksEnabled() && config.tickRate() != null) {
            Instant now = Instant.now();
            Instant targetTime = nextTickTime.get();

            if (targetTime != null && !now.isBefore(targetTime)) {
                // Compute elapsed since last tick for the event
                Instant previous = lastTick.getAndSet(now);
                Duration elapsed = Duration.between(previous, now);

                // Schedule next tick from the target time to maintain steady rate
                // This ensures we don't lose ticks due to scheduler jitter
                nextTickTime.set(targetTime.plus(config.tickRate()));

                long frame = frameCount.incrementAndGet();
                eventQueue.offer(TickEvent.of(frame, elapsed));
            }
        }
    }

    @Override
    public void close() {
        running.set(false);

        // Stop input reader thread (waits 2x poll timeout for clean exit)
        if (inputReader != null) {
            inputReader.stop(config.pollTimeout().toMillis() * 2);
        }

        // Remove shutdown hook if it was registered (prevents it from running during normal close)
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                // JVM is already shutting down, hook will run anyway
            }
        }

        // Shutdown scheduler
        if (scheduler != null) {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Perform cleanup
        cleanup();
    }

    /**
     * Performs terminal cleanup. This is idempotent and safe to call multiple times.
     * Called both from close() and from the shutdown hook.
     */
    private void cleanup() {
        // Ensure cleanup only runs once
        if (!cleanedUp.compareAndSet(false, true)) {
            return;
        }

        // Restore terminal state
        try {
            if (config.mouseCapture()) {
                backend.disableMouseCapture();
            }
            if (config.hideCursor()) {
                backend.showCursor();
            }
            if (config.alternateScreen()) {
                backend.leaveAlternateScreen();
            }
        } catch (Exception e) {
            // Best effort cleanup - continue even on error
        } finally {
            try {
                backend.close();
            } catch (Exception e) {
                // Best effort cleanup
            }
        }
    }

    /**
     * Returns a new builder for constructing TuiRunner instances.
     * <p>
     * The builder provides a fluent API for configuring all aspects of the
     * TUI application including bindings and auto-registration of action handlers.
     *
     * <pre>{@code
     * TuiRunner.builder()
     *     .bindings(BindingSets.vim())
     *     .eventHandler(app)
     *     .renderer(app::render)
     *     .autoBindingRegistration(true)
     *     .build()
     *     .run();
     * }</pre>
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link TuiRunner} instances.
     * <p>
     * Provides a fluent API for configuring the TUI application including
     * bindings, event handlers, renderers, and auto-registration of annotated
     * action handlers.
     */
    public static final class Builder {
        private TuiConfig.Builder configBuilder = TuiConfig.builder();
        private Bindings bindings = BindingSets.defaults();
        private EventHandler eventHandler;
        private Renderer renderer;
        private boolean autoBindingRegistration;

        private Builder() {
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
         * Sets the event handler for the application.
         *
         * @param eventHandler the event handler
         * @return this builder
         */
        public Builder eventHandler(EventHandler eventHandler) {
            this.eventHandler = eventHandler;
            return this;
        }

        /**
         * Sets the renderer for the application.
         *
         * @param renderer the renderer
         * @return this builder
         */
        public Builder renderer(Renderer renderer) {
            this.renderer = renderer;
            return this;
        }

        /**
         * Enables or disables automatic registration of action handlers.
         * <p>
         * When enabled, methods annotated with {@code @OnAction} on the
         * event handler object are automatically registered.
         *
         * @param enabled true to enable auto-registration
         * @return this builder
         */
        public Builder autoBindingRegistration(boolean enabled) {
            this.autoBindingRegistration = enabled;
            return this;
        }

        /**
         * Enables automatic registration of action handlers.
         * <p>
         * Equivalent to {@code autoBindingRegistration(true)}.
         *
         * @return this builder
         */
        public Builder withAutoBindingRegistration() {
            return autoBindingRegistration(true);
        }

        /**
         * Sets whether to enable raw mode.
         *
         * @param rawMode true to enable raw mode
         * @return this builder
         */
        public Builder rawMode(boolean rawMode) {
            this.configBuilder.rawMode(rawMode);
            return this;
        }

        /**
         * Sets whether to use alternate screen buffer.
         *
         * @param alternateScreen true to use alternate screen
         * @return this builder
         */
        public Builder alternateScreen(boolean alternateScreen) {
            this.configBuilder.alternateScreen(alternateScreen);
            return this;
        }

        /**
         * Sets whether to hide the cursor.
         *
         * @param hideCursor true to hide the cursor
         * @return this builder
         */
        public Builder hideCursor(boolean hideCursor) {
            this.configBuilder.hideCursor(hideCursor);
            return this;
        }

        /**
         * Sets whether to capture mouse events.
         *
         * @param mouseCapture true to enable mouse capture
         * @return this builder
         */
        public Builder mouseCapture(boolean mouseCapture) {
            this.configBuilder.mouseCapture(mouseCapture);
            return this;
        }

        /**
         * Sets the timeout for polling events.
         *
         * @param pollTimeout poll timeout duration
         * @return this builder
         */
        public Builder pollTimeout(Duration pollTimeout) {
            this.configBuilder.pollTimeout(pollTimeout);
            return this;
        }

        /**
         * Sets the interval between tick events.
         *
         * @param tickRate the tick interval
         * @return this builder
         */
        public Builder tickRate(Duration tickRate) {
            this.configBuilder.tickRate(tickRate);
            return this;
        }

        /**
         * Disables automatic tick events.
         *
         * @return this builder
         */
        public Builder noTick() {
            this.configBuilder.noTick();
            return this;
        }

        /**
         * Sets the resize grace period.
         * <p>
         * This defines the maximum time before resize events are processed,
         * ensuring the UI redraws promptly on terminal resize even when
         * ticks are disabled or have a long interval.
         *
         * @param resizeGracePeriod the grace period, or null to disable automatic resize handling
         * @return this builder
         */
        public Builder resizeGracePeriod(Duration resizeGracePeriod) {
            this.configBuilder.resizeGracePeriod(resizeGracePeriod);
            return this;
        }

        /**
         * Sets whether to register a JVM shutdown hook for cleanup.
         *
         * @param shutdownHook true to register a shutdown hook
         * @return this builder
         */
        public Builder shutdownHook(boolean shutdownHook) {
            this.configBuilder.shutdownHook(shutdownHook);
            return this;
        }

        /**
         * Builds the TuiRunner and returns an instance ready to run.
         *
         * @return a configured TuiRunner
         * @throws Exception if terminal initialization fails
         */
        public ConfiguredRunner build() throws Exception {
            TuiConfig config = configBuilder.build();
            TuiRunner runner = TuiRunner.create(config);

            ActionHandler actionHandler = null;
            if (autoBindingRegistration && eventHandler != null) {
                actionHandler = new ActionHandler(bindings)
                        .registerAnnotated(eventHandler);
            }

            return new ConfiguredRunner(runner, eventHandler, renderer, actionHandler);
        }
    }

    /**
     * A fully configured TuiRunner ready to execute.
     * <p>
     * Created by {@link Builder#build()}, this class wraps a TuiRunner
     * with its event handler, renderer, and optional action handler.
     */
    public static final class ConfiguredRunner implements AutoCloseable {
        private final TuiRunner runner;
        private final EventHandler eventHandler;
        private final Renderer renderer;
        private final ActionHandler actionHandler;

        private ConfiguredRunner(TuiRunner runner, EventHandler eventHandler,
                                 Renderer renderer, ActionHandler actionHandler) {
            this.runner = runner;
            this.eventHandler = eventHandler;
            this.renderer = renderer;
            this.actionHandler = actionHandler;
        }

        /**
         * Runs the TUI application.
         * <p>
         * If an action handler is configured, events are first dispatched
         * to it before being passed to the event handler.
         *
         * @throws Exception if an error occurs during execution
         */
        public void run() throws Exception {
            if (eventHandler == null || renderer == null) {
                throw new IllegalStateException(
                        "Both eventHandler and renderer must be set before calling run()");
            }

            if (actionHandler != null) {
                // Wrap the event handler to dispatch through the action handler first
                EventHandler wrappedHandler = (event, r) -> {
                    if (actionHandler.dispatch(event)) {
                        return true;
                    }
                    return eventHandler.handle(event, r);
                };
                runner.run(wrappedHandler, renderer);
            } else {
                runner.run(eventHandler, renderer);
            }
        }

        /**
         * Returns the underlying TuiRunner.
         *
         * @return the TuiRunner
         */
        public TuiRunner runner() {
            return runner;
        }

        /**
         * Returns the action handler, if auto-registration was enabled.
         *
         * @return the action handler, or null if not configured
         */
        public ActionHandler actionHandler() {
            return actionHandler;
        }

        /**
         * Signals the runner to stop.
         */
        public void quit() {
            runner.quit();
        }

        @Override
        public void close() {
            runner.close();
        }
    }
}

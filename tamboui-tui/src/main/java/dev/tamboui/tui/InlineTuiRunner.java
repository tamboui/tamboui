/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import dev.tamboui.inline.InlineDisplay;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Text;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.TickEvent;
import dev.tamboui.tui.event.UiRunnable;

/**
 * Event loop for inline displays.
 * <p>
 * InlineTuiRunner combines the inline display semantics of {@link InlineDisplay}
 * with the event handling capabilities of {@link TuiRunner}. It provides:
 * <ul>
 *   <li>Raw mode for key event reading (like TuiRunner)</li>
 *   <li>NO alternate screen (stays inline, like InlineDisplay)</li>
 *   <li>Tick events for animations (WaveText, etc.)</li>
 *   <li>Input reader thread for responsive keyboard handling</li>
 *   <li>Thread-safe operations for background task integration</li>
 * </ul>
 *
 * <pre>{@code
 * try (var runner = InlineTuiRunner.create(4)) {
 *     int[] progress = {0};
 *
 *     runner.run(
 *         (event, r) -> {
 *             if (event instanceof TickEvent) {
 *                 progress[0] = Math.min(100, progress[0] + 1);
 *                 if (progress[0] >= 100) r.quit();
 *             }
 *             return true;
 *         },
 *         frame -> {
 *             gauge(progress[0] / 100.0).render(frame.area(), frame.buffer());
 *         }
 *     );
 * }
 * }</pre>
 *
 * @see InlineDisplay
 * @see TuiRunner
 */
public final class InlineTuiRunner implements AutoCloseable {

    private final Backend backend;
    private final InlineViewport viewport;
    private final InlineTuiConfig config;
    private final BlockingQueue<Event> eventQueue;
    private final AtomicBoolean running;
    private final AtomicBoolean cleanedUp;
    private final ScheduledExecutorService scheduler;
    private final boolean schedulerOwned;
    private final AtomicLong frameCount;
    private final Thread shutdownHook;
    private final AtomicReference<Instant> lastTick;
    private final AtomicReference<Instant> nextTickTime;
    private final TerminalInputReader inputReader;

    private InlineTuiRunner(Backend backend, InlineViewport viewport, InlineTuiConfig config) {
        this.backend = backend;
        this.viewport = viewport;
        this.config = config;
        this.eventQueue = new LinkedBlockingQueue<>();
        this.running = new AtomicBoolean(true);
        this.cleanedUp = new AtomicBoolean(false);
        this.frameCount = new AtomicLong(0);
        this.lastTick = new AtomicReference<>(Instant.now());
        this.nextTickTime = new AtomicReference<>(
                config.tickRate() != null ? Instant.now().plus(config.tickRate()) : null);

        // Set up scheduler - use provided scheduler or create one
        Schedulers.Scheduler scheduler = Schedulers.resolve(config.scheduler());
        this.scheduler = scheduler.scheduler();
        this.schedulerOwned = scheduler.owned();

        // Only schedule the internal callback if ticks are enabled
        if (config.ticksEnabled() && config.tickRate() != null) {
            long periodMs = config.tickRate().toMillis();
            this.scheduler.scheduleAtFixedRate(this::schedulerCallback, periodMs, periodMs, TimeUnit.MILLISECONDS);
        }

        // Create and start the input reader thread
        this.inputReader = new TerminalInputReader(backend, eventQueue, config.bindings(), running, config.pollTimeout());
        this.inputReader.start();

        // Register shutdown hook
        this.shutdownHook = new Thread(this::cleanup, "inline-tui-shutdown-hook");
        Runtime.getRuntime().addShutdownHook(this.shutdownHook);
    }

    /**
     * Creates an InlineTuiRunner with the specified height and default configuration.
     *
     * @param height the number of lines for the inline display
     * @return a new InlineTuiRunner
     * @throws Exception if terminal initialization fails
     */
    public static InlineTuiRunner create(int height) throws Exception {
        return create(InlineTuiConfig.defaults(height));
    }

    /**
     * Creates an InlineTuiRunner with the specified configuration.
     *
     * @param config the configuration to use
     * @return a new InlineTuiRunner
     * @throws Exception if terminal initialization fails
     */
    public static InlineTuiRunner create(InlineTuiConfig config) throws Exception {
        Backend backend = BackendFactory.create();
        InlineDisplay display;

        try {
            // Enable raw mode for key events
            backend.enableRawMode();

            // Create inline display using shared backend (no alternate screen)
            int width = backend.size().width();
            display = InlineDisplay.withBackend(config.height(), width, backend);
            if (config.clearOnClose()) {
                display.clearOnClose();
            }

            InlineViewport viewport = new InlineViewport(display);
            return new InlineTuiRunner(backend, viewport, config);
        } catch (Exception e) {
            try {
                backend.disableRawMode();
            } catch (Exception ignored) {
            }
            backend.close();
            throw e;
        }
    }

    /**
     * Runs the main event loop with the given handler and renderer.
     * <p>
     * The event handler is called for each event (key, mouse, tick).
     * The renderer is called to update the display when the handler returns true.
     *
     * @param handler  the event handler
     * @param renderer the UI renderer
     * @throws Exception if an error occurs during execution
     */
    public void run(InlineEventHandler handler, Renderer renderer) throws Exception {
        // Mark this thread as the render thread
        RenderThread.setRenderThread(Thread.currentThread());

        try {
            // Initial draw
            viewport.draw(renderer::render);

            while (running.get()) {
                Event event = pollEvent(config.pollTimeout());
                if (event != null) {
                    // Handle UiRunnable events (scheduled work from other threads)
                    if (event instanceof UiRunnable) {
                        ((UiRunnable) event).run();
                        continue;
                    }

                    boolean shouldRedraw = handler.handle(event, this);
                    if (shouldRedraw && running.get()) {
                        viewport.draw(renderer::render);
                    }
                }
            }
        } finally {
            RenderThread.clearRenderThread();
        }
    }

    /**
     * Polls for the next event with the specified timeout.
     *
     * @param timeout the maximum time to wait
     * @return the next event, or null if timeout expires
     */
    public Event pollEvent(Duration timeout) {
        try {
            // Prioritize input events over tick events
            Event event = findInputEvent();
            if (event != null) {
                return event;
            }

            return eventQueue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Searches the queue for an input event (non-tick), removing and returning it.
     */
    private Event findInputEvent() {
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

        for (Event tick : ticks) {
            eventQueue.offer(tick);
        }

        return inputEvent;
    }

    /**
     * Prints a plain text message above the viewport.
     *
     * @param message the message to print
     */
    public void println(String message) {
        viewport.println(message);
    }

    /**
     * Prints styled text above the viewport.
     *
     * @param text the styled text to print
     */
    public void println(Text text) {
        viewport.println(text);
    }

    /**
     * Sets the content height for the next draw.
     * <p>
     * This controls how many terminal lines are allocated for the inline display.
     * Calling this before rendering allows the display to grow or shrink dynamically.
     *
     * @param height the desired content height in lines
     */
    public void setContentHeight(int height) {
        viewport.setContentHeight(height);
    }

    /**
     * Executes an action on the render thread.
     * <p>
     * If called from the render thread, the action is executed immediately.
     * If called from another thread, the action is queued for execution.
     *
     * @param action the action to execute
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
     * the action even if called from the render thread.
     *
     * @param action the action to execute
     */
    public void runLater(Runnable action) {
        eventQueue.offer(new UiRunnable(action));
    }

    /**
     * Signals the runner to stop.
     */
    public void quit() {
        running.set(false);
    }

    /**
     * Returns whether the runner is still running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Returns the viewport width.
     *
     * @return the width in characters
     */
    public int width() {
        return viewport.width();
    }

    /**
     * Returns the viewport height.
     *
     * @return the height in lines
     */
    public int height() {
        return viewport.height();
    }

    /**
     * Draws the UI using the given renderer.
     *
     * @param renderer the render function
     */
    public void draw(Consumer<Frame> renderer) {
        viewport.draw(renderer);
    }

    /**
     * Returns the shared scheduler for scheduling tasks.
     * <p>
     * This scheduler runs on a dedicated daemon thread. Tasks scheduled here
     * execute on the scheduler thread, not the render thread. To modify UI state
     * from a scheduled task, use {@link #runOnRenderThread(Runnable)}.
     *
     * @return the scheduler (never null)
     */
    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    /**
     * Scheduler callback that generates tick events.
     */
    private void schedulerCallback() {
        if (!running.get()) {
            return;
        }

        if (config.ticksEnabled() && config.tickRate() != null) {
            Instant now = Instant.now();
            Instant targetTime = nextTickTime.get();

            if (targetTime != null && !now.isBefore(targetTime)) {
                Instant previous = lastTick.getAndSet(now);
                Duration elapsed = Duration.between(previous, now);

                nextTickTime.set(targetTime.plus(config.tickRate()));

                long frame = frameCount.incrementAndGet();
                eventQueue.offer(TickEvent.of(frame, elapsed));
            }
        }
    }

    /**
     * Closes this runner and releases resources.
     * <p>
     * If a scheduler was provided via configuration, it is NOT shut down -
     * the caller retains ownership and is responsible for its lifecycle.
     * If no scheduler was provided, the internally-created scheduler is shut down.
     */
    @Override
    public void close() {
        running.set(false);

        // Stop input reader thread
        if (inputReader != null) {
            inputReader.stop(config.pollTimeout().toMillis() * 2);
        }

        // Remove shutdown hook
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (IllegalStateException e) {
            // JVM is already shutting down
        }

        // Shutdown scheduler only if we own it
        if (schedulerOwned) {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        cleanup();
    }

    /**
     * Performs cleanup. This is idempotent and safe to call multiple times.
     */
    private void cleanup() {
        if (!cleanedUp.compareAndSet(false, true)) {
            return;
        }

        try {
            viewport.release();
        } catch (Exception ignored) {
        }

        try {
            backend.disableRawMode();
        } catch (Exception ignored) {
        }

        try {
            viewport.close();
        } catch (Exception ignored) {
        }

        try {
            backend.close();
        } catch (Exception ignored) {
        }
    }
}

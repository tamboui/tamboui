/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import dev.tamboui.layout.Size;
import dev.tamboui.terminal.Backend;
import dev.tamboui.terminal.BackendFactory;
import dev.tamboui.terminal.Frame;
import dev.tamboui.terminal.Terminal;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.EventParser;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.ResizeEvent;
import dev.tamboui.tui.event.TickEvent;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
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
    private final ScheduledExecutorService tickScheduler;
    private final AtomicLong frameCount;
    private final Thread shutdownHook;
    private volatile Instant lastTick;
    private volatile Size lastSize;

    private TuiRunner(Backend backend, Terminal<Backend> terminal, TuiConfig config) {
        this.backend = backend;
        this.terminal = terminal;
        this.config = config;
        this.eventQueue = new LinkedBlockingQueue<>();
        this.running = new AtomicBoolean(true);
        this.cleanedUp = new AtomicBoolean(false);
        this.frameCount = new AtomicLong(0);
        this.lastTick = Instant.now();

        // Initialize last known size
        try {
            Size size = backend.size();
            this.lastSize = size;
        } catch (IOException e) {
            this.lastSize = new Size(80, 24); // Fallback default
        }

        // Set up resize handler
        backend.onResize(() -> {
            try {
                Size newSize = backend.size();
                if (!newSize.equals(lastSize)) {
                    lastSize = newSize;
                    eventQueue.offer(ResizeEvent.of(newSize.width(), newSize.height()));
                }
            } catch (IOException e) {
                // Ignore resize errors
            }
        });

        // Set up tick scheduler if configured
        if (config.ticksEnabled()) {
            this.tickScheduler = new ScheduledThreadPoolExecutor(1, r -> {
                Thread t = new Thread(r, "tui-tick");
                t.setDaemon(true);
                return t;
            });
            long periodMs = config.tickRate().toMillis();
            tickScheduler.scheduleAtFixedRate(this::generateTick, periodMs, periodMs, TimeUnit.MILLISECONDS);
        } else {
            this.tickScheduler = null;
        }

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
     *
     * @param handler  the event handler
     * @param renderer the UI renderer
     * @throws Exception if an error occurs during execution
     */
    public void run(EventHandler handler, Renderer renderer) throws Exception {
        // Initial draw
        terminal.draw(renderer::render);

        while (running.get()) {
            Event event = pollEvent(config.pollTimeout());

            if (event != null) {
                boolean shouldRedraw = handler.handle(event, this);
                if (shouldRedraw && running.get()) {
                    terminal.draw(renderer::render);
                }
            }
        }
    }

    /**
     * Polls for the next event with the specified timeout.
     *
     * @param timeout the maximum time to wait
     * @return the next event, or null if timeout expires
     */
    public Event pollEvent(Duration timeout) {
        // Check queue first (for resize and tick events)
        Event queued = eventQueue.poll();
        if (queued != null) {
            return queued;
        }

        // Read from terminal
        try {
            return EventParser.readEvent(backend, (int) timeout.toMillis(), config.keyMap());
        } catch (IOException e) {
            return null;
        }
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
     * @throws IOException if an I/O error occurs
     */
    public void draw(Consumer<Frame> renderer) throws IOException {
        terminal.draw(renderer);
    }

    /**
     * Returns true if the runner is still running.
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
     * Returns the underlying terminal.
     */
    public Terminal<Backend> terminal() {
        return terminal;
    }

    /**
     * Returns the underlying backend.
     */
    public Backend backend() {
        return backend;
    }

    private void generateTick() {
        if (!running.get()) {
            return;
        }

        Instant now = Instant.now();
        Duration elapsed = Duration.between(lastTick, now);
        lastTick = now;

        long frame = frameCount.incrementAndGet();
        eventQueue.offer(TickEvent.of(frame, elapsed));
    }

    @Override
    public void close() {
        running.set(false);

        // Remove shutdown hook if it was registered (prevents it from running during normal close)
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            } catch (IllegalStateException e) {
                // JVM is already shutting down, hook will run anyway
            }
        }

        // Shutdown tick scheduler
        if (tickScheduler != null) {
            tickScheduler.shutdownNow();
            try {
                tickScheduler.awaitTermination(100, TimeUnit.MILLISECONDS);
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
}

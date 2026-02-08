/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.pilot;

import java.io.IOException;
import java.time.Duration;

import dev.tamboui.layout.Size;
import dev.tamboui.terminal.TestBackend;
import dev.tamboui.tui.EventHandler;
import dev.tamboui.tui.Renderer;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;

/**
 * Test runner for TuiRunner applications.
 * Runs the application with a headless TestBackend in a background thread
 * and provides a {@link Pilot} to drive it.
 */
public final class TuiTestRunner implements TestRunner {

    private static final Size DEFAULT_SIZE = new Size(80, 24);

    private final TuiRunner tuiRunner;
    private final TestBackend backend;
    private final TuiPilot pilot;
    private final Thread runnerThread;
    private volatile boolean running = true;

    private TuiTestRunner(TuiRunner tuiRunner, TestBackend backend, TuiPilot pilot,
                          EventHandler handler, Renderer renderer) {
        this.tuiRunner = tuiRunner;
        this.backend = backend;
        this.pilot = pilot;
        this.runnerThread = new Thread(() -> {
            try {
                tuiRunner.run(handler, renderer);
            } catch (Exception e) {
                throw new RuntimeException("TuiTestRunner failed", e);
            }
        }, "tui-test-runner");
        this.runnerThread.setDaemon(true);
    }

    /**
     * Creates a test runner with the given handler and renderer (default size 80x24).
     *
     * @param handler  the event handler
     * @param renderer the renderer
     * @return the test runner (already started)
     * @throws Exception if initialization fails
     */
    public static TuiTestRunner runTest(EventHandler handler, Renderer renderer) throws Exception {
        return runTest(handler, renderer, DEFAULT_SIZE);
    }

    /**
     * Creates a test runner with the given handler, renderer, and terminal size.
     *
     * @param handler  the event handler
     * @param renderer the renderer
     * @param size     the terminal size
     * @return the test runner (already started)
     * @throws Exception if initialization fails
     */
    public static TuiTestRunner runTest(EventHandler handler, Renderer renderer, Size size) throws Exception {
        TestBackend backend = new TestBackend(size.width(), size.height());
        TuiConfig config = TuiConfig.builder()
                .rawMode(false)
                .alternateScreen(false)
                .hideCursor(false)
                .mouseCapture(true)
                .shutdownHook(false)
                .pollTimeout(Duration.ofMillis(10))
                .noTick()
                .backend(backend)
                .build();
        return runTest(handler, renderer, size, config);
    }

    /**
     * Creates a test runner with the given handler, renderer, size, and config.
     * The config must have a {@link TestBackend} as its backend (so the runner can inject events).
     *
     * @param handler  the event handler
     * @param renderer the renderer
     * @param size     the terminal size (used to validate; backend in config should match)
     * @param config   the TUI config (must use a TestBackend)
     * @return the test runner (already started)
     * @throws Exception if initialization fails
     */
    public static TuiTestRunner runTest(EventHandler handler, Renderer renderer, Size size, TuiConfig config) throws Exception {
        if (!(config.backend() instanceof TestBackend)) {
            throw new IllegalArgumentException("TuiTestRunner requires config.backend() to be a TestBackend");
        }
        TestBackend backend = (TestBackend) config.backend();
        TuiRunner tuiRunner = TuiRunner.create(config);
        TuiPilot pilot = new TuiPilot(tuiRunner, backend);

        TuiTestRunner runner = new TuiTestRunner(tuiRunner, backend, pilot, handler, renderer);
        runner.runnerThread.start();

        // Allow runner thread to start and complete initial render before returning,
        // so pilot() and first interactions see a ready app.
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return runner;
    }

    @Override
    public Pilot pilot() {
        return pilot;
    }

    /**
     * Returns the underlying TuiRunner.
     *
     * @return the TuiRunner
     */
    public TuiRunner runner() {
        return tuiRunner;
    }

    /**
     * Returns the test backend.
     *
     * @return the TestBackend
     */
    public TestBackend backend() {
        return backend;
    }

    @Override
    public void close() throws IOException {
        if (running) {
            running = false;
            tuiRunner.quit();
            try {
                runnerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while closing test runner", e);
            }
            tuiRunner.close();
        }
    }
}

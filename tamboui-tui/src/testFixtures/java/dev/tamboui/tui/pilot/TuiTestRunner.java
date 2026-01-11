/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.pilot;

import dev.tamboui.layout.Size;
import dev.tamboui.tui.EventHandler;
import dev.tamboui.tui.Renderer;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.pilot.Pilot;
import dev.tamboui.tui.pilot.TestBackend;
import dev.tamboui.tui.pilot.TestRunner;

import java.io.IOException;

/**
 * Test runner for TUI applications.
 * <p>
 * Provides a headless testing environment similar to Textual's {@code run_test} method.
 * The test runner uses a {@link TestBackend} that doesn't interact with a real terminal,
 * making it suitable for automated testing.
 *
 * <pre>{@code
 * try (TuiTestRunner test = TuiTestRunner.runTest(handler, renderer)) {
 *     Pilot pilot = test.pilot();
 *     pilot.press('q');
 *     pilot.press(KeyCode.ESCAPE);
 *     pilot.click(10, 5);
 *     pilot.resize(100, 30);
 * }
 * }</pre>
 */
public final class TuiTestRunner implements TestRunner {

    private final TuiRunner runner;
    private final TestBackend backend;
    private final TuiPilot pilot;
    private final Thread runnerThread;
    private volatile boolean running;

    private final EventHandler handler;
    private final Renderer renderer;

    private TuiTestRunner(TuiRunner runner, TestBackend backend, TuiPilot pilot, EventHandler handler, Renderer renderer) {
        this.runner = runner;
        this.backend = backend;
        this.pilot = pilot;
        this.handler = handler;
        this.renderer = renderer;
        this.running = true;
        this.runnerThread = new Thread(() -> {
            try {
                runner.run(handler, renderer);
            } catch (Exception e) {
                // Handle exceptions during test run
                throw new RuntimeException("Test runner failed", e);
            }
        }, "test-runner");
        runnerThread.setDaemon(true);
    }

    /**
     * Creates a test runner with the given event handler and renderer.
     *
     * @param handler the event handler
     * @param renderer the renderer
     * @return a test runner instance
     * @throws Exception if initialization fails
     */
    public static TuiTestRunner runTest(EventHandler handler, Renderer renderer) throws Exception {
        return runTest(handler, renderer, new Size(80, 24));
    }

    /**
     * Creates a test runner with the given event handler, renderer, and terminal size.
     *
     * @param handler the event handler
     * @param renderer the renderer
     * @param size the terminal size
     * @return a test runner instance
     * @throws Exception if initialization fails
     */
    public static TuiTestRunner runTest(EventHandler handler, Renderer renderer, Size size) throws Exception {
        return runTest(handler, renderer, size, TuiConfig.builder()
            .rawMode(false) // Not needed for testing
            .alternateScreen(false) // Not needed for testing
            .hideCursor(false) // Not needed for testing
            .mouseCapture(true) // Enable for mouse testing
            .shutdownHook(false) // Not needed for testing
            .pollTimeout(java.time.Duration.ofMillis(10)) // Short timeout for faster event processing in tests
            .noTick() // Disable ticks unless needed
            .build());
    }

    /**
     * Creates a test runner with the given configuration.
     *
     * @param handler the event handler
     * @param renderer the renderer
     * @param size the terminal size
     * @param config the TUI configuration
     * @return a test runner instance
     * @throws Exception if initialization fails
     */
    public static TuiTestRunner runTest(EventHandler handler, Renderer renderer, Size size, TuiConfig config) throws Exception {
        TestBackend backend = new TestBackend(size);
        TuiRunner runner = TuiRunner.create(backend, config);
        TuiPilot pilot = new TuiPilot(runner, backend);

        TuiTestRunner testRunner = new TuiTestRunner(runner, backend, pilot, handler, renderer);

        // Start the runner in a separate thread
        testRunner.runnerThread.start();

        // Wait a bit for initialization
        Thread.sleep(50);

        return testRunner;
    }

    @Override
    public Pilot pilot() {
        return pilot;
    }

    /**
     * Returns the underlying TUI runner.
     *
     * @return the TUI runner
     */
    public TuiRunner runner() {
        return runner;
    }

    /**
     * Returns the test backend.
     *
     * @return the test backend
     */
    public TestBackend backend() {
        return backend;
    }

    @Override
    public void close() throws IOException {
        if (running) {
            running = false;
            runner.quit();
            try {
                runnerThread.join(1000); // Wait up to 1 second for thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Convert to IOException to satisfy AutoCloseable contract
                throw new IOException("Interrupted while closing test runner", e);
            }
            runner.close();
        }
    }
}

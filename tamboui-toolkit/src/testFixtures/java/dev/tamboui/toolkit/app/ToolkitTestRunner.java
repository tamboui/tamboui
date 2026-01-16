/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.app;

import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Size;
import dev.tamboui.terminal.Backend;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.pilot.Pilot;
import dev.tamboui.tui.pilot.TestBackend;
import dev.tamboui.tui.pilot.TestRunner;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Test runner for ToolkitRunner applications.
 * <p>
 * Provides a headless testing environment for ToolkitRunner applications,
 * with support for widget selection by element ID.
 *
 * <pre>{@code
 * Supplier&lt;Element&gt; elementSupplier = () -&gt; panel("Hello", text("World"));
 *
 * try (ToolkitTestRunner test = ToolkitTestRunner.runTest(elementSupplier)) {
 *     Pilot pilot = test.pilot();
 *     pilot.press('q');
 *     pilot.click("#button-id");  // Widget selection works!
 *     pilot.doubleClick("#item");
 * }
 * }</pre>
 */
public final class ToolkitTestRunner implements TestRunner {

    private final ToolkitRunner runner;
    private final TestBackend backend;
    private final ToolkitPilot pilot;
    private final Thread runnerThread;
    private volatile boolean running;

    private final Supplier<Element> elementSupplier;
    private final TuiConfig config;

    private ToolkitTestRunner(ToolkitRunner runner, TestBackend backend, ToolkitPilot pilot,
                              Supplier<Element> elementSupplier, TuiConfig config) {
        this.runner = runner;
        this.backend = backend;
        this.pilot = pilot;
        this.elementSupplier = elementSupplier;
        this.config = config;
        this.running = true;
        this.runnerThread = new Thread(() -> {
            try {
                runner.run(elementSupplier);
            } catch (Exception e) {
                // Handle exceptions during test run
                throw new RuntimeException("Test runner failed", e);
            }
        }, "toolkit-test-runner");
        runnerThread.setDaemon(true);
    }

    /**
     * Creates a test runner with the given element supplier.
     *
     * @param elementSupplier provides the root element for each render
     * @return a test runner instance
     * @throws Exception if initialization fails
     */
    public static ToolkitTestRunner runTest(Supplier<Element> elementSupplier) throws Exception {
        return runTest(elementSupplier, new Size(80, 24));
    }

    /**
     * Creates a test runner with the given element supplier and terminal size.
     *
     * @param elementSupplier provides the root element for each render
     * @param size the terminal size
     * @return a test runner instance
     * @throws Exception if initialization fails
     */
    public static ToolkitTestRunner runTest(Supplier<Element> elementSupplier, Size size) throws Exception {
        return runTest(elementSupplier, size, TuiConfig.builder()
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
     * @param elementSupplier provides the root element for each render
     * @param size the terminal size
     * @param config the TUI configuration
     * @return a test runner instance
     * @throws Exception if initialization fails
     */
    public static ToolkitTestRunner runTest(Supplier<Element> elementSupplier, Size size, TuiConfig config) throws Exception {
        TestBackend backend = new TestBackend(size);
        ToolkitRunner runner = ToolkitRunner.create(backend, config);
        ToolkitPilot pilot = new ToolkitPilot(runner.tuiRunner(), backend, runner.focusManager());

        ToolkitTestRunner testRunner = new ToolkitTestRunner(runner, backend, pilot, elementSupplier, config);

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
     * Returns the underlying ToolkitRunner.
     *
     * @return the ToolkitRunner
     */
    public ToolkitRunner runner() {
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

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.app;

import java.time.Duration;
import java.util.function.Supplier;

import dev.tamboui.error.RuntimeIOException;
import dev.tamboui.layout.Size;
import dev.tamboui.terminal.TestBackend;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.error.TuiException;
import dev.tamboui.tui.pilot.Pilot;
import dev.tamboui.tui.pilot.TestRunner;

/**
 * Test runner for ToolkitRunner applications.
 * Runs the application with a headless TestBackend in a background thread
 * and provides a {@link Pilot} with widget selection by element ID.
 */
public final class ToolkitTestRunner implements TestRunner {

    private static final Size DEFAULT_SIZE = new Size(80, 24);
    private static final int CLOSE_TIMEOUT_MS = 1000;

    private final ToolkitRunner runner;
    private final TestBackend backend;
    private final ToolkitPilot pilot;
    private final Thread runnerThread;
    private volatile boolean running = true;

    private ToolkitTestRunner(ToolkitRunner runner, TestBackend backend, ToolkitPilot pilot,
                              Supplier<Element> elementSupplier) {
        this.runner = runner;
        this.backend = backend;
        this.pilot = pilot;
        this.runnerThread = new Thread(() -> {
            try {
                runner.run(elementSupplier);
            } catch (Exception e) {
                throw new TuiException("ToolkitTestRunner failed", e);
            }
        }, "toolkit-test-runner");
        this.runnerThread.setDaemon(true);
    }

    /**
     * Creates a test runner with the given element supplier (default size 80x24).
     *
     * @param elementSupplier provides the root element for each render
     * @return the test runner (already started)
     * @throws Exception if initialization fails
     */
    public static ToolkitTestRunner runTest(Supplier<Element> elementSupplier) throws Exception {
        return runTest(elementSupplier, DEFAULT_SIZE);
    }

    /**
     * Creates a test runner with the given element supplier and terminal size.
     *
     * @param elementSupplier provides the root element for each render
     * @param size            the terminal size
     * @return the test runner (already started)
     * @throws Exception if initialization fails
     */
    public static ToolkitTestRunner runTest(Supplier<Element> elementSupplier, Size size) throws Exception {
        return runTest(elementSupplier, size, testConfig(new TestBackend(size.width(), size.height())));
    }

    /**
     * Creates a test runner with the given element supplier, size, and config.
     *
     * @param elementSupplier provides the root element for each render
     * @param size            the terminal size
     * @param config          the TUI config (should include backend from TestBackend for headless run)
     * @return the test runner (already started)
     * @throws Exception if initialization fails
     */
    public static ToolkitTestRunner runTest(Supplier<Element> elementSupplier, Size size, TuiConfig config) throws Exception {
        final TestBackend backend;
        final TuiConfig configToUse;
        if (config.backend() instanceof TestBackend) {
            backend = (TestBackend) config.backend();
            configToUse = config;
        } else {
            backend = new TestBackend(size.width(), size.height());
            configToUse = testConfig(backend);
        }
        ToolkitRunner runner = ToolkitRunner.create(configToUse);
        ToolkitPilot pilot = new ToolkitPilot(runner, backend);

        ToolkitTestRunner testRunner = new ToolkitTestRunner(runner, backend, pilot, elementSupplier);
        testRunner.runnerThread.start();

        // Allow runner thread to start and complete initial render before returning,
        // so pilot() and first interactions see a ready app and populated ElementRegistry.
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return testRunner;
    }

    private static TuiConfig testConfig(TestBackend backend) {
        return TuiConfig.builder()
                .rawMode(false)
                .alternateScreen(false)
                .hideCursor(false)
                .mouseCapture(true)
                .shutdownHook(false)
                .pollTimeout(Duration.ofMillis(10))
                .noTick()
                .backend(backend)
                .build();
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
     * @return the TestBackend
     */
    public TestBackend backend() {
        return backend;
    }

    @Override
    public void close() throws RuntimeIOException {
        if (running) {
            running = false;
            runner.quit();
            try {
                runnerThread.join(CLOSE_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeIOException("Interrupted while closing test runner", e);
            }
            runner.close();
        }
    }
}

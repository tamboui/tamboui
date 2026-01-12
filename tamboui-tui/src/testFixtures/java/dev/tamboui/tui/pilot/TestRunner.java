/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.pilot;

/**
 * Interface for test runners that provide a Pilot for testing.
 * <p>
 * Implementations are provided by each module (TuiRunner, ToolkitRunner).
 * This interface defines the common contract that all test runners must follow.
 *
 * <pre>{@code
 * try (TestRunner test = TestRunner.runTest(...)) {
 *     Pilot pilot = test.pilot();
 *     pilot.press('q');
 * }
 * }</pre>
 */
@SuppressWarnings("try")
public interface TestRunner extends AutoCloseable {
    /**
     * Returns the pilot for controlling the test.
     *
     * @return the pilot
     */
    Pilot pilot();
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.pilot;

import java.io.IOException;

/**
 * Test runner that hosts a TUI application with a test backend and provides
 * a {@link Pilot} to drive it programmatically.
 */
public interface TestRunner extends AutoCloseable {

    /**
     * Returns the pilot for driving the application.
     *
     * @return the pilot
     */
    Pilot pilot();

    /**
     * Stops the application and releases resources.
     *
     * @throws IOException if an error occurs during close
     */
    @Override
    void close() throws IOException;
}

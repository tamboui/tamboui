/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.error;

import java.io.PrintStream;

/**
 * Context provided to error handlers when a render error occurs.
 * <p>
 * Provides access to error output streams and control over the runner.
 */
public interface ErrorContext {

    /**
     * Returns the output stream for logging fatal errors.
     * <p>
     * This stream is configured via {@code TuiConfig.errorOutput()} and defaults to
     * {@code System.err} (captured at config creation time). When the TUI has
     * captured standard streams, this provides a way to log errors to an external
     * destination.
     *
     * @return the error output stream
     */
    PrintStream errorOutput();

    /**
     * Signals the runner to stop.
     * <p>
     * Call this when the error handler determines the application should quit. The
     * runner will perform cleanup and exit the main loop.
     */
    void quit();
}

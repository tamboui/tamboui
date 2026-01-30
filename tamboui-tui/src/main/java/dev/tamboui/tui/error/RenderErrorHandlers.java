/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.error;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

/**
 * Factory methods for common error handler implementations.
 * <p>
 * Provides pre-built handlers for common error handling scenarios:
 * <ul>
 * <li>{@link #displayAndQuit()} - Default: show error in UI, wait for
 * dismissal</li>
 * <li>{@link #logAndQuit(PrintStream)} - Log to stream and quit
 * immediately</li>
 * <li>{@link #writeToFile(Path)} - Write to file then display in UI</li>
 * </ul>
 */
public final class RenderErrorHandlers {

    private RenderErrorHandlers() {
        // Factory class - no instantiation
    }

    /**
     * Returns the default handler that displays the error in the UI.
     * <p>
     * The error display shows the exception type, message, and a scrollable stack
     * trace. Users can press 'q' to quit or use arrow keys to scroll.
     *
     * @return the default display-and-quit handler
     */
    public static RenderErrorHandler displayAndQuit() {
        return DisplayAndQuitHandler.INSTANCE;
    }

    /**
     * Returns a handler that logs the error to a stream and quits immediately.
     * <p>
     * The full stack trace is printed to the provided stream before the runner
     * quits. The terminal is cleaned up before printing.
     *
     * @param output
     *            the output stream to log to
     * @return a log-and-quit handler
     */
    public static RenderErrorHandler logAndQuit(PrintStream output) {
        return new LogAndQuitHandler(output);
    }

    /**
     * Returns a handler that writes the error to a file, then displays in the UI.
     * <p>
     * This is useful when you want both a log file for debugging and immediate user
     * feedback. If the file cannot be written, the handler falls back to just
     * displaying in the UI.
     *
     * @param logFile
     *            the path to write the error log
     * @return a write-to-file-then-display handler
     */
    public static RenderErrorHandler writeToFile(Path logFile) {
        return new WriteToFileHandler(logFile);
    }

    /**
     * Returns a handler that suppresses errors and continues.
     * <p>
     * <strong>Warning:</strong> This is dangerous and should only be used in
     * specific scenarios where you understand the implications. Errors are logged
     * to the error output but rendering continues.
     *
     * @return a suppress handler
     */
    public static RenderErrorHandler suppress() {
        return SuppressHandler.INSTANCE;
    }

    // Default handler - displays in UI
    private static final class DisplayAndQuitHandler implements RenderErrorHandler {
        static final DisplayAndQuitHandler INSTANCE = new DisplayAndQuitHandler();

        @Override
        public ErrorAction handle(RenderError error, ErrorContext context) {
            return ErrorAction.DISPLAY_AND_QUIT;
        }
    }

    // Handler that logs to a stream and quits
    private static final class LogAndQuitHandler implements RenderErrorHandler {
        private final PrintStream output;

        LogAndQuitHandler(PrintStream output) {
            this.output = output;
        }

        @Override
        public ErrorAction handle(RenderError error, ErrorContext context) {
            output.println("=== TamboUI Render Error ===");
            output.println("Type: " + error.fullExceptionType());
            output.println("Message: " + error.message());
            output.println("Timestamp: " + error.timestamp());
            output.println();
            output.println(error.formattedStackTrace());
            output.flush();
            return ErrorAction.QUIT_IMMEDIATELY;
        }
    }

    // Handler that writes to file then displays
    private static final class WriteToFileHandler implements RenderErrorHandler {
        private final Path logFile;

        WriteToFileHandler(Path logFile) {
            this.logFile = logFile;
        }

        @Override
        public ErrorAction handle(RenderError error, ErrorContext context) {
            try {
                PrintStream fileOut = new PrintStream(new FileOutputStream(logFile.toFile(), true));
                try {
                    fileOut.println("=== TamboUI Render Error ===");
                    fileOut.println("Type: " + error.fullExceptionType());
                    fileOut.println("Message: " + error.message());
                    fileOut.println("Timestamp: " + error.timestamp());
                    fileOut.println();
                    fileOut.println(error.formattedStackTrace());
                    fileOut.println();
                    fileOut.flush();
                } finally {
                    fileOut.close();
                }
            } catch (IOException e) {
                // Failed to write to file - log to error output
                context.errorOutput().println(
                        "Warning: Could not write error to " + logFile + ": " + e.getMessage());
            }
            return ErrorAction.DISPLAY_AND_QUIT;
        }
    }

    // Handler that suppresses errors (dangerous)
    private static final class SuppressHandler implements RenderErrorHandler {
        static final SuppressHandler INSTANCE = new SuppressHandler();

        @Override
        public ErrorAction handle(RenderError error, ErrorContext context) {
            context.errorOutput().println("Warning: Suppressed render error: " + error.message());
            return ErrorAction.SUPPRESS;
        }
    }
}

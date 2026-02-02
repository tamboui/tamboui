/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.error;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.*;

class RenderErrorHandlersTest {

    @Test
    @DisplayName("displayAndQuit() returns DISPLAY_AND_QUIT action")
    void displayAndQuitReturnsDisplayAction() {
        RenderErrorHandler handler = RenderErrorHandlers.displayAndQuit();
        RenderError error = RenderError.from(new RuntimeException("test"));
        ErrorContext context = createMockContext();

        ErrorAction action = handler.handle(error, context);

        assertThat(action).isEqualTo(ErrorAction.DISPLAY_AND_QUIT);
    }

    @Test
    @DisplayName("logAndQuit() logs to output and returns QUIT_IMMEDIATELY")
    void logAndQuitLogsAndReturnsQuitAction() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(output);
        RenderErrorHandler handler = RenderErrorHandlers.logAndQuit(printStream);
        RenderError error = RenderError.from(new RuntimeException("Test error message"));
        ErrorContext context = createMockContext();

        ErrorAction action = handler.handle(error, context);

        assertThat(action).isEqualTo(ErrorAction.QUIT_IMMEDIATELY);
        String logged = output.toString();
        assertThat(logged)
                .contains("TamboUI Render Error")
                .contains("RuntimeException")
                .contains("Test error message");
    }

    @Test
    @DisplayName("writeToFile() writes error to file and returns DISPLAY_AND_QUIT")
    void writeToFileWritesAndReturnsDisplayAction(@TempDir Path tempDir) throws Exception {
        Path logFile = tempDir.resolve("error.log");
        RenderErrorHandler handler = RenderErrorHandlers.writeToFile(logFile);
        RenderError error = RenderError.from(new RuntimeException("File test error"));
        ErrorContext context = createMockContext();

        ErrorAction action = handler.handle(error, context);

        assertThat(action).isEqualTo(ErrorAction.DISPLAY_AND_QUIT);
        assertThat(logFile).exists();
        String content = new String(Files.readAllBytes(logFile), StandardCharsets.UTF_8);
        assertThat(content)
                .contains("TamboUI Render Error")
                .contains("RuntimeException")
                .contains("File test error");
    }

    @Test
    @DisplayName("writeToFile() falls back gracefully on write failure")
    void writeToFileFallsBackOnFailure() {
        // Use an invalid path that will fail
        Path invalidPath = Paths.get("/nonexistent/directory/error.log");
        RenderErrorHandler handler = RenderErrorHandlers.writeToFile(invalidPath);
        RenderError error = RenderError.from(new RuntimeException("test"));
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        ErrorContext context = createMockContext(new PrintStream(errorOutput));

        ErrorAction action = handler.handle(error, context);

        // Should still return DISPLAY_AND_QUIT even on file write failure
        assertThat(action).isEqualTo(ErrorAction.DISPLAY_AND_QUIT);
        assertThat(errorOutput.toString()).contains("Warning");
    }

    @Test
    @DisplayName("suppress() returns SUPPRESS action")
    void suppressReturnsSuppressAction() {
        RenderErrorHandler handler = RenderErrorHandlers.suppress();
        RenderError error = RenderError.from(new RuntimeException("test"));
        ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();
        ErrorContext context = createMockContext(new PrintStream(errorOutput));

        ErrorAction action = handler.handle(error, context);

        assertThat(action).isEqualTo(ErrorAction.SUPPRESS);
        assertThat(errorOutput.toString()).contains("Suppressed render error");
    }

    private ErrorContext createMockContext() {
        return createMockContext(new PrintStream(new ByteArrayOutputStream()));
    }

    private ErrorContext createMockContext(PrintStream errorOutput) {
        return new ErrorContext() {
            @Override
            public PrintStream errorOutput() {
                return errorOutput;
            }

            @Override
            public void quit() {
                // No-op for testing
            }
        };
    }
}

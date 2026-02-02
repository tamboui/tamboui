/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RenderErrorTest {

    @Test
    @DisplayName("from() creates RenderError with cause")
    void fromCreateRenderErrorWithCause() {
        RuntimeException cause = new RuntimeException("Test error");

        RenderError error = RenderError.from(cause);

        assertThat(error.cause()).isSameAs(cause);
    }

    @Test
    @DisplayName("from() captures timestamp")
    void fromCapturesTimestamp() {
        long before = System.currentTimeMillis();
        RenderError error = RenderError.from(new RuntimeException("Test"));
        long after = System.currentTimeMillis();

        assertThat(error.timestamp()).isBetween(before, after);
    }

    @Test
    @DisplayName("from() formats stack trace")
    void fromFormatsStackTrace() {
        RuntimeException cause = new RuntimeException("Test error");

        RenderError error = RenderError.from(cause);

        assertThat(error.formattedStackTrace())
                .contains("RuntimeException")
                .contains("Test error")
                .contains("at ");
    }

    @Test
    @DisplayName("message() returns exception message")
    void messageReturnsExceptionMessage() {
        RenderError error = RenderError.from(new RuntimeException("Custom message"));

        assertThat(error.message()).isEqualTo("Custom message");
    }

    @Test
    @DisplayName("message() returns class name when no message")
    void messageReturnsClassNameWhenNoMessage() {
        RenderError error = RenderError.from(new NullPointerException());

        assertThat(error.message()).isEqualTo("java.lang.NullPointerException");
    }

    @Test
    @DisplayName("exceptionType() returns simple class name")
    void exceptionTypeReturnsSimpleClassName() {
        RenderError error = RenderError.from(new IllegalStateException("test"));

        assertThat(error.exceptionType()).isEqualTo("IllegalStateException");
    }

    @Test
    @DisplayName("fullExceptionType() returns full class name")
    void fullExceptionTypeReturnsFullClassName() {
        RenderError error = RenderError.from(new IllegalArgumentException("test"));

        assertThat(error.fullExceptionType()).isEqualTo("java.lang.IllegalArgumentException");
    }

    @Test
    @DisplayName("from() throws NullPointerException for null cause")
    void fromThrowsForNullCause() {
        assertThatNullPointerException()
                .isThrownBy(() -> RenderError.from(null));
    }

    @Test
    @DisplayName("toString() includes type, message and timestamp")
    void toStringIncludesRelevantInfo() {
        RenderError error = RenderError.from(new RuntimeException("Test"));

        assertThat(error.toString())
                .contains("RenderError")
                .contains("RuntimeException")
                .contains("Test");
    }
}

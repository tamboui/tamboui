/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

/**
 * Immutable container for render error information.
 * <p>
 * Captures the exception that occurred during rendering along with a timestamp
 * and a pre-formatted stack trace for display.
 */
public final class RenderError {

    private final Throwable cause;
    private final long timestamp;
    private final String formattedStackTrace;

    private RenderError(Throwable cause, long timestamp, String formattedStackTrace) {
        this.cause = cause;
        this.timestamp = timestamp;
        this.formattedStackTrace = formattedStackTrace;
    }

    /**
     * Creates a RenderError from the given exception.
     *
     * @param cause
     *            the exception that occurred
     * @return a new RenderError
     * @throws NullPointerException
     *             if cause is null
     */
    public static RenderError from(Throwable cause) {
        Objects.requireNonNull(cause, "cause");
        return new RenderError(cause, System.currentTimeMillis(), formatStackTrace(cause));
    }

    /**
     * Returns the original exception that caused the error.
     *
     * @return the cause
     */
    public Throwable cause() {
        return cause;
    }

    /**
     * Returns the timestamp when the error was captured.
     *
     * @return the timestamp in milliseconds since epoch
     */
    public long timestamp() {
        return timestamp;
    }

    /**
     * Returns a pre-formatted stack trace suitable for display.
     *
     * @return the formatted stack trace
     */
    public String formattedStackTrace() {
        return formattedStackTrace;
    }

    /**
     * Returns the error message from the cause, or the class name if no message.
     *
     * @return the error message
     */
    public String message() {
        String msg = cause.getMessage();
        return msg != null ? msg : cause.getClass().getName();
    }

    /**
     * Returns the simple class name of the exception.
     *
     * @return the exception type name
     */
    public String exceptionType() {
        return cause.getClass().getSimpleName();
    }

    /**
     * Returns the full class name of the exception.
     *
     * @return the full exception class name
     */
    public String fullExceptionType() {
        return cause.getClass().getName();
    }

    private static String formatStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    @Override
    public String toString() {
        return String.format("RenderError[%s: %s at %d]", exceptionType(), message(), timestamp);
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.tamboui.tui.trace.FileTraceSink;
import dev.tamboui.tui.trace.TraceSink;

/**
 * Factory for creating toolkit {@link EventTracer} instances.
 * <p>
 * For applications using {@link dev.tamboui.toolkit.app.ToolkitRunner}, tracing is
 * enabled via {@link dev.tamboui.tui.TuiConfig.Builder#traceFromEnvironment()};
 * the runner wires the same sink to the event router automatically.
 * <p>
 * Use this factory when you create an {@link EventRouter} yourself (e.g. headless
 * tests with EventRouterTestHarness) and want optional tracing from the environment:
 *
 * <pre>{@code
 * EventTracer tracer = EventTracerFactory.create();
 * EventRouter router = new EventRouter(focusManager, elementRegistry, tracer);
 * }</pre>
 *
 * Tracing is enabled when the {@code TAMBOUI_EVENT_TRACE} environment variable
 * is set to a file path.
 *
 * @see EventTracer
 * @see SinkEventTracer
 * @see FileTraceSink
 */
public final class EventTracerFactory {

    private static final Logger LOGGER = Logger.getLogger(EventTracerFactory.class.getName());

    /**
     * Environment variable name for enabling event tracing (file path).
     */
    public static final String ENV_VAR = "TAMBOUI_EVENT_TRACE";

    private EventTracerFactory() {
    }

    /**
     * Creates a toolkit event tracer from the environment.
     * <p>
     * If {@code TAMBOUI_EVENT_TRACE} is set to a file path, returns a tracer that
     * writes to that file. Otherwise returns {@link EventTracer#NOOP}. If the file
     * cannot be opened, logs a warning and returns NOOP.
     *
     * @return an event tracer (never null)
     */
    public static EventTracer create() {
        try {
            TraceSink sink = FileTraceSink.fromEnvironment();
            if (sink == null) {
                return EventTracer.NOOP;
            }
            return new SinkEventTracer(sink);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to open trace file. Toolkit event tracing is disabled.", e);
            return EventTracer.NOOP;
        }
    }

    /**
     * Creates a toolkit event tracer that writes to the given sink.
     *
     * @param sink the trace sink (e.g. shared with TuiConfig.builder().traceSink(sink) for one file)
     * @return an event tracer (never null)
     */
    public static EventTracer create(TraceSink sink) {
        return new SinkEventTracer(sink);
    }


    /**
     * Returns whether event tracing is requested via the environment variable.
     * <p>
     * Does not check if the file can be opened.
     *
     * @return true if {@code TAMBOUI_EVENT_TRACE} is set
     */
    public static boolean isEnabled() {
        String path = System.getenv(ENV_VAR);
        return path != null;
    }
}

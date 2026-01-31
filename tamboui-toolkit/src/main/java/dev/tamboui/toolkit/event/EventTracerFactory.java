/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for creating event tracers based on environment configuration.
 * <p>
 * The tracer is enabled by setting the {@code TAMBOUI_EVENT_TRACE} environment
 * variable to a file path:
 * <pre>
 * export TAMBOUI_EVENT_TRACE=/tmp/trace.log
 * ./run-demo.sh my-demo
 * </pre>
 * <p>
 * If the environment variable is not set or is empty, a no-op tracer is returned.
 * If the file cannot be opened, a warning is logged and a no-op tracer is returned.
 *
 * @see EventTracer
 * @see FileEventTracer
 */
public final class EventTracerFactory {

    private static final Logger LOGGER = Logger.getLogger(EventTracerFactory.class.getName());

    /**
     * Environment variable name for enabling event tracing.
     */
    public static final String ENV_VAR = "TAMBOUI_EVENT_TRACE";

    private EventTracerFactory() {
        // Factory class
    }

    /**
     * Creates an event tracer based on environment configuration.
     * <p>
     * If the {@code TAMBOUI_EVENT_TRACE} environment variable is set to a file path,
     * returns a {@link FileEventTracer}. Otherwise, returns a no-op tracer.
     *
     * @return an event tracer
     */
    public static EventTracer create() {
        String tracePath = System.getenv(ENV_VAR);
        if (tracePath == null || tracePath.trim().isEmpty()) {
            return EventTracer.NOOP;
        }
        return create(Paths.get(tracePath.trim()));
    }

    /**
     * Creates an event tracer writing to the specified file.
     * <p>
     * If the file cannot be opened, a warning is logged and a no-op tracer is returned.
     *
     * @param filePath the path to the trace file
     * @return an event tracer
     */
    public static EventTracer create(Path filePath) {
        try {
            return new FileEventTracer(filePath);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to open trace file: " + filePath + ". Event tracing is disabled.", e);
            return EventTracer.NOOP;
        }
    }

    /**
     * Returns whether event tracing is enabled via environment variable.
     *
     * @return true if the TAMBOUI_EVENT_TRACE environment variable is set
     */
    public static boolean isEnabled() {
        String tracePath = System.getenv(ENV_VAR);
        return tracePath != null && !tracePath.trim().isEmpty();
    }
}

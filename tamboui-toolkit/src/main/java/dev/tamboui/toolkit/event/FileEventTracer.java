/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import dev.tamboui.tui.event.Event;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Event tracer that writes JSON Lines to a file.
 * <p>
 * Each line is a complete JSON object, making the file easy to parse,
 * grep, and analyze with standard tools. The format is:
 * <pre>
 * {"ts":"2026-01-29T10:00:00.123Z","type":"route_start","event":"KeyEvent[TAB]",...}
 * </pre>
 * <p>
 * The file is opened for append, so multiple runs can be captured in the same file.
 * Output is flushed after each trace call to ensure data is written even if the
 * application crashes.
 *
 * @see EventTracer
 * @see EventTracerFactory
 */
public final class FileEventTracer implements EventTracer {

    private static final Logger LOGGER = Logger.getLogger(FileEventTracer.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final BufferedWriter writer;
    private final Path filePath;
    private volatile boolean closed;

    /**
     * Creates a new file event tracer.
     *
     * @param filePath the path to the trace file
     * @throws IOException if the file cannot be opened for writing
     */
    public FileEventTracer(Path filePath) throws IOException {
        this.filePath = filePath;
        this.writer = Files.newBufferedWriter(
            filePath,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
        this.closed = false;

        // Write header line to mark start of session
        writeLine("{\"ts\":\"%s\",\"type\":\"session_start\"}", timestamp());
    }

    @Override
    public void traceRouteStart(long routeId, Event event, String focusedId, int elementCount) {
        writeLine("{\"ts\":\"%s\",\"rid\":%d,\"type\":\"route_start\",\"event\":\"%s\",\"focused\":%s,\"elements\":%d}",
            timestamp(), routeId, escape(event.toString()), jsonString(focusedId), elementCount);
    }

    @Override
    public void traceRouteEnd(long routeId, Event event, EventResult result) {
        writeLine("{\"ts\":\"%s\",\"rid\":%d,\"type\":\"route_end\",\"event\":\"%s\",\"result\":\"%s\"}",
            timestamp(), routeId, escape(event.toString()), result.name());
    }

    @Override
    public void traceCandidate(long routeId, String elementId, String elementType, String phase, String decision, String reason) {
        if (reason != null) {
            writeLine("{\"ts\":\"%s\",\"rid\":%d,\"type\":\"candidate\",\"id\":%s,\"elementType\":\"%s\",\"phase\":\"%s\",\"decision\":\"%s\",\"reason\":\"%s\"}",
                timestamp(), routeId, jsonString(elementId), escape(elementType), phase, decision, escape(reason));
        } else {
            writeLine("{\"ts\":\"%s\",\"rid\":%d,\"type\":\"candidate\",\"id\":%s,\"elementType\":\"%s\",\"phase\":\"%s\",\"decision\":\"%s\"}",
                timestamp(), routeId, jsonString(elementId), escape(elementType), phase, decision);
        }
    }

    @Override
    public void traceFocusChange(long routeId, String fromId, String toId, String reason) {
        writeLine("{\"ts\":\"%s\",\"rid\":%d,\"type\":\"focus_change\",\"from\":%s,\"to\":%s,\"reason\":\"%s\"}",
            timestamp(), routeId, jsonString(fromId), jsonString(toId), escape(reason));
    }

    @Override
    public void traceFocusNavigation(long routeId, String action, boolean success, String fromId, String toId) {
        writeLine("{\"ts\":\"%s\",\"rid\":%d,\"type\":\"focus_nav\",\"action\":\"%s\",\"success\":%b,\"from\":%s,\"to\":%s}",
            timestamp(), routeId, action, success, jsonString(fromId), jsonString(toId));
    }

    @Override
    public void traceDragState(long routeId, String action, String elementId, int x, int y) {
        writeLine("{\"ts\":\"%s\",\"rid\":%d,\"type\":\"drag\",\"action\":\"%s\",\"element\":%s,\"x\":%d,\"y\":%d}",
            timestamp(), routeId, action, jsonString(elementId), x, y);
    }

    @Override
    public void traceGlobalHandler(long routeId, int index, EventResult result) {
        writeLine("{\"ts\":\"%s\",\"rid\":%d,\"type\":\"global_handler\",\"index\":%d,\"result\":\"%s\"}",
            timestamp(), routeId, index, result.name());
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            writeLine("{\"ts\":\"%s\",\"type\":\"session_end\"}", timestamp());
            writer.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to close trace file: " + filePath, e);
        }
    }

    private String timestamp() {
        return TIMESTAMP_FORMAT.format(Instant.now());
    }

    private void writeLine(String format, Object... args) {
        if (closed) {
            return;
        }
        try {
            writer.write(String.format(format, args));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to write trace: " + filePath, e);
        }
    }

    private static String jsonString(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + escape(value) + "\"";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

}

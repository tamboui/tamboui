/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.trace;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


/**
 * Trace sink that appends records to a file as tab-separated lines:
 * {@code ts\trid\ttype\tpayload}.
 * <p>
 * Can be created from an explicit path or from the environment variable
 * {@code TAMBOUI_EVENT_TRACE} (path to the trace file).
 */
public final class FileTraceSink implements TraceSink {

    private static final String ENV_TRACE_PATH = "TAMBOUI_EVENT_TRACE";

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    /** Filename-safe timestamp (no colons) for default trace file names. */
    private static final DateTimeFormatter FILENAME_TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss.SSS").withZone(ZoneOffset.UTC);

    private final BufferedWriter writer;

    private boolean closed;

    /**
     * Creates a file trace sink writing to the given path.
     *
     * @param path the file path to append to
     * @throws IOException if the file cannot be opened for writing
     */
    public FileTraceSink(Path path) throws IOException {
        this.writer = new BufferedWriter(new OutputStreamWriter(
                Files.newOutputStream(path.toAbsolutePath(), java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND, java.nio.file.StandardOpenOption.WRITE),
                StandardCharsets.UTF_8));
    }

    /**
     * Creates a file trace sink using the environment variable {@code TAMBOUI_EVENT_TRACE}.
     * If the variable is set to a non-empty file path, that path is used. If the variable
     * is set to empty (blank), a timestamped file is used in the current directory:
     * {@code tamboui-event-trace-<timestamp>.json} (filename-safe, no colons). If the
     * variable is not set, returns null (tracing disabled).
     *
     * @return a new FileTraceSink, or null if the variable is not set
     * @throws IOException if the file cannot be opened
     */
    public static FileTraceSink fromEnvironment() throws IOException {
        String path = System.getenv(ENV_TRACE_PATH);
        if (path == null) {
            return null;
        }
        if (path.isEmpty()) {
            path = "tamboui-event-trace-" + FILENAME_TIMESTAMP_FORMAT.format(Instant.now()) + ".log";
        }
        return new FileTraceSink(Paths.get(path));
    }

    @Override
    public void write(long rid, String type, String payload) {
        if(closed) { return; }

        String timestamp = timestamp();
        String safePayload = (payload == null || payload.trim().isEmpty()) ? null : payload.replace("\n", " ");
        try {
            synchronized (writer) {
                if(safePayload==null) {
                    writer.write(String.format("{ \"ts\": \"%s\", \"rid\": %d, \"type\": \"%s\" }", timestamp, rid, type));
                } else {
                    writer.write(String.format("{ \"ts\": \"%s\", \"rid\": %d, \"type\": \"%s\", \"data\": \"%s\" }", timestamp, rid, type, safePayload));
                }
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            // Best effort; do not throw
        }
    }

    private String timestamp() {
        return TIMESTAMP_FORMAT.format(Instant.now());
    }

    @Override
    public void close() {
        try {

            synchronized (writer) {
                writer.write(String.format("{ \"ts\": \"%s\",\"type\": \"%s\" }", timestamp(), "session_end"));
                writer.close();
            }
        } catch (IOException e) {
            // Ignore
        } finally {
            this.closed = true;
        }
    }

}

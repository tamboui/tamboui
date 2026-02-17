/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.trace;


/**
 * Sink for writing a single chronological stream of trace records.
 * <p>
 * Each record is one line JSON with
 * at least: timestamp, route/session id, type, and type-specific payload.
 *
 * @see FileTraceSink
 */
public interface TraceSink {

    /**
     * Appends one trace record.
     *
     * @param rid     route or session id for correlation
     * @param type    record type (e.g. {@code event}, {@code handler_result}, {@code route_start}, {@code focus_change})
     * @param payload type-specific payload string (may be empty)
     */
    void write(long rid, String type, String payload);

    /**
     * Closes the sink and releases resources.
     */
    void close();
}

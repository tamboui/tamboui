/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.internal.record;

/**
 * A raw captured frame containing the exact bytes written to stdout and its
 * timestamp. Used by AnsiTerminalCapture for raw byte stream capture.
 */
final class RawFrame {
    private final byte[] data;
    private final long timestampMs;

    RawFrame(byte[] data, long timestampMs) {
        this.data = data;
        this.timestampMs = timestampMs;
    }

    /**
     * Returns the raw bytes written to stdout during this frame.
     */
    byte[] data() {
        return data;
    }

    /**
     * Returns the timestamp when this frame was captured (milliseconds since
     * recording start).
     */
    long timestampMs() {
        return timestampMs;
    }
}

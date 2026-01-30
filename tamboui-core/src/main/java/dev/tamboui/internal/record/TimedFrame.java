/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.internal.record;

import dev.tamboui.buffer.Buffer;

/**
 * A captured frame with its timestamp relative to recording start. This is an
 * internal API and not part of the public contract.
 */
final class TimedFrame {

    private final Buffer buffer;
    private final long timestampMs;

    TimedFrame(Buffer buffer, long timestampMs) {
        this.buffer = buffer;
        this.timestampMs = timestampMs;
    }

    Buffer buffer() {
        return buffer;
    }

    long timestampMs() {
        return timestampMs;
    }
}

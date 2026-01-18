/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import dev.tamboui.tui.TuiConfig;

import java.time.Duration;

/// Represents an animation timer tick event.
///
///
///
/// Tick events are generated at regular intervals when animation mode is enabled
/// via {@link TuiConfig#tickRate()}.
public final class TickEvent implements Event {

    private final long frameCount;
    private final Duration elapsed;

    public TickEvent(long frameCount, Duration elapsed) {
        this.frameCount = frameCount;
        this.elapsed = elapsed;
    }

    /// Creates a tick event with the given frame count and elapsed time.
    public static TickEvent of(long frameCount, Duration elapsed) {
        return new TickEvent(frameCount, elapsed);
    }

    /// Returns the elapsed time in milliseconds.
    public long elapsedMillis() {
        return elapsed.toMillis();
    }

    /// Returns the elapsed time in seconds as a double.
    public double elapsedSeconds() {
        return elapsed.toNanos() / 1_000_000_000.0;
    }

    public long frameCount() {
        return frameCount;
    }

    public Duration elapsed() {
        return elapsed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TickEvent)) {
            return false;
        }
        TickEvent tickEvent = (TickEvent) o;
        return frameCount == tickEvent.frameCount && elapsed.equals(tickEvent.elapsed);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(frameCount);
        result = 31 * result + elapsed.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("TickEvent[frameCount=%d, elapsed=%s]", frameCount, elapsed);
    }
}


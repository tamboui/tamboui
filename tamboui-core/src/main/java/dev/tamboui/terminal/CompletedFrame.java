/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;

/**
 * Represents a completed frame after rendering.
 */
public final class CompletedFrame {
    private final Buffer buffer;
    private final Rect area;

    public CompletedFrame(Buffer buffer, Rect area) {
        this.buffer = buffer;
        this.area = area;
    }

    public Buffer buffer() {
        return buffer;
    }

    public Rect area() {
        return area;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompletedFrame)) {
            return false;
        }
        CompletedFrame that = (CompletedFrame) o;
        return buffer.equals(that.buffer) && area.equals(that.area);
    }

    @Override
    public int hashCode() {
        int result = buffer.hashCode();
        result = 31 * result + area.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format("CompletedFrame[buffer=%s, area=%s]", buffer, area);
    }
}

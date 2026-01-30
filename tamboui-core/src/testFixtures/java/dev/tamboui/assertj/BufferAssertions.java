/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.assertj;

import dev.tamboui.buffer.Buffer;

/**
 * Entry point for AssertJ custom assertions for {@link Buffer}.
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * import static dev.tamboui.assertj.BufferAssertions.assertThat;
 *
 * Buffer actual = Buffer.empty(new Rect(0, 0, 10, 5));
 * Buffer expected = Buffer.filled(new Rect(0, 0, 10, 5), Cell.EMPTY);
 * assertThat(actual).isEqualTo(expected);
 * }</pre>
 */
public final class BufferAssertions {

    private BufferAssertions() {
        // Utility class
    }

    /**
     * Creates a new instance of {@link BufferAssert}.
     *
     * @param actual
     *            the buffer we want to make assertions on
     * @return the created assertion object
     */
    public static BufferAssert assertThat(Buffer actual) {
        return new BufferAssert(actual);
    }

}

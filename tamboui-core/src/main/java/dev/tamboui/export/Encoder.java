/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;

/**
 * Encodes a region of a buffer to a format and writes to an {@link Appendable}.
 *
 * @param <O> the options type for this format
 */
public interface Encoder<O extends ExportOptions> {

    /**
     * Encodes the given region of the buffer with the given options and appends the result to the output.
     * When {@code region} is empty, implementations should produce minimal or empty output.
     *
     * @param buffer the buffer to export from
     * @param region the rectangle to export (already clipped to buffer bounds)
     * @param options format-specific options
     * @param out     where to append the encoded output
     */
    void encode(Buffer buffer, Rect region, O options, Appendable out);
}

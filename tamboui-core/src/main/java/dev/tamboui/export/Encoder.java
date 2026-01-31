/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export;

import dev.tamboui.buffer.Buffer;

/**
 * Encodes a buffer to a format and writes to an {@link Appendable}.
 *
 * @param <O> the options type for this format
 */
public interface Encoder<O extends ExportOptions> {

    /**
     * Encodes the buffer with the given options and appends the result to the output.
     *
     * @param buffer the buffer to export
     * @param options format-specific options
     * @param out     where to append the encoded output
     */
    void encode(Buffer buffer, O options, Appendable out);
}

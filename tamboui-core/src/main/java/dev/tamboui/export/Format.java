/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export;

/**
 * A format for buffer export (e.g. SVG, HTML, text).
 *
 * @param <O> the options type for this format
 */
public interface Format<O extends ExportOptions> {

    /**
     * Returns a short identifier for this format (e.g. "svg", "html", "text").
     *
     * @return the format id
     */
    String id();

    /**
     * Returns default options for this format.
     *
     * @return default options
     */
    O defaultOptions();

    /**
     * Returns the encoder for this format.
     *
     * @return the encoder
     */
    Encoder<O> encoder();
}

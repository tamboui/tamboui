/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export;

import dev.tamboui.buffer.Buffer;

import java.util.Objects;

/**
 * Entry point for the fluent export API.
 * Use {@link Buffer#export()} or {@link Export#from(Buffer)} to start.
 *
 * <p>Example:
 * <pre>{@code
 * String svg = buffer.export()
 *     .as(Formats.SVG)
 *     .options(o -> o.title("My App"))
 *     .toString();
 *
 * buffer.export()
 *     .as(Formats.HTML)
 *     .options(o -> o.inlineStyles(true))
 *     .toFile(Paths.get("out.html"));
 * }</pre>
 */
public final class Export {

    private Export() {
    }

    /**
     * Starts an export request for the given buffer.
     *
     * @param buffer the buffer to export
     * @return the export request
     */
    public static ExportRequest from(Buffer buffer) {
        return new ExportRequest(Objects.requireNonNull(buffer, "buffer"));
    }
}

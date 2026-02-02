/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.export.html.HtmlOptions;
import dev.tamboui.export.svg.SvgOptions;
import dev.tamboui.export.text.TextOptions;
import dev.tamboui.layout.Rect;

/**
 * Entry point for the fluent export API after selecting a buffer.
 * Start with {@link #export(Buffer)}, then use {@link #crop(Rect)} to export only a region.
 */
public final class ExportRequest {

    private final Buffer buffer;
    private final Rect crop;

    ExportRequest(Buffer buffer) {
        this(buffer, null);
    }

    /**
     * Starts an export request for the given buffer.
     *
     * @param buffer the buffer to export
     * @return the export request
     */
    public static ExportRequest export(Buffer buffer) {
        return new ExportRequest(Objects.requireNonNull(buffer, "buffer"));
    }

    ExportRequest(Buffer buffer, Rect crop) {
        this.buffer = Objects.requireNonNull(buffer, "buffer");
        this.crop = crop;
    }

    /**
     * Limits export to the given rectangle (clipped to buffer bounds).
     * Returns a new request; the current request is unchanged.
     *
     * @param rect the region to export (null or empty is not recommended)
     * @return a new export request with the crop applied
     */
    public ExportRequest crop(Rect rect) {
        return new ExportRequest(buffer, rect);
    }

    Buffer buffer() {
        return buffer;
    }

    Rect region() {
        if (crop == null) {
            return buffer.area();
        }
        Rect clipped = crop.intersection(buffer.area());
        return clipped.isEmpty() ? Rect.ZERO : clipped;
    }

    /**
     * Selects the export format and returns a step where options and destination can be set.
     *
     * @param format the format (e.g. {@link Formats#SVG}, {@link Formats#HTML}, {@link Formats#TEXT})
     * @param <O>    the options type for this format
     * @return the export step for configuring options and writing output
     */
    public <O extends ExportOptions> ExportStep<O> as(Format<O> format) {
        Objects.requireNonNull(format, "format");
        O options = format.defaultOptions();
        return new ExportStep<O>(this, format, options);
    }

    /**
     * Shorthand for {@code as(Formats.SVG)}. Exports to SVG.
     *
     * @return the export step for configuring SVG options and writing output
     */
    public ExportStep<SvgOptions> svg() {
        return as(Formats.SVG);
    }

    /**
     * Shorthand for {@code as(Formats.HTML)}. Exports to HTML.
     *
     * @return the export step for configuring HTML options and writing output
     */
    public ExportStep<HtmlOptions> html() {
        return as(Formats.HTML);
    }

    /**
     * Shorthand for {@code as(Formats.TEXT)}. Exports to plain or ANSI-styled text.
     *
     * @return the export step for configuring text options and writing output
     */
    public ExportStep<TextOptions> text() {
        return as(Formats.TEXT);
    }

    /**
     * Exports the buffer to the given path. Format is chosen by file extension:
     * .svg → SVG, .html / .htm → HTML, .txt / .asc → plain text, .ans / .ansi → ANSI text.
     * Unknown extensions default to SVG.
     *
     * @param path the output path
     * @throws IOException if writing fails
     */
    public void toFile(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        String name = path.getFileName() != null ? path.getFileName().toString() : "";
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".svg")) {
            as(Formats.SVG).toFile(path);
        } else if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            as(Formats.HTML).toFile(path);
        } else if (lower.endsWith(".ans") || lower.endsWith(".ansi")) {
            as(Formats.TEXT).options(o -> o.styles(true)).toFile(path);
        } else if (lower.endsWith(".txt") || lower.endsWith(".asc")) {
            as(Formats.TEXT).toFile(path);
        } else {
            as(Formats.SVG).toFile(path);
        }
    }
}

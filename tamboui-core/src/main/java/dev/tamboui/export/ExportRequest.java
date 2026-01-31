/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.export.html.HtmlOptions;
import dev.tamboui.export.svg.SvgOptions;
import dev.tamboui.export.text.TextOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/**
 * Entry point for the fluent export API after selecting a buffer.
 * Obtain via {@link Buffer#export()} or {@link Export#from(Buffer)}.
 */
public final class ExportRequest {

    private final Buffer buffer;

    ExportRequest(Buffer buffer) {
        this.buffer = Objects.requireNonNull(buffer, "buffer");
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

    Buffer buffer() {
        return buffer;
    }
}

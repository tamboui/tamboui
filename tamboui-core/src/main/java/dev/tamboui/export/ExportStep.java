/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;


/**
 * Fluent step after selecting a format. Allows configuring options and writing output.
 *
 * @param <O> the options type for the selected format
 */
public final class ExportStep<O extends ExportOptions> {

    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    private final ExportRequest request;
    private final Format<O> format;
    private final O options;

    ExportStep(ExportRequest request, Format<O> format, O options) {
        this.request = request;
        this.format = format;
        this.options = options;
    }

    /**
     * Applies the given consumer to the format options (e.g. set title, theme).
     *
     * @param mutator consumer that configures the options
     * @return this step for chaining
     */
    public ExportStep<O> options(Consumer<O> mutator) {
        if (mutator != null) {
            mutator.accept(options);
        }
        return this;
    }

    /**
     * Writes the exported output to the given path.
     *
     * @param path the file path
     * @throws IOException if writing fails
     */
    public void toFile(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(path), UTF_8)) {
            format.encoder().encode(request.buffer(), request.region(), options, writer);
        }
    }

    /**
     * Writes the exported output to the given stream (UTF-8).
     *
     * @param out the output stream
     * @throws IOException if writing fails
     */
    public void to(OutputStream out) throws IOException {
        Objects.requireNonNull(out, "out");
        to(new OutputStreamWriter(out, UTF_8));
    }

    /**
     * Writes the exported output to the given writer.
     * The caller controls charset and destination; this method does not close the writer.
     *
     * @param out the writer
     * @throws IOException if writing or flushing fails
     */
    public void to(Writer out) throws IOException {
        Objects.requireNonNull(out, "out");
        format.encoder().encode(request.buffer(), request.region(), options, out);
        out.flush();
    }

    /**
     * Returns the exported output as a string.
     *
     * @return the encoded string
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        format.encoder().encode(request.buffer(), request.region(), options, sb);
        return sb.toString();
    }

    /**
     * Returns the exported output as UTF-8 bytes.
     *
     * @return the encoded bytes
     */
    public byte[] toBytes() {
        return toString().getBytes(UTF_8);
    }
}

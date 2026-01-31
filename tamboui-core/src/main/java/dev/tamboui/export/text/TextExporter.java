/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export.text;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;

import java.util.Objects;

/**
 * Exports a {@link Buffer} to text in the same spirit as Rich's {@code Console.export_text}.
 * <p>
 * By default outputs cell symbols line by line with no styling (plain text). With
 * {@link TextOptions#styles(boolean) styles(true)}, ANSI escape codes are included so the
 * output can be rendered with colors and modifiers in a terminal. Continuation cells
 * (trailing columns of wide characters) are skipped so each grapheme appears once.
 *
 * <h2>Notes</h2>
 * <ul>
 *     <li>This is a pure string export; it does not require a backend.</li>
 *     <li>Line endings use the platform default ({@code System.lineSeparator()}).</li>
 * </ul>
 */
public final class TextExporter {

    private TextExporter() {
    }

    /**
     * Encodes the buffer to text and appends to the given output.
     * Used by the fluent export API.
     *
     * @param buffer  the buffer to export
     * @param options export options
     * @param out     where to append the text
     */
    static void encode(Buffer buffer, TextOptions options, Appendable out) {
        String text = buildText(buffer, options);
        try {
            out.append(text);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String buildText(Buffer buffer, TextOptions options) {
        Objects.requireNonNull(buffer, "buffer");
        Objects.requireNonNull(options, "options");

        if (options.styles) {
            String ansi = buffer.toAnsiString();
            return ansi.replace("\r\n", System.lineSeparator());
        }

        int widthCells = buffer.width();
        int heightCells = buffer.height();
        int baseX = buffer.area().x();
        int baseY = buffer.area().y();
        String newline = System.lineSeparator();

        StringBuilder result = new StringBuilder((widthCells + newline.length()) * heightCells);

        for (int y = 0; y < heightCells; y++) {
            for (int x = 0; x < widthCells; x++) {
                Cell cell = buffer.get(baseX + x, baseY + y);
                String symbol = cell.symbol();
                // Skip continuation cells so wide characters appear once
                if (!symbol.isEmpty()) {
                    result.append(symbol);
                }
            }
            if (y < heightCells - 1) {
                result.append(newline);
            }
        }

        return result.toString();
    }
}

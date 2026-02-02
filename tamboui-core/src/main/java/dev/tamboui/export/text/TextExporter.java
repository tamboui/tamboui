/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export.text;

import java.util.Objects;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.AnsiStringBuilder;

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
     * Encodes the given region of the buffer to text and appends to the given output.
     * Used by the fluent export API.
     *
     * @param buffer  the buffer to export from
     * @param region  the rectangle to export (empty produces empty string)
     * @param options export options
     * @param out     where to append the text
     */
    static void encode(Buffer buffer, Rect region, TextOptions options, Appendable out) {
        String text = buildText(buffer, region, options);
        try {
            out.append(text);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String buildText(Buffer buffer, Rect region, TextOptions options) {
        Objects.requireNonNull(buffer, "buffer");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(options, "options");

        if (region.isEmpty()) {
            return "";
        }

        int widthCells = region.width();
        int heightCells = region.height();
        int baseX = region.x();
        int baseY = region.y();
        String newline = System.lineSeparator();

        if (options.styles) {
            StringBuilder result = new StringBuilder((widthCells * 2 + newline.length()) * heightCells);
            Style lastStyle = null;
            for (int y = 0; y < heightCells; y++) {
                if (y > 0) {
                    result.append(newline);
                }
                for (int x = 0; x < widthCells; x++) {
                    Cell cell = buffer.get(baseX + x, baseY + y);
                    if (cell.isContinuation()) {
                        continue;
                    }
                    if (!cell.style().equals(lastStyle)) {
                        result.append(AnsiStringBuilder.styleToAnsi(cell.style()));
                        lastStyle = cell.style();
                    }
                    result.append(cell.symbol());
                }
            }
            result.append(AnsiStringBuilder.RESET);
            return result.toString();
        }

        StringBuilder result = new StringBuilder((widthCells + newline.length()) * heightCells);
        for (int y = 0; y < heightCells; y++) {
            for (int x = 0; x < widthCells; x++) {
                Cell cell = buffer.get(baseX + x, baseY + y);
                String symbol = cell.symbol();
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

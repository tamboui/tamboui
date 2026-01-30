/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.assertj;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;

/**
 * Utility class for formatting buffer differences in a readable way, similar to
 * ratatui.rs assertion output.
 */
final class BufferDiffFormatter {

    private BufferDiffFormatter() {
        // Utility class
    }

    /**
     * Formats a diff between two buffers, showing both buffers side-by-side.
     *
     * @param actual
     *            the actual buffer
     * @param expected
     *            the expected buffer
     * @return a formatted string showing the difference
     */
    static String formatDiff(Buffer actual, Buffer expected) {
        StringBuilder sb = new StringBuilder();

        // Format actual buffer
        sb.append(" actual: ").append(formatBuffer(actual)).append("\n");
        sb.append(" expected: ").append(formatBuffer(expected));

        return sb.toString();
    }

    /**
     * Formats a buffer for display, similar to ratatui.rs format.
     *
     * @param buffer
     *            the buffer to format
     * @return a formatted string representation
     */
    static String formatBuffer(Buffer buffer) {
        if (buffer == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        Rect area = buffer.area();

        // Buffer header
        sb.append("Buffer {\n");
        sb.append("    area: ").append(formatRect(area)).append(",\n");

        // Content lines
        List<String> contentLines = formatContentLines(buffer);
        sb.append("    content: [\n");
        for (int i = 0; i < contentLines.size(); i++) {
            sb.append("        \"").append(contentLines.get(i)).append("\"");
            if (i < contentLines.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("    ],\n");

        // Format styles - only include non-empty styles
        List<String> styleLines = formatStyleLines(buffer);
        sb.append("    styles: [\n");
        for (int i = 0; i < styleLines.size(); i++) {
            sb.append("        ").append(styleLines.get(i));
            if (i < styleLines.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("    ]\n");
        sb.append("}");

        return sb.toString();
    }

    private static String formatRect(Rect rect) {
        return String.format("Rect { x: %d, y: %d, width: %d, height: %d }", rect.x(), rect.y(),
                rect.width(), rect.height());
    }

    private static List<String> formatContentLines(Buffer buffer) {
        List<String> lines = new ArrayList<>();
        Rect area = buffer.area();

        for (int y = area.top(); y < area.bottom(); y++) {
            StringBuilder line = new StringBuilder();
            for (int x = area.left(); x < area.right(); x++) {
                Cell cell = buffer.get(x, y);
                String symbol = cell.symbol();
                // Escape quotes in the symbol
                symbol = symbol.replace("\"", "\\\"");
                line.append(symbol);
            }
            lines.add(line.toString());
        }

        return lines;
    }

    private static List<String> formatStyleLines(Buffer buffer) {
        List<String> styleLines = new ArrayList<>();
        Rect area = buffer.area();

        for (int y = area.top(); y < area.bottom(); y++) {
            for (int x = area.left(); x < area.right(); x++) {
                Cell cell = buffer.get(x, y);
                Style style = cell.style();

                // Only include non-empty styles
                if (!style.equals(Style.EMPTY)) {
                    StringBuilder styleLine = new StringBuilder();
                    styleLine.append("x: ").append(x).append(", y: ").append(y);

                    // Format foreground color
                    if (style.fg().isPresent()) {
                        styleLine.append(", fg: ").append(formatColor(style.fg().get()));
                    } else {
                        styleLine.append(", fg: Reset");
                    }

                    // Format background color
                    if (style.bg().isPresent()) {
                        styleLine.append(", bg: ").append(formatColor(style.bg().get()));
                    } else {
                        styleLine.append(", bg: Reset");
                    }

                    // Format modifiers
                    EnumSet<Modifier> modifiers = style.effectiveModifiers();
                    if (modifiers.isEmpty()) {
                        styleLine.append(", modifier: NONE");
                    } else {
                        styleLine.append(", modifier: ").append(formatModifiers(modifiers));
                    }

                    styleLines.add(styleLine.toString());
                }
            }
        }

        // If no styles found, add a placeholder
        if (styleLines.isEmpty()) {
            styleLines.add("x: 0, y: 0, fg: Reset, bg: Reset, modifier: NONE");
        }

        return styleLines;
    }

    private static String formatColor(Color color) {
        if (color == null) {
            return "Reset";
        }
        if (color instanceof Color.Named) {
            return ((Color.Named) color).name();
        } else if (color instanceof Color.Reset) {
            return "Reset";
        } else if (color instanceof Color.Ansi) {
            return ((Color.Ansi) color).color().name();
        } else if (color instanceof Color.Indexed) {
            return "Indexed(" + ((Color.Indexed) color).index() + ")";
        } else if (color instanceof Color.Rgb) {
            Color.Rgb rgb = (Color.Rgb) color;
            return String.format("Rgb(%d, %d, %d)", rgb.r(), rgb.g(), rgb.b());
        }
        return color.toString();
    }

    private static String formatModifiers(EnumSet<Modifier> modifiers) {
        if (modifiers.isEmpty()) {
            return "NONE";
        }
        List<String> modifierNames = new ArrayList<>();
        for (Modifier mod : modifiers) {
            modifierNames.add(mod.name());
        }
        return String.join(" | ", modifierNames);
    }
}

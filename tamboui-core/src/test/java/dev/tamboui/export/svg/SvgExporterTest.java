/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export.svg;

import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.export.Formats;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

import static dev.tamboui.export.ExportRequest.export;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SvgExporterTest {

    @Test
    void exportsSvgWithStylesAndBackgrounds() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 6, 2));
        buffer.setString(0, 0, "Hello!", Style.EMPTY.fg(Color.hex("#c5c8c6")));
        buffer.setString(0, 1, "AB", Style.EMPTY.onBlue().bold());
        buffer.setString(2, 1, "CD", Style.EMPTY.italic().underlined());

        String svg = export(buffer).as(Formats.SVG)
            .options(o -> o.title("Test").uniqueId("test"))
            .toString();

        // Basic structure
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("test-matrix"));
        assertTrue(svg.contains("clipPath id=\"test-line-0\""));
        assertTrue(svg.contains("clipPath id=\"test-line-1\""));

        // Style rules
        assertTrue(svg.contains("font-weight: bold"));
        assertTrue(svg.contains("font-style: italic"));
        assertTrue(svg.contains("text-decoration: underline"));

        // Background rect for the bold-on-blue run
        assertTrue(svg.contains("<rect") && svg.contains("shape-rendering=\"crispEdges\""));

        // Text nodes for non-space content
        assertTrue(svg.contains(">Hello!<") || svg.contains("Hello!"));
        assertTrue(svg.contains(">AB<") || svg.contains("AB"));
        assertTrue(svg.contains(">CD<") || svg.contains("CD"));
    }

    @Test
    void cropExportsOnlyRegion() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 4));
        buffer.setString(0, 0, "Row0", Style.EMPTY);
        buffer.setString(0, 1, "Row1", Style.EMPTY);
        buffer.setString(0, 2, "Row2", Style.EMPTY);
        buffer.setString(0, 3, "Row3", Style.EMPTY);

        Rect crop = new Rect(0, 1, 4, 2);  // Row1 and Row2, first 4 cols
        String svg = export(buffer).crop(crop).svg().options(o -> o.uniqueId("crop")).toString();

        assertTrue(svg.contains("Row1"));
        assertTrue(svg.contains("Row2"));
        assertFalse(svg.contains("Row0"));
        assertFalse(svg.contains("Row3"));
    }
}

/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class BufferSvgExporterTest {

    @Test
    void exportsSvgWithStylesAndBackgrounds() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 6, 2));
        buffer.setString(0, 0, "Hello!", Style.EMPTY.fg(Color.hex("#c5c8c6")));
        buffer.setString(0, 1, "AB", Style.EMPTY.onBlue().bold());
        buffer.setString(2, 1, "CD", Style.EMPTY.italic().underlined());

        String svg = BufferSvgExporter.exportSvg(buffer, new BufferSvgExporter.Options()
            .title("Test")
            .uniqueId("test"));

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
}


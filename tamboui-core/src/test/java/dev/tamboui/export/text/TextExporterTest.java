/*
 * Copyright (c) 2026 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.export.text;

import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.export.Formats;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

import static dev.tamboui.export.ExportRequest.export;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TextExporterTest {

    @Test
    void exportsPlainText() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 2));
        buffer.setString(0, 0, "Hello", Style.EMPTY);
        buffer.setString(0, 1, "World", Style.EMPTY);

        String text = export(buffer).as(Formats.TEXT).toString();

        assertTrue(text.contains("Hello"));
        assertTrue(text.contains("World"));
        assertFalse(text.contains("\u001b"), "plain text must not contain ANSI escapes");
        assertEquals(2, text.split(System.lineSeparator()).length);
    }

    @Test
    void exportsTextWithAnsiWhenStylesTrue() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 6, 1));
        buffer.setString(0, 0, "Hi", Style.EMPTY.fg(Color.GREEN).bold());

        String text = export(buffer).as(Formats.TEXT).options(o -> o.styles(true)).toString();

        assertTrue(text.contains("Hi"));
        assertTrue(text.contains("\u001b["), "styles=true must include ANSI escape sequences");
        assertTrue(text.contains("m"), "ANSI SGR sequence terminator");
    }

    @Test
    void skipsContinuationCells() {
        Buffer buffer = Buffer.withLines("A\u4E16");  // A + CJK (2 cols)
        String text = export(buffer).as(Formats.TEXT).toString();
        assertTrue(text.contains("A"));
        assertTrue(text.contains("\u4E16"));
        assertEquals(1, text.split(System.lineSeparator()).length);
    }
}

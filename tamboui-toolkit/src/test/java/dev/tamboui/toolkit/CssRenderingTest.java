/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.elements.Column;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.toolkit.elements.Row;
import dev.tamboui.toolkit.elements.TextElement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that verifies CSS styles are actually applied during rendering.
 */
class CssRenderingTest {

    // Path to the demo's theme resources (single source of truth)
    private static final Path THEMES_DIR = Paths
            .get("../tamboui-css/demos/css-demo/src/main/resources/themes");

    private StyleEngine styleEngine;
    private DefaultRenderContext context;
    private Buffer buffer;
    private Frame frame;

    @BeforeEach
    void setUp() throws IOException {
        styleEngine = StyleEngine.create();
        styleEngine.loadStylesheet("light", THEMES_DIR.resolve("light.tcss"));
        styleEngine.setActiveStylesheet("light");

        context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 80, 24);
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
    }

    @Test
    void textElement_rendersWithCssForeground() {
        TextElement text = new TextElement("Hello");
        Rect area = new Rect(0, 0, 10, 1);

        text.render(frame, area, context);

        // Check that the cells have the CSS foreground from * selector
        Cell cell = buffer.get(0, 0);
        System.out.println("Cell at (0,0): symbol='" + cell.symbol() + "' style=" + cell.style());
        System.out.println("  fg: " + cell.style().fg());
        System.out.println("  bg: " + cell.style().bg());

        assertThat(cell.symbol()).isEqualTo("H");
        // Foreground from * selector (#1a1a1a)
        assertThat(cell.style().fg()).isPresent();
        assertThat(cell.style().fg().get()).isInstanceOf(Color.Rgb.class);
        // Background is NOT set on * selector - TextElement is transparent
        assertThat(cell.style().bg()).isEmpty();
    }

    @Test
    void textElement_withClass_rendersWithCssForeground() {
        TextElement text = new TextElement("Primary");
        text.addClass("primary");
        Rect area = new Rect(0, 0, 10, 1);

        text.render(frame, area, context);

        Cell cell = buffer.get(0, 0);
        System.out.println("Cell at (0,0): symbol='" + cell.symbol() + "' style=" + cell.style());
        System.out.println("  fg: " + cell.style().fg());
        System.out.println("  bg: " + cell.style().bg());

        assertThat(cell.symbol()).isEqualTo("P");
        // Should have blue foreground from .primary (#0055aa)
        assertThat(cell.style().fg()).isPresent();
        assertThat(cell.style().fg().get()).isInstanceOf(Color.Rgb.class);
        Color.Rgb fg = (Color.Rgb) cell.style().fg().get();
        assertThat(fg.r()).isEqualTo(0x00);
        assertThat(fg.g()).isEqualTo(0x55);
        assertThat(fg.b()).isEqualTo(0xaa);
        // Background is NOT set - .primary class doesn't set background
        assertThat(cell.style().bg()).isEmpty();
    }

    @Test
    void row_rendersWithCssForeground() {
        Row row = new Row(new TextElement("Hello"));
        Rect area = new Rect(0, 0, 80, 1);

        row.render(frame, area, context);

        // Check that cell 0 has text with foreground
        Cell cell0 = buffer.get(0, 0);
        System.out.println("Cell at (0,0): symbol='" + cell0.symbol() + "' style=" + cell0.style());

        // Check that cell 10 (no text) - Row fills its background from CSS rule
        Cell cell10 = buffer.get(10, 0);
        System.out.println(
                "Cell at (10,0): symbol='" + cell10.symbol() + "' style=" + cell10.style());

        // TextElement has foreground from CSS
        assertThat(cell0.style().fg()).isPresent();
        // Row has background from "Row, Column" CSS rule (#eeeeee in light theme)
        assertThat(cell0.style().bg()).isPresent();
        assertThat(cell0.style().bg().get()).isInstanceOf(Color.Rgb.class);
        Color.Rgb rowBg = (Color.Rgb) cell0.style().bg().get();
        assertThat(rowBg.r()).isEqualTo(0xee);
        assertThat(rowBg.g()).isEqualTo(0xee);
        assertThat(rowBg.b()).isEqualTo(0xee);
    }

    @Test
    void column_rendersWithCssForeground() {
        Column column = new Column(new TextElement("Hello"));
        Rect area = new Rect(0, 0, 80, 24);

        column.render(frame, area, context);

        // Check that cell 0 has text with foreground
        Cell cell0 = buffer.get(0, 0);
        System.out.println("Cell at (0,0): symbol='" + cell0.symbol() + "' style=" + cell0.style());

        // Check that cell on row 1 (no text) - Column fills its background from CSS
        // rule
        Cell cell01 = buffer.get(0, 1);
        System.out
                .println("Cell at (0,1): symbol='" + cell01.symbol() + "' style=" + cell01.style());

        // TextElement has foreground from CSS
        assertThat(cell0.style().fg()).isPresent();
        // Column has background from "Row, Column" CSS rule (#eeeeee in light theme)
        assertThat(cell0.style().bg()).isPresent();
        assertThat(cell0.style().bg().get()).isInstanceOf(Color.Rgb.class);
        Color.Rgb colBg = (Color.Rgb) cell0.style().bg().get();
        assertThat(colBg.r()).isEqualTo(0xee);
        assertThat(colBg.g()).isEqualTo(0xee);
        assertThat(colBg.b()).isEqualTo(0xee);
    }

    @Test
    void panel_rendersWithCssBackground() {
        Panel panel = new Panel(new TextElement("Hello"));
        Rect area = new Rect(0, 0, 20, 5);

        panel.render(frame, area, context);

        // Check that the border has the style
        Cell borderCell = buffer.get(0, 0);
        System.out.println("Border cell at (0,0): symbol='" + borderCell.symbol() + "' style="
                + borderCell.style());

        // Check that inner area has background
        Cell innerCell = buffer.get(2, 2);
        System.out.println("Inner cell at (2,2): symbol='" + innerCell.symbol() + "' style="
                + innerCell.style());

        // Panel has its own background rule in CSS (#eeeeee)
        assertThat(borderCell.style().bg()).isPresent();
        assertThat(borderCell.style().bg().get()).isInstanceOf(Color.Rgb.class);
        Color.Rgb panelBg = (Color.Rgb) borderCell.style().bg().get();
        assertThat(panelBg.r()).isEqualTo(0xee);
        assertThat(panelBg.g()).isEqualTo(0xee);
        assertThat(panelBg.b()).isEqualTo(0xee);
    }

    @Test
    void panel_withStatusClass_hasBorderColor() {
        Panel panel = new Panel(new TextElement("Status Bar"));
        panel.addClass("status");
        Rect area = new Rect(0, 0, 30, 3);

        panel.render(frame, area, context);

        // Check that the border has the correct foreground color from border-color CSS
        Cell borderCell = buffer.get(0, 0);

        // The Panel's background is applied to its area, then children render on top
        // Check that the Panel area (background) has correct color by checking border
        // cell's bg
        // .status class sets background: $bg-primary (#eeeeee in light theme)
        assertThat(borderCell.style().bg()).isPresent();
        assertThat(borderCell.style().bg().get()).isInstanceOf(Color.Rgb.class);
        Color.Rgb panelBg = (Color.Rgb) borderCell.style().bg().get();
        assertThat(panelBg.r()).isEqualTo(0xee);
        assertThat(panelBg.g()).isEqualTo(0xee);
        assertThat(panelBg.b()).isEqualTo(0xee);

        // Border foreground should be #888888 from border-color CSS
        assertThat(borderCell.style().fg()).isPresent();
        assertThat(borderCell.style().fg().get()).isInstanceOf(Color.Rgb.class);
        Color.Rgb borderFg = (Color.Rgb) borderCell.style().fg().get();
        assertThat(borderFg.r()).isEqualTo(0x88);
        assertThat(borderFg.g()).isEqualTo(0x88);
        assertThat(borderFg.b()).isEqualTo(0x88);
    }
}

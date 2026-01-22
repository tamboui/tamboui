/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that verifies CSS styles are actually applied during rendering.
 */
class CssRenderingTest {

    private StyleEngine styleEngine;
    private DefaultRenderContext context;
    private Buffer buffer;
    private Frame frame;

    @BeforeEach
    void setUp() throws IOException {
        styleEngine = StyleEngine.create();
        styleEngine.loadStylesheet("light", "/themes/light.tcss");
        styleEngine.setActiveStylesheet("light");

        context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 80, 24);
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
    }

    @Test
    void textElement_rendersWithCssBackground() {
        TextElement text = new TextElement("Hello");
        Rect area = new Rect(0, 0, 10, 1);

        text.render(frame, area, context);

        // Check that the cells have the CSS background
        Cell cell = buffer.get(0, 0);
        System.out.println("Cell at (0,0): symbol='" + cell.symbol() + "' style=" + cell.style());
        System.out.println("  fg: " + cell.style().fg());
        System.out.println("  bg: " + cell.style().bg());

        assertThat(cell.symbol()).isEqualTo("H");
        assertThat(cell.style().bg()).isPresent();
        assertThat(cell.style().bg().get()).isInstanceOf(Color.Rgb.class);
    }

    @Test
    void textElement_withClass_rendersWithCssBackground() {
        TextElement text = new TextElement("Primary");
        text.addClass("primary");
        Rect area = new Rect(0, 0, 10, 1);

        text.render(frame, area, context);

        Cell cell = buffer.get(0, 0);
        System.out.println("Cell at (0,0): symbol='" + cell.symbol() + "' style=" + cell.style());
        System.out.println("  fg: " + cell.style().fg());
        System.out.println("  bg: " + cell.style().bg());

        assertThat(cell.symbol()).isEqualTo("P");
        // Should have blue foreground from .primary and RGB background from *
        assertThat(cell.style().fg()).isPresent();
        assertThat(cell.style().bg()).isPresent();
    }

    @Test
    void row_rendersWithCssBackground() {
        Row row = new Row(new TextElement("Hello"));
        Rect area = new Rect(0, 0, 80, 1);

        row.render(frame, area, context);

        // Check that cell 0 has text with background
        Cell cell0 = buffer.get(0, 0);
        System.out.println("Cell at (0,0): symbol='" + cell0.symbol() + "' style=" + cell0.style());

        // Check that cell 10 (no text) still has background from Row
        Cell cell10 = buffer.get(10, 0);
        System.out.println("Cell at (10,0): symbol='" + cell10.symbol() + "' style=" + cell10.style());

        assertThat(cell0.style().bg()).isPresent();
        assertThat(cell10.style().bg()).isPresent();
    }

    @Test
    void column_rendersWithCssBackground() {
        Column column = new Column(new TextElement("Hello"));
        Rect area = new Rect(0, 0, 80, 24);

        column.render(frame, area, context);

        // Check that cell 0 has text with background
        Cell cell0 = buffer.get(0, 0);
        System.out.println("Cell at (0,0): symbol='" + cell0.symbol() + "' style=" + cell0.style());

        // Check that cell on row 1 (no text) still has background from Column
        Cell cell01 = buffer.get(0, 1);
        System.out.println("Cell at (0,1): symbol='" + cell01.symbol() + "' style=" + cell01.style());

        assertThat(cell0.style().bg()).isPresent();
        assertThat(cell01.style().bg()).isPresent();
    }

    @Test
    void panel_rendersWithCssBackground() {
        Panel panel = new Panel(new TextElement("Hello"));
        Rect area = new Rect(0, 0, 20, 5);

        panel.render(frame, area, context);

        // Check that the border has the style
        Cell borderCell = buffer.get(0, 0);
        System.out.println("Border cell at (0,0): symbol='" + borderCell.symbol() + "' style=" + borderCell.style());

        // Check that inner area has background
        Cell innerCell = buffer.get(2, 2);
        System.out.println("Inner cell at (2,2): symbol='" + innerCell.symbol() + "' style=" + innerCell.style());

        assertThat(borderCell.style().bg()).isPresent();
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
        // Check that the Panel area (background) has correct color by checking border cell's bg
        assertThat(borderCell.style().bg()).isPresent();
        assertThat(borderCell.style().bg().get()).isInstanceOf(Color.Rgb.class);
        Color.Rgb panelBg = (Color.Rgb) borderCell.style().bg().get();
        assertThat(panelBg.r()).isEqualTo(0xcc);
        assertThat(panelBg.g()).isEqualTo(0xcc);
        assertThat(panelBg.b()).isEqualTo(0xcc);

        // Border foreground should be #888888 from border-color CSS
        assertThat(borderCell.style().fg()).isPresent();
        assertThat(borderCell.style().fg().get()).isInstanceOf(Color.Rgb.class);
        Color.Rgb borderFg = (Color.Rgb) borderCell.style().fg().get();
        assertThat(borderFg.r()).isEqualTo(0x88);
        assertThat(borderFg.g()).isEqualTo(0x88);
        assertThat(borderFg.b()).isEqualTo(0x88);
    }
}

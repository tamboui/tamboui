/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit;

import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.elements.TextElement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that verifies CSS can override programmatic named colors.
 * <p>
 * When code uses {@code .red()} or {@code .fg(Color.RED)}, a CSS class "red" is
 * added. Theme stylesheets should be able to override the color via {@code .red
 * { color: #FF5555; }}.
 */
class NamedColorCssOverrideTest {

    private static final Rect AREA = new Rect(0, 0, 10, 1);

    private Cell renderAndGetFirst(String text, TextElement element, String... cssRules) {
        StyleEngine styleEngine = StyleEngine.create();
        for (String css : cssRules) {
            styleEngine.addStylesheet(css);
        }
        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 80, 24));
        Frame frame = Frame.forTesting(buffer);

        element.render(frame, AREA, context);
        return buffer.get(0, 0);
    }

    @Test
    void cssClassSelectorOverridesNamedForegroundColor() {
        TextElement text = new TextElement("Error").red();
        Cell cell = renderAndGetFirst("Error", text, ".red { color: #FF5555; }");

        assertThat(cell.style().fg()).isPresent();
        assertThat(cell.style().fg().get()).isInstanceOf(Color.Rgb.class);
        Color.Rgb fg = (Color.Rgb) cell.style().fg().get();
        assertThat(fg.r()).isEqualTo(0xFF);
        assertThat(fg.g()).isEqualTo(0x55);
        assertThat(fg.b()).isEqualTo(0x55);
    }

    @Test
    void cssClassSelectorOverridesNamedBackgroundColor() {
        TextElement text = new TextElement("Alert").onRed();
        Cell cell = renderAndGetFirst("Alert", text, ".bg-red { background: #CC0000; }");

        assertThat(cell.style().bg()).isPresent();
        assertThat(cell.style().bg().get()).isInstanceOf(Color.Rgb.class);
        Color.Rgb bg = (Color.Rgb) cell.style().bg().get();
        assertThat(bg.r()).isEqualTo(0xCC);
        assertThat(bg.g()).isEqualTo(0x00);
        assertThat(bg.b()).isEqualTo(0x00);
    }

    @Test
    void genericCssRuleOverridesNamedColor() {
        // Named colors are "soft" — CSS always wins (standard CSS behavior)
        TextElement text = new TextElement("Error").red();
        Cell cell = renderAndGetFirst("Error", text, "* { color: white; }");

        // CSS white overrides the Named red
        assertThat(cell.style().fg()).isPresent();
        Color fg = cell.style().fg().get();
        assertThat(fg).isInstanceOf(Color.Named.class);
        assertThat(((Color.Named) fg).name()).isEqualTo("white");
    }

    @Test
    void namedColorFallsBackToAnsiWithoutCss() {
        // Without any CSS engine, Named colors render as their ANSI default
        TextElement text = new TextElement("Error").red();
        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        // No style engine set
        Buffer buffer = Buffer.empty(new Rect(0, 0, 80, 24));
        Frame frame = Frame.forTesting(buffer);

        text.render(frame, AREA, context);

        Cell cell = buffer.get(0, 0);
        assertThat(cell.style().fg()).isPresent();
        Color fg = cell.style().fg().get();
        // Should be the Named red (which delegates to ANSI red for rendering)
        assertThat(fg).isInstanceOf(Color.Named.class);
        assertThat(((Color.Named) fg).name()).isEqualTo("red");
    }

    @Test
    void explicitRgbColorStillOverridesCss() {
        // Explicit non-Named colors should still override CSS
        TextElement text = new TextElement("Custom").fg(Color.rgb(1, 2, 3));
        Cell cell = renderAndGetFirst("Custom", text, "* { color: white; }");

        assertThat(cell.style().fg()).isPresent();
        assertThat(cell.style().fg().get()).isInstanceOf(Color.Rgb.class);
        Color.Rgb fg = (Color.Rgb) cell.style().fg().get();
        assertThat(fg.r()).isEqualTo(1);
        assertThat(fg.g()).isEqualTo(2);
        assertThat(fg.b()).isEqualTo(3);
    }
}

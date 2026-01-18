/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Text;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MarkupTextElement.
 */
class MarkupTextElementTest {

    @Test
    @DisplayName("MarkupTextElement renders plain text")
    void rendersPlainText() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        markupText("Hello")
            .render(frame, area, RenderContext.empty());

        assertThat(buffer)
            .hasSymbolAt(0, 0, "H")
            .hasSymbolAt(1, 0, "e")
            .hasSymbolAt(2, 0, "l")
            .hasSymbolAt(3, 0, "l")
            .hasSymbolAt(4, 0, "o");
    }

    @Test
    @DisplayName("MarkupTextElement renders bold markup")
    void rendersBoldMarkup() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        markupText("[bold]Hello[/bold]")
            .render(frame, area, RenderContext.empty());

        assertThat(buffer).at(0, 0).hasSymbol("H");
        assertThat(buffer.get(0, 0).style().addModifiers()).contains(Modifier.BOLD);
    }

    @Test
    @DisplayName("MarkupTextElement renders color markup")
    void rendersColorMarkup() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        markupText("[red]Red[/red]")
            .render(frame, area, RenderContext.empty());

        assertThat(buffer).at(0, 0).hasSymbol("R").hasForeground(Color.RED);
    }

    @Test
    @DisplayName("MarkupTextElement resolves custom tags via CSS classes")
    void resolvesCustomTagsViaCssClasses() {
        // Set up StyleEngine with CSS defining .keyword class
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test",
            ".keyword {\n" +
            "    color: magenta;\n" +
            "    text-style: bold;\n" +
            "}");
        styleEngine.setActiveStylesheet("test");

        // Create context with StyleEngine
        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        // Render markup with custom [keyword] tag
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        markupText("[keyword]function[/keyword]")
            .render(frame, area, context);

        // The text "function" should be styled with magenta color
        assertThat(buffer)
            .at(0, 0).hasSymbol("f").hasForeground(Color.MAGENTA);
    }

    @Test
    @DisplayName("MarkupTextElement should not be dim when in row with dim siblings")
    void shouldNotBeDimInRowWithDimSiblings() {
        Rect area = new Rect(0, 0, 50, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        StyleEngine styleEngine = StyleEngine.create();
        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        // Mimic the demo: markupText followed by dim text elements
        row(
            markupText("Rich [red]live[/red] editor").cyan(),
            text(" Help ").dim()
        ).render(frame, area, context);

        // "R" in "Rich" should be cyan and NOT dim
        assertThat(buffer).at(0, 0).hasSymbol("R").hasForeground(Color.CYAN);
        assertThat(buffer.get(0, 0).style().addModifiers()).doesNotContain(Modifier.DIM);

        // "l" in "live" should be red and NOT dim
        assertThat(buffer).at(5, 0).hasSymbol("l").hasForeground(Color.RED);
        assertThat(buffer.get(5, 0).style().addModifiers()).doesNotContain(Modifier.DIM);
    }
}

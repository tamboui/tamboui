/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DialogElement.
 */
class DialogElementTest {

    @Test
    @DisplayName("styleAttributes exposes title")
    void styleAttributes_exposesTitle() {
        assertThat(dialog().title("Confirm").styleAttributes()).containsEntry("title", "Confirm");
    }

    @Test
    @DisplayName("Attribute selector [title] affects Dialog border color")
    void attributeSelector_title_affectsBorderColor() {
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", "DialogElement[title=\"Confirm\"] { border-color: cyan; }");
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 40, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        dialog("Confirm", text("Delete file?")).rounded().render(frame, area, context);

        // Dialog centers itself, so we need to find the border with cyan color
        // For a 40x10 area with default minWidth=20 and content, the dialog will be centered
        int borderX = -1, borderY = -1;
        for (int x = 0; x < 40 && borderX < 0; x++) {
            for (int y = 0; y < 10 && borderX < 0; y++) {
                Cell cell = buffer.get(x, y);
                if ("╭".equals(cell.symbol())) {
                    borderX = x;
                    borderY = y;
                }
            }
        }
        assertThat(borderX).as("Should find dialog border").isGreaterThanOrEqualTo(0);
        assertThat(buffer).at(borderX, borderY).hasSymbol("╭").hasForeground(Color.CYAN);
    }
}

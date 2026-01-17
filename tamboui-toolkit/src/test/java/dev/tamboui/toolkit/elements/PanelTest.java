/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Panel.
 */
class PanelTest {

    @Test
    @DisplayName("Panel exposes title attribute")
    void styleAttributes_exposesTitle() {
        assertThat(panel("Test Tree").styleAttributes()).containsEntry("title", "Test Tree");
    }

    @Test
    @DisplayName("Panel exposes bottom-title attribute")
    void styleAttributes_exposesBottomTitle() {
        assertThat(panel().bottomTitle("Status").styleAttributes()).containsEntry("bottom-title", "Status");
    }

    @Test
    @DisplayName("Panel without title has empty styleAttributes")
    void styleAttributes_emptyWithoutTitle() {
        assertThat(panel().styleAttributes()).isEmpty();
    }

    @Test
    @DisplayName("Panel includes generic attr() attributes")
    void styleAttributes_includesGenericAttrs() {
        assertThat(panel("Test").attr("data-type", "info").styleAttributes())
            .containsEntry("title", "Test")
            .containsEntry("data-type", "info");
    }

    @Test
    @DisplayName("Attribute selector affects Panel border color")
    void attributeSelector_affectsBorderColor() {
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", "Panel[title=\"Test\"] { border-color: cyan; }");
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        panel("Test").render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.CYAN);
    }
}

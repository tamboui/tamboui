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
 * Tests for LineGaugeElement.
 */
class LineGaugeElementTest {

    @Test
    @DisplayName("styleAttributes exposes label")
    void styleAttributes_exposesLabel() {
        assertThat(lineGauge(0.5).label("Processing").styleAttributes()).containsEntry("label", "Processing");
    }

    @Test
    @DisplayName("styleAttributes empty without label")
    void styleAttributes_emptyWithoutLabel() {
        assertThat(lineGauge(0.5).styleAttributes()).isEmpty();
    }

    @Test
    @DisplayName("Attribute selector [label] affects LineGauge filled style via child selector")
    void attributeSelector_label_affectsFilledStyle() {
        StyleEngine styleEngine = StyleEngine.create();
        // Use child selector to style the filled portion of LineGauge with matching label
        styleEngine.addStylesheet("test",
                "LineGaugeElement[label=\"Processing\"] LineGaugeElement-filled { color: cyan; }");
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 30, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        // Render with 50% progress - "Processing" is 10 chars, gauge starts after label
        lineGauge(0.5).label("Processing").render(frame, area, context);

        // The filled portion of the gauge should have cyan color
        // Label is "Processing" (10 chars), gauge starts at position 10
        assertThat(buffer.get(10, 0).style().fg()).contains(Color.CYAN);
    }
}

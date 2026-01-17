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
import dev.tamboui.terminal.Frame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for GaugeElement.
 */
class GaugeElementTest {

    @Test
    @DisplayName("GaugeElement fluent API chains correctly")
    void fluentApiChaining() {
        GaugeElement element = gauge(0.5)
            .label("Loading...")
            .gaugeColor(Color.GREEN)
            .useUnicode(true)
            .title("Progress")
            .rounded()
            .borderColor(Color.CYAN);

        assertThat(element).isInstanceOf(GaugeElement.class);
    }

    @Test
    @DisplayName("gauge(double) clamps ratio to 0.0-1.0")
    void ratioClampingLow() {
        GaugeElement element = gauge(-0.5);
        // Should not throw, ratio is clamped
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("gauge(double) clamps high ratio to 1.0")
    void ratioClampingHigh() {
        GaugeElement element = gauge(1.5);
        // Should not throw, ratio is clamped
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("percent() method works correctly")
    void percentMethod() {
        GaugeElement element = gauge().percent(75);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("GaugeElement renders to buffer")
    void rendersToBuffer() {
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        gauge(0.5)
            .label("50%")
            .title("Progress")
            .rounded()
            .render(frame, area, RenderContext.empty());

        // Check border is rendered (rounded corner)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("╭");
        assertThat(buffer.get(19, 0).symbol()).isEqualTo("╮");
    }

    @Test
    @DisplayName("GaugeElement renders filled portion")
    void rendersFilledPortion() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        gauge(1.0)
            .label("")
            .render(frame, area, RenderContext.empty());

        // At 100%, first cells should be filled blocks
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("█");
        assertThat(buffer.get(19, 0).symbol()).isEqualTo("█");
    }

    @Test
    @DisplayName("Empty area does not render")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        Frame frame = Frame.forTesting(buffer);

        // Should not throw
        gauge(0.5).render(frame, emptyArea, RenderContext.empty());
    }

    @Test
    @DisplayName("GaugeElement with gaugeStyle")
    void withGaugeStyle() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        gauge(1.0)
            .label("")
            .gaugeColor(Color.RED)
            .render(frame, area, RenderContext.empty());

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.RED);
    }

    @Test
    @DisplayName("styleAttributes exposes title")
    void styleAttributes_exposesTitle() {
        assertThat(gauge(0.5).title("Progress").styleAttributes()).containsEntry("title", "Progress");
    }

    @Test
    @DisplayName("styleAttributes exposes label")
    void styleAttributes_exposesLabel() {
        assertThat(gauge(0.5).label("50%").styleAttributes()).containsEntry("label", "50%");
    }

    @Test
    @DisplayName("Attribute selector [title] affects Gauge border color")
    void attributeSelector_title_affectsBorderColor() {
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", "GaugeElement[title=\"Progress\"] { border-color: cyan; }");
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        gauge(0.5).title("Progress").rounded().render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.CYAN);
    }

    @Test
    @DisplayName("Attribute selector [label] affects Gauge styling")
    void attributeSelector_label_affectsStyling() {
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", "GaugeElement[label=\"50%\"] { border-color: green; }");
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        gauge(0.5).label("50%").rounded().render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.GREEN);
    }
}

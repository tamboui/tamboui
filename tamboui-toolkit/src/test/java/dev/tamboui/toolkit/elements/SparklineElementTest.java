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

import java.util.Arrays;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SparklineElement.
 */
class SparklineElementTest {

    @Test
    @DisplayName("SparklineElement fluent API chains correctly")
    void fluentApiChaining() {
        SparklineElement element = sparkline(1, 2, 3, 4, 5)
            .color(Color.CYAN)
            .title("CPU Usage")
            .rounded()
            .borderColor(Color.GREEN);

        assertThat(element).isInstanceOf(SparklineElement.class);
    }

    @Test
    @DisplayName("sparkline() creates empty element")
    void emptySparkline() {
        SparklineElement element = sparkline();
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("sparkline(long...) accepts long array")
    void sparklineWithLongArray() {
        SparklineElement element = sparkline(10L, 20L, 30L, 40L);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("sparkline(int...) accepts int array")
    void sparklineWithIntArray() {
        SparklineElement element = sparkline(10, 20, 30, 40);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("sparkline(Collection) accepts collection")
    void sparklineWithCollection() {
        SparklineElement element = sparkline(Arrays.asList(1, 2, 3, 4, 5));
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("data(long...) replaces data")
    void dataMethod() {
        SparklineElement element = sparkline().data(5L, 10L, 15L);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("SparklineElement renders to buffer")
    void rendersToBuffer() {
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        sparkline(1, 2, 3, 4, 5, 6, 7, 8)
            .title("Chart")
            .rounded()
            .render(frame, area, RenderContext.empty());

        // Check border is rendered
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("╭");
    }

    @Test
    @DisplayName("Empty area does not render")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 3));
        Frame frame = Frame.forTesting(buffer);

        // Should not throw
        sparkline(1, 2, 3).render(frame, emptyArea, RenderContext.empty());
    }

    @Test
    @DisplayName("SparklineElement with color")
    void withColor() {
        Rect area = new Rect(0, 0, 10, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        sparkline(8, 8, 8, 8, 8, 8, 8, 8)
            .color(Color.MAGENTA)
            .render(frame, area, RenderContext.empty());

        // The sparkline data should have the color applied
        // (checking that rendering completes without error)
        assertThat(buffer).isNotNull();
    }

    @Test
    @DisplayName("SparklineElement with max value")
    void withMaxValue() {
        SparklineElement element = sparkline(1, 2, 3).max(100L);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("styleAttributes exposes title")
    void styleAttributes_exposesTitle() {
        assertThat(sparkline(1, 2, 3).title("CPU").styleAttributes()).containsEntry("title", "CPU");
    }

    @Test
    @DisplayName("Attribute selector [title] affects Sparkline border color")
    void attributeSelector_title_affectsBorderColor() {
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", "SparklineElement[title=\"CPU\"] { border-color: cyan; }");
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        sparkline(1, 2, 3, 4, 5).title("CPU").rounded().render(frame, area, context);

        assertThat(buffer).at(0, 0).hasSymbol("╭").hasForeground(Color.CYAN);
    }
}

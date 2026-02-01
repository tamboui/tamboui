/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for FlowElement.
 */
class FlowTest {

    @Test
    @DisplayName("children flow left-to-right")
    void childrenFlowLeftToRight() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        // "AB" (2) + "CD" (2) + "EF" (2) = 6, fits in 20
        flow(text("AB"), text("CD"), text("EF"))
            .render(frame, area, RenderContext.empty());

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(2, 0, "C")
            .hasSymbolAt(4, 0, "E");
    }

    @Test
    @DisplayName("children wrap at available width")
    void wrappingAtWidth() {
        Rect area = new Rect(0, 0, 5, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        // "AB"(2) + "CD"(2) = 4 fits in 5, "EF"(2) wraps to row 2
        flow(text("AB"), text("CD"), text("EF"))
            .render(frame, area, RenderContext.empty());

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(2, 0, "C")
            .hasSymbolAt(0, 1, "E");
    }

    @Test
    @DisplayName("preferredWidth returns sum of all children")
    void preferredWidth() {
        FlowElement f = flow(text("AB"), text("CD"), text("EF"));
        // 2 + 2 + 2 = 6
        assertThat(f.preferredWidth()).isEqualTo(6);
    }

    @Test
    @DisplayName("preferredWidth includes spacing")
    void preferredWidthWithSpacing() {
        FlowElement f = flow(text("AB"), text("CD"), text("EF")).spacing(1);
        // 2 + 1 + 2 + 1 + 2 = 8
        assertThat(f.preferredWidth()).isEqualTo(8);
    }

    @Test
    @DisplayName("preferredWidth includes margin")
    void preferredWidthWithMargin() {
        FlowElement f = flow(text("AB")).margin(new Margin(0, 2, 0, 3));
        // 2 + 2 + 3 = 7
        assertThat(f.preferredWidth()).isEqualTo(7);
    }

    @Test
    @DisplayName("empty flow does not render")
    void emptyFlow() {
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        flow()
            .render(frame, area, RenderContext.empty());

        BufferAssertions.assertThat(buffer).isEqualTo(Buffer.empty(area));
    }

    @Test
    @DisplayName("flow created from collection")
    void flowFromCollection() {
        FlowElement f = flow(Arrays.asList(text("A"), text("BB")));
        assertThat(f.preferredWidth()).isEqualTo(3);
    }

    @Test
    @DisplayName("fluent API chains correctly")
    void fluentApiChaining() {
        FlowElement f = flow(text("A"))
            .spacing(1)
            .rowSpacing(2)
            .margin(1)
            .margin(new Margin(1, 2, 3, 4));

        assertThat(f).isInstanceOf(FlowElement.class);
    }

    // ==================== CSS property tests ====================

    @Nested
    @DisplayName("CSS property resolution")
    class CssTests {

        private DefaultRenderContext cssContext(String css) {
            StyleEngine styleEngine = StyleEngine.create();
            styleEngine.addStylesheet("test", css);
            styleEngine.setActiveStylesheet("test");
            DefaultRenderContext ctx = DefaultRenderContext.createEmpty();
            ctx.setStyleEngine(styleEngine);
            return ctx;
        }

        @Test
        @DisplayName("spacing from CSS")
        void cssSpacing() {
            DefaultRenderContext ctx = cssContext(".f { spacing: 2; }");

            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            flow(text("AB"), text("CD"))
                .addClass("f")
                .render(frame, area, ctx);

            // "AB" at x=0, spacing=2, "CD" at x=4
            BufferAssertions.assertThat(buffer)
                .hasSymbolAt(0, 0, "A")
                .hasSymbolAt(4, 0, "C");
        }

        @Test
        @DisplayName("flow-row-spacing from CSS")
        void cssFlowRowSpacing() {
            DefaultRenderContext ctx = cssContext(".f { flow-row-spacing: 1; }");

            Rect area = new Rect(0, 0, 3, 4);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // "AB"(2) fits on row 0, "CD"(2) wraps to row with spacing
            flow(text("AB"), text("CD"))
                .addClass("f")
                .render(frame, area, ctx);

            BufferAssertions.assertThat(buffer)
                .hasSymbolAt(0, 0, "A")
                .hasSymbolAt(0, 2, "C");  // y=0+1+1=2 (height 1 + rowSpacing 1)
        }

        @Test
        @DisplayName("margin from CSS")
        void cssMargin() {
            DefaultRenderContext ctx = cssContext(".f { margin: 1; }");

            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            flow(text("X"))
                .addClass("f")
                .render(frame, area, ctx);

            // With margin 1, text renders at (1,1)
            BufferAssertions.assertThat(buffer).hasSymbolAt(1, 1, "X");
        }

        @Test
        @DisplayName("programmatic values override CSS")
        void programmaticOverridesCss() {
            DefaultRenderContext ctx = cssContext(".f { spacing: 5; }");

            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Programmatic spacing(0) overrides CSS spacing: 5
            flow(text("AB"), text("CD"))
                .addClass("f")
                .spacing(0)
                .render(frame, area, ctx);

            // "AB" at x=0, "CD" at x=2 (no spacing)
            BufferAssertions.assertThat(buffer)
                .hasSymbolAt(0, 0, "A")
                .hasSymbolAt(2, 0, "C");
        }
    }
}

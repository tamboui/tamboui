/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.ContentAlignment;
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
 * Tests for StackElement.
 */
class StackTest {

    @Test
    @DisplayName("last child renders on top")
    void lastChildOnTop() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        stack(text("AAAAAAAAAA"), text("B"))
            .render(frame, area, RenderContext.empty());

        // "B" renders on top of "A" at position 0
        BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "B");
        // Rest of the line still has "A" from first child
        BufferAssertions.assertThat(buffer).hasSymbolAt(1, 0, "A");
    }

    @Test
    @DisplayName("preferredWidth returns max of all children")
    void preferredWidth() {
        StackElement s = stack(text("Hi"), text("Hello"), text("Hey"));
        // "Hello" is the widest at 5
        assertThat(s.preferredWidth()).isEqualTo(5);
    }

    @Test
    @DisplayName("preferredWidth includes margin")
    void preferredWidthWithMargin() {
        StackElement s = stack(text("Hello")).margin(new Margin(0, 2, 0, 3));
        // 5 + 2 + 3 = 10
        assertThat(s.preferredWidth()).isEqualTo(10);
    }

    @Test
    @DisplayName("empty stack does not render")
    void emptyStack() {
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        stack()
            .render(frame, area, RenderContext.empty());

        BufferAssertions.assertThat(buffer).isEqualTo(Buffer.empty(area));
    }

    @Test
    @DisplayName("single child renders correctly")
    void singleChild() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        stack(text("Hello"))
            .render(frame, area, RenderContext.empty());

        BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "H");
    }

    @Test
    @DisplayName("stack created from collection")
    void stackFromCollection() {
        StackElement s = stack(Arrays.asList(text("A"), text("BB")));
        assertThat(s.preferredWidth()).isEqualTo(2);
    }

    @Test
    @DisplayName("fluent API chains correctly")
    void fluentApiChaining() {
        StackElement s = stack(text("A"))
            .alignment(ContentAlignment.CENTER)
            .margin(1)
            .margin(new Margin(1, 2, 3, 4));

        assertThat(s).isInstanceOf(StackElement.class);
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
        @DisplayName("content-align from CSS")
        void cssContentAlign() {
            DefaultRenderContext ctx = cssContext(".s { content-align: center; }");

            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // "X" is 1 char wide, 1 char high
            // center in 20x5: x=(20-1)/2=9, y=(5-1)/2=2
            stack(text("X"))
                .addClass("s")
                .render(frame, area, ctx);

            BufferAssertions.assertThat(buffer).hasSymbolAt(9, 2, "X");
        }

        @Test
        @DisplayName("margin from CSS")
        void cssMargin() {
            DefaultRenderContext ctx = cssContext(".s { margin: 1; }");

            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            stack(text("X"))
                .addClass("s")
                .render(frame, area, ctx);

            // With margin 1 and STRETCH, text renders at (1,1)
            BufferAssertions.assertThat(buffer).hasSymbolAt(1, 1, "X");
        }

        @Test
        @DisplayName("programmatic values override CSS")
        void programmaticOverridesCss() {
            DefaultRenderContext ctx = cssContext(".s { content-align: top-left; }");

            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Programmatic STRETCH overrides CSS top-left
            stack(text("X"))
                .addClass("s")
                .alignment(ContentAlignment.STRETCH)
                .render(frame, area, ctx);

            // With STRETCH, text renders at (0,0)
            BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "X");
        }
    }
}

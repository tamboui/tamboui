/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.RenderContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DockElement.
 */
class DockTest {

    @Test
    @DisplayName("renders all 5 regions")
    void allFiveRegions() {
        Rect area = new Rect(0, 0, 30, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        dock()
            .top(text("T"))
            .bottom(text("B"))
            .left(text("L"))
            .right(text("R"))
            .center(text("C"))
            .topHeight(Constraint.length(2))
            .bottomHeight(Constraint.length(2))
            .leftWidth(Constraint.length(5))
            .rightWidth(Constraint.length(5))
            .render(frame, area, RenderContext.empty());

        // Top: y=0
        BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "T");
        // Bottom: y=8
        BufferAssertions.assertThat(buffer).hasSymbolAt(0, 8, "B");
        // Left: y=2, x=0
        BufferAssertions.assertThat(buffer).hasSymbolAt(0, 2, "L");
        // Right: y=2, x=25
        BufferAssertions.assertThat(buffer).hasSymbolAt(25, 2, "R");
        // Center: y=2, x=5
        BufferAssertions.assertThat(buffer).hasSymbolAt(5, 2, "C");
    }

    @Test
    @DisplayName("center only layout works")
    void centerOnly() {
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        dock()
            .center(text("C"))
            .render(frame, area, RenderContext.empty());

        BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "C");
    }

    @Test
    @DisplayName("omitted regions do not leave gaps")
    void omittedRegions() {
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        dock()
            .top(text("T"))
            .center(text("C"))
            .topHeight(Constraint.length(2))
            .render(frame, area, RenderContext.empty());

        // Top at y=0
        BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "T");
        // Center at y=2 (after top)
        BufferAssertions.assertThat(buffer).hasSymbolAt(0, 2, "C");
    }

    @Test
    @DisplayName("preferredWidth computes correctly")
    void preferredWidth() {
        DockElement d = dock()
            .left(text("Left"))
            .center(text("Center"))
            .right(text("Right"))
            .leftWidth(Constraint.length(5))
            .rightWidth(Constraint.length(5));

        // leftWidth(5) + "Center"(6) + rightWidth(5) = 16
        assertThat(d.preferredWidth()).isEqualTo(16);
    }

    @Test
    @DisplayName("preferredWidth includes margin")
    void preferredWidthWithMargin() {
        DockElement d = dock()
            .center(text("Hello"))
            .margin(new Margin(1, 2, 1, 3));

        // "Hello"(5) + left(3) + right(2) = 10
        assertThat(d.preferredWidth()).isEqualTo(10);
    }

    @Test
    @DisplayName("empty area does not render")
    void emptyArea() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 5));
        Frame frame = Frame.forTesting(buffer);

        dock()
            .center(text("Test"))
            .render(frame, emptyArea, RenderContext.empty());

        BufferAssertions.assertThat(buffer).isEqualTo(Buffer.empty(new Rect(0, 0, 20, 5)));
    }

    @Test
    @DisplayName("fluent API chains correctly")
    void fluentApiChaining() {
        DockElement d = dock()
            .top(text("T"))
            .bottom(text("B"))
            .left(text("L"))
            .right(text("R"))
            .center(text("C"))
            .topHeight(Constraint.length(3))
            .bottomHeight(Constraint.length(1))
            .leftWidth(Constraint.length(20))
            .rightWidth(Constraint.length(10))
            .margin(1)
            .margin(new Margin(1, 2, 3, 4));

        assertThat(d).isInstanceOf(DockElement.class);
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
        @DisplayName("dock-top-height from CSS")
        void cssDockTopHeight() {
            DefaultRenderContext ctx = cssContext(".d { dock-top-height: 3; }");

            Rect area = new Rect(0, 0, 20, 10);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            dock()
                .top(text("T"))
                .center(text("C"))
                .addClass("d")
                .render(frame, area, ctx);

            // Top at y=0
            BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "T");
            // Center at y=3 (after 3-row top)
            BufferAssertions.assertThat(buffer).hasSymbolAt(0, 3, "C");
        }

        @Test
        @DisplayName("dock-bottom-height from CSS")
        void cssDockBottomHeight() {
            DefaultRenderContext ctx = cssContext(".d { dock-bottom-height: 3; }");

            Rect area = new Rect(0, 0, 20, 10);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            dock()
                .bottom(text("B"))
                .center(text("C"))
                .addClass("d")
                .render(frame, area, ctx);

            // Bottom at y=7 (10-3=7)
            BufferAssertions.assertThat(buffer).hasSymbolAt(0, 7, "B");
        }

        @Test
        @DisplayName("dock-left-width from CSS")
        void cssDockLeftWidth() {
            DefaultRenderContext ctx = cssContext(".d { dock-left-width: 8; }");

            Rect area = new Rect(0, 0, 30, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            dock()
                .left(text("L"))
                .center(text("C"))
                .addClass("d")
                .render(frame, area, ctx);

            // Left at x=0
            BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "L");
            // Center at x=8
            BufferAssertions.assertThat(buffer).hasSymbolAt(8, 0, "C");
        }

        @Test
        @DisplayName("dock-right-width from CSS")
        void cssDockRightWidth() {
            DefaultRenderContext ctx = cssContext(".d { dock-right-width: 8; }");

            Rect area = new Rect(0, 0, 30, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            dock()
                .right(text("R"))
                .center(text("C"))
                .addClass("d")
                .render(frame, area, ctx);

            // Right at x=22 (30-8=22)
            BufferAssertions.assertThat(buffer).hasSymbolAt(22, 0, "R");
        }

        @Test
        @DisplayName("margin from CSS")
        void cssMargin() {
            DefaultRenderContext ctx = cssContext(".d { margin: 1; }");

            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            dock()
                .center(text("C"))
                .addClass("d")
                .render(frame, area, ctx);

            // With margin 1, center starts at (1,1) instead of (0,0)
            BufferAssertions.assertThat(buffer).hasSymbolAt(1, 1, "C");
        }

        @Test
        @DisplayName("programmatic values override CSS")
        void programmaticOverridesCss() {
            DefaultRenderContext ctx = cssContext(".d { dock-top-height: 5; }");

            Rect area = new Rect(0, 0, 20, 10);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Programmatic topHeight(2) overrides CSS dock-top-height: 5
            dock()
                .top(text("T"))
                .center(text("C"))
                .addClass("d")
                .topHeight(Constraint.length(2))
                .render(frame, area, ctx);

            // Center at y=2 (not y=5)
            BufferAssertions.assertThat(buffer).hasSymbolAt(0, 2, "C");
        }
    }

    // ==================== Fit content tests ====================

    @Nested
    @DisplayName("Fit content (preferred size) behavior")
    class FitContentTests {

        @Test
        @DisplayName("panel with row fits to content height (3 = borders + 1 row)")
        void panelWithRowFitsToContent() {
            Rect area = new Rect(0, 0, 40, 10);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Panel with a row should have height 3: top border + row content + bottom border
            dock()
                .top(panel(() -> row(text("Header"))))
                .center(text("C"))
                .render(frame, area, RenderContext.empty());

            // Top panel border at y=0
            BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "┌");
            // Panel content (Header) at y=1
            BufferAssertions.assertThat(buffer).hasSymbolAt(1, 1, "H");
            // Panel bottom border at y=2
            BufferAssertions.assertThat(buffer).hasSymbolAt(0, 2, "└");
            // Center at y=3 (after 3-row panel)
            BufferAssertions.assertThat(buffer).hasSymbolAt(0, 3, "C");
        }

        @Test
        @DisplayName("panel with multiple texts in column fits height")
        void panelWithColumnFitsHeight() {
            Rect area = new Rect(0, 0, 40, 10);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Panel with 2 text lines should have height 4: borders(2) + lines(2)
            dock()
                .top(panel(text("Line1"), text("Line2")))
                .center(text("C"))
                .render(frame, area, RenderContext.empty());

            // Panel top border at y=0
            BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "┌");
            // Line1 at y=1
            BufferAssertions.assertThat(buffer).hasSymbolAt(1, 1, "L");
            // Line2 at y=2
            BufferAssertions.assertThat(buffer).hasSymbolAt(1, 2, "L");
            // Panel bottom border at y=3
            BufferAssertions.assertThat(buffer).hasSymbolAt(0, 3, "└");
            // Center at y=4
            BufferAssertions.assertThat(buffer).hasSymbolAt(0, 4, "C");
        }

        @Test
        @DisplayName("explicit constraint overrides fit content")
        void explicitConstraintOverridesFitContent() {
            Rect area = new Rect(0, 0, 40, 10);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Even with panel (preferred 3), explicit constraint wins
            dock()
                .top(panel(() -> row(text("Header"))), Constraint.length(5))
                .center(text("C"))
                .render(frame, area, RenderContext.empty());

            // Center at y=5 (not y=3)
            BufferAssertions.assertThat(buffer).hasSymbolAt(0, 5, "C");
        }

        @Test
        @DisplayName("bottom panel also fits content")
        void bottomPanelFitsContent() {
            Rect area = new Rect(0, 0, 40, 10);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            dock()
                .center(text("C"))
                .bottom(panel(() -> row(text("Footer"))))
                .render(frame, area, RenderContext.empty());

            // Center at y=0 (top of remaining area)
            BufferAssertions.assertThat(buffer).hasSymbolAt(0, 0, "C");
            // Bottom panel starts at y=7 (10-3=7)
            BufferAssertions.assertThat(buffer).hasSymbolAt(0, 7, "┌");
            // Footer text at y=8
            BufferAssertions.assertThat(buffer).hasSymbolAt(1, 8, "F");
            // Bottom panel bottom border at y=9
            BufferAssertions.assertThat(buffer).hasSymbolAt(0, 9, "└");
        }
    }
}

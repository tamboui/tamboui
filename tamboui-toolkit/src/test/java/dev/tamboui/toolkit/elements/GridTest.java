/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
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
 * Tests for GridElement.
 */
class GridTest {

    // ==================== Basic tests ====================

    @Test
    @DisplayName("preferredWidth() returns 0 for empty grid")
    void preferredWidth_emptyGrid() {
        GridElement g = grid();
        assertThat(g.preferredWidth()).isEqualTo(0);
    }

    @Test
    @DisplayName("preferredWidth() returns child width for single child")
    void preferredWidth_singleChild() {
        GridElement g = grid(text("Hello"));
        // "Hello" = 5, 1 col (ceil(sqrt(1))=1)
        assertThat(g.preferredWidth()).isEqualTo(5);
    }

    @Test
    @DisplayName("preferredWidth() includes horizontal gutter")
    void preferredWidth_withGutter() {
        // 4 items -> ceil(sqrt(4))=2 columns, max child width=1
        // 1*2 + 2*(2-1) = 4
        GridElement g = grid(text("A"), text("B"), text("C"), text("D")).gutter(2);
        assertThat(g.preferredWidth()).isEqualTo(4);
    }

    @Test
    @DisplayName("preferredWidth() includes margin")
    void preferredWidth_withMargin() {
        GridElement g = grid(text("Hello")).margin(new Margin(1, 2, 1, 3));
        // 5 + 2 (right) + 3 (left) = 10
        assertThat(g.preferredWidth()).isEqualTo(10);
    }

    @Test
    @DisplayName("Grid renders 4 items in a 2x2 grid")
    void rendersToBuffer() {
        // 4 items "A","B","C","D" with gridSize(2) in 20x2 area
        // 2 cols of 10 each, 2 rows
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        grid(text("A"), text("B"), text("C"), text("D"))
            .gridSize(2)
            .render(frame, area, RenderContext.empty());

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(0, 1, "C")
            .hasSymbolAt(10, 1, "D");
    }

    @Test
    @DisplayName("Empty area does not render")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 5));
        Frame frame = Frame.forTesting(buffer);

        // Should not throw
        grid(text("Test")).render(frame, emptyArea, RenderContext.empty());

        BufferAssertions.assertThat(buffer).isEqualTo(Buffer.empty(new Rect(0, 0, 20, 5)));
    }

    // ==================== Grid sizing tests ====================

    @Test
    @DisplayName("Auto grid size uses ceil(sqrt(n)) columns")
    void autoGridSize() {
        // 4 items -> ceil(sqrt(4))=2 cols, 2 rows
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        grid(text("A"), text("B"), text("C"), text("D"))
            .render(frame, area, RenderContext.empty());

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(0, 1, "C")
            .hasSymbolAt(10, 1, "D");

        // 9 items -> ceil(sqrt(9))=3 cols, 3 rows
        Rect area9 = new Rect(0, 0, 30, 3);
        Buffer buffer9 = Buffer.empty(area9);
        Frame frame9 = Frame.forTesting(buffer9);

        grid(text("A"), text("B"), text("C"),
             text("D"), text("E"), text("F"),
             text("G"), text("H"), text("I"))
            .render(frame9, area9, RenderContext.empty());

        BufferAssertions.assertThat(buffer9)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(20, 0, "C")
            .hasSymbolAt(0, 1, "D")
            .hasSymbolAt(10, 1, "E")
            .hasSymbolAt(20, 1, "F")
            .hasSymbolAt(0, 2, "G")
            .hasSymbolAt(10, 2, "H")
            .hasSymbolAt(20, 2, "I");
    }

    @Test
    @DisplayName("Explicit gridSize forces dimensions")
    void explicitGridSize() {
        // gridSize(3, 2) forces 3 columns, 2 rows in a 30x2 area
        Rect area = new Rect(0, 0, 30, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        grid(text("A"), text("B"), text("C"),
             text("D"), text("E"), text("F"))
            .gridSize(3, 2)
            .render(frame, area, RenderContext.empty());

        // 3 cols of 10 each
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(20, 0, "C")
            .hasSymbolAt(0, 1, "D")
            .hasSymbolAt(10, 1, "E")
            .hasSymbolAt(20, 1, "F");
    }

    @Test
    @DisplayName("gridSize with columns only auto-calculates rows")
    void gridSizeColumnsOnly() {
        // gridSize(3) with 7 items -> 3 cols, ceil(7/3)=3 rows
        GridElement g = grid(
            text("A"), text("B"), text("C"),
            text("D"), text("E"), text("F"),
            text("G")
        ).gridSize(3);

        Rect area = new Rect(0, 0, 30, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        g.render(frame, area, RenderContext.empty());

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(20, 0, "C")
            .hasSymbolAt(0, 1, "D")
            .hasSymbolAt(10, 1, "E")
            .hasSymbolAt(20, 1, "F")
            .hasSymbolAt(0, 2, "G");
    }

    // ==================== Constraint tests ====================

    @Test
    @DisplayName("Grid columns constraints control column widths")
    void gridColumnsConstraints() {
        // gridColumns(length(10), fill()) in a 30-wide area with 2 cols
        // col0: 10 wide, col1: fill -> 20
        Rect area = new Rect(0, 0, 30, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        grid(text("A"), text("B"))
            .gridSize(2)
            .gridColumns(Constraint.length(10), Constraint.fill())
            .render(frame, area, RenderContext.empty());

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B");
    }

    @Test
    @DisplayName("Grid columns constraints cycle when fewer than columns")
    void gridColumnsConstraintsCycling() {
        // gridColumns(length(8)) with 3 columns in 30-wide area
        // All 3 columns get length(8) via cycling
        Rect area = new Rect(0, 0, 30, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        grid(text("A"), text("B"), text("C"))
            .gridSize(3)
            .gridColumns(Constraint.length(8))
            .render(frame, area, RenderContext.empty());

        // Each col is 8 wide: col0 at x=0, col1 at x=8, col2 at x=16
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(8, 0, "B")
            .hasSymbolAt(16, 0, "C");
    }

    @Test
    @DisplayName("Grid rows constraints control row heights")
    void gridRowsConstraints() {
        // gridRows(length(2), length(3)) in a 20x5 area with 2 rows
        // row0: height 2, row1: height 3
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        grid(text("A"), text("B"), text("C"), text("D"))
            .gridSize(2)
            .gridRows(Constraint.length(2), Constraint.length(3))
            .render(frame, area, RenderContext.empty());

        // row0 at y=0 (height 2), row1 at y=2 (height 3)
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(0, 2, "C")
            .hasSymbolAt(10, 2, "D");
    }

    @Test
    @DisplayName("Gutter creates gaps between cells")
    void gutterBetweenCells() {
        // 2x2 grid with gutter(1) in 21x3 area
        // horizontal: 2 cols with 1 gutter -> constraints [fill, len(1), fill] -> 10, 1, 10
        // vertical: per-row heights (1 each), 1 gutter between
        Rect area = new Rect(0, 0, 21, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        grid(text("A"), text("B"), text("C"), text("D"))
            .gridSize(2)
            .gutter(1)
            .render(frame, area, RenderContext.empty());

        // col0: x=0,w=10; gutter x=10; col1: x=11,w=10
        // row0: y=0; gutter y=1; row1: y=2
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(11, 0, "B")
            .hasSymbolAt(0, 2, "C")
            .hasSymbolAt(11, 2, "D");
    }

    @Test
    @DisplayName("Asymmetric gutter has different horizontal and vertical values")
    void gutterAsymmetric() {
        // 2x2 grid with gutter(2, 1) in 22x3 area
        // horizontal: 2 cols with 2 gutter -> [fill, len(2), fill] -> 10, 2, 10
        // vertical: row heights (1 each), 1 gutter between
        Rect area = new Rect(0, 0, 22, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        grid(text("A"), text("B"), text("C"), text("D"))
            .gridSize(2)
            .gutter(2, 1)
            .render(frame, area, RenderContext.empty());

        // col0: x=0,w=10; gutter x=10,w=2; col1: x=12,w=10
        // row0: y=0; gutter 1; row1: y=2
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(12, 0, "B")
            .hasSymbolAt(0, 2, "C")
            .hasSymbolAt(12, 2, "D");
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
        @DisplayName("grid-size from CSS")
        void cssGridSize() {
            DefaultRenderContext ctx = cssContext(".g { grid-size: 2; }");

            Rect area = new Rect(0, 0, 20, 2);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            grid(text("A"), text("B"), text("C"), text("D"))
                .addClass("g")
                .render(frame, area, ctx);

            BufferAssertions.assertThat(buffer)
                .hasSymbolAt(0, 0, "A")
                .hasSymbolAt(10, 0, "B")
                .hasSymbolAt(0, 1, "C")
                .hasSymbolAt(10, 1, "D");
        }

        @Test
        @DisplayName("grid-columns from CSS")
        void cssGridColumns() {
            DefaultRenderContext ctx = cssContext(".g { grid-size: 2; grid-columns: fill fill(2); }");

            Rect area = new Rect(0, 0, 30, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            grid(text("A"), text("B"))
                .addClass("g")
                .render(frame, area, ctx);

            // fill + fill(2) in 30 wide: col0 = 10, col1 = 20
            BufferAssertions.assertThat(buffer)
                .hasSymbolAt(0, 0, "A")
                .hasSymbolAt(10, 0, "B");
        }

        @Test
        @DisplayName("grid-rows from CSS")
        void cssGridRows() {
            DefaultRenderContext ctx = cssContext(".g { grid-size: 2; grid-rows: 2 3; }");

            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            grid(text("A"), text("B"), text("C"), text("D"))
                .addClass("g")
                .render(frame, area, ctx);

            // row0 at y=0 (height 2), row1 at y=2 (height 3)
            BufferAssertions.assertThat(buffer)
                .hasSymbolAt(0, 0, "A")
                .hasSymbolAt(10, 0, "B")
                .hasSymbolAt(0, 2, "C")
                .hasSymbolAt(10, 2, "D");
        }

        @Test
        @DisplayName("grid-gutter from CSS")
        void cssGridGutter() {
            DefaultRenderContext ctx = cssContext(".g { grid-size: 2; grid-gutter: 1 2; }");

            Rect area = new Rect(0, 0, 21, 4);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            grid(text("A"), text("B"), text("C"), text("D"))
                .addClass("g")
                .render(frame, area, ctx);

            // horizontal gutter 1: col0 x=0,w=10; gutter x=10; col1 x=11,w=10
            // vertical gutter 2: row0 y=0; gutter 2; row1 y=3
            BufferAssertions.assertThat(buffer)
                .hasSymbolAt(0, 0, "A")
                .hasSymbolAt(11, 0, "B")
                .hasSymbolAt(0, 3, "C")
                .hasSymbolAt(11, 3, "D");
        }

        @Test
        @DisplayName("Programmatic values override CSS")
        void programmaticOverridesCss() {
            DefaultRenderContext ctx = cssContext(
                ".g { grid-size: 3; grid-gutter: 5; }");

            Rect area = new Rect(0, 0, 20, 2);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Programmatic gridSize(2) and gutter(0) override CSS grid-size: 3 and grid-gutter: 5
            grid(text("A"), text("B"), text("C"), text("D"))
                .addClass("g")
                .gridSize(2)
                .gutter(0)
                .render(frame, area, ctx);

            BufferAssertions.assertThat(buffer)
                .hasSymbolAt(0, 0, "A")
                .hasSymbolAt(10, 0, "B")
                .hasSymbolAt(0, 1, "C")
                .hasSymbolAt(10, 1, "D");
        }
    }

    // ==================== Fluent API test ====================

    @Test
    @DisplayName("Fluent API chains correctly")
    void fluentApiChaining() {
        GridElement g = grid(text("A"), text("B"))
            .gridSize(2, 1)
            .gridColumns(Constraint.fill(), Constraint.fill())
            .gridRows(Constraint.fill())
            .gutter(1)
            .gutter(1, 2)
            .margin(1)
            .margin(new Margin(1, 2, 3, 4))
            .flex(Flex.CENTER);

        assertThat(g).isInstanceOf(GridElement.class);
    }

    @Test
    @DisplayName("Grid created from collection")
    void gridFromCollection() {
        GridElement g = grid(Arrays.asList(text("A"), text("B"), text("C")));
        assertThat(g.preferredWidth()).isGreaterThan(0);
    }
}

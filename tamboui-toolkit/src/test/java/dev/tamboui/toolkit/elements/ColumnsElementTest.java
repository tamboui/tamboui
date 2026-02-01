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
import dev.tamboui.layout.Flex;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.layout.columns.ColumnOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ColumnsElement.
 */
class ColumnsElementTest {

    @Test
    @DisplayName("preferredWidth() returns 0 for empty columns")
    void preferredWidth_emptyColumns() {
        ColumnsElement cols = columns();
        assertThat(cols.preferredWidth()).isEqualTo(0);
    }

    @Test
    @DisplayName("preferredWidth() calculates width for single child")
    void preferredWidth_singleChild() {
        ColumnsElement cols = columns(text("Hello"));
        // "Hello" = 5 characters, 1 column
        assertThat(cols.preferredWidth()).isEqualTo(5);
    }

    @Test
    @DisplayName("preferredWidth() uses maxWidth * count for multiple children")
    void preferredWidth_multipleChildren() {
        ColumnsElement cols = columns(
            text("A"),       // 1
            text("BB"),      // 2
            text("CCC")      // 3
        );
        // max width = 3, 3 cols => 3 * 3 = 9
        assertThat(cols.preferredWidth()).isEqualTo(9);
    }

    @Test
    @DisplayName("preferredWidth() includes spacing")
    void preferredWidth_withSpacing() {
        ColumnsElement cols = columns(
            text("AA"),      // 2
            text("BB"),      // 2
            text("CC")       // 2
        ).spacing(1);
        // max width = 2, 3 cols => 2 * 3 + 1 * 2 = 8
        assertThat(cols.preferredWidth()).isEqualTo(8);
    }

    @Test
    @DisplayName("preferredWidth() includes margin")
    void preferredWidth_withMargin() {
        ColumnsElement cols = columns(
            text("Hello")    // 5
        ).margin(new Margin(1, 2, 1, 3)); // top, right, bottom, left
        // 5 + 2 (right) + 3 (left) = 10
        assertThat(cols.preferredWidth()).isEqualTo(10);
    }

    @Test
    @DisplayName("preferredWidth() with explicit column count")
    void preferredWidth_withExplicitColumnCount() {
        ColumnsElement cols = columns(
            text("AAAA"),    // 4
            text("BB"),      // 2
            text("CCC"),     // 3
            text("D")        // 1
        ).columnCount(2);
        // max width = 4, 2 cols => 4 * 2 = 8
        assertThat(cols.preferredWidth()).isEqualTo(8);
    }

    @Test
    @DisplayName("Columns renders children in a single row when all fit")
    void rendersToBuffer() {
        // 3 items "A", "B", "C" with spacing(1) in 20-wide area
        // auto-detected cols = 3 (items are 1 char wide, all fit)
        // constraints: [fill(), len(1), fill(), len(1), fill()]
        // 20 - 2 spacing = 18, 18/3 = 6 per col
        // col0: x=0,w=6; spacing x=6; col1: x=7,w=6; spacing x=13; col2: x=14,w=6
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        columns(text("A"), text("B"), text("C"))
            .spacing(1)
            .render(frame, area, RenderContext.empty());

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(7, 0, "B")
            .hasSymbolAt(14, 0, "C");
    }

    @Test
    @DisplayName("Auto column count adapts to available width")
    void autoColumnCount() {
        // 6 items of width 5
        ColumnsElement cols = columns(
            text("AAAAA"),
            text("BBBBB"),
            text("CCCCC"),
            text("DDDDD"),
            text("EEEEE"),
            text("FFFFF")
        );

        // In 30-wide: (30+0)/(5+0) = 6 cols, 30/6 = 5 per col, all on row 0
        Rect wideArea = new Rect(0, 0, 30, 1);
        Buffer wideBuffer = Buffer.empty(wideArea);
        Frame wideFrame = Frame.forTesting(wideBuffer);
        cols.render(wideFrame, wideArea, RenderContext.empty());

        BufferAssertions.assertThat(wideBuffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(5, 0, "B")
            .hasSymbolAt(10, 0, "C")
            .hasSymbolAt(15, 0, "D")
            .hasSymbolAt(20, 0, "E")
            .hasSymbolAt(25, 0, "F");

        // In 12-wide: (12+0)/(5+0) = 2 cols, 12/2 = 6 per col, 3 rows
        Rect narrowArea = new Rect(0, 0, 12, 3);
        Buffer narrowBuffer = Buffer.empty(narrowArea);
        Frame narrowFrame = Frame.forTesting(narrowBuffer);
        cols.render(narrowFrame, narrowArea, RenderContext.empty());

        BufferAssertions.assertThat(narrowBuffer)
            .hasSymbolAt(0, 0, "A")   // row 0, col 0
            .hasSymbolAt(6, 0, "B")   // row 0, col 1
            .hasSymbolAt(0, 1, "C")   // row 1, col 0
            .hasSymbolAt(6, 1, "D")   // row 1, col 1
            .hasSymbolAt(0, 2, "E")   // row 2, col 0
            .hasSymbolAt(6, 2, "F");  // row 2, col 1
    }

    @Test
    @DisplayName("Row-first ordering fills left-to-right then top-to-bottom")
    void rowFirstOrdering() {
        // [A,B,C,D] in 2-col grid, 20 wide: 10 per col
        // row-first: row0=[A,B], row1=[C,D]
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        columns(text("A"), text("B"), text("C"), text("D"))
            .columnCount(2)
            .render(frame, area, RenderContext.empty());

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")    // row 0, col 0
            .hasSymbolAt(10, 0, "B")   // row 0, col 1
            .hasSymbolAt(0, 1, "C")    // row 1, col 0
            .hasSymbolAt(10, 1, "D");  // row 1, col 1
    }

    @Test
    @DisplayName("Column-first ordering fills top-to-bottom then left-to-right")
    void columnFirstOrdering() {
        // [A,B,C,D] in 2-col grid column-first, 20 wide: 10 per col
        // col0=[A,B], col1=[C,D] -> row0=[A,C], row1=[B,D]
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        columns(text("A"), text("B"), text("C"), text("D"))
            .columnCount(2)
            .columnFirst()
            .render(frame, area, RenderContext.empty());

        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")    // row 0, col 0 (child 0)
            .hasSymbolAt(10, 0, "C")   // row 0, col 1 (child 2)
            .hasSymbolAt(0, 1, "B")    // row 1, col 0 (child 1)
            .hasSymbolAt(10, 1, "D");  // row 1, col 1 (child 3)
    }

    @Test
    @DisplayName("Explicit column count forces N columns regardless of width")
    void explicitColumnCount() {
        // 4 items of width 10, forced to 2 columns in a 100-wide area
        Rect area = new Rect(0, 0, 100, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        columns(
            text("AAAAAAAAAA"),
            text("BBBBBBBBBB"),
            text("CCCCCCCCCC"),
            text("DDDDDDDDDD")
        ).columnCount(2)
         .render(frame, area, RenderContext.empty());

        // 2 cols in 100 wide = 50 per col
        // row0=[A,B], row1=[C,D]
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(50, 0, "B")
            .hasSymbolAt(0, 1, "C")
            .hasSymbolAt(50, 1, "D");

        assertThat(columns(
            text("AAAAAAAAAA"), text("BBBBBBBBBB"),
            text("CCCCCCCCCC"), text("DDDDDDDDDD")
        ).columnCount(2).preferredHeight(100, RenderContext.empty())).isEqualTo(2);
    }

    @Test
    @DisplayName("Spacing between columns creates gaps")
    void spacingBetweenColumns() {
        // 3 items "AA", "BB", "CC" with spacing(2) in 10-wide area
        // 3 cols: (10+2)/(2+2) = 3
        // constraints: [fill(), len(2), fill(), len(2), fill()]
        // 10 - 4 spacing = 6, 6/3 = 2 per col
        // col0: x=0,w=2; gap x=2,w=2; col1: x=4,w=2; gap x=6,w=2; col2: x=8,w=2
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        columns(text("AA"), text("BB"), text("CC"))
            .spacing(2)
            .render(frame, area, RenderContext.empty());

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "AA", Style.EMPTY);
        expected.setString(4, 0, "BB", Style.EMPTY);
        expected.setString(8, 0, "CC", Style.EMPTY);

        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("Empty area does not render")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 5));
        Frame frame = Frame.forTesting(buffer);

        // Should not throw
        columns(text("Test")).render(frame, emptyArea, RenderContext.empty());

        // Buffer should remain unchanged (all empty)
        BufferAssertions.assertThat(buffer).isEqualTo(Buffer.empty(new Rect(0, 0, 20, 5)));
    }

    @Test
    @DisplayName("Columns fluent API chains correctly")
    void fluentApiChaining() {
        ColumnsElement cols = columns(text("A"), text("B"))
            .spacing(1)
            .flex(Flex.SPACE_BETWEEN)
            .margin(2)
            .columnCount(2)
            .order(ColumnOrder.COLUMN_FIRST);

        assertThat(cols).isInstanceOf(ColumnsElement.class);
    }

    @Test
    @DisplayName("Columns created from collection")
    void columnsFromCollection() {
        ColumnsElement cols = columns(Arrays.asList(text("A"), text("B"), text("C")));
        assertThat(cols.preferredWidth()).isEqualTo(3);
    }

    @Test
    @DisplayName("preferredHeight provides enough rows to render all children")
    void preferredHeightSizesBufferForAllRows() {
        // 4 items of width 5, forced to 2 columns -> 2 rows
        ColumnsElement cols = columns(
            text("AAAAA"), text("BBBBB"),
            text("CCCCC"), text("DDDDD")
        ).columnCount(2);

        int width = 20;
        int height = cols.preferredHeight(width, RenderContext.empty());
        assertThat(height).isEqualTo(2);

        // Render into a buffer sized by preferredHeight (as println now does)
        Rect area = new Rect(0, 0, width, height);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        cols.render(frame, area, RenderContext.empty());

        // All 4 items must be present across both rows
        BufferAssertions.assertThat(buffer)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B")
            .hasSymbolAt(0, 1, "C")
            .hasSymbolAt(10, 1, "D");

        // A height-1 buffer (the old println behavior) would lose row 1
        Buffer truncated = Buffer.empty(new Rect(0, 0, width, 1));
        Frame truncatedFrame = Frame.forTesting(truncated);
        cols.render(truncatedFrame, truncated.area(), RenderContext.empty());

        BufferAssertions.assertThat(truncated)
            .hasSymbolAt(0, 0, "A")
            .hasSymbolAt(10, 0, "B");
        // C and D are lost — only row 0 fits
        assertThat(truncated.get(0, 0).symbol()).isEqualTo("A");
        assertThat(truncated.get(10, 0).symbol()).isEqualTo("B");
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
            // 3 items "AA", "BB", "CC" with spacing: 2 from CSS in 10-wide area
            // Same layout as spacingBetweenColumns test but driven by CSS
            DefaultRenderContext ctx = cssContext(".spaced { spacing: 2; }");

            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            columns(text("AA"), text("BB"), text("CC"))
                .addClass("spaced")
                .render(frame, area, ctx);

            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "AA", Style.EMPTY);
            expected.setString(4, 0, "BB", Style.EMPTY);
            expected.setString(8, 0, "CC", Style.EMPTY);

            BufferAssertions.assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("margin from CSS")
        void cssMargin() {
            // margin: 0 2 pushes content inward by 2 on each side
            // 20-wide area -> 16 effective, 2 items of width 1 -> 2 cols of 8
            DefaultRenderContext ctx = cssContext(".margined { margin: 0 2; }");

            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            columns(text("A"), text("B"))
                .addClass("margined")
                .render(frame, area, ctx);

            // margin left=2, so content starts at x=2
            // 16 effective width, 2 cols of 8
            BufferAssertions.assertThat(buffer)
                .hasSymbolAt(2, 0, "A")
                .hasSymbolAt(10, 0, "B");
        }

        @Test
        @DisplayName("column-count from CSS")
        void cssColumnCount() {
            // Force 2 columns via CSS, 4 items -> 2 rows
            DefaultRenderContext ctx = cssContext(".two-cols { column-count: 2; }");

            Rect area = new Rect(0, 0, 20, 2);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            columns(text("A"), text("B"), text("C"), text("D"))
                .addClass("two-cols")
                .render(frame, area, ctx);

            // 2 cols of 10 each, row0=[A,B], row1=[C,D]
            BufferAssertions.assertThat(buffer)
                .hasSymbolAt(0, 0, "A")
                .hasSymbolAt(10, 0, "B")
                .hasSymbolAt(0, 1, "C")
                .hasSymbolAt(10, 1, "D");
        }

        @Test
        @DisplayName("column-order from CSS")
        void cssColumnOrder() {
            // Column-first ordering via CSS
            DefaultRenderContext ctx = cssContext(
                ".col-first { column-count: 2; column-order: column-first; }");

            Rect area = new Rect(0, 0, 20, 2);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            columns(text("A"), text("B"), text("C"), text("D"))
                .addClass("col-first")
                .render(frame, area, ctx);

            // column-first: col0=[A,B], col1=[C,D] -> row0=[A,C], row1=[B,D]
            BufferAssertions.assertThat(buffer)
                .hasSymbolAt(0, 0, "A")
                .hasSymbolAt(10, 0, "C")
                .hasSymbolAt(0, 1, "B")
                .hasSymbolAt(10, 1, "D");
        }

        @Test
        @DisplayName("programmatic values override CSS")
        void programmaticOverridesCss() {
            // CSS says 2 columns row-first, programmatic says 4 columns column-first
            DefaultRenderContext ctx = cssContext(
                ".css-props { column-count: 2; column-order: row-first; spacing: 5; }");

            Rect area = new Rect(0, 0, 20, 2);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Programmatic column-count: 4, column-order: column-first, spacing: 0
            // override CSS column-count: 2, column-order: row-first, spacing: 5
            columns(text("A"), text("B"), text("C"), text("D"))
                .addClass("css-props")
                .columnCount(4)
                .order(ColumnOrder.COLUMN_FIRST)
                .spacing(0)
                .render(frame, area, ctx);

            // 4 cols of 5 each, all on 1 row (column-first with 1 row = same as row-first)
            BufferAssertions.assertThat(buffer)
                .hasSymbolAt(0, 0, "A")
                .hasSymbolAt(5, 0, "B")
                .hasSymbolAt(10, 0, "C")
                .hasSymbolAt(15, 0, "D");
        }
    }
}

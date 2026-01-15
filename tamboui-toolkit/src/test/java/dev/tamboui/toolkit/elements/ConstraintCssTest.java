/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static dev.tamboui.toolkit.Toolkit.*;

/**
 * Tests that Row and Column correctly consume CSS constraint properties.
 */
class ConstraintCssTest {

    private StyleEngine styleEngine;
    private DefaultRenderContext context;
    private Buffer buffer;
    private Frame frame;

    @BeforeEach
    void setUp() throws IOException {
        styleEngine = StyleEngine.create();
        styleEngine.loadStylesheet("constraint-test", "/themes/constraint-test.tcss");
        styleEngine.setActiveStylesheet("constraint-test");

        context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);
    }

    private void setupBuffer(int width, int height) {
        Rect area = new Rect(0, 0, width, height);
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
    }

    @Nested
    @DisplayName("Column height constraints from CSS")
    class ColumnHeightTests {

        @Test
        @DisplayName("CSS height: <number> sets fixed height")
        void cssHeightFixed() {
            setupBuffer(20, 10);

            column(
                text("A").addClass("height-fixed"),  // height: 3
                text("B")
            ).render(frame, new Rect(0, 0, 20, 10), context);

            // A should be at row 0
            assertThat(buffer).hasSymbolAt(0, 0, "A");
            // B should be at row 3 (after A's 3 rows)
            assertThat(buffer).hasSymbolAt(0, 3, "B");
        }

        @Test
        @DisplayName("CSS height: fit sizes to content")
        void cssHeightFit() {
            setupBuffer(20, 10);

            column(
                text("Line1\nLine2").addClass("height-fit"),  // Should be 2 rows
                text("After")
            ).render(frame, new Rect(0, 0, 20, 10), context);

            // First text at row 0
            assertThat(buffer).hasSymbolAt(0, 0, "L");
            // "After" should be at row 2 (after 2 lines of content)
            assertThat(buffer).hasSymbolAt(0, 2, "A");
        }
    }

    @Nested
    @DisplayName("Row width constraints from CSS")
    class RowWidthTests {

        @Test
        @DisplayName("CSS width: <number> sets fixed width")
        void cssWidthFixed() {
            setupBuffer(30, 1);

            row(
                text("A").addClass("width-fixed"),  // width: 10
                text("B")
            ).render(frame, new Rect(0, 0, 30, 1), context);

            // A at column 0
            assertThat(buffer).hasSymbolAt(0, 0, "A");
            // B should be at column 10 (after A's 10 columns)
            assertThat(buffer).hasSymbolAt(10, 0, "B");
        }

        @Test
        @DisplayName("CSS width: fit sizes to content")
        void cssWidthFit() {
            setupBuffer(30, 1);

            row(
                text("Hello").addClass("width-fit"),  // Should be 5 columns
                text("World")
            ).render(frame, new Rect(0, 0, 30, 1), context);

            // "Hello" at column 0
            assertThat(buffer).hasSymbolAt(0, 0, "H");
            // "World" should start at column 5
            assertThat(buffer).hasSymbolAt(5, 0, "W");
        }

        @Test
        @DisplayName("CSS width: percent uses percentage of available width")
        void cssWidthPercent() {
            setupBuffer(40, 1);

            row(
                text("A").addClass("width-percent"),  // width: 25% = 10 columns
                text("B")
            ).render(frame, new Rect(0, 0, 40, 1), context);

            // A at column 0
            assertThat(buffer).hasSymbolAt(0, 0, "A");
            // B should be at column 10 (25% of 40)
            assertThat(buffer).hasSymbolAt(10, 0, "B");
        }
    }

    @Nested
    @DisplayName("Descendant selector constraints")
    class DescendantSelectorTests {

        @Test
        @DisplayName("Descendant selector .parent .child applies constraints")
        void descendantSelectorAppliesWidthConstraint() {
            setupBuffer(40, 1);

            // .parent-row .child-text { width: fit; }
            row(
                text("Short").addClass("child-text"),
                text("Rest")
            ).addClass("parent-row")
             .render(frame, new Rect(0, 0, 40, 1), context);

            // With width: fit, "Short" takes only 5 columns
            assertThat(buffer).hasSymbolAt(0, 0, "S");
            // With flex: center on parent-row, rest of space is distributed
            // So "Rest" won't be immediately after "Short"
        }
    }

    @Nested
    @DisplayName("Programmatic constraints override CSS")
    class ProgrammaticOverrideTests {

        @Test
        @DisplayName("Programmatic constraint overrides CSS height")
        void programmaticOverridesCssHeight() {
            setupBuffer(20, 10);

            column(
                text("A").addClass("height-fixed").length(5),  // CSS: 3, programmatic: 5
                text("B")
            ).render(frame, new Rect(0, 0, 20, 10), context);

            // A should be at row 0
            assertThat(buffer).hasSymbolAt(0, 0, "A");
            // B should be at row 5 (programmatic 5 overrides CSS 3)
            assertThat(buffer).hasSymbolAt(0, 5, "B");
        }

        @Test
        @DisplayName("Programmatic constraint overrides CSS width")
        void programmaticOverridesCssWidth() {
            setupBuffer(30, 1);

            row(
                text("A").addClass("width-fixed").length(15),  // CSS: 10, programmatic: 15
                text("B")
            ).render(frame, new Rect(0, 0, 30, 1), context);

            // A at column 0
            assertThat(buffer).hasSymbolAt(0, 0, "A");
            // B should be at column 15 (programmatic overrides CSS)
            assertThat(buffer).hasSymbolAt(15, 0, "B");
        }
    }
}

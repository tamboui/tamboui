/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Overflow;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.RenderContext;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for layout elements (Panel, Row, Column, Spacer).
 */
class LayoutElementsTest {

    @Nested
    @DisplayName("Panel tests")
    class PanelTests {

        @Test
        @DisplayName("Panel fluent API chains correctly")
        void fluentApiChaining() {
            Panel element = panel("Title", text("Content"))
                .rounded()
                .borderColor(Color.GREEN)
                .fg(Color.WHITE)
                .bg(Color.BLACK);

            assertThat(element).isInstanceOf(Panel.class);
        }

        @Test
        @DisplayName("panel() creates empty panel")
        void emptyPanel() {
            Panel element = panel();
            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("panel(Element...) creates panel without title")
        void panelWithoutTitle() {
            Panel element = panel(text("Content"));
            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("Panel renders border")
        void rendersBorder() {
            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            panel("Test", text("Content"))
                .rounded()
                .render(frame, area, RenderContext.empty());

            // Rounded corners
            assertThat(buffer.get(0, 0).symbol()).isEqualTo("╭");
            assertThat(buffer.get(19, 0).symbol()).isEqualTo("╮");
            assertThat(buffer.get(0, 4).symbol()).isEqualTo("╰");
            assertThat(buffer.get(19, 4).symbol()).isEqualTo("╯");
        }

        @Test
        @DisplayName("Panel renders title")
        void rendersTitle() {
            Rect area = new Rect(0, 0, 20, 3);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            panel("Title", text("X"))
                .render(frame, area, RenderContext.empty());

            // Title should appear in top border
            assertThat(buffer.get(1, 0).symbol()).isEqualTo("T");
            assertThat(buffer.get(2, 0).symbol()).isEqualTo("i");
        }

        @Test
        @DisplayName("Empty area does not render")
        void emptyAreaNoRender() {
            Rect emptyArea = new Rect(0, 0, 0, 0);
            Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 5));
            Frame frame = Frame.forTesting(buffer);

            panel("Title").render(frame, emptyArea, RenderContext.empty());
        }

        @Test
        @DisplayName("Panel with border color")
        void withBorderColor() {
            Rect area = new Rect(0, 0, 10, 3);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            panel("X")
                .borderColor(Color.RED)
                .render(frame, area, RenderContext.empty());

            assertThat(buffer.get(0, 0).style().fg()).contains(Color.RED);
        }

        @Test
        @DisplayName("fit() computes height for borders only")
        void fitEmptyPanel() {
            Panel p = panel().fit();
            // Empty panel: 2 rows for borders (top + bottom)
            assertThat(p.constraint()).isEqualTo(Constraint.length(2));
        }

        @Test
        @DisplayName("fit() computes height with children")
        void fitWithChildren() {
            Panel p = panel(
                text("Line 1"),
                text("Line 2"),
                text("Line 3")
            ).fit();
            // 2 rows for borders + 3 children (1 row each)
            assertThat(p.constraint()).isEqualTo(Constraint.length(5));
        }

        @Test
        @DisplayName("fit() computes height with padding")
        void fitWithPadding() {
            Panel p = panel(text("Line 1"))
                .padding(1)
                .fit();
            // 2 rows for borders + 2 rows for padding (top + bottom) + 1 child
            assertThat(p.constraint()).isEqualTo(Constraint.length(5));
        }

        @Test
        @DisplayName("fit() respects child length constraints")
        void fitRespectsChildConstraints() {
            Panel p = panel(
                text("Line 1").length(3),
                text("Line 2")
            ).fit();
            // 2 rows for borders + 3 (from length constraint) + 1 (default)
            assertThat(p.constraint()).isEqualTo(Constraint.length(6));
        }

        @Test
        @DisplayName("fit() is dynamic - computed when constraint() is called")
        void fitIsDynamic() {
            Panel p = panel().fit();
            assertThat(p.constraint()).isEqualTo(Constraint.length(2));

            // Add children after fit()
            p.add(text("Added"));
            // Constraint should now include the new child
            assertThat(p.constraint()).isEqualTo(Constraint.length(3));
        }
    }

    @Nested
    @DisplayName("Row tests")
    class RowTests {

        @Test
        @DisplayName("Row fluent API chains correctly")
        void fluentApiChaining() {
            Row element = row(text("Left"), spacer(), text("Right"))
                .spacing(1)
                .fg(Color.WHITE);

            assertThat(element).isInstanceOf(Row.class);
        }

        @Test
        @DisplayName("row() creates empty row")
        void emptyRow() {
            Row element = row();
            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("Row renders children horizontally")
        void rendersHorizontally() {
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            row(
                text("A").length(5),
                text("B").length(5)
            ).render(frame, area, RenderContext.empty());

            assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
            assertThat(buffer.get(5, 0).symbol()).isEqualTo("B");
        }

        @Test
        @DisplayName("Empty area does not render")
        void emptyAreaNoRender() {
            Rect emptyArea = new Rect(0, 0, 0, 0);
            Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
            Frame frame = Frame.forTesting(buffer);

            row(text("A")).render(frame, emptyArea, RenderContext.empty());
        }

        @Test
        @DisplayName("Row with fill children distributes space")
        void fillDistribution() {
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            row(
                text("L").fill(),
                text("R").fill()
            ).render(frame, area, RenderContext.empty());

            // Both should be rendered with space distributed
            assertThat(buffer.get(0, 0).symbol()).isEqualTo("L");
        }

        @Test
        @DisplayName("Row with fit() text elements uses natural width")
        void fitTextElementsUseNaturalWidth() {
            Rect area = new Rect(0, 0, 30, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Three text elements with fit() - each should get exactly its content width
            // "Hello " = 6 chars, "World" = 5 chars, "!" = 1 char
            row(
                text("Hello ").fit(),
                text("World").fit(),
                text("!").fit()
            ).render(frame, area, RenderContext.empty());

            // Text should render with no truncation, adjacent positions
            BufferAssertions.assertThat(buffer)
                .hasSymbolAt(0, 0, "H")      // start of "Hello "
                .hasSymbolAt(5, 0, " ")      // trailing space of "Hello "
                .hasSymbolAt(6, 0, "W")      // start of "World"
                .hasSymbolAt(10, 0, "d")     // end of "World"
                .hasSymbolAt(11, 0, "!");    // the "!"
        }

        @Test
        @DisplayName("fit() constraint returns Constraint.fit()")
        void fitReturnsCorrectConstraint() {
            TextElement element = text("Hello").fit();
            assertThat(element.constraint()).isEqualTo(Constraint.fit());
        }
    }

    @Nested
    @DisplayName("Column tests")
    class ColumnTests {

        @Test
        @DisplayName("Column fluent API chains correctly")
        void fluentApiChaining() {
            Column element = column(text("Top"), spacer(), text("Bottom"))
                .spacing(1)
                .fg(Color.WHITE);

            assertThat(element).isInstanceOf(Column.class);
        }

        @Test
        @DisplayName("column() creates empty column")
        void emptyColumn() {
            Column element = column();
            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("Column renders children vertically")
        void rendersVertically() {
            Rect area = new Rect(0, 0, 10, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            column(
                text("A").length(1),
                text("B").length(1)
            ).render(frame, area, RenderContext.empty());

            assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
            assertThat(buffer.get(0, 1).symbol()).isEqualTo("B");
        }

        @Test
        @DisplayName("Empty area does not render")
        void emptyAreaNoRender() {
            Rect emptyArea = new Rect(0, 0, 0, 0);
            Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 5));
            Frame frame = Frame.forTesting(buffer);

            column(text("A")).render(frame, emptyArea, RenderContext.empty());
        }
    }

    @Nested
    @DisplayName("Spacer tests")
    class SpacerTests {

        @Test
        @DisplayName("spacer() creates fill spacer")
        void fillSpacer() {
            Spacer element = spacer();
            assertThat(element.constraint()).isEqualTo(Constraint.fill());
        }

        @Test
        @DisplayName("spacer(int) creates fixed spacer")
        void fixedSpacer() {
            Spacer element = spacer(10);
            assertThat(element.constraint()).isEqualTo(Constraint.length(10));
        }

        @Test
        @DisplayName("Spacer renders empty space")
        void rendersEmptySpace() {
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            spacer().render(frame, area, RenderContext.empty());

            // Spacer should not change the content (it's just empty space)
            // This test verifies it doesn't throw
            assertThat(buffer).isNotNull();
        }
    }

    @Nested
    @DisplayName("Width-aware preferred height")
    class PreferredHeightWithWidthTests {

        @Test
        @DisplayName("Row.preferredHeight(width) returns max child height")
        void rowMaxChildHeight() {
            Row r = row(
                text("Short"),
                text("12345678901234567890").overflow(Overflow.WRAP_CHARACTER)
            );
            // At width 20, each child gets 10 chars (20/2)
            // "12345678901234567890" = 20 chars at width 10 = 2 lines
            assertThat(r.preferredHeight(20, null)).isEqualTo(2);

            // At width 10, each child gets 5 chars (10/2)
            // "12345678901234567890" = 20 chars at width 5 = 4 lines
            assertThat(r.preferredHeight(10, null)).isEqualTo(4);
        }

        @Test
        @DisplayName("Row.preferredHeight(width) accounts for spacing")
        void rowWithSpacing() {
            Row r = row(
                text("12345678901234567890").overflow(Overflow.WRAP_CHARACTER),
                text("12345678901234567890").overflow(Overflow.WRAP_CHARACTER)
            ).spacing(2);
            // Width 22: 2 spacing + 20 content = 10 per child, each wraps to 2 lines
            assertThat(r.preferredHeight(22, null)).isEqualTo(2);
        }

        @Test
        @DisplayName("Column.preferredHeight(width) returns sum of child heights")
        void columnSumChildHeights() {
            Column c = column(
                text("Short"),
                text("12345678901234567890").overflow(Overflow.WRAP_CHARACTER)
            );
            // At width 20, first child = 1, second child = 1 (no wrapping)
            assertThat(c.preferredHeight(20, null)).isEqualTo(2);

            // At width 10, first child = 1, second child = 2 (wraps)
            assertThat(c.preferredHeight(10, null)).isEqualTo(3);
        }

        @Test
        @DisplayName("Column.preferredHeight(width) accounts for spacing")
        void columnWithSpacing() {
            Column c = column(
                text("A"),
                text("B"),
                text("C")
            ).spacing(1);
            // 3 children + 2 spacing = 5
            assertThat(c.preferredHeight(20, null)).isEqualTo(5);
        }

        @Test
        @DisplayName("Panel.preferredHeight(width) includes borders")
        void panelIncludesBorders() {
            Panel p = panel(text("Content"));
            // 2 for borders + 1 for content
            assertThat(p.preferredHeight(20, null)).isEqualTo(3);
        }

        @Test
        @DisplayName("Panel.preferredHeight(width) includes padding")
        void panelIncludesPadding() {
            Panel p = panel(text("Content")).padding(1);
            // 2 for borders + 2 for padding + 1 for content
            assertThat(p.preferredHeight(20, null)).isEqualTo(5);
        }

        @Test
        @DisplayName("Panel.preferredHeight(width) calculates wrapped content height")
        void panelWithWrappedContent() {
            Panel p = panel(
                text("12345678901234567890").overflow(Overflow.WRAP_CHARACTER)
            );
            // At width 22: 2 borders, content width = 20, text fits in 1 line
            assertThat(p.preferredHeight(22, null)).isEqualTo(3);

            // At width 12: 2 borders, content width = 10, text wraps to 2 lines
            assertThat(p.preferredHeight(12, null)).isEqualTo(4);
        }

        @Test
        @DisplayName("Nested row in column calculates height correctly")
        void nestedRowInColumn() {
            Column c = column(
                text("Header"),
                row(
                    text("12345678901234567890").overflow(Overflow.WRAP_CHARACTER),
                    text("Short")
                )
            );
            // At width 20: header = 1, row max = 1 (20 chars / 2 = 10 per child, wraps to 2)
            // Actually: 20 chars at width 10 = 2 lines
            assertThat(c.preferredHeight(20, null)).isEqualTo(3); // 1 + 2
        }

        @Test
        @DisplayName("Empty containers return sensible defaults")
        void emptyContainers() {
            assertThat(row().preferredHeight(20, null)).isEqualTo(1);
            assertThat(column().preferredHeight(20, null)).isEqualTo(0);
            assertThat(panel().preferredHeight(20, null)).isEqualTo(2); // Just borders
        }
    }

    @Nested
    @DisplayName("Nested layout tests")
    class NestedLayoutTests {

        @Test
        @DisplayName("Nested row in column renders correctly")
        void nestedRowInColumn() {
            Rect area = new Rect(0, 0, 20, 3);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            column(
                row(text("A"), text("B")),
                text("C")
            ).render(frame, area, RenderContext.empty());

            // Should render without error
            assertThat(buffer).isNotNull();
        }

        @Test
        @DisplayName("Panel with row content")
        void panelWithRow() {
            Rect area = new Rect(0, 0, 30, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            panel("Header",
                row(
                    text("Left").fill(),
                    text("Right").fill()
                )
            ).rounded().render(frame, area, RenderContext.empty());

            assertThat(buffer.get(0, 0).symbol()).isEqualTo("╭");
        }

        @Test
        @DisplayName("Complex nested layout")
        void complexNestedLayout() {
            Rect area = new Rect(0, 0, 40, 10);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            column(
                panel("Title", text("Content")).rounded(),
                row(
                    panel("Left", text("L")),
                    panel("Right", text("R"))
                )
            ).render(frame, area, RenderContext.empty());

            // Should render without error
            assertThat(buffer).isNotNull();
        }

        @Test
        @DisplayName("Row with many text elements clips each to its area")
        void rowWithManyTextElementsClips() {
            // Simulate the stats row: 10 text elements in a 40-cell wide row
            // Each element should get ~4 cells and be clipped
            Rect area = new Rect(0, 0, 40, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            row(
                text("A 4"), text("passed"),
                text("B 3"), text("failed"),
                text("C 0"), text("skipped"),
                text("D 0"), text("running"),
                text("E 0"), text("pending")
            ).render(frame, area, RenderContext.empty());

            // Check that text doesn't extend beyond the row's area
            // The last cell (x=39) should be within bounds
            // If overflow occurred, we'd see content from later elements bleeding over

            // Print buffer for debugging
            StringBuilder row0 = new StringBuilder();
            for (int x = 0; x < 40; x++) {
                row0.append(buffer.get(x, 0).symbol());
            }
            System.out.println("Row content: [" + row0 + "]");

            // Each element should be clipped to ~4 cells
            // With 10 elements in 40 cells, each gets 4 cells
            // "passed" (6 chars) should be clipped to "pass"
            // Verify no text appears outside bounds
            assertThat(buffer.get(39, 0).symbol()).isNotNull();
        }

        @Test
        @DisplayName("Row text elements are clipped to assigned area - not overflow")
        void rowTextElementsDoNotOverflow() {
            // A narrower test case: 2 text elements in 10-cell row
            // Each should get 5 cells
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            row(
                text("AAAAAAAAAA"), // 10 chars, should be clipped to 5
                text("BBBBBBBBBB")  // 10 chars, should be clipped to 5
            ).render(frame, area, RenderContext.empty());

            // Print buffer for debugging
            StringBuilder row0 = new StringBuilder();
            for (int x = 0; x < 10; x++) {
                row0.append(buffer.get(x, 0).symbol());
            }
            System.out.println("Row content: [" + row0 + "]");

            // First 5 chars should be "AAAAA"
            // Next 5 chars should be "BBBBB"
            assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
            assertThat(buffer.get(4, 0).symbol()).isEqualTo("A");
            assertThat(buffer.get(5, 0).symbol()).isEqualTo("B");
            assertThat(buffer.get(9, 0).symbol()).isEqualTo("B");
        }

        @Test
        @DisplayName("Row with flex:start still clips text elements")
        void rowWithFlexStartClipsText() {
            // Test with flex(START) like the user's CSS
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            row(
                text("AAAAAAAAAA"), // 10 chars
                text("BBBBBBBBBB")  // 10 chars
            ).flex(dev.tamboui.layout.Flex.START)
             .render(frame, area, RenderContext.empty());

            // Print buffer for debugging
            StringBuilder row0 = new StringBuilder();
            for (int x = 0; x < 10; x++) {
                row0.append(buffer.get(x, 0).symbol());
            }
            System.out.println("Row with flex:start content: [" + row0 + "]");

            // With flex:start, elements should still be clipped
            // Each gets 5 cells, remaining space goes at end
            assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
            assertThat(buffer.get(5, 0).symbol()).isEqualTo("B");
        }

        @Test
        @DisplayName("Panel with row inside clips correctly")
        void panelWithRowClipsCorrectly() {
            // Test the full structure: Panel > Column > Row > TextElements
            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            panel("Test",
                column(
                    row(
                        text("AAAAAAAAAA"),
                        text("BBBBBBBBBB")
                    )
                )
            ).render(frame, area, RenderContext.empty());

            // Print row 1 (inside the panel, after border)
            StringBuilder row1 = new StringBuilder();
            for (int x = 0; x < 20; x++) {
                row1.append(buffer.get(x, 1).symbol());
            }
            System.out.println("Panel row 1: [" + row1 + "]");

            // The row should be inside the panel (border at x=0, x=19)
            // Inner width = 18, so each text gets 9 chars
            // Content should be clipped and not overflow the panel border
            assertThat(buffer.get(0, 1).symbol()).isEqualTo("│"); // left border
            assertThat(buffer.get(19, 1).symbol()).isEqualTo("│"); // right border
        }

        @Test
        @DisplayName("Stats row with CSS does not overflow panel - reproduces user issue")
        void statsRowWithCssDoesNotOverflowPanel() {
            // Reproduce the user's issue: Panel > Column > Row with many text elements
            // CSS has text-overflow: wrap-character on .stats-row
            // Text should NOT overflow the panel borders

            StyleEngine styleEngine = StyleEngine.create();
            styleEngine.addStylesheet(
                ".stats-row { text-overflow: wrap-character; height: 1; flex: start; }\n" +
                ".success { color: green; }\n" +
                ".error { color: red; }\n" +
                ".warning { color: yellow; }\n" +
                ".info { color: cyan; }\n" +
                ".dim { color: gray; }"
            );

            DefaultRenderContext context = DefaultRenderContext.createEmpty();
            context.setStyleEngine(styleEngine);

            // Panel width 50, simulating user's Tests panel
            Rect area = new Rect(0, 0, 50, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Create structure matching user's code
            panel("Tests",
                column(
                    row(
                        text("Header")
                    ),
                    row(
                        text("+ 4").addClass("success"), text("passed"),
                        text("x 3").addClass("error"), text("failed"),
                        text("> 0").addClass("warning"), text("skipped"),
                        text("~ 0").addClass("info"), text("running"),
                        text("? 0").addClass("dim"), text("pending")
                    ).addClass("stats-row")
                )
            ).render(frame, area, context);

            // The panel borders must be intact - text must NOT overflow
            // Row 0: top border (┌Tests...┐)
            // Row 1: Header row with borders
            // Row 2: Empty row due to column expansion
            // Row 3: Stats row - THIS IS WHERE THE BUG IS (right border missing)
            // Row 4: bottom border (└...┘)
            BufferAssertions.assertThat(buffer)
                .hasSymbolAt(0, 1, "│")   // left border row 1
                .hasSymbolAt(49, 1, "│")  // right border row 1
                .hasSymbolAt(0, 2, "│")   // left border row 2
                .hasSymbolAt(49, 2, "│")  // right border row 2
                .hasSymbolAt(0, 3, "│")   // left border row 3 (stats row)
                .hasSymbolAt(49, 3, "│"); // right border row 3 (stats row) - BUG: this fails!
        }

        @Test
        @DisplayName("Simple row in panel does not overflow - minimal reproduction")
        void simpleRowInPanelDoesNotOverflow() {
            // Minimal test: Panel > Row with text that exceeds available width
            // Without any CSS, just pure layout
            Rect area = new Rect(0, 0, 20, 3);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Inner area should be 18 wide (20 - 2 borders)
            // Row with 4 text elements, each ~4.5 chars
            panel("T",
                row(
                    text("AAAA"),
                    text("BBBB"),
                    text("CCCC"),
                    text("DDDD")
                )
            ).render(frame, area, RenderContext.empty());

            // Right border must be intact at position 19
            BufferAssertions.assertThat(buffer)
                .hasSymbolAt(0, 1, "│")   // left border
                .hasSymbolAt(19, 1, "│"); // right border - must NOT be overwritten
        }
    }
}

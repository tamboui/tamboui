/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.sparkline;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.style.TestStylePropertyResolver;
import dev.tamboui.widgets.block.Block;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class DualSparklineTest {

    @Test
    @DisplayName("Centre row renders separator")
    void centreRowRendersSeparator() {
        // 3-row area: top series, centre separator, bottom series
        DualSparkline chart = DualSparkline.builder()
                .topData(8)
                .bottomData(8)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasSymbolAt(0, 1, "─");
    }

    @Test
    @DisplayName("Top series renders above centre, bottom empty when zero")
    void topSeriesRendersAboveCentre() {
        DualSparkline chart = DualSparkline.builder()
                .topData(8)
                .bottomData(0)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // row 0 = top full bar, row 1 = centre separator, row 2 = bottom empty
        assertThat(buffer).hasContent("█", "─", " ");
    }

    @Test
    @DisplayName("Bottom series renders partial cells as complement glyph in inverse video")
    void bottomSeriesUsesInverseVideoPartials() {
        // 4/8 = 0.5 → top uses ▄ (lower half); bottom uses the complement glyph ▄
        // with the REVERSED modifier, so the top half of the cell shows the bar colour
        DualSparkline chart = DualSparkline.builder()
                .topData(4)
                .bottomData(4)
                .max(8)
                .bottomStyle(Style.EMPTY.fg(Color.BLUE))
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasContent("▄", "─", "▄");
        assertThat(buffer).hasStyleAt(0, 2, Style.EMPTY.fg(Color.BLUE).reversed());
    }

    @Test
    @DisplayName("Bottom series keeps eighth-level resolution via inverse video")
    void bottomSeriesKeepsEighthResolution() {
        // Bottom values 1..7 of max 8 render as the complement glyphs ▇▆▅▄▃▂▁ in
        // inverse video — no collapse to the coarse ▔/▀/█ upper-block ladder
        DualSparkline chart = DualSparkline.builder()
                .topData(0, 0, 0, 0, 0, 0, 0)
                .bottomData(1, 2, 3, 4, 5, 6, 7)
                .max(8)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 7, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasContent("       ", "───────", "▇▆▅▄▃▂▁");
        assertThat(buffer).hasStyleAt(0, 2, Style.EMPTY.reversed());
        assertThat(buffer).hasStyleAt(6, 2, Style.EMPTY.reversed());
    }

    @Test
    @DisplayName("Bottom series renders below centre, top empty when zero")
    void bottomSeriesRendersBelowCentre() {
        DualSparkline chart = DualSparkline.builder()
                .topData(0)
                .bottomData(8)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // row 0 = top empty, row 1 = centre separator, row 2 = bottom full bar
        assertThat(buffer).hasContent(" ", "─", "█");
    }

    @Test
    @DisplayName("Top and bottom styles applied to correct rows")
    void stylesAppliedToCorrectRows() {
        DualSparkline chart = DualSparkline.builder()
                .topData(8)
                .bottomData(8)
                .topStyle(Style.EMPTY.fg(Color.GREEN))
                .bottomStyle(Style.EMPTY.fg(Color.BLUE))
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasForegroundAt(0, 0, Color.GREEN);
        assertThat(buffer).hasForegroundAt(0, 2, Color.BLUE);
    }

    @Test
    @DisplayName("Y-axis labels rendered when showYAxis is true")
    void yAxisLabelsRendered() {
        DualSparkline chart = DualSparkline.builder()
                .topData(8)
                .bottomData(8)
                .max(8)
                .showYAxis(true)
                .build();
        // 6 wide: 4 label cols + 2 data cols; 3 rows
        Rect area = new Rect(0, 0, 6, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Row 0: max label "   8" then bar; row 1: "   0" then separator; row 2: "   8" then bar
        assertThat(buffer).hasContent(
                "   8█ ",
                "   0─ ",
                "   8█ "
        );
    }

    @Test
    @DisplayName("Y-axis labels absent when showYAxis is false")
    void yAxisLabelsAbsentWhenDisabled() {
        DualSparkline chart = DualSparkline.builder()
                .topData(8)
                .bottomData(8)
                .max(8)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // x=0 is the bar column — should have a bar symbol, not a label character
        assertThat(buffer).hasContent("█", "─", "█");
    }

    // -------------------------------------------------------------------------
    // Basic rendering — no axes (parallel to SparklineTest)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Renders basic top and bottom data with sub-pixel symbols")
    void rendersBasicTopAndBottomData() {
        // 0 → " ", 4/8 = 0.5 → "▄", 8/8 = 1.0 → "█"
        DualSparkline chart = DualSparkline.builder()
                .topData(0, 4, 8)
                .bottomData(0, 4, 8)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 3, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasContent(" ▄█", "───", " ▄█");
    }

    @Test
    @DisplayName("Renders with explicit max value producing partial bars")
    void rendersWithMaxValue() {
        // 50/100 = 0.5 → "▄", 100/100 = 1.0 → "█"
        DualSparkline chart = DualSparkline.builder()
                .topData(50, 100)
                .bottomData(50, 100)
                .max(100)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 2, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasContent("▄█", "──", "▄█");
    }

    @Test
    @DisplayName("Auto-scales to max across both datasets")
    void autoScalesToDataMax() {
        // max = 100; 25/100 → "▂", 50/100 → "▄", 100/100 → "█"
        DualSparkline chart = DualSparkline.builder()
                .topData(25, 50, 100)
                .bottomData(25, 50, 100)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 3, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasContent("▂▄█", "───", "▆▄█");
    }

    @Test
    @DisplayName("Truncates data to fit area width showing most recent ticks")
    void truncatesDataToFitArea() {
        // 8 data points, 3-wide area → shows last 3: [6, 7, 8] of max 8
        // 6/8 → "▆", 7/8 → "▇", 8/8 → "█"
        DualSparkline chart = DualSparkline.builder()
                .topData(1, 2, 3, 4, 5, 6, 7, 8)
                .bottomData(1, 2, 3, 4, 5, 6, 7, 8)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 3, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasContent("▆▇█", "───", "▂▁█");
    }

    @Test
    @DisplayName("THREE_LEVELS bar set uses coarser symbols")
    void withThreeLevelsBarSet() {
        // With THREE_LEVELS, 3/8 → "▄" and 6/8=0.75 → "█" (not "▃"/"▆" as in
        // NINE_LEVELS). The bottom series quantizes identically to the top:
        // 6/8 collapses to a full cell (no inverse video) and 3/8 renders as
        // the complement of the effective half level ("▄" in inverse video).
        DualSparkline chart = DualSparkline.builder()
                .topData(0, 3, 6, 8)
                .bottomData(0, 3, 6, 8)
                .barSet(Sparkline.BarSet.THREE_LEVELS)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 4, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasContent(" ▄██", "────", " ▄██");
        assertThat(buffer).hasStyleAt(1, 2, Style.EMPTY.reversed());
        assertThat(buffer).hasStyleAt(2, 2, Style.EMPTY);
    }

    @Test
    @DisplayName("Handles empty top and bottom data without throwing")
    void handlesAllEmptyData() {
        DualSparkline chart = DualSparkline.builder()
                .topData(new long[0])
                .bottomData(new long[0])
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 3, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // No ticks → nothing written; buffer stays empty
        assertThat(buffer).hasContent("   ", "   ", "   ");
    }

    @Test
    @DisplayName("Handles all-zero data without division by zero")
    void handlesAllZeroData() {
        DualSparkline chart = DualSparkline.builder()
                .topData(0, 0, 0)
                .bottomData(0, 0, 0)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 3, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // effectiveMax = 1 (floor), all barPx = 0 → empty bars; centre separator written
        assertThat(buffer).hasContent("   ", "───", "   ");
    }

    @Test
    @DisplayName("Renders fewer data points than area width")
    void fewerDataPointsThanWidth() {
        // 2 data points in a 5-wide area: bars at col 0-1, empty at col 2-4
        DualSparkline chart = DualSparkline.builder()
                .topData(4, 8)
                .bottomData(4, 8)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 5, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // 4/8 → "▄", 8/8 → "█"; columns 2-4 remain empty
        assertThat(buffer).hasContent("▄█   ", "──   ", "▄█   ");
    }

    // -------------------------------------------------------------------------
    // X-axis labels
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("X-axis labels rendered below chart body")
    void xAxisLabelsRendered() {
        // 8 data points, 8-wide area → labels don't overlap
        DualSparkline chart = DualSparkline.builder()
                .topData(0, 0, 0, 0, 0, 0, 0, 0)
                .bottomData(0, 0, 0, 0, 0, 0, 0, 0)
                .showYAxis(false)
                .xLabels("-7s", "now")
                .build();
        // height 4: 3 chart rows + 1 x-axis row
        Rect area = new Rect(0, 0, 8, 4);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // x-axis at y=3: "-7s" at col 0, "now" right-aligned ending at col 7
        assertThat(buffer).hasSymbolAt(0, 3, "-");
        assertThat(buffer).hasSymbolAt(1, 3, "7");
        assertThat(buffer).hasSymbolAt(2, 3, "s");
        assertThat(buffer).hasSymbolAt(5, 3, "n");
        assertThat(buffer).hasSymbolAt(7, 3, "w");
    }

    @Test
    @DisplayName("Block border wraps the chart")
    void blockBorderWrapsChart() {
        DualSparkline chart = DualSparkline.builder()
                .topData(8)
                .bottomData(8)
                .showYAxis(false)
                .block(Block.bordered())
                .build();
        Rect area = new Rect(0, 0, 5, 5);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer)
                .hasSymbolAt(0, 0, "┌")
                .hasSymbolAt(4, 0, "┐")
                .hasSymbolAt(0, 4, "└")
                .hasSymbolAt(4, 4, "┘");
    }

    @Test
    @DisplayName("Empty area does not throw")
    void emptyAreaDoesNotThrow() {
        DualSparkline chart = DualSparkline.builder()
                .topData(1, 2, 3)
                .bottomData(1, 2, 3)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 5));

        assertThatCode(() -> chart.render(area, buffer)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Explicit max scales both series to full bars")
    void explicitMaxScalesBothSeries() {
        DualSparkline chart = DualSparkline.builder()
                .topData(100)
                .bottomData(100)
                .max(100)
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasContent("█", "─", "█");
    }

    @Test
    @DisplayName("autoMax clears explicit max and rescales from data")
    void autoMaxClearsExplicitMax() {
        DualSparkline chart = DualSparkline.builder()
                .topData(50)
                .bottomData(100)
                .max(200)
                .autoMax() // revert to data max = 100
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // bottom = 100 = max → full bar
        assertThat(buffer).hasSymbolAt(0, 2, "█");
    }

    @Test
    @DisplayName("topData from int array accepted")
    void topDataFromIntArray() {
        DualSparkline chart = DualSparkline.builder()
                .topData(new int[]{8})
                .bottomData(new long[0])
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasSymbolAt(0, 0, "█");
    }

    @Test
    @DisplayName("bottomData from int array accepted")
    void bottomDataFromIntArray() {
        DualSparkline chart = DualSparkline.builder()
                .topData(new long[0])
                .bottomData(new int[]{8})
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasSymbolAt(0, 2, "█");
    }

    @Test
    @DisplayName("topData from List accepted")
    void topDataFromList() {
        DualSparkline chart = DualSparkline.builder()
                .topData(Arrays.asList(8L))
                .bottomData(new long[0])
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasSymbolAt(0, 0, "█");
    }

    @Test
    @DisplayName("bottomData from List accepted")
    void bottomDataFromList() {
        DualSparkline chart = DualSparkline.builder()
                .topData(new long[0])
                .bottomData(Arrays.asList(8L))
                .showYAxis(false)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasSymbolAt(0, 2, "█");
    }

    @Test
    @DisplayName("from(long[], long[]) factory builds with defaults")
    void fromArrayFactory() {
        // Default has showYAxis=false, so bars start at col 0
        DualSparkline chart = DualSparkline.from(new long[]{8}, new long[]{8});
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasContent("█", "─", "█");
    }

    @Test
    @DisplayName("from(List, List) factory builds with defaults")
    void fromListFactory() {
        // Default has showYAxis=false, so bars start at col 0
        DualSparkline chart = DualSparkline.from(Arrays.asList(8L), Arrays.asList(8L));
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasContent("█", "─", "█");
    }

    @Test
    @DisplayName("RIGHT_TO_LEFT renders newest data at left column")
    void rightToLeftNewestDataAtLeft() {
        // data: [0, 8] — newest value is 8 (last element)
        // LEFT_TO_RIGHT:  col 0 = 0 (empty), col 1 = 8 (full)
        // RIGHT_TO_LEFT:  col 0 = 8 (full),  col 1 = 0 (empty)
        DualSparkline chart = DualSparkline.builder()
                .topData(0, 8)
                .bottomData(0, 8)
                .showYAxis(false)
                .direction(Sparkline.RenderDirection.RIGHT_TO_LEFT)
                .build();
        Rect area = new Rect(0, 0, 2, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Newest (8) at left, oldest (0) at right
        assertThat(buffer).hasSymbolAt(0, 0, "█"); // top full at left
        assertThat(buffer).hasSymbolAt(1, 0, " "); // top empty at right
        assertThat(buffer).hasSymbolAt(0, 2, "█"); // bottom full at left
        assertThat(buffer).hasSymbolAt(1, 2, " "); // bottom empty at right
    }

    // -------------------------------------------------------------------------
    // CSS property resolution
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("CSS top-color applied to top series bars")
    void cssTopColorApplied() {
        TestStylePropertyResolver resolver = TestStylePropertyResolver.of("top-color", Color.GREEN);
        DualSparkline chart = DualSparkline.builder()
                .topData(8)
                .bottomData(0)
                .showYAxis(false)
                .styleResolver(resolver)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasForegroundAt(0, 0, Color.GREEN);
    }

    @Test
    @DisplayName("CSS bottom-color applied to bottom series bars")
    void cssBottomColorApplied() {
        TestStylePropertyResolver resolver = TestStylePropertyResolver.of("bottom-color", Color.BLUE);
        DualSparkline chart = DualSparkline.builder()
                .topData(0)
                .bottomData(8)
                .showYAxis(false)
                .styleResolver(resolver)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasForegroundAt(0, 2, Color.BLUE);
    }

    @Test
    @DisplayName("Programmatic topForeground overrides CSS top-color")
    void programmaticTopForegroundOverridesCss() {
        TestStylePropertyResolver resolver = TestStylePropertyResolver.of("top-color", Color.GREEN);
        DualSparkline chart = DualSparkline.builder()
                .topData(8)
                .bottomData(0)
                .showYAxis(false)
                .styleResolver(resolver)
                .topForeground(Color.RED)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasForegroundAt(0, 0, Color.RED);
    }

    @Test
    @DisplayName("Programmatic bottomForeground overrides CSS bottom-color")
    void programmaticBottomForegroundOverridesCss() {
        TestStylePropertyResolver resolver = TestStylePropertyResolver.of("bottom-color", Color.BLUE);
        DualSparkline chart = DualSparkline.builder()
                .topData(0)
                .bottomData(8)
                .showYAxis(false)
                .styleResolver(resolver)
                .bottomForeground(Color.MAGENTA)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasForegroundAt(0, 2, Color.MAGENTA);
    }

    @Test
    @DisplayName("topStyle fg takes lowest precedence below topForeground and CSS")
    void topStyleFgLowerPrecedenceThanCss() {
        TestStylePropertyResolver resolver = TestStylePropertyResolver.of("top-color", Color.GREEN);
        DualSparkline chart = DualSparkline.builder()
                .topData(8)
                .bottomData(0)
                .topStyle(Style.EMPTY.fg(Color.YELLOW))
                .showYAxis(false)
                .styleResolver(resolver)
                .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // CSS top-color (GREEN) wins over topStyle fg (YELLOW)
        assertThat(buffer).hasForegroundAt(0, 0, Color.GREEN);
    }

    @Test
    @DisplayName("RIGHT_TO_LEFT x-axis labels are mirrored")
    void rightToLeftXAxisLabels() {
        // 8 ticks; "-7s" should land on the right, "now" on the left
        DualSparkline chart = DualSparkline.builder()
                .topData(0, 0, 0, 0, 0, 0, 0, 0)
                .bottomData(0, 0, 0, 0, 0, 0, 0, 0)
                .showYAxis(false)
                .xLabels("-7s", "now")
                .direction(Sparkline.RenderDirection.RIGHT_TO_LEFT)
                .build();
        Rect area = new Rect(0, 0, 8, 4);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // "now" at col 0 (left), "-7s" right-aligned ending at col 7 (right)
        assertThat(buffer).hasSymbolAt(0, 3, "n");
        assertThat(buffer).hasSymbolAt(2, 3, "w");
        assertThat(buffer).hasSymbolAt(5, 3, "-");
        assertThat(buffer).hasSymbolAt(7, 3, "s");
    }

    // -------------------------------------------------------------------------
    // Compact height (no centre separator)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Two-row height drops centre separator")
    void twoRowHeightDropsCentreSeparator() {
        DualSparkline chart = DualSparkline.builder()
            .topData(8)
            .bottomData(8)
            .max(8)
            .build();
        Rect area = new Rect(0, 0, 1, 2);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Row 0: top series full bar
        assertThat(buffer).hasSymbolAt(0, 0, "\u2588"); // █
        // Row 1: bottom series full bar (reversed)
        assertThat(buffer).hasSymbolAt(0, 1, "\u2588"); // █
    }

    @Test
    @DisplayName("Single-row height shows top series only")
    void singleRowShowsTopOnly() {
        DualSparkline chart = DualSparkline.builder()
            .topData(8)
            .bottomData(8)
            .max(8)
            .build();
        Rect area = new Rect(0, 0, 1, 1);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasSymbolAt(0, 0, "\u2588"); // █ (top)
    }

    @Test
    @DisplayName("Even height keeps both halves equal")
    void evenHeightKeepsEqualHalves() {
        DualSparkline chart = DualSparkline.builder()
            .topData(8)
            .bottomData(8)
            .max(8)
            .build();
        // 4 rows: top=1, centre=1, bottom=1, spare=1
        Rect area = new Rect(0, 0, 1, 4);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Row 0: top full
        assertThat(buffer).hasSymbolAt(0, 0, "\u2588"); // █
        // Row 1: centre separator
        assertThat(buffer).hasSymbolAt(0, 1, "\u2500"); // ─
        // Row 2: bottom full
        assertThat(buffer).hasSymbolAt(0, 2, "\u2588"); // █
        // Row 3: spare (empty)
        assertThat(buffer).hasSymbolAt(0, 3, " ");
    }

    @Test
    @DisplayName("Renders with top and bottom color mappers")
    void rendersWithTopAndBottomColorMappers() {
        DualSparkline chart = DualSparkline.builder()
            .topData(10)
            .bottomData(20)
            .max(100)
            .topColorMapper(val -> Color.GREEN)
            .bottomColorMapper(val -> Color.BLUE)
            .showYAxis(false)
            .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasForegroundAt(0, 0, Color.GREEN);
        assertThat(buffer).hasForegroundAt(0, 2, Color.BLUE);
    }

    @Test
    @DisplayName("Renders with top and bottom programmatic gradients")
    void rendersWithTopAndBottomGradients() {
        DualSparkline chart = DualSparkline.builder()
            .topData(0, 100)
            .bottomData(0, 100)
            .max(100)
            .topGradient(Color.GREEN, Color.RED)
            .bottomGradient(Color.CYAN, Color.MAGENTA)
            .showYAxis(false)
            .build();
        Rect area = new Rect(0, 0, 2, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // Column 0 has 0 value (GREEN for top, CYAN for bottom)
        // Column 1 has 100 value (RED for top, MAGENTA for bottom)
        assertThat(buffer).hasForegroundAt(1, 0, Color.RED);
        assertThat(buffer).hasForegroundAt(1, 2, Color.MAGENTA);
    }

    @Test
    @DisplayName("Renders with CSS top and bottom gradients")
    void rendersWithCssTopAndBottomGradients() {
        TestStylePropertyResolver resolver = TestStylePropertyResolver.builder()
            .set("top-gradient-low", Color.GREEN)
            .set("top-gradient-high", Color.RED)
            .set("bottom-gradient-low", Color.CYAN)
            .set("bottom-gradient-high", Color.MAGENTA)
            .build();
        DualSparkline chart = DualSparkline.builder()
            .topData(100)
            .bottomData(100)
            .max(100)
            .styleResolver(resolver)
            .showYAxis(false)
            .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasForegroundAt(0, 0, Color.RED);
        assertThat(buffer).hasForegroundAt(0, 2, Color.MAGENTA);
    }

    @Test
    @DisplayName("CSS top and bottom gradients fall back to shared gradient")
    void cssGradientsFallBackToSharedGradient() {
        TestStylePropertyResolver resolver = TestStylePropertyResolver.builder()
            .set("gradient-low", Color.GREEN)
            .set("gradient-high", Color.RED)
            .build();
        DualSparkline chart = DualSparkline.builder()
            .topData(100)
            .bottomData(100)
            .max(100)
            .styleResolver(resolver)
            .showYAxis(false)
            .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        assertThat(buffer).hasForegroundAt(0, 0, Color.RED);
        assertThat(buffer).hasForegroundAt(0, 2, Color.RED);
    }

    @Test
    @DisplayName("Bottom inverse-video partial bar retains gradient color")
    void bottomInverseVideoRetainsGradientColor() {
        DualSparkline chart = DualSparkline.builder()
            .topData(0)
            .bottomData(4) // half cell -> partial block rendered reversed
            .max(8)
            .bottomGradient(Color.GREEN, Color.RED)
            .showYAxis(false)
            .build();
        Rect area = new Rect(0, 0, 1, 3);
        Buffer buffer = Buffer.empty(area);

        chart.render(area, buffer);

        // 4/8 = 0.5 ratio
        Style bottomCellStyle = buffer.get(0, 2).style();
        org.assertj.core.api.Assertions.assertThat(bottomCellStyle.addModifiers()).contains(dev.tamboui.style.Modifier.REVERSED);
        assertThat(buffer).hasForegroundAt(0, 2, dev.tamboui.style.ColorGradient.of(Color.GREEN, Color.RED).at(0.5));
    }
}

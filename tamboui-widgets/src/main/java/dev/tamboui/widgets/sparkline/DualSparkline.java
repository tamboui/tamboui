/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.sparkline;

import java.util.List;
import java.util.function.LongFunction;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.text.CharWidth;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;

/**
 * A dual sparkline widget that displays two time-series datasets as vertical bars growing in opposite directions
 * from a shared centre axis.
 * <p>
 * The top series renders as bars growing <em>upward</em> from the centre; the bottom series renders as bars growing
 * <em>downward</em> from the centre. Sub-pixel resolution is achieved using Unicode block characters (▁▂▃▄▅▆▇█), giving
 * smooth visual gradation within a single character row. This layout matches the style of macOS Activity Monitor's
 * network and disk activity graphs.
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * DualSparkline chart = DualSparkline.builder()
 *         .topData(inRates)
 *         .bottomData(outRates)
 *         .topStyle(Style.EMPTY.fg(Color.GREEN))
 *         .bottomStyle(Style.EMPTY.fg(Color.BLUE))
 *         .xLabels("-60s", "-45s", "-30s", "-15s", "now")
 *         .block(Block.builder().borderType(BorderType.ROUNDED)
 *                 .title(Title.from("In / Out  msg/s")).build())
 *         .build();
 * }</pre>
 *
 * <h2>Differences from {@link Sparkline}</h2>
 * <table>
 * <caption>Feature comparison between Sparkline and DualSparkline</caption>
 * <tr>
 * <th></th>
 * <th>{@code Sparkline}</th>
 * <th>{@code DualSparkline}</th>
 * </tr>
 * <tr>
 * <td>Series</td>
 * <td>1</td>
 * <td>2 (top + bottom)</td>
 * </tr>
 * <tr>
 * <td>Growth direction</td>
 * <td>always upward from bottom row</td>
 * <td>top grows up, bottom grows down, from a shared centre separator row</td>
 * </tr>
 * <tr>
 * <td>Height</td>
 * <td>1 row (fixed at bottom of area)</td>
 * <td>fills the full area height</td>
 * </tr>
 * <tr>
 * <td>Y-axis labels</td>
 * <td>optional: max at the bar row</td>
 * <td>optional: max / 0 / max at top, centre, and bottom rows</td>
 * </tr>
 * <tr>
 * <td>X-axis labels</td>
 * <td>optional label row below the bar row</td>
 * <td>optional label row rendered below the chart body</td>
 * </tr>
 * <tr>
 * <td>BarSet</td>
 * <td>yes ({@link Sparkline.BarSet})</td>
 * <td>yes (reuses {@link Sparkline.BarSet})</td>
 * </tr>
 * </table>
 */
public final class DualSparkline implements Widget {

    /**
     * CSS property for the top series bar colour ({@code top-color}).
     * <p>
     * Resolved by {@link Builder#styleResolver(StylePropertyResolver)}; a programmatic
     * value set via {@link Builder#topForeground(Color)} takes precedence.
     */
    public static final PropertyDefinition<Color> TOP_COLOR =
            PropertyDefinition.of("top-color", ColorConverter.INSTANCE);

    /**
     * CSS property for the bottom series bar colour ({@code bottom-color}).
     * <p>
     * Resolved by {@link Builder#styleResolver(StylePropertyResolver)}; a programmatic
     * value set via {@link Builder#bottomForeground(Color)} takes precedence.
     */
    public static final PropertyDefinition<Color> BOTTOM_COLOR =
            PropertyDefinition.of("bottom-color", ColorConverter.INSTANCE);

    private static final int Y_LABEL_WIDTH = 4;
    private static final Style DIM = Style.EMPTY.dim();
    private static final String CENTRE_SEPARATOR = "─";

    private final long[] topData;
    private final long[] bottomData;
    private final Style topStyle;
    private final Style bottomStyle;
    private final Long max;
    private final Block block;
    private final Sparkline.BarSet barSet;
    private final Sparkline.BarSet reversedBarSet;
    private final Sparkline.RenderDirection direction;
    private final boolean showYAxis;
    private final LongFunction<String> yAxisFormatter;
    private final String[] xLabels;

    private DualSparkline(Builder builder) {
        this.topData = builder.topData;
        this.bottomData = builder.bottomData;
        this.max = builder.max;
        this.block = builder.block;
        this.barSet = builder.barSet;
        this.reversedBarSet = builder.barSet.reversed();
        this.direction = builder.direction;
        this.showYAxis = builder.showYAxis;
        this.yAxisFormatter = builder.yAxisFormatter;
        this.xLabels = builder.xLabels;

        Color resolvedTopFg = builder.resolveTopColor();
        Style baseTopStyle = builder.topStyle;
        this.topStyle = resolvedTopFg != null ? baseTopStyle.fg(resolvedTopFg) : baseTopStyle;

        Color resolvedBottomFg = builder.resolveBottomColor();
        Style baseBottomStyle = builder.bottomStyle;
        this.bottomStyle = resolvedBottomFg != null ? baseBottomStyle.fg(resolvedBottomFg) : baseBottomStyle;
    }

    /**
     * Creates a new builder.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a dual sparkline with the given top and bottom data using default settings.
     *
     * @param topData    the top series data (bars grow upward from centre)
     * @param bottomData the bottom series data (bars grow downward from centre)
     * @return a new DualSparkline
     */
    public static DualSparkline from(long[] topData, long[] bottomData) {
        return builder().topData(topData).bottomData(bottomData).build();
    }

    /**
     * Creates a dual sparkline with the given top and bottom data using default settings.
     *
     * @param topData    the top series data (bars grow upward from centre)
     * @param bottomData the bottom series data (bars grow downward from centre)
     * @return a new DualSparkline
     */
    public static DualSparkline from(List<Long> topData, List<Long> bottomData) {
        return builder().topData(topData).bottomData(bottomData).build();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty()) {
            return;
        }

        Rect inner = area;
        if (block != null) {
            block.render(area, buffer);
            inner = block.inner(area);
        }

        if (inner.isEmpty()) {
            return;
        }

        int innerH = inner.height();
        int innerW = inner.width();

        boolean hasXAxis = xLabels != null && xLabels.length > 0;
        // Reserve one row at the bottom for x-axis labels when configured
        int chartBodyRows = hasXAxis ? Math.max(1, innerH - 1) : innerH;
        // Layout: top half, optional centre separator, bottom half.
        // At height >= 3 the centre separator is shown; at height <= 2 it is
        // dropped so both series still get a data row.
        //   5 rows → top=2, centre=1, bottom=2
        //   4 rows → top=1, centre=1, bottom=1, spare=1 (empty)
        //   3 rows → top=1, centre=1, bottom=1
        //   2 rows → top=1, bottom=1 (no centre separator)
        //   1 row  → top=1 only
        boolean hasCentre = chartBodyRows >= 3;
        int topHalfH;
        int bottomHalfH;
        int centerRow;
        if (hasCentre) {
            // Both halves get equal rows; any spare row stays empty
            int dataRows = chartBodyRows - 1; // exclude centre
            topHalfH = dataRows / 2;
            bottomHalfH = dataRows / 2;
            centerRow = topHalfH;
        } else if (chartBodyRows >= 2) {
            // No centre separator — both series get 1 row
            topHalfH = 1;
            bottomHalfH = 1;
            centerRow = -1;
        } else {
            // Single row — top only
            topHalfH = 1;
            bottomHalfH = 0;
            centerRow = -1;
        }

        int yLabelW = showYAxis ? Y_LABEL_WIDTH : 0;
        int chartW = Math.max(1, innerW - yLabelW);

        int dataLen = Math.max(topData.length, bottomData.length);
        int ticks = Math.min(dataLen, chartW);

        long effectiveMax = computeMax();

        // --- Bar rows ---
        for (int r = 0; r < chartBodyRows; r++) {
            int y = inner.y() + r;

            if (showYAxis) {
                String maxLabel;
                if (yAxisFormatter != null) {
                    maxLabel = String.format("%4s", yAxisFormatter.apply(effectiveMax));
                } else {
                    maxLabel = effectiveMax > 9999 ? "999+" : String.format("%4d", effectiveMax);
                }
                String label;
                if (r == 0) {
                    label = maxLabel;
                } else if (hasCentre && r == centerRow) {
                    label = "   0";
                } else if (bottomHalfH > 0 && r == chartBodyRows - 1) {
                    label = maxLabel;
                } else {
                    label = "    ";
                }
                buffer.setString(inner.x(), y, label, DIM);
            }

            for (int t = 0; t < ticks; t++) {
                int x = direction == Sparkline.RenderDirection.RIGHT_TO_LEFT
                        ? inner.x() + yLabelW + (ticks - 1 - t)
                        : inner.x() + yLabelW + t;
                int dataIdx = dataLen - ticks + t;
                long topVal = dataIdx >= 0 && dataIdx < topData.length ? topData[dataIdx] : 0;
                long botVal = dataIdx >= 0 && dataIdx < bottomData.length ? bottomData[dataIdx] : 0;

                String ch;
                Style style;

                // Determine which region this row belongs to
                int topStart = 0;
                int bottomStart = hasCentre ? centerRow + 1 : topHalfH;

                if (r >= topStart && r < topStart + topHalfH) {
                    // Top series: bars grow upward from the centre
                    int rowOffset = (topStart + topHalfH - 1) - r; // 0 at row nearest centre
                    long barPx = topVal * topHalfH * 8 / effectiveMax;
                    long threshold = (long) rowOffset * 8;
                    if (barPx >= threshold + 8) {
                        ch = barSet.full();
                    } else if (barPx > threshold) {
                        ch = barSet.symbolForLevel((double) (barPx - threshold) / 8.0);
                    } else {
                        ch = barSet.empty();
                    }
                    style = topStyle;
                } else if (hasCentre && r == centerRow) {
                    ch = CENTRE_SEPARATOR;
                    style = DIM;
                } else if (r >= bottomStart && r < bottomStart + bottomHalfH) {
                    // Bottom series: bars grow downward from the centre.
                    // Uses reversed bar set so partial cells fill from the top,
                    // connecting smoothly to full blocks above.
                    int rowOffset = r - bottomStart; // 0 at row nearest centre
                    long barPx = botVal * bottomHalfH * 8 / effectiveMax;
                    long threshold = (long) rowOffset * 8;
                    if (barPx >= threshold + 8) {
                        ch = reversedBarSet.full();
                    } else if (barPx > threshold) {
                        ch = reversedBarSet.symbolForLevel((double) (barPx - threshold) / 8.0);
                    } else {
                        ch = reversedBarSet.empty();
                    }
                    style = bottomStyle;
                } else {
                    // Spare row (even height) — empty
                    ch = barSet.empty();
                    style = Style.EMPTY;
                }

                buffer.setString(x, y, ch, style);
            }
        }

        // --- X-axis label row ---
        if (hasXAxis) {
            int xAxisY = inner.y() + chartBodyRows;
            // Distribute labels evenly across the tick range, writing directly to the buffer.
            // Buffer cells not written remain as empty space (Buffer.empty initialises all to ' ').
            boolean rtl = direction == Sparkline.RenderDirection.RIGHT_TO_LEFT;
            for (int li = 0; li < xLabels.length; li++) {
                String lbl = xLabels[li];
                int lblWidth = CharWidth.of(lbl);
                double rawFraction = xLabels.length > 1 ? (double) li / (xLabels.length - 1) : 0;
                // Mirror label positions for RIGHT_TO_LEFT so the first label lands on the right
                double fraction = rtl ? 1.0 - rawFraction : rawFraction;
                int col = (int) Math.round(fraction * (ticks - 1));
                // Right-align whichever label lands at the rightmost column
                boolean atRightEdge = rtl ? li == 0 : li == xLabels.length - 1;
                int start = atRightEdge
                        ? Math.max(0, col - lblWidth + 1)
                        : col;
                if (start < chartW) {
                    String truncated = CharWidth.substringByWidth(lbl, chartW - start);
                    buffer.setString(inner.x() + yLabelW + start, xAxisY, truncated, DIM);
                }
            }
        }
    }

    private long computeMax() {
        if (max != null) {
            return Math.max(1, max);
        }
        long m = 1;
        for (long v : topData) {
            m = Math.max(m, v);
        }
        for (long v : bottomData) {
            m = Math.max(m, v);
        }
        return m;
    }

    /**
     * Builder for {@link DualSparkline}.
     */
    public static final class Builder {
        private long[] topData = new long[0];
        private long[] bottomData = new long[0];
        private Style topStyle = Style.EMPTY;
        private Style bottomStyle = Style.EMPTY;
        private Long max;
        private Block block;
        private Sparkline.BarSet barSet = Sparkline.BarSet.NINE_LEVELS;
        private Sparkline.RenderDirection direction = Sparkline.RenderDirection.LEFT_TO_RIGHT;
        private boolean showYAxis = false;
        private LongFunction<String> yAxisFormatter;
        private String[] xLabels;
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();
        private Color topForeground;
        private Color bottomForeground;

        private Builder() {
        }

        /**
         * Sets the top series data (bars grow upward from centre).
         *
         * @param data the data values
         * @return this builder
         */
        public Builder topData(long... data) {
            this.topData = data != null ? data.clone() : new long[0];
            return this;
        }

        /**
         * Sets the top series data from an int array (bars grow upward from centre).
         *
         * @param data the data values
         * @return this builder
         */
        public Builder topData(int... data) {
            if (data == null) {
                this.topData = new long[0];
            } else {
                this.topData = new long[data.length];
                for (int i = 0; i < data.length; i++) {
                    this.topData[i] = data[i];
                }
            }
            return this;
        }

        /**
         * Sets the top series data from a list (bars grow upward from centre).
         *
         * @param data the data values
         * @return this builder
         */
        public Builder topData(List<Long> data) {
            this.topData = data == null ? new long[0] : data.stream().mapToLong(Long::longValue).toArray();
            return this;
        }

        /**
         * Sets the bottom series data (bars grow downward from centre).
         *
         * @param data the data values
         * @return this builder
         */
        public Builder bottomData(long... data) {
            this.bottomData = data != null ? data.clone() : new long[0];
            return this;
        }

        /**
         * Sets the bottom series data from an int array (bars grow downward from centre).
         *
         * @param data the data values
         * @return this builder
         */
        public Builder bottomData(int... data) {
            if (data == null) {
                this.bottomData = new long[0];
            } else {
                this.bottomData = new long[data.length];
                for (int i = 0; i < data.length; i++) {
                    this.bottomData[i] = data[i];
                }
            }
            return this;
        }

        /**
         * Sets the bottom series data from a list (bars grow downward from centre).
         *
         * @param data the data values
         * @return this builder
         */
        public Builder bottomData(List<Long> data) {
            this.bottomData = data == null ? new long[0] : data.stream().mapToLong(Long::longValue).toArray();
            return this;
        }

        /**
         * Sets the style for the top series bars.
         *
         * @param style the style
         * @return this builder
         */
        public Builder topStyle(Style style) {
            this.topStyle = style != null ? style : Style.EMPTY;
            return this;
        }

        /**
         * Sets the style for the bottom series bars.
         *
         * @param style the style
         * @return this builder
         */
        public Builder bottomStyle(Style style) {
            this.bottomStyle = style != null ? style : Style.EMPTY;
            return this;
        }

        /**
         * Sets an explicit maximum value for scaling both series. When not set the maximum value across both datasets
         * is used.
         *
         * @param max the maximum value
         * @return this builder
         */
        public Builder max(long max) {
            this.max = max;
            return this;
        }

        /**
         * Clears an explicit maximum, reverting to auto-scaling from the data.
         *
         * @return this builder
         */
        public Builder autoMax() {
            this.max = null;
            return this;
        }

        /**
         * Wraps the chart in a block (border + optional title).
         *
         * @param block the block
         * @return this builder
         */
        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Sets the bar symbol set used for sub-pixel rendering.
         *
         * @param barSet the bar set
         * @return this builder
         */
        public Builder barSet(Sparkline.BarSet barSet) {
            this.barSet = barSet != null ? barSet : Sparkline.BarSet.NINE_LEVELS;
            return this;
        }

        /**
         * Sets the render direction. In {@link Sparkline.RenderDirection#LEFT_TO_RIGHT} (default) the oldest
         * data point appears at the left and the newest at the right. In
         * {@link Sparkline.RenderDirection#RIGHT_TO_LEFT} the newest data point appears at the left, matching
         * a right-to-left scrolling display.
         *
         * @param direction the render direction
         * @return this builder
         */
        public Builder direction(Sparkline.RenderDirection direction) {
            this.direction = direction != null ? direction : Sparkline.RenderDirection.LEFT_TO_RIGHT;
            return this;
        }

        /**
         * Controls whether a Y-axis label column is rendered on the left. Shows the shared maximum at the top and
         * bottom rows and {@code 0} at the centre row. Defaults to {@code false}.
         *
         * @param show whether to show the y-axis labels
         * @return this builder
         */
        public Builder showYAxis(boolean show) {
            this.showYAxis = show;
            return this;
        }

        /**
         * Sets a custom formatter for Y-axis labels. When set, used instead of the default integer format. The
         * function receives the raw data value and should return a short label (up to 4 characters).
         *
         * @param formatter function mapping a data value to a label string
         * @return this builder
         */
        public Builder yAxisFormatter(LongFunction<String> formatter) {
            this.yAxisFormatter = formatter;
            return this;
        }

        /**
         * Sets the x-axis labels rendered as a single row below the chart body. Labels are distributed evenly across
         * the data range. The last label is right-aligned at its position so it does not overflow the right edge.
         * <p>
         * Example: {@code xLabels("-60s", "-45s", "-30s", "-15s", "now")}
         *
         * @param labels the labels, distributed left-to-right
         * @return this builder
         */
        public Builder xLabels(String... labels) {
            this.xLabels = labels != null ? labels.clone() : null;
            return this;
        }

        /**
         * Sets the CSS property resolver used to read {@code top-color} and {@code bottom-color} properties.
         * Programmatic values set via {@link #topForeground(Color)} and {@link #bottomForeground(Color)} take
         * precedence over CSS values.
         *
         * @param resolver the resolver
         * @return this builder
         */
        public Builder styleResolver(StylePropertyResolver resolver) {
            this.styleResolver = resolver != null ? resolver : StylePropertyResolver.empty();
            return this;
        }

        /**
         * Sets the foreground colour for the top series bars, overriding any CSS {@code top-color} property.
         *
         * @param color the colour
         * @return this builder
         */
        public Builder topForeground(Color color) {
            this.topForeground = color;
            return this;
        }

        /**
         * Sets the foreground colour for the bottom series bars, overriding any CSS {@code bottom-color} property.
         *
         * @param color the colour
         * @return this builder
         */
        public Builder bottomForeground(Color color) {
            this.bottomForeground = color;
            return this;
        }

        private Color resolveTopColor() {
            return styleResolver.resolve(TOP_COLOR, topForeground);
        }

        private Color resolveBottomColor() {
            return styleResolver.resolve(BOTTOM_COLOR, bottomForeground);
        }

        /**
         * Builds the widget.
         *
         * @return a new DualSparkline
         */
        public DualSparkline build() {
            return new DualSparkline(this);
        }
    }
}

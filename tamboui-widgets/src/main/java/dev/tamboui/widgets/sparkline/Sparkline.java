/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.sparkline;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.LongFunction;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.ColorGradient;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.StandardProperties;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.text.CharWidth;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;

/**
 * A sparkline widget for displaying data trends in a compact form.
 * <p>
 * Sparklines are small, word-sized graphics that show data trends.
 * Each data point is rendered as a bar using Unicode block characters
 * with varying heights based on the value.
 *
 * <pre>{@code
 * // Simple sparkline with data
 * Sparkline sparkline = Sparkline.builder()
 *     .data(1, 2, 3, 4, 5, 4, 3, 2, 1)
 *     .style(Style.EMPTY.fg(Color.CYAN))
 *     .build();
 *
 * // With block wrapper, max, Y-axis and X-axis labels
 * Sparkline sparkline2 = Sparkline.builder()
 *     .data(dataArray)
 *     .max(100)
 *     .showYAxis(true)
 *     .xLabels("-60s", "-30s", "now")
 *     .block(Block.bordered().title(Title.from("CPU Usage")))
 *     .barSet(Sparkline.BarSet.THREE_LEVELS)
 *     .build();
 * }</pre>
 *
 * @see BarSet
 */
public final class Sparkline implements Widget {

    /**
     * CSS property for the gradient low stop color ({@code gradient-low}).
     */
    public static final PropertyDefinition<Color> GRADIENT_LOW =
            PropertyDefinition.of("gradient-low", ColorConverter.INSTANCE);

    /**
     * CSS property for the gradient mid stop color ({@code gradient-mid}).
     */
    public static final PropertyDefinition<Color> GRADIENT_MID =
            PropertyDefinition.of("gradient-mid", ColorConverter.INSTANCE);

    /**
     * CSS property for the gradient high stop color ({@code gradient-high}).
     */
    public static final PropertyDefinition<Color> GRADIENT_HIGH =
            PropertyDefinition.of("gradient-high", ColorConverter.INSTANCE);

    static {
        PropertyRegistry.registerAll(GRADIENT_LOW, GRADIENT_MID, GRADIENT_HIGH);
    }

    /**
     * Direction for rendering sparkline data.
     * <p>
     * Controls the order in which data points are laid out horizontally.
     * Used by both Sparkline and DualSparkline.
     */
    public enum RenderDirection {
        /** Render data from left to right (default). Oldest data at the left, newest at the right. */
        LEFT_TO_RIGHT,
        /** Render data from right to left. Newest data at the left, oldest at the right. */
        RIGHT_TO_LEFT
    }

    /**
     * Symbol set for rendering bar heights.
     * <p>
     * Contains Unicode block characters for different fill levels:
     * <ul>
     *   <li><b>empty</b> - symbol for zero/empty value</li>
     *   <li><b>oneEighth</b> - symbol for 1/8 fill</li>
     *   <li><b>oneQuarter</b> - symbol for 1/4 fill</li>
     *   <li><b>threeEighths</b> - symbol for 3/8 fill</li>
     *   <li><b>half</b> - symbol for 1/2 fill</li>
     *   <li><b>fiveEighths</b> - symbol for 5/8 fill</li>
     *   <li><b>threeQuarters</b> - symbol for 3/4 fill</li>
     *   <li><b>sevenEighths</b> - symbol for 7/8 fill</li>
     *   <li><b>full</b> - symbol for full fill</li>
     * </ul>
     */
    public static final class BarSet {
        private final String empty;
        private final String oneEighth;
        private final String oneQuarter;
        private final String threeEighths;
        private final String half;
        private final String fiveEighths;
        private final String threeQuarters;
        private final String sevenEighths;
        private final String full;

        /**
         * Creates a new bar set with the given symbols.
         *
         * @param empty         symbol for zero/empty value
         * @param oneEighth     symbol for 1/8 fill
         * @param oneQuarter    symbol for 1/4 fill
         * @param threeEighths  symbol for 3/8 fill
         * @param half          symbol for 1/2 fill
         * @param fiveEighths   symbol for 5/8 fill
         * @param threeQuarters symbol for 3/4 fill
         * @param sevenEighths  symbol for 7/8 fill
         * @param full          symbol for full fill
         */
        public BarSet(
            String empty,
            String oneEighth,
            String oneQuarter,
            String threeEighths,
            String half,
            String fiveEighths,
            String threeQuarters,
            String sevenEighths,
            String full
        ) {
            this.empty = empty;
            this.oneEighth = oneEighth;
            this.oneQuarter = oneQuarter;
            this.threeEighths = threeEighths;
            this.half = half;
            this.fiveEighths = fiveEighths;
            this.threeQuarters = threeQuarters;
            this.sevenEighths = sevenEighths;
            this.full = full;
        }
        /**
         * Nine-level bar set with fine-grained fill levels.
         * Uses: ▁▂▃▄▅▆▇█ (fills from bottom of cell upward).
         */
        public static final BarSet NINE_LEVELS = new BarSet(
            " ", "▁", "▂", "▃", "▄", "▅", "▆", "▇", "█"
        );

        /**
         * Three-level bar set with coarse fill levels.
         * Uses: ▄█ (empty, half, full; fills from bottom of cell upward).
         */
        public static final BarSet THREE_LEVELS = new BarSet(
            " ", "▄", "▄", "▄", "▄", "█", "█", "█", "█"
        );

        /**
         * Nine-level reversed bar set for downward-growing bars.
         * Uses: ▔▀█ — fills from top of cell downward.
         * <p>
         * Unicode only defines upper one-eighth (▔) and upper half (▀) block characters,
         * so intermediate levels are approximated to the nearest available symbol.
         */
        public static final BarSet NINE_LEVELS_REVERSED = new BarSet(
            " ", "▔", "▔", "▀", "▀", "▀", "█", "█", "█"
        );

        /**
         * Three-level reversed bar set for downward-growing bars.
         * Uses: ▀█ (empty, half, full; fills from top of cell downward).
         */
        public static final BarSet THREE_LEVELS_REVERSED = new BarSet(
            " ", "▀", "▀", "▀", "▀", "█", "█", "█", "█"
        );

        /**
         * Returns the reversed bar set for downward-growing bars.
         * <p>
         * The reversed set uses upper block characters that fill from the top of the cell
         * downward, suitable for the bottom half of a dual sparkline. If this bar set
         * is {@link #NINE_LEVELS}, returns {@link #NINE_LEVELS_REVERSED}; if {@link #THREE_LEVELS},
         * returns {@link #THREE_LEVELS_REVERSED}; otherwise returns {@link #NINE_LEVELS_REVERSED}
         * as a best-effort default.
         *
         * @return a bar set with symbols that fill from the top of the cell
         */
        public BarSet reversed() {
            if (this.equals(THREE_LEVELS)) {
                return THREE_LEVELS_REVERSED;
            }
            return NINE_LEVELS_REVERSED;
        }

        /**
         * Returns the symbol for the given fill level (0.0 to 1.0).
         *
         * @param level the fill level between 0.0 and 1.0
         * @return the symbol for the given level
         */
        public String symbolForLevel(double level) {
            if (level <= 0.0) {
                return empty;
            }
            if (level <= 0.125) {
                return oneEighth;
            }
            if (level <= 0.250) {
                return oneQuarter;
            }
            if (level <= 0.375) {
                return threeEighths;
            }
            if (level <= 0.500) {
                return half;
            }
            if (level <= 0.625) {
                return fiveEighths;
            }
            if (level <= 0.750) {
                return threeQuarters;
            }
            if (level <= 0.875) {
                return sevenEighths;
            }
            return full;
        }

        /**
         * Returns the empty symbol.
         *
         * @return the empty symbol
         */
        public String empty() {
            return empty;
        }

        /**
         * Returns the one-eighth fill symbol.
         *
         * @return the one-eighth symbol
         */
        public String oneEighth() {
            return oneEighth;
        }

        /**
         * Returns the one-quarter fill symbol.
         *
         * @return the one-quarter symbol
         */
        public String oneQuarter() {
            return oneQuarter;
        }

        /**
         * Returns the three-eighths fill symbol.
         *
         * @return the three-eighths symbol
         */
        public String threeEighths() {
            return threeEighths;
        }

        /**
         * Returns the half fill symbol.
         *
         * @return the half symbol
         */
        public String half() {
            return half;
        }

        /**
         * Returns the five-eighths fill symbol.
         *
         * @return the five-eighths symbol
         */
        public String fiveEighths() {
            return fiveEighths;
        }

        /**
         * Returns the three-quarters fill symbol.
         *
         * @return the three-quarters symbol
         */
        public String threeQuarters() {
            return threeQuarters;
        }

        /**
         * Returns the seven-eighths fill symbol.
         *
         * @return the seven-eighths symbol
         */
        public String sevenEighths() {
            return sevenEighths;
        }

        /**
         * Returns the full fill symbol.
         *
         * @return the full symbol
         */
        public String full() {
            return full;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BarSet)) {
                return false;
            }
            BarSet barSet = (BarSet) o;
            return empty.equals(barSet.empty)
                && oneEighth.equals(barSet.oneEighth)
                && oneQuarter.equals(barSet.oneQuarter)
                && threeEighths.equals(barSet.threeEighths)
                && half.equals(barSet.half)
                && fiveEighths.equals(barSet.fiveEighths)
                && threeQuarters.equals(barSet.threeQuarters)
                && sevenEighths.equals(barSet.sevenEighths)
                && full.equals(barSet.full);
        }

        @Override
        public int hashCode() {
            int result = empty.hashCode();
            result = 31 * result + oneEighth.hashCode();
            result = 31 * result + oneQuarter.hashCode();
            result = 31 * result + threeEighths.hashCode();
            result = 31 * result + half.hashCode();
            result = 31 * result + fiveEighths.hashCode();
            result = 31 * result + threeQuarters.hashCode();
            result = 31 * result + sevenEighths.hashCode();
            result = 31 * result + full.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return String.format(
                "BarSet[empty=%s, oneEighth=%s, oneQuarter=%s, threeEighths=%s, half=%s, fiveEighths=%s, threeQuarters=%s, sevenEighths=%s, full=%s]",
                empty, oneEighth, oneQuarter, threeEighths, half, fiveEighths, threeQuarters, sevenEighths, full);
        }
    }

    private static final int Y_LABEL_WIDTH = 4;
    private static final Style DIM = Style.EMPTY.dim();

    private final long[] data;
    private final Long max;
    private final Block block;
    private final BarSet barSet;
    private final RenderDirection direction;
    private final Style style;
    private final boolean showYAxis;
    private final String[] xLabels;
    private final LongFunction<Color> colorMapper;
    private final ColorGradient gradient;

    private Sparkline(Builder builder) {
        this.data = builder.data;
        this.max = builder.max;
        this.block = builder.block;
        this.barSet = builder.barSet;
        this.direction = builder.direction;
        this.showYAxis = builder.showYAxis;
        this.xLabels = builder.xLabels;
        this.colorMapper = builder.colorMapper;

        // Resolve style-aware properties
        Color resolvedFg = builder.resolveForeground();

        Style baseStyle = builder.style;
        if (resolvedFg != null) {
            baseStyle = baseStyle.fg(resolvedFg);
        }
        this.style = baseStyle;

        // Resolve gradient: programmatic takes precedence over CSS stops
        ColorGradient resolvedGradient = builder.gradient;
        if (resolvedGradient == null) {
            resolvedGradient = builder.resolveGradient();
        }
        this.gradient = resolvedGradient;
    }

    /**
     * Returns the custom color mapper, if set.
     *
     * @return the color mapper, or empty
     */
    public Optional<LongFunction<Color>> colorMapper() {
        return Optional.ofNullable(colorMapper);
    }

    /**
     * Returns the color gradient, if set.
     *
     * @return the color gradient, or empty
     */
    public Optional<ColorGradient> gradient() {
        return Optional.ofNullable(gradient);
    }

    /**
     * Creates a new sparkline builder.
     *
     * @return a new Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a sparkline with the given data values.
     *
     * @param data the data values
     * @return a new Sparkline
     */
    public static Sparkline from(long... data) {
        return builder().data(data).build();
    }

    /**
     * Creates a sparkline with the given data values.
     *
     * @param data the data values
     * @return a new Sparkline
     */
    public static Sparkline from(List<Long> data) {
        return builder().data(data).build();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty()) {
            return;
        }

        // Render block if present (even with empty data, so borders/titles remain visible)
        Rect sparklineArea = area;
        if (block != null) {
            block.render(area, buffer);
            sparklineArea = block.inner(area);
        }

        if (sparklineArea.isEmpty() || data.length == 0) {
            return;
        }

        // Calculate the effective max value
        long effectiveMax = calculateMax();
        if (effectiveMax == 0) {
            effectiveMax = 1; // Avoid division by zero
        }

        int innerH = sparklineArea.height();
        boolean hasXAxis = xLabels != null && xLabels.length > 0;
        // Reserve one row at the bottom for x-axis labels when configured
        int chartH = hasXAxis ? Math.max(1, innerH - 1) : innerH;

        int yLabelW = showYAxis ? Y_LABEL_WIDTH : 0;
        int chartW = Math.max(1, sparklineArea.width() - yLabelW);

        // Determine how many data points to display
        int displayCount = Math.min(data.length, chartW);
        int dataOffset = data.length > chartW
            ? data.length - chartW
            : 0;

        // Render Y-axis labels
        if (showYAxis) {
            String maxLabel = effectiveMax > 9999 ? "999+" : String.format("%4d", effectiveMax);
            // Max label at top row of chart
            buffer.setString(sparklineArea.x(), sparklineArea.y(), maxLabel, DIM);
            if (chartH > 1) {
                // Zero label at bottom row of chart
                buffer.setString(sparklineArea.x(), sparklineArea.y() + chartH - 1, "   0", DIM);
            }
        }

        Style effectiveStyle = style != null ? style : Style.EMPTY;

        // Render each bar column across all chart rows (multi-row sub-pixel rendering)
        for (int i = 0; i < displayCount; i++) {
            int dataIndex = dataOffset + i;

            if (dataIndex < 0 || dataIndex >= data.length) {
                continue;
            }

            long value = data[dataIndex];
            Style columnStyle = effectiveStyle;
            if (colorMapper != null) {
                Color mapped = colorMapper.apply(value);
                if (mapped != null) {
                    columnStyle = columnStyle.fg(mapped);
                }
            } else if (gradient != null) {
                double ratio = effectiveMax > 0 ? (double) value / effectiveMax : 0.0;
                columnStyle = columnStyle.fg(gradient.at(ratio));
            }

            // Scale value to sub-pixel height: chartH rows * 8 levels per row
            long barPx = effectiveMax > 0 ? value * chartH * 8 / effectiveMax : 0;

            // In LEFT_TO_RIGHT: data[0] at left (x=0), data[n] at right
            // In RIGHT_TO_LEFT: data[0] at right, data[n] at left
            int x = direction == RenderDirection.LEFT_TO_RIGHT
                ? sparklineArea.x() + yLabelW + i
                : sparklineArea.x() + yLabelW + (displayCount - 1 - i);

            if (x < sparklineArea.x() || x >= sparklineArea.right()) {
                continue;
            }

            // Render from bottom row to top row
            long remaining = barPx;
            for (int row = chartH - 1; row >= 0; row--) {
                String symbol;
                if (remaining >= 8) {
                    symbol = barSet.full();
                } else if (remaining > 0) {
                    symbol = barSet.symbolForLevel((double) remaining / 8.0);
                } else {
                    symbol = barSet.empty();
                }
                int y = sparklineArea.y() + row;
                buffer.setString(x, y, symbol, columnStyle);
                remaining = remaining >= 8 ? remaining - 8 : 0;
            }
        }

        // Render X-axis labels below the chart body
        if (hasXAxis && innerH > chartH) {
            int xAxisY = sparklineArea.y() + chartH;
            boolean rtl = direction == RenderDirection.RIGHT_TO_LEFT;
            for (int li = 0; li < xLabels.length; li++) {
                String lbl = xLabels[li];
                int lblWidth = CharWidth.of(lbl);
                double rawFraction = xLabels.length > 1 ? (double) li / (xLabels.length - 1) : 0;
                double fraction = rtl ? 1.0 - rawFraction : rawFraction;
                int col = (int) Math.round(fraction * (displayCount - 1));
                boolean atRightEdge = rtl ? li == 0 : li == xLabels.length - 1;
                int start = atRightEdge
                        ? Math.max(0, col - lblWidth + 1)
                        : col;
                if (start < chartW) {
                    String truncated = CharWidth.substringByWidth(lbl, chartW - start);
                    buffer.setString(sparklineArea.x() + yLabelW + start, xAxisY, truncated, DIM);
                }
            }
        }
    }

    private long calculateMax() {
        if (max != null) {
            return max;
        }
        return Arrays.stream(data).max().orElse(0);
    }

    /**
     * Builder for {@link Sparkline}.
     */
    public static final class Builder {
        private long[] data = new long[0];
        private Long max;
        private Block block;
        private BarSet barSet = BarSet.NINE_LEVELS;
        private RenderDirection direction = RenderDirection.LEFT_TO_RIGHT;
        private Style style = Style.EMPTY;
        private boolean showYAxis = false;
        private String[] xLabels;
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();

        // Style-aware properties (resolved via styleResolver in build())
        private Color foreground;
        private LongFunction<Color> colorMapper;
        private ColorGradient gradient;
        private Color gradientLow;
        private Color gradientMid;
        private Color gradientHigh;

        private Builder() {}

        /**
         * Sets the data values to display.
         *
         * @param data the data values
         * @return this builder
         */
        public Builder data(long... data) {
            this.data = data != null ? data.clone() : new long[0];
            return this;
        }

        /**
         * Sets the data values from a list.
         *
         * @param data the data values
         * @return this builder
         */
        public Builder data(List<Long> data) {
            if (data == null || data.isEmpty()) {
                this.data = new long[0];
            } else {
                this.data = data.stream().mapToLong(Long::longValue).toArray();
            }
            return this;
        }

        /**
         * Sets the data values from an int array.
         *
         * @param data the data values
         * @return this builder
         */
        public Builder data(int... data) {
            if (data == null) {
                this.data = new long[0];
            } else {
                this.data = new long[data.length];
                for (int i = 0; i < data.length; i++) {
                    this.data[i] = data[i];
                }
            }
            return this;
        }

        /**
         * Sets the maximum value for scaling.
         * <p>
         * If not set, the maximum value in the data is used.
         *
         * @param max the maximum value
         * @return this builder
         */
        public Builder max(long max) {
            this.max = max;
            return this;
        }

        /**
         * Clears the explicit maximum value, using data maximum instead.
         *
         * @return this builder
         */
        public Builder autoMax() {
            this.max = null;
            return this;
        }

        /**
         * Wraps the sparkline in a block.
         *
         * @param block the block to wrap in
         * @return this builder
         */
        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Sets the bar symbol set.
         *
         * @param barSet the bar symbol set
         * @return this builder
         */
        public Builder barSet(BarSet barSet) {
            this.barSet = barSet != null ? barSet : BarSet.NINE_LEVELS;
            return this;
        }

        /**
         * Sets the render direction.
         *
         * @param direction the render direction
         * @return this builder
         */
        public Builder direction(RenderDirection direction) {
            this.direction = direction != null ? direction : RenderDirection.LEFT_TO_RIGHT;
            return this;
        }

        /**
         * Controls whether a Y-axis label is rendered on the left showing the maximum data value.
         * The label column is 4 characters wide. Defaults to {@code false}.
         * <p>
         * When the sparkline area has only 1 row of height, the label appears on the same row as the bars.
         *
         * @param show whether to show the y-axis label
         * @return this builder
         */
        public Builder showYAxis(boolean show) {
            this.showYAxis = show;
            return this;
        }

        /**
         * Sets the x-axis labels rendered as a single row below the sparkline bars. Labels are distributed
         * evenly across the data range. The last label is right-aligned at its position so it does not
         * overflow the right edge. Requires at least 2 rows of height (1 for bars + 1 for labels).
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
         * Sets the style for the sparkline bars.
         *
         * @param style the bar style
         * @return this builder
         */
        public Builder style(Style style) {
            this.style = style != null ? style : Style.EMPTY;
            return this;
        }

        /**
         * Sets the property resolver for style-aware properties.
         * <p>
         * When set, properties like {@code color} will be resolved
         * if not set programmatically.
         *
         * @param resolver the property resolver
         * @return this builder
         */
        public Builder styleResolver(StylePropertyResolver resolver) {
            this.styleResolver = resolver != null ? resolver : StylePropertyResolver.empty();
            return this;
        }

        /**
         * Sets the foreground (bar) color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the foreground color
         * @return this builder
         */
        public Builder foreground(Color color) {
            this.foreground = color;
            return this;
        }

        /**
         * Sets a custom color mapper function that maps each data value to a bar foreground color.
         * <p>
         * When set, this takes precedence over gradients and base styles.
         *
         * @param colorMapper the color mapper function
         * @return this builder
         */
        public Builder colorMapper(LongFunction<Color> colorMapper) {
            this.colorMapper = colorMapper;
            return this;
        }

        /**
         * Sets color gradient stops to map normalized values in {@code [0, max]} to foreground colors.
         *
         * @param stops the color gradient stops
         * @return this builder
         */
        public Builder gradient(Color... stops) {
            this.gradient = (stops != null && stops.length > 0) ? ColorGradient.of(stops) : null;
            return this;
        }

        /**
         * Sets a color gradient to map normalized values in {@code [0, max]} to foreground colors.
         *
         * @param gradient the color gradient
         * @return this builder
         */
        public Builder gradient(ColorGradient gradient) {
            this.gradient = gradient;
            return this;
        }

        /**
         * Sets the gradient low stop color programmatically.
         *
         * @param color the low stop color
         * @return this builder
         */
        public Builder gradientLow(Color color) {
            this.gradientLow = color;
            return this;
        }

        /**
         * Sets the gradient mid stop color programmatically.
         *
         * @param color the mid stop color
         * @return this builder
         */
        public Builder gradientMid(Color color) {
            this.gradientMid = color;
            return this;
        }

        /**
         * Sets the gradient high stop color programmatically.
         *
         * @param color the high stop color
         * @return this builder
         */
        public Builder gradientHigh(Color color) {
            this.gradientHigh = color;
            return this;
        }

        /**
         * Builds the sparkline.
         *
         * @return a new Sparkline
         */
        public Sparkline build() {
            return new Sparkline(this);
        }

        // Resolution helpers
        private Color resolveForeground() {
            return styleResolver.resolve(StandardProperties.COLOR, foreground);
        }

        private ColorGradient resolveGradient() {
            Color low = styleResolver.resolve(GRADIENT_LOW, gradientLow);
            Color mid = styleResolver.resolve(GRADIENT_MID, gradientMid);
            Color high = styleResolver.resolve(GRADIENT_HIGH, gradientHigh);

            List<Color> stops = new ArrayList<>();
            if (low != null) {
                stops.add(low);
            }
            if (mid != null) {
                stops.add(mid);
            }
            if (high != null) {
                stops.add(high);
            }

            return stops.isEmpty() ? null : ColorGradient.of(stops);
        }
    }
}

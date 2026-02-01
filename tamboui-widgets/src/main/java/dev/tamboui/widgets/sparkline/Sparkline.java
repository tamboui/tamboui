/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.sparkline;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.style.StandardProperties;
import dev.tamboui.style.Style;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;

import java.util.Arrays;
import java.util.List;

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
 * // With block wrapper and custom max
 * Sparkline sparkline2 = Sparkline.builder()
 *     .data(dataArray)
 *     .max(100)
 *     .block(Block.bordered().title(Title.from("CPU Usage")))
 *     .barSet(Sparkline.BarSet.THREE_LEVELS)
 *     .build();
 * }</pre>
 *
 * @see RenderDirection
 * @see BarSet
 */
public final class Sparkline implements Widget {

    /**
     * Direction for rendering sparkline data.
     */
    public enum RenderDirection {
        /** Render data from left to right (default). */
        LEFT_TO_RIGHT,
        /** Render data from right to left. */
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
         * Uses: ▁▂▃▄▅▆▇█
         */
        public static final BarSet NINE_LEVELS = new BarSet(
            " ", "▁", "▂", "▃", "▄", "▅", "▆", "▇", "█"
        );

        /**
         * Three-level bar set with coarse fill levels.
         * Uses: ▄█ (empty, half, full)
         */
        public static final BarSet THREE_LEVELS = new BarSet(
            " ", "▄", "▄", "▄", "▄", "█", "█", "█", "█"
        );

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

    private final long[] data;
    private final Long max;
    private final Block block;
    private final BarSet barSet;
    private final RenderDirection direction;
    private final Style style;

    private Sparkline(Builder builder) {
        this.data = builder.data;
        this.max = builder.max;
        this.block = builder.block;
        this.barSet = builder.barSet;
        this.direction = builder.direction;

        // Resolve style-aware properties
        Color resolvedFg = builder.resolveForeground();

        Style baseStyle = builder.style;
        if (resolvedFg != null) {
            baseStyle = baseStyle.fg(resolvedFg);
        }
        this.style = baseStyle;
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
        if (area.isEmpty() || data.length == 0) {
            return;
        }

        // Render block if present
        Rect sparklineArea = area;
        if (block != null) {
            block.render(area, buffer);
            sparklineArea = block.inner(area);
        }

        if (sparklineArea.isEmpty()) {
            return;
        }

        // Calculate the effective max value
        long effectiveMax = calculateMax();
        if (effectiveMax == 0) {
            effectiveMax = 1; // Avoid division by zero
        }

        // Determine how many data points to display
        int displayCount = Math.min(data.length, sparklineArea.width());
        int dataOffset = data.length > sparklineArea.width()
            ? data.length - sparklineArea.width()
            : 0;

        // Render each bar
        for (int i = 0; i < displayCount; i++) {
            int dataIndex = dataOffset + i;

            if (dataIndex < 0 || dataIndex >= data.length) {
                continue;
            }

            long value = data[dataIndex];
            double level = (double) value / effectiveMax;
            String symbol = barSet.symbolForLevel(level);

            // In LEFT_TO_RIGHT: data[0] at left (x=0), data[n] at right
            // In RIGHT_TO_LEFT: data[0] at right, data[n] at left
            int x = direction == RenderDirection.LEFT_TO_RIGHT
                ? sparklineArea.x() + i
                : sparklineArea.right() - 1 - i;

            // Render from bottom of area
            int y = sparklineArea.bottom() - 1;

            if (x >= sparklineArea.x() && x < sparklineArea.right()) {
                buffer.setString(x, y, symbol, style != null ? style : Style.EMPTY);
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
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();

        // Style-aware properties (resolved via styleResolver in build())
        private Color foreground;

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
    }
}

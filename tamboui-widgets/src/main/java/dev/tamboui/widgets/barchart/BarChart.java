/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.barchart;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Direction;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.style.StandardProperties;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;
import static dev.tamboui.util.CollectionUtil.listCopyOf;

import java.util.ArrayList;
import java.util.List;

/**
 * A bar chart widget for displaying grouped data.
 * <p>
 * Supports both vertical (default) and horizontal bar orientations,
 * with customizable bar widths, gaps, and styling.
 *
 * <pre>{@code
 * // Simple bar chart with values
 * BarChart chart = BarChart.builder()
 *     .data(BarGroup.of(10, 20, 30, 40))
 *     .build();
 *
 * // Grouped bar chart with labels
 * BarChart chart2 = BarChart.builder()
 *     .data(
 *         BarGroup.of("Q1", Bar.of(100, "Jan"), Bar.of(150, "Feb")),
 *         BarGroup.of("Q2", Bar.of(120, "Mar"), Bar.of(180, "Apr"))
 *     )
 *     .barWidth(3)
 *     .barGap(1)
 *     .groupGap(2)
 *     .barStyle(Style.EMPTY.fg(Color.CYAN))
 *     .block(Block.bordered().title(Title.from("Sales")))
 *     .build();
 * }</pre>
 *
 * @see Bar
 * @see BarGroup
 */
public final class BarChart implements Widget {

    /**
     * Property key for the default bar color.
     * <p>
     * CSS property name: {@code bar-color}
     */
    public static final PropertyDefinition<Color> BAR_COLOR =
            PropertyDefinition.of("bar-color", ColorConverter.INSTANCE);

    /**
     * Property key for the value label color.
     * <p>
     * CSS property name: {@code value-color}
     */
    public static final PropertyDefinition<Color> VALUE_COLOR =
            PropertyDefinition.of("value-color", ColorConverter.INSTANCE);

    /**
     * Property key for the bar label color.
     * <p>
     * CSS property name: {@code label-color}
     */
    public static final PropertyDefinition<Color> LABEL_COLOR =
            PropertyDefinition.of("label-color", ColorConverter.INSTANCE);

    static {
        PropertyRegistry.registerAll(BAR_COLOR, VALUE_COLOR, LABEL_COLOR);
    }

    /**
     * Symbol set for rendering bar fills.
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
         * Creates a new bar set with the given symbols for each fill level.
         *
         * @param empty the empty symbol
         * @param oneEighth the one-eighth fill symbol
         * @param oneQuarter the one-quarter fill symbol
         * @param threeEighths the three-eighths fill symbol
         * @param half the half fill symbol
         * @param fiveEighths the five-eighths fill symbol
         * @param threeQuarters the three-quarters fill symbol
         * @param sevenEighths the seven-eighths fill symbol
         * @param full the full fill symbol
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
         * Nine-level vertical bar set.
         */
        public static final BarSet NINE_LEVELS = new BarSet(
            " ", "▁", "▂", "▃", "▄", "▅", "▆", "▇", "█"
        );

        /**
         * Three-level vertical bar set.
         */
        public static final BarSet THREE_LEVELS = new BarSet(
            " ", "▄", "▄", "▄", "▄", "█", "█", "█", "█"
        );

        /**
         * Horizontal bar set (left to right).
         */
        public static final BarSet HORIZONTAL = new BarSet(
            " ", "▏", "▎", "▍", "▌", "▋", "▊", "▉", "█"
        );

        /**
         * Returns the symbols as an array for indexed access.
         *
         * @return the symbols array
         */
        public String[] symbols() {
            return new String[] {
                empty, oneEighth, oneQuarter, threeEighths,
                half, fiveEighths, threeQuarters, sevenEighths, full
            };
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
         * @return the one-eighth fill symbol
         */
        public String oneEighth() {
            return oneEighth;
        }

        /**
         * Returns the one-quarter fill symbol.
         *
         * @return the one-quarter fill symbol
         */
        public String oneQuarter() {
            return oneQuarter;
        }

        /**
         * Returns the three-eighths fill symbol.
         *
         * @return the three-eighths fill symbol
         */
        public String threeEighths() {
            return threeEighths;
        }

        /**
         * Returns the half fill symbol.
         *
         * @return the half fill symbol
         */
        public String half() {
            return half;
        }

        /**
         * Returns the five-eighths fill symbol.
         *
         * @return the five-eighths fill symbol
         */
        public String fiveEighths() {
            return fiveEighths;
        }

        /**
         * Returns the three-quarters fill symbol.
         *
         * @return the three-quarters fill symbol
         */
        public String threeQuarters() {
            return threeQuarters;
        }

        /**
         * Returns the seven-eighths fill symbol.
         *
         * @return the seven-eighths fill symbol
         */
        public String sevenEighths() {
            return sevenEighths;
        }

        /**
         * Returns the full fill symbol.
         *
         * @return the full fill symbol
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

    private final List<BarGroup> data;
    private final Long max;
    private final int barWidth;
    private final int barGap;
    private final int groupGap;
    private final Direction direction;
    private final Style style;
    private final Style barStyle;
    private final Style valueStyle;
    private final Style labelStyle;
    private final Block block;
    private final BarSet barSet;

    private BarChart(Builder builder) {
        this.data = listCopyOf(builder.data);
        this.max = builder.max;
        this.barWidth = builder.barWidth;
        this.barGap = builder.barGap;
        this.groupGap = builder.groupGap;
        this.direction = builder.direction;
        this.block = builder.block;
        this.barSet = builder.barSet;

        // Resolve style-aware properties
        Color resolvedBg = builder.resolveBackground();
        Color resolvedBarColor = builder.resolveBarColor();
        Color resolvedValueColor = builder.resolveValueColor();
        Color resolvedLabelColor = builder.resolveLabelColor();

        Style baseStyle = builder.style;
        if (resolvedBg != null) {
            baseStyle = baseStyle != null
                    ? baseStyle.bg(resolvedBg)
                    : Style.EMPTY.bg(resolvedBg);
        }
        this.style = baseStyle;

        Style baseBarStyle = builder.barStyle;
        if (resolvedBarColor != null) {
            baseBarStyle = baseBarStyle != null
                    ? baseBarStyle.fg(resolvedBarColor)
                    : Style.EMPTY.fg(resolvedBarColor);
        }
        this.barStyle = baseBarStyle;

        Style baseValueStyle = builder.valueStyle;
        if (resolvedValueColor != null) {
            baseValueStyle = baseValueStyle != null
                    ? baseValueStyle.fg(resolvedValueColor)
                    : Style.EMPTY.fg(resolvedValueColor);
        }
        this.valueStyle = baseValueStyle;

        Style baseLabelStyle = builder.labelStyle;
        if (resolvedLabelColor != null) {
            baseLabelStyle = baseLabelStyle != null
                    ? baseLabelStyle.fg(resolvedLabelColor)
                    : Style.EMPTY.fg(resolvedLabelColor);
        }
        this.labelStyle = baseLabelStyle;
    }

    /**
     * Creates a new bar chart builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty() || data.isEmpty()) {
            return;
        }

        // Apply overall style
        if (style != null) {
            buffer.setStyle(area, style);
        }

        // Render block if present
        Rect chartArea = area;
        if (block != null) {
            block.render(area, buffer);
            chartArea = block.inner(area);
        }

        if (chartArea.isEmpty()) {
            return;
        }

        // Calculate max value
        long effectiveMax = calculateMax();
        if (effectiveMax == 0) {
            effectiveMax = 1;
        }

        if (direction == Direction.VERTICAL) {
            renderVertical(chartArea, buffer, effectiveMax);
        } else {
            renderHorizontal(chartArea, buffer, effectiveMax);
        }
    }

    private void renderVertical(Rect area, Buffer buffer, long maxValue) {
        // Reserve space for labels (bottom row)
        int labelHeight = hasLabels() ? 1 : 0;
        int chartHeight = area.height() - labelHeight;

        if (chartHeight <= 0) {
            return;
        }

        int x = area.x();
        boolean firstGroup = true;

        for (BarGroup group : data) {
            if (!firstGroup) {
                x += groupGap;
            }
            firstGroup = false;

            boolean firstBar = true;
            for (Bar bar : group.bars()) {
                if (!firstBar) {
                    x += barGap;
                }
                firstBar = false;

                if (x + barWidth > area.right()) {
                    break; // No more space
                }

                // Calculate bar height
                double ratio = (double) bar.value() / maxValue;
                int barHeight = (int) Math.round(ratio * chartHeight);
                barHeight = Math.min(barHeight, chartHeight);

                // Get effective style
                Style effectiveBarStyle = bar.style().orElse(barStyle != null ? barStyle : Style.EMPTY);

                // Render bar from bottom
                for (int h = 0; h < barHeight; h++) {
                    int y = area.y() + chartHeight - 1 - h;
                    for (int w = 0; w < barWidth && x + w < area.right(); w++) {
                        buffer.setString(x + w, y, barSet.full, effectiveBarStyle);
                    }
                }

                // Render partial fill for sub-cell precision
                if (barHeight < chartHeight && bar.value() > 0) {
                    double remainder = (ratio * chartHeight) - barHeight;
                    if (remainder > 0) {
                        int symbolIndex = (int) Math.round(remainder * 8);
                        symbolIndex = Math.min(symbolIndex, 8);
                        if (symbolIndex > 0) {
                            String[] symbols = barSet.symbols();
                            int y = area.y() + chartHeight - 1 - barHeight;
                            for (int w = 0; w < barWidth && x + w < area.right(); w++) {
                                buffer.setString(x + w, y, symbols[symbolIndex], effectiveBarStyle);
                            }
                        }
                    }
                }

                // Render label if present
                if (labelHeight > 0 && bar.label().isPresent()) {
                    String labelStr = truncate(bar.label().get().rawContent(), barWidth);
                    Style effectiveLabelStyle = labelStyle != null ? labelStyle : Style.EMPTY;
                    int labelStrWidth = CharWidth.of(labelStr);
                    int labelX = x + (barWidth - labelStrWidth) / 2;
                    labelX = Math.max(x, labelX);
                    buffer.setString(labelX, area.bottom() - 1, labelStr, effectiveLabelStyle);
                }

                // Render value above bar
                if (chartHeight > 1 && barHeight < chartHeight) {
                    String valueStr = bar.displayValue();
                    int valueStrWidth = CharWidth.of(valueStr);
                    if (valueStrWidth <= barWidth) {
                        Style effectiveValueStyle = bar.valueStyle()
                            .orElse(valueStyle != null ? valueStyle : Style.EMPTY);
                        int valueX = x + (barWidth - valueStrWidth) / 2;
                        valueX = Math.max(x, valueX);
                        int valueY = area.y() + chartHeight - barHeight - 1;
                        if (valueY >= area.y()) {
                            buffer.setString(valueX, valueY, valueStr, effectiveValueStyle);
                        }
                    }
                }

                x += barWidth;
            }
        }
    }

    private void renderHorizontal(Rect area, Buffer buffer, long maxValue) {
        // Calculate label width (leftmost column)
        int labelWidth = calculateLabelWidth();
        int chartWidth = area.width() - labelWidth - 1; // -1 for spacing

        if (chartWidth <= 0) {
            return;
        }

        int y = area.y();
        boolean firstGroup = true;

        for (BarGroup group : data) {
            if (!firstGroup) {
                y += groupGap;
            }
            firstGroup = false;

            boolean firstBar = true;
            for (Bar bar : group.bars()) {
                if (!firstBar) {
                    y += barGap;
                }
                firstBar = false;

                if (y + barWidth > area.bottom()) {
                    break; // No more space
                }

                // Calculate bar length
                double ratio = (double) bar.value() / maxValue;
                int barLength = (int) Math.round(ratio * chartWidth);
                barLength = Math.min(barLength, chartWidth);

                // Get effective style
                Style effectiveBarStyle = bar.style().orElse(barStyle != null ? barStyle : Style.EMPTY);

                // Render label
                if (bar.label().isPresent()) {
                    String labelStr = truncate(bar.label().get().rawContent(), labelWidth);
                    Style effectiveLabelStyle = labelStyle != null ? labelStyle : Style.EMPTY;
                    // Right-align label
                    int labelStrWidth = CharWidth.of(labelStr);
                    int labelX = area.x() + labelWidth - labelStrWidth;
                    for (int h = 0; h < barWidth && y + h < area.bottom(); h++) {
                        buffer.setString(labelX, y + h, labelStr, effectiveLabelStyle);
                    }
                }

                // Render bar
                int barStartX = area.x() + labelWidth + 1;
                for (int l = 0; l < barLength; l++) {
                    for (int h = 0; h < barWidth && y + h < area.bottom(); h++) {
                        buffer.setString(barStartX + l, y + h, BarSet.HORIZONTAL.full, effectiveBarStyle);
                    }
                }

                // Render value at end of bar
                String valueStr = bar.displayValue();
                int valueStrWidth = CharWidth.of(valueStr);
                if (barLength + valueStrWidth + 1 <= chartWidth) {
                    Style effectiveValueStyle = bar.valueStyle()
                        .orElse(valueStyle != null ? valueStyle : Style.EMPTY);
                    buffer.setString(barStartX + barLength + 1, y, valueStr, effectiveValueStyle);
                }

                y += barWidth;
            }
        }
    }

    private long calculateMax() {
        if (max != null) {
            return max;
        }
        return data.stream()
            .mapToLong(BarGroup::maxValue)
            .max()
            .orElse(0);
    }

    private boolean hasLabels() {
        return data.stream()
            .flatMap(g -> g.bars().stream())
            .anyMatch(b -> b.label().isPresent());
    }

    private int calculateLabelWidth() {
        return data.stream()
            .flatMap(g -> g.bars().stream())
            .filter(b -> b.label().isPresent())
            .mapToInt(b -> b.label().get().width())
            .max()
            .orElse(0);
    }

    private String truncate(String str, int maxWidth) {
        if (CharWidth.of(str) <= maxWidth) {
            return str;
        }
        return CharWidth.substringByWidth(str, maxWidth);
    }

    /**
     * Builder for {@link BarChart}.
     */
    public static final class Builder {
        private final List<BarGroup> data = new ArrayList<>();
        private Long max;
        private int barWidth = 1;
        private int barGap = 1;
        private int groupGap = 1;
        private Direction direction = Direction.VERTICAL;
        private Style style;
        private Style barStyle;
        private Style valueStyle;
        private Style labelStyle;
        private Block block;
        private BarSet barSet = BarSet.NINE_LEVELS;
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();

        // Style-aware properties (resolved via styleResolver in build())
        private Color background;
        private Color barColor;
        private Color valueColor;
        private Color labelColor;

        private Builder() {}

        /**
         * Adds bar groups to the chart.
         *
         * @param groups the bar groups to add
         * @return this builder
         */
        public Builder data(BarGroup... groups) {
            if (groups != null) {
                this.data.addAll(listCopyOf(groups));
            }
            return this;
        }

        /**
         * Adds bar groups to the chart.
         *
         * @param groups the bar groups to add
         * @return this builder
         */
        public Builder data(List<BarGroup> groups) {
            if (groups != null) {
                this.data.addAll(groups);
            }
            return this;
        }

        /**
         * Adds a single bar group.
         *
         * @param group the bar group to add
         * @return this builder
         */
        public Builder addGroup(BarGroup group) {
            if (group != null) {
                this.data.add(group);
            }
            return this;
        }

        /**
         * Sets the maximum value for scaling.
         *
         * @param max the maximum value
         * @return this builder
         */
        public Builder max(long max) {
            this.max = max;
            return this;
        }

        /**
         * Clears the explicit max, using data maximum instead.
         *
         * @return this builder
         */
        public Builder autoMax() {
            this.max = null;
            return this;
        }

        /**
         * Sets the bar width (default: 1).
         *
         * @param barWidth the bar width
         * @return this builder
         */
        public Builder barWidth(int barWidth) {
            this.barWidth = Math.max(1, barWidth);
            return this;
        }

        /**
         * Sets the gap between bars in a group (default: 1).
         *
         * @param barGap the gap between bars
         * @return this builder
         */
        public Builder barGap(int barGap) {
            this.barGap = Math.max(0, barGap);
            return this;
        }

        /**
         * Sets the gap between groups (default: 1).
         *
         * @param groupGap the gap between groups
         * @return this builder
         */
        public Builder groupGap(int groupGap) {
            this.groupGap = Math.max(0, groupGap);
            return this;
        }

        /**
         * Sets the chart direction (default: VERTICAL).
         *
         * @param direction the chart direction
         * @return this builder
         */
        public Builder direction(Direction direction) {
            this.direction = direction != null ? direction : Direction.VERTICAL;
            return this;
        }

        /**
         * Sets the overall chart style.
         *
         * @param style the chart style
         * @return this builder
         */
        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        /**
         * Sets the default bar style.
         *
         * @param barStyle the bar style
         * @return this builder
         */
        public Builder barStyle(Style barStyle) {
            this.barStyle = barStyle;
            return this;
        }

        /**
         * Sets the value display style.
         *
         * @param valueStyle the value style
         * @return this builder
         */
        public Builder valueStyle(Style valueStyle) {
            this.valueStyle = valueStyle;
            return this;
        }

        /**
         * Sets the label style.
         *
         * @param labelStyle the label style
         * @return this builder
         */
        public Builder labelStyle(Style labelStyle) {
            this.labelStyle = labelStyle;
            return this;
        }

        /**
         * Wraps the chart in a block.
         *
         * @param block the surrounding block
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
         * Sets the property resolver for style-aware properties.
         * <p>
         * When set, properties like {@code background}, {@code bar-color},
         * {@code value-color}, and {@code label-color} will be resolved
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
         * Sets the background color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the background color
         * @return this builder
         */
        public Builder background(Color color) {
            this.background = color;
            return this;
        }

        /**
         * Sets the default bar color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the bar color
         * @return this builder
         */
        public Builder barColor(Color color) {
            this.barColor = color;
            return this;
        }

        /**
         * Sets the value label color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the value color
         * @return this builder
         */
        public Builder valueColor(Color color) {
            this.valueColor = color;
            return this;
        }

        /**
         * Sets the bar label color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the label color
         * @return this builder
         */
        public Builder labelColor(Color color) {
            this.labelColor = color;
            return this;
        }

        /**
         * Builds the bar chart.
         *
         * @return the built bar chart
         */
        public BarChart build() {
            return new BarChart(this);
        }

        // Resolution helpers
        private Color resolveBackground() {
            return styleResolver.resolve(StandardProperties.BACKGROUND, background);
        }

        private Color resolveBarColor() {
            return styleResolver.resolve(BAR_COLOR, barColor);
        }

        private Color resolveValueColor() {
            return styleResolver.resolve(VALUE_COLOR, valueColor);
        }

        private Color resolveLabelColor() {
            return styleResolver.resolve(LABEL_COLOR, labelColor);
        }
    }
}

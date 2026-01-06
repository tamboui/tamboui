/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.gauge;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyKey;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.style.StandardPropertyKeys;
import dev.tamboui.style.Style;
import dev.tamboui.style.StyledProperty;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widget.Widget;

/**
 * A progress indicator that renders as a horizontal line.
 * <p>
 * Unlike {@link Gauge}, LineGauge renders on a single line and uses line-drawing
 * characters to show progress. It's more compact and suitable for status bars.
 *
 * <pre>{@code
 * LineGauge gauge = LineGauge.builder()
 *     .ratio(0.75)
 *     .label("Progress: ")
 *     .lineSet(LineGauge.THICK)
 *     .filledStyle(Style.EMPTY.fg(Color.GREEN))
 *     .build();
 * }</pre>
 */
public final class LineGauge implements Widget {

    /**
     * Line character set using thin lines.
     */
    public static final LineSet NORMAL = new LineSet("─", "━");

    /**
     * Line character set using thick lines.
     */
    public static final LineSet THICK = new LineSet("━", "━");

    /**
     * Line character set using double lines.
     */
    public static final LineSet DOUBLE = new LineSet("═", "═");

    /**
     * Property key for the filled portion color.
     * <p>
     * CSS property name: {@code filled-color}
     */
    public static final PropertyKey<Color> FILLED_COLOR =
            PropertyKey.of("filled-color", ColorConverter.INSTANCE);

    /**
     * Property key for the unfilled portion color.
     * <p>
     * CSS property name: {@code unfilled-color}
     */
    public static final PropertyKey<Color> UNFILLED_COLOR =
            PropertyKey.of("unfilled-color", ColorConverter.INSTANCE);

    private final double ratio;
    private final Line label;
    private final Style style;
    private final Style filledStyle;
    private final Style unfilledStyle;
    private final LineSet lineSet;

    private LineGauge(Builder builder) {
        this.ratio = builder.ratio;
        this.label = builder.label;
        this.lineSet = builder.lineSet;

        // Resolve style-aware properties
        Color resolvedBg = builder.background.resolve();
        Color resolvedFilledColor = builder.filledColor.resolve();
        Color resolvedUnfilledColor = builder.unfilledColor.resolve();

        Style baseStyle = builder.style;
        if (resolvedBg != null) {
            baseStyle = baseStyle.bg(resolvedBg);
        }
        this.style = baseStyle;

        Style baseFilledStyle = builder.filledStyle;
        if (resolvedFilledColor != null) {
            baseFilledStyle = baseFilledStyle.fg(resolvedFilledColor);
        }
        this.filledStyle = baseFilledStyle;

        Style baseUnfilledStyle = builder.unfilledStyle;
        if (resolvedUnfilledColor != null) {
            baseUnfilledStyle = baseUnfilledStyle.fg(resolvedUnfilledColor);
        }
        this.unfilledStyle = baseUnfilledStyle;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a line gauge with the given percentage (0-100).
     */
    public static LineGauge percent(int percent) {
        return builder().percent(percent).build();
    }

    /**
     * Creates a line gauge with the given ratio (0.0-1.0).
     */
    public static LineGauge ratio(double ratio) {
        return builder().ratio(ratio).build();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty() || area.height() < 1) {
            return;
        }

        // Apply background style
        buffer.setStyle(area, style);

        int y = area.top();
        int startX = area.left();

        // Render label if present
        if (label != null) {
            startX = buffer.setLine(startX, y, label);
        }

        // Calculate gauge width (remaining space)
        int gaugeWidth = area.right() - startX;
        if (gaugeWidth <= 0) {
            return;
        }

        // Calculate filled width
        int filledWidth = (int) (gaugeWidth * ratio);

        // Render filled portion
        for (int x = startX; x < startX + filledWidth; x++) {
            buffer.set(x, y, new Cell(lineSet.filled(), filledStyle));
        }

        // Render unfilled portion
        for (int x = startX + filledWidth; x < area.right(); x++) {
            buffer.set(x, y, new Cell(lineSet.unfilled(), unfilledStyle));
        }
    }

    /**
     * Defines the characters used for filled and unfilled portions of the line gauge.
     */
    public static final class LineSet {
        private final String unfilled;
        private final String filled;

        public LineSet(String unfilled, String filled) {
            if (unfilled == null || unfilled.isEmpty()) {
                throw new IllegalArgumentException("Unfilled character cannot be null or empty");
            }
            if (filled == null || filled.isEmpty()) {
                throw new IllegalArgumentException("Filled character cannot be null or empty");
            }
            this.unfilled = unfilled;
            this.filled = filled;
        }

        public String unfilled() {
            return unfilled;
        }

        public String filled() {
            return filled;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LineSet)) {
                return false;
            }
            LineSet lineSet = (LineSet) o;
            return unfilled.equals(lineSet.unfilled) && filled.equals(lineSet.filled);
        }

        @Override
        public int hashCode() {
            int result = unfilled.hashCode();
            result = 31 * result + filled.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return String.format("LineSet[unfilled=%s, filled=%s]", unfilled, filled);
        }
    }

    public static final class Builder {
        private double ratio = 0.0;
        private Line label;
        private Style style = Style.EMPTY;
        private Style filledStyle = Style.EMPTY;
        private Style unfilledStyle = Style.EMPTY;
        private LineSet lineSet = NORMAL;
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();

        // Style-aware properties bound to this builder's resolver
        private final StyledProperty<Color> background =
                StyledProperty.of(StandardPropertyKeys.BACKGROUND, null, () -> styleResolver);
        private final StyledProperty<Color> filledColor =
                StyledProperty.of(FILLED_COLOR, null, () -> styleResolver);
        private final StyledProperty<Color> unfilledColor =
                StyledProperty.of(UNFILLED_COLOR, null, () -> styleResolver);

        private Builder() {}

        /**
         * Sets the progress as a percentage (0-100).
         *
         * @throws IllegalArgumentException if percent is not in range 0-100
         */
        public Builder percent(int percent) {
            if (percent < 0 || percent > 100) {
                throw new IllegalArgumentException("Percent must be between 0 and 100, got: " + percent);
            }
            this.ratio = percent / 100.0;
            return this;
        }

        /**
         * Sets the progress as a ratio (0.0-1.0).
         *
         * @throws IllegalArgumentException if ratio is not in range 0.0-1.0
         */
        public Builder ratio(double ratio) {
            if (ratio < 0.0 || ratio > 1.0) {
                throw new IllegalArgumentException("Ratio must be between 0.0 and 1.0, got: " + ratio);
            }
            this.ratio = ratio;
            return this;
        }

        /**
         * Sets the label displayed before the gauge line.
         */
        public Builder label(String label) {
            this.label = Line.from(label);
            return this;
        }

        /**
         * Sets the label displayed before the gauge line.
         */
        public Builder label(Line label) {
            this.label = label;
            return this;
        }

        /**
         * Sets the label displayed before the gauge line.
         */
        public Builder label(Span span) {
            this.label = Line.from(span);
            return this;
        }

        /**
         * Sets the overall style for the widget.
         */
        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        /**
         * Sets the style for the filled portion of the gauge.
         */
        public Builder filledStyle(Style filledStyle) {
            this.filledStyle = filledStyle;
            return this;
        }

        /**
         * Sets the style for the unfilled portion of the gauge.
         */
        public Builder unfilledStyle(Style unfilledStyle) {
            this.unfilledStyle = unfilledStyle;
            return this;
        }

        /**
         * Sets the line character set to use.
         */
        public Builder lineSet(LineSet lineSet) {
            this.lineSet = lineSet;
            return this;
        }

        /**
         * Sets the property resolver for style-aware properties.
         * <p>
         * When set, properties like {@code filled-color}, {@code unfilled-color},
         * and {@code background} will be resolved if not set programmatically.
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
            this.background.set(color);
            return this;
        }

        /**
         * Sets the filled portion color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the filled color
         * @return this builder
         */
        public Builder filledColor(Color color) {
            this.filledColor.set(color);
            return this;
        }

        /**
         * Sets the unfilled portion color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the unfilled color
         * @return this builder
         */
        public Builder unfilledColor(Color color) {
            this.unfilledColor.set(color);
            return this;
        }

        public LineGauge build() {
            return new LineGauge(this);
        }
    }
}

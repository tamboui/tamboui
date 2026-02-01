/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.gauge;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.style.StandardProperties;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;

/**
 * A progress bar widget that renders a bar filled according to the progress value.
 * <p>
 * Progress can be set as a percentage (0-100) or as a ratio (0.0-1.0).
 * The gauge can optionally display a label centered in the bar.
 * <p>
 * When unicode mode is enabled, the widget uses block characters for smoother
 * visual progression with 8 extra fractional parts per cell.
 *
 * <pre>{@code
 * Gauge gauge = Gauge.builder()
 *     .percent(75)
 *     .label("75%")
 *     .gaugeStyle(Style.EMPTY.fg(Color.GREEN))
 *     .build();
 * }</pre>
 */
public final class Gauge implements Widget {

    // Unicode block characters for sub-cell precision (1/8 increments)
    private static final String[] UNICODE_BLOCKS = {
        " ",  // 0/8 - empty
        "▏",  // 1/8
        "▎",  // 2/8
        "▍",  // 3/8
        "▌",  // 4/8
        "▋",  // 5/8
        "▊",  // 6/8
        "▉",  // 7/8
        "█"   // 8/8 - full
    };

    /**
     * Property key for the gauge (filled portion) color.
     * <p>
     * CSS property name: {@code gauge-color}
     */
    public static final PropertyDefinition<Color> GAUGE_COLOR =
            PropertyDefinition.of("gauge-color", ColorConverter.INSTANCE);

    static {
        PropertyRegistry.register(GAUGE_COLOR);
    }

    private final double ratio;
    private final Line label;
    private final Block block;
    private final Style style;
    private final Style gaugeStyle;
    private final boolean useUnicode;

    private Gauge(Builder builder) {
        this.ratio = builder.ratio;
        this.label = builder.label;
        this.block = builder.block;
        this.useUnicode = builder.useUnicode;

        // Resolve style-aware properties
        Color resolvedBg = builder.resolveBackground();
        Color resolvedGaugeColor = builder.resolveGaugeColor();

        Style baseStyle = builder.style;
        if (resolvedBg != null) {
            baseStyle = baseStyle.bg(resolvedBg);
        }
        this.style = baseStyle;

        Style baseGaugeStyle = builder.gaugeStyle;
        if (resolvedGaugeColor != null) {
            baseGaugeStyle = baseGaugeStyle.fg(resolvedGaugeColor);
        }
        this.gaugeStyle = baseGaugeStyle;
    }

    /**
     * Creates a new gauge builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a gauge with the given percentage (0-100).
     *
     * @param percent the progress percentage (0-100)
     * @return a new gauge
     */
    public static Gauge percent(int percent) {
        return builder().percent(percent).build();
    }

    /**
     * Creates a gauge with the given ratio (0.0-1.0).
     *
     * @param ratio the progress ratio (0.0-1.0)
     * @return a new gauge
     */
    public static Gauge ratio(double ratio) {
        return builder().ratio(ratio).build();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty()) {
            return;
        }

        // Apply background style
        buffer.setStyle(area, style);

        // Render block if present
        Rect gaugeArea = area;
        if (block != null) {
            block.render(area, buffer);
            gaugeArea = block.inner(area);
        }

        if (gaugeArea.isEmpty()) {
            return;
        }

        // Calculate filled width
        int totalWidth = gaugeArea.width();
        double filledWidth = totalWidth * ratio;
        int fullCells = (int) filledWidth;

        // Render filled portion
        for (int x = gaugeArea.left(); x < gaugeArea.left() + fullCells && x < gaugeArea.right(); x++) {
            for (int y = gaugeArea.top(); y < gaugeArea.bottom(); y++) {
                buffer.set(x, y, new Cell(UNICODE_BLOCKS[8], gaugeStyle)); // Full block
            }
        }

        // Render partial cell if unicode is enabled
        if (useUnicode && fullCells < totalWidth) {
            double fractional = filledWidth - fullCells;
            int blockIndex = (int) (fractional * 8);
            if (blockIndex > 0) {
                int x = gaugeArea.left() + fullCells;
                for (int y = gaugeArea.top(); y < gaugeArea.bottom(); y++) {
                    buffer.set(x, y, new Cell(UNICODE_BLOCKS[blockIndex], gaugeStyle));
                }
            }
        }

        // Render label centered
        Line labelLine;
        if (label != null) {
            labelLine = label;
        } else {
            int percent = (int) (ratio * 100);
            labelLine = Line.from(percent + "%");
        }

        int labelWidth = labelLine.width();
        int labelX = gaugeArea.left() + (totalWidth - labelWidth) / 2;
        int labelY = gaugeArea.top() + (gaugeArea.height() - 1) / 2;

        // Render label with appropriate style based on position
        int col = labelX;
        for (Span span : labelLine.spans()) {
            String content = span.content();
            for (int i = 0; i < content.length(); i++) {
                if (col >= gaugeArea.left() && col < gaugeArea.right()) {
                    String ch = String.valueOf(content.charAt(i));
                    // Use inverted style if over filled portion
                    Style cellStyle;
                    if (col < gaugeArea.left() + fullCells) {
                        // Over filled part - use style with inverted colors for visibility
                        cellStyle = gaugeStyle.patch(span.style());
                    } else {
                        cellStyle = style.patch(span.style());
                    }
                    buffer.set(col, labelY, new Cell(ch, cellStyle));
                }
                col++;
            }
        }
    }

    /**
     * Builder for {@link Gauge}.
     */
    public static final class Builder {
        private double ratio = 0.0;
        private Line label;
        private Block block;
        private Style style = Style.EMPTY;
        private Style gaugeStyle = Style.EMPTY;
        private boolean useUnicode = true;
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();

        // Style-aware properties (resolved via styleResolver in build())
        private Color background;
        private Color gaugeColor;

        private Builder() {}

        /**
         * Sets the progress as a percentage (0-100).
         *
         * @param percent the progress percentage (0-100)
         * @return this builder
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
         * @param ratio the progress ratio (0.0-1.0)
         * @return this builder
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
         * Sets the label displayed centered in the gauge.
         * If not set, defaults to showing the percentage.
         *
         * @param label the label text
         * @return this builder
         */
        public Builder label(String label) {
            this.label = Line.from(label);
            return this;
        }

        /**
         * Sets the label displayed centered in the gauge.
         *
         * @param label the label as a styled line
         * @return this builder
         */
        public Builder label(Line label) {
            this.label = label;
            return this;
        }

        /**
         * Sets the label displayed centered in the gauge.
         *
         * @param span the label as a styled span
         * @return this builder
         */
        public Builder label(Span span) {
            this.label = Line.from(span);
            return this;
        }

        /**
         * Wraps the gauge in a block container.
         *
         * @param block the block container
         * @return this builder
         */
        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Sets the style for the widget background (not the filled bar).
         *
         * @param style the background style
         * @return this builder
         */
        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        /**
         * Sets the style for the filled portion of the gauge.
         *
         * @param gaugeStyle the gauge style
         * @return this builder
         */
        public Builder gaugeStyle(Style gaugeStyle) {
            this.gaugeStyle = gaugeStyle;
            return this;
        }

        /**
         * Enables or disables unicode block characters for smoother rendering.
         * When enabled (default), uses 8 fractional parts per cell.
         *
         * @param useUnicode {@code true} to enable unicode rendering
         * @return this builder
         */
        public Builder useUnicode(boolean useUnicode) {
            this.useUnicode = useUnicode;
            return this;
        }

        /**
         * Sets the property resolver for style-aware properties.
         * <p>
         * When set, properties like {@code gauge-color} and {@code background}
         * will be resolved if not set programmatically.
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
         * Sets the gauge (filled portion) color programmatically.
         * <p>
         * This takes precedence over values from the style resolver.
         *
         * @param color the gauge color
         * @return this builder
         */
        public Builder gaugeColor(Color color) {
            this.gaugeColor = color;
            return this;
        }

        /**
         * Builds the gauge.
         *
         * @return the new gauge
         */
        public Gauge build() {
            return new Gauge(this);
        }

        // Resolution helpers
        private Color resolveBackground() {
            return styleResolver.resolve(StandardProperties.BACKGROUND, background);
        }

        private Color resolveGaugeColor() {
            return styleResolver.resolve(GAUGE_COLOR, gaugeColor);
        }
    }
}

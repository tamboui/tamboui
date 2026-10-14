/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.gauge;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
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
import dev.tamboui.text.GraphemeClusters;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.sparkline.Sparkline;

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

    /**
     * CSS property for the gradient low stop color ({@code gradient-low}).
     */
    public static final PropertyDefinition<Color> GRADIENT_LOW = Sparkline.GRADIENT_LOW;

    /**
     * CSS property for the gradient mid stop color ({@code gradient-mid}).
     */
    public static final PropertyDefinition<Color> GRADIENT_MID = Sparkline.GRADIENT_MID;

    /**
     * CSS property for the gradient high stop color ({@code gradient-high}).
     */
    public static final PropertyDefinition<Color> GRADIENT_HIGH = Sparkline.GRADIENT_HIGH;

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
        PropertyRegistry.registerAll(GAUGE_COLOR, GRADIENT_LOW, GRADIENT_MID, GRADIENT_HIGH);
    }

    private final double ratio;
    private final Line label;
    private final Block block;
    private final Style style;
    private final Style gaugeStyle;
    private final boolean useUnicode;
    private final ColorGradient gradient;

    private Gauge(Builder builder) {
        this.ratio = builder.ratio;
        this.label = builder.label;
        this.block = builder.block;
        this.useUnicode = builder.useUnicode;

        ColorGradient resolvedGradient = builder.gradient;
        if (resolvedGradient == null) {
            resolvedGradient = builder.resolveGradient();
        }
        this.gradient = resolvedGradient;

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
     * Returns the color gradient, if configured.
     *
     * @return the color gradient, or empty
     */
    public Optional<ColorGradient> gradient() {
        return Optional.ofNullable(gradient);
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
            Style cellStyle = gaugeStyle;
            if (gradient != null) {
                double frac = totalWidth > 1 ? (double) (x - gaugeArea.left()) / (totalWidth - 1) : 0.0;
                cellStyle = cellStyle.fg(gradient.at(frac));
            }
            for (int y = gaugeArea.top(); y < gaugeArea.bottom(); y++) {
                buffer.set(x, y, new Cell(UNICODE_BLOCKS[8], cellStyle)); // Full block
            }
        }

        // Render partial cell if unicode is enabled
        if (useUnicode && fullCells < totalWidth) {
            double fractional = filledWidth - fullCells;
            int blockIndex = (int) (fractional * 8);
            if (blockIndex > 0) {
                int x = gaugeArea.left() + fullCells;
                Style cellStyle = gaugeStyle;
                if (gradient != null) {
                    double frac = totalWidth > 1 ? (double) (x - gaugeArea.left()) / (totalWidth - 1) : 0.0;
                    cellStyle = cellStyle.fg(gradient.at(frac));
                }
                for (int y = gaugeArea.top(); y < gaugeArea.bottom(); y++) {
                    buffer.set(x, y, new Cell(UNICODE_BLOCKS[blockIndex], cellStyle));
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

        // Blank the cells under the label in the style of the bar beneath them (the gauge style
        // over the filled portion, the widget style elsewhere); each span's style is patched onto it
        int labelLeft = Math.max(labelX, gaugeArea.left());
        int labelRight = Math.min(labelX + labelWidth, gaugeArea.right());
        for (int x = labelLeft; x < labelRight; x++) {
            Style barStyle = x < gaugeArea.left() + fullCells ? gaugeStyle : style;
            buffer.set(x, labelY, new Cell(" ", barStyle));
        }

        // Write the label a character at a time, as wide as it is on screen: an emoji or a CJK
        // character takes two cells. A character that the gauge's edges would cut in half, or a
        // zero-width one on its own, is left out.
        int col = labelX;
        for (Span span : labelLine.spans()) {
            String content = span.content();
            int i = 0;
            while (i < content.length()) {
                int end = GraphemeClusters.clusterEnd(content, i);
                String character = content.substring(i, end);
                int width = CharWidth.of(character);
                if (width > 0 && col >= gaugeArea.left() && col + width <= gaugeArea.right()) {
                    buffer.setString(col, labelY, character, span.style());
                }
                col += width;
                i = end;
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
        private ColorGradient gradient;
        private Color gradientLow;
        private Color gradientMid;
        private Color gradientHigh;

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
         * Sets color gradient stops to map the filled bar across its width.
         *
         * @param stops the color gradient stops
         * @return this builder
         */
        public Builder gradient(Color... stops) {
            this.gradient = (stops != null && stops.length > 0) ? ColorGradient.of(stops) : null;
            return this;
        }

        /**
         * Sets a color gradient to map the filled bar across its width.
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

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.wavetext;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.DoubleConverter;
import dev.tamboui.style.IntegerConverter;
import dev.tamboui.style.PropertyKey;
import dev.tamboui.style.StandardPropertyKeys;
import dev.tamboui.style.Style;
import dev.tamboui.style.StyledProperty;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.widget.StatefulWidget;

/**
 * A widget that renders text with an animated wave brightness effect.
 * <p>
 * By default, the wave creates a dark "shadow" that moves through otherwise
 * bright text. This can be inverted to have a bright peak moving through
 * dim text.
 *
 * <pre>{@code
 * // Dark shadow moving through bright text (default)
 * WaveText wave = WaveText.builder()
 *     .text("Processing...")
 *     .color(Color.CYAN)
 *     .build();
 *
 * // Bright peak moving through dim text (inverted)
 * WaveText wave = WaveText.builder()
 *     .text("Loading...")
 *     .color(Color.YELLOW)
 *     .inverted(true)
 *     .build();
 *
 * // Back-and-forth oscillation instead of looping
 * WaveText wave = WaveText.builder()
 *     .text("Thinking...")
 *     .mode(Mode.OSCILLATE)
 *     .build();
 * }</pre>
 *
 * <h2>Configuration Options</h2>
 * <ul>
 *   <li><b>color</b> - Base color of the text</li>
 *   <li><b>dimFactor</b> - Brightness of the dim portion (0.0-1.0, default 0.3)</li>
 *   <li><b>peakWidth</b> - Width of the wave peak in characters (default 3)</li>
 *   <li><b>peakCount</b> - Number of peaks in the wave (default 1)</li>
 *   <li><b>speed</b> - Animation speed multiplier (default 1.0)</li>
 *   <li><b>mode</b> - LOOP (continuous) or OSCILLATE (back-and-forth)</li>
 *   <li><b>inverted</b> - If true, bright peak on dim text; if false, dark shadow on bright text</li>
 * </ul>
 */
public final class WaveText implements StatefulWidget<WaveTextState> {

    /**
     * Animation mode for the wave.
     */
    public enum Mode {
        /** Wave loops continuously in one direction. */
        LOOP,
        /** Wave oscillates back and forth. */
        OSCILLATE
    }

    // ═══════════════════════════════════════════════════════════════
    // Property Keys for styling
    // ═══════════════════════════════════════════════════════════════

    /**
     * The {@code wave-dim-factor} property for the dim factor (0.0-1.0).
     */
    public static final PropertyKey<Double> DIM_FACTOR =
            PropertyKey.of("wave-dim-factor", DoubleConverter.INSTANCE);

    /**
     * The {@code wave-peak-width} property for the peak width in characters.
     */
    public static final PropertyKey<Integer> PEAK_WIDTH =
            PropertyKey.of("wave-peak-width", IntegerConverter.INSTANCE);

    /**
     * The {@code wave-peak-count} property for the number of peaks.
     */
    public static final PropertyKey<Integer> PEAK_COUNT =
            PropertyKey.of("wave-peak-count", IntegerConverter.INSTANCE);

    /**
     * The {@code wave-speed} property for animation speed multiplier.
     */
    public static final PropertyKey<Double> SPEED =
            PropertyKey.of("wave-speed", DoubleConverter.INSTANCE);

    private final String text;
    private final Color color;
    private final double dimFactor;
    private final int peakWidth;
    private final int peakCount;
    private final double speed;
    private final Mode mode;
    private final boolean inverted;
    private final Style baseStyle;

    private WaveText(Builder builder) {
        this.text = builder.text;
        this.color = builder.colorProp.resolve();
        this.dimFactor = builder.dimFactorProp.resolve();
        this.peakWidth = builder.peakWidthProp.resolve();
        this.peakCount = builder.peakCountProp.resolve();
        this.speed = builder.speedProp.resolve();
        this.mode = builder.mode;
        this.inverted = builder.inverted;
        this.baseStyle = builder.baseStyle;
    }

    /**
     * Creates a new builder for WaveText.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a simple wave text with default settings.
     *
     * @param text the text to display
     * @param color the base color
     * @return a new WaveText
     */
    public static WaveText of(String text, Color color) {
        return builder().text(text).color(color).build();
    }

    @Override
    public void render(Rect area, Buffer buffer, WaveTextState state) {
        if (area.isEmpty() || text == null || text.isEmpty()) {
            return;
        }

        int textLen = text.length();
        long tick = state.tick();

        // Render each character with appropriate brightness
        int x = area.x();
        int y = area.y();
        int maxChars = Math.min(textLen, area.width());

        for (int i = 0; i < maxChars; i++) {
            char c = text.charAt(i);

            // Calculate brightness for this character
            double brightness = calculateBrightness(i, textLen, tick);

            // Apply brightness to color
            Color charColor = applyBrightness(color, brightness);
            Style charStyle = baseStyle.fg(charColor);

            buffer.setString(x + i, y, String.valueOf(c), charStyle);
        }
    }

    /**
     * Calculates the brightness for a character at the given position.
     *
     * @param charIndex the character index
     * @param textLen   the total text length
     * @param tick      the current animation tick
     * @return the brightness (0.0-1.0)
     */
    private double calculateBrightness(int charIndex, int textLen, long tick) {
        // Calculate wave position(s) based on mode
        double cycleLength = textLen + peakWidth;
        double rawPosition = tick * speed * 0.2;

        double minDistance = Double.MAX_VALUE;

        // Calculate distance to nearest peak
        for (int p = 0; p < peakCount; p++) {
            // Offset each peak evenly across the text
            double peakOffset = (double) p * textLen / peakCount;
            double peakPosition;

            if (mode == Mode.OSCILLATE) {
                // Back-and-forth: use triangle wave
                double phase = (rawPosition + peakOffset) % (cycleLength * 2);
                if (phase < cycleLength) {
                    peakPosition = phase - peakWidth / 2.0;
                } else {
                    peakPosition = (cycleLength * 2 - phase) - peakWidth / 2.0;
                }
            } else {
                // Loop: continuous movement
                peakPosition = ((rawPosition + peakOffset) % cycleLength) - peakWidth / 2.0;
            }

            double distance = Math.abs(charIndex - peakPosition);
            minDistance = Math.min(minDistance, distance);
        }

        // Calculate brightness based on distance from nearest peak
        double peakBrightness;
        if (minDistance <= peakWidth / 2.0) {
            peakBrightness = 0.0; // At peak center
        } else if (minDistance <= peakWidth) {
            // Smooth falloff from peak
            peakBrightness = (minDistance - peakWidth / 2.0) / (peakWidth / 2.0);
        } else {
            peakBrightness = 1.0; // Far from peak
        }

        // Apply dimFactor and inversion
        // peakBrightness is 0 at peak center, 1 far from peak
        // Default (not inverted): dark peak (dimFactor) on bright text (1.0)
        // Inverted: bright peak (1.0) on dim text (dimFactor)
        if (inverted) {
            // Bright peak on dim background: peak=bright (1.0), far=dim (dimFactor)
            return dimFactor + (1.0 - peakBrightness) * (1.0 - dimFactor);
        } else {
            // Dark peak on bright background: peak=dim (dimFactor), far=bright (1.0)
            return dimFactor + peakBrightness * (1.0 - dimFactor);
        }
    }

    /**
     * Applies a brightness factor to a color.
     *
     * @param color the color to modify
     * @param brightness the brightness factor (0.0 = black, 1.0 = original)
     * @return the modified color
     */
    private Color applyBrightness(Color color, double brightness) {
        Color.Rgb rgb = color.toRgb();
        int r = (int) (rgb.r() * brightness);
        int g = (int) (rgb.g() * brightness);
        int b = (int) (rgb.b() * brightness);
        return Color.rgb(r, g, b);
    }

    /**
     * Returns the text being displayed.
     *
     * @return the text
     */
    public String text() {
        return text;
    }

    /**
     * Returns the base color.
     *
     * @return the color
     */
    public Color color() {
        return color;
    }

    /**
     * Builder for {@link WaveText}.
     */
    public static final class Builder {
        private String text = "";
        private Mode mode = Mode.LOOP;
        private boolean inverted = false;
        private Style baseStyle = Style.EMPTY;
        private StylePropertyResolver styleResolver = StylePropertyResolver.empty();

        // Style-aware properties bound to this builder's resolver
        private final StyledProperty<Color> colorProp =
                StyledProperty.of(StandardPropertyKeys.COLOR, Color.WHITE, () -> styleResolver);
        private final StyledProperty<Double> dimFactorProp =
                StyledProperty.of(DIM_FACTOR, 0.3, () -> styleResolver);
        private final StyledProperty<Integer> peakWidthProp =
                StyledProperty.of(PEAK_WIDTH, 3, () -> styleResolver);
        private final StyledProperty<Integer> peakCountProp =
                StyledProperty.of(PEAK_COUNT, 1, () -> styleResolver);
        private final StyledProperty<Double> speedProp =
                StyledProperty.of(SPEED, 1.0, () -> styleResolver);

        private Builder() {}

        /**
         * Sets the text to display.
         *
         * @param text the text
         * @return this builder
         */
        public Builder text(String text) {
            this.text = text != null ? text : "";
            return this;
        }

        /**
         * Sets the base color for the text.
         *
         * @param color the base color
         * @return this builder
         */
        public Builder color(Color color) {
            this.colorProp.set(color);
            return this;
        }

        /**
         * Sets the dim factor for the wave effect.
         * <p>
         * A value of 0.0 means completely black, 1.0 means no dimming.
         * Default is 0.3.
         *
         * @param dimFactor the dim factor (0.0-1.0)
         * @return this builder
         */
        public Builder dimFactor(double dimFactor) {
            this.dimFactorProp.set(Math.max(0.0, Math.min(1.0, dimFactor)));
            return this;
        }

        /**
         * Sets the width of the wave peak in characters.
         * <p>
         * Larger values create a wider wave area. Default is 3.
         *
         * @param peakWidth the peak width
         * @return this builder
         */
        public Builder peakWidth(int peakWidth) {
            this.peakWidthProp.set(Math.max(1, peakWidth));
            return this;
        }

        /**
         * Sets the number of peaks in the wave.
         * <p>
         * Multiple peaks create a more dynamic effect. Default is 1.
         *
         * @param peakCount the number of peaks
         * @return this builder
         */
        public Builder peakCount(int peakCount) {
            this.peakCountProp.set(Math.max(1, peakCount));
            return this;
        }

        /**
         * Sets the animation speed multiplier.
         * <p>
         * Values greater than 1.0 speed up the animation,
         * values less than 1.0 slow it down. Default is 1.0.
         *
         * @param speed the speed multiplier
         * @return this builder
         */
        public Builder speed(double speed) {
            this.speedProp.set(Math.max(0.1, speed));
            return this;
        }

        /**
         * Sets the property resolver for style-aware properties.
         * <p>
         * When set, properties like {@code color}, {@code wave-dim-factor},
         * {@code wave-peak-width}, {@code wave-peak-count}, and {@code wave-speed}
         * will be resolved from the styling system if not set programmatically.
         *
         * @param resolver the property resolver
         * @return this builder
         */
        public Builder styleResolver(StylePropertyResolver resolver) {
            this.styleResolver = resolver != null ? resolver : StylePropertyResolver.empty();
            return this;
        }

        /**
         * Sets the animation mode.
         * <p>
         * {@link Mode#LOOP} creates continuous movement in one direction.
         * {@link Mode#OSCILLATE} creates back-and-forth movement.
         * Default is LOOP.
         *
         * @param mode the animation mode
         * @return this builder
         */
        public Builder mode(Mode mode) {
            this.mode = mode != null ? mode : Mode.LOOP;
            return this;
        }

        /**
         * Sets the wave to oscillate back and forth.
         * <p>
         * Shorthand for {@code mode(Mode.OSCILLATE)}.
         *
         * @return this builder
         */
        public Builder oscillate() {
            this.mode = Mode.OSCILLATE;
            return this;
        }

        /**
         * Sets whether the effect is inverted.
         * <p>
         * When false (default), a dark shadow moves through bright text.
         * When true, a bright peak moves through dim text.
         *
         * @param inverted true to invert the effect
         * @return this builder
         */
        public Builder inverted(boolean inverted) {
            this.inverted = inverted;
            return this;
        }

        /**
         * Sets the base style (modifiers like bold, italic).
         * <p>
         * The foreground color will be overridden by the wave effect.
         *
         * @param style the base style
         * @return this builder
         */
        public Builder style(Style style) {
            this.baseStyle = style != null ? style : Style.EMPTY;
            return this;
        }

        /**
         * Builds the WaveText widget.
         *
         * @return a new WaveText
         */
        public WaveText build() {
            return new WaveText(this);
        }
    }
}

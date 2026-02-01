/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.spinner;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import dev.tamboui.widget.StatefulWidget;

/**
 * A widget that renders an animated spinner by cycling through frame characters.
 * <p>
 * The spinner displays one frame at a time, determined by the current tick
 * in the {@link SpinnerState}. Each tick advances to the next frame in the sequence.
 *
 * <pre>{@code
 * // Create a spinner with the DOTS style
 * Spinner spinner = Spinner.builder()
 *     .spinnerStyle(SpinnerStyle.DOTS)
 *     .build();
 *
 * // Create a spinner with custom frames
 * Spinner spinner = Spinner.builder()
 *     .frames("*", "+", "x", "+")
 *     .build();
 *
 * // Create a spinner with a custom frame set
 * Spinner spinner = Spinner.builder()
 *     .frameSet(SpinnerFrameSet.of("⠋", "⠙", "⠹", "⠸"))
 *     .build();
 * }</pre>
 *
 * @see SpinnerStyle
 * @see SpinnerFrameSet
 * @see SpinnerState
 */
public final class Spinner implements StatefulWidget<SpinnerState> {

    /**
     * The {@code spinner-style} property for selecting a predefined spinner style.
     * <p>
     * CSS values are derived from enum names by converting to lowercase and
     * replacing underscores with hyphens. For example:
     * <ul>
     *   <li>{@code dots} - braille dot pattern (default)</li>
     *   <li>{@code line} - classic -\|/</li>
     *   <li>{@code bouncing-bar} - bouncing bar [=== ]</li>
     *   <li>{@code gauge} - horizontal block fill</li>
     * </ul>
     * This property is NOT inheritable. Default: {@link SpinnerStyle#DOTS}
     */
    public static final PropertyDefinition<SpinnerStyle> SPINNER_STYLE =
            PropertyDefinition.builder("spinner-style", SpinnerStyleConverter.INSTANCE)
                    .defaultValue(SpinnerStyle.DOTS)
                    .build();

    /**
     * The {@code spinner-frames} property for custom spinner frame characters.
     * <p>
     * Format: variable number of quoted strings representing the animation frames.
     * <p>
     * Example: {@code spinner-frames: "-" "\\" "|" "/"}
     * <p>
     * This property overrides {@code spinner-style} when set.
     * This property is NOT inheritable.
     */
    public static final PropertyDefinition<SpinnerFrameSet> SPINNER_FRAMES =
            PropertyDefinition.of("spinner-frames", SpinnerFrameSetConverter.INSTANCE);

    static {
        PropertyRegistry.registerAll(SPINNER_STYLE, SPINNER_FRAMES);
    }

    private final SpinnerFrameSet frameSet;
    private final Style style;

    private Spinner(Builder builder) {
        this.frameSet = builder.frameSet != null ? builder.frameSet : SpinnerStyle.DOTS.frameSet();
        this.style = builder.style;
    }

    /**
     * Creates a new builder for Spinner.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a simple spinner with default DOTS style.
     *
     * @return a new Spinner
     */
    public static Spinner dots() {
        return builder().build();
    }

    @Override
    public void render(Rect area, Buffer buffer, SpinnerState state) {
        if (area.isEmpty() || frameSet.frameCount() == 0) {
            return;
        }

        String frame = frameSet.frame((int) state.tick());

        // Truncate frame if it exceeds the available width
        String truncated = CharWidth.substringByWidth(frame, area.width());
        buffer.setString(area.x(), area.y(), truncated, style);
    }

    /**
     * Returns the frame set used by this spinner.
     *
     * @return the frame set
     */
    public SpinnerFrameSet frameSet() {
        return frameSet;
    }

    /**
     * Returns the frames used by this spinner.
     *
     * @return the frame array
     */
    public String[] frames() {
        return frameSet.frames();
    }

    /**
     * Returns the maximum display width across all frames.
     *
     * @return the maximum frame width
     */
    public int maxFrameWidth() {
        int max = 0;
        for (String frame : frameSet.frames()) {
            max = Math.max(max, CharWidth.of(frame));
        }
        return max;
    }

    /**
     * Builder for {@link Spinner}.
     */
    public static final class Builder {
        private SpinnerFrameSet frameSet;
        private Style style = Style.EMPTY;

        private Builder() {
        }

        /**
         * Sets the spinner style (predefined frame set).
         *
         * @param spinnerStyle the spinner style
         * @return this builder
         */
        public Builder spinnerStyle(SpinnerStyle spinnerStyle) {
            if (spinnerStyle != null) {
                this.frameSet = spinnerStyle.frameSet();
            }
            return this;
        }

        /**
         * Sets the frame set for the spinner.
         *
         * @param frameSet the frame set
         * @return this builder
         */
        public Builder frameSet(SpinnerFrameSet frameSet) {
            this.frameSet = frameSet;
            return this;
        }

        /**
         * Sets custom frames for the spinner.
         * <p>
         * Custom frames override any previously set spinner style or frame set.
         *
         * @param frames the frame strings
         * @return this builder
         */
        public Builder frames(String... frames) {
            if (frames != null && frames.length > 0) {
                this.frameSet = SpinnerFrameSet.of(frames);
            }
            return this;
        }

        /**
         * Sets the style for the spinner text.
         *
         * @param style the style
         * @return this builder
         */
        public Builder style(Style style) {
            this.style = style != null ? style : Style.EMPTY;
            return this;
        }

        /**
         * Builds the Spinner widget.
         *
         * @return a new Spinner
         */
        public Spinner build() {
            return new Spinner(this);
        }
    }
}

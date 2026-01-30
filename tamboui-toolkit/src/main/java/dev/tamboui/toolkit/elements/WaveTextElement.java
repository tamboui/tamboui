/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.widgets.wavetext.WaveText;
import dev.tamboui.widgets.wavetext.WaveTextState;

/**
 * An element that displays text with an animated wave brightness effect.
 * <p>
 * By default, a dark "shadow" moves through otherwise bright text. This can be
 * inverted to have a bright peak moving through dim text.
 *
 * <pre>{@code
 * // Dark shadow on bright text (default)
 * waveText("Loading...", Color.CYAN)
 *
 * // With configuration
 * waveText("Processing...")
 *     .color(Color.YELLOW)
 *     .peakWidth(5)
 *     .speed(1.5)
 *     .oscillate()  // Back-and-forth instead of looping
 *
 * // Inverted: bright peak on dim text
 * waveText("Thinking...").inverted()
 * }</pre>
 *
 * @see WaveText
 */
public final class WaveTextElement extends StyledElement<WaveTextElement> {

    private String text;
    private Color color;
    private double dimFactor = 0.3;
    private int peakWidth = 3;
    private int peakCount = 1;
    private double speed = 1.0;
    private WaveText.Mode mode = WaveText.Mode.LOOP;
    private boolean inverted = false;
    private WaveTextState state;

    /**
     * Creates a wave text element with the given text.
     *
     * @param text
     *            the text to display
     */
    public WaveTextElement(String text) {
        this.text = text;
        this.state = new WaveTextState();
    }

    /**
     * Creates a wave text element with the given text and color.
     *
     * @param text
     *            the text to display
     * @param color
     *            the base color
     */
    public WaveTextElement(String text, Color color) {
        this.text = text;
        this.color = color;
        this.state = new WaveTextState();
    }

    /**
     * Sets the text to display.
     *
     * @param text
     *            the text
     * @return this element for chaining
     */
    public WaveTextElement text(String text) {
        this.text = text;
        return this;
    }

    /**
     * Sets the base color for the wave effect.
     *
     * @param color
     *            the color
     * @return this element for chaining
     */
    public WaveTextElement color(Color color) {
        this.color = color;
        return this;
    }

    /**
     * Sets the dim factor for non-peak characters.
     * <p>
     * A value of 0.0 means completely black, 1.0 means no dimming. Default is 0.3.
     *
     * @param dimFactor
     *            the dim factor (0.0-1.0)
     * @return this element for chaining
     */
    public WaveTextElement dimFactor(double dimFactor) {
        this.dimFactor = dimFactor;
        return this;
    }

    /**
     * Sets the width of the bright peak in characters.
     * <p>
     * Larger values create a wider bright area. Default is 3.
     *
     * @param peakWidth
     *            the peak width
     * @return this element for chaining
     */
    public WaveTextElement peakWidth(int peakWidth) {
        this.peakWidth = peakWidth;
        return this;
    }

    /**
     * Sets the animation speed multiplier.
     * <p>
     * Values greater than 1.0 speed up the animation, values less than 1.0 slow it
     * down. Default is 1.0.
     *
     * @param speed
     *            the speed multiplier
     * @return this element for chaining
     */
    public WaveTextElement speed(double speed) {
        this.speed = speed;
        return this;
    }

    /**
     * Sets the number of peaks in the wave.
     * <p>
     * Multiple peaks create a more dynamic effect. Default is 1.
     *
     * @param peakCount
     *            the number of peaks
     * @return this element for chaining
     */
    public WaveTextElement peakCount(int peakCount) {
        this.peakCount = peakCount;
        return this;
    }

    /**
     * Sets the animation mode.
     * <p>
     * {@link WaveText.Mode#LOOP} creates continuous movement in one direction.
     * {@link WaveText.Mode#OSCILLATE} creates back-and-forth movement. Default is
     * LOOP.
     *
     * @param mode
     *            the animation mode
     * @return this element for chaining
     */
    public WaveTextElement mode(WaveText.Mode mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Sets the wave to oscillate back and forth.
     * <p>
     * Shorthand for {@code mode(WaveText.Mode.OSCILLATE)}.
     *
     * @return this element for chaining
     */
    public WaveTextElement oscillate() {
        this.mode = WaveText.Mode.OSCILLATE;
        return this;
    }

    /**
     * Sets whether the effect is inverted.
     * <p>
     * When false (default), a dark shadow moves through bright text. When true, a
     * bright peak moves through dim text.
     *
     * @param inverted
     *            true to invert the effect
     * @return this element for chaining
     */
    public WaveTextElement inverted(boolean inverted) {
        this.inverted = inverted;
        return this;
    }

    /**
     * Sets the effect to inverted mode (bright peak on dim text).
     *
     * @return this element for chaining
     */
    public WaveTextElement inverted() {
        this.inverted = true;
        return this;
    }

    /**
     * Sets the state for animation.
     * <p>
     * If not set, an internal state is used and advanced automatically.
     *
     * @param state
     *            the wave text state
     * @return this element for chaining
     */
    public WaveTextElement state(WaveTextState state) {
        this.state = state;
        return this;
    }

    @Override
    public int preferredWidth() {
        return text != null ? text.length() : 0;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty() || text == null || text.isEmpty()) {
            return;
        }

        // Advance the animation state
        state.advance();

        // Get the CSS resolver for this element
        StylePropertyResolver resolver = context.resolveStyle(this)
                .map(r -> (StylePropertyResolver) r).orElse(StylePropertyResolver.empty());

        // Build the widget
        WaveText.Builder builder = WaveText.builder().text(text).styleResolver(resolver)
                .dimFactor(dimFactor).peakWidth(peakWidth).peakCount(peakCount).speed(speed)
                .mode(mode).inverted(inverted).style(context.currentStyle());

        // Only set color explicitly if it was programmatically specified
        if (color != null) {
            builder.color(color);
        }

        // Render
        frame.renderStatefulWidget(builder.build(), area, state);
    }
}

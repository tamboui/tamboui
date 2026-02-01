/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.CharWidth;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.widgets.spinner.Spinner;
import dev.tamboui.widgets.spinner.SpinnerFrameSet;
import dev.tamboui.widgets.spinner.SpinnerState;
import dev.tamboui.widgets.spinner.SpinnerStyle;

/**
 * An element that displays an animated spinner with an optional label.
 * <p>
 * The spinner cycles through frame characters on each render tick,
 * creating a loading animation effect.
 *
 * <pre>{@code
 * // Simple spinner
 * spinner()
 *
 * // Spinner with label
 * spinner("Loading...").cyan()
 *
 * // Spinner with specific style
 * spinner(SpinnerStyle.LINE, "Processing...")
 *
 * // Spinner with custom frames
 * spinner().frames("*", "+", "x", "+").label("Working")
 *
 * // Spinner with custom frame set
 * spinner().frameSet(SpinnerFrameSet.of("⠋", "⠙", "⠹", "⠸")).label("Working")
 * }</pre>
 *
 * <h2>CSS Properties</h2>
 * <p>
 * The spinner style and frames can be configured via CSS:
 * <ul>
 *   <li>{@code spinner-style} - Predefined spinner style (e.g., {@code dots}, {@code line},
 *       {@code bouncing-bar}, {@code gauge})</li>
 *   <li>{@code spinner-frames} - Custom animation frames as quoted strings
 *       (e.g., {@code spinner-frames: "-" "\\" "|" "/"})</li>
 * </ul>
 * <p>
 * Example CSS:
 * <pre>{@code
 * .my-spinner {
 *     spinner-style: line;
 *     color: cyan;
 * }
 *
 * .custom-spinner {
 *     spinner-frames: "⠋" "⠙" "⠹" "⠸" "⠼" "⠴" "⠦" "⠧";
 * }
 * }</pre>
 * <p>
 * Note: Programmatic styles set via {@link #spinnerStyle}, {@link #frameSet}, or
 * {@link #frames} take precedence over CSS styles.
 *
 * @see Spinner
 * @see SpinnerStyle
 * @see SpinnerFrameSet
 */
public final class SpinnerElement extends StyledElement<SpinnerElement> {

    private SpinnerStyle spinnerStyle = SpinnerStyle.DOTS;
    private SpinnerFrameSet customFrameSet;
    private String label;
    private SpinnerState state;

    /**
     * Creates a spinner element with the default DOTS style.
     */
    public SpinnerElement() {
        this.state = new SpinnerState();
    }

    /**
     * Creates a spinner element with the given style.
     *
     * @param spinnerStyle the spinner style
     */
    public SpinnerElement(SpinnerStyle spinnerStyle) {
        this.spinnerStyle = spinnerStyle != null ? spinnerStyle : SpinnerStyle.DOTS;
        this.state = new SpinnerState();
    }

    /**
     * Creates a spinner element with a label.
     *
     * @param label the label text
     */
    public SpinnerElement(String label) {
        this.label = label;
        this.state = new SpinnerState();
    }

    /**
     * Creates a spinner element with a style and label.
     *
     * @param spinnerStyle the spinner style
     * @param label the label text
     */
    public SpinnerElement(SpinnerStyle spinnerStyle, String label) {
        this.spinnerStyle = spinnerStyle != null ? spinnerStyle : SpinnerStyle.DOTS;
        this.label = label;
        this.state = new SpinnerState();
    }

    /**
     * Sets the spinner style.
     *
     * @param spinnerStyle the spinner style
     * @return this element for chaining
     */
    public SpinnerElement spinnerStyle(SpinnerStyle spinnerStyle) {
        this.spinnerStyle = spinnerStyle != null ? spinnerStyle : SpinnerStyle.DOTS;
        this.customFrameSet = null;
        return this;
    }

    /**
     * Sets a custom frame set for the spinner, overriding the style.
     * <p>
     * This allows full control over the spinner animation frames.
     *
     * @param frameSet the frame set
     * @return this element for chaining
     */
    public SpinnerElement frameSet(SpinnerFrameSet frameSet) {
        this.customFrameSet = frameSet;
        return this;
    }

    /**
     * Sets custom frames for the spinner, overriding the style.
     *
     * @param frames the frame strings
     * @return this element for chaining
     */
    public SpinnerElement frames(String... frames) {
        if (frames != null && frames.length > 0) {
            this.customFrameSet = SpinnerFrameSet.of(frames);
        }
        return this;
    }

    /**
     * Sets the label displayed next to the spinner.
     *
     * @param label the label text
     * @return this element for chaining
     */
    public SpinnerElement label(String label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the animation state.
     * <p>
     * If not set, an internal state is used and advanced automatically.
     *
     * @param state the spinner state
     * @return this element for chaining
     */
    public SpinnerElement state(SpinnerState state) {
        this.state = state;
        return this;
    }

    @Override
    public int preferredWidth() {
        int width = computeMaxFrameWidth();
        if (label != null && !label.isEmpty()) {
            width += 1 + CharWidth.of(label); // space + label
        }
        return width;
    }

    @Override
    public int preferredHeight() {
        return 1;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        // Advance the animation state
        state.advance();

        // Resolve the effective frame set with priority: explicit > CSS > default
        SpinnerFrameSet effectiveFrameSet = resolveFrameSet(context);

        // Build the spinner widget
        Spinner.Builder builder = Spinner.builder()
                .style(context.currentStyle())
                .frameSet(effectiveFrameSet);
        Spinner spinner = builder.build();

        // Calculate spinner area
        int spinnerWidth = Math.min(spinner.maxFrameWidth(), area.width());
        Rect spinnerArea = new Rect(area.x(), area.y(), spinnerWidth, 1);

        // Render the spinner
        frame.renderStatefulWidget(spinner, spinnerArea, state);

        // Render label if present and there's space
        if (label != null && !label.isEmpty()) {
            int labelX = area.x() + spinnerWidth + 1;
            int availableWidth = area.width() - spinnerWidth - 1;
            if (availableWidth > 0) {
                String truncated = CharWidth.substringByWidth(label, availableWidth);
                frame.buffer().setString(labelX, area.y(), truncated, context.currentStyle());
            }
        }
    }

    /**
     * Resolves the effective frame set with priority: explicit &gt; CSS &gt; default.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>Explicit programmatic frame set (via {@link #frameSet} or {@link #frames})</li>
     *   <li>CSS {@code spinner-frames} property</li>
     *   <li>Explicit programmatic style (via {@link #spinnerStyle})</li>
     *   <li>CSS {@code spinner-style} property</li>
     *   <li>Default (DOTS)</li>
     * </ol>
     */
    private SpinnerFrameSet resolveFrameSet(RenderContext context) {
        // 1. Explicit programmatic frame set takes highest priority
        if (customFrameSet != null) {
            return customFrameSet;
        }

        // 2. Check CSS properties
        StylePropertyResolver resolver = styleResolver(context);

        // CSS spinner-frames overrides spinner-style
        SpinnerFrameSet cssFrameSet = resolver.get(Spinner.SPINNER_FRAMES).orElse(null);
        if (cssFrameSet != null) {
            return cssFrameSet;
        }

        // 3. Explicit programmatic style
        if (spinnerStyle != null && spinnerStyle != SpinnerStyle.DOTS) {
            return spinnerStyle.frameSet();
        }

        // 4. CSS spinner-style
        SpinnerStyle cssStyle = resolver.get(Spinner.SPINNER_STYLE).orElse(null);
        if (cssStyle != null) {
            return cssStyle.frameSet();
        }

        // 5. Default: use the configured style (defaults to DOTS)
        return spinnerStyle.frameSet();
    }

    private int computeMaxFrameWidth() {
        SpinnerFrameSet frameSet = customFrameSet != null ? customFrameSet : spinnerStyle.frameSet();
        int max = 0;
        for (String f : frameSet.frames()) {
            max = Math.max(max, CharWidth.of(f));
        }
        return max;
    }
}

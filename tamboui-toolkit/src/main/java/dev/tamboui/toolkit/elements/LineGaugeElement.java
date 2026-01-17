/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widgets.gauge.LineGauge;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A DSL wrapper for the LineGauge widget.
 * <p>
 * A compact single-line progress indicator.
 * <pre>{@code
 * lineGauge(0.75)
 *     .label("Progress: ")
 *     .filledColor(Color.GREEN)
 * }</pre>
 *
 * <h2>CSS Child Selectors</h2>
 * <p>
 * The following child selectors can be used to style sub-components:
 * <ul>
 *   <li>{@code LineGaugeElement-filled} - The filled portion of the gauge</li>
 *   <li>{@code LineGaugeElement-unfilled} - The unfilled portion of the gauge</li>
 * </ul>
 * <p>
 * Example CSS:
 * <pre>{@code
 * LineGaugeElement-filled { color: green; }
 * LineGaugeElement-unfilled { color: gray; }
 * }</pre>
 * <p>
 * Note: Programmatic styles set via {@link #filledStyle(Style)} or {@link #unfilledStyle(Style)}
 * take precedence over CSS styles.
 */
public final class LineGaugeElement extends StyledElement<LineGaugeElement> {

    private static final Style DEFAULT_FILLED_STYLE = Style.EMPTY;
    private static final Style DEFAULT_UNFILLED_STYLE = Style.EMPTY;

    private double ratio = 0.0;
    private String label;
    private Style filledStyle;
    private Style unfilledStyle;
    private LineGauge.LineSet lineSet = LineGauge.NORMAL;

    public LineGaugeElement() {
    }

    public LineGaugeElement(double ratio) {
        this.ratio = Math.max(0.0, Math.min(1.0, ratio));
    }

    public LineGaugeElement(int percent) {
        this.ratio = Math.max(0, Math.min(100, percent)) / 100.0;
    }

    /**
     * Sets the progress as a ratio (0.0-1.0).
     */
    public LineGaugeElement ratio(double ratio) {
        this.ratio = Math.max(0.0, Math.min(1.0, ratio));
        return this;
    }

    /**
     * Sets the progress as a percentage (0-100).
     */
    public LineGaugeElement percent(int percent) {
        this.ratio = Math.max(0, Math.min(100, percent)) / 100.0;
        return this;
    }

    /**
     * Sets the label displayed before the gauge line.
     */
    public LineGaugeElement label(String label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the style for the filled portion.
     */
    public LineGaugeElement filledStyle(Style style) {
        this.filledStyle = style;
        return this;
    }

    /**
     * Sets the color for the filled portion.
     */
    public LineGaugeElement filledColor(Color color) {
        this.filledStyle = Style.EMPTY.fg(color);
        return this;
    }

    /**
     * Sets the style for the unfilled portion.
     */
    public LineGaugeElement unfilledStyle(Style style) {
        this.unfilledStyle = style;
        return this;
    }

    /**
     * Sets the color for the unfilled portion.
     */
    public LineGaugeElement unfilledColor(Color color) {
        this.unfilledStyle = Style.EMPTY.fg(color);
        return this;
    }

    /**
     * Uses thick line characters.
     */
    public LineGaugeElement thick() {
        this.lineSet = LineGauge.THICK;
        return this;
    }

    /**
     * Uses double line characters.
     */
    public LineGaugeElement doubleLine() {
        this.lineSet = LineGauge.DOUBLE;
        return this;
    }

    /**
     * Sets the line character set.
     */
    public LineGaugeElement lineSet(LineGauge.LineSet lineSet) {
        this.lineSet = lineSet;
        return this;
    }

    @Override
    public Map<String, String> styleAttributes() {
        Map<String, String> attrs = new LinkedHashMap<>(super.styleAttributes());
        if (label != null) {
            attrs.put("label", label);
        }
        return Collections.unmodifiableMap(attrs);
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        // Resolve styles with priority: explicit > CSS > default
        Style effectiveFilledStyle = resolveEffectiveStyle(context, "filled", filledStyle, DEFAULT_FILLED_STYLE);
        Style effectiveUnfilledStyle = resolveEffectiveStyle(context, "unfilled", unfilledStyle, DEFAULT_UNFILLED_STYLE);

        LineGauge.Builder builder = LineGauge.builder()
            .ratio(ratio)
            .style(context.currentStyle())
            .filledStyle(effectiveFilledStyle)
            .unfilledStyle(effectiveUnfilledStyle)
            .lineSet(lineSet);

        if (label != null) {
            builder.label(label);
        }

        frame.renderWidget(builder.build(), area);
    }
}

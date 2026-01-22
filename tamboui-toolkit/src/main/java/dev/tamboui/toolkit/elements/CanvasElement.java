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
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.canvas.Canvas;
import dev.tamboui.widgets.canvas.Context;
import dev.tamboui.widgets.canvas.Marker;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A DSL wrapper for the Canvas widget.
 * <p>
 * Draws arbitrary shapes on a coordinate system.
 * <pre>{@code
 * canvas()
 *     .xBounds(-10, 10)
 *     .yBounds(-10, 10)
 *     .marker(Marker.BRAILLE)
 *     .paint(ctx -> {
 *         ctx.draw(new Circle(0, 0, 5, Color.RED));
 *     })
 *     .title("Drawing")
 *     .rounded()
 * }</pre>
 */
public final class CanvasElement extends StyledElement<CanvasElement> {

    private double xMin = 0.0;
    private double xMax = 1.0;
    private double yMin = 0.0;
    private double yMax = 1.0;
    private Marker marker = Marker.BRAILLE;
    private Color backgroundColor;
    private Consumer<Context> paintCallback;
    private String title;
    private BorderType borderType;
    private Color borderColor;

    public CanvasElement() {
    }

    /**
     * Sets the X-axis bounds.
     */
    public CanvasElement xBounds(double min, double max) {
        this.xMin = min;
        this.xMax = max;
        return this;
    }

    /**
     * Sets the Y-axis bounds.
     */
    public CanvasElement yBounds(double min, double max) {
        this.yMin = min;
        this.yMax = max;
        return this;
    }

    /**
     * Sets both axis bounds.
     */
    public CanvasElement bounds(double xMin, double xMax, double yMin, double yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        return this;
    }

    /**
     * Sets the marker type for rendering points.
     */
    public CanvasElement marker(Marker marker) {
        this.marker = marker != null ? marker : Marker.BRAILLE;
        return this;
    }

    /**
     * Uses Braille marker (highest resolution).
     */
    public CanvasElement braille() {
        this.marker = Marker.BRAILLE;
        return this;
    }

    /**
     * Uses half-block marker.
     */
    public CanvasElement halfBlock() {
        this.marker = Marker.HALF_BLOCK;
        return this;
    }

    /**
     * Uses dot marker.
     */
    public CanvasElement dot() {
        this.marker = Marker.DOT;
        return this;
    }

    /**
     * Uses block marker.
     */
    public CanvasElement block() {
        this.marker = Marker.BLOCK;
        return this;
    }

    /**
     * Sets the background color.
     */
    public CanvasElement backgroundColor(Color color) {
        this.backgroundColor = color;
        return this;
    }

    /**
     * Sets the paint callback for drawing shapes.
     */
    public CanvasElement paint(Consumer<Context> callback) {
        this.paintCallback = callback;
        return this;
    }

    /**
     * Sets the title.
     */
    public CanvasElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     */
    public CanvasElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     */
    public CanvasElement borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    @Override
    public Map<String, String> styleAttributes() {
        Map<String, String> attrs = new LinkedHashMap<>(super.styleAttributes());
        if (title != null) {
            attrs.put("title", title);
        }
        return Collections.unmodifiableMap(attrs);
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }

        Canvas.Builder builder = Canvas.builder()
            .xBounds(xMin, xMax)
            .yBounds(yMin, yMax)
            .marker(marker);

        if (backgroundColor != null) {
            builder.backgroundColor(backgroundColor);
        }

        if (paintCallback != null) {
            builder.paint(paintCallback);
        }

        if (title != null || borderType != null) {
            Block.Builder blockBuilder = Block.builder()
                    .borders(Borders.ALL)
                    .styleResolver(styleResolver(context));
            if (title != null) {
                blockBuilder.title(Title.from(title));
            }
            if (borderType != null) {
                blockBuilder.borderType(borderType);
            }
            if (borderColor != null) {
                blockBuilder.borderColor(borderColor);
            }
            builder.block(blockBuilder.build());
        }

        frame.renderWidget(builder.build(), area);
    }
}

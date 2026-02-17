/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.canvas.Canvas;
import dev.tamboui.widgets.canvas.Context;
import dev.tamboui.widgets.canvas.Marker;

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

    /**
     * Creates a new canvas element with default bounds and Braille marker.
     */
    public CanvasElement() {
    }

    /**
     * Sets the X-axis bounds.
     *
     * @param min the minimum X value
     * @param max the maximum X value
     * @return this element
     */
    public CanvasElement xBounds(double min, double max) {
        this.xMin = min;
        this.xMax = max;
        return this;
    }

    /**
     * Sets the Y-axis bounds.
     *
     * @param min the minimum Y value
     * @param max the maximum Y value
     * @return this element
     */
    public CanvasElement yBounds(double min, double max) {
        this.yMin = min;
        this.yMax = max;
        return this;
    }

    /**
     * Sets both axis bounds.
     *
     * @param xMin the minimum X value
     * @param xMax the maximum X value
     * @param yMin the minimum Y value
     * @param yMax the maximum Y value
     * @return this element
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
     *
     * @param marker the marker type
     * @return this element
     */
    public CanvasElement marker(Marker marker) {
        this.marker = marker != null ? marker : Marker.BRAILLE;
        return this;
    }

    /**
     * Uses Braille marker (highest resolution).
     *
     * @return this element
     */
    public CanvasElement braille() {
        this.marker = Marker.BRAILLE;
        return this;
    }

    /**
     * Uses half-block marker.
     *
     * @return this element
     */
    public CanvasElement halfBlock() {
        this.marker = Marker.HALF_BLOCK;
        return this;
    }

    /**
     * Uses dot marker.
     *
     * @return this element
     */
    public CanvasElement dot() {
        this.marker = Marker.DOT;
        return this;
    }

    /**
     * Uses block marker.
     *
     * @return this element
     */
    public CanvasElement block() {
        this.marker = Marker.BLOCK;
        return this;
    }

    /**
     * Sets the background color.
     *
     * @param color the background color
     * @return this element
     */
    public CanvasElement backgroundColor(Color color) {
        this.backgroundColor = color;
        return this;
    }

    /**
     * Sets the paint callback for drawing shapes.
     *
     * @param callback the paint callback receiving a drawing context
     * @return this element
     */
    public CanvasElement paint(Consumer<Context> callback) {
        this.paintCallback = callback;
        return this;
    }

    /**
     * Sets the title.
     *
     * @param title the canvas title
     * @return this element
     */
    public CanvasElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     *
     * @return this element
     */
    public CanvasElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this element
     */
    public CanvasElement borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        // Canvas width based on x bounds range, scaled for marker resolution
        double xRange = xMax - xMin;
        double yRange = yMax - yMin;
        int width;
        int height;
        if (marker == Marker.BRAILLE) {
            // Braille has 2x2 dots per cell horizontally, 4 vertically
            width = (int) Math.ceil(xRange / 2);
            height = (int) Math.ceil(yRange / 4);
        } else if (marker == Marker.HALF_BLOCK) {
            // Half block is 1x2 pixels per cell
            width = (int) Math.ceil(xRange);
            height = (int) Math.ceil(yRange / 2);
        } else {
            width = (int) Math.ceil(xRange);
            height = (int) Math.ceil(yRange);
        }
        int borderSize = (title != null || borderType != null) ? 2 : 0;
        return Size.of(Math.max(width, 20) + borderSize, Math.max(height, 10) + borderSize);
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

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.sparkline.Sparkline;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A DSL wrapper for the Sparkline widget.
 * <p>
 * Displays data trends in a compact form using bar characters.
 * <pre>{@code
 * sparkline(1, 2, 3, 4, 5, 4, 3, 2, 1)
 *     .color(Color.CYAN)
 *     .title("CPU Usage")
 *     .rounded()
 * }</pre>
 */
public final class SparklineElement extends StyledElement<SparklineElement> {

    private long[] data = new long[0];
    private Long max;
    private Sparkline.BarSet barSet = Sparkline.BarSet.NINE_LEVELS;
    private Sparkline.RenderDirection direction = Sparkline.RenderDirection.LEFT_TO_RIGHT;
    private String title;
    private BorderType borderType;
    private Color borderColor;

    /** Creates a sparkline element with no data. */
    public SparklineElement() {
    }

    /**
     * Creates a sparkline element with the given data values.
     *
     * @param data the data values
     */
    public SparklineElement(long... data) {
        this.data = data != null ? data.clone() : new long[0];
    }

    /**
     * Creates a sparkline element with the given integer data values.
     *
     * @param data the data values as integers
     */
    public SparklineElement(int... data) {
        if (data != null) {
            this.data = new long[data.length];
            for (int i = 0; i < data.length; i++) {
                this.data[i] = data[i];
            }
        }
    }

    /**
     * Creates a sparkline element with data values from a collection.
     *
     * @param data the data values as a collection of numbers
     */
    public SparklineElement(Collection<? extends Number> data) {
        if (data != null) {
            this.data = data.stream().mapToLong(Number::longValue).toArray();
        }
    }

    /**
     * Sets the data values.
     *
     * @param data the data values
     * @return this element
     */
    public SparklineElement data(long... data) {
        this.data = data != null ? data.clone() : new long[0];
        return this;
    }

    /**
     * Sets the data values from integers.
     *
     * @param data the data values as integers
     * @return this element
     */
    public SparklineElement data(int... data) {
        if (data != null) {
            this.data = new long[data.length];
            for (int i = 0; i < data.length; i++) {
                this.data[i] = data[i];
            }
        }
        return this;
    }

    /**
     * Sets the data values from a collection.
     *
     * @param data the data values as a collection of numbers
     * @return this element
     */
    public SparklineElement data(Collection<? extends Number> data) {
        if (data != null) {
            this.data = data.stream().mapToLong(Number::longValue).toArray();
        }
        return this;
    }

    /**
     * Sets the maximum value for scaling.
     *
     * @param max the maximum value
     * @return this element
     */
    public SparklineElement max(long max) {
        this.max = max;
        return this;
    }

    /**
     * Uses auto-scaling based on data maximum.
     *
     * @return this element
     */
    public SparklineElement autoMax() {
        this.max = null;
        return this;
    }

    /**
     * Sets the sparkline color.
     *
     * @param color the sparkline color
     * @return this element
     */
    public SparklineElement color(Color color) {
        return fg(color);
    }

    /**
     * Uses three-level bar set (coarser display).
     *
     * @return this element
     */
    public SparklineElement threeLevels() {
        this.barSet = Sparkline.BarSet.THREE_LEVELS;
        return this;
    }

    /**
     * Sets the bar character set.
     *
     * @param barSet the bar character set
     * @return this element
     */
    public SparklineElement barSet(Sparkline.BarSet barSet) {
        this.barSet = barSet;
        return this;
    }

    /**
     * Renders data from right to left.
     *
     * @return this element
     */
    public SparklineElement rightToLeft() {
        this.direction = Sparkline.RenderDirection.RIGHT_TO_LEFT;
        return this;
    }

    /**
     * Sets the render direction.
     *
     * @param direction the render direction
     * @return this element
     */
    public SparklineElement direction(Sparkline.RenderDirection direction) {
        this.direction = direction;
        return this;
    }

    /**
     * Sets the title.
     *
     * @param title the sparkline title
     * @return this element
     */
    public SparklineElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     *
     * @return this element
     */
    public SparklineElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this element
     */
    public SparklineElement borderColor(Color color) {
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

        Sparkline.Builder builder = Sparkline.builder()
            .data(data)
            .style(context.currentStyle())
            .barSet(barSet)
            .direction(direction);

        if (max != null) {
            builder.max(max);
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

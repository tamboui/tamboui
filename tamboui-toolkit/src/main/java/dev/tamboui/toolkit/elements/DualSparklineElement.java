/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.LongFunction;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.ColorGradient;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.Size;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.sparkline.DualSparkline;
import dev.tamboui.widgets.sparkline.Sparkline;

/**
 * A DSL wrapper for the {@link DualSparkline} widget.
 * <p>
 * Displays two time-series datasets as vertical bars growing in opposite directions
 * from a shared centre axis — useful for paired metrics such as network in/out or
 * disk read/write.
 * <pre>{@code
 * dualSparkline()
 *     .topData(inRates)
 *     .bottomData(outRates)
 *     .topColor(Color.GREEN)
 *     .bottomColor(Color.BLUE)
 *     .title("In / Out")
 *     .rounded()
 * }</pre>
 *
 * <h2>CSS Child Selectors</h2>
 * <p>
 * The following CSS properties can be used via a style resolver:
 * <ul>
 *   <li>{@code top-color} - The foreground colour of the top series bars</li>
 *   <li>{@code bottom-color} - The foreground colour of the bottom series bars</li>
 * </ul>
 */
public final class DualSparklineElement extends StyledElement<DualSparklineElement> {

    private long[] topData = new long[0];
    private long[] bottomData = new long[0];
    private Long max;
    private Sparkline.BarSet barSet = Sparkline.BarSet.NINE_LEVELS;
    private Sparkline.RenderDirection direction = Sparkline.RenderDirection.LEFT_TO_RIGHT;
    private boolean showYAxis;
    private String[] xLabels;
    private Style topStyle;
    private Style bottomStyle;
    private Color topForeground;
    private Color bottomForeground;
    private String title;
    private BorderType borderType;
    private Color borderColor;
    private LongFunction<Color> topColorMapper;
    private LongFunction<Color> bottomColorMapper;
    private ColorGradient topGradient;
    private ColorGradient bottomGradient;

    /** Creates a dual sparkline element with no data. */
    public DualSparklineElement() {
    }

    /**
     * Sets the top series data values.
     *
     * @param data the top series values
     * @return this element
     */
    public DualSparklineElement topData(long... data) {
        this.topData = data != null ? data.clone() : new long[0];
        return this;
    }

    /**
     * Sets the top series data values from integers.
     *
     * @param data the top series values as integers
     * @return this element
     */
    public DualSparklineElement topData(int... data) {
        if (data != null) {
            this.topData = new long[data.length];
            for (int i = 0; i < data.length; i++) {
                this.topData[i] = data[i];
            }
        }
        return this;
    }

    /**
     * Sets the top series data values from a collection.
     *
     * @param data the top series values as a collection of numbers
     * @return this element
     */
    public DualSparklineElement topData(Collection<? extends Number> data) {
        if (data != null) {
            this.topData = data.stream().mapToLong(Number::longValue).toArray();
        }
        return this;
    }

    /**
     * Sets the bottom series data values.
     *
     * @param data the bottom series values
     * @return this element
     */
    public DualSparklineElement bottomData(long... data) {
        this.bottomData = data != null ? data.clone() : new long[0];
        return this;
    }

    /**
     * Sets the bottom series data values from integers.
     *
     * @param data the bottom series values as integers
     * @return this element
     */
    public DualSparklineElement bottomData(int... data) {
        if (data != null) {
            this.bottomData = new long[data.length];
            for (int i = 0; i < data.length; i++) {
                this.bottomData[i] = data[i];
            }
        }
        return this;
    }

    /**
     * Sets the bottom series data values from a collection.
     *
     * @param data the bottom series values as a collection of numbers
     * @return this element
     */
    public DualSparklineElement bottomData(Collection<? extends Number> data) {
        if (data != null) {
            this.bottomData = data.stream().mapToLong(Number::longValue).toArray();
        }
        return this;
    }

    /**
     * Sets the foreground colour for the top series bars.
     *
     * @param color the top bar colour
     * @return this element
     */
    public DualSparklineElement topColor(Color color) {
        this.topForeground = color;
        return this;
    }

    /**
     * Sets the foreground colour for the bottom series bars.
     *
     * @param color the bottom bar colour
     * @return this element
     */
    public DualSparklineElement bottomColor(Color color) {
        this.bottomForeground = color;
        return this;
    }

    /**
     * Sets the style for the top series bars.
     *
     * @param style the top bar style
     * @return this element
     */
    public DualSparklineElement topStyle(Style style) {
        this.topStyle = style;
        return this;
    }

    /**
     * Sets the style for the bottom series bars.
     *
     * @param style the bottom bar style
     * @return this element
     */
    public DualSparklineElement bottomStyle(Style style) {
        this.bottomStyle = style;
        return this;
    }

    /**
     * Sets the maximum value for scaling both series.
     *
     * @param max the maximum value
     * @return this element
     */
    public DualSparklineElement max(long max) {
        this.max = max;
        return this;
    }

    /**
     * Uses auto-scaling based on data maximum.
     *
     * @return this element
     */
    public DualSparklineElement autoMax() {
        this.max = null;
        return this;
    }

    /**
     * Shows or hides the Y-axis label on the left side.
     *
     * @param show whether to show the Y-axis label
     * @return this element
     */
    public DualSparklineElement showYAxis(boolean show) {
        this.showYAxis = show;
        return this;
    }

    /**
     * Sets X-axis labels rendered below the bottom series bars.
     * Labels are distributed evenly across the data range.
     *
     * @param labels the labels, distributed left-to-right
     * @return this element
     */
    public DualSparklineElement xLabels(String... labels) {
        this.xLabels = labels != null ? labels.clone() : null;
        return this;
    }

    /**
     * Uses three-level bar set (coarser display).
     *
     * @return this element
     */
    public DualSparklineElement threeLevels() {
        this.barSet = Sparkline.BarSet.THREE_LEVELS;
        return this;
    }

    /**
     * Sets the bar character set.
     *
     * @param barSet the bar character set
     * @return this element
     */
    public DualSparklineElement barSet(Sparkline.BarSet barSet) {
        this.barSet = barSet;
        return this;
    }

    /**
     * Renders data from right to left.
     *
     * @return this element
     */
    public DualSparklineElement rightToLeft() {
        this.direction = Sparkline.RenderDirection.RIGHT_TO_LEFT;
        return this;
    }

    /**
     * Sets the render direction.
     *
     * @param direction the render direction
     * @return this element
     */
    public DualSparklineElement direction(Sparkline.RenderDirection direction) {
        this.direction = direction;
        return this;
    }

    /**
     * Sets the title.
     *
     * @param title the sparkline title
     * @return this element
     */
    public DualSparklineElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     *
     * @return this element
     */
    public DualSparklineElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this element
     */
    public DualSparklineElement borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Sets a color mapper function for the top series bars.
     *
     * @param topColorMapper function mapping a value to a Color
     * @return this element
     */
    public DualSparklineElement topColorMapper(LongFunction<Color> topColorMapper) {
        this.topColorMapper = topColorMapper;
        return this;
    }

    /**
     * Sets a color mapper function for the bottom series bars.
     *
     * @param bottomColorMapper function mapping a value to a Color
     * @return this element
     */
    public DualSparklineElement bottomColorMapper(LongFunction<Color> bottomColorMapper) {
        this.bottomColorMapper = bottomColorMapper;
        return this;
    }

    /**
     * Sets a color gradient for the top series bars.
     *
     * @param stops gradient color stops
     * @return this element
     */
    public DualSparklineElement topGradient(Color... stops) {
        this.topGradient = stops != null && stops.length > 0 ? ColorGradient.of(stops) : null;
        return this;
    }

    /**
     * Sets a color gradient for the top series bars.
     *
     * @param topGradient the color gradient
     * @return this element
     */
    public DualSparklineElement topGradient(ColorGradient topGradient) {
        this.topGradient = topGradient;
        return this;
    }

    /**
     * Sets a color gradient for the bottom series bars.
     *
     * @param stops gradient color stops
     * @return this element
     */
    public DualSparklineElement bottomGradient(Color... stops) {
        this.bottomGradient = stops != null && stops.length > 0 ? ColorGradient.of(stops) : null;
        return this;
    }

    /**
     * Sets a color gradient for the bottom series bars.
     *
     * @param bottomGradient the color gradient
     * @return this element
     */
    public DualSparklineElement bottomGradient(ColorGradient bottomGradient) {
        this.bottomGradient = bottomGradient;
        return this;
    }

    /**
     * Sets the same color gradient for both top and bottom series bars.
     *
     * @param stops gradient color stops
     * @return this element
     */
    public DualSparklineElement gradient(Color... stops) {
        ColorGradient grad = stops != null && stops.length > 0 ? ColorGradient.of(stops) : null;
        this.topGradient = grad;
        this.bottomGradient = grad;
        return this;
    }

    /**
     * Sets the same color gradient for both top and bottom series bars.
     *
     * @param gradient the color gradient
     * @return this element
     */
    public DualSparklineElement gradient(ColorGradient gradient) {
        this.topGradient = gradient;
        this.bottomGradient = gradient;
        return this;
    }

    @Override
    public Size preferredSize(int availableWidth, int availableHeight, RenderContext context) {
        int dataLen = Math.max(topData.length, bottomData.length);
        int width = Math.max(dataLen, 10);
        int borderWidth = (title != null || borderType != null) ? 2 : 0;
        width += borderWidth;
        // Minimum: top half + centre + bottom half = 3 rows, plus borders
        int height = 3 + borderWidth;
        return Size.of(width, height);
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

        DualSparkline.Builder builder = DualSparkline.builder()
            .topData(topData)
            .bottomData(bottomData)
            .barSet(barSet)
            .direction(direction)
            .showYAxis(showYAxis)
            .styleResolver(styleResolver(context));

        if (topStyle != null) {
            builder.topStyle(topStyle);
        }
        if (bottomStyle != null) {
            builder.bottomStyle(bottomStyle);
        }
        if (topForeground != null) {
            builder.topForeground(topForeground);
        }
        if (bottomForeground != null) {
            builder.bottomForeground(bottomForeground);
        }
        if (topColorMapper != null) {
            builder.topColorMapper(topColorMapper);
        }
        if (bottomColorMapper != null) {
            builder.bottomColorMapper(bottomColorMapper);
        }
        if (topGradient != null) {
            builder.topGradient(topGradient);
        }
        if (bottomGradient != null) {
            builder.bottomGradient(bottomGradient);
        }
        if (max != null) {
            builder.max(max);
        }
        if (xLabels != null) {
            builder.xLabels(xLabels);
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

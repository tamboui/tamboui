/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.layout.Direction;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widgets.barchart.Bar;
import dev.tamboui.widgets.barchart.BarChart;
import dev.tamboui.widgets.barchart.BarGroup;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A DSL wrapper for the BarChart widget.
 * <p>
 * Displays grouped bar charts.
 * <pre>{@code
 * barChart()
 *     .data(10, 20, 30, 40)
 *     .barWidth(3)
 *     .barColor(Color.CYAN)
 *     .title("Sales")
 *     .rounded()
 * }</pre>
 */
public final class BarChartElement extends StyledElement<BarChartElement> {

    private final List<BarGroup> groups = new ArrayList<>();
    private Long max;
    private int barWidth = 1;
    private int barGap = 1;
    private int groupGap = 1;
    private Direction direction = Direction.VERTICAL;
    private Style barStyle;
    private Style valueStyle;
    private Style labelStyle;
    private BarChart.BarSet barSet = BarChart.BarSet.NINE_LEVELS;
    private String title;
    private BorderType borderType;
    private Color borderColor;

    /**
     * Creates a new bar chart element with default settings.
     */
    public BarChartElement() {
    }

    /**
     * Adds data as simple values (single group).
     *
     * @param values the data values
     * @return this element
     */
    public BarChartElement data(long... values) {
        this.groups.clear();
        this.groups.add(BarGroup.of(values));
        return this;
    }

    /**
     * Adds data as bars with labels.
     *
     * @param bars the bars to display
     * @return this element
     */
    public BarChartElement data(Bar... bars) {
        this.groups.clear();
        this.groups.add(BarGroup.of(bars));
        return this;
    }

    /**
     * Adds a bar group.
     *
     * @param group the bar group to add
     * @return this element
     */
    public BarChartElement group(BarGroup group) {
        this.groups.add(group);
        return this;
    }

    /**
     * Adds bar groups.
     *
     * @param groups the bar groups to set
     * @return this element
     */
    public BarChartElement groups(BarGroup... groups) {
        this.groups.clear();
        this.groups.addAll(Arrays.asList(groups));
        return this;
    }

    /**
     * Adds bar groups from a list.
     *
     * @param groups the bar groups to set
     * @return this element
     */
    public BarChartElement groups(List<BarGroup> groups) {
        this.groups.clear();
        this.groups.addAll(groups);
        return this;
    }

    /**
     * Sets the maximum value for scaling.
     *
     * @param max the maximum value
     * @return this element
     */
    public BarChartElement max(long max) {
        this.max = max;
        return this;
    }

    /**
     * Uses auto-scaling based on data maximum.
     *
     * @return this element
     */
    public BarChartElement autoMax() {
        this.max = null;
        return this;
    }

    /**
     * Sets the bar width.
     *
     * @param width the bar width in columns
     * @return this element
     */
    public BarChartElement barWidth(int width) {
        this.barWidth = Math.max(1, width);
        return this;
    }

    /**
     * Sets the gap between bars in a group.
     *
     * @param gap the gap between bars
     * @return this element
     */
    public BarChartElement barGap(int gap) {
        this.barGap = Math.max(0, gap);
        return this;
    }

    /**
     * Sets the gap between groups.
     *
     * @param gap the gap between groups
     * @return this element
     */
    public BarChartElement groupGap(int gap) {
        this.groupGap = Math.max(0, gap);
        return this;
    }

    /**
     * Sets horizontal direction.
     *
     * @return this element
     */
    public BarChartElement horizontal() {
        this.direction = Direction.HORIZONTAL;
        return this;
    }

    /**
     * Sets vertical direction.
     *
     * @return this element
     */
    public BarChartElement vertical() {
        this.direction = Direction.VERTICAL;
        return this;
    }

    /**
     * Sets the direction.
     *
     * @param direction the chart direction
     * @return this element
     */
    public BarChartElement direction(Direction direction) {
        this.direction = direction;
        return this;
    }

    /**
     * Sets the bar style.
     *
     * @param style the bar style
     * @return this element
     */
    public BarChartElement barStyle(Style style) {
        this.barStyle = style;
        return this;
    }

    /**
     * Sets the bar color.
     *
     * @param color the bar foreground color
     * @return this element
     */
    public BarChartElement barColor(Color color) {
        this.barStyle = Style.EMPTY.fg(color);
        return this;
    }

    /**
     * Sets the value display style.
     *
     * @param style the value label style
     * @return this element
     */
    public BarChartElement valueStyle(Style style) {
        this.valueStyle = style;
        return this;
    }

    /**
     * Sets the label style.
     *
     * @param style the bar label style
     * @return this element
     */
    public BarChartElement labelStyle(Style style) {
        this.labelStyle = style;
        return this;
    }

    /**
     * Uses three-level bar set.
     *
     * @return this element
     */
    public BarChartElement threeLevels() {
        this.barSet = BarChart.BarSet.THREE_LEVELS;
        return this;
    }

    /**
     * Sets the bar character set.
     *
     * @param barSet the bar character set to use
     * @return this element
     */
    public BarChartElement barSet(BarChart.BarSet barSet) {
        this.barSet = barSet;
        return this;
    }

    /**
     * Sets the title.
     *
     * @param title the chart title
     * @return this element
     */
    public BarChartElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     *
     * @return this element
     */
    public BarChartElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this element
     */
    public BarChartElement borderColor(Color color) {
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

        BarChart.Builder builder = BarChart.builder()
            .data(groups)
            .barWidth(barWidth)
            .barGap(barGap)
            .groupGap(groupGap)
            .direction(direction)
            .barSet(barSet)
            .style(context.currentStyle());

        if (max != null) {
            builder.max(max);
        }

        if (barStyle != null) {
            builder.barStyle(barStyle);
        }

        if (valueStyle != null) {
            builder.valueStyle(valueStyle);
        }

        if (labelStyle != null) {
            builder.labelStyle(labelStyle);
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

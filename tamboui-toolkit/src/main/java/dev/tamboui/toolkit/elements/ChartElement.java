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
import dev.tamboui.widgets.chart.Axis;
import dev.tamboui.widgets.chart.Chart;
import dev.tamboui.widgets.chart.Dataset;
import dev.tamboui.widgets.chart.LegendPosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A DSL wrapper for the Chart widget.
 * <p>
 * Plots datasets in a cartesian coordinate system.
 * <pre>{@code
 * chart()
 *     .dataset(Dataset.builder()
 *         .name("Series 1")
 *         .data(new double[][] {{0,1}, {1,3}, {2,2}})
 *         .build())
 *     .xAxis(Axis.builder().bounds(0, 4).build())
 *     .yAxis(Axis.builder().bounds(0, 5).build())
 *     .title("My Chart")
 *     .rounded()
 * }</pre>
 */
public final class ChartElement extends StyledElement<ChartElement> {

    private final List<Dataset> datasets = new ArrayList<>();
    private Axis xAxis = Axis.defaults();
    private Axis yAxis = Axis.defaults();
    private LegendPosition legendPosition = LegendPosition.TOP_RIGHT;
    private String title;
    private BorderType borderType;
    private Color borderColor;

    /**
     * Creates a new chart element with default settings.
     */
    public ChartElement() {
    }

    /**
     * Adds a dataset.
     *
     * @param dataset the dataset to add
     * @return this element
     */
    public ChartElement dataset(Dataset dataset) {
        this.datasets.add(dataset);
        return this;
    }

    /**
     * Sets all datasets.
     *
     * @param datasets the datasets to set
     * @return this element
     */
    public ChartElement datasets(Dataset... datasets) {
        this.datasets.clear();
        this.datasets.addAll(Arrays.asList(datasets));
        return this;
    }

    /**
     * Sets all datasets from a list.
     *
     * @param datasets the datasets to set
     * @return this element
     */
    public ChartElement datasets(List<Dataset> datasets) {
        this.datasets.clear();
        this.datasets.addAll(datasets);
        return this;
    }

    /**
     * Sets the X-axis configuration.
     *
     * @param xAxis the X-axis configuration
     * @return this element
     */
    public ChartElement xAxis(Axis xAxis) {
        this.xAxis = xAxis != null ? xAxis : Axis.defaults();
        return this;
    }

    /**
     * Sets the Y-axis configuration.
     *
     * @param yAxis the Y-axis configuration
     * @return this element
     */
    public ChartElement yAxis(Axis yAxis) {
        this.yAxis = yAxis != null ? yAxis : Axis.defaults();
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
    public ChartElement bounds(double xMin, double xMax, double yMin, double yMax) {
        this.xAxis = Axis.builder().bounds(xMin, xMax).build();
        this.yAxis = Axis.builder().bounds(yMin, yMax).build();
        return this;
    }

    /**
     * Sets the legend position.
     *
     * @param position the legend position
     * @return this element
     */
    public ChartElement legendPosition(LegendPosition position) {
        this.legendPosition = position;
        return this;
    }

    /**
     * Hides the legend.
     *
     * @return this element
     */
    public ChartElement hideLegend() {
        this.legendPosition = null;
        return this;
    }

    /**
     * Sets the title.
     *
     * @param title the chart title
     * @return this element
     */
    public ChartElement title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Uses rounded borders.
     *
     * @return this element
     */
    public ChartElement rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border color.
     *
     * @param color the border color
     * @return this element
     */
    public ChartElement borderColor(Color color) {
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

        Chart.Builder builder = Chart.builder()
            .datasets(datasets)
            .xAxis(xAxis)
            .yAxis(yAxis)
            .style(context.currentStyle());

        if (legendPosition != null) {
            builder.legendPosition(legendPosition);
        } else {
            builder.hideLegend();
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

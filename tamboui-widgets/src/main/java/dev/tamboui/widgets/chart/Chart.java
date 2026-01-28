/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.chart;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;
import static dev.tamboui.util.CollectionUtil.listCopyOf;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A chart widget for plotting datasets in a cartesian coordinate system.
 * <p>
 * Supports scatter plots, line charts, and bar charts with configurable
 * axes, legends, and styling.
 *
 * <pre>{@code
 * Chart chart = Chart.builder()
 *     .datasets(
 *         Dataset.builder()
 *             .name("Series 1")
 *             .data(new double[][] {{0, 1}, {1, 3}, {2, 2}, {3, 4}})
 *             .graphType(GraphType.LINE)
 *             .style(Style.EMPTY.fg(Color.CYAN))
 *             .build()
 *     )
 *     .xAxis(Axis.builder()
 *         .title("X")
 *         .bounds(0, 4)
 *         .labels("0", "1", "2", "3", "4")
 *         .build())
 *     .yAxis(Axis.builder()
 *         .title("Y")
 *         .bounds(0, 5)
 *         .labels("0", "1", "2", "3", "4", "5")
 *         .build())
 *     .block(Block.bordered().title(Title.from("My Chart")))
 *     .build();
 * }</pre>
 *
 * @see Dataset
 * @see Axis
 * @see GraphType
 */
public final class Chart implements Widget {

    private final List<Dataset> datasets;
    private final Axis xAxis;
    private final Axis yAxis;
    private final Block block;
    private final Style style;
    private final LegendPosition legendPosition;

    private Chart(Builder builder) {
        this.datasets = listCopyOf(builder.datasets);
        this.xAxis = builder.xAxis;
        this.yAxis = builder.yAxis;
        this.block = builder.block;
        this.style = builder.style;
        this.legendPosition = builder.legendPosition;
    }

    /**
     * Creates a new chart builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a chart with the given datasets.
     *
     * @param datasets the datasets to plot
     * @return a new chart
     */
    public static Chart of(Dataset... datasets) {
        return builder().datasets(datasets).build();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty()) {
            return;
        }

        // Apply overall style
        if (style != null) {
            buffer.setStyle(area, style);
        }

        // Render block if present
        Rect chartArea = area;
        if (block != null) {
            block.render(area, buffer);
            chartArea = block.inner(area);
        }

        if (chartArea.isEmpty() || datasets.isEmpty()) {
            return;
        }

        // Calculate layout
        int yAxisLabelWidth = calculateYAxisLabelWidth();
        int xAxisLabelHeight = xAxis.hasLabels() ? 1 : 0;
        int yAxisTitleWidth = yAxis.title().isPresent() ? 2 : 0;
        int xAxisTitleHeight = xAxis.title().isPresent() ? 1 : 0;

        // Graph area (excluding axes)
        int graphLeft = chartArea.x() + yAxisLabelWidth + yAxisTitleWidth;
        int graphTop = chartArea.y();
        int graphWidth = chartArea.width() - yAxisLabelWidth - yAxisTitleWidth;
        int graphHeight = chartArea.height() - xAxisLabelHeight - xAxisTitleHeight;

        if (graphWidth <= 0 || graphHeight <= 0) {
            return;
        }

        Rect graphArea = new Rect(graphLeft, graphTop, graphWidth, graphHeight);

        // Render Y-axis labels
        renderYAxisLabels(buffer, chartArea, graphArea);

        // Render X-axis labels
        renderXAxisLabels(buffer, chartArea, graphArea);

        // Render axis titles
        renderAxisTitles(buffer, chartArea, graphArea);

        // Render datasets
        for (Dataset dataset : datasets) {
            renderDataset(buffer, graphArea, dataset);
        }

        // Render legend
        if (legendPosition != null && hasLegend()) {
            renderLegend(buffer, graphArea);
        }
    }

    private void renderYAxisLabels(Buffer buffer, Rect chartArea, Rect graphArea) {
        if (!yAxis.hasLabels()) {
            return;
        }

        List<Span> labels = yAxis.labels();
        int labelCount = labels.size();
        if (labelCount == 0) {
            return;
        }

        Style labelStyle = yAxis.style();
        int labelWidth = calculateYAxisLabelWidth();
        int x = chartArea.x() + (yAxis.title().isPresent() ? 2 : 0);

        for (int i = 0; i < labelCount; i++) {
            // Distribute labels evenly along Y axis (bottom to top)
            double fraction = labelCount > 1 ? (double) i / (labelCount - 1) : 0;
            int y = graphArea.bottom() - 1 - (int) Math.round(fraction * (graphArea.height() - 1));

            if (y >= graphArea.y() && y < graphArea.bottom()) {
                String labelText = labels.get(i).content();
                int labelTextWidth = CharWidth.of(labelText);
                // Right-align label
                int labelX = x + labelWidth - labelTextWidth;
                buffer.setString(Math.max(x, labelX), y, labelText, labelStyle);
            }
        }
    }

    private void renderXAxisLabels(Buffer buffer, Rect chartArea, Rect graphArea) {
        if (!xAxis.hasLabels()) {
            return;
        }

        List<Span> labels = xAxis.labels();
        int labelCount = labels.size();
        if (labelCount == 0) {
            return;
        }

        Style labelStyle = xAxis.style();
        int y = graphArea.bottom();

        for (int i = 0; i < labelCount; i++) {
            // Distribute labels evenly along X axis
            double fraction = labelCount > 1 ? (double) i / (labelCount - 1) : 0;
            int x = graphArea.x() + (int) Math.round(fraction * (graphArea.width() - 1));

            if (x >= graphArea.x() && x < graphArea.right()) {
                String labelText = labels.get(i).content();
                int labelTextWidth = CharWidth.of(labelText);
                // Center label under position
                int labelX = x - labelTextWidth / 2;
                labelX = Math.max(graphArea.x(), Math.min(labelX, graphArea.right() - labelTextWidth));
                buffer.setString(labelX, y, labelText, labelStyle);
            }
        }
    }

    private void renderAxisTitles(Buffer buffer, Rect chartArea, Rect graphArea) {
        // Y-axis title (vertical, on left)
        yAxis.title().ifPresent(title -> {
            int x = chartArea.x();
            // For simplicity, just show characters vertically
            String titleText = title.rawContent();
            int row = 0;
            int i = 0;
            while (i < titleText.length() && row < graphArea.height()) {
                int codePoint = titleText.codePointAt(i);
                String symbol = new String(Character.toChars(codePoint));
                buffer.setString(x, graphArea.y() + row, symbol, yAxis.style());
                row++;
                i += Character.charCount(codePoint);
            }
        });

        // X-axis title (horizontal, at bottom)
        xAxis.title().ifPresent(title -> {
            int y = chartArea.bottom() - 1;
            String titleText = title.rawContent();
            int titleWidth = CharWidth.of(titleText);
            int x = graphArea.x() + (graphArea.width() - titleWidth) / 2;
            buffer.setString(Math.max(graphArea.x(), x), y, titleText, xAxis.style());
        });
    }

    private void renderDataset(Buffer buffer, Rect graphArea, Dataset dataset) {
        double[][] data = dataset.data();
        if (data.length == 0) {
            return;
        }

        Style dataStyle = dataset.style();
        String marker = dataset.marker().symbol();

        // Calculate coordinate mappings
        double xMin = xAxis.min();
        double xMax = xAxis.max();
        double yMin = yAxis.min();
        double yMax = yAxis.max();
        double xRange = xMax - xMin;
        double yRange = yMax - yMin;

        if (xRange == 0) {
            xRange = 1;
        }
        if (yRange == 0) {
            yRange = 1;
        }

        int[] screenX = new int[data.length];
        int[] screenY = new int[data.length];

        // Convert data coordinates to screen coordinates
        for (int i = 0; i < data.length; i++) {
            double x = data[i][0];
            double y = data[i][1];

            // Map to graph area
            double xFraction = (x - xMin) / xRange;
            double yFraction = (y - yMin) / yRange;

            screenX[i] = graphArea.x() + (int) Math.round(xFraction * (graphArea.width() - 1));
            screenY[i] = graphArea.bottom() - 1 - (int) Math.round(yFraction * (graphArea.height() - 1));
        }

        // Render based on graph type
        switch (dataset.graphType()) {
            case SCATTER:
                renderScatter(buffer, graphArea, screenX, screenY, marker, dataStyle);
                break;
            case LINE:
                renderLine(buffer, graphArea, screenX, screenY, marker, dataStyle);
                break;
            case BAR:
                renderBars(buffer, graphArea, screenX, screenY, dataStyle);
                break;
            default:
                break;
        }
    }

    private void renderScatter(Buffer buffer, Rect graphArea, int[] screenX, int[] screenY,
                                String marker, Style style) {
        for (int i = 0; i < screenX.length; i++) {
            int x = screenX[i];
            int y = screenY[i];
            if (isInBounds(graphArea, x, y)) {
                buffer.setString(x, y, marker, style);
            }
        }
    }

    private void renderLine(Buffer buffer, Rect graphArea, int[] screenX, int[] screenY,
                             String marker, Style style) {
        // First render the points
        renderScatter(buffer, graphArea, screenX, screenY, marker, style);

        // Then connect with lines
        for (int i = 0; i < screenX.length - 1; i++) {
            drawLine(buffer, graphArea, screenX[i], screenY[i], screenX[i + 1], screenY[i + 1], style);
        }
    }

    private void renderBars(Buffer buffer, Rect graphArea, int[] screenX, int[] screenY, Style style) {
        int baseY = graphArea.bottom() - 1;

        for (int i = 0; i < screenX.length; i++) {
            int x = screenX[i];
            int topY = screenY[i];

            // Draw vertical bar from baseline to data point
            for (int y = baseY; y >= topY && y >= graphArea.y(); y--) {
                if (isInBounds(graphArea, x, y)) {
                    buffer.setString(x, y, "█", style);
                }
            }
        }
    }

    private void drawLine(Buffer buffer, Rect graphArea, int x0, int y0, int x1, int y1, Style style) {
        // Bresenham's line algorithm
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            if (isInBounds(graphArea, x0, y0)) {
                // Use different characters based on line direction
                String lineChar = getLineChar(dx, dy, sx, sy);
                buffer.setString(x0, y0, lineChar, style);
            }

            if (x0 == x1 && y0 == y1) {
                break;
            }

            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private String getLineChar(int dx, int dy, int sx, int sy) {
        if (dx == 0) {
            return "│";
        }
        if (dy == 0) {
            return "─";
        }
        if (dx > dy * 2) {
            return "─";
        }
        if (dy > dx * 2) {
            return "│";
        }
        if (sx == sy) {
            return "╲";
        }
        return "╱";
    }

    private boolean isInBounds(Rect area, int x, int y) {
        return x >= area.x() && x < area.right() && y >= area.y() && y < area.bottom();
    }

    private void renderLegend(Buffer buffer, Rect graphArea) {
        // Count datasets with names
        List<Dataset> namedDatasets = datasets.stream()
            .filter(Dataset::hasName)
            .collect(Collectors.toList());

        if (namedDatasets.isEmpty()) {
            return;
        }

        // Calculate legend size
        int maxNameLength = namedDatasets.stream()
            .mapToInt(d -> d.name().map(n -> n.rawContent().length()).orElse(0))
            .max()
            .orElse(0);

        int legendWidth = maxNameLength + 4; // marker + space + name + padding
        int legendHeight = namedDatasets.size();

        // Position legend
        int legendX, legendY;
        switch (legendPosition) {
            case TOP_LEFT:
                legendX = graphArea.x() + 1;
                legendY = graphArea.y();
                break;
            case TOP_RIGHT:
                legendX = graphArea.right() - legendWidth - 1;
                legendY = graphArea.y();
                break;
            case BOTTOM_LEFT:
                legendX = graphArea.x() + 1;
                legendY = graphArea.bottom() - legendHeight;
                break;
            case BOTTOM_RIGHT:
                legendX = graphArea.right() - legendWidth - 1;
                legendY = graphArea.bottom() - legendHeight;
                break;
            default:
                legendX = graphArea.right() - legendWidth - 1;
                legendY = graphArea.y();
                break;
        }

        // Render legend entries
        for (int i = 0; i < namedDatasets.size(); i++) {
            Dataset ds = namedDatasets.get(i);
            int y = legendY + i;
            if (y >= graphArea.y() && y < graphArea.bottom()) {
                String entry = ds.marker().symbol() + " " + ds.name().map(Line::rawContent).orElse("");
                buffer.setString(legendX, y, entry, ds.style());
            }
        }
    }

    private boolean hasLegend() {
        return datasets.stream().anyMatch(Dataset::hasName);
    }

    private int calculateYAxisLabelWidth() {
        if (!yAxis.hasLabels()) {
            return 0;
        }
        return yAxis.labels().stream()
            .mapToInt(s -> CharWidth.of(s.content()))
            .max()
            .orElse(0) + 1;
    }

    /**
     * Builder for {@link Chart}.
     */
    public static final class Builder {
        private final List<Dataset> datasets = new ArrayList<>();
        private Axis xAxis = Axis.defaults();
        private Axis yAxis = Axis.defaults();
        private Block block;
        private Style style;
        private LegendPosition legendPosition = LegendPosition.TOP_RIGHT;

        private Builder() {}

        /**
         * Sets the datasets to plot.
         *
         * @param datasets the datasets to plot
         * @return this builder
         */
        public Builder datasets(Dataset... datasets) {
            this.datasets.clear();
            if (datasets != null) {
                this.datasets.addAll(listCopyOf(datasets));
            }
            return this;
        }

        /**
         * Sets the datasets to plot.
         *
         * @param datasets the datasets to plot
         * @return this builder
         */
        public Builder datasets(List<Dataset> datasets) {
            this.datasets.clear();
            if (datasets != null) {
                this.datasets.addAll(datasets);
            }
            return this;
        }

        /**
         * Adds a dataset.
         *
         * @param dataset the dataset to add
         * @return this builder
         */
        public Builder addDataset(Dataset dataset) {
            if (dataset != null) {
                this.datasets.add(dataset);
            }
            return this;
        }

        /**
         * Sets the X-axis configuration.
         *
         * @param xAxis the x-axis configuration
         * @return this builder
         */
        public Builder xAxis(Axis xAxis) {
            this.xAxis = xAxis != null ? xAxis : Axis.defaults();
            return this;
        }

        /**
         * Sets the Y-axis configuration.
         *
         * @param yAxis the y-axis configuration
         * @return this builder
         */
        public Builder yAxis(Axis yAxis) {
            this.yAxis = yAxis != null ? yAxis : Axis.defaults();
            return this;
        }

        /**
         * Wraps the chart in a block.
         *
         * @param block the block to wrap the chart in
         * @return this builder
         */
        public Builder block(Block block) {
            this.block = block;
            return this;
        }

        /**
         * Sets the chart style.
         *
         * @param style the style to apply to the chart
         * @return this builder
         */
        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        /**
         * Sets the legend position.
         *
         * @param position the legend position
         * @return this builder
         */
        public Builder legendPosition(LegendPosition position) {
            this.legendPosition = position;
            return this;
        }

        /**
         * Hides the legend.
         *
         * @return this builder
         */
        public Builder hideLegend() {
            this.legendPosition = null;
            return this;
        }

        /**
         * Builds the chart.
         *
         * @return a new chart instance
         */
        public Chart build() {
            return new Chart(this);
        }
    }
}

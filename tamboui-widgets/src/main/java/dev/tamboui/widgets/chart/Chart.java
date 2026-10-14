/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widget.Widget;
import dev.tamboui.widgets.block.Block;

import static dev.tamboui.util.CollectionUtil.listCopyOf;

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

    /**
     * A line with an end more than this many cells away from the bottom-left cell of the graph
     * is first clipped to this distance. That keeps the line arithmetic within the long range;
     * rounding a clipped end to a cell moves the line by at most half a cell on the graph.
     */
    private static final double CLIP_LIMIT = 1 << 29;

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

        // Cell of each point, counted from the bottom-left cell of the graph area. Kept as doubles:
        // a point far outside the graph has a cell number beyond the int range.
        double[] cellX = new double[data.length];
        double[] cellY = new double[data.length];

        for (int i = 0; i < data.length; i++) {
            double xFraction = (data[i][0] - xMin) / xRange;
            double yFraction = (data[i][1] - yMin) / yRange;

            cellX[i] = roundToCell(xFraction * (graphArea.width() - 1));
            cellY[i] = roundToCell(yFraction * (graphArea.height() - 1));
        }

        // Render based on graph type
        switch (dataset.graphType()) {
            case SCATTER:
                renderScatter(buffer, graphArea, cellX, cellY, marker, dataStyle);
                break;
            case LINE:
                renderLine(buffer, graphArea, cellX, cellY, marker, dataStyle);
                break;
            case BAR:
                renderBars(buffer, graphArea, cellX, cellY, dataStyle);
                break;
            default:
                break;
        }
    }

    private void renderScatter(Buffer buffer, Rect graphArea, double[] cellX, double[] cellY,
                                String marker, Style style) {
        for (int i = 0; i < cellX.length; i++) {
            double x = cellX[i];
            double y = cellY[i];
            if (x >= 0 && x < graphArea.width() && y >= 0 && y < graphArea.height()) {
                buffer.setString(graphArea.x() + (int) x, graphArea.bottom() - 1 - (int) y, marker, style);
            }
        }
    }

    private void renderLine(Buffer buffer, Rect graphArea, double[] cellX, double[] cellY,
                             String marker, Style style) {
        // First render the points
        renderScatter(buffer, graphArea, cellX, cellY, marker, style);

        // Then connect them, leaving a gap at a point with a NaN or infinite coordinate
        for (int i = 0; i < cellX.length - 1; i++) {
            if (isFinite(cellX[i], cellY[i]) && isFinite(cellX[i + 1], cellY[i + 1])) {
                drawSegment(buffer, graphArea, cellX[i], cellY[i], cellX[i + 1], cellY[i + 1], style);
            }
        }
    }

    private void renderBars(Buffer buffer, Rect graphArea, double[] cellX, double[] cellY, Style style) {
        for (int i = 0; i < cellX.length; i++) {
            double x = cellX[i];
            double y = cellY[i];
            if (!isFinite(x, y) || x < 0 || x >= graphArea.width() || y < 0) {
                continue;
            }

            // Draw vertical bar from baseline to data point
            int top = (int) Math.min(y, graphArea.height() - 1);
            for (int row = 0; row <= top; row++) {
                buffer.setString(graphArea.x() + (int) x, graphArea.bottom() - 1 - row, "█", style);
            }
        }
    }

    /**
     * Draws the line between two points given as cells counted from the bottom-left cell of the
     * graph area, with y going up.
     */
    private void drawSegment(Buffer buffer, Rect graphArea, double x0, double y0, double x1, double y1,
                             Style style) {
        double[] ends = {x0, y0, x1, y1};
        double farthest = Math.max(Math.max(Math.abs(x0), Math.abs(y0)), Math.max(Math.abs(x1), Math.abs(y1)));
        if (farthest > CLIP_LIMIT) {
            ends = clipToSquare(x0, y0, x1, y1, CLIP_LIMIT);
            if (ends == null) {
                return;
            }
        }
        long left = graphArea.x();
        long bottom = graphArea.bottom() - 1L;
        drawLine(buffer, graphArea, left + (long) ends[0], bottom - (long) ends[1],
            left + (long) ends[2], bottom - (long) ends[3], style);
    }

    /**
     * Draws the cells of Bresenham's line from (x0, y0) to (x1, y1) that are inside the area.
     * <p>
     * Along the longer axis the line moves one cell per step. After k steps it has moved
     * k * minor / major cells along the other axis, rounded to the nearest cell with ties going
     * to the start: the same cells as the usual loop that adds up the error step by step. Only
     * the steps inside the area are visited, so a line to a point far outside costs no more than
     * a line across the area.
     */
    private void drawLine(Buffer buffer, Rect area, long x0, long y0, long x1, long y1, Style style) {
        long dx = Math.abs(x1 - x0);
        long dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        // Use different characters based on line direction
        String lineChar = getLineChar(dx, dy, sx, sy);

        boolean alongX = dx >= dy;
        long major = alongX ? dx : dy;
        long minor = alongX ? dy : dx;
        long start = alongX ? x0 : y0;
        int step = alongX ? sx : sy;
        long low = alongX ? area.x() : area.y();
        long high = (alongX ? area.right() : area.bottom()) - 1L;

        long first = Math.max(0, step > 0 ? low - start : start - high);
        long last = Math.min(major, step > 0 ? high - start : start - low);
        for (long k = first; k <= last; k++) {
            long side = major == 0 ? 0 : (2 * k * minor + major - 1) / (2 * major);
            long x = alongX ? x0 + sx * k : x0 + sx * side;
            long y = alongX ? y0 + sy * side : y0 + sy * k;
            if (isInBounds(area, x, y)) {
                buffer.setString((int) x, (int) y, lineChar, style);
            }
        }
    }

    /**
     * Clips the segment to the square from -limit to limit on both axes, rounding the ends to
     * cells.
     *
     * @return the ends of the clipped segment as {x0, y0, x1, y1}, or null if it misses the square
     */
    private static double[] clipToSquare(double x0, double y0, double x1, double y1, double limit) {
        // Liang-Barsky: the part of the segment, from 0 to 1, that is on the inner side of all edges
        double dx = x1 - x0;
        double dy = y1 - y0;
        double[] range = {0, 1};
        if (!clipEdge(-dx, x0 + limit, range) || !clipEdge(dx, limit - x0, range)
                || !clipEdge(-dy, y0 + limit, range) || !clipEdge(dy, limit - y0, range)) {
            return null;
        }
        double[] ends = {x0 + range[0] * dx, y0 + range[0] * dy, x0 + range[1] * dx, y0 + range[1] * dy};
        for (int i = 0; i < ends.length; i++) {
            if (Double.isNaN(ends[i])) {
                return null;
            }
            // Rounding errors of huge coordinates must not move an end out of the square
            ends[i] = Math.max(-limit, Math.min(limit, roundToCell(ends[i])));
        }
        return ends;
    }

    private static boolean clipEdge(double p, double q, double[] range) {
        if (p == 0) {
            return q >= 0;
        }
        double t = q / p;
        if (p < 0) {
            range[0] = Math.max(range[0], t);
        } else {
            range[1] = Math.min(range[1], t);
        }
        return range[0] <= range[1];
    }

    /**
     * Rounds a position to the nearest cell as {@link Math#round(double)} does. NaN and
     * infinities are kept, and so are positions from 2^52 up: they are whole numbers already,
     * but may not fit in a long.
     */
    private static double roundToCell(double position) {
        if (Double.isNaN(position) || Math.abs(position) >= 0x1p52) {
            return position;
        }
        return Math.round(position);
    }

    private static boolean isFinite(double x, double y) {
        return Double.isFinite(x) && Double.isFinite(y);
    }

    private String getLineChar(long dx, long dy, int sx, int sy) {
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

    private boolean isInBounds(Rect area, long x, long y) {
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

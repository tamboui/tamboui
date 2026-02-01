/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.columns;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.widget.Widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.tamboui.util.CollectionUtil.listCopyOf;

/**
 * A grid layout widget that arranges children into columns.
 * <p>
 * The column count must be set explicitly (no auto-detection at the widget level).
 * Children are rendered in grid cells whose positions are determined by the
 * {@linkplain ColumnOrder ordering mode}.
 * <p>
 * Column widths default to {@link Constraint#fill()} for each column but can
 * be overridden via {@link Builder#columnWidths(Constraint...)}.
 * Row heights default to equal distribution of available height but can be
 * overridden via {@link Builder#rowHeights(int...)}.
 *
 * <pre>{@code
 * Columns columns = Columns.builder()
 *     .children(widget1, widget2, widget3, widget4)
 *     .columnCount(2)
 *     .spacing(1)
 *     .build();
 *
 * columns.render(area, buffer);
 * }</pre>
 */
public final class Columns implements Widget {

    private final List<Widget> children;
    private final int columnCount;
    private final int spacing;
    private final Flex flex;
    private final ColumnOrder order;
    private final List<Constraint> columnWidths;
    private final int[] rowHeights;

    private Columns(Builder builder) {
        this.children = listCopyOf(builder.children);
        this.columnCount = builder.columnCount;
        this.spacing = builder.spacing;
        this.flex = builder.flex;
        this.order = builder.order;
        this.columnWidths = builder.columnWidths != null ? listCopyOf(builder.columnWidths) : null;
        this.rowHeights = builder.rowHeights != null ? builder.rowHeights.clone() : null;
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        if (area.isEmpty() || children.isEmpty() || columnCount <= 0) {
            return;
        }

        int cols = Math.min(columnCount, children.size());
        int rows = (children.size() + cols - 1) / cols;

        // Build horizontal constraints with spacing gaps
        List<Constraint> hConstraints = buildHorizontalConstraints(cols);

        List<Rect> colAreas = Layout.horizontal()
            .constraints(hConstraints)
            .flex(flex)
            .split(area);

        // Extract only the column areas (skip spacing areas)
        List<Rect> columnRects = extractColumnRects(colAreas);

        // Compute per-row heights
        int[] heights = computeRowHeights(rows, area.height());

        // Render each child in its cell
        int currentY = area.y();
        for (int row = 0; row < rows; row++) {
            if (currentY >= area.bottom()) {
                break;
            }
            int rowHeight = Math.min(heights[row], area.bottom() - currentY);
            for (int col = 0; col < cols && col < columnRects.size(); col++) {
                int childIndex = order.resolveIndex(row, col, rows, cols);
                if (childIndex < children.size()) {
                    Widget child = children.get(childIndex);
                    Rect colRect = columnRects.get(col);
                    Rect cellArea = new Rect(colRect.x(), currentY, colRect.width(), rowHeight);
                    child.render(cellArea, buffer);
                }
            }
            currentY += rowHeight;
        }
    }

    private List<Constraint> buildHorizontalConstraints(int cols) {
        List<Constraint> constraints = new ArrayList<>();
        for (int c = 0; c < cols; c++) {
            if (columnWidths != null && c < columnWidths.size()) {
                constraints.add(columnWidths.get(c));
            } else {
                constraints.add(Constraint.fill());
            }
            if (spacing > 0 && c < cols - 1) {
                constraints.add(Constraint.length(spacing));
            }
        }
        return constraints;
    }

    private List<Rect> extractColumnRects(List<Rect> colAreas) {
        List<Rect> columnRects = new ArrayList<>();
        for (int i = 0; i < colAreas.size(); i++) {
            if (spacing > 0 && i % 2 == 1) {
                continue;
            }
            columnRects.add(colAreas.get(i));
        }
        return columnRects;
    }

    private int[] computeRowHeights(int rows, int availableHeight) {
        int[] heights = new int[rows];
        if (rowHeights != null) {
            // Use provided row heights, capped to available height
            int remaining = availableHeight;
            for (int i = 0; i < rows; i++) {
                if (i < rowHeights.length) {
                    heights[i] = Math.min(rowHeights[i], remaining);
                } else {
                    heights[i] = Math.min(1, remaining);
                }
                remaining = Math.max(0, remaining - heights[i]);
            }
        } else {
            // Equal distribution
            int baseHeight = availableHeight / rows;
            int remainder = availableHeight % rows;
            for (int i = 0; i < rows; i++) {
                heights[i] = baseHeight + (i < remainder ? 1 : 0);
            }
        }
        return heights;
    }

    /**
     * Builder for {@link Columns}.
     */
    public static final class Builder {
        private final List<Widget> children = new ArrayList<>();
        private int columnCount = 1;
        private int spacing = 0;
        private Flex flex = Flex.START;
        private ColumnOrder order = ColumnOrder.ROW_FIRST;
        private List<Constraint> columnWidths;
        private int[] rowHeights;

        private Builder() {
        }

        /**
         * Sets the children widgets.
         *
         * @param children the child widgets
         * @return this builder
         */
        public Builder children(Widget... children) {
            this.children.clear();
            this.children.addAll(Arrays.asList(children));
            return this;
        }

        /**
         * Sets the children widgets from a list.
         *
         * @param children the child widgets
         * @return this builder
         */
        public Builder children(List<Widget> children) {
            this.children.clear();
            this.children.addAll(children);
            return this;
        }

        /**
         * Sets the number of columns.
         *
         * @param count the column count (must be at least 1)
         * @return this builder
         */
        public Builder columnCount(int count) {
            this.columnCount = Math.max(1, count);
            return this;
        }

        /**
         * Sets the spacing between columns in cells.
         *
         * @param spacing the spacing (0 or more)
         * @return this builder
         */
        public Builder spacing(int spacing) {
            this.spacing = Math.max(0, spacing);
            return this;
        }

        /**
         * Sets how remaining space is distributed among columns.
         *
         * @param flex the flex mode
         * @return this builder
         */
        public Builder flex(Flex flex) {
            this.flex = flex;
            return this;
        }

        /**
         * Sets the ordering mode for children in the grid.
         *
         * @param order the column ordering mode
         * @return this builder
         */
        public Builder order(ColumnOrder order) {
            this.order = order;
            return this;
        }

        /**
         * Sets the width constraints for columns.
         * <p>
         * If not set, all columns use {@link Constraint#fill()}.
         * If fewer constraints than columns are provided, remaining columns use
         * {@link Constraint#fill()}.
         *
         * @param widths the column width constraints
         * @return this builder
         */
        public Builder columnWidths(Constraint... widths) {
            this.columnWidths = Arrays.asList(widths);
            return this;
        }

        /**
         * Sets the width constraints for columns from a list.
         *
         * @param widths the column width constraints
         * @return this builder
         */
        public Builder columnWidths(List<Constraint> widths) {
            this.columnWidths = new ArrayList<>(widths);
            return this;
        }

        /**
         * Sets explicit row heights.
         * <p>
         * If not set, rows share the available height equally.
         * If fewer heights than rows are provided, remaining rows default to 1.
         *
         * @param heights the row heights
         * @return this builder
         */
        public Builder rowHeights(int... heights) {
            this.rowHeights = heights.clone();
            return this;
        }

        /**
         * Builds the {@link Columns} widget.
         *
         * @return a new Columns widget
         */
        public Columns build() {
            return new Columns(this);
        }
    }
}

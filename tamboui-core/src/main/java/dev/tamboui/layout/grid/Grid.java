/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.LayoutException;
import dev.tamboui.layout.Rect;
import dev.tamboui.widget.Widget;

import static dev.tamboui.util.CollectionUtil.listCopyOf;

/**
 * A CSS Grid-inspired layout widget that arranges children into a grid
 * with explicit control over grid dimensions, per-column/per-row sizing
 * constraints, and gutter spacing.
 * <p>
 * Grid supports two mutually exclusive modes:
 * <ul>
 *   <li><b>Children mode</b>: Sequential placement using {@code children()} and {@code columnCount()}</li>
 *   <li><b>Area mode</b>: Template-based placement using {@code gridAreas()} and {@code area()}</li>
 * </ul>
 * <p>
 * The staged builder pattern ensures compile-time safety - you cannot mix modes.
 *
 * <h2>Children Mode Example</h2>
 * <pre>{@code
 * Grid grid = Grid.builder()
 *     .children(widget1, widget2, widget3, widget4)
 *     .columnCount(2)
 *     .horizontalGutter(1)
 *     .verticalGutter(1)
 *     .build();
 * }</pre>
 *
 * <h2>Area Mode Example</h2>
 * <pre>{@code
 * Grid grid = Grid.builder()
 *     .gridAreas("header header header",
 *                "nav    main   main",
 *                "nav    main   main",
 *                "footer footer footer")
 *     .area("header", headerWidget)
 *     .area("nav", navWidget)
 *     .area("main", mainWidget)
 *     .area("footer", footerWidget)
 *     .horizontalGutter(1)
 *     .verticalGutter(1)
 *     .build();
 * }</pre>
 */
public final class Grid implements Widget {

    // Children mode fields
    private final List<Widget> children;
    private final int columnCount;
    private final int[] rowHeights;

    // Area mode fields
    private final GridArea gridArea;
    private final Map<String, Widget> areaWidgets;

    // Common fields
    private final int horizontalGutter;
    private final int verticalGutter;
    private final Flex flex;
    private final List<Constraint> columnConstraints;
    private final List<Constraint> rowConstraints;

    private Grid(List<Widget> children, int columnCount, int[] rowHeights,
                 GridArea gridArea, Map<String, Widget> areaWidgets,
                 int horizontalGutter, int verticalGutter, Flex flex,
                 List<Constraint> columnConstraints, List<Constraint> rowConstraints) {
        this.children = children;
        this.columnCount = columnCount;
        this.rowHeights = rowHeights;
        this.gridArea = gridArea;
        this.areaWidgets = areaWidgets;
        this.horizontalGutter = horizontalGutter;
        this.verticalGutter = verticalGutter;
        this.flex = flex;
        this.columnConstraints = columnConstraints;
        this.rowConstraints = rowConstraints;
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
        if (area.isEmpty()) {
            return;
        }

        // Area-based rendering
        if (gridArea != null) {
            renderWithAreas(area, buffer);
            return;
        }

        // Children-based rendering
        if (children.isEmpty() || columnCount <= 0) {
            return;
        }

        int cols = Math.min(columnCount, children.size());
        int rows = (children.size() + cols - 1) / cols;

        // Build horizontal constraints with gutter gaps
        List<Constraint> hConstraints = buildHorizontalConstraints(cols);

        List<Rect> colAreas = Layout.horizontal()
            .constraints(hConstraints)
            .flex(flex)
            .split(area);

        // Extract only column areas (skip gutter areas)
        List<Rect> columnRects = extractColumnRects(colAreas);

        if (rowConstraints != null && !rowConstraints.isEmpty()) {
            renderWithRowConstraints(area, buffer, rows, cols, columnRects);
        } else if (rowHeights != null) {
            renderWithExplicitRowHeights(area, buffer, rows, cols, columnRects);
        } else {
            renderWithEqualRowHeights(area, buffer, rows, cols, columnRects);
        }
    }

    private void renderWithAreas(Rect area, Buffer buffer) {
        if (areaWidgets == null || areaWidgets.isEmpty()) {
            return;
        }

        int cols = gridArea.columns();
        int rows = gridArea.rows();

        // Build horizontal constraints with gutter gaps
        List<Constraint> hConstraints = buildHorizontalConstraintsForAreas(cols);

        List<Rect> colAreas = Layout.horizontal()
            .constraints(hConstraints)
            .flex(flex)
            .split(area);

        // Extract only column areas (skip gutter areas)
        List<Rect> columnRects = extractColumnRects(colAreas);

        // Compute row heights and Y positions
        int[] heights;
        if (rowConstraints != null && !rowConstraints.isEmpty()) {
            heights = computeRowHeightsFromConstraints(area, rows);
        } else {
            heights = computeEqualRowHeightsForAreas(area.height(), rows);
        }

        int[] rowYPositions = computeRowYPositions(area.y(), heights, rows);

        // Render each named area
        for (Map.Entry<String, Widget> entry : areaWidgets.entrySet()) {
            String areaName = entry.getKey();
            Widget widget = entry.getValue();
            GridArea.AreaBounds bounds = gridArea.boundsFor(areaName);

            if (bounds == null) {
                // Should not happen - validated at build time
                continue;
            }

            // Calculate merged cell rect
            Rect cellRect = computeMergedCellRect(bounds, columnRects, rowYPositions, heights);
            widget.render(cellRect, buffer);
        }
    }

    private List<Constraint> buildHorizontalConstraintsForAreas(int cols) {
        List<Constraint> constraints = new ArrayList<>();
        for (int c = 0; c < cols; c++) {
            if (columnConstraints != null && !columnConstraints.isEmpty()) {
                constraints.add(columnConstraints.get(c % columnConstraints.size()));
            } else {
                constraints.add(Constraint.fill());
            }
            if (horizontalGutter > 0 && c < cols - 1) {
                constraints.add(Constraint.length(horizontalGutter));
            }
        }
        return constraints;
    }

    private int[] computeRowHeightsFromConstraints(Rect area, int rows) {
        // Build row constraints with gutters
        List<Constraint> vConstraints = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            vConstraints.add(rowConstraints.get(r % rowConstraints.size()));
            if (verticalGutter > 0 && r < rows - 1) {
                vConstraints.add(Constraint.length(verticalGutter));
            }
        }

        List<Rect> allRowAreas = Layout.vertical()
            .constraints(vConstraints)
            .flex(flex)
            .split(area);

        // Extract row heights (skip gutter areas)
        int[] heights = new int[rows];
        int rowIndex = 0;
        for (int i = 0; i < allRowAreas.size() && rowIndex < rows; i++) {
            if (verticalGutter > 0 && i % 2 == 1) {
                continue;
            }
            heights[rowIndex++] = allRowAreas.get(i).height();
        }
        return heights;
    }

    private int[] computeEqualRowHeightsForAreas(int availableHeight, int rows) {
        int totalGutter = verticalGutter * (rows - 1);
        int distributable = Math.max(0, availableHeight - totalGutter);
        int baseHeight = distributable / rows;
        int remainder = distributable % rows;

        int[] heights = new int[rows];
        for (int i = 0; i < rows; i++) {
            heights[i] = baseHeight + (i < remainder ? 1 : 0);
        }
        return heights;
    }

    private int[] computeRowYPositions(int startY, int[] heights, int rows) {
        int[] positions = new int[rows];
        int currentY = startY;
        for (int r = 0; r < rows; r++) {
            positions[r] = currentY;
            currentY += heights[r] + verticalGutter;
        }
        return positions;
    }

    private Rect computeMergedCellRect(GridArea.AreaBounds bounds,
            List<Rect> columnRects, int[] rowYPositions, int[] rowHeights) {

        int startCol = bounds.column();
        int endCol = bounds.endColumn() - 1;
        int startRow = bounds.row();
        int endRow = bounds.endRow() - 1;

        // Clamp to available columns/rows
        if (startCol >= columnRects.size() || startRow >= rowYPositions.length) {
            return new Rect(0, 0, 0, 0);
        }
        endCol = Math.min(endCol, columnRects.size() - 1);
        endRow = Math.min(endRow, rowYPositions.length - 1);

        // X position from first column
        int x = columnRects.get(startCol).x();

        // Width spans from first column start to last column end (includes gutters between)
        int endX = columnRects.get(endCol).right();
        int width = endX - x;

        // Y position from first row
        int y = rowYPositions[startRow];

        // Height spans rows plus gutters between them
        int totalHeight = 0;
        for (int r = startRow; r <= endRow; r++) {
            totalHeight += rowHeights[r];
            if (r < endRow) {
                totalHeight += verticalGutter;
            }
        }

        return new Rect(x, y, width, totalHeight);
    }

    private void renderWithRowConstraints(Rect area, Buffer buffer, int rows, int cols,
                                          List<Rect> columnRects) {
        // Build row constraints: cycle rowConstraints over rows, interleave with vertical gutter
        List<Constraint> vConstraints = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            vConstraints.add(rowConstraints.get(r % rowConstraints.size()));
            if (verticalGutter > 0 && r < rows - 1) {
                vConstraints.add(Constraint.length(verticalGutter));
            }
        }

        List<Rect> allRowAreas = Layout.vertical()
            .constraints(vConstraints)
            .flex(flex)
            .split(area);

        // Extract only row areas (skip gutter areas)
        List<Rect> rowRects = new ArrayList<>();
        for (int i = 0; i < allRowAreas.size(); i++) {
            if (verticalGutter > 0 && i % 2 == 1) {
                continue;
            }
            rowRects.add(allRowAreas.get(i));
        }

        // Render children
        for (int row = 0; row < rows && row < rowRects.size(); row++) {
            Rect rowRect = rowRects.get(row);
            for (int col = 0; col < cols && col < columnRects.size(); col++) {
                int childIndex = row * cols + col;
                if (childIndex < children.size()) {
                    Rect colRect = columnRects.get(col);
                    Rect cellArea = new Rect(colRect.x(), rowRect.y(), colRect.width(), rowRect.height());
                    children.get(childIndex).render(cellArea, buffer);
                }
            }
        }
    }

    private void renderWithExplicitRowHeights(Rect area, Buffer buffer, int rows, int cols,
                                              List<Rect> columnRects) {
        int[] heights = computeRowHeights(rows, area.height());
        renderRows(area, buffer, rows, cols, columnRects, heights);
    }

    private void renderWithEqualRowHeights(Rect area, Buffer buffer, int rows, int cols,
                                           List<Rect> columnRects) {
        int availableHeight = area.height() - verticalGutter * (rows - 1);
        int baseHeight = Math.max(0, availableHeight) / rows;
        int remainder = Math.max(0, availableHeight) % rows;
        int[] heights = new int[rows];
        for (int i = 0; i < rows; i++) {
            heights[i] = baseHeight + (i < remainder ? 1 : 0);
        }
        renderRows(area, buffer, rows, cols, columnRects, heights);
    }

    private void renderRows(Rect area, Buffer buffer, int rows, int cols,
                            List<Rect> columnRects, int[] heights) {
        int currentY = area.y();
        for (int row = 0; row < rows; row++) {
            if (currentY >= area.bottom()) {
                break;
            }
            int rowHeight = Math.min(heights[row], area.bottom() - currentY);
            for (int col = 0; col < cols && col < columnRects.size(); col++) {
                int childIndex = row * cols + col;
                if (childIndex < children.size()) {
                    Rect colRect = columnRects.get(col);
                    Rect cellArea = new Rect(colRect.x(), currentY, colRect.width(), rowHeight);
                    children.get(childIndex).render(cellArea, buffer);
                }
            }
            currentY += rowHeight + verticalGutter;
        }
    }

    private List<Constraint> buildHorizontalConstraints(int cols) {
        List<Constraint> constraints = new ArrayList<>();
        for (int c = 0; c < cols; c++) {
            if (columnConstraints != null && !columnConstraints.isEmpty()) {
                constraints.add(columnConstraints.get(c % columnConstraints.size()));
            } else {
                constraints.add(Constraint.fill());
            }
            if (horizontalGutter > 0 && c < cols - 1) {
                constraints.add(Constraint.length(horizontalGutter));
            }
        }
        return constraints;
    }

    private List<Rect> extractColumnRects(List<Rect> colAreas) {
        List<Rect> columnRects = new ArrayList<>();
        for (int i = 0; i < colAreas.size(); i++) {
            if (horizontalGutter > 0 && i % 2 == 1) {
                continue;
            }
            columnRects.add(colAreas.get(i));
        }
        return columnRects;
    }

    private int[] computeRowHeights(int rows, int availableHeight) {
        int[] heights = new int[rows];
        int remaining = availableHeight - verticalGutter * (rows - 1);
        remaining = Math.max(0, remaining);
        for (int i = 0; i < rows; i++) {
            if (rowHeights != null && i < rowHeights.length) {
                heights[i] = Math.min(rowHeights[i], remaining);
            } else {
                heights[i] = Math.min(1, remaining);
            }
            remaining = Math.max(0, remaining - heights[i]);
        }
        return heights;
    }

    /**
     * Initial builder for {@link Grid}.
     * <p>
     * Call {@link #gridAreas(String...)} for area-based layout or
     * {@link #children(Widget...)} for children-based layout.
     */
    public static final class Builder {

        private Builder() {
        }

        /**
         * Creates an area-based grid layout using CSS grid-template-areas style.
         * <p>
         * Each string represents a row; tokens are space-separated area names.
         * Use "." for empty cells. Named areas must form contiguous rectangles.
         *
         * @param rowTemplates the row templates (e.g., "A A B", "A A C")
         * @return an AreaBuilder for further configuration
         * @throws LayoutException if the template is invalid
         */
        public AreaBuilder gridAreas(String... rowTemplates) {
            GridArea gridArea = GridArea.parse(rowTemplates);
            return new AreaBuilder(gridArea);
        }

        /**
         * Creates a children-based grid layout with sequential placement.
         *
         * @param children the child widgets
         * @return a ChildrenBuilder for further configuration
         */
        public ChildrenBuilder children(Widget... children) {
            return new ChildrenBuilder(Arrays.asList(children));
        }

        /**
         * Creates a children-based grid layout with sequential placement.
         *
         * @param children the child widgets
         * @return a ChildrenBuilder for further configuration
         */
        public ChildrenBuilder children(List<Widget> children) {
            return new ChildrenBuilder(new ArrayList<>(children));
        }
    }

    /**
     * Builder for area-based grid layouts.
     */
    public static final class AreaBuilder {
        private final GridArea gridArea;
        private final Map<String, Widget> areaWidgets = new LinkedHashMap<>();
        private int horizontalGutter = 0;
        private int verticalGutter = 0;
        private Flex flex = Flex.START;
        private List<Constraint> columnConstraints;
        private List<Constraint> rowConstraints;

        private AreaBuilder(GridArea gridArea) {
            this.gridArea = gridArea;
        }

        /**
         * Assigns a widget to a named area.
         * <p>
         * Areas without assigned widgets render as empty space.
         *
         * @param areaName the area name from the template
         * @param widget the widget to place in that area
         * @return this builder
         * @throws LayoutException if the area name is not defined in the template
         */
        public AreaBuilder area(String areaName, Widget widget) {
            if (gridArea.boundsFor(areaName) == null) {
                throw new LayoutException("Widget assigned to undefined area '" + areaName + "'");
            }
            areaWidgets.put(areaName, widget);
            return this;
        }

        /**
         * Sets the horizontal gutter between columns.
         *
         * @param gutter the horizontal gutter in cells
         * @return this builder
         */
        public AreaBuilder horizontalGutter(int gutter) {
            this.horizontalGutter = Math.max(0, gutter);
            return this;
        }

        /**
         * Sets the vertical gutter between rows.
         *
         * @param gutter the vertical gutter in cells
         * @return this builder
         */
        public AreaBuilder verticalGutter(int gutter) {
            this.verticalGutter = Math.max(0, gutter);
            return this;
        }

        /**
         * Sets how remaining space is distributed.
         *
         * @param flex the flex mode
         * @return this builder
         */
        public AreaBuilder flex(Flex flex) {
            this.flex = flex;
            return this;
        }

        /**
         * Sets the width constraints for columns.
         * <p>
         * Constraints cycle when fewer than the column count.
         *
         * @param constraints the column width constraints
         * @return this builder
         */
        public AreaBuilder columnConstraints(Constraint... constraints) {
            this.columnConstraints = Arrays.asList(constraints);
            return this;
        }

        /**
         * Sets the width constraints for columns from a list.
         *
         * @param constraints the column width constraints
         * @return this builder
         */
        public AreaBuilder columnConstraints(List<Constraint> constraints) {
            this.columnConstraints = new ArrayList<>(constraints);
            return this;
        }

        /**
         * Sets the height constraints for rows.
         * <p>
         * Constraints cycle when fewer than the row count.
         *
         * @param constraints the row height constraints
         * @return this builder
         */
        public AreaBuilder rowConstraints(Constraint... constraints) {
            this.rowConstraints = Arrays.asList(constraints);
            return this;
        }

        /**
         * Sets the height constraints for rows from a list.
         *
         * @param constraints the row height constraints
         * @return this builder
         */
        public AreaBuilder rowConstraints(List<Constraint> constraints) {
            this.rowConstraints = new ArrayList<>(constraints);
            return this;
        }

        /**
         * Builds the {@link Grid} widget.
         *
         * @return a new Grid widget
         */
        public Grid build() {
            return new Grid(
                null, 0, null,
                gridArea, new LinkedHashMap<>(areaWidgets),
                horizontalGutter, verticalGutter, flex,
                columnConstraints != null ? listCopyOf(columnConstraints) : null,
                rowConstraints != null ? listCopyOf(rowConstraints) : null
            );
        }
    }

    /**
     * Builder for children-based grid layouts.
     */
    public static final class ChildrenBuilder {
        private final List<Widget> children;
        private int columnCount = 1;
        private Integer rowCount;
        private int horizontalGutter = 0;
        private int verticalGutter = 0;
        private Flex flex = Flex.START;
        private List<Constraint> columnConstraints;
        private List<Constraint> rowConstraints;
        private int[] rowHeights;

        private ChildrenBuilder(List<Widget> children) {
            this.children = children;
        }

        /**
         * Sets the number of columns.
         *
         * @param count the column count (must be at least 1)
         * @return this builder
         */
        public ChildrenBuilder columnCount(int count) {
            this.columnCount = Math.max(1, count);
            return this;
        }

        /**
         * Sets the number of rows.
         * <p>
         * When set, validates that children fit within columns × rows cells.
         * When not set, rows expand automatically to fit all children.
         *
         * @param count the row count (must be at least 1)
         * @return this builder
         */
        public ChildrenBuilder rowCount(int count) {
            this.rowCount = Math.max(1, count);
            return this;
        }

        /**
         * Sets the horizontal gutter between columns.
         *
         * @param gutter the horizontal gutter in cells
         * @return this builder
         */
        public ChildrenBuilder horizontalGutter(int gutter) {
            this.horizontalGutter = Math.max(0, gutter);
            return this;
        }

        /**
         * Sets the vertical gutter between rows.
         *
         * @param gutter the vertical gutter in cells
         * @return this builder
         */
        public ChildrenBuilder verticalGutter(int gutter) {
            this.verticalGutter = Math.max(0, gutter);
            return this;
        }

        /**
         * Sets how remaining space is distributed.
         *
         * @param flex the flex mode
         * @return this builder
         */
        public ChildrenBuilder flex(Flex flex) {
            this.flex = flex;
            return this;
        }

        /**
         * Sets the width constraints for columns.
         * <p>
         * Constraints cycle when fewer than the column count.
         *
         * @param constraints the column width constraints
         * @return this builder
         */
        public ChildrenBuilder columnConstraints(Constraint... constraints) {
            this.columnConstraints = Arrays.asList(constraints);
            return this;
        }

        /**
         * Sets the width constraints for columns from a list.
         *
         * @param constraints the column width constraints
         * @return this builder
         */
        public ChildrenBuilder columnConstraints(List<Constraint> constraints) {
            this.columnConstraints = new ArrayList<>(constraints);
            return this;
        }

        /**
         * Sets the height constraints for rows.
         * <p>
         * When set, row heights are determined by the layout solver
         * rather than by children preferred heights or equal distribution.
         * Constraints cycle when fewer than the row count.
         *
         * @param constraints the row height constraints
         * @return this builder
         */
        public ChildrenBuilder rowConstraints(Constraint... constraints) {
            this.rowConstraints = Arrays.asList(constraints);
            return this;
        }

        /**
         * Sets the height constraints for rows from a list.
         *
         * @param constraints the row height constraints
         * @return this builder
         */
        public ChildrenBuilder rowConstraints(List<Constraint> constraints) {
            this.rowConstraints = new ArrayList<>(constraints);
            return this;
        }

        /**
         * Sets explicit row heights.
         * <p>
         * If not set and no row constraints are set, rows share the available height equally.
         * If fewer heights than rows are provided, remaining rows default to 1.
         *
         * @param heights the row heights
         * @return this builder
         */
        public ChildrenBuilder rowHeights(int... heights) {
            this.rowHeights = heights.clone();
            return this;
        }

        /**
         * Builds the {@link Grid} widget.
         *
         * @return a new Grid widget
         * @throws LayoutException if rowCount is set and children exceed available cells
         */
        public Grid build() {
            // Validate children fit within grid when rowCount is set
            if (rowCount != null) {
                int maxCells = columnCount * rowCount;
                if (children.size() > maxCells) {
                    throw new LayoutException(String.format(
                        "Grid has %d children but only %d cells (%d columns * %d rows)",
                        children.size(), maxCells, columnCount, rowCount));
                }
            }

            return new Grid(
                listCopyOf(children), columnCount,
                rowHeights != null ? rowHeights.clone() : null,
                null, null,
                horizontalGutter, verticalGutter, flex,
                columnConstraints != null ? listCopyOf(columnConstraints) : null,
                rowConstraints != null ? listCopyOf(rowConstraints) : null
            );
        }
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.toolkit.element.ContainerElement;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.IntegerConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widget.Widget;
import dev.tamboui.layout.columns.ColumnOrder;
import dev.tamboui.layout.columns.Columns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A multi-column grid layout container that arranges children into columns.
 * <p>
 * The column count is auto-calculated from the available width and the maximum
 * preferred width of children, or can be set explicitly via {@link #columnCount(int)}.
 * <p>
 * Items can be ordered row-first (default) or column-first:
 * <ul>
 *   <li>Row-first: items fill left-to-right, then top-to-bottom (like reading text)</li>
 *   <li>Column-first: items fill top-to-bottom, then left-to-right (like newspaper columns)</li>
 * </ul>
 * <p>
 * All layout properties can be set via CSS or programmatically.
 * Programmatic values override CSS values when both are set.
 * <p>
 * Supported CSS properties:
 * <ul>
 *   <li>{@code flex} — "start", "center", "end", "space-between", "space-around", "space-evenly"</li>
 *   <li>{@code spacing} — gap between columns in cells</li>
 *   <li>{@code margin} — margin around the columns</li>
 *   <li>{@code column-count} — explicit number of columns</li>
 *   <li>{@code column-order} — "row-first" or "column-first"</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * columns(text("A"), text("B"), text("C"), text("D")).columnCount(2).spacing(1)
 * </pre>
 */
public final class ColumnsElement extends ContainerElement<ColumnsElement> {

    /**
     * CSS property definition for the number of columns.
     */
    public static final PropertyDefinition<Integer> COLUMN_COUNT =
        PropertyDefinition.of("column-count", IntegerConverter.INSTANCE);

    /**
     * CSS property definition for the column ordering mode.
     */
    public static final PropertyDefinition<ColumnOrder> COLUMN_ORDER =
        PropertyDefinition.of("column-order", ColumnOrderConverter.INSTANCE);

    static {
        PropertyRegistry.registerAll(COLUMN_COUNT, COLUMN_ORDER);
    }

    private Integer spacing;
    private Flex flex;
    private Margin margin;
    private Integer columnCount;
    private ColumnOrder order;

    /**
     * Creates an empty columns layout.
     */
    public ColumnsElement() {
    }

    /**
     * Creates a columns layout with the given children.
     *
     * @param children the child elements
     */
    public ColumnsElement(Element... children) {
        this.children.addAll(Arrays.asList(children));
    }

    /**
     * Creates a columns layout with the given children.
     *
     * @param children the child elements
     */
    public ColumnsElement(Collection<? extends Element> children) {
        this.children.addAll(children);
    }

    /**
     * Sets the spacing between columns.
     * <p>
     * Can also be set via CSS {@code spacing} property.
     *
     * @param spacing spacing in cells between adjacent columns
     * @return this columns layout for method chaining
     */
    public ColumnsElement spacing(int spacing) {
        this.spacing = Math.max(0, spacing);
        return this;
    }

    /**
     * Sets how remaining space is distributed among columns.
     * <p>
     * Can also be set via CSS {@code flex} property.
     *
     * @param flex the flex mode for space distribution
     * @return this columns layout for method chaining
     * @see Flex
     */
    public ColumnsElement flex(Flex flex) {
        this.flex = flex;
        return this;
    }

    /**
     * Sets the margin around the columns layout.
     * <p>
     * Can also be set via CSS {@code margin} property.
     *
     * @param margin the margin
     * @return this columns layout for method chaining
     */
    public ColumnsElement margin(Margin margin) {
        this.margin = margin;
        return this;
    }

    /**
     * Sets uniform margin around the columns layout.
     *
     * @param value the margin value for all sides
     * @return this columns layout for method chaining
     */
    public ColumnsElement margin(int value) {
        this.margin = Margin.uniform(value);
        return this;
    }

    /**
     * Sets an explicit column count, overriding auto-detection.
     * <p>
     * Can also be set via CSS {@code column-count} property.
     *
     * @param count the number of columns
     * @return this columns layout for method chaining
     */
    public ColumnsElement columnCount(int count) {
        this.columnCount = Math.max(1, count);
        return this;
    }

    /**
     * Sets the ordering mode for children in the grid.
     * <p>
     * Can also be set via CSS {@code column-order} property.
     *
     * @param order the column ordering mode
     * @return this columns layout for method chaining
     * @see ColumnOrder
     */
    public ColumnsElement order(ColumnOrder order) {
        this.order = order;
        return this;
    }

    /**
     * Convenience method to set column-first ordering.
     * <p>
     * Equivalent to {@code order(ColumnOrder.COLUMN_FIRST)}.
     *
     * @return this columns layout for method chaining
     */
    public ColumnsElement columnFirst() {
        this.order = ColumnOrder.COLUMN_FIRST;
        return this;
    }

    @Override
    public int preferredWidth() {
        if (children.isEmpty()) {
            return 0;
        }

        int maxChildWidth = 0;
        for (Element child : children) {
            maxChildWidth = Math.max(maxChildWidth, child.preferredWidth());
        }

        int cols = columnCount != null ? columnCount : children.size();
        cols = Math.min(cols, children.size());

        int effectiveSpacing = this.spacing != null ? this.spacing : 0;
        int width = maxChildWidth * cols + effectiveSpacing * (cols - 1);

        if (margin != null) {
            width += margin.left() + margin.right();
        }

        return width;
    }

    @Override
    public int preferredHeight(int availableWidth, RenderContext context) {
        if (children.isEmpty() || availableWidth <= 0) {
            return 0;
        }

        int effectiveSpacing = this.spacing != null ? this.spacing : 0;
        int cols = computeColumnCount(availableWidth, effectiveSpacing, this.columnCount);
        int rows = (children.size() + cols - 1) / cols;
        ColumnOrder effectiveOrder = this.order != null ? this.order : ColumnOrder.ROW_FIRST;

        int totalHeight = 0;
        for (int row = 0; row < rows; row++) {
            int rowHeight = 1;
            for (int col = 0; col < cols; col++) {
                int childIndex = effectiveOrder.resolveIndex(row, col, rows, cols);
                if (childIndex < children.size()) {
                    Element child = children.get(childIndex);
                    int childWidth = Math.max(1, (availableWidth - effectiveSpacing * (cols - 1)) / cols);
                    rowHeight = Math.max(rowHeight, child.preferredHeight(childWidth, context));
                }
            }
            totalHeight += rowHeight;
        }

        return totalHeight;
    }

    /**
     * Computes the number of columns based on available width and child widths.
     *
     * @param availableWidth the available width in cells
     * @param spacing        the spacing between columns
     * @param explicitCount  explicit column count override, or {@code null} for auto-detection
     * @return the computed column count
     */
    private int computeColumnCount(int availableWidth, int spacing, Integer explicitCount) {
        if (explicitCount != null) {
            return Math.min(explicitCount, children.size());
        }

        int maxChildWidth = 0;
        for (Element child : children) {
            maxChildWidth = Math.max(maxChildWidth, child.preferredWidth());
        }

        if (maxChildWidth <= 0) {
            return children.size();
        }

        int cols = (availableWidth + spacing) / (maxChildWidth + spacing);
        return Math.max(1, Math.min(cols, children.size()));
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (children.isEmpty()) {
            return;
        }

        // Get CSS resolver for property resolution
        CssStyleResolver cssResolver = context.resolveStyle(this).orElse(null);

        // Resolve margin: programmatic > CSS > none
        Margin effectiveMargin = this.margin;
        if (effectiveMargin == null && cssResolver != null) {
            effectiveMargin = cssResolver.margin().orElse(null);
        }

        // Apply margin to get the effective render area
        Rect effectiveArea = area;
        if (effectiveMargin != null) {
            effectiveArea = effectiveMargin.inner(area);
            if (effectiveArea.isEmpty()) {
                return;
            }
        }

        // Fill background with current style
        Style effectiveStyle = context.currentStyle();
        if (effectiveStyle.bg().isPresent()) {
            frame.buffer().setStyle(effectiveArea, effectiveStyle);
        }

        // Resolve flex: programmatic > CSS > START
        Flex effectiveFlex = this.flex;
        if (effectiveFlex == null && cssResolver != null) {
            effectiveFlex = cssResolver.flex().orElse(Flex.START);
        } else if (effectiveFlex == null) {
            effectiveFlex = Flex.START;
        }

        // Resolve spacing: programmatic > CSS > 0
        int effectiveSpacing = this.spacing != null ? this.spacing : 0;
        if (this.spacing == null && cssResolver != null) {
            effectiveSpacing = cssResolver.spacing().orElse(0);
        }

        // Resolve column-count: programmatic > CSS > auto
        Integer effectiveColumnCount = this.columnCount;
        if (effectiveColumnCount == null && cssResolver != null) {
            effectiveColumnCount = cssResolver.get(COLUMN_COUNT).orElse(null);
        }

        // Resolve column-order: programmatic > CSS > ROW_FIRST
        ColumnOrder effectiveOrder = this.order;
        if (effectiveOrder == null && cssResolver != null) {
            effectiveOrder = cssResolver.get(COLUMN_ORDER).orElse(ColumnOrder.ROW_FIRST);
        } else if (effectiveOrder == null) {
            effectiveOrder = ColumnOrder.ROW_FIRST;
        }

        // Compute column count from effective area
        int cols = computeColumnCount(effectiveArea.width(), effectiveSpacing, effectiveColumnCount);
        int rows = (children.size() + cols - 1) / cols;

        // Build horizontal constraints to compute column widths for preferred height
        List<Constraint> hConstraints = new ArrayList<>();
        for (int c = 0; c < cols; c++) {
            hConstraints.add(Constraint.fill());
            if (effectiveSpacing > 0 && c < cols - 1) {
                hConstraints.add(Constraint.length(effectiveSpacing));
            }
        }

        List<Rect> colAreas = Layout.horizontal()
            .constraints(hConstraints.toArray(new Constraint[0]))
            .flex(effectiveFlex)
            .split(effectiveArea);

        // Extract only the column areas (skip spacing areas)
        List<Rect> columnRects = new ArrayList<>();
        for (int i = 0; i < colAreas.size(); i++) {
            if (effectiveSpacing > 0 && i % 2 == 1) {
                continue;
            }
            columnRects.add(colAreas.get(i));
        }

        // Compute per-row heights from children's preferred heights
        int[] rowHeights = new int[rows];
        for (int row = 0; row < rows; row++) {
            int rowHeight = 1;
            for (int col = 0; col < cols; col++) {
                int childIndex = effectiveOrder.resolveIndex(row, col, rows, cols);
                if (childIndex < children.size()) {
                    Element child = children.get(childIndex);
                    int colWidth = col < columnRects.size() ? columnRects.get(col).width() : 1;
                    rowHeight = Math.max(rowHeight, child.preferredHeight(colWidth, context));
                }
            }
            rowHeights[row] = rowHeight;
        }

        // Wrap each child Element as a lambda Widget
        List<Widget> childWidgets = new ArrayList<>(children.size());
        for (Element child : children) {
            childWidgets.add((cellArea, buf) -> context.renderChild(child, frame, cellArea));
        }

        // Build and render the Columns widget
        Columns widget = Columns.builder()
            .children(childWidgets)
            .columnCount(cols)
            .spacing(effectiveSpacing)
            .flex(effectiveFlex)
            .order(effectiveOrder)
            .rowHeights(rowHeights)
            .build();

        frame.renderWidget(widget, effectiveArea);
    }
}

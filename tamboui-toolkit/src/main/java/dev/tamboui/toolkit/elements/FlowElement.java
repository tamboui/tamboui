/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.IntegerConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.ContainerElement;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.widget.Widget;
import dev.tamboui.layout.flow.Flow;
import dev.tamboui.layout.flow.FlowItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A wrap layout element where children flow left-to-right and wrap to the
 * next line when exceeding the available width.
 * <p>
 * Unlike the widget-level {@link Flow} which requires explicit item widths,
 * this element auto-measures children via {@code Element.preferredWidth()}.
 * <p>
 * All layout properties can be set via CSS or programmatically.
 * Programmatic values override CSS values when both are set.
 * <p>
 * Supported CSS properties:
 * <ul>
 *   <li>{@code spacing} — horizontal spacing between items on the same row</li>
 *   <li>{@code flow-row-spacing} — vertical spacing between wrapped rows</li>
 *   <li>{@code margin} — margin around the flow layout</li>
 *   <li>{@code background} — background color fill</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * flow(text("Tag1"), text("Tag2"), text("Tag3"))
 *     .spacing(1)
 *     .rowSpacing(1)
 * </pre>
 */
public final class FlowElement extends ContainerElement<FlowElement> {

    /**
     * CSS property definition for vertical spacing between rows.
     */
    public static final PropertyDefinition<Integer> FLOW_ROW_SPACING =
        PropertyDefinition.of("flow-row-spacing", IntegerConverter.INSTANCE);

    static {
        PropertyRegistry.registerAll(FLOW_ROW_SPACING);
    }

    private Integer spacing;
    private Integer rowSpacing;
    private Margin margin;

    /**
     * Creates an empty flow layout.
     */
    public FlowElement() {
    }

    /**
     * Creates a flow layout with the given children.
     *
     * @param children the child elements
     */
    public FlowElement(Element... children) {
        this.children.addAll(Arrays.asList(children));
    }

    /**
     * Creates a flow layout with the given children.
     *
     * @param children the child elements
     */
    public FlowElement(Collection<? extends Element> children) {
        this.children.addAll(children);
    }

    /**
     * Sets the horizontal spacing between items on the same row.
     *
     * @param spacing the horizontal spacing in cells
     * @return this flow for method chaining
     */
    public FlowElement spacing(int spacing) {
        this.spacing = spacing;
        return this;
    }

    /**
     * Sets the vertical spacing between wrapped rows.
     *
     * @param rowSpacing the row spacing in cells
     * @return this flow for method chaining
     */
    public FlowElement rowSpacing(int rowSpacing) {
        this.rowSpacing = rowSpacing;
        return this;
    }

    /**
     * Sets the margin around the flow layout.
     *
     * @param margin the margin
     * @return this flow for method chaining
     */
    public FlowElement margin(Margin margin) {
        this.margin = margin;
        return this;
    }

    /**
     * Sets uniform margin around the flow layout.
     *
     * @param value the margin value for all sides
     * @return this flow for method chaining
     */
    public FlowElement margin(int value) {
        this.margin = Margin.uniform(value);
        return this;
    }

    @Override
    public int preferredWidth() {
        if (children.isEmpty()) {
            return 0;
        }

        int effectiveSpacing = this.spacing != null ? this.spacing : 0;

        // Single-row estimate: sum of all children widths + spacing
        int totalWidth = 0;
        for (int i = 0; i < children.size(); i++) {
            totalWidth += children.get(i).preferredWidth();
            if (i < children.size() - 1) {
                totalWidth += effectiveSpacing;
            }
        }

        if (margin != null) {
            totalWidth += margin.left() + margin.right();
        }

        return totalWidth;
    }

    @Override
    public int preferredHeight(int availableWidth, RenderContext context) {
        if (children.isEmpty() || availableWidth <= 0) {
            return 0;
        }

        int effectiveSpacing = this.spacing != null ? this.spacing : 0;
        int effectiveRowSpacing = this.rowSpacing != null ? this.rowSpacing : 0;

        int effectiveWidth = availableWidth;
        if (margin != null) {
            effectiveWidth -= margin.horizontalTotal();
        }
        if (effectiveWidth <= 0) {
            return 0;
        }

        // Simulate flow to count rows
        int currentX = 0;
        int totalHeight = 0;
        int rowHeight = 0;

        for (Element child : children) {
            int childWidth = child.preferredWidth();

            // Wrap check
            if (currentX + childWidth > effectiveWidth && currentX > 0) {
                totalHeight += rowHeight + effectiveRowSpacing;
                currentX = 0;
                rowHeight = 0;
            }

            int childHeight = child.preferredHeight(childWidth, context);
            rowHeight = Math.max(rowHeight, childHeight);
            currentX += childWidth + effectiveSpacing;
        }

        // Add last row
        totalHeight += rowHeight;

        if (margin != null) {
            totalHeight += margin.verticalTotal();
        }

        return totalHeight;
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

        // Resolve spacing: programmatic > CSS > 0
        int effectiveSpacing = resolveSpacing(cssResolver);
        int effectiveRowSpacing = resolveRowSpacing(cssResolver);

        // Measure each child's preferred width and wrap as FlowItem
        List<FlowItem> flowItems = new ArrayList<>(children.size());
        List<Widget> childWidgets = new ArrayList<>(children.size());

        for (Element child : children) {
            int childWidth = Math.max(1, child.preferredWidth());
            int childHeight = Math.max(1, child.preferredHeight(childWidth, context));
            Widget widget = (a, b) -> context.renderChild(child, frame, a);
            flowItems.add(FlowItem.of(widget, childWidth, childHeight));
        }

        // Build and render the Flow widget
        Flow flow = Flow.builder()
            .items(flowItems)
            .horizontalSpacing(effectiveSpacing)
            .verticalSpacing(effectiveRowSpacing)
            .build();

        frame.renderWidget(flow, effectiveArea);
    }

    private int resolveSpacing(CssStyleResolver cssResolver) {
        if (this.spacing != null) {
            return this.spacing;
        }
        if (cssResolver != null) {
            return cssResolver.spacing().orElse(0);
        }
        return 0;
    }

    private int resolveRowSpacing(CssStyleResolver cssResolver) {
        if (this.rowSpacing != null) {
            return this.rowSpacing;
        }
        if (cssResolver != null) {
            return cssResolver.get(FLOW_ROW_SPACING).orElse(0);
        }
        return 0;
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.toolkit.element.ContainerElement;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A horizontal layout container that arranges children in a row.
 * <p>
 * Layout properties can be set via CSS or programmatically:
 * <ul>
 *   <li>{@code flex} - Flex positioning mode: "start", "center", "end", "space-between", "space-around", "space-evenly"</li>
 *   <li>{@code spacing} - Gap between children in cells</li>
 *   <li>{@code margin} - Margin around the row</li>
 * </ul>
 * <p>
 * Programmatic values override CSS values when both are set.
 * <p>
 * Example usage:
 * <pre>
 * row(child1, child2, child3).flex(Flex.CENTER).spacing(1)
 * </pre>
 */
public final class Row extends ContainerElement<Row> {

    private Integer spacing;
    private Flex flex;
    private Margin margin;

    /** Creates an empty row. */
    public Row() {
    }

    /**
     * Creates a row with the given children.
     *
     * @param children the child elements
     */
    public Row(Element... children) {
        this.children.addAll(Arrays.asList(children));
    }

    /**
     * Sets the spacing between children.
     * <p>
     * Can also be set via CSS {@code spacing} property.
     *
     * @param spacing spacing in cells between adjacent children
     * @return this row for method chaining
     */
    public Row spacing(int spacing) {
        this.spacing = Math.max(0, spacing);
        return this;
    }

    /**
     * Sets how remaining space is distributed among children.
     * <p>
     * Can also be set via CSS {@code flex} property.
     *
     * @param flex the flex mode for space distribution
     * @return this row for method chaining
     * @see Flex
     */
    public Row flex(Flex flex) {
        this.flex = flex;
        return this;
    }

    /**
     * Sets the margin around the row.
     * <p>
     * Can also be set via CSS {@code margin} property.
     *
     * @param margin the margin
     * @return this row for method chaining
     */
    public Row margin(Margin margin) {
        this.margin = margin;
        return this;
    }

    /**
     * Sets uniform margin around the row.
     *
     * @param value the margin value for all sides
     * @return this row for method chaining
     */
    public Row margin(int value) {
        this.margin = Margin.uniform(value);
        return this;
    }

    @Override
    public int preferredWidth() {
        if (children.isEmpty()) {
            return 0;
        }

        int width = 0;

        // Sum preferred widths of all children
        for (Element child : children) {
            width += child.preferredWidth();
        }

        // Add spacing between children (n-1 spacings)
        int effectiveSpacing = this.spacing != null ? this.spacing : 0;
        if (children.size() > 1) {
            width += effectiveSpacing * (children.size() - 1);
        }

        // Add margin width if present
        if (margin != null) {
            width += margin.left() + margin.right();
        }

        return width;
    }

    @Override
    public int preferredHeight(int availableWidth, RenderContext context) {
        if (children.isEmpty() || availableWidth <= 0) {
            return 1;
        }

        // For a row, calculate max height of children
        // Give each child an equal share of width as a reasonable approximation
        int effectiveSpacing = this.spacing != null ? this.spacing : 0;
        int totalSpacing = effectiveSpacing * Math.max(0, children.size() - 1);
        int contentWidth = Math.max(1, availableWidth - totalSpacing);
        int childWidth = Math.max(1, contentWidth / children.size());

        int maxHeight = 1;
        for (Element child : children) {
            maxHeight = Math.max(maxHeight, child.preferredHeight(childWidth, context));
        }

        return maxHeight;
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

        // Build constraints, accounting for spacing
        List<Constraint> constraints = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            Constraint c = child.constraint();
            // Check CSS width constraint if programmatic is null (Row uses width)
            if (c == null && child instanceof Styleable) {
                CssStyleResolver childCss = context.resolveStyle((Styleable) child).orElse(null);
                if (childCss != null) {
                    c = childCss.widthConstraint().orElse(null);
                }
            }
            // Handle Fit constraint by querying preferred width
            if (c instanceof Constraint.Fit) {
                int preferred = child.preferredWidth();
                c = preferred > 0 ? Constraint.length(preferred) : Constraint.fill();
            }
            constraints.add(c != null ? c : Constraint.fill());

            // Add spacing constraint between children
            if (effectiveSpacing > 0 && i < children.size() - 1) {
                constraints.add(Constraint.length(effectiveSpacing));
            }
        }

        List<Rect> areas = Layout.horizontal()
            .constraints(constraints.toArray(new Constraint[0]))
            .flex(effectiveFlex)
            .split(effectiveArea);

        // Render children (skipping spacing areas)
        int childIndex = 0;
        for (int i = 0; i < areas.size() && childIndex < children.size(); i++) {
            if (effectiveSpacing > 0 && i % 2 == 1) {
                // Skip spacing area
                continue;
            }
            Element child = children.get(childIndex);
            Rect childArea = areas.get(i);
            context.renderChild(child, frame, childArea);
            childIndex++;
        }
    }
}

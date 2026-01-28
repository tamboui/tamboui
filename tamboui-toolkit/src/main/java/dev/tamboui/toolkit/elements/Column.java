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
 * A vertical layout container that arranges children in a column.
 * <p>
 * Layout properties can be set via CSS or programmatically:
 * <ul>
 *   <li>{@code flex} - Flex positioning mode: "start", "center", "end", "space-between", "space-around", "space-evenly"</li>
 *   <li>{@code spacing} - Gap between children in cells</li>
 *   <li>{@code margin} - Margin around the column</li>
 * </ul>
 * <p>
 * Programmatic values override CSS values when both are set.
 * <p>
 * Example usage:
 * <pre>
 * column(child1, child2, child3).flex(Flex.CENTER).spacing(1)
 * </pre>
 */
public final class Column extends ContainerElement<Column> {

    private Integer spacing;
    private Flex flex;
    private Margin margin;

    /**
     * Creates a new empty column.
     */
    public Column() {
    }

    /**
     * Creates a new column with the given children.
     *
     * @param children the child elements to arrange vertically
     */
    public Column(Element... children) {
        this.children.addAll(Arrays.asList(children));
    }

    /**
     * Sets the spacing between children.
     * <p>
     * Can also be set via CSS {@code spacing} property.
     *
     * @param spacing spacing in cells between adjacent children
     * @return this column for method chaining
     */
    public Column spacing(int spacing) {
        this.spacing = Math.max(0, spacing);
        return this;
    }

    /**
     * Sets how remaining space is distributed among children.
     * <p>
     * Can also be set via CSS {@code flex} property.
     *
     * @param flex the flex mode for space distribution
     * @return this column for method chaining
     * @see Flex
     */
    public Column flex(Flex flex) {
        this.flex = flex;
        return this;
    }

    /**
     * Sets the margin around the column.
     * <p>
     * Can also be set via CSS {@code margin} property.
     *
     * @param margin the margin
     * @return this column for method chaining
     */
    public Column margin(Margin margin) {
        this.margin = margin;
        return this;
    }

    /**
     * Sets uniform margin around the column.
     *
     * @param value the margin value for all sides
     * @return this column for method chaining
     */
    public Column margin(int value) {
        this.margin = Margin.uniform(value);
        return this;
    }

    @Override
    public int preferredWidth() {
        if (children.isEmpty()) {
            return 0;
        }

        // Column stacks children vertically, so width is the maximum child width
        int maxWidth = 0;
        for (Element child : children) {
            maxWidth = Math.max(maxWidth, child.preferredWidth());
        }

        // Add margin width if present
        if (margin != null) {
            maxWidth += margin.left() + margin.right();
        }

        return maxWidth;
    }

    @Override
    public int preferredHeight(int availableWidth, RenderContext context) {
        if (children.isEmpty() || availableWidth <= 0) {
            return 0;
        }

        // Calculate effective spacing
        int effectiveSpacing = this.spacing != null ? this.spacing : 0;
        int totalSpacing = effectiveSpacing * Math.max(0, children.size() - 1);

        // Sum heights of all children (Column is vertical, so all children get full width)
        int totalHeight = 0;
        for (Element child : children) {
            totalHeight += child.preferredHeight(availableWidth, context);
        }

        return totalHeight + totalSpacing;
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
        int effectiveSpacing = this.spacing != null ? this.spacing : 0;
        if (this.spacing == null && cssResolver != null) {
            effectiveSpacing = cssResolver.spacing().orElse(0);
        }

        // Resolve flex: programmatic > CSS > null (don't apply if not set)
        Flex effectiveFlex = this.flex;
        if (effectiveFlex == null && cssResolver != null) {
            effectiveFlex = cssResolver.flex().orElse(null);
        }

        // Build constraints, accounting for spacing
        List<Constraint> constraints = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            Constraint c = child.constraint();
            // Check CSS height constraint if programmatic is null (Column uses height)
            if (c == null && child instanceof Styleable) {
                CssStyleResolver childCss = context.resolveStyle((Styleable) child).orElse(null);
                if (childCss != null) {
                    c = childCss.heightConstraint().orElse(null);
                }
            }
            // Handle Fit constraint by querying preferred height
            if (c instanceof Constraint.Fit) {
                int preferred = child.preferredHeight();
                c = preferred > 0 ? Constraint.length(preferred) : null;
            }
            if (c == null) {
                // For text elements without explicit constraint, calculate height from content
                c = calculateDefaultConstraint(child);
            }
            constraints.add(c != null ? c : Constraint.fill());

            // Add spacing constraint between children
            if (effectiveSpacing > 0 && i < children.size() - 1) {
                constraints.add(Constraint.length(effectiveSpacing));
            }
        }

        // Build layout - only apply flex if explicitly set
        Layout layout = Layout.vertical()
            .constraints(constraints.toArray(new Constraint[0]));

        if (effectiveFlex != null) {
            layout = layout.flex(effectiveFlex);
        }

        List<Rect> areas = layout.split(effectiveArea);

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

    /**
     * Calculates a default height constraint for elements that return null.
     * For text elements, this returns a constraint based on line count.
     */
    private Constraint calculateDefaultConstraint(Element child) {
        if (child instanceof TextElement) {
            TextElement text = (TextElement) child;
            return text.calculateHeightConstraint();
        }
        return null;
    }
}

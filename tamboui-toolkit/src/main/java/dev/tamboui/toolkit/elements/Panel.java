/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.toolkit.element.ContainerElement;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Direction;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Padding;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.text.Overflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A container element with borders and title.
 * <p>
 * CSS properties {@code border-type} and {@code border-color} are automatically
 * resolved through the underlying {@link Block} widget.
 * <p>
 * Layout properties can be set via CSS or programmatically:
 * <ul>
 *   <li>{@code direction} - Layout direction: "horizontal"/"row" or "vertical"/"column"</li>
 *   <li>{@code flex} - Flex positioning mode: "start", "center", "end", "space-between", "space-around", "space-evenly"</li>
 *   <li>{@code margin} - Margin around the panel: single value or CSS-style shorthand</li>
 *   <li>{@code spacing} - Gap between children in cells</li>
 * </ul>
 * <p>
 * Programmatic values override CSS values when both are set.
 */
public final class Panel extends ContainerElement<Panel> {

    private String title;
    private String bottomTitle;
    private Overflow titleOverflow = Overflow.CLIP;
    private BorderType borderType;
    private Color borderColor;
    private Color focusedBorderColor;
    private Padding padding;
    private Direction direction;
    private Flex flex;
    private Margin margin;
    private Integer spacing;
    private boolean fitToContent;

    public Panel() {
    }

    public Panel(String title, Element... children) {
        this.title = title;
        this.children.addAll(Arrays.asList(children));
    }

    public Panel(Element... children) {
        this.children.addAll(Arrays.asList(children));
    }

    /**
     * Sets the panel title.
     */
    public Panel title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the bottom title.
     */
    public Panel bottomTitle(String title) {
        this.bottomTitle = title;
        return this;
    }

    /**
     * Sets the title overflow mode.
     */
    public Panel titleOverflow(Overflow overflow) {
        this.titleOverflow = overflow;
        return this;
    }

    /**
     * Truncate title with ellipsis at end if it doesn't fit: "Long title..."
     */
    public Panel titleEllipsis() {
        this.titleOverflow = Overflow.ELLIPSIS;
        return this;
    }

    /**
     * Truncate title with ellipsis at start if it doesn't fit: "...ong title"
     */
    public Panel titleEllipsisStart() {
        this.titleOverflow = Overflow.ELLIPSIS_START;
        return this;
    }

    @Override
    public Map<String, String> styleAttributes() {
        Map<String, String> attrs = new LinkedHashMap<>(super.styleAttributes());
        if (title != null) {
            attrs.put("title", title);
        }
        if (bottomTitle != null) {
            attrs.put("bottom-title", bottomTitle);
        }
        return Collections.unmodifiableMap(attrs);
    }

    /**
     * Sets the border type to rounded.
     */
    public Panel rounded() {
        this.borderType = BorderType.ROUNDED;
        return this;
    }

    /**
     * Sets the border type to double.
     */
    public Panel doubleBorder() {
        this.borderType = BorderType.DOUBLE;
        return this;
    }

    /**
     * Sets the border type to thick.
     */
    public Panel thick() {
        this.borderType = BorderType.THICK;
        return this;
    }

    /**
     * Sets the border type.
     */
    public Panel borderType(BorderType type) {
        this.borderType = type;
        return this;
    }

    /**
     * Makes this panel borderless.
     * <p>
     * This sets the border type to {@link BorderType#NONE}, which renders
     * no borders but still reserves space for them if borders are enabled.
     */
    public Panel borderless() {
        this.borderType = BorderType.NONE;
        return this;
    }

    /**
     * Sets the border color.
     */
    public Panel borderColor(Color color) {
        this.borderColor = color;
        return this;
    }

    /**
     * Sets the border color when focused.
     */
    public Panel focusedBorderColor(Color color) {
        this.focusedBorderColor = color;
        return this;
    }

    /**
     * Sets uniform padding inside the panel.
     */
    public Panel padding(int value) {
        this.padding = Padding.uniform(value);
        return this;
    }

    /**
     * Sets the padding inside the panel.
     */
    public Panel padding(Padding padding) {
        this.padding = padding;
        return this;
    }

    /**
     * Sets the layout direction for children.
     * <p>
     * Can also be set via CSS {@code direction} property.
     *
     * @param direction the layout direction
     * @return this panel for chaining
     */
    public Panel direction(Direction direction) {
        this.direction = direction;
        return this;
    }

    /**
     * Sets the layout direction to horizontal.
     *
     * @return this panel for chaining
     */
    public Panel horizontal() {
        this.direction = Direction.HORIZONTAL;
        return this;
    }

    /**
     * Sets the layout direction to vertical.
     *
     * @return this panel for chaining
     */
    public Panel vertical() {
        this.direction = Direction.VERTICAL;
        return this;
    }

    /**
     * Sets the flex layout mode for positioning children.
     * <p>
     * Can also be set via CSS {@code flex} property.
     *
     * @param flex the flex mode
     * @return this panel for chaining
     */
    public Panel flex(Flex flex) {
        this.flex = flex;
        return this;
    }

    /**
     * Sets the margin around the panel.
     * <p>
     * Can also be set via CSS {@code margin} property.
     *
     * @param margin the margin
     * @return this panel for chaining
     */
    public Panel margin(Margin margin) {
        this.margin = margin;
        return this;
    }

    /**
     * Sets uniform margin around the panel.
     *
     * @param value the margin value for all sides
     * @return this panel for chaining
     */
    public Panel margin(int value) {
        this.margin = Margin.uniform(value);
        return this;
    }

    /**
     * Sets the spacing (gap) between children.
     * <p>
     * Can also be set via CSS {@code spacing} property.
     *
     * @param spacing the spacing in cells
     * @return this panel for chaining
     */
    public Panel spacing(int spacing) {
        this.spacing = spacing;
        return this;
    }

    /**
     * Enables automatic height calculation to fit the panel's content.
     * <p>
     * When enabled, the constraint is computed dynamically based on:
     * <ul>
     *   <li>Border overhead: 2 rows (top and bottom borders)</li>
     *   <li>Padding overhead: vertical padding if set</li>
     *   <li>Children height: sum of child heights (1 row each by default,
     *       or the length from their constraint if specified)</li>
     * </ul>
     * <p>
     * The height is computed when {@link #constraint()} is called, so children
     * can be added before or after calling this method.
     *
     * @return this panel for chaining
     */
    public Panel fit() {
        this.fitToContent = true;
        return this;
    }

    @Override
    public Constraint constraint() {
        if (fitToContent) {
            return Constraint.length(computeContentHeight());
        }
        return layoutConstraint;
    }

    @Override
    public int preferredHeight(int availableWidth, RenderContext context) {
        if (availableWidth <= 0) {
            return 2; // Just borders
        }

        // Border overhead: 2 rows for top and bottom
        int height = 2;

        // Padding overhead
        if (padding != null) {
            height += padding.verticalTotal();
        }

        // Content width after borders and padding
        int paddingHorizontal = padding != null ? padding.horizontalTotal() : 0;
        int contentWidth = Math.max(1, availableWidth - 2 - paddingHorizontal);

        if (children.isEmpty()) {
            return height;
        }

        // Determine layout direction (default vertical)
        Direction effectiveDirection = this.direction != null ? this.direction : Direction.VERTICAL;
        int effectiveSpacing = this.spacing != null ? this.spacing : 0;

        if (effectiveDirection == Direction.VERTICAL) {
            // Sum of children heights + spacing
            int totalSpacing = effectiveSpacing * Math.max(0, children.size() - 1);
            for (Element child : children) {
                height += child.preferredHeight(contentWidth, context);
            }
            height += totalSpacing;
        } else {
            // Horizontal: max height of children with equal width distribution
            int totalSpacing = effectiveSpacing * Math.max(0, children.size() - 1);
            int childWidth = Math.max(1, (contentWidth - totalSpacing) / children.size());
            int maxChildHeight = 1;
            for (Element child : children) {
                maxChildHeight = Math.max(maxChildHeight, child.preferredHeight(childWidth, context));
            }
            height += maxChildHeight;
        }

        return height;
    }

    /**
     * Computes the total height required to fit the panel's content.
     *
     * @return the computed height including borders, padding, and children
     */
    private int computeContentHeight() {
        // Border overhead: 2 rows for top and bottom (Panel always uses Borders.ALL)
        int height = 2;

        // Padding overhead
        if (padding != null) {
            height += padding.verticalTotal();
        }

        // Children height: sum of child heights
        for (Element child : children) {
            Constraint c = child.constraint();
            if (c instanceof Constraint.Length) {
                height += ((Constraint.Length) c).value();
            } else {
                // Default: 1 row per child
                height += 1;
            }
        }

        return height;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
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

        // Get current style from context (already resolved by StyledElement.render)
        Style effectiveStyle = context.currentStyle();

        // Get the CSS resolver for this element
        StylePropertyResolver resolver = cssResolver != null
                ? cssResolver
                : StylePropertyResolver.empty();

        // Determine border color: focus color > programmatic color
        boolean isFocused = elementId != null && context.isFocused(elementId);
        Color effectiveBorderColor = isFocused && focusedBorderColor != null
                ? focusedBorderColor
                : borderColor;

        // Get padding: programmatic > CSS > none
        Padding effectivePadding = this.padding;
        if (effectivePadding == null && cssResolver != null) {
            effectivePadding = cssResolver.padding().orElse(Padding.NONE);
        } else if (effectivePadding == null) {
            effectivePadding = Padding.NONE;
        }

        // Build the block - CSS properties are resolved by the widget
        Block.Builder blockBuilder = Block.builder()
                .borders(Borders.ALL)
                .padding(effectivePadding)
                .style(effectiveStyle)
                .styleResolver(resolver);

        // Set programmatic overrides if specified
        if (borderType != null) {
            blockBuilder.borderType(borderType);
        }
        if (effectiveBorderColor != null) {
            blockBuilder.borderColor(effectiveBorderColor);
        }

        if (title != null) {
            blockBuilder.title(Title.from(Line.from(Span.raw(title))).overflow(titleOverflow));
        }

        if (bottomTitle != null) {
            blockBuilder.titleBottom(Title.from(bottomTitle));
        }

        Block block = blockBuilder.build();

        // Render the block
        frame.renderWidget(block, effectiveArea);

        // Get inner area for children
        Rect innerArea = block.inner(effectiveArea);
        if (innerArea.isEmpty() || children.isEmpty()) {
            return;
        }

        // Resolve layout properties: programmatic > CSS > defaults
        Direction effectiveDirection = this.direction;
        if (effectiveDirection == null && cssResolver != null) {
            effectiveDirection = cssResolver.direction().orElse(Direction.VERTICAL);
        } else if (effectiveDirection == null) {
            effectiveDirection = Direction.VERTICAL;
        }

        Flex effectiveFlex = this.flex;
        if (effectiveFlex == null && cssResolver != null) {
            effectiveFlex = cssResolver.flex().orElse(null);
        }

        Integer effectiveSpacing = this.spacing;
        if (effectiveSpacing == null && cssResolver != null) {
            effectiveSpacing = cssResolver.spacing().orElse(null);
        }

        // Layout children using resolved direction, flex, and spacing
        List<Constraint> constraints = new ArrayList<>();
        boolean isHorizontal = effectiveDirection == Direction.HORIZONTAL;
        for (Element child : children) {
            Constraint c = child.constraint();
            // Check CSS constraint if programmatic is null (width for horizontal, height for vertical)
            if (c == null && child instanceof Styleable) {
                CssStyleResolver childCss = context.resolveStyle((Styleable) child).orElse(null);
                if (childCss != null) {
                    c = isHorizontal
                            ? childCss.widthConstraint().orElse(null)
                            : childCss.heightConstraint().orElse(null);
                }
            }
            // Default to fill() so children expand to use available space
            constraints.add(c != null ? c : Constraint.fill());
        }

        Layout layout = effectiveDirection == Direction.HORIZONTAL
                ? Layout.horizontal()
                : Layout.vertical();

        layout = layout.constraints(constraints.toArray(new Constraint[0]));

        if (effectiveFlex != null) {
            layout = layout.flex(effectiveFlex);
        }
        if (effectiveSpacing != null) {
            layout = layout.spacing(effectiveSpacing);
        }

        List<Rect> areas = layout.split(innerArea);

        // Render children (they self-register for events in their own render() if needed)
        for (int i = 0; i < children.size() && i < areas.size(); i++) {
            Element child = children.get(i);
            Rect childArea = areas.get(i);
            context.renderChild(child, frame, childArea);
        }
    }

}

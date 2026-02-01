/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.ConstraintConverter;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.dock.Dock;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.widget.Widget;

/**
 * A 5-region dock layout element that arranges children into top, bottom,
 * left, right, and center regions — the most common TUI application structure
 * (header + sidebar + content + footer).
 * <p>
 * Extends {@link StyledElement} directly (not {@code ContainerElement}) because
 * it has 5 named regions, not a positional children list.
 * <p>
 * All layout properties can be set via CSS or programmatically.
 * Programmatic values override CSS values when both are set.
 * <p>
 * Supported CSS properties:
 * <ul>
 *   <li>{@code dock-top-height} — height constraint for the top region</li>
 *   <li>{@code dock-bottom-height} — height constraint for the bottom region</li>
 *   <li>{@code dock-left-width} — width constraint for the left region</li>
 *   <li>{@code dock-right-width} — width constraint for the right region</li>
 *   <li>{@code margin} — margin around the dock layout</li>
 *   <li>{@code background} — background color fill</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * dock()
 *     .top(text("Header").bold())
 *     .bottom(text("Footer").dim())
 *     .left(text("Sidebar"))
 *     .center(text("Main Content"))
 *     .topHeight(Constraint.length(3))
 *     .leftWidth(Constraint.length(20))
 * </pre>
 */
public final class DockElement extends StyledElement<DockElement> {

    /**
     * CSS property definition for the top region height.
     */
    public static final PropertyDefinition<Constraint> DOCK_TOP_HEIGHT =
        PropertyDefinition.of("dock-top-height", ConstraintConverter.INSTANCE);

    /**
     * CSS property definition for the bottom region height.
     */
    public static final PropertyDefinition<Constraint> DOCK_BOTTOM_HEIGHT =
        PropertyDefinition.of("dock-bottom-height", ConstraintConverter.INSTANCE);

    /**
     * CSS property definition for the left region width.
     */
    public static final PropertyDefinition<Constraint> DOCK_LEFT_WIDTH =
        PropertyDefinition.of("dock-left-width", ConstraintConverter.INSTANCE);

    /**
     * CSS property definition for the right region width.
     */
    public static final PropertyDefinition<Constraint> DOCK_RIGHT_WIDTH =
        PropertyDefinition.of("dock-right-width", ConstraintConverter.INSTANCE);

    static {
        PropertyRegistry.registerAll(DOCK_TOP_HEIGHT, DOCK_BOTTOM_HEIGHT, DOCK_LEFT_WIDTH, DOCK_RIGHT_WIDTH);
    }

    private Element topElement;
    private Element bottomElement;
    private Element leftElement;
    private Element rightElement;
    private Element centerElement;
    private Constraint topHeight;
    private Constraint bottomHeight;
    private Constraint leftWidth;
    private Constraint rightWidth;
    private Margin margin;

    /**
     * Creates an empty dock layout.
     */
    public DockElement() {
    }

    /**
     * Sets the top region element.
     *
     * @param element the top element (e.g., a header bar)
     * @return this dock for method chaining
     */
    public DockElement top(Element element) {
        this.topElement = element;
        return this;
    }

    /**
     * Sets the top region element with a height constraint.
     *
     * @param element the top element (e.g., a header bar)
     * @param height the height constraint for this region
     * @return this dock for method chaining
     */
    public DockElement top(Element element, Constraint height) {
        this.topElement = element;
        this.topHeight = height;
        return this;
    }

    /**
     * Sets the bottom region element.
     *
     * @param element the bottom element (e.g., a status bar)
     * @return this dock for method chaining
     */
    public DockElement bottom(Element element) {
        this.bottomElement = element;
        return this;
    }

    /**
     * Sets the bottom region element with a height constraint.
     *
     * @param element the bottom element (e.g., a status bar)
     * @param height the height constraint for this region
     * @return this dock for method chaining
     */
    public DockElement bottom(Element element, Constraint height) {
        this.bottomElement = element;
        this.bottomHeight = height;
        return this;
    }

    /**
     * Sets the left region element.
     *
     * @param element the left element (e.g., a sidebar)
     * @return this dock for method chaining
     */
    public DockElement left(Element element) {
        this.leftElement = element;
        return this;
    }

    /**
     * Sets the left region element with a width constraint.
     *
     * @param element the left element (e.g., a sidebar)
     * @param width the width constraint for this region
     * @return this dock for method chaining
     */
    public DockElement left(Element element, Constraint width) {
        this.leftElement = element;
        this.leftWidth = width;
        return this;
    }

    /**
     * Sets the right region element.
     *
     * @param element the right element (e.g., a side panel)
     * @return this dock for method chaining
     */
    public DockElement right(Element element) {
        this.rightElement = element;
        return this;
    }

    /**
     * Sets the right region element with a width constraint.
     *
     * @param element the right element (e.g., a side panel)
     * @param width the width constraint for this region
     * @return this dock for method chaining
     */
    public DockElement right(Element element, Constraint width) {
        this.rightElement = element;
        this.rightWidth = width;
        return this;
    }

    /**
     * Sets the center region element.
     *
     * @param element the center element (e.g., main content)
     * @return this dock for method chaining
     */
    public DockElement center(Element element) {
        this.centerElement = element;
        return this;
    }

    /**
     * Sets the height constraint for the top region.
     *
     * @param constraint the height constraint
     * @return this dock for method chaining
     */
    public DockElement topHeight(Constraint constraint) {
        this.topHeight = constraint;
        return this;
    }

    /**
     * Sets the height constraint for the bottom region.
     *
     * @param constraint the height constraint
     * @return this dock for method chaining
     */
    public DockElement bottomHeight(Constraint constraint) {
        this.bottomHeight = constraint;
        return this;
    }

    /**
     * Sets the width constraint for the left region.
     *
     * @param constraint the width constraint
     * @return this dock for method chaining
     */
    public DockElement leftWidth(Constraint constraint) {
        this.leftWidth = constraint;
        return this;
    }

    /**
     * Sets the width constraint for the right region.
     *
     * @param constraint the width constraint
     * @return this dock for method chaining
     */
    public DockElement rightWidth(Constraint constraint) {
        this.rightWidth = constraint;
        return this;
    }

    /**
     * Sets the margin around the dock layout.
     *
     * @param margin the margin
     * @return this dock for method chaining
     */
    public DockElement margin(Margin margin) {
        this.margin = margin;
        return this;
    }

    /**
     * Sets uniform margin around the dock layout.
     *
     * @param value the margin value for all sides
     * @return this dock for method chaining
     */
    public DockElement margin(int value) {
        this.margin = Margin.uniform(value);
        return this;
    }

    @Override
    public int preferredWidth() {
        StylePropertyResolver resolver = StylePropertyResolver.empty();
        int leftW = leftElement != null ? constraintHint(resolveLeftWidth(resolver, leftElement)) : 0;
        int centerW = centerElement != null ? centerElement.preferredWidth() : 0;
        int rightW = rightElement != null ? constraintHint(resolveRightWidth(resolver, rightElement)) : 0;
        int width = leftW + centerW + rightW;
        if (margin != null) {
            width += margin.left() + margin.right();
        }
        return width;
    }

    @Override
    public int preferredHeight(int availableWidth, RenderContext context) {
        StylePropertyResolver resolver = StylePropertyResolver.empty();
        int topH = topElement != null ? constraintHint(resolveTopHeight(resolver, topElement, availableWidth, context)) : 0;
        int bottomH = bottomElement != null ? constraintHint(resolveBottomHeight(resolver, bottomElement, availableWidth, context)) : 0;

        int leftH = leftElement != null ? leftElement.preferredHeight(availableWidth, context) : 0;
        int centerH = centerElement != null ? centerElement.preferredHeight(availableWidth, context) : 0;
        int rightH = rightElement != null ? rightElement.preferredHeight(availableWidth, context) : 0;
        int middleH = Math.max(leftH, Math.max(centerH, rightH));

        int height = topH + middleH + bottomH;
        if (margin != null) {
            height += margin.verticalTotal();
        }
        return height;
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        EventResult result = super.handleKeyEvent(event, focused);
        if (result.isHandled()) {
            return result;
        }
        // Forward to all non-null child regions
        Element[] regions = {topElement, bottomElement, leftElement, rightElement, centerElement};
        for (Element region : regions) {
            if (region != null && region.handleKeyEvent(event, true) == EventResult.HANDLED) {
                return EventResult.HANDLED;
            }
        }
        return EventResult.UNHANDLED;
    }

    @Override
    public EventResult handleMouseEvent(MouseEvent event) {
        EventResult result = super.handleMouseEvent(event);
        if (result.isHandled()) {
            return result;
        }
        // Forward to child regions whose area contains the mouse position
        Element[] regions = {topElement, bottomElement, leftElement, rightElement, centerElement};
        for (Element region : regions) {
            if (region != null) {
                Rect area = region.renderedArea();
                if (area != null && area.contains(event.x(), event.y())) {
                    if (region.handleMouseEvent(event) == EventResult.HANDLED) {
                        return EventResult.HANDLED;
                    }
                }
            }
        }
        return EventResult.UNHANDLED;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        // Get CSS resolver for property resolution, use empty resolver as fallback
        CssStyleResolver cssResolver = context.resolveStyle(this).orElse(null);
        StylePropertyResolver resolver = cssResolver != null ? cssResolver : StylePropertyResolver.empty();

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

        // Resolve constraints: programmatic > CSS > element preferred size > defaults
        Constraint effectiveTopHeight = resolveTopHeight(resolver, topElement, effectiveArea.width(), context);
        Constraint effectiveBottomHeight = resolveBottomHeight(resolver, bottomElement, effectiveArea.width(), context);
        Constraint effectiveLeftWidth = resolveLeftWidth(resolver, leftElement);
        Constraint effectiveRightWidth = resolveRightWidth(resolver, rightElement);

        // Wrap Elements as lambda Widgets
        Widget topWidget = topElement != null
            ? (a, b) -> context.renderChild(topElement, frame, a) : null;
        Widget bottomWidget = bottomElement != null
            ? (a, b) -> context.renderChild(bottomElement, frame, a) : null;
        Widget leftWidget = leftElement != null
            ? (a, b) -> context.renderChild(leftElement, frame, a) : null;
        Widget rightWidget = rightElement != null
            ? (a, b) -> context.renderChild(rightElement, frame, a) : null;
        Widget centerWidget = centerElement != null
            ? (a, b) -> context.renderChild(centerElement, frame, a) : null;

        // Build and render the Dock widget
        Dock dock = Dock.builder()
            .top(topWidget)
            .bottom(bottomWidget)
            .left(leftWidget)
            .right(rightWidget)
            .center(centerWidget)
            .topHeight(effectiveTopHeight)
            .bottomHeight(effectiveBottomHeight)
            .leftWidth(effectiveLeftWidth)
            .rightWidth(effectiveRightWidth)
            .build();

        frame.renderWidget(dock, effectiveArea);
    }

    private Constraint resolveTopHeight(StylePropertyResolver resolver, Element element, int availableWidth, RenderContext context) {
        // Use standard resolution: programmatic → CSS → null
        Constraint resolved = resolver.resolve(DOCK_TOP_HEIGHT, this.topHeight);
        if (resolved != null) {
            return resolved;
        }
        // Fall back to element's preferred height
        if (element != null) {
            int preferredHeight = element.preferredHeight(availableWidth, context);
            if (preferredHeight > 0) {
                return Constraint.length(preferredHeight);
            }
        }
        return Constraint.length(1);
    }

    private Constraint resolveBottomHeight(StylePropertyResolver resolver, Element element, int availableWidth, RenderContext context) {
        // Use standard resolution: programmatic → CSS → null
        Constraint resolved = resolver.resolve(DOCK_BOTTOM_HEIGHT, this.bottomHeight);
        if (resolved != null) {
            return resolved;
        }
        // Fall back to element's preferred height
        if (element != null) {
            int preferredHeight = element.preferredHeight(availableWidth, context);
            if (preferredHeight > 0) {
                return Constraint.length(preferredHeight);
            }
        }
        return Constraint.length(1);
    }

    private Constraint resolveLeftWidth(StylePropertyResolver resolver, Element element) {
        // Use standard resolution: programmatic → CSS → null
        Constraint resolved = resolver.resolve(DOCK_LEFT_WIDTH, this.leftWidth);
        if (resolved != null) {
            return resolved;
        }
        // Fall back to element's preferred width
        if (element != null) {
            int preferredWidth = element.preferredWidth();
            if (preferredWidth > 0) {
                return Constraint.length(preferredWidth);
            }
        }
        return Constraint.length(10);
    }

    private Constraint resolveRightWidth(StylePropertyResolver resolver, Element element) {
        // Use standard resolution: programmatic → CSS → null
        Constraint resolved = resolver.resolve(DOCK_RIGHT_WIDTH, this.rightWidth);
        if (resolved != null) {
            return resolved;
        }
        // Fall back to element's preferred width
        if (element != null) {
            int preferredWidth = element.preferredWidth();
            if (preferredWidth > 0) {
                return Constraint.length(preferredWidth);
            }
        }
        return Constraint.length(10);
    }

    /**
     * Extracts a size hint from a constraint for preferred width/height calculations.
     */
    private static int constraintHint(Constraint constraint) {
        if (constraint instanceof Constraint.Length) {
            return ((Constraint.Length) constraint).value();
        }
        if (constraint instanceof Constraint.Min) {
            return ((Constraint.Min) constraint).value();
        }
        if (constraint instanceof Constraint.Max) {
            return ((Constraint.Max) constraint).value();
        }
        return 1;
    }
}

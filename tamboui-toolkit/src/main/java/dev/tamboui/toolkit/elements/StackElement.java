/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.layout.ContentAlignment;
import dev.tamboui.layout.ContentAlignmentConverter;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.ContainerElement;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;

import java.util.Arrays;
import java.util.Collection;

/**
 * An overlapping layers layout element where children render on top of each
 * other using a painter's algorithm (last child on top).
 * <p>
 * Essential for dialogs, popups, floating overlays, and any scenario
 * where UI elements need to overlap.
 * <p>
 * All layout properties can be set via CSS or programmatically.
 * Programmatic values override CSS values when both are set.
 * <p>
 * Supported CSS properties:
 * <ul>
 *   <li>{@code content-align} — how children are aligned within the stack
 *       (default: {@code stretch})</li>
 *   <li>{@code margin} — margin around the stack layout</li>
 *   <li>{@code background} — background color fill</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>
 * stack(
 *     text("Background").fill(),
 *     panel("Dialog", text("Content")).rounded()
 * ).alignment(ContentAlignment.CENTER)
 * </pre>
 */
public final class StackElement extends ContainerElement<StackElement> {

    /**
     * CSS property definition for the content alignment.
     */
    public static final PropertyDefinition<ContentAlignment> CONTENT_ALIGN =
        PropertyDefinition.of("content-align", ContentAlignmentConverter.INSTANCE);

    static {
        PropertyRegistry.registerAll(CONTENT_ALIGN);
    }

    private ContentAlignment alignment;
    private Margin margin;

    /**
     * Creates an empty stack layout.
     */
    public StackElement() {
    }

    /**
     * Creates a stack layout with the given children.
     *
     * @param children the child elements
     */
    public StackElement(Element... children) {
        this.children.addAll(Arrays.asList(children));
    }

    /**
     * Creates a stack layout with the given children.
     *
     * @param children the child elements
     */
    public StackElement(Collection<? extends Element> children) {
        this.children.addAll(children);
    }

    /**
     * Sets how children are aligned within the stack.
     * <p>
     * Default is {@link ContentAlignment#STRETCH} which fills
     * children to the full area.
     *
     * @param alignment the alignment mode
     * @return this stack for method chaining
     */
    public StackElement alignment(ContentAlignment alignment) {
        this.alignment = alignment;
        return this;
    }

    /**
     * Sets the margin around the stack layout.
     *
     * @param margin the margin
     * @return this stack for method chaining
     */
    public StackElement margin(Margin margin) {
        this.margin = margin;
        return this;
    }

    /**
     * Sets uniform margin around the stack layout.
     *
     * @param value the margin value for all sides
     * @return this stack for method chaining
     */
    public StackElement margin(int value) {
        this.margin = Margin.uniform(value);
        return this;
    }

    @Override
    public int preferredWidth() {
        int maxWidth = 0;
        for (Element child : children) {
            maxWidth = Math.max(maxWidth, child.preferredWidth());
        }
        if (margin != null) {
            maxWidth += margin.left() + margin.right();
        }
        return maxWidth;
    }

    @Override
    public int preferredHeight(int availableWidth, RenderContext context) {
        int maxHeight = 0;
        for (Element child : children) {
            maxHeight = Math.max(maxHeight, child.preferredHeight(availableWidth, context));
        }
        if (margin != null) {
            maxHeight += margin.verticalTotal();
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

        // Resolve alignment: programmatic > CSS > STRETCH
        ContentAlignment effectiveAlignment = this.alignment;
        if (effectiveAlignment == null && cssResolver != null) {
            effectiveAlignment = cssResolver.get(CONTENT_ALIGN).orElse(ContentAlignment.STRETCH);
        } else if (effectiveAlignment == null) {
            effectiveAlignment = ContentAlignment.STRETCH;
        }

        // Render each child in order (last on top)
        for (Element child : children) {
            Rect childArea;
            if (effectiveAlignment == ContentAlignment.STRETCH) {
                childArea = effectiveArea;
            } else {
                int childWidth = child.preferredWidth();
                int childHeight = child.preferredHeight(effectiveArea.width(), context);
                childArea = effectiveAlignment.align(effectiveArea, childWidth, childHeight);
            }
            if (!childArea.isEmpty()) {
                context.renderChild(child, frame, childArea);
            }
        }
    }
}

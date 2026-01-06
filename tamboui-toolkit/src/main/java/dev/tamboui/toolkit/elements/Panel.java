/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
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
import java.util.List;

/**
 * A container element with borders and title.
 * <p>
 * CSS properties {@code border-type} and {@code border-color} are automatically
 * resolved through the underlying {@link Block} widget.
 * Renders children vertically inside the bordered area.
 */
public final class Panel extends StyledElement<Panel> {

    private String title;
    private String bottomTitle;
    private Overflow titleOverflow = Overflow.CLIP;
    private BorderType borderType;
    private Color borderColor;
    private Color focusedBorderColor;
    private Padding padding;
    private final List<Element> children = new ArrayList<>();
    private boolean focusable;

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
     * Makes this panel focusable.
     */
    public Panel focusable() {
        this.focusable = true;
        return this;
    }

    /**
     * Sets whether this panel is focusable.
     */
    public Panel focusable(boolean focusable) {
        this.focusable = focusable;
        return this;
    }

    /**
     * Adds a child element.
     */
    public Panel add(Element child) {
        this.children.add(child);
        return this;
    }

    /**
     * Adds multiple child elements.
     */
    public Panel add(Element... children) {
        this.children.addAll(Arrays.asList(children));
        return this;
    }

    @Override
    public boolean isFocusable() {
        return focusable;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        // Get current style from context (already resolved by StyledElement.render)
        Style effectiveStyle = context.currentStyle();

        // Get the CSS resolver for this element
        StylePropertyResolver resolver = context.resolveStyle(this)
                .map(r -> (StylePropertyResolver) r)
                .orElse(StylePropertyResolver.empty());

        // Determine border color: focus color > programmatic color
        boolean isFocused = elementId != null && context.isFocused(elementId);
        Color effectiveBorderColor = isFocused && focusedBorderColor != null
                ? focusedBorderColor
                : borderColor;

        // Get padding: programmatic > CSS > none
        Padding effectivePadding = this.padding;
        if (effectivePadding == null) {
            effectivePadding = context.resolveStyle(this)
                    .flatMap(CssStyleResolver::padding)
                    .orElse(Padding.NONE);
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
        frame.renderWidget(block, area);

        // Get inner area for children
        Rect innerArea = block.inner(area);
        if (innerArea.isEmpty() || children.isEmpty()) {
            return;
        }

        // Layout children vertically
        List<Constraint> constraints = new ArrayList<>();
        for (Element child : children) {
            Constraint c = child.constraint();
            // Default to fill() so children expand to use available space
            constraints.add(c != null ? c : Constraint.fill());
        }

        List<Rect> areas = Layout.vertical()
                .constraints(constraints.toArray(new Constraint[0]))
                .split(innerArea);

        // Render children and register them for events
        DefaultRenderContext internalContext = (DefaultRenderContext) context;
        for (int i = 0; i < children.size() && i < areas.size(); i++) {
            Element child = children.get(i);
            Rect childArea = areas.get(i);
            child.render(frame, childArea, context);
            internalContext.registerElement(child, childArea);
        }
    }
}

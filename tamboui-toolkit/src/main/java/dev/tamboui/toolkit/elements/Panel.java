/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.css.cascade.ResolvedStyle;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
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
import java.util.Optional;

/**
 * A container element with borders and title.
 * Renders children vertically inside the bordered area.
 */
public final class Panel extends StyledElement<Panel> {

    private String title;
    private String bottomTitle;
    private Overflow titleOverflow = Overflow.CLIP;
    private BorderType borderType = BorderType.PLAIN;
    private Color borderColor;
    private Color focusedBorderColor;
    private Padding padding;
    private final List<Element> children = new ArrayList<>();
    private boolean focusable;
    private Flex flex = Flex.START;

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
     * Sets the flex mode for distributing remaining space among children.
     */
    public Panel flex(Flex flex) {
        this.flex = flex != null ? flex : Flex.START;
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

        // Determine border color: focus color > programmatic color > CSS color
        boolean isFocused = elementId != null && context.isFocused(elementId);
        Color effectiveBorderColor = isFocused && focusedBorderColor != null
            ? focusedBorderColor
            : borderColor;

        // Get CSS style for additional properties
        Optional<ResolvedStyle> cssStyle = context.resolveStyle(this);

        // If no programmatic border color set, try to get from CSS
        if (effectiveBorderColor == null) {
            effectiveBorderColor = cssStyle
                .flatMap(resolved -> resolved.getProperty("border-color"))
                .flatMap(context::parseColor)
                .orElse(null);
        }

        // Get padding: programmatic > CSS > none
        Padding effectivePadding = this.padding;
        if (effectivePadding == null) {
            effectivePadding = cssStyle
                .flatMap(ResolvedStyle::padding)
                .orElse(Padding.NONE);
        }

        // Get border type: programmatic > CSS > default (PLAIN)
        BorderType effectiveBorderType = this.borderType;
        if (effectiveBorderType == BorderType.PLAIN) {
            // Only check CSS if using default value
            effectiveBorderType = cssStyle
                .flatMap(ResolvedStyle::borderType)
                .orElse(BorderType.PLAIN);
        }

        // Build the block
        Block.Builder blockBuilder = Block.builder()
            .borders(Borders.ALL)
            .borderType(effectiveBorderType)
            .padding(effectivePadding)
            .style(effectiveStyle);

        if (effectiveBorderColor != null) {
            blockBuilder.borderStyle(Style.EMPTY.fg(effectiveBorderColor));
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
            .flex(flex)
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

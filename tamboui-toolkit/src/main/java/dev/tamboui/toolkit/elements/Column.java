/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A vertical layout container that arranges children in a column.
 */
public final class Column extends StyledElement<Column> {

    private final List<Element> children = new ArrayList<>();
    private int spacing = 0;

    public Column() {
    }

    public Column(Element... children) {
        this.children.addAll(Arrays.asList(children));
    }

    /**
     * Sets the spacing between children.
     */
    public Column spacing(int spacing) {
        this.spacing = Math.max(0, spacing);
        return this;
    }

    /**
     * Adds a child element.
     */
    public Column add(Element child) {
        this.children.add(child);
        return this;
    }

    /**
     * Adds multiple child elements.
     */
    public Column add(Element... children) {
        this.children.addAll(Arrays.asList(children));
        return this;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        if (children.isEmpty()) {
            return;
        }

        // Fill background with current style
        Style effectiveStyle = context.currentStyle();
        if (effectiveStyle.bg().isPresent()) {
            frame.buffer().setStyle(area, effectiveStyle);
        }

        // Build constraints, accounting for spacing
        List<Constraint> constraints = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            Element child = children.get(i);
            Constraint c = child.constraint();
            if (c == null) {
                // For text elements without explicit constraint, calculate height from content
                c = calculateDefaultConstraint(child);
            }
            constraints.add(c != null ? c : Constraint.fill());

            // Add spacing constraint between children
            if (spacing > 0 && i < children.size() - 1) {
                constraints.add(Constraint.length(spacing));
            }
        }

        List<Rect> areas = Layout.vertical()
            .constraints(constraints.toArray(new Constraint[0]))
            .split(area);

        // Render children (skipping spacing areas) and register them for events
        DefaultRenderContext internalContext = (DefaultRenderContext) context;
        int childIndex = 0;
        for (int i = 0; i < areas.size() && childIndex < children.size(); i++) {
            if (spacing > 0 && i % 2 == 1) {
                // Skip spacing area
                continue;
            }
            Element child = children.get(childIndex);
            Rect childArea = areas.get(i);
            child.render(frame, childArea, context);
            internalContext.registerElement(child, childArea);
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

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.toolkit.element.ContainerElement;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A vertical layout container that arranges children in a column.
 *
 * <p>Supports flex modes for distributing remaining space:
 * <pre>
 * column(child1, child2, child3).flex(Flex.CENTER).spacing(1)
 * </pre>
 */
public final class Column extends ContainerElement<Column> {

    private int spacing = 0;
    private Flex flex = Flex.START;

    public Column() {
    }

    public Column(Element... children) {
        this.children.addAll(Arrays.asList(children));
    }

    /**
     * Sets the spacing between children.
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
     *
     * @param flex the flex mode for space distribution
     * @return this column for method chaining
     * @see Flex
     */
    public Column flex(Flex flex) {
        this.flex = flex != null ? flex : Flex.START;
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
            .flex(flex)
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

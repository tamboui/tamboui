/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Layout;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.ContainerElement;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;

/**
 * A scope element that can show/hide its content dynamically.
 * <p>
 * When visible, renders its children in a vertical layout. When hidden, the
 * scope collapses to zero height, taking no space. This is useful for temporary
 * displays like parallel downloads that should disappear when complete.
 *
 * <h2>Key Features</h2>
 * <ul>
 * <li>Declarative visibility: state drives rendering</li>
 * <li>Zero height when hidden (collapses completely)</li>
 * <li>Optional clearing when hiding</li>
 * <li>Works with parent event loop (no separate event handling)</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * 
 * <pre>{@code
 * import static dev.tamboui.toolkit.InlineToolkit.*;
 * import static dev.tamboui.toolkit.Toolkit.*;
 *
 * // State
 * private boolean downloading = true;
 * private double[] progress = {0.0, 0.0, 0.0};
 *
 * @Override
 * protected Element render() {
 *     return column(waveText("Package Installation").cyan(),
 *
 *             // Scope for parallel downloads - collapses when done
 *             scope(downloading, row(text("file1.zip: "), gauge(progress[0])),
 *                     row(text("file2.zip: "), gauge(progress[1])),
 *                     row(text("file3.zip: "), gauge(progress[2]))),
 *
 *             // Always visible
 *             text(downloading ? "Downloading..." : "Complete!").dim());
 * }
 *
 * // When downloads complete:
 * private void onDownloadsComplete() {
 *     downloading = false; // Scope collapses on next render
 *     println(text("All downloads complete!").green());
 * }
 * }</pre>
 *
 * @see dev.tamboui.toolkit.InlineToolkit#scope(Element...)
 */
public final class InlineScopeElement extends ContainerElement<InlineScopeElement> {

    private boolean visible = true;

    /**
     * Creates an empty scope element.
     */
    public InlineScopeElement() {
    }

    /**
     * Creates a scope element with the given children.
     *
     * @param children
     *            the child elements
     */
    public InlineScopeElement(Element... children) {
        this.children.addAll(Arrays.asList(children));
    }

    /**
     * Sets the visibility of this scope.
     * <p>
     * When visible is false, the scope collapses to zero height and its children
     * are not rendered.
     *
     * @param visible
     *            true to show, false to hide
     * @return this scope for chaining
     */
    public InlineScopeElement visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Returns whether this scope is currently visible.
     *
     * @return true if visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Shows this scope (sets visibility to true).
     *
     * @return this scope for chaining
     */
    public InlineScopeElement show() {
        this.visible = true;
        return this;
    }

    /**
     * Hides this scope (sets visibility to false).
     *
     * @return this scope for chaining
     */
    public InlineScopeElement hide() {
        this.visible = false;
        return this;
    }

    /**
     * Adds child elements to this scope.
     *
     * @param children
     *            the children to add
     * @return this scope for chaining
     */
    public InlineScopeElement children(Element... children) {
        this.children.addAll(Arrays.asList(children));
        return this;
    }

    @Override
    public Constraint constraint() {
        if (!visible) {
            // Return zero-height constraint when hidden
            return Constraint.length(0);
        }
        // Use parent's constraint if set, otherwise calculate from children
        Constraint parentConstraint = super.constraint();
        if (parentConstraint != null) {
            return parentConstraint;
        }
        // Calculate height based on children
        int totalHeight = 0;
        for (Element child : children) {
            int childHeight = child.preferredHeight();
            if (childHeight > 0) {
                totalHeight += childHeight;
            } else {
                // If any child has unknown height, use fill
                return Constraint.fill();
            }
        }
        return totalHeight > 0 ? Constraint.length(totalHeight) : Constraint.fill();
    }

    @Override
    public int preferredHeight(int availableWidth, RenderContext context) {
        if (!visible || children.isEmpty() || availableWidth <= 0) {
            return 0;
        }

        // Sum heights of all children
        int totalHeight = 0;
        for (Element child : children) {
            totalHeight += child.preferredHeight(availableWidth, context);
        }
        return totalHeight;
    }

    @Override
    protected void renderContent(Frame frame, Rect area, RenderContext context) {
        // Don't render anything when hidden
        if (!visible || children.isEmpty()) {
            return;
        }

        // Fill background with current style if set
        Style effectiveStyle = context.currentStyle();
        if (effectiveStyle.bg().isPresent()) {
            frame.buffer().setStyle(area, effectiveStyle);
        }

        // Build constraints for vertical layout
        List<Constraint> constraints = new ArrayList<>();
        for (Element child : children) {
            Constraint c = child.constraint();
            // Handle null or Fit constraint by querying preferred height
            if (c == null) {
                // First try text element special case
                if (child instanceof TextElement) {
                    c = ((TextElement) child).calculateHeightConstraint();
                }
                if (c == null) {
                    int preferred = child.preferredHeight();
                    c = preferred > 0 ? Constraint.length(preferred) : Constraint.fill();
                }
            } else if (c instanceof Constraint.Fit) {
                int preferred = child.preferredHeight();
                c = preferred > 0 ? Constraint.length(preferred) : Constraint.fill();
            }
            constraints.add(c);
        }

        // Layout children vertically
        Layout layout = Layout.vertical().constraints(constraints.toArray(new Constraint[0]));
        List<Rect> areas = layout.split(area);

        // Render each child
        for (int i = 0; i < areas.size() && i < children.size(); i++) {
            Element child = children.get(i);
            Rect childArea = areas.get(i);
            context.renderChild(child, frame, childArea);
        }
    }
}

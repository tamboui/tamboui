/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.event.KeyEventHandler;
import dev.tamboui.toolkit.event.MouseEventHandler;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;

/**
 * Base interface for all DSL elements.
 * Elements represent UI components that can be rendered to a frame.
 * <p>
 * Elements can handle events by implementing event handler methods or
 * by registering handler lambdas. Events propagate through the element
 * tree and can be consumed to stop propagation.
 */
public interface Element {

    /**
     * Renders this element to the given frame within the specified area.
     *
     * @param frame the frame to render to
     * @param area the area to render within
     * @param context the render context providing focus and state information
     */
    void render(Frame frame, Rect area, RenderContext context);

    /**
     * Returns the layout constraint for this element, if any.
     * Used by container elements to determine sizing.
     *
     * @return the constraint, or null if the element should use default sizing
     */
    default Constraint constraint() {
        return null;
    }

    /**
     * Returns the preferred width of this element in cells.
     * Used when the element has a {@link Constraint.Fit} constraint.
     *
     * @return the preferred width, or 0 if not applicable
     */
    default int preferredWidth() {
        return 0;
    }

    /**
     * Returns the preferred height of this element in cells.
     * Used when the element has a {@link Constraint.Fit} constraint.
     *
     * @return the preferred height, or 0 if not applicable
     */
    default int preferredHeight() {
        return 0;
    }

    /**
     * Returns the preferred height of this element given an available width and render context.
     * <p>
     * This is useful for elements that may wrap content (like text) where the
     * height depends on the available width. The render context allows CSS-aware
     * height calculation where properties like {@code text-overflow} may be set via CSS.
     * <p>
     * When context is null, implementations should use programmatic property values only.
     *
     * @param availableWidth the available width in cells
     * @param context the render context for CSS resolution, may be null
     * @return the preferred height given the available width
     */
    default int preferredHeight(int availableWidth, RenderContext context) {
        return preferredHeight();
    }

    /**
     * Returns whether this element can receive focus.
     *
     * @return true if focusable, false otherwise
     */
    default boolean isFocusable() {
        return false;
    }

    /**
     * Returns the unique ID of this element, if set.
     * Used for focus management and state tracking.
     *
     * @return the element ID, or null if not set
     */
    default String id() {
        return null;
    }

    /**
     * Handles a key event on this element.
     * Override to handle key events directly on the element.
     *
     * @param event the key event
     * @param focused whether this element is currently focused
     * @return HANDLED if the event was handled, UNHANDLED otherwise
     */
    default EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        return EventResult.UNHANDLED;
    }

    /**
     * Handles a mouse event on this element.
     * Override to handle mouse events directly on the element.
     *
     * @param event the mouse event
     * @return HANDLED if the event was handled, UNHANDLED otherwise
     */
    default EventResult handleMouseEvent(MouseEvent event) {
        return EventResult.UNHANDLED;
    }

    /**
     * Returns the key event handler, if any.
     *
     * @return the handler or null
     */
    default KeyEventHandler keyEventHandler() {
        return null;
    }

    /**
     * Returns the mouse event handler, if any.
     *
     * @return the handler or null
     */
    default MouseEventHandler mouseEventHandler() {
        return null;
    }

    /**
     * Returns whether this element is draggable.
     *
     * @return true if draggable
     */
    default boolean isDraggable() {
        return false;
    }

    /**
     * Returns the last rendered area of this element.
     * Used for hit testing and event routing.
     *
     * @return the last rendered area, or null if not yet rendered
     */
    default Rect renderedArea() {
        return null;
    }
}

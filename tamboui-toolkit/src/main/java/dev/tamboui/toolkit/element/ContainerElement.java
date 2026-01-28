/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

import dev.tamboui.layout.Rect;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for container elements that hold children and forward events to them.
 * <p>
 * When a container receives an event it doesn't handle itself, it forwards the event
 * to its children. This enables interactive elements (lists, text inputs) inside
 * containers to receive events when their parent container is focused.
 * <p>
 * Subclasses should:
 * <ul>
 *   <li>Use {@link #children} to store child elements</li>
 *   <li>Override {@link #renderContent} for layout-specific rendering</li>
 * </ul>
 *
 * @param <T> the concrete container type for method chaining
 */
public abstract class ContainerElement<T extends ContainerElement<T>> extends StyledElement<T> {

    /**
     * Creates a new container element.
     */
    protected ContainerElement() {
    }

    /** The list of child elements in this container. */
    protected final List<Element> children = new ArrayList<>();

    /**
     * Adds a child element.
     *
     * @param child the child to add
     * @return this container for chaining
     */
    public T add(Element child) {
        this.children.add(child);
        return self();
    }

    /**
     * Adds multiple child elements.
     *
     * @param children the children to add
     * @return this container for chaining
     */
    public T add(Element... children) {
        this.children.addAll(Arrays.asList(children));
        return self();
    }

    /**
     * Returns the list of children (for subclass access).
     *
     * @return the children list
     */
    protected List<Element> children() {
        return children;
    }

    /**
     * Handles key events by first trying custom handlers, then forwarding to children.
     * <p>
     * Children receive events with {@code focused=true} since they are within
     * the focused container's subtree.
     *
     * @param event the key event
     * @param focused whether this container is focused
     * @return HANDLED if this container or a child handled the event
     */
    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        // Let custom handler run first if set
        EventResult result = super.handleKeyEvent(event, focused);
        if (result.isHandled()) {
            return result;
        }

        // Forward to children (they inherit focused state from parent)
        for (Element child : children) {
            if (child.handleKeyEvent(event, true) == EventResult.HANDLED) {
                return EventResult.HANDLED;
            }
        }

        return EventResult.UNHANDLED;
    }

    /**
     * Handles mouse events by first trying custom handlers, then forwarding to children.
     * <p>
     * Only forwards to children whose rendered area contains the mouse position,
     * ensuring that mouse events are routed to the correct child in multi-panel layouts.
     *
     * @param event the mouse event
     * @return HANDLED if this container or a child handled the event
     */
    @Override
    public EventResult handleMouseEvent(MouseEvent event) {
        // Let custom handler run first if set
        EventResult result = super.handleMouseEvent(event);
        if (result.isHandled()) {
            return result;
        }

        // Forward to children whose area contains the mouse position
        for (Element child : children) {
            Rect area = child.renderedArea();
            if (area != null && area.contains(event.x(), event.y())) {
                if (child.handleMouseEvent(event) == EventResult.HANDLED) {
                    return EventResult.HANDLED;
                }
            }
        }

        return EventResult.UNHANDLED;
    }
}

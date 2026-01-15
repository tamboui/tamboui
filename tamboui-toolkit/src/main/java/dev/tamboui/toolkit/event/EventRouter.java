/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.layout.Rect;
import dev.tamboui.tui.bindings.ActionHandler;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Routes events to elements based on focus and position.
 * <p>
 * Events are routed as follows:
 * <ul>
 *   <li>Key events go to the focused element</li>
 *   <li>Mouse events go to the element at the mouse position</li>
 *   <li>Tab/Shift+Tab navigate focus</li>
 *   <li>Drag events are tracked and routed to the dragged element</li>
 * </ul>
 * <p>
 * Events can be consumed by handlers to stop propagation.
 */
public final class EventRouter {

    private final FocusManager focusManager;
    private final List<Element> elements = new ArrayList<>();
    private final IdentityHashMap<Element, Rect> elementAreas = new IdentityHashMap<>();
    private final List<GlobalEventHandler> globalHandlers = new ArrayList<>();

    // Drag state
    private Element draggingElement;
    private DragHandler dragHandler;
    private int dragStartX;
    private int dragStartY;

    public EventRouter(FocusManager focusManager) {
        this.focusManager = focusManager;
    }

    /**
     * Adds a global event handler that is called before element-specific handlers.
     * Global handlers can intercept events before they reach elements.
     *
     * @param handler the handler to add
     */
    public void addGlobalHandler(GlobalEventHandler handler) {
        globalHandlers.add(handler);
    }

    /**
     * Adds an action handler as a global event handler.
     * <p>
     * This is a convenience method that wraps the action handler.
     * Events are dispatched to the action handler before reaching elements.
     *
     * @param handler the action handler to add
     */
    public void addGlobalHandler(ActionHandler handler) {
        addGlobalHandler(event -> handler.dispatch(event)
                ? EventResult.HANDLED
                : EventResult.UNHANDLED);
    }

    /**
     * Removes a global event handler.
     *
     * @param handler the handler to remove
     */
    public void removeGlobalHandler(GlobalEventHandler handler) {
        globalHandlers.remove(handler);
    }

    /**
     * Registers an element for event routing.
     * Called during rendering to build the element list.
     * The area is stored internally - elements don't need to track it.
     * <p>
     * If an element is already registered, this updates its area but
     * does not add a duplicate entry.
     */
    public void registerElement(Element element, Rect area) {
        // Prevent duplicate registration (element identity check)
        if (!elementAreas.containsKey(element)) {
            elements.add(element);
        }
        elementAreas.put(element, area);
    }

    /**
     * Gets the rendered area for an element.
     */
    private Rect getArea(Element element) {
        return elementAreas.get(element);
    }

    /**
     * Clears all registered elements.
     * Should be called at the start of each render cycle.
     */
    public void clear() {
        elements.clear();
        elementAreas.clear();
    }

    /**
     * Routes an event to the appropriate element(s).
     * <p>
     * For key events, the focused element is given first chance to handle the event.
     * This allows text inputs to consume character keys before global handlers see them.
     * Global handlers are called after element routing if the event wasn't handled.
     *
     * @param event the event to route
     * @return HANDLED if any handler handled the event, UNHANDLED otherwise
     */
    public EventResult route(Event event) {
        if (event instanceof KeyEvent) {
            return routeKeyEvent((KeyEvent) event);
        }

        // For non-key events, call global handlers first
        for (GlobalEventHandler handler : globalHandlers) {
            EventResult result = handler.handle(event);
            if (result.isHandled()) {
                return result;
            }
        }

        if (event instanceof MouseEvent) {
            return routeMouseEvent((MouseEvent) event);
        }
        return EventResult.UNHANDLED;
    }

    private EventResult routeKeyEvent(KeyEvent event) {
        // Handle focus navigation first
        if (event.isFocusNext()) {
            if (focusManager.focusNext()) {
                return EventResult.HANDLED;
            }
            return EventResult.UNHANDLED;
        }

        if (event.isFocusPrevious()) {
            if (focusManager.focusPrevious()) {
                return EventResult.HANDLED;
            }
            return EventResult.UNHANDLED;
        }

        // Escape cancels drag first
        if (event.isCancel() && draggingElement != null) {
            endDrag(-1, -1);
            return EventResult.HANDLED;
        }

        // Route to focused element first - this lets text inputs consume character keys
        String focusedId = focusManager.focusedId();
        if (focusedId != null) {
            for (Element element : elements) {
                if (focusedId.equals(element.id())) {
                    // Try element's handler
                    EventResult result = element.handleKeyEvent(event, true);
                    if (result.isHandled()) {
                        return result;
                    }
                    // Try lambda handler
                    KeyEventHandler handler = element.keyEventHandler();
                    if (handler != null) {
                        result = handler.handle(event);
                        if (result.isHandled()) {
                            return result;
                        }
                    }
                }
            }
        }

        // Call global handlers after focused element but before unfocused elements
        // This allows global actions (like quit) to work when text input doesn't consume the key
        for (GlobalEventHandler handler : globalHandlers) {
            EventResult result = handler.handle(event);
            if (result.isHandled()) {
                return result;
            }
        }

        // If not consumed, give all elements a chance to handle (for global hotkeys)
        for (Element element : elements) {
            if (focusedId == null || !focusedId.equals(element.id())) {
                EventResult result = element.handleKeyEvent(event, false);
                if (result.isHandled()) {
                    return result;
                }
            }
        }

        // Escape clears focus if no element handled it
        if (event.isCancel() && focusManager.focusedId() != null) {
            focusManager.clearFocus();
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }

    private EventResult routeMouseEvent(MouseEvent event) {
        // Handle ongoing drag
        if (draggingElement != null) {
            if (event.kind() == MouseEventKind.DRAG) {
                int deltaX = event.x() - dragStartX;
                int deltaY = event.y() - dragStartY;
                dragHandler.onDrag(event.x(), event.y(), deltaX, deltaY);
                return EventResult.HANDLED;
            }
            if (event.kind() == MouseEventKind.RELEASE) {
                endDrag(event.x(), event.y());
                return EventResult.HANDLED;
            }
        }

        // Handle new press - check for drag or focus
        if (event.kind() == MouseEventKind.PRESS && event.isLeftButton()) {
            // Find element at position (reverse order for z-ordering)
            for (int i = elements.size() - 1; i >= 0; i--) {
                Element element = elements.get(i);
                Rect area = getArea(element);
                if (area != null && area.contains(event.x(), event.y())) {
                    // Focus the element first (before potential drag)
                    boolean wasFocused = false;
                    if (element.isFocusable() && element.id() != null) {
                        focusManager.setFocus(element.id());
                        wasFocused = true;
                    }

                    // Check if draggable
                    if (element.isDraggable() && element instanceof StyledElement) {
                        StyledElement<?> styled = (StyledElement<?>) element;
                        DragHandler handler = styled.dragHandler();
                        if (handler != null) {
                            startDrag(element, handler, event.x(), event.y());
                            return EventResult.HANDLED;
                        }
                    }

                    // Route to element's handler
                    EventResult result = element.handleMouseEvent(event);
                    if (result.isHandled()) {
                        return result;
                    }
                    MouseEventHandler handler = element.mouseEventHandler();
                    if (handler != null) {
                        result = handler.handle(event);
                        if (result.isHandled()) {
                            return result;
                        }
                    }

                    // Only stop here if we actually did something (focused or had handlers)
                    // Otherwise continue to check elements underneath
                    if (wasFocused) {
                        return EventResult.HANDLED;
                    }
                    // Continue checking other elements - this one didn't handle the click
                }
            }

            // Clicked outside all elements - clear focus
            focusManager.clearFocus();
        }

        // Route other mouse events to element at position
        if (event.kind() == MouseEventKind.MOVE ||
            event.kind() == MouseEventKind.SCROLL_UP ||
            event.kind() == MouseEventKind.SCROLL_DOWN) {

            for (int i = elements.size() - 1; i >= 0; i--) {
                Element element = elements.get(i);
                Rect area = getArea(element);
                if (area != null && area.contains(event.x(), event.y())) {
                    EventResult result = element.handleMouseEvent(event);
                    if (result.isHandled()) {
                        return result;
                    }
                    MouseEventHandler handler = element.mouseEventHandler();
                    if (handler != null) {
                        result = handler.handle(event);
                        if (result.isHandled()) {
                            return result;
                        }
                    }
                }
            }
        }

        return EventResult.UNHANDLED;
    }

    private void startDrag(Element element, DragHandler handler, int x, int y) {
        this.draggingElement = element;
        this.dragHandler = handler;
        this.dragStartX = x;
        this.dragStartY = y;
        handler.onDragStart(x, y);
    }

    private void endDrag(int x, int y) {
        if (dragHandler != null && x >= 0 && y >= 0) {
            dragHandler.onDragEnd(x, y);
        }
        this.draggingElement = null;
        this.dragHandler = null;
    }

    /**
     * Returns whether a drag operation is in progress.
     */
    public boolean isDragging() {
        return draggingElement != null;
    }

    /**
     * Returns the element being dragged.
     */
    public Element draggingElement() {
        return draggingElement;
    }

    /**
     * Returns the number of registered elements (for debugging).
     */
    public int elementCount() {
        return elements.size();
    }
}

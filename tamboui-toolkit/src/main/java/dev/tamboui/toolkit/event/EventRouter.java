/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import dev.tamboui.css.Styleable;
import dev.tamboui.layout.Rect;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.toolkit.jfr.CandidateEvent;
import dev.tamboui.toolkit.jfr.DragStateEvent;
import dev.tamboui.toolkit.jfr.FocusChangeEvent;
import dev.tamboui.toolkit.jfr.FocusNavigationEvent;
import dev.tamboui.toolkit.jfr.GlobalHandlerEvent;
import dev.tamboui.toolkit.jfr.RoutingEvent;
import dev.tamboui.tui.bindings.ActionHandler;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
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
 * <p>
 * The router uses an {@link ElementRegistry} to track element areas by ID,
 * which can be used by external systems (like effects) to look up element positions.
 * <p>
 * Event routing decisions can be traced via JFR events (when enabled).
 */
public final class EventRouter implements AutoCloseable {

    private final FocusManager focusManager;
    private final ElementRegistry elementRegistry;
    private final AtomicLong routeIdCounter = new AtomicLong();
    private final List<Element> elements = new ArrayList<>();
    private final IdentityHashMap<Element, Rect> elementAreas = new IdentityHashMap<>();
    private final List<GlobalEventHandler> globalHandlers = new ArrayList<>();

    // Drag state
    private Element draggingElement;
    private DragHandler dragHandler;
    private int dragStartX;
    private int dragStartY;

    /**
     * Creates a new event router with tracing enabled when available.
    
     * @param focusManager    the focus manager for focus navigation
     * @param elementRegistry the registry for tracking element areas by ID
     */
    public EventRouter(FocusManager focusManager, ElementRegistry elementRegistry) {
        this.focusManager = focusManager;
        this.elementRegistry = elementRegistry;
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
     * <p>
     * Elements are also registered in the {@link ElementRegistry}
     * for CSS-like queries by external systems (like effects).
     *
     * @param element the element to register
     * @param area the element's rendered area
     */
    public void registerElement(Element element, Rect area) {
        // Prevent duplicate registration (element identity check)
        if (!elementAreas.containsKey(element)) {
            elements.add(element);
        }
        elementAreas.put(element, area);

        // Register in ElementRegistry for CSS-like queries
        String id = element.id();
        String type = null;
        Set<String> cssClasses = Collections.emptySet();

        if (element instanceof Styleable) {
            Styleable styleable = (Styleable) element;
            type = styleable.styleType();
            cssClasses = styleable.cssClasses();
        }

        elementRegistry.register(id, type, cssClasses, area);
    }

    /**
     * Gets the rendered area for an element.
     */
    private Rect getArea(Element element) {
        return elementAreas.get(element);
    }

    /**
     * Gets the fully qualified class name of an element.
     */
    private String elementTypeOf(Element element) {
        return element.getClass().getName();
    }

    /**
     * Clears all registered elements.
     * Should be called at the start of each render cycle.
     */
    public void clear() {
        elements.clear();
        elementAreas.clear();
        elementRegistry.clear();
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
        long routeId = routeIdCounter.incrementAndGet();
        RoutingEvent routing = null;
        if (RoutingEvent.enabled()) {
            routing = RoutingEvent.begin(routeId, event, focusManager.focusedId(), elements.size());
        }
        try {
            EventResult result;
            if (event instanceof KeyEvent) {
                result = routeKeyEvent(routeId, (KeyEvent) event);
            } else if (event instanceof MouseEvent) {
                // For non-key events, call global handlers first
                result = routeGlobalHandlers(routeId, event);
                if (result.isUnhandled()) {
                    result = routeMouseEvent(routeId, (MouseEvent) event);
                }
            } else {
                // For non-key events, call global handlers first
                result = routeGlobalHandlers(routeId, event);
            }

            if (routing != null) {
                routing.setResult(result);
            }
            return result;
        } finally {
            if (routing != null) {
                routing.end();
                routing.commit();
            }
        }
    }

    private EventResult routeGlobalHandlers(long routeId, Event event) {
        for (int i = 0; i < globalHandlers.size(); i++) {
            EventResult result = globalHandlers.get(i).handle(event);
            if (GlobalHandlerEvent.enabled()) {
                GlobalHandlerEvent.commit(routeId, i, result);
            }
            if (result.isHandled()) {
                return result;
            }
        }
        return EventResult.UNHANDLED;
    }

    private EventResult routeKeyEvent(long routeId, KeyEvent event) {
        // Handle focus navigation first
        // Check focusPrevious before focusNext because Shift+Tab is more specific than Tab
        if (event.isFocusPrevious()) {
            String fromId = focusManager.focusedId();
            boolean success = focusManager.focusPrevious();
            String toId = focusManager.focusedId();
            if (FocusNavigationEvent.enabled()) {
                FocusNavigationEvent.commit(routeId, "focusPrevious", success, fromId, toId);
            }
            if (success) {
                if (FocusChangeEvent.enabled()) {
                    FocusChangeEvent.commit(routeId, fromId, toId, "Shift+Tab navigation");
                }
                return EventResult.HANDLED;
            }
            return EventResult.UNHANDLED;
        }

        if (event.isFocusNext()) {
            String fromId = focusManager.focusedId();
            boolean success = focusManager.focusNext();
            String toId = focusManager.focusedId();
            if (FocusNavigationEvent.enabled()) {
                FocusNavigationEvent.commit(routeId, "focusNext", success, fromId, toId);
            }
            if (success) {
                if (FocusChangeEvent.enabled()) {
                    FocusChangeEvent.commit(routeId, fromId, toId, "Tab navigation");
                }
                return EventResult.HANDLED;
            }
            return EventResult.UNHANDLED;
        }

        // Escape cancels drag first
        if (event.isCancel() && draggingElement != null) {
            if (DragStateEvent.enabled()) {
                DragStateEvent.commit(routeId, "cancel", draggingElement.id(), -1, -1);
            }
            endDrag(-1, -1);
            return EventResult.HANDLED;
        }

        // Route to focused element first - this lets text inputs consume character keys
        String focusedId = focusManager.focusedId();
        if (focusedId != null) {
            for (Element element : elements) {
                if (focusedId.equals(element.id())) {
                    if (CandidateEvent.enabled()) {
                        CandidateEvent.commit(routeId, element.id(), elementTypeOf(element), "focused", "tried", "element is focused");
                    }
                    // Try element's handler
                    EventResult result = element.handleKeyEvent(event, true);
                    if (result.isHandled()) {
                        if (CandidateEvent.enabled()) {
                            CandidateEvent.commit(routeId, element.id(), elementTypeOf(element), "focused", "handled", "handleKeyEvent returned HANDLED");
                        }
                        return result;
                    }
                    // Handle focus navigation requests
                    if (result == EventResult.FOCUS_NEXT) {
                        focusManager.focusNext();
                        return EventResult.HANDLED;
                    }
                    if (result == EventResult.FOCUS_PREVIOUS) {
                        focusManager.focusPrevious();
                        return EventResult.HANDLED;
                    }
                    // Try lambda handler
                    KeyEventHandler handler = element.keyEventHandler();
                    if (handler != null) {
                        result = handler.handle(event);
                        if (result.isHandled()) {
                            if (CandidateEvent.enabled()) {
                                CandidateEvent.commit(routeId, element.id(), elementTypeOf(element), "focused", "handled", "keyEventHandler returned HANDLED");
                            }
                            return result;
                        }
                        // Handle focus navigation requests from lambda handler
                        if (result == EventResult.FOCUS_NEXT) {
                            focusManager.focusNext();
                            return EventResult.HANDLED;
                        }
                        if (result == EventResult.FOCUS_PREVIOUS) {
                            focusManager.focusPrevious();
                            return EventResult.HANDLED;
                        }
                    }
                    if (CandidateEvent.enabled()) {
                        CandidateEvent.commit(routeId, element.id(), elementTypeOf(element), "focused", "unhandled", null);
                    }
                }
            }
        }

        // Call global handlers after focused element but before unfocused elements
        // This allows global actions (like quit) to work when text input doesn't consume the key
        for (int i = 0; i < globalHandlers.size(); i++) {
            EventResult result = globalHandlers.get(i).handle(event);
            if (GlobalHandlerEvent.enabled()) {
                GlobalHandlerEvent.commit(routeId, i, result);
            }
            if (result.isHandled()) {
                return result;
            }
        }

        // If not consumed, give all elements a chance to handle (for global hotkeys)
        for (Element element : elements) {
            if (focusedId == null || !focusedId.equals(element.id())) {
                if (CandidateEvent.enabled()) {
                    CandidateEvent.commit(routeId, element.id(), elementTypeOf(element), "unfocused", "tried", null);
                }
                EventResult result = element.handleKeyEvent(event, false);
                if (result.isHandled()) {
                    if (CandidateEvent.enabled()) {
                        CandidateEvent.commit(routeId, element.id(), elementTypeOf(element), "unfocused", "handled", "handleKeyEvent returned HANDLED");
                    }
                    return result;
                }
            }
        }

        // Escape clears focus if no element handled it
        if (event.isCancel() && focusManager.focusedId() != null) {
            String prevFocus = focusManager.focusedId();
            focusManager.clearFocus();
            if (FocusChangeEvent.enabled()) {
                FocusChangeEvent.commit(routeId, prevFocus, null, "Escape clears focus");
            }
            return EventResult.HANDLED;
        }

        return EventResult.UNHANDLED;
    }

    private EventResult routeMouseEvent(long routeId, MouseEvent event) {
        // Handle ongoing drag
        if (draggingElement != null) {
            if (event.kind() == MouseEventKind.DRAG) {
                int deltaX = event.x() - dragStartX;
                int deltaY = event.y() - dragStartY;
                if (DragStateEvent.enabled()) {
                    DragStateEvent.commit(routeId, "drag", draggingElement.id(), event.x(), event.y());
                }
                dragHandler.onDrag(event.x(), event.y(), deltaX, deltaY);
                return EventResult.HANDLED;
            }
            if (event.kind() == MouseEventKind.RELEASE) {
                if (DragStateEvent.enabled()) {
                    DragStateEvent.commit(routeId, "end", draggingElement.id(), event.x(), event.y());
                }
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
                    if (CandidateEvent.enabled()) {
                        CandidateEvent.commit(routeId, element.id(), elementTypeOf(element), "mouse_press", "hit", "at (" + event.x() + "," + event.y() + ")");
                    }

                    // Focus the element first (before potential drag)
                    boolean wasFocused = false;
                    if (element.isFocusable() && element.id() != null) {
                        String prevFocus = focusManager.focusedId();
                        focusManager.setFocus(element.id());
                        wasFocused = true;
                        if (!element.id().equals(prevFocus)) {
                            if (FocusChangeEvent.enabled()) {
                                FocusChangeEvent.commit(routeId, prevFocus, element.id(), "click to focus");
                            }
                        }
                    }

                    // Check if draggable
                    if (element.isDraggable() && element instanceof StyledElement) {
                        StyledElement<?> styled = (StyledElement<?>) element;
                        DragHandler handler = styled.dragHandler();
                        if (handler != null) {
                            startDrag(routeId, element, handler, event.x(), event.y());
                            return EventResult.HANDLED;
                        }
                    }

                    // Route to element's handler
                    EventResult result = element.handleMouseEvent(event);
                    if (result.isHandled()) {
                        if (CandidateEvent.enabled()) {
                            CandidateEvent.commit(routeId, element.id(), elementTypeOf(element), "mouse_press", "handled", "handleMouseEvent returned HANDLED");
                        }
                        return result;
                    }
                    MouseEventHandler handler = element.mouseEventHandler();
                    if (handler != null) {
                        result = handler.handle(event);
                        if (result.isHandled()) {
                            if (CandidateEvent.enabled()) {
                                CandidateEvent.commit(routeId, element.id(), elementTypeOf(element), "mouse_press", "handled", "mouseEventHandler returned HANDLED");
                            }
                            return result;
                        }
                    }

                    // Only stop here if we actually did something (focused or had handlers)
                    // Otherwise continue to check elements underneath
                    if (wasFocused) {
                        if (CandidateEvent.enabled()) {
                            CandidateEvent.commit(routeId, element.id(), elementTypeOf(element), "mouse_press", "handled", "click focused element");
                        }
                        return EventResult.HANDLED;
                    }
                    // Continue checking other elements - this one didn't handle the click
                }
            }

            // Clicked outside all elements - clear focus
            String prevFocus = focusManager.focusedId();
            if (prevFocus != null) {
                focusManager.clearFocus();
                if (FocusChangeEvent.enabled()) {
                    FocusChangeEvent.commit(routeId, prevFocus, null, "clicked outside all elements");
                }
            }
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

    private void startDrag(long routeId, Element element, DragHandler handler, int x, int y) {
        this.draggingElement = element;
        this.dragHandler = handler;
        this.dragStartX = x;
        this.dragStartY = y;
        if (DragStateEvent.enabled()) {
            DragStateEvent.commit(routeId, "start", element.id(), x, y);
        }
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
     * Closes the event router and releases resources.
     */
    @Override
    public void close() {
    }

    /**
     * Returns whether a drag operation is in progress.
     *
     * @return true if a drag is in progress
     */
    public boolean isDragging() {
        return draggingElement != null;
    }

    /**
     * Returns the element being dragged.
     *
     * @return the element being dragged, or null if no drag is in progress
     */
    public Element draggingElement() {
        return draggingElement;
    }

    /**
     * Returns the number of registered elements (for debugging).
     *
     * @return the number of registered elements
     */
    public int elementCount() {
        return elements.size();
    }

    /**
     * Returns the element registry used by this router.
     * <p>
     * The registry contains ID-to-area mappings for all elements with IDs.
     * External systems (like effects) can use this to look up element positions.
     *
     * @return the element registry
     */
    public ElementRegistry elementRegistry() {
        return elementRegistry;
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.app;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.layout.Rect;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.event.EventRouter;
import dev.tamboui.toolkit.event.EventRouterResult;
import dev.tamboui.toolkit.event.GlobalEventHandler;
import dev.tamboui.toolkit.event.TestElement;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.tui.event.Event;

/**
 * A headless test harness for testing event routing without a TUI backend.
 * <p>
 * Provides a fluent API for setting up elements and sending events:
 *
 * <pre>{@code
 * EventRouterTestHarness harness = EventRouterTestHarness.create()
 *     .withElement(new TestElement("input1").focusable(), new Rect(0, 0, 10, 1))
 *     .withElement(new TestElement("input2").focusable(), new Rect(0, 1, 10, 1));
 *
 * harness.send(KeyEvent.ofKey(KeyCode.TAB))
 *     .assertHandled()
 *     .assertFocusMovedTo("input2");
 * }</pre>
 * <p>
 * Note: This bypasses render thread checks since tests don't run on a render thread.
 * The check passes when no render thread has been set (per RenderThread design).
 */
public final class EventRouterTestHarness {

    private final FocusManager focusManager = new FocusManager();
    private final ElementRegistry elementRegistry = new ElementRegistry();
    private final EventRouter eventRouter = new EventRouter(focusManager, elementRegistry);

    private final List<ElementArea> elements = new ArrayList<>();
    private String initialFocusId;
    private boolean clearFocusRequested;

    /**
     * Creates a new harness with no elements and no initial focus.
     *
     * @return a new EventRouterTestHarness
     */
    public static EventRouterTestHarness create() {
        return new EventRouterTestHarness();
    }

    private EventRouterTestHarness() {
    }

    /**
     * Registers an element with the given area for the next {@link #send(Event)}.
     *
     * @param element the element (e.g. {@link TestElement})
     * @param area    the rendered area
     * @return this harness for chaining
     */
    public EventRouterTestHarness withElement(Element element, Rect area) {
        elements.add(new ElementArea(element, area));
        return this;
    }

    /**
     * Sets the initial focus for the next send (applied after registering elements).
     * Call with {@code null} to start with no focus (clears auto-focus).
     *
     * @param elementId the id to focus, or null to clear focus
     * @return this harness for chaining
     */
    public EventRouterTestHarness withFocus(String elementId) {
        this.initialFocusId = elementId;
        this.clearFocusRequested = (elementId == null);
        return this;
    }

    /**
     * Adds a global event handler (called before element handlers).
     *
     * @param handler the handler
     * @return this harness for chaining
     */
    public EventRouterTestHarness withGlobalHandler(GlobalEventHandler handler) {
        eventRouter.addGlobalHandler(handler);
        return this;
    }

    /**
     * Routes one event and returns a result for assertions.
     * Before routing, clears and re-registers all elements and applies initial focus.
     *
     * @param event the event to route
     * @return result with routing outcome and focus before/after
     */
    public EventRouterResult send(Event event) {
        String focusBefore = focusManager.focusedId();

        eventRouter.clear();
        focusManager.clearFocusables();

        for (ElementArea ea : elements) {
            eventRouter.registerElement(ea.element, ea.area);
            if (ea.element.isFocusable() && ea.element.id() != null) {
                focusManager.registerFocusable(ea.element.id(), ea.area);
            }
        }

        if (clearFocusRequested) {
            focusManager.clearFocus();
        } else if (initialFocusId != null) {
            focusManager.setFocus(initialFocusId);
        }
        initialFocusId = null;
        clearFocusRequested = false;

        EventResult result = eventRouter.route(event);
        String focusAfter = focusManager.focusedId();

        return new EventRouterResult(result, focusBefore, focusAfter, this);
    }

    /**
     * Returns the focus manager.
     *
     * @return the FocusManager
     */
    public FocusManager focusManager() {
        return focusManager;
    }

    /**
     * Returns the event router.
     *
     * @return the EventRouter
     */
    public EventRouter eventRouter() {
        return eventRouter;
    }

    /**
     * Returns the element registry.
     *
     * @return the ElementRegistry
     */
    public ElementRegistry elementRegistry() {
        return elementRegistry;
    }

    /**
     * Returns the currently focused element id.
     *
     * @return the focused id, or null
     */
    public String focusedId() {
        return focusManager.focusedId();
    }

    /**
     * Clears registered elements and initial focus (global handlers remain).
     * Use to reuse the harness for a new scenario.
     *
     * @return this harness for chaining
     */
    public EventRouterTestHarness reset() {
        elements.clear();
        initialFocusId = null;
        clearFocusRequested = false;
        eventRouter.clear();
        focusManager.clearFocusables();
        focusManager.clearFocus();
        return this;
    }

    private static final class ElementArea {
        final Element element;
        final Rect area;

        ElementArea(Element element, Rect area) {
            this.element = element;
            this.area = area;
        }
    }
}

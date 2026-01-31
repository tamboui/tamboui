/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.test;

import dev.tamboui.layout.Rect;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.event.EventRouter;
import dev.tamboui.toolkit.event.GlobalEventHandler;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.tui.bindings.ActionHandler;
import dev.tamboui.tui.event.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A headless test harness for testing event routing without a TUI backend.
 * <p>
 * Provides a fluent API for setting up elements and sending events:
 *
 * <pre>{@code
 * TestToolkitApp app = TestToolkitApp.create()
 *     .withElement(TestElement.create("input1").focusable(), new Rect(0, 0, 10, 1))
 *     .withElement(TestElement.create("input2").focusable(), new Rect(0, 1, 10, 1));
 *
 * app.send(KeyEvent.ofKey(KeyCode.TAB))
 *     .assertHandled()
 *     .assertFocusMovedTo("input2");
 * }</pre>
 * <p>
 * Note: This bypasses render thread checks since tests don't run on a render thread.
 * The check passes when no render thread has been set (per RenderThread design).
 */
public final class TestToolkitApp {

    private final FocusManager focusManager;
    private final ElementRegistry elementRegistry;
    private final EventRouter eventRouter;
    private final List<TestElement> elements = new ArrayList<>();

    private TestToolkitApp() {
        this.focusManager = new FocusManager();
        this.elementRegistry = new ElementRegistry();
        this.eventRouter = new EventRouter(focusManager, elementRegistry);
    }

    /**
     * Creates a new test app.
     *
     * @return a new test app
     */
    public static TestToolkitApp create() {
        return new TestToolkitApp();
    }

    /**
     * Adds an element to the app at the specified area.
     * <p>
     * The element is registered with both the event router and focus manager
     * (if focusable).
     *
     * @param element the test element
     * @param area    the element's area
     * @return this app for chaining
     */
    public TestToolkitApp withElement(TestElement element, Rect area) {
        elements.add(element);
        eventRouter.registerElement(element, area);
        if (element.isFocusable() && element.id() != null) {
            focusManager.registerFocusable(element.id(), area);
        }
        return this;
    }

    /**
     * Adds a global event handler.
     *
     * @param handler the global handler
     * @return this app for chaining
     */
    public TestToolkitApp withGlobalHandler(GlobalEventHandler handler) {
        eventRouter.addGlobalHandler(handler);
        return this;
    }

    /**
     * Adds an action handler as a global event handler.
     *
     * @param handler the action handler
     * @return this app for chaining
     */
    public TestToolkitApp withGlobalHandler(ActionHandler handler) {
        eventRouter.addGlobalHandler(handler);
        return this;
    }

    /**
     * Sets the initial focus to the element with the given ID.
     *
     * @param elementId the element ID to focus
     * @return this app for chaining
     */
    public TestToolkitApp withFocus(String elementId) {
        focusManager.setFocus(elementId);
        return this;
    }

    /**
     * Sends an event through the router and returns the result.
     *
     * @param event the event to send
     * @return the result with fluent assertions
     */
    public EventRouterResult send(Event event) {
        String focusBefore = focusManager.focusedId();
        EventResult result = eventRouter.route(event);
        String focusAfter = focusManager.focusedId();
        return new EventRouterResult(result, focusBefore, focusAfter, this);
    }

    /**
     * Returns the focus manager.
     *
     * @return the focus manager
     */
    public FocusManager focusManager() {
        return focusManager;
    }

    /**
     * Returns the event router.
     *
     * @return the event router
     */
    public EventRouter eventRouter() {
        return eventRouter;
    }

    /**
     * Returns the element registry.
     *
     * @return the element registry
     */
    public ElementRegistry elementRegistry() {
        return elementRegistry;
    }

    /**
     * Returns the currently focused element ID.
     *
     * @return the focused ID, or null if nothing is focused
     */
    public String focusedId() {
        return focusManager.focusedId();
    }

    /**
     * Returns all registered elements.
     *
     * @return unmodifiable list of elements
     */
    public List<TestElement> elements() {
        return Collections.unmodifiableList(elements);
    }

    /**
     * Returns the element with the given ID.
     *
     * @param elementId the element ID
     * @return the element, or null if not found
     */
    public TestElement element(String elementId) {
        for (TestElement element : elements) {
            if (elementId.equals(element.id())) {
                return element;
            }
        }
        return null;
    }

    /**
     * Clears all recorded events from all elements.
     */
    public void clearRecordedEvents() {
        for (TestElement element : elements) {
            element.clearRecordedEvents();
        }
    }

    /**
     * Clears the router state and re-registers all elements.
     * <p>
     * Call this at the start of each test scenario to simulate a fresh render.
     */
    public void reset() {
        eventRouter.clear();
        focusManager.clearFocusables();
        for (TestElement element : elements) {
            Rect area = element.renderedArea();
            if (area != null) {
                eventRouter.registerElement(element, area);
                if (element.isFocusable() && element.id() != null) {
                    focusManager.registerFocusable(element.id(), area);
                }
            }
        }
    }
}

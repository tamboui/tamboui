/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.test;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.event.KeyEventHandler;
import dev.tamboui.toolkit.event.MouseEventHandler;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A mock element for testing event routing and focus management.
 * <p>
 * Provides configurable behavior for focus, event handling, and
 * records all received events for later assertion.
 *
 * <pre>{@code
 * TestElement element = TestElement.create("input1")
 *     .focusable()
 *     .handlesAllKeys();
 *
 * // After routing events
 * assertThat(element.keyEvents()).hasSize(2);
 * assertThat(element.receivedFocusedKeyEvent()).isTrue();
 * }</pre>
 */
public final class TestElement implements Element {

    private final String elementId;
    private boolean focusable;
    private boolean handlesAllKeys;
    private boolean handlesAllMouse;
    private KeyEventHandler keyHandler;
    private MouseEventHandler mouseHandler;
    private Rect lastRenderedArea;

    // Event recording
    private final List<KeyEvent> keyEvents = new ArrayList<>();
    private final List<MouseEvent> mouseEvents = new ArrayList<>();
    private final List<Boolean> focusedStates = new ArrayList<>();

    private TestElement(String elementId) {
        this.elementId = elementId;
    }

    /**
     * Creates a new test element with the given ID.
     *
     * @param elementId the element ID
     * @return a new test element
     */
    public static TestElement create(String elementId) {
        return new TestElement(elementId);
    }

    /**
     * Makes this element focusable.
     *
     * @return this element for chaining
     */
    public TestElement focusable() {
        this.focusable = true;
        return this;
    }

    /**
     * Sets whether this element is focusable.
     *
     * @param focusable true to make focusable
     * @return this element for chaining
     */
    public TestElement focusable(boolean focusable) {
        this.focusable = focusable;
        return this;
    }

    /**
     * Configures this element to handle all key events when focused.
     *
     * @return this element for chaining
     */
    public TestElement handlesAllKeys() {
        this.handlesAllKeys = true;
        return this;
    }

    /**
     * Configures this element to handle all mouse events.
     *
     * @return this element for chaining
     */
    public TestElement handlesAllMouse() {
        this.handlesAllMouse = true;
        return this;
    }

    /**
     * Sets a custom key event handler.
     *
     * @param handler the handler
     * @return this element for chaining
     */
    public TestElement onKeyEvent(KeyEventHandler handler) {
        this.keyHandler = handler;
        return this;
    }

    /**
     * Sets a custom mouse event handler.
     *
     * @param handler the handler
     * @return this element for chaining
     */
    public TestElement onMouseEvent(MouseEventHandler handler) {
        this.mouseHandler = handler;
        return this;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        this.lastRenderedArea = area;
    }

    @Override
    public Constraint constraint() {
        return null;
    }

    @Override
    public boolean isFocusable() {
        return focusable;
    }

    @Override
    public String id() {
        return elementId;
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        keyEvents.add(event);
        focusedStates.add(focused);

        // Don't call keyHandler here - the EventRouter will call keyEventHandler() separately
        // This avoids double-calling the handler

        if (focused && handlesAllKeys) {
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    @Override
    public EventResult handleMouseEvent(MouseEvent event) {
        mouseEvents.add(event);

        if (mouseHandler != null) {
            return mouseHandler.handle(event);
        }

        if (handlesAllMouse) {
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    @Override
    public KeyEventHandler keyEventHandler() {
        return keyHandler;
    }

    @Override
    public MouseEventHandler mouseEventHandler() {
        return mouseHandler;
    }

    @Override
    public Rect renderedArea() {
        return lastRenderedArea;
    }

    // ═══════════════════════════════════════════════════════════════
    // Event recording accessors for testing assertions
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns all received key events.
     *
     * @return the list of key events
     */
    public List<KeyEvent> keyEvents() {
        return keyEvents;
    }

    /**
     * Returns all received mouse events.
     *
     * @return the list of mouse events
     */
    public List<MouseEvent> mouseEvents() {
        return mouseEvents;
    }

    /**
     * Returns whether any key event was received while this element was focused.
     *
     * @return true if a focused key event was received
     */
    public boolean receivedFocusedKeyEvent() {
        return focusedStates.contains(true);
    }

    /**
     * Returns whether any key event was received while this element was NOT focused.
     *
     * @return true if an unfocused key event was received
     */
    public boolean receivedUnfocusedKeyEvent() {
        return focusedStates.contains(false);
    }

    /**
     * Returns the focused states for each received key event.
     *
     * @return the list of focused states (parallel to keyEvents)
     */
    public List<Boolean> focusedStates() {
        return focusedStates;
    }

    /**
     * Clears all recorded events.
     */
    public void clearRecordedEvents() {
        keyEvents.clear();
        mouseEvents.clear();
        focusedStates.clear();
    }
}

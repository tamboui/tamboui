/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;

/**
 * A mock element for testing event routing and focus management.
 * <p>
 * Provides configurable behavior for focus, event handling, and
 * records all received events for later assertion.
 *
 * <pre>{@code
 * TestElement element = new TestElement("input1")
 *     .focusable()
 *     .handlesAllKeys();
 *
 * // After routing events
 * assertThat(element.keyEvents()).hasSize(2);
 * assertThat(element.receivedFocusedKeyEvent()).isTrue();
 * }</pre>
 */
public final class TestElement implements Element {

    private String id;
    private boolean focusable;
    private EventResult keyEventResult = EventResult.UNHANDLED;
    private EventResult mouseEventResult = EventResult.UNHANDLED;
    private KeyEventHandler keyEventHandler;
    private MouseEventHandler mouseEventHandler;
    private Rect lastRenderedArea;

     // Event recording
     private final List<KeyEvent> keyEvents = new ArrayList<>();
     private final List<MouseEvent> mouseEvents = new ArrayList<>();
     private final List<Boolean> focusedStates = new ArrayList<>();

    /**
     * Creates a test element with no id (call {@link #id(String)} to set).
     */
    public TestElement() {
    }

    /**
     * Creates a test element with the given id.
     *
     * @param id the element id
     */
    public TestElement(String id) {
        this.id = id;
    }

    /**
     * Sets the element id.
     *
     * @param id the id
     * @return this element for chaining
     */
    public TestElement id(String id) {
        this.id = id;
        return this;
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
     * Makes this element report HANDLED for all key events (before any handler).
     *
     * @return this element for chaining
     */
    public TestElement handlesAllKeys() {
        this.keyEventResult = EventResult.HANDLED;
        return this;
    }

    /**
     * Sets the result to return from handleKeyEvent when no custom handler is set.
     *
     * @param result HANDLED or UNHANDLED
     * @return this element for chaining
     */
    public TestElement keyEventResult(EventResult result) {
        this.keyEventResult = result;
        return this;
    }

    /**
     * Sets the result to return from handleMouseEvent when no custom handler is set.
     *
     * @param result HANDLED or UNHANDLED
     * @return this element for chaining
     */
    public TestElement mouseEventResult(EventResult result) {
        this.mouseEventResult = result;
        return this;
    }

    /**
     * Sets a custom key event handler.
     *
     * @param handler the handler
     * @return this element for chaining
     */
    public TestElement onKeyEvent(KeyEventHandler handler) {
        this.keyEventHandler = handler;
        return this;
    }

    /**
     * Sets a custom mouse event handler.
     *
     * @param handler the handler
     * @return this element for chaining
     */
    public TestElement onMouseEvent(MouseEventHandler handler) {
        this.mouseEventHandler = handler;
        return this;
    }

    /**
     * Returns an unmodifiable list of key events received by this element.
     *
     * @return the list of key events
     */
    public List<KeyEvent> keyEvents() {
        return Collections.unmodifiableList(keyEvents);
    }

    /**
     * Returns an unmodifiable list of mouse events received by this element.
     *
     * @return the list of mouse events
     */
    public List<MouseEvent> mouseEvents() {
        return Collections.unmodifiableList(mouseEvents);
    }

    /**
    * Returns the focused state for each received key event (parallel to {@link #keyEvents()}).
     *
     * @return unmodifiable list of focused flags
     */
    public List<Boolean> focusedStates() {
        return Collections.unmodifiableList(focusedStates);
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
     * Returns whether any key event was received while this element was not focused.
     *
     * @return true if an unfocused key event was received
     */
    public boolean receivedUnfocusedKeyEvent() {
        return focusedStates.contains(false);
    }

    /**
     * Clears recorded events (for reuse in tests).
     *
     * @return this element for chaining
     */
    public TestElement clearRecordedEvents() {
        keyEvents.clear();
        mouseEvents.clear();
        focusedStates.clear();
        return this;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        this.lastRenderedArea = area;
    }

    @Override
    public int preferredWidth() {
        return 0;
    }

    @Override
    public int preferredHeight() {
        return 0;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean isFocusable() {
        return focusable;
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        keyEvents.add(event);
        focusedStates.add(focused);
        if (keyEventHandler != null) {
            return keyEventHandler.handle(event);
        }
        return keyEventResult;
    }

    @Override
    public EventResult handleMouseEvent(MouseEvent event) {
        mouseEvents.add(event);
        if (mouseEventHandler != null) {
            return mouseEventHandler.handle(event);
        }
        return mouseEventResult;
    }

    @Override
    public KeyEventHandler keyEventHandler() {
        return keyEventHandler;
    }

    @Override
    public MouseEventHandler mouseEventHandler() {
        return mouseEventHandler;
    }

    @Override
    public Rect renderedArea() {
        return lastRenderedArea;
    }
}

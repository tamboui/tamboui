/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.component;

import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.element.StyledElement;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.focus.Focusable;
import dev.tamboui.tui.bindings.ActionHandler;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;

/**
 * Base class for stateful components with event handling.
 * <p>
 * Components handle key and mouse events when focused using
 * {@code @OnAction} annotations:
 *
 * <pre>{@code
 * public class CounterComponent extends Component<CounterComponent> {
 *     private int count = 0;
 *
 *     @OnAction(Actions.MOVE_UP)
 *     void increment(Event event) {
 *         count++;
 *     }
 *
 *     @Override
 *     protected Element render() {
 *         return text("Count: " + count);
 *     }
 * }
 * }</pre>
 */
public abstract class Component<T extends Component<T>> extends StyledElement<T> implements Focusable {

    private ActionHandler actionHandler;
    private RenderContext currentRenderContext;

    @Override
    public boolean isFocusable() {
        return true;
    }

    /**
     * Returns whether this component is currently focused.
     *
     * @return true if focused
     */
    protected boolean isFocused() {
        return currentRenderContext != null && currentRenderContext.isFocused(elementId);
    }

    /**
     * Renders the component's content.
     * Subclasses must implement this to define the component's appearance.
     *
     * @return the element tree to render
     */
    protected abstract Element render();

    @Override
    protected final void renderContent(Frame frame, Rect area, RenderContext renderContext) {
        if (elementId == null) {
            throw new IllegalStateException(
                    "Component " + getClass().getSimpleName() + " must have an id. Use .id(\"myId\") when creating the component.");
        }

        DefaultRenderContext internalContext = (DefaultRenderContext) renderContext;
        this.currentRenderContext = renderContext;

        // Create ActionHandler on first render
        if (actionHandler == null) {
            actionHandler = new ActionHandler(internalContext.bindings())
                    .registerAnnotated(this);
        }

        // Register as focusable
        internalContext.focusManager().registerFocusable(elementId, area);

        // Render the component's content
        Element content = render();
        if (content != null) {
            content.render(frame, area, renderContext);
        }
    }

    /**
     * Called internally to handle key events.
     * Dispatches to {@code @OnAction} annotated methods via ActionHandler.
     */
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        if (focused && actionHandler != null && actionHandler.dispatch(event)) {
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }

    /**
     * Called internally to handle mouse events.
     * Dispatches to {@code @OnAction} annotated methods via ActionHandler.
     */
    public EventResult handleMouseEvent(MouseEvent event) {
        if (actionHandler != null && actionHandler.dispatch(event)) {
            return EventResult.HANDLED;
        }
        return EventResult.UNHANDLED;
    }
}

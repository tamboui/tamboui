/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.event.KeyEventHandler;
import dev.tamboui.toolkit.event.MouseEventHandler;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;

import java.util.function.Supplier;

/**
 * An element that lazily evaluates its content on each render.
 * <p>
 * This allows state to be captured in the supplier closure,
 * enabling encapsulated component-local state:
 * <pre>{@code
 * int count = 0;
 * panel("Counter", () -> text("Count: " + count))
 * }</pre>
 */
public final class LazyElement implements Element {

    private final Supplier<? extends Element> supplier;
    private Element lastElement;

    /**
     * Creates a new lazy element with the given supplier.
     *
     * @param supplier the supplier that produces the element on each render
     */
    public LazyElement(Supplier<? extends Element> supplier) {
        this.supplier = supplier;
    }

    @Override
    public void render(Frame frame, Rect area, RenderContext context) {
        lastElement = supplier.get();
        if (lastElement != null) {
            context.renderChild(lastElement, frame, area);
        }
    }

    @Override
    public int preferredWidth() {
        // Evaluate to get preferred width if not yet rendered
        if (lastElement == null) {
            lastElement = supplier.get();
        }
        return lastElement != null ? lastElement.preferredWidth() : 0;
    }

    @Override
    public int preferredHeight() {
        // Evaluate to get preferred height if not yet rendered
        if (lastElement == null) {
            lastElement = supplier.get();
        }
        return lastElement != null ? lastElement.preferredHeight() : 0;
    }

    @Override
    public int preferredHeight(int availableWidth, RenderContext context) {
        // Evaluate to get preferred height if not yet rendered
        if (lastElement == null) {
            lastElement = supplier.get();
        }
        return lastElement != null ? lastElement.preferredHeight(availableWidth, context) : 0;
    }

    @Override
    public Constraint constraint() {
        // Evaluate to get constraint if not yet rendered
        if (lastElement == null) {
            lastElement = supplier.get();
        }
        return lastElement != null ? lastElement.constraint() : null;
    }

    @Override
    public String id() {
        return lastElement != null ? lastElement.id() : null;
    }

    @Override
    public boolean isFocusable() {
        return lastElement != null && lastElement.isFocusable();
    }

    @Override
    public KeyEventHandler keyEventHandler() {
        return lastElement != null ? lastElement.keyEventHandler() : null;
    }

    @Override
    public MouseEventHandler mouseEventHandler() {
        return lastElement != null ? lastElement.mouseEventHandler() : null;
    }

    @Override
    public boolean isDraggable() {
        return lastElement != null && lastElement.isDraggable();
    }

    @Override
    public Rect renderedArea() {
        return lastElement != null ? lastElement.renderedArea() : null;
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        return lastElement != null ? lastElement.handleKeyEvent(event, focused) : EventResult.UNHANDLED;
    }

    @Override
    public EventResult handleMouseEvent(MouseEvent event) {
        return lastElement != null ? lastElement.handleMouseEvent(event) : EventResult.UNHANDLED;
    }
}

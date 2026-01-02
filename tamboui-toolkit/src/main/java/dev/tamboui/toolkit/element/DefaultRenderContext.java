/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

import dev.tamboui.toolkit.event.EventRouter;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.toolkit.component.ComponentTree;
import dev.tamboui.layout.Rect;

/**
 * Default implementation of RenderContext with internal framework methods.
 * <p>
 * This class is used internally by the toolkit. User code should only
 * interact with the {@link RenderContext} interface.
 */
public final class DefaultRenderContext implements RenderContext {

    private final FocusManager focusManager;
    private final ComponentTree componentTree;
    private final EventRouter eventRouter;

    public DefaultRenderContext(FocusManager focusManager, ComponentTree componentTree, EventRouter eventRouter) {
        this.focusManager = focusManager;
        this.componentTree = componentTree;
        this.eventRouter = eventRouter;
    }

    /**
     * Creates an empty context for simple rendering without focus management.
     */
    public static DefaultRenderContext createEmpty() {
        FocusManager fm = new FocusManager();
        return new DefaultRenderContext(fm, new ComponentTree(), new EventRouter(fm));
    }

    // ═══════════════════════════════════════════════════════════════
    // Public API (from RenderContext interface)
    // ═══════════════════════════════════════════════════════════════

    @Override
    public boolean isFocused(String elementId) {
        return focusManager.isFocused(elementId);
    }

    @Override
    public boolean hasFocus() {
        return focusManager.focusedId() != null;
    }

    // ═══════════════════════════════════════════════════════════════
    // Internal API (for framework use only)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns the focus manager.
     * <p>
     * Internal use only.
     */
    public FocusManager focusManager() {
        return focusManager;
    }

    /**
     * Returns the component tree.
     * <p>
     * Internal use only.
     */
    public ComponentTree componentTree() {
        return componentTree;
    }

    /**
     * Returns the event router.
     * <p>
     * Internal use only.
     */
    public EventRouter eventRouter() {
        return eventRouter;
    }

    /**
     * Registers an element for event routing and focus management.
     * Called by container elements after rendering children.
     * <p>
     * Internal use only.
     *
     * @param element the element to register
     * @param area the rendered area
     */
    public void registerElement(Element element, Rect area) {
        eventRouter.registerElement(element, area);
        if (element.isFocusable() && element.id() != null) {
            focusManager.registerFocusable(element.id(), area);
        }
    }
}

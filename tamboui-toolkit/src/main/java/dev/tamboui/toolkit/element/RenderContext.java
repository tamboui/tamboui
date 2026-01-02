/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

/**
 * Context provided during rendering, giving access to focus state.
 * <p>
 * This interface exposes only what user code needs during rendering.
 * Internal framework functionality is handled automatically.
 */
public interface RenderContext {

    /**
     * Returns whether the element with the given ID is currently focused.
     *
     * @param elementId the element ID to check
     * @return true if focused, false otherwise
     */
    boolean isFocused(String elementId);

    /**
     * Returns whether any element is currently focused.
     *
     * @return true if an element is focused
     */
    boolean hasFocus();

    /**
     * Creates an empty context for simple rendering without focus management.
     * Primarily useful for testing.
     */
    static RenderContext empty() {
        return DefaultRenderContext.createEmpty();
    }
}

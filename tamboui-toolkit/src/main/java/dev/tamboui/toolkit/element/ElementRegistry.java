/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

import dev.tamboui.layout.Rect;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry that maps element IDs to their rendered areas.
 * <p>
 * ElementRegistry is populated during the render pass as elements are rendered.
 * It provides a way to look up element areas by ID, which is useful for
 * effect systems that need to target specific elements without holding
 * direct references to Element objects.
 * <p>
 * The registry should be cleared at the start of each render cycle and
 * repopulated as elements render themselves.
 *
 * <pre>{@code
 * ElementRegistry registry = new ElementRegistry();
 *
 * // During render, elements register themselves
 * registry.register("header", headerArea);
 * registry.register("content", contentArea);
 *
 * // Later, look up areas by ID
 * Rect headerArea = registry.getArea("header");
 * }</pre>
 */
public final class ElementRegistry {

    private final Map<String, Rect> elementAreas = new HashMap<>();

    /**
     * Registers an element's rendered area by ID.
     * <p>
     * If an element with the same ID is already registered, its area is updated.
     *
     * @param elementId the element ID
     * @param area      the rendered area
     */
    public void register(String elementId, Rect area) {
        if (elementId != null && area != null) {
            elementAreas.put(elementId, area);
        }
    }

    /**
     * Returns the rendered area for an element by ID.
     *
     * @param elementId the element ID
     * @return the element's area, or null if not registered
     */
    public Rect getArea(String elementId) {
        if (elementId == null) {
            return null;
        }
        return elementAreas.get(elementId);
    }

    /**
     * Returns whether an element with the given ID is registered.
     *
     * @param elementId the element ID
     * @return true if registered
     */
    public boolean contains(String elementId) {
        return elementId != null && elementAreas.containsKey(elementId);
    }

    /**
     * Clears all registered elements.
     * <p>
     * Should be called at the start of each render cycle.
     */
    public void clear() {
        elementAreas.clear();
    }

    /**
     * Returns the number of registered elements.
     *
     * @return the count
     */
    public int size() {
        return elementAreas.size();
    }
}

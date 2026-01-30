/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.List;

import dev.tamboui.layout.Rect;

/**
 * Registry that stores information about styled areas rendered to a buffer.
 * <p>
 * StyledAreaRegistry complements ElementRegistry by tracking styled spans
 * within elements. While ElementRegistry tracks entire element areas,
 * StyledAreaRegistry tracks individual styled regions within those elements.
 * <p>
 * This is a CSS-unaware interface that stores areas with their associated tags
 * and styles. The toolkit layer (e.g., TFX) interprets tags as CSS class names
 * and context keys as element IDs to enable CSS selector support.
 *
 * @see StyledAreaInfo
 * @see Tags
 */
public interface StyledAreaRegistry {

    /**
     * Registers a styled area.
     * <p>
     * If the style has a Tags extension, the area will be queryable by tag name.
     * The contextKey associates this area with a parent context (typically an
     * element ID) for hierarchical selector matching.
     *
     * @param style
     *            the style containing Tags and other properties
     * @param area
     *            the rectangular area
     * @param contextKey
     *            the parent context key (may be null)
     */
    void register(Style style, Rect area, String contextKey);

    /**
     * Returns all registered styled areas.
     *
     * @return a list of all areas (never null, may be empty)
     */
    List<StyledAreaInfo> all();

    /**
     * Returns the number of registered areas.
     *
     * @return the count
     */
    int size();

    /**
     * Clears all registered areas.
     * <p>
     * This should be called at the start of each render cycle.
     */
    void clear();

    /**
     * Creates a new default StyledAreaRegistry implementation.
     *
     * @return a new registry instance
     */
    static StyledAreaRegistry create() {
        return new DefaultStyledAreaRegistry();
    }

    /**
     * Returns a no-op registry that ignores all registrations.
     * <p>
     * This is useful when styled area tracking is not needed.
     *
     * @return the no-op registry instance
     */
    static StyledAreaRegistry noop() {
        return NoopStyledAreaRegistry.INSTANCE;
    }
}

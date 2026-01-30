/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import dev.tamboui.layout.Rect;

/**
 * Information about a styled area that has been rendered.
 * <p>
 * This is a plain data class that stores information about a styled region
 * without any CSS-specific concepts. The toolkit layer interprets the
 * {@link #contextKey()} as an element ID and the {@link #tags()} as CSS
 * classes.
 * <p>
 * StyledAreaInfo is created automatically when styled content with Tags is
 * rendered to a Buffer.
 *
 * @see StyledAreaRegistry
 * @see Tags
 */
public final class StyledAreaInfo {

    private final Tags tags;
    private final Rect area;
    private final String contextKey;

    /**
     * Creates a new styled area info.
     *
     * @param tags
     *            the tags associated with this area (never null)
     * @param area
     *            the rectangular area (never null)
     * @param contextKey
     *            the context key (may be null); toolkit uses element ID
     */
    public StyledAreaInfo(Tags tags, Rect area, String contextKey) {
        if (tags == null) {
            throw new IllegalArgumentException("tags cannot be null");
        }
        if (area == null) {
            throw new IllegalArgumentException("area cannot be null");
        }
        this.tags = tags;
        this.area = area;
        this.contextKey = contextKey;
    }

    /**
     * Returns the tags associated with this styled area.
     * <p>
     * Tags are string names that can be used to identify or categorize styled
     * content. The toolkit layer interprets these as CSS class names.
     *
     * @return the tags (never null, may be empty)
     */
    public Tags tags() {
        return tags;
    }

    /**
     * Returns the rectangular area where this styled content was rendered.
     *
     * @return the area (never null)
     */
    public Rect area() {
        return area;
    }

    /**
     * Returns the context key for this styled area.
     * <p>
     * The context key provides a way to associate styled areas with a parent
     * context. The toolkit layer uses the containing element's ID as the context
     * key, enabling selectors like {@code #myPanel .highlight}.
     *
     * @return the context key, or null if not set
     */
    public String contextKey() {
        return contextKey;
    }

    @Override
    public String toString() {
        return String.format("StyledAreaInfo[tags=%s, area=%s, contextKey=%s]", tags, area,
                contextKey);
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

import java.util.Optional;
import java.util.Set;

import dev.tamboui.css.Styleable;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.StyledAreaInfo;

/**
 * A styled span that can be targeted by CSS selectors.
 * <p>
 * This adapter interprets the core layer's CSS-unaware data types in
 * CSS terms:
 * <ul>
 *   <li>{@link dev.tamboui.style.Tags} are exposed as CSS classes</li>
 *   <li>{@code contextKey} is interpreted as an element ID for parent resolution</li>
 *   <li>{@code styleType()} returns "Span" for selector matching</li>
 * </ul>
 * <p>
 * This enables CSS selectors like:
 * <ul>
 *   <li>{@code .highlight} - matches all spans with the "highlight" tag</li>
 *   <li>{@code #myPanel .highlight} - matches "highlight" spans inside #myPanel</li>
 *   <li>{@code Span.error} - matches spans with type "Span" and class "error"</li>
 * </ul>
 *
 * @see StyledAreaInfo
 * @see Styleable
 */
public final class StyledSpan implements Styleable {

    private static final String STYLE_TYPE = "Span";

    private final StyledAreaInfo info;
    private final ElementRegistry elementRegistry;

    /**
     * Creates a new styled span wrapping the given StyledAreaInfo.
     *
     * @param info            the styled area info to wrap
     * @param elementRegistry the element registry for resolving context keys to elements
     */
    public StyledSpan(StyledAreaInfo info, ElementRegistry elementRegistry) {
        if (info == null) {
            throw new IllegalArgumentException("info cannot be null");
        }
        this.info = info;
        this.elementRegistry = elementRegistry;
    }

    /**
     * Returns the area of this styled span.
     *
     * @return the rectangular area
     */
    public Rect area() {
        return info.area();
    }

    @Override
    public String styleType() {
        return STYLE_TYPE;
    }

    @Override
    public Optional<String> cssId() {
        // Styled areas don't have IDs (only elements do)
        return Optional.empty();
    }

    @Override
    public Set<String> cssClasses() {
        return info.tags().values();
    }

    @Override
    public Optional<Styleable> cssParent() {
        String contextKey = info.contextKey();
        if (contextKey == null || elementRegistry == null) {
            return Optional.empty();
        }

        // Interpret contextKey as element ID, resolve to ElementInfo (which is Styleable)
        return elementRegistry.query("#" + contextKey)
                .map(ei -> (Styleable) ei);
    }

    @Override
    public String toString() {
        return String.format("StyledSpan[tags=%s, area=%s, parent=%s]",
                cssClasses(), area(), info.contextKey());
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.css.selector.Selector;
import dev.tamboui.css.selector.SelectorParser;
import dev.tamboui.layout.Rect;

import dev.tamboui.tui.RenderThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registry that maps elements to their rendered areas with full CSS selector support.
 * <p>
 * ElementRegistry is populated during the render pass as elements are rendered.
 * It provides a way to look up element areas using CSS selectors, which is useful for
 * effect systems that need to target specific elements without holding
 * direct references to Element objects.
 * <p>
 * The registry should be cleared at the start of each render cycle and
 * repopulated as elements render themselves.
 *
 * <h2>Supported Selectors</h2>
 * <ul>
 *   <li>{@code *} - Universal selector (matches all elements)</li>
 *   <li>{@code Type} - Type selector (e.g., Panel, Button)</li>
 *   <li>{@code #id} - ID selector</li>
 *   <li>{@code .class} - Class selector</li>
 *   <li>{@code :pseudo} - Pseudo-class selector (focus, hover, disabled, etc.)</li>
 *   <li>{@code [attr]} - Attribute existence selector</li>
 *   <li>{@code [attr=value]} - Attribute equals selector</li>
 *   <li>{@code [attr^=value]} - Attribute starts-with selector</li>
 *   <li>{@code [attr$=value]} - Attribute ends-with selector</li>
 *   <li>{@code [attr*=value]} - Attribute contains selector</li>
 *   <li>{@code A B} - Descendant combinator</li>
 *   <li>{@code A > B} - Child combinator</li>
 *   <li>{@code A.class#id} - Compound selectors</li>
 * </ul>
 *
 * <pre>{@code
 * ElementRegistry registry = new ElementRegistry();
 *
 * // During render, elements register themselves
 * registry.register("header", "Panel", Set.of("main"), headerArea, null);
 * registry.register("btn", "Button", Set.of("primary", "large"), buttonArea, headerInfo);
 *
 * // Query by ID
 * Optional<ElementInfo> header = registry.query("#header");
 *
 * // Query by class with pseudo-class state
 * PseudoClassState state = PseudoClassState.ofFocused();
 * List<ElementInfo> focused = registry.queryAll(".primary:focus", state);
 *
 * // Query with descendant combinator
 * List<ElementInfo> panelButtons = registry.queryAll("Panel Button");
 * }</pre>
 */
public final class ElementRegistry {

    /**
     * Creates a new empty element registry.
     */
    public ElementRegistry() {
    }

    private final Map<String, ElementInfo> elementsById = new HashMap<>();
    private final List<ElementInfo> allElements = new ArrayList<>();

    /**
     * Information about a registered element.
     * <p>
     * Implements {@link Styleable} to be compatible with CSS selector matching.
     */
    public static final class ElementInfo implements Styleable {
        private final String id;
        private final String type;
        private final Set<String> cssClasses;
        private final Map<String, String> attributes;
        private final Rect area;
        private final ElementInfo parent;

        ElementInfo(String id, String type, Set<String> cssClasses,
                    Map<String, String> attributes, Rect area, ElementInfo parent) {
            this.id = id;
            this.type = type;
            this.cssClasses = cssClasses != null ? cssClasses : Collections.emptySet();
            this.attributes = attributes != null ? attributes : Collections.emptyMap();
            this.area = area;
            this.parent = parent;
        }

        /**
         * Returns the element ID, or null if not set.
         *
         * @return the element ID, or null
         */
        public String id() {
            return id;
        }

        /**
         * Returns the element type name.
         *
         * @return the element type name
         */
        public String type() {
            return type;
        }

        /**
         * Returns the element's rendered area.
         *
         * @return the rendered area
         */
        public Rect area() {
            return area;
        }

        /**
         * Returns the parent element info, or null if this is a root element.
         *
         * @return the parent element info, or null
         */
        public ElementInfo parent() {
            return parent;
        }

        // Styleable implementation

        @Override
        public String styleType() {
            return type != null ? type : "";
        }

        @Override
        public Optional<String> cssId() {
            return Optional.ofNullable(id);
        }

        @Override
        public Set<String> cssClasses() {
            return cssClasses;
        }

        @Override
        public Optional<Styleable> cssParent() {
            return Optional.ofNullable(parent);
        }

        @Override
        public Map<String, String> styleAttributes() {
            return attributes;
        }

        /**
         * Builds the ancestor chain for selector matching.
         * <p>
         * Returns the ancestors in order from root to immediate parent.
         *
         * @return the ancestor chain
         */
        public List<Styleable> ancestors() {
            List<Styleable> ancestors = new ArrayList<>();
            ElementInfo current = parent;
            while (current != null) {
                ancestors.add(0, current); // Add at beginning for root-to-parent order
                current = current.parent;
            }
            return ancestors;
        }
    }

    /**
     * Registers an element with full information.
     *
     * @param elementId  the element ID (may be null)
     * @param type       the element type name
     * @param cssClasses the element's CSS classes
     * @param area       the rendered area
     */
    public void register(String elementId, String type, Set<String> cssClasses, Rect area) {
        register(elementId, type, cssClasses, null, area, null);
    }

    /**
     * Registers an element with full information including parent.
     * <p>
     * Must be called from the render thread.
     *
     * @param elementId  the element ID (may be null)
     * @param type       the element type name
     * @param cssClasses the element's CSS classes
     * @param attributes the element's style attributes (may be null)
     * @param area       the rendered area
     * @param parent     the parent element info (may be null for root elements)
     */
    public void register(String elementId, String type, Set<String> cssClasses,
                         Map<String, String> attributes, Rect area, ElementInfo parent) {
        RenderThread.checkRenderThread();
        if (area == null) {
            return;
        }
        ElementInfo info = new ElementInfo(elementId, type, cssClasses, attributes, area, parent);
        allElements.add(info);
        if (elementId != null) {
            elementsById.put(elementId, info);
        }
    }

    /**
     * Registers an element's rendered area by ID only.
     * <p>
     * This is a convenience method for simple ID-based registration.
     *
     * @param elementId the element ID
     * @param area      the rendered area
     */
    public void register(String elementId, Rect area) {
        register(elementId, null, null, null, area, null);
    }

    /**
     * Queries for a single element matching the selector.
     * <p>
     * Uses {@link PseudoClassState#NONE} for pseudo-class matching.
     *
     * @param selector the CSS selector string
     * @return the matching element info, or empty if not found
     */
    public Optional<ElementInfo> query(String selector) {
        return query(selector, PseudoClassState.NONE);
    }

    /**
     * Queries for a single element matching the selector with pseudo-class state.
     *
     * @param selector the CSS selector string
     * @param state    the pseudo-class state for matching
     * @return the matching element info, or empty if not found
     */
    public Optional<ElementInfo> query(String selector, PseudoClassState state) {
        if (selector == null || selector.isEmpty()) {
            return Optional.empty();
        }

        try {
            Selector parsed = SelectorParser.parse(selector);

            // Fast path for ID-only selector
            if (selector.startsWith("#") && !selector.contains(" ") && !selector.contains(">")
                    && !selector.contains(".") && !selector.contains(":") && !selector.contains("[")) {
                String id = selector.substring(1);
                ElementInfo info = elementsById.get(id);
                if (info != null && parsed.matches(info, state, info.ancestors())) {
                    return Optional.of(info);
                }
                return Optional.empty();
            }

            // Search all elements
            for (ElementInfo info : allElements) {
                if (parsed.matches(info, state, info.ancestors())) {
                    return Optional.of(info);
                }
            }
        } catch (IllegalArgumentException e) {
            // Invalid selector, return empty
        }
        return Optional.empty();
    }

    /**
     * Queries for all elements matching the selector.
     * <p>
     * Uses {@link PseudoClassState#NONE} for pseudo-class matching.
     *
     * @param selector the CSS selector string
     * @return list of matching element info (may be empty)
     */
    public List<ElementInfo> queryAll(String selector) {
        return queryAll(selector, PseudoClassState.NONE);
    }

    /**
     * Queries for all elements matching the selector with pseudo-class state.
     *
     * @param selector the CSS selector string
     * @param state    the pseudo-class state for matching
     * @return list of matching element info (may be empty)
     */
    public List<ElementInfo> queryAll(String selector, PseudoClassState state) {
        if (selector == null || selector.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Selector parsed = SelectorParser.parse(selector);
            List<ElementInfo> results = new ArrayList<>();

            for (ElementInfo info : allElements) {
                if (parsed.matches(info, state, info.ancestors())) {
                    results.add(info);
                }
            }
            return results;
        } catch (IllegalArgumentException e) {
            // Invalid selector, return empty
            return Collections.emptyList();
        }
    }

    /**
     * Returns the rendered area for an element by ID.
     * <p>
     * This is a convenience method equivalent to {@code query("#" + elementId)}.
     *
     * @param elementId the element ID
     * @return the element's area, or null if not registered
     */
    public Rect getArea(String elementId) {
        if (elementId == null) {
            return null;
        }
        ElementInfo info = elementsById.get(elementId);
        return info != null ? info.area : null;
    }

    /**
     * Returns whether an element with the given ID is registered.
     *
     * @param elementId the element ID
     * @return true if registered
     */
    public boolean contains(String elementId) {
        if (elementId == null) {
            return false;
        }
        return elementsById.containsKey(elementId);
    }

    /**
     * Clears all registered elements.
     * <p>
     * Should be called at the start of each render cycle.
     * Must be called from the render thread.
     */
    public void clear() {
        RenderThread.checkRenderThread();
        elementsById.clear();
        allElements.clear();
    }

    /**
     * Returns the number of registered elements.
     *
     * @return the count
     */
    public int size() {
        return allElements.size();
    }
}

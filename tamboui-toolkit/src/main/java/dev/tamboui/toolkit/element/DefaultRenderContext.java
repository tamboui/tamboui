/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.event.EventRouter;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.toolkit.elements.ErrorPlaceholder;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.layout.Rect;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Default implementation of RenderContext with internal framework methods.
 * <p>
 * <strong>INTERNAL USE ONLY</strong> - This class is used internally by the toolkit framework.
 * User code and element implementations should only interact with the {@link RenderContext} interface.
 * <p>
 * <strong>CODE SMELL WARNING:</strong> If you find yourself casting {@code RenderContext} to
 * {@code DefaultRenderContext} in your code, this is a design problem. The only legitimate
 * user of this class's internal methods is {@link StyledElement#render}.
 * <p>
 * Methods like {@link #withElement}, {@link #registerElement}, and stack manipulation
 * are framework internals that should never be called directly from element implementations.
 */
public final class DefaultRenderContext implements RenderContext {

    private static final Logger LOGGER = Logger.getLogger(DefaultRenderContext.class.getName());

    private final FocusManager focusManager;
    private final EventRouter eventRouter;
    private final ElementRegistry elementRegistry;
    private final Deque<Style> styleStack = new ArrayDeque<>();
    private final Deque<Styleable> elementStack = new ArrayDeque<>();
    private final Deque<CssStyleResolver> resolverStack = new ArrayDeque<>();
    private StyleEngine styleEngine;
    private Bindings bindings = BindingSets.defaults();
    private boolean faultTolerant;

    /**
     * Creates a new render context.
     * <p>
     * The ElementRegistry is obtained from the EventRouter.
     *
     * @param focusManager the focus manager
     * @param eventRouter  the event router (must have an ElementRegistry)
     */
    public DefaultRenderContext(FocusManager focusManager, EventRouter eventRouter) {
        this.focusManager = focusManager;
        this.eventRouter = eventRouter;
        this.elementRegistry = eventRouter.elementRegistry();
    }

    /**
     * Creates an empty context for simple rendering without focus management.
     *
     * @return a new empty render context
     */
    public static DefaultRenderContext createEmpty() {
        FocusManager fm = new FocusManager();
        ElementRegistry registry = new ElementRegistry();
        return new DefaultRenderContext(fm, new EventRouter(fm, registry));
    }

    /**
     * Sets the style engine for CSS resolution.
     *
     * @param styleEngine the style engine, or null to disable CSS
     */
    public void setStyleEngine(StyleEngine styleEngine) {
        this.styleEngine = styleEngine;
    }

    /**
     * Returns the style engine, if configured.
     *
     * @return the style engine, or empty if not configured
     */
    public Optional<StyleEngine> styleEngine() {
        return Optional.ofNullable(styleEngine);
    }

    /**
     * Sets the bindings used for action matching.
     *
     * @param bindings the bindings to use
     */
    public void setBindings(Bindings bindings) {
        this.bindings = bindings != null ? bindings : BindingSets.defaults();
    }

    /**
     * Returns the current bindings.
     *
     * @return the bindings
     */
    public Bindings bindings() {
        return bindings;
    }

    /**
     * Enables or disables fault-tolerant rendering.
     * <p>
     * When enabled, exceptions thrown during child rendering are caught
     * and an error placeholder is displayed instead.
     *
     * @param faultTolerant true to enable fault-tolerant rendering
     */
    public void setFaultTolerant(boolean faultTolerant) {
        this.faultTolerant = faultTolerant;
    }

    /**
     * Returns whether fault-tolerant rendering is enabled.
     *
     * @return true if fault-tolerant rendering is enabled
     */
    public boolean isFaultTolerant() {
        return faultTolerant;
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

    @Override
    public Optional<CssStyleResolver> resolveStyle(Styleable element) {
        if (styleEngine == null) {
            return Optional.empty();
        }

        // Build pseudo-class state based on focus
        PseudoClassState state = PseudoClassState.NONE;
        if (element instanceof Element) {
            Element e = (Element) element;
            if (e.id() != null && isFocused(e.id())) {
                state = PseudoClassState.ofFocused();
            }
        }

        // Build ancestor chain
        List<Styleable> ancestors = buildAncestorChain(element);

        CssStyleResolver resolved = styleEngine.resolve(element, state, ancestors);

        // If we have a parent resolver on the stack, create a merged resolver
        // that inherits properties from the parent (e.g., border-type from Component to Panel)
        CssStyleResolver parentResolver = resolverStack.isEmpty() ? null : resolverStack.peek();
        if (parentResolver != null) {
            resolved = resolved.withFallback(parentResolver);
        }

        return resolved.hasProperties() ? Optional.of(resolved) : Optional.empty();
    }

    @Override
    public Optional<CssStyleResolver> resolveStyle(String styleType, String... cssClasses) {
        if (styleEngine == null) {
            return Optional.empty();
        }

        Set<String> classes = cssClasses.length > 0
                ? new HashSet<>(Arrays.asList(cssClasses))
                : Collections.emptySet();
        Styleable virtual = new VirtualStyleable(styleType, classes);
        CssStyleResolver resolved = styleEngine.resolve(virtual, PseudoClassState.NONE, Collections.emptyList());
        return resolved.hasProperties() ? Optional.of(resolved) : Optional.empty();
    }

    /**
     * A simple Styleable for resolving CSS styles by type and classes.
     */
    private static final class VirtualStyleable implements Styleable {
        private final String type;
        private final Set<String> classes;

        VirtualStyleable(String type, Set<String> classes) {
            this.type = type;
            this.classes = classes;
        }

        @Override
        public String styleType() {
            return type;
        }

        @Override
        public Optional<String> cssId() {
            return Optional.empty();
        }

        @Override
        public Set<String> cssClasses() {
            return classes;
        }

        @Override
        public Optional<Styleable> cssParent() {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Color> parseColor(String colorValue) {
        if (styleEngine == null || colorValue == null) {
            return Optional.empty();
        }
        return styleEngine.parseColor(colorValue);
    }

    @Override
    public void renderChild(Element child, Frame frame, Rect area) {
        String childId = child.id();

        if (faultTolerant) {
            try {
                // Push the child's context key only for the child's render scope.
                // If the child fails and we fall back to an ErrorPlaceholder, we must not
                // keep the child's context key active, otherwise placeholder output would
                // be incorrectly attributed to the child.
                if (childId != null) {
                    frame.pushContextKey(childId);
                }
                try {
                    child.render(frame, area, this);
                    registerElement(child, area);
                    return;
                } finally {
                    if (childId != null) {
                        frame.popContextKey();
                    }
                }
            } catch (Throwable t) {
                // Render error placeholder instead of the failed child (no child context key active)
                ErrorPlaceholder placeholder = ErrorPlaceholder.from(t, childId);
                try {
                    placeholder.render(frame, area, this);
                } catch (Throwable ignored) {
                    // Even the placeholder failed - nothing more we can do
                }
                return;
            }
        }

        // Non-fault-tolerant rendering: always attribute output to the child.
        if (childId != null) {
            frame.pushContextKey(childId);
        }
        try {
                child.render(frame, area, this);
                registerElement(child, area);
        } finally {
            if (childId != null) {
                frame.popContextKey();
            }
        }
    }

    @Override
    public Style currentStyle() {
        return styleStack.isEmpty() ? Style.EMPTY : styleStack.peek();
    }

    @Override
    public Style childStyle(String childName, PseudoClassState state) {
        if (styleEngine == null || elementStack.isEmpty()) {
            return currentStyle();
        }

        Styleable parent = elementStack.peek();
        String childType = parent.styleType() + "-" + childName;
        Styleable virtual = new VirtualChild(childType, parent);

        // Build ancestor chain: parent's ancestors + parent
        List<Styleable> ancestors = buildAncestorChain(parent);
        ancestors.add(parent);

        CssStyleResolver resolved = styleEngine.resolve(virtual, state, ancestors);
        return resolved.hasProperties()
            ? currentStyle().patch(resolved.toStyle())
            : currentStyle();
    }

    @Override
    public Style childStyle(String childName, ChildPosition position, PseudoClassState state) {
        // Merge position-derived pseudo-classes with the provided state
        PseudoClassState mergedState = state
            .withFirstChild(position.isFirst())
            .withLastChild(position.isLast())
            .withNthChild(position.nthChild());

        return childStyle(childName, mergedState);
    }

    /**
     * A virtual Styleable representing a child element.
     */
    private static final class VirtualChild implements Styleable {
        private final String type;
        private final Styleable parent;

        VirtualChild(String type, Styleable parent) {
            this.type = type;
            this.parent = parent;
        }

        @Override
        public String styleType() {
            return type;
        }

        @Override
        public Optional<String> cssId() {
            return Optional.empty();
        }

        @Override
        public Set<String> cssClasses() {
            return Collections.emptySet();
        }

        @Override
        public Optional<Styleable> cssParent() {
            return Optional.of(parent);
        }
    }

    private List<Styleable> buildAncestorChain(Styleable element) {
        List<Styleable> ancestors = new ArrayList<>();

        // First, try explicit cssParent chain (takes precedence)
        Optional<Styleable> parent = element.cssParent();
        while (parent.isPresent()) {
            ancestors.add(0, parent.get());
            parent = parent.get().cssParent();
        }

        // If no explicit parent, use the element stack (runtime render hierarchy)
        // This enables descendant selectors for dynamically created elements
        if (ancestors.isEmpty() && !elementStack.isEmpty()) {
            ancestors.addAll(elementStack);
        }

        return ancestors;
    }

    // ═══════════════════════════════════════════════════════════════
    // Internal API (for framework use only)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns the focus manager.
     * <p>
     * Internal use only.
     *
     * @return the focus manager
     */
    public FocusManager focusManager() {
        return focusManager;
    }

    /**
     * Returns the event router.
     * <p>
     * Internal use only.
     *
     * @return the event router
     */
    public EventRouter eventRouter() {
        return eventRouter;
    }

    /**
     * Registers an element for event routing and focus management.
     * Called by container elements after rendering children.
     * <p>
     * The EventRouter handles registration in the ElementRegistry for ID-based lookups.
     * <p>
     * Internal use only.
     *
     * @param element the element to register
     * @param area the rendered area
     */
    public void registerElement(Element element, Rect area) {
        // EventRouter handles both event routing and ElementRegistry population
        eventRouter.registerElement(element, area);

        if (element.isFocusable()) {
            String id = element.id();
            if (id != null) {
                focusManager.registerFocusable(id, area);
            } else {
                // This should only happen if a subclass overrides isFocusable() without ensuring an ID
                LOGGER.warning("Focusable element of type " + element.getClass().getSimpleName()
                        + " has no ID and will not be registered in the focus chain. "
                        + "Ensure the element has an ID set.");
            }
        }
    }

    /**
     * Returns the element registry for ID-based area lookups.
     * <p>
     * The registry is populated during rendering and can be used
     * by effect systems to target elements by ID.
     *
     * @return the element registry
     */
    public ElementRegistry elementRegistry() {
        return elementRegistry;
    }

    /**
     * Executes an action with an element and style pushed onto their stacks.
     * <p>
     * The style is merged with the current style. Both the element and merged style
     * are available via {@link #currentElement()} and {@link #currentStyle()}.
     * This enables {@link #childStyle(String)} to work without passing the parent.
     * <p>
     * Internal use only - called by StyledElement.render().
     *
     * @param element the element being rendered
     * @param style the element's resolved style
     * @param action the action to execute
     */
    public void withElement(Styleable element, Style style, Runnable action) {
        withElement(element, style, null, action);
    }

    /**
     * Executes an action with an element, style, and CSS resolver pushed onto their stacks.
     * <p>
     * The style is merged with the current style. The element, merged style, and resolver
     * are available via {@link #currentElement()}, {@link #currentStyle()}, and
     * {@link #currentResolver()}.
     * <p>
     * Internal use only - called by StyledElement.render().
     *
     * @param element the element being rendered
     * @param style the element's resolved style
     * @param resolver the element's CSS resolver (may be null)
     * @param action the action to execute
     */
    public void withElement(Styleable element, Style style, CssStyleResolver resolver, Runnable action) {
        Style merged = currentStyle().patch(style);
        styleStack.push(merged);
        elementStack.push(element);
        if (resolver != null) {
            resolverStack.push(resolver);
        }
        try {
            action.run();
        } finally {
            elementStack.pop();
            styleStack.pop();
            if (resolver != null) {
                resolverStack.pop();
            }
        }
    }

    /**
     * Returns the current element being rendered, if any.
     *
     * @return the current element, or empty if no element is being rendered
     */
    public Optional<Styleable> currentElement() {
        return elementStack.isEmpty() ? Optional.empty() : Optional.of(elementStack.peek());
    }

    /**
     * Returns the current CSS resolver from the resolver stack, if any.
     * <p>
     * This allows child elements to access CSS properties from their parent elements
     * that are not part of the Style cascade (e.g., border-type).
     *
     * @return the current CSS resolver, or empty if none is active
     */
    public Optional<CssStyleResolver> currentResolver() {
        return resolverStack.isEmpty() ? Optional.empty() : Optional.of(resolverStack.peek());
    }
}

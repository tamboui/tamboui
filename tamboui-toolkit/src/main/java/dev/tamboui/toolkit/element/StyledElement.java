/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.css.cascade.PseudoClassState;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.toolkit.event.DragHandler;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.event.KeyEventHandler;
import dev.tamboui.toolkit.event.MouseEventHandler;
import dev.tamboui.toolkit.id.IdGenerator;
import dev.tamboui.tui.bindings.ActionHandler;
import dev.tamboui.tui.error.TuiException;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Abstract base for elements that support styling and event handling.
 * Provides a fluent API for setting colors, modifiers, and event handlers.
 * <p>
 * Implements {@link Styleable} to support CSS-based styling.
 *
 * @param <T> the concrete element type for method chaining
 */
public abstract class StyledElement<T extends StyledElement<T>> implements Element, Styleable {

    /** Creates a new styled element with default values. */
    protected StyledElement() {
    }

    /** The inline style applied to this element. */
    protected Style style = Style.EMPTY;
    /** The layout constraint for this element. */
    protected Constraint layoutConstraint;
    /** The unique identifier for this element. */
    protected String elementId;
    /** The CSS classes applied to this element. */
    protected Set<String> cssClasses = new LinkedHashSet<>();
    /** The style attributes for CSS attribute selector matching. */
    protected Map<String, String> styleAttrs = new LinkedHashMap<>();
    /** The CSS parent for ancestor matching. */
    protected Styleable cssParent;
    /** The key event handler for this element. */
    protected KeyEventHandler keyHandler;
    /** The mouse event handler for this element. */
    protected MouseEventHandler mouseHandler;
    /** The drag handler for this element. */
    protected DragHandler dragHandler;
    /** Whether this element is draggable. */
    protected boolean draggable;
    /** Whether this element is focusable. */
    protected boolean focusable;
    /** The last area this element was rendered in. */
    protected Rect lastRenderedArea;

    /**
     * Returns this element cast to the concrete type for method chaining.
     *
     * @return this element
     */
    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    // Foreground colors

    /**
     * Sets the foreground color.
     *
     * @param color the foreground color
     * @return this element for chaining
     */
    public T fg(Color color) {
        this.style = style.fg(color);
        return self();
    }

    /**
     * Sets the foreground color to black.
     *
     * @return this element for chaining
     */
    public T black() {
        return fg(Color.BLACK);
    }

    /**
     * Sets the foreground color to red.
     *
     * @return this element for chaining
     */
    public T red() {
        return fg(Color.RED);
    }

    /**
     * Sets the foreground color to green.
     *
     * @return this element for chaining
     */
    public T green() {
        return fg(Color.GREEN);
    }

    /**
     * Sets the foreground color to yellow.
     *
     * @return this element for chaining
     */
    public T yellow() {
        return fg(Color.YELLOW);
    }

    /**
     * Sets the foreground color to blue.
     *
     * @return this element for chaining
     */
    public T blue() {
        return fg(Color.BLUE);
    }

    /**
     * Sets the foreground color to magenta.
     *
     * @return this element for chaining
     */
    public T magenta() {
        return fg(Color.MAGENTA);
    }

    /**
     * Sets the foreground color to cyan.
     *
     * @return this element for chaining
     */
    public T cyan() {
        return fg(Color.CYAN);
    }

    /**
     * Sets the foreground color to white.
     *
     * @return this element for chaining
     */
    public T white() {
        return fg(Color.WHITE);
    }

    /**
     * Sets the foreground color to gray.
     *
     * @return this element for chaining
     */
    public T gray() {
        return fg(Color.GRAY);
    }

    // Background colors

    /**
     * Sets the background color.
     *
     * @param color the background color
     * @return this element for chaining
     */
    public T bg(Color color) {
        this.style = style.bg(color);
        return self();
    }

    /**
     * Sets the background color to black.
     *
     * @return this element for chaining
     */
    public T onBlack() {
        return bg(Color.BLACK);
    }

    /**
     * Sets the background color to red.
     *
     * @return this element for chaining
     */
    public T onRed() {
        return bg(Color.RED);
    }

    /**
     * Sets the background color to green.
     *
     * @return this element for chaining
     */
    public T onGreen() {
        return bg(Color.GREEN);
    }

    /**
     * Sets the background color to yellow.
     *
     * @return this element for chaining
     */
    public T onYellow() {
        return bg(Color.YELLOW);
    }

    /**
     * Sets the background color to blue.
     *
     * @return this element for chaining
     */
    public T onBlue() {
        return bg(Color.BLUE);
    }

    /**
     * Sets the background color to magenta.
     *
     * @return this element for chaining
     */
    public T onMagenta() {
        return bg(Color.MAGENTA);
    }

    /**
     * Sets the background color to cyan.
     *
     * @return this element for chaining
     */
    public T onCyan() {
        return bg(Color.CYAN);
    }

    /**
     * Sets the background color to white.
     *
     * @return this element for chaining
     */
    public T onWhite() {
        return bg(Color.WHITE);
    }

    // Modifiers

    /**
     * Applies the bold modifier.
     *
     * @return this element for chaining
     */
    public T bold() {
        this.style = style.bold();
        return self();
    }

    /**
     * Applies the dim modifier.
     *
     * @return this element for chaining
     */
    public T dim() {
        this.style = style.dim();
        return self();
    }

    /**
     * Applies the italic modifier.
     *
     * @return this element for chaining
     */
    public T italic() {
        this.style = style.italic();
        return self();
    }

    /**
     * Applies the underlined modifier.
     *
     * @return this element for chaining
     */
    public T underlined() {
        this.style = style.underlined();
        return self();
    }

    /**
     * Applies the reversed modifier.
     *
     * @return this element for chaining
     */
    public T reversed() {
        this.style = style.reversed();
        return self();
    }

    /**
     * Applies the crossed-out modifier.
     *
     * @return this element for chaining
     */
    public T crossedOut() {
        this.style = style.crossedOut();
        return self();
    }

    // Style

    /**
     * Sets the style for this element.
     *
     * @param style the style to apply
     * @return this element for chaining
     */
    public T style(Style style) {
        this.style = style;
        return self();
    }

    /**
     * Returns the current style.
     *
     * @return the style
     */
    public Style getStyle() {
        return style;
    }

    // ═══════════════════════════════════════════════════════════════
    // Render template method
    // ═══════════════════════════════════════════════════════════════

    /**
     * Renders this element. This is a template method that:
     * <ol>
     *   <li>Resolves the effective CSS + inline style</li>
     *   <li>Pushes the style and CSS resolver onto the context's stacks</li>
     *   <li>Calls {@link #renderContent} for subclass-specific rendering</li>
     *   <li>Pops the style and resolver when done</li>
     * </ol>
     * <p>
     * Subclasses must implement {@link #renderContent} instead of overriding this method.
     * The current style is available via {@link RenderContext#currentStyle()}.
     * CSS properties not in Style (e.g., border-type) can be accessed from parent
     * elements via {@link DefaultRenderContext#currentResolver()}.
     *
     * @param frame the frame to render to
     * @param area the area to render in
     * @param context the render context
     */
    @Override
    public final void render(Frame frame, Rect area, RenderContext context) {
        if (area.isEmpty()) {
            return;
        }
        this.lastRenderedArea = area;

        // Resolve CSS for this element
        CssStyleResolver cssResolver = context.resolveStyle(this).orElse(null);
        Style effectiveStyle;
        if (cssResolver != null) {
            Style cssStyle = cssResolver.toStyle();
            // Named colors are soft defaults — strip them when CSS provides a color
            Style overlay = style;
            if (overlay.fg().orElse(null) instanceof Color.Named && cssStyle.fg().isPresent()) {
                overlay = overlay.fg(null);
            }
            if (overlay.bg().orElse(null) instanceof Color.Named && cssStyle.bg().isPresent()) {
                overlay = overlay.bg(null);
            }
            effectiveStyle = cssStyle.patch(overlay);
        } else {
            effectiveStyle = style;
        }

        if (context instanceof DefaultRenderContext) {
            DefaultRenderContext ctx = (DefaultRenderContext) context;
            // Auto-generate ID for focusable elements that don't have one
            // Must happen BEFORE renderContent so focus checks work
            if (isFocusable() && elementId == null) {
                elementId = IdGenerator.newId(this);
            }
            ctx.withElement(this, effectiveStyle, cssResolver, () -> renderContent(frame, area, context));
            ctx.registerElement(this, area);
        } else {
            // Fallback for non-default contexts (e.g., testing)
            renderContent(frame, area, context);
        }
    }

    /**
     * Renders the content of this element.
     * <p>
     * Subclasses implement this method to perform their specific rendering.
     * The current style (CSS + inline, merged with parent styles) is available
     * via {@link RenderContext#currentStyle()}.
     *
     * @param frame the frame to render to
     * @param area the area to render in
     * @param context the render context
     */
    protected abstract void renderContent(Frame frame, Rect area, RenderContext context);

    /**
     * Resolves a style for a sub-component following the priority: explicit > CSS > default.
     * <p>
     * This helper method standardizes how element sub-components (like cursors, placeholders,
     * scrollbar thumbs, etc.) resolve their styles. It enables CSS theming while preserving
     * programmatic control when needed.
     * <p>
     * The resolution order is:
     * <ol>
     *   <li>If {@code explicitStyle} is non-null, use it (programmatic override)</li>
     *   <li>Otherwise, check CSS via {@link RenderContext#childStyle(String)}</li>
     *   <li>If CSS provides styles, use those</li>
     *   <li>Otherwise, use {@code defaultStyle}</li>
     * </ol>
     * <p>
     * Example usage in a TextInput element:
     * <pre>{@code
     * // In renderContent():
     * Style effectiveCursorStyle = resolveEffectiveStyle(context, "cursor", cursorStyle, DEFAULT_CURSOR_STYLE);
     * Style effectivePlaceholderStyle = resolveEffectiveStyle(context, "placeholder", placeholderStyle, DEFAULT_PLACEHOLDER_STYLE);
     * }</pre>
     * <p>
     * This enables CSS like:
     * <pre>{@code
     * TextInputElement-cursor { text-style: reversed; }
     * TextInputElement-placeholder { color: gray; }
     * }</pre>
     *
     * @param context the render context for CSS resolution
     * @param childName the child name (e.g., "cursor", "placeholder", "thumb")
     * @param explicitStyle the explicitly set style (may be null for CSS/default)
     * @param defaultStyle the default style to use when no explicit or CSS style is set
     * @return the resolved style
     */
    protected Style resolveEffectiveStyle(RenderContext context,
                                          String childName,
                                          Style explicitStyle,
                                          Style defaultStyle) {
        return resolveEffectiveStyle(context, childName, null, explicitStyle, defaultStyle);
    }

    /**
     * Resolves a style for a sub-component with pseudo-class state, following the priority: explicit > CSS > default.
     * <p>
     * Use this overload for stateful children like selected items or focused tabs.
     * <p>
     * Example usage:
     * <pre>{@code
     * Style effectiveHighlightStyle = resolveEffectiveStyle(
     *     context, "item", PseudoClassState.ofSelected(),
     *     highlightStyle, DEFAULT_HIGHLIGHT_STYLE);
     * }</pre>
     *
     * @param context the render context for CSS resolution
     * @param childName the child name (e.g., "item", "row", "tab")
     * @param state the pseudo-class state (e.g., selected, hover), or null for none
     * @param explicitStyle the explicitly set style (may be null for CSS/default)
     * @param defaultStyle the default style to use when no explicit or CSS style is set
     * @return the resolved style
     */
    protected Style resolveEffectiveStyle(RenderContext context,
                                          String childName,
                                          PseudoClassState state,
                                          Style explicitStyle,
                                          Style defaultStyle) {
        // Priority 1: Explicit programmatic style
        if (explicitStyle != null) {
            return explicitStyle;
        }

        // Priority 2: CSS child style
        Style cssStyle = state != null
            ? context.childStyle(childName, state)
            : context.childStyle(childName);
        if (!cssStyle.equals(context.currentStyle())) {
            // CSS provided specific styling for this child
            return cssStyle;
        }

        // Priority 3: Default style
        return defaultStyle;
    }

    /**
     * Gets the StylePropertyResolver for CSS property resolution.
     * <p>
     * This is useful for passing to widgets like Block that need to resolve
     * CSS properties beyond basic styling (e.g., border-type, border-color).
     *
     * @param context the render context
     * @return the StylePropertyResolver, or an empty resolver if no CSS is available
     */
    protected StylePropertyResolver styleResolver(RenderContext context) {
        return context.resolveStyle(this)
                .map(r -> (StylePropertyResolver) r)
                .orElse(StylePropertyResolver.empty());
    }

    // Layout constraint

    /**
     * Sets the layout constraint for this element.
     *
     * @param constraint the constraint
     * @return this element for chaining
     */
    public T constraint(Constraint constraint) {
        this.layoutConstraint = constraint;
        return self();
    }

    /**
     * Sets the constraint to an exact length.
     *
     * @param length the exact length
     * @return this element for chaining
     */
    public T length(int length) {
        return constraint(Constraint.length(length));
    }

    /**
     * Sets the constraint to a percentage of the parent's size.
     *
     * @param percent the percentage value
     * @return this element for chaining
     */
    public T percent(int percent) {
        return constraint(Constraint.percentage(percent));
    }

    /**
     * Sets the constraint to fill available space.
     *
     * @return this element for chaining
     */
    public T fill() {
        return constraint(Constraint.fill());
    }

    /**
     * Sets the constraint to fill available space with the given weight.
     *
     * @param weight the fill weight
     * @return this element for chaining
     */
    public T fill(int weight) {
        return constraint(Constraint.fill(weight));
    }

    /**
     * Sets a minimum size constraint.
     *
     * @param min the minimum size
     * @return this element for chaining
     */
    public T min(int min) {
        return constraint(Constraint.min(min));
    }

    /**
     * Sets a maximum size constraint.
     *
     * @param max the maximum size
     * @return this element for chaining
     */
    public T max(int max) {
        return constraint(Constraint.max(max));
    }

    /**
     * Sets the constraint to fit the element's preferred size.
     * <p>
     * When used in a row, the element will take only as much width as needed
     * (queried via {@link #preferredWidth()}). This is useful for inline text
     * where you want each text element to use its natural width.
     *
     * @return this element for chaining
     */
    public T fit() {
        return constraint(Constraint.fit());
    }

    @Override
    public Constraint constraint() {
        return layoutConstraint;
    }

    // Focusable

    /**
     * Makes this element focusable, allowing it to participate in TAB navigation.
     * <p>
     * When an element is focusable:
     * <ul>
     *   <li>It is included in the focus chain for TAB/Shift+TAB navigation</li>
     *   <li>It receives keyboard events via {@link #handleKeyEvent(KeyEvent, boolean)} when focused</li>
     *   <li>An ID is required for focus management - if not set via {@link #id(String)},
     *       one will be auto-generated at render time</li>
     * </ul>
     * <p>
     * Note: {@code focusable()} and {@link #onKeyEvent(KeyEventHandler)} are orthogonal:
     * <ul>
     *   <li>Use {@code focusable()} when the element should participate in TAB navigation</li>
     *   <li>Use {@code onKeyEvent()} to handle keyboard events (works regardless of focusability)</li>
     *   <li>Use both when you want TAB navigation AND custom key handling</li>
     * </ul>
     *
     * @return this element for chaining
     */
    public T focusable() {
        this.focusable = true;
        return self();
    }

    /**
     * Sets whether this element is focusable for TAB navigation.
     *
     * @param focusable true to make focusable, false otherwise
     * @return this element for chaining
     * @see #focusable() for more details on focusability
     */
    public T focusable(boolean focusable) {
        this.focusable = focusable;
        return self();
    }

    @Override
    public boolean isFocusable() {
        return focusable;
    }

    // ID for focus management

    /**
     * Sets the element's ID for focus management and CSS targeting.
     * <p>
     * The ID is immutable once set and cannot be changed. If an ID is needed
     * but not explicitly provided, calling {@link #focusable()} will auto-generate one.
     *
     * @param id the unique identifier for this element
     * @return this element for chaining
     * @throws IllegalStateException if the ID has already been set
     */
    public T id(String id) {
        if (this.elementId != null) {
            throw new TuiException("Element ID cannot be changed once set. Current ID: " + this.elementId);
        }
        this.elementId = id;
        return self();
    }

    @Override
    public String id() {
        return elementId;
    }

    // CSS classes

    /**
     * Adds one or more CSS classes to this element.
     *
     * @param classes the class names to add
     * @return this element for chaining
     */
    public T addClass(String... classes) {
        for (String c : classes) {
            if (c != null && !c.isEmpty()) {
                this.cssClasses.add(c);
            }
        }
        return self();
    }

    /**
     * Removes a CSS class from this element.
     *
     * @param className the class name to remove
     * @return this element for chaining
     */
    public T removeClass(String className) {
        this.cssClasses.remove(className);
        return self();
    }

    /**
     * Toggles a CSS class based on a condition.
     *
     * @param className the class name to toggle
     * @param condition true to add the class, false to remove it
     * @return this element for chaining
     */
    public T toggleClass(String className, boolean condition) {
        if (condition) {
            this.cssClasses.add(className);
        } else {
            this.cssClasses.remove(className);
        }
        return self();
    }

    /**
     * Sets the CSS parent for ancestor matching.
     *
     * @param parent the parent element
     * @return this element for chaining
     */
    public T cssParent(Styleable parent) {
        this.cssParent = parent;
        return self();
    }

    /**
     * Sets a style attribute for attribute selector matching.
     * <p>
     * Style attributes can be used with CSS attribute selectors like
     * {@code Panel[data-type="info"]} to target specific elements.
     *
     * @param name the attribute name
     * @param value the attribute value
     * @return this element for chaining
     */
    public T attr(String name, String value) {
        if (value != null) {
            this.styleAttrs.put(name, value);
        } else {
            this.styleAttrs.remove(name);
        }
        return self();
    }

    // Styleable interface implementation

    @Override
    public Optional<String> cssId() {
        return Optional.ofNullable(elementId);
    }

    @Override
    public Set<String> cssClasses() {
        Set<String> implied = style.implicitStyleNames();
        if (implied.isEmpty()) {
            return Collections.unmodifiableSet(cssClasses);
        }
        Set<String> all = new LinkedHashSet<>(cssClasses);
        all.addAll(implied);
        return Collections.unmodifiableSet(all);
    }

    @Override
    public Optional<Styleable> cssParent() {
        return Optional.ofNullable(cssParent);
    }

    @Override
    public Map<String, String> styleAttributes() {
        return Collections.unmodifiableMap(styleAttrs);
    }

    // Event handlers

    /**
     * Sets a key event handler for this element.
     * <p>
     * The handler receives keyboard events when:
     * <ul>
     *   <li>This element is focused (receives events directly)</li>
     *   <li>A focused descendant doesn't handle the event (bubbles up)</li>
     * </ul>
     * <p>
     * <strong>Relationship with {@link #focusable()}:</strong>
     * <ul>
     *   <li>{@code onKeyEvent()} registers a handler but does NOT make the element focusable</li>
     *   <li>{@code focusable()} adds the element to TAB navigation but does NOT add a handler</li>
     *   <li>Use both together when you want TAB navigation AND custom key handling</li>
     *   <li>Use only {@code onKeyEvent()} for handling keys from focused children or global shortcuts</li>
     * </ul>
     *
     * @param handler the key event handler
     * @return this element for chaining
     * @see #focusable()
     */
    public T onKeyEvent(KeyEventHandler handler) {
        this.keyHandler = handler;
        return self();
    }

    /**
     * Sets an action handler for this element.
     * <p>
     * This is a convenience method that dispatches both key and mouse events
     * to the action handler. Key events are dispatched when this element is focused,
     * mouse events when clicking on the element.
     *
     * @param handler the action handler to use
     * @return this element for chaining
     */
    public T onAction(ActionHandler handler) {
        onKeyEvent(event -> handler.dispatch(event)
                ? EventResult.HANDLED
                : EventResult.UNHANDLED);
        onMouseEvent(event -> handler.dispatch(event)
                ? EventResult.HANDLED
                : EventResult.UNHANDLED);
        return self();
    }

    /**
     * Sets the mouse event handler for this element.
     *
     * @param handler the mouse event handler
     * @return this element for chaining
     */
    public T onMouseEvent(MouseEventHandler handler) {
        this.mouseHandler = handler;
        return self();
    }

    /**
     * Makes this element draggable with the given handler.
     *
     * @param handler the drag handler
     * @return this element for chaining
     */
    public T onDrag(DragHandler handler) {
        this.dragHandler = handler;
        this.draggable = true;
        return self();
    }

    /**
     * Makes this element draggable with position callback.
     *
     * @param onMove callback receiving (deltaX, deltaY) during drag
     * @return this element for chaining
     */
    public T draggable(BiConsumer<Integer, Integer> onMove) {
        this.draggable = true;
        this.dragHandler = new DragHandler() {
            private int lastX, lastY;

            @Override
            public void onDragStart(int x, int y) {
                lastX = x;
                lastY = y;
            }

            @Override
            public void onDrag(int currentX, int currentY, int deltaX, int deltaY) {
                // Calculate incremental delta (since last drag event, not since start)
                int incrementalDeltaX = currentX - lastX;
                int incrementalDeltaY = currentY - lastY;
                lastX = currentX;
                lastY = currentY;
                onMove.accept(incrementalDeltaX, incrementalDeltaY);
            }

            @Override
            public void onDragEnd(int endX, int endY) {
            }
        };
        return self();
    }

    @Override
    public KeyEventHandler keyEventHandler() {
        return keyHandler;
    }

    @Override
    public MouseEventHandler mouseEventHandler() {
        return mouseHandler;
    }

    @Override
    public boolean isDraggable() {
        return draggable;
    }

    @Override
    public Rect renderedArea() {
        return lastRenderedArea;
    }

    @Override
    public EventResult handleKeyEvent(KeyEvent event, boolean focused) {
        if (keyHandler != null) {
            return keyHandler.handle(event);
        }
        return EventResult.UNHANDLED;
    }

    @Override
    public EventResult handleMouseEvent(MouseEvent event) {
        if (mouseHandler != null) {
            return mouseHandler.handle(event);
        }
        return EventResult.UNHANDLED;
    }

    /**
     * Returns the drag handler, if any.
     *
     * @return the drag handler, or {@code null} if none is set
     */
    public DragHandler dragHandler() {
        return dragHandler;
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.toolkit.event.DragHandler;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.event.KeyEventHandler;
import dev.tamboui.toolkit.event.MouseEventHandler;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;

import java.util.Collections;
import java.util.LinkedHashSet;
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

    protected Style style = Style.EMPTY;
    protected Constraint layoutConstraint;
    protected String elementId;
    protected Set<String> cssClasses = new LinkedHashSet<>();
    protected Styleable cssParent;
    protected KeyEventHandler keyHandler;
    protected MouseEventHandler mouseHandler;
    protected DragHandler dragHandler;
    protected boolean draggable;
    protected Rect lastRenderedArea;

    /**
     * Returns this element cast to the concrete type for method chaining.
     */
    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    // Foreground colors

    public T fg(Color color) {
        this.style = style.fg(color);
        return self();
    }

    public T black() {
        return fg(Color.BLACK);
    }

    public T red() {
        return fg(Color.RED);
    }

    public T green() {
        return fg(Color.GREEN);
    }

    public T yellow() {
        return fg(Color.YELLOW);
    }

    public T blue() {
        return fg(Color.BLUE);
    }

    public T magenta() {
        return fg(Color.MAGENTA);
    }

    public T cyan() {
        return fg(Color.CYAN);
    }

    public T white() {
        return fg(Color.WHITE);
    }

    public T gray() {
        return fg(Color.GRAY);
    }

    // Background colors

    public T bg(Color color) {
        this.style = style.bg(color);
        return self();
    }

    public T onBlack() {
        return bg(Color.BLACK);
    }

    public T onRed() {
        return bg(Color.RED);
    }

    public T onGreen() {
        return bg(Color.GREEN);
    }

    public T onYellow() {
        return bg(Color.YELLOW);
    }

    public T onBlue() {
        return bg(Color.BLUE);
    }

    public T onMagenta() {
        return bg(Color.MAGENTA);
    }

    public T onCyan() {
        return bg(Color.CYAN);
    }

    public T onWhite() {
        return bg(Color.WHITE);
    }

    // Modifiers

    public T bold() {
        this.style = style.bold();
        return self();
    }

    public T dim() {
        this.style = style.dim();
        return self();
    }

    public T italic() {
        this.style = style.italic();
        return self();
    }

    public T underlined() {
        this.style = style.underlined();
        return self();
    }

    public T reversed() {
        this.style = style.reversed();
        return self();
    }

    public T crossedOut() {
        this.style = style.crossedOut();
        return self();
    }

    // Style

    public T style(Style style) {
        this.style = style;
        return self();
    }

    public Style getStyle() {
        return style;
    }

    /**
     * Resolves the effective style by merging CSS and inline styles.
     * CSS styles provide the base, inline styles override.
     * <p>
     * This method is private because it should only be called once per render,
     * from the {@link #render} template method. Subclasses should use
     * {@link RenderContext#currentStyle()} to access the resolved style.
     *
     * @param context the render context for CSS resolution
     * @return the effective style combining CSS and inline styles
     */
    private Style resolveEffectiveStyle(RenderContext context) {
        Optional<CssStyleResolver> cssStyle = context.resolveStyle(this);
        // CSS provides base, inline style overrides
        return cssStyle.map(resolvedStyle -> resolvedStyle.toStyle().patch(style)).orElseGet(() -> style);
    }

    // ═══════════════════════════════════════════════════════════════
    // Render template method
    // ═══════════════════════════════════════════════════════════════

    /**
     * Renders this element. This is a template method that:
     * <ol>
     *   <li>Resolves the effective CSS + inline style</li>
     *   <li>Pushes the style onto the context's style stack</li>
     *   <li>Calls {@link #renderContent} for subclass-specific rendering</li>
     *   <li>Pops the style when done</li>
     * </ol>
     * <p>
     * Subclasses must implement {@link #renderContent} instead of overriding this method.
     * The current style is available via {@link RenderContext#currentStyle()}.
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
        Style effectiveStyle = resolveEffectiveStyle(context);

        if (context instanceof DefaultRenderContext) {
            DefaultRenderContext ctx = (DefaultRenderContext) context;
            ctx.withElement(this, effectiveStyle, () -> renderContent(frame, area, context));
            // Self-register for event routing if this element needs events
            if (needsEventRouting()) {
                ctx.registerElement(this, area);
            }
        } else {
            // Fallback for non-default contexts (e.g., testing)
            renderContent(frame, area, context);
        }
    }

    /**
     * Returns whether this element needs to be registered with the event router.
     */
    private boolean needsEventRouting() {
        return draggable || keyHandler != null || mouseHandler != null;
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

    // Layout constraint

    public T constraint(Constraint constraint) {
        this.layoutConstraint = constraint;
        return self();
    }

    public T length(int length) {
        return constraint(Constraint.length(length));
    }

    public T percent(int percent) {
        return constraint(Constraint.percentage(percent));
    }

    public T fill() {
        return constraint(Constraint.fill());
    }

    public T fill(int weight) {
        return constraint(Constraint.fill(weight));
    }

    public T min(int min) {
        return constraint(Constraint.min(min));
    }

    public T max(int max) {
        return constraint(Constraint.max(max));
    }

    @Override
    public Constraint constraint() {
        return layoutConstraint;
    }

    // ID for focus management

    public T id(String id) {
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

    // Styleable interface implementation

    @Override
    public Optional<String> cssId() {
        return Optional.ofNullable(elementId);
    }

    @Override
    public Set<String> cssClasses() {
        return Collections.unmodifiableSet(cssClasses);
    }

    @Override
    public Optional<Styleable> cssParent() {
        return Optional.ofNullable(cssParent);
    }

    // Event handlers

    /**
     * Sets the key event handler for this element.
     * The handler receives key events when this element is focused.
     *
     * @param handler the key event handler
     * @return this element for chaining
     */
    public T onKeyEvent(KeyEventHandler handler) {
        this.keyHandler = handler;
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
        if (focused && keyHandler != null) {
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
     */
    public DragHandler dragHandler() {
        return dragHandler;
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.toolkit;

import java.time.Duration;

import dev.tamboui.style.StyledAreaRegistry;
import dev.tamboui.tfx.Effect;
import dev.tamboui.tfx.TFxDuration;
import dev.tamboui.toolkit.app.ToolkitPostRenderProcessor;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.tui.EventHandler;
import dev.tamboui.tui.Renderer;
import dev.tamboui.tui.event.TickEvent;

/**
 * High-level API for integrating TFX effects with the Toolkit DSL.
 * <p>
 * ToolkitEffects provides a simple interface for adding effects to elements by
 * ID, with automatic resolution of element areas during rendering. Effects
 * automatically follow their target elements when the terminal resizes because
 * areas are looked up dynamically each frame.
 * <p>
 * <b>Design Philosophy:</b>
 * <ul>
 * <li><b>Element-targeted:</b> Effects can target specific elements by ID</li>
 * <li><b>Non-invasive:</b> Uses wrapper pattern like FpsOverlay</li>
 * <li><b>Automatic timing:</b> Uses TickEvent elapsed time for consistency</li>
 * <li><b>Resize-safe:</b> Effects follow elements automatically on resize</li>
 * </ul>
 * <p>
 * <b>Usage with ToolkitRunner:</b>
 * 
 * <pre>{@code
 * ToolkitEffects effects = new ToolkitEffects();
 * effects.addEffect("header", Fx.fadeFromFg(Color.BLACK, 800, Interpolation.QuadOut));
 *
 * try (ToolkitRunner runner = ToolkitRunner.create(config)) {
 *     effects.runWith(runner, () -> panel("Header", text("Welcome!")).id("header").rounded());
 * }
 * }</pre>
 * <p>
 * <b>Usage with TuiRunner:</b>
 * 
 * <pre>{@code
 * ToolkitEffects effects = new ToolkitEffects();
 * effects.addEffect("header", Fx.fadeFromFg(Color.BLACK, 800, Interpolation.QuadOut));
 *
 * try (TuiRunner runner = TuiRunner.create(config)) {
 *     runner.run(effects.wrapHandler(myHandler, eventRouter),
 *             effects.wrapRenderer(myRenderer, eventRouter));
 * }
 * }</pre>
 *
 * @see ElementEffectRegistry
 * @see dev.tamboui.tfx.tui.TfxIntegration
 */
public final class ToolkitEffects {

    private final ElementEffectRegistry registry;
    private Duration lastElapsed;

    /**
     * Creates a new ToolkitEffects instance.
     */
    public ToolkitEffects() {
        this.registry = new ElementEffectRegistry();
        this.lastElapsed = Duration.ZERO;
    }

    /**
     * Adds an effect that targets a specific element by ID.
     * <p>
     * The effect will be applied to the element's rendered area once the element is
     * rendered and its area is available.
     *
     * @param elementId
     *            the ID of the target element
     * @param effect
     *            the effect to add
     */
    public void addEffect(String elementId, Effect effect) {
        registry.addEffect(elementId, effect);
    }

    /**
     * Adds an effect that targets a specific element.
     * <p>
     * Convenience method that extracts the element's ID.
     *
     * @param element
     *            the target element
     * @param effect
     *            the effect to add
     * @throws IllegalArgumentException
     *             if the element has no ID
     */
    public void addEffect(Element element, Effect effect) {
        registry.addEffect(element, effect);
    }

    /**
     * Adds an effect that targets all elements matching a CSS-like selector.
     * <p>
     * Each matching element receives a copy of the effect. The effect is applied
     * once the elements are rendered and their areas are available.
     * <p>
     * Supported selectors:
     * <ul>
     * <li>{@code #id} - matches element by ID</li>
     * <li>{@code .class} - matches elements by CSS class</li>
     * <li>{@code Type} - matches elements by type name (e.g., Panel, Button)</li>
     * <li>{@code Type.class} - combined type and class</li>
     * <li>{@code .class1.class2} - multiple classes (all must match)</li>
     * </ul>
     * <p>
     * Example:
     * 
     * <pre>{@code
     * // Apply effect to all elements with class "highlight"
     * effects.addEffectBySelector(".highlight", Fx.fadeToFg(Color.YELLOW, 500));
     *
     * // Apply effect to all Panel elements
     * effects.addEffectBySelector("Panel", Fx.fadeToFg(Color.CYAN, 500));
     * }</pre>
     *
     * @param selector
     *            the CSS-like selector
     * @param effect
     *            the effect to add (copied for each matching element)
     */
    public void addEffectBySelector(String selector, Effect effect) {
        registry.addEffectBySelector(selector, effect);
    }

    /**
     * Adds a global effect that applies to the entire frame area.
     * <p>
     * Global effects are not targeted to any specific element.
     *
     * @param effect
     *            the effect to add
     */
    public void addGlobalEffect(Effect effect) {
        registry.addGlobalEffect(effect);
    }

    /**
     * Returns whether any effects are currently running or pending.
     *
     * @return true if effects are active
     */
    public boolean isRunning() {
        return registry.isRunning();
    }

    /**
     * Clears all effects.
     */
    public void clear() {
        registry.clear();
    }

    /**
     * Wraps an event handler to capture tick timing and force redraws.
     * <p>
     * The wrapper:
     * <ul>
     * <li>Captures elapsed time from {@link TickEvent}s</li>
     * <li>Forces redraws when effects are active</li>
     * </ul>
     *
     * @param handler
     *            the event handler to wrap
     * @return a wrapped event handler
     */
    public EventHandler wrapHandler(EventHandler handler) {
        return (event, runner) -> {
            // Capture elapsed time from tick events
            if (event instanceof TickEvent) {
                lastElapsed = ((TickEvent) event).elapsed();
            }

            // Delegate to wrapped handler
            boolean shouldRedraw = handler.handle(event, runner);

            // Force redraw if effects are running
            if (registry.isRunning()) {
                return true;
            }

            return shouldRedraw;
        };
    }

    /**
     * Wraps a renderer to resolve and process effects after rendering.
     * <p>
     * The wrapper:
     * <ul>
     * <li>Calls the wrapped renderer first</li>
     * <li>Expands pending selector effects to element and styled area
     * instances</li>
     * <li>Processes all active effects on the buffer with dynamic area lookup</li>
     * </ul>
     * <p>
     * When a StyledAreaRegistry is provided, effects can target styled spans using
     * CSS selectors like {@code .highlight} or {@code #myPanel .highlight}.
     * <p>
     * When a FocusManager is provided, pseudo-class selectors like {@code :focus}
     * are supported.
     *
     * @param renderer
     *            the renderer to wrap
     * @param elementRegistry
     *            the element registry containing element areas
     * @param styledAreaRegistry
     *            the styled area registry
     * @param focusManager
     *            the focus manager for pseudo-class state
     * @return a wrapped renderer
     */
    public Renderer wrapRenderer(Renderer renderer, ElementRegistry elementRegistry,
            StyledAreaRegistry styledAreaRegistry, FocusManager focusManager) {
        return frame -> {
            // Render the UI first
            renderer.render(frame);

            // Expand pending selector effects
            registry.expandSelectors(elementRegistry, styledAreaRegistry);

            // Process effects on the buffer with dynamic area lookup
            if (registry.isRunning()) {
                TFxDuration delta = TFxDuration.fromJavaDuration(lastElapsed);
                registry.processEffects(delta, frame.buffer(), frame.area(), elementRegistry,
                        styledAreaRegistry, focusManager);
            }
        };
    }

    /**
     * Creates a post-render processor for use with ToolkitRunner.Builder.
     * <p>
     * This returns a processor that expands pending selector effects and processes
     * all active effects on the buffer. Areas are looked up dynamically from the
     * element registry each frame, so effects automatically follow elements when
     * the terminal resizes.
     * <p>
     * When the Frame has a StyledAreaRegistry configured, effects can also target
     * styled spans using CSS selectors like {@code .highlight} or
     * {@code #myPanel .highlight}.
     * <p>
     * <b>Usage:</b>
     * 
     * <pre>{@code
     * ToolkitEffects effects = new ToolkitEffects();
     * effects.addEffect("header", Fx.fadeFromFg(Color.BLACK, 800, Interpolation.QuadOut));
     *
     * try (var runner = ToolkitRunner.builder().postRenderProcessor(effects.asPostRenderProcessor())
     *         .build()) {
     *     runner.run(() -> panel("Header", text("Welcome!")).id("header"));
     * }
     * }</pre>
     *
     * @return a post-render processor for ToolkitRunner
     */
    public ToolkitPostRenderProcessor asPostRenderProcessor() {
        return (frame, elementRegistry, styledAreaRegistry, focusManager, elapsed) -> {
            // Expand pending selector effects
            registry.expandSelectors(elementRegistry, styledAreaRegistry);

            // Process effects on the buffer with dynamic area lookup
            if (registry.isRunning()) {
                TFxDuration delta = TFxDuration.fromJavaDuration(elapsed);
                registry.processEffects(delta, frame.buffer(), frame.area(), elementRegistry,
                        styledAreaRegistry, focusManager);
            }
        };
    }

    /**
     * Returns the underlying effect registry.
     * <p>
     * This provides direct access for advanced use cases.
     *
     * @return the effect registry
     */
    public ElementEffectRegistry registry() {
        return registry;
    }
}

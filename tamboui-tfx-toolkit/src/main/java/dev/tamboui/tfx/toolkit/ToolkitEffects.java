/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.toolkit;

import dev.tamboui.tfx.Effect;
import dev.tamboui.tfx.TFxDuration;
import dev.tamboui.toolkit.app.ToolkitPostRenderProcessor;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.tui.EventHandler;
import dev.tamboui.tui.Renderer;
import dev.tamboui.tui.event.TickEvent;

import java.time.Duration;

/**
 * High-level API for integrating TFX effects with the Toolkit DSL.
 * <p>
 * ToolkitEffects provides a simple interface for adding effects to elements
 * by ID, with automatic resolution of element areas during rendering.
 * <p>
 * <b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Element-targeted:</b> Effects can target specific elements by ID</li>
 *   <li><b>Non-invasive:</b> Uses wrapper pattern like FpsOverlay</li>
 *   <li><b>Automatic timing:</b> Uses TickEvent elapsed time for consistency</li>
 * </ul>
 * <p>
 * <b>Usage with ToolkitRunner:</b>
 * <pre>{@code
 * ToolkitEffects effects = new ToolkitEffects();
 * effects.addEffect("header", Fx.fadeFromFg(Color.BLACK, 800, Interpolation.QuadOut));
 *
 * try (ToolkitRunner runner = ToolkitRunner.create(config)) {
 *     effects.runWith(runner, () ->
 *         panel("Header", text("Welcome!"))
 *             .id("header")
 *             .rounded()
 *     );
 * }
 * }</pre>
 * <p>
 * <b>Usage with TuiRunner:</b>
 * <pre>{@code
 * ToolkitEffects effects = new ToolkitEffects();
 * effects.addEffect("header", Fx.fadeFromFg(Color.BLACK, 800, Interpolation.QuadOut));
 *
 * try (TuiRunner runner = TuiRunner.create(config)) {
 *     runner.run(
 *         effects.wrapHandler(myHandler, eventRouter),
 *         effects.wrapRenderer(myRenderer, eventRouter)
 *     );
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
     * The effect will be applied to the element's rendered area once
     * the element is rendered and its area is available.
     *
     * @param elementId the ID of the target element
     * @param effect    the effect to add
     */
    public void addEffect(String elementId, Effect effect) {
        registry.addEffect(elementId, effect);
    }

    /**
     * Adds an effect that targets a specific element.
     * <p>
     * Convenience method that extracts the element's ID.
     *
     * @param element the target element
     * @param effect  the effect to add
     * @throws IllegalArgumentException if the element has no ID
     */
    public void addEffect(Element element, Effect effect) {
        registry.addEffect(element, effect);
    }

    /**
     * Adds a global effect that applies to the entire frame area.
     * <p>
     * Global effects are not targeted to any specific element.
     *
     * @param effect the effect to add
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
     *   <li>Captures elapsed time from {@link TickEvent}s</li>
     *   <li>Forces redraws when effects are active</li>
     * </ul>
     *
     * @param handler the event handler to wrap
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
     *   <li>Calls the wrapped renderer first</li>
     *   <li>Resolves pending effects to element areas</li>
     *   <li>Processes all active effects on the buffer</li>
     * </ul>
     *
     * @param renderer        the renderer to wrap
     * @param elementRegistry the element registry containing element areas
     * @return a wrapped renderer
     */
    public Renderer wrapRenderer(Renderer renderer, ElementRegistry elementRegistry) {
        return frame -> {
            // Render the UI first
            renderer.render(frame);

            // Resolve pending effects to element areas
            registry.resolvePendingEffects(elementRegistry);

            // Process effects on the buffer
            if (registry.isRunning()) {
                TFxDuration delta = TFxDuration.fromJavaDuration(lastElapsed);
                registry.processEffects(delta, frame.buffer(), frame.area());
            }
        };
    }

    /**
     * Creates a post-render processor for use with ToolkitRunner.Builder.
     * <p>
     * This returns a processor that resolves pending effects to element areas
     * and processes all active effects on the buffer. The processor receives
     * elapsed time from ToolkitRunner, so no event handler wrapping is needed.
     * <p>
     * <b>Usage:</b>
     * <pre>{@code
     * ToolkitEffects effects = new ToolkitEffects();
     * effects.addEffect("header", Fx.fadeFromFg(Color.BLACK, 800, Interpolation.QuadOut));
     *
     * try (var runner = ToolkitRunner.builder()
     *         .postRenderProcessor(effects.asPostRenderProcessor())
     *         .build()) {
     *     runner.run(() -> panel("Header", text("Welcome!")).id("header"));
     * }
     * }</pre>
     *
     * @return a post-render processor for ToolkitRunner
     */
    public ToolkitPostRenderProcessor asPostRenderProcessor() {
        return (frame, elementRegistry, elapsed) -> {
            // Resolve pending effects to element areas
            registry.resolvePendingEffects(elementRegistry);

            // Process effects on the buffer
            if (registry.isRunning()) {
                TFxDuration delta = TFxDuration.fromJavaDuration(elapsed);
                registry.processEffects(delta, frame.buffer(), frame.area());
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

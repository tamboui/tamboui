/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.tui;

import dev.tamboui.layout.Rect;
import dev.tamboui.tfx.Effect;
import dev.tamboui.tfx.EffectManager;
import dev.tamboui.tfx.TFxDuration;
import dev.tamboui.tui.EventHandler;
import dev.tamboui.tui.Renderer;
import dev.tamboui.tui.TuiRunner;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.TickEvent;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Integrates TFX effects with the TuiRunner event loop.
 * <p>
 * TfxIntegration provides a non-invasive way to add visual effects to TUI applications
 * by wrapping the event handler and renderer. Effects are processed during tick events
 * and applied after the user's rendering completes.
 * <p>
 * <b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Non-invasive:</b> Wraps existing handlers without requiring changes to application code</li>
 *   <li><b>Automatic timing:</b> Uses TickEvent elapsed time for consistent animation</li>
 *   <li><b>Forced redraws:</b> Automatically triggers redraws when effects are running</li>
 * </ul>
 * <p>
 * <b>Usage:</b>
 * <pre>{@code
 * TfxIntegration tfx = new TfxIntegration();
 * tfx.addEffect(Fx.fadeFromFg(Color.BLACK, 1000, Interpolation.QuadOut));
 *
 * tui.run(
 *     tfx.wrapHandler((event, runner) -> handleEvent(event)),
 *     tfx.wrapRenderer(frame -> renderUI(frame))
 * );
 * }</pre>
 *
 * @see EffectManager
 * @see Effect
 */
public final class TfxIntegration {

    private final EffectManager effectManager;
    /**
     * Last elapsed duration from tick events. Uses AtomicReference for
     * safe visibility between event handler and renderer.
     */
    private final AtomicReference<Duration> lastElapsed = new AtomicReference<>(Duration.ZERO);

    /**
     * Creates a new TfxIntegration instance.
     */
    public TfxIntegration() {
        this.effectManager = new EffectManager();
    }

    /**
     * Adds an effect to be processed.
     * <p>
     * The effect will be applied to the entire frame area during rendering.
     * For effects that target specific areas, use {@link #addEffect(Effect, Rect)}.
     *
     * @param effect the effect to add
     */
    public void addEffect(Effect effect) {
        effectManager.addEffect(effect);
    }

    /**
     * Adds an effect with a specific target area.
     * <p>
     * The effect will only be applied within the specified rectangular area.
     *
     * @param effect the effect to add
     * @param area   the area to apply the effect to
     */
    public void addEffect(Effect effect, Rect area) {
        effectManager.addEffect(effect.withArea(area));
    }

    /**
     * Returns whether any effects are currently running.
     * <p>
     * This can be used to determine if the UI needs continuous redraws.
     *
     * @return true if effects are running
     */
    public boolean isRunning() {
        return effectManager.isRunning();
    }

    /**
     * Clears all effects.
     */
    public void clearEffects() {
        effectManager.clear();
    }

    /**
     * Returns the number of active effects.
     *
     * @return the effect count
     */
    public int effectCount() {
        return effectManager.size();
    }

    /**
     * Wraps an event handler to capture tick timing and force redraws when effects are running.
     * <p>
     * The wrapper:
     * <ul>
     *   <li>Captures elapsed time from {@link TickEvent}s for effect timing</li>
     *   <li>Forces redraws (returns true) when effects are active</li>
     *   <li>Delegates all events to the wrapped handler</li>
     * </ul>
     *
     * @param handler the event handler to wrap
     * @return a wrapped event handler
     */
    public EventHandler wrapHandler(EventHandler handler) {
        return (event, runner) -> {
            // Capture elapsed time from tick events
            if (event instanceof TickEvent) {
                lastElapsed.set(((TickEvent) event).elapsed());
            }

            // Delegate to wrapped handler
            boolean shouldRedraw = handler.handle(event, runner);

            // Force redraw if effects are running
            if (effectManager.isRunning()) {
                return true;
            }

            return shouldRedraw;
        };
    }

    /**
     * Wraps a renderer to process effects after rendering.
     * <p>
     * The wrapper:
     * <ul>
     *   <li>Calls the wrapped renderer first</li>
     *   <li>Processes all active effects on the buffer</li>
     *   <li>Automatically removes completed effects</li>
     * </ul>
     *
     * @param renderer the renderer to wrap
     * @return a wrapped renderer
     */
    public Renderer wrapRenderer(Renderer renderer) {
        return frame -> {
            // Render the UI first
            renderer.render(frame);

            // Process effects on the buffer
            if (effectManager.isRunning()) {
                TFxDuration delta = TFxDuration.fromJavaDuration(lastElapsed.get());
                effectManager.processEffects(delta, frame.buffer(), frame.area());
            }
        };
    }

    /**
     * Utility method to run a TUI application with TFX integration.
     * <p>
     * This is a convenience method that wraps the handler and renderer
     * and calls {@link TuiRunner#run(EventHandler, Renderer)}.
     *
     * @param runner   the TUI runner
     * @param handler  the event handler
     * @param renderer the renderer
     * @throws Exception if an error occurs during execution
     */
    public void runWith(TuiRunner runner, EventHandler handler, Renderer renderer) throws Exception {
        runner.run(wrapHandler(handler), wrapRenderer(renderer));
    }
}

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

/// Integrates TFX effects with the TuiRunner event loop.
///
///
///
/// TfxIntegration provides a non-invasive way to add visual effects to TUI applications
/// by wrapping the event handler and renderer. Effects are processed during tick events
/// and applied after the user's rendering completes.
///
///
///
/// **Design Philosophy:**
///
/// - **Non-invasive:** Wraps existing handlers without requiring changes to application code
/// - **Automatic timing:** Uses TickEvent elapsed time for consistent animation
/// - **Forced redraws:** Automatically triggers redraws when effects are running
///
///
///
///
/// **Usage:**
/// ```java
/// TfxIntegration tfx = new TfxIntegration();
/// tfx.addEffect(Fx.fadeFromFg(Color.BLACK, 1000, Interpolation.QuadOut));
///
/// tui.run(
///     tfx.wrapHandler((event, runner) -> handleEvent(event)),
///     tfx.wrapRenderer(frame -> renderUI(frame))
/// );
/// }
/// ```
///
/// @see EffectManager
/// @see Effect
public final class TfxIntegration {

    private final EffectManager effectManager;
    private Duration lastElapsed;

    /// Creates a new TfxIntegration instance.
    public TfxIntegration() {
        this.effectManager = new EffectManager();
        this.lastElapsed = Duration.ZERO;
    }

    /// Adds an effect to be processed.
    ///
    ///
    ///
    /// The effect will be applied to the entire frame area during rendering.
    /// For effects that target specific areas, use {@link #addEffect(Effect, Rect)}.
    ///
    /// @param effect the effect to add
    public void addEffect(Effect effect) {
        effectManager.addEffect(effect);
    }

    /// Adds an effect with a specific target area.
    ///
    ///
    ///
    /// The effect will only be applied within the specified rectangular area.
    ///
    /// @param effect the effect to add
    /// @param area   the area to apply the effect to
    public void addEffect(Effect effect, Rect area) {
        effectManager.addEffect(effect.withArea(area));
    }

    /// Returns whether any effects are currently running.
    ///
    ///
    ///
    /// This can be used to determine if the UI needs continuous redraws.
    ///
    /// @return true if effects are running
    public boolean isRunning() {
        return effectManager.isRunning();
    }

    /// Clears all effects.
    public void clearEffects() {
        effectManager.clear();
    }

    /// Returns the number of active effects.
    ///
    /// @return the effect count
    public int effectCount() {
        return effectManager.size();
    }

    /// Wraps an event handler to capture tick timing and force redraws when effects are running.
    ///
    ///
    ///
    /// The wrapper:
    ///
    /// - Captures elapsed time from {@link TickEvent}s for effect timing
    /// - Forces redraws (returns true) when effects are active
    /// - Delegates all events to the wrapped handler
    ///
    ///
    /// @param handler the event handler to wrap
    /// @return a wrapped event handler
    public EventHandler wrapHandler(EventHandler handler) {
        return (event, runner) -> {
            // Capture elapsed time from tick events
            if (event instanceof TickEvent) {
                lastElapsed = ((TickEvent) event).elapsed();
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

    /// Wraps a renderer to process effects after rendering.
    ///
    ///
    ///
    /// The wrapper:
    ///
    /// - Calls the wrapped renderer first
    /// - Processes all active effects on the buffer
    /// - Automatically removes completed effects
    ///
    ///
    /// @param renderer the renderer to wrap
    /// @return a wrapped renderer
    public Renderer wrapRenderer(Renderer renderer) {
        return frame -> {
            // Render the UI first
            renderer.render(frame);

            // Process effects on the buffer
            if (effectManager.isRunning()) {
                TFxDuration delta = TFxDuration.fromJavaDuration(lastElapsed);
                effectManager.processEffects(delta, frame.buffer(), frame.area());
            }
        };
    }

    /// Utility method to run a TUI application with TFX integration.
    ///
    ///
    ///
    /// This is a convenience method that wraps the handler and renderer
    /// and calls {@link TuiRunner#run(EventHandler, Renderer)}.
    ///
    /// @param runner   the TUI runner
    /// @param handler  the event handler
    /// @param renderer the renderer
    /// @throws Exception if an error occurs during execution
    public void runWith(TuiRunner runner, EventHandler handler, Renderer renderer) throws Exception {
        runner.run(wrapHandler(handler), wrapRenderer(renderer));
    }
}


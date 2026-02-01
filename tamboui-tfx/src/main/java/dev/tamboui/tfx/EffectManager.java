/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a collection of terminal UI effects and their lifecycle.
 * <p>
 * The EffectManager is responsible for:
 * <ul>
 *   <li><b>Effect Storage:</b> Maintaining a collection of active effects</li>
 *   <li><b>Frame Processing:</b> Processing all effects each frame with the elapsed time</li>
 *   <li><b>Lifecycle Management:</b> Automatically removing completed effects</li>
 *   <li><b>Performance Optimization:</b> Providing {@link #isRunning()} to optimize render loops</li>
 * </ul>
 * <p>
 * <b>Design Philosophy:</b>
 * <p>
 * The EffectManager follows a "fire and forget" pattern. Once an effect is added,
 * it will be processed automatically each frame until completion, at which point it
 * is removed. This eliminates the need for manual effect lifecycle management.
 * <p>
 * <b>Usage Pattern:</b>
 * <pre>{@code
 * EffectManager manager = new EffectManager();
 * 
 * // Add effects
 * manager.addEffect(Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut));
 * 
 * // In render loop
 * if (manager.isRunning()) {
 *     TFxDuration delta = TFxDuration.fromMillis(frameTimeMs);
 *     manager.processEffects(delta, buffer, area);
 * }
 * }</pre>
 * <p>
 * <b>Thread Safety:</b>
 * <p>
 * EffectManager is not thread-safe. All operations should be performed on the
 * same thread that handles rendering (typically the main render thread).
 */
public final class EffectManager {
    
    private final List<Effect> effects;
    
    /**
     * Creates a new EffectManager.
     */
    public EffectManager() {
        this.effects = new ArrayList<>();
    }
    
    /**
     * Adds an effect to be processed by the manager.
     * <p>
     * The effect will be processed each frame until it is complete.
     * 
     * @param effect The effect to add to the manager
     */
    public void addEffect(Effect effect) {
        effects.add(effect);
    }
    
    /**
     * Returns whether there are any active effects currently being managed.
     * <p>
     * This method is useful for optimizing render loops: if {@code isRunning()}
     * returns {@code false} and there are no other state changes, the UI redraw
     * can be skipped.
     * 
     * @return true if there are active effects, false otherwise
     */
    public boolean isRunning() {
        return !effects.isEmpty();
    }
    
    /**
     * Processes all active effects for the given duration.
     * <p>
     * This method should be called each frame in your render loop. It will:
     * 1. Process each effect for the specified duration
     * 2. Remove completed effects
     * 
     * @param duration The time elapsed since the last frame
     * @param buffer The buffer to render effects into
     * @param area The area within which effects should be rendered
     */
    public void processEffects(TFxDuration duration, Buffer buffer, Rect area) {
        // Iterate over a copy to allow concurrent additions during processing
        List<Effect> snapshot = new ArrayList<>(effects);
        List<Effect> toRemove = new ArrayList<>();

        for (Effect effect : snapshot) {
            effect.process(duration, buffer, area);
            if (effect.done()) {
                toRemove.add(effect);
            }
        }

        effects.removeAll(toRemove);
    }
    
    /**
     * Returns the number of active effects.
     *
     * @return the number of active effects
     */
    public int size() {
        return effects.size();
    }
    
    /**
     * Clears all effects.
     */
    public void clear() {
        effects.clear();
    }
}



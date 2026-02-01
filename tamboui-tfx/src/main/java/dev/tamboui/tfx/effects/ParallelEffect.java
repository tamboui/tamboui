/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.effects;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.tfx.CellFilter;
import dev.tamboui.tfx.TFxColorSpace;
import dev.tamboui.tfx.TFxDuration;
import dev.tamboui.tfx.Effect;
import dev.tamboui.tfx.EffectTimer;
import dev.tamboui.tfx.Interpolation;
import dev.tamboui.tfx.Shader;
import dev.tamboui.layout.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * Shader implementation that runs multiple effects in parallel, simultaneously.
 * <p>
 * ParallelEffect composes multiple effects to run concurrently, allowing them
 * to overlap and combine visually. All effects start at the same time and run
 * independently until completion.
 * <p>
 * <b>Design Philosophy:</b>
 * <p>
 * Parallel composition enables layering effects to create rich, multi-layered
 * animations. Each effect operates independently on the same buffer, allowing
 * them to combine visually (e.g., a fade effect combined with a dissolve effect).
 * <p>
 * <b>Behavior:</b>
 * <ul>
 *   <li>All effects start simultaneously</li>
 *   <li>Effects run independently and can have different durations</li>
 *   <li>Effects are applied to the same buffer in order (later effects may
 *       overwrite earlier ones)</li>
 *   <li>The parallel effect completes when all constituent effects have completed</li>
 * </ul>
 * <p>
 * <b>Visual Layering:</b>
 * <p>
 * When multiple effects are applied in parallel, they are processed in the order
 * they were provided. Later effects may visually overwrite earlier effects depending
 * on their cell filters and rendering logic. Use cell filters to control which
 * cells each effect affects.
 * <p>
 * <b>Usage Pattern:</b>
 * <pre>{@code
 * // Run fade and dissolve simultaneously
 * Effect parallel = Fx.parallel(
 *     Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut)
 *         .withFilter(CellFilter.text()),
 *     Fx.dissolve(2500, Interpolation.QuadOut)
 *         .withFilter(CellFilter.text())
 * );
 * }</pre>
 * <p>
 * This shader is typically created through {@link dev.tamboui.tfx.Fx#parallel(Effect...)}
 * rather than directly.
 */
public final class ParallelEffect implements Shader {
    
    private final List<Effect> effects;
    
    /**
     * Creates a parallel effect that runs the given effects simultaneously.
     *
     * @param effects the list of effects to run in parallel
     * @return a new parallel effect
     */
    public static ParallelEffect of(List<Effect> effects) {
        return new ParallelEffect(effects);
    }
    
    /**
     * Creates a parallel effect that runs the given effects simultaneously.
     *
     * @param effects the effects to run in parallel
     * @return a new parallel effect
     */
    public static ParallelEffect of(Effect... effects) {
        List<Effect> effectList = new ArrayList<>();
        for (Effect effect : effects) {
            effectList.add(effect);
        }
        return new ParallelEffect(effectList);
    }
    
    private ParallelEffect(List<Effect> effects) {
        this.effects = new ArrayList<>(effects);
    }
    
    @Override
    public String name() {
        return "parallel";
    }
    
    @Override
    public TFxDuration process(TFxDuration duration, Buffer buffer, Rect area) {
        TFxDuration remaining = duration;
        
        for (Effect effect : effects) {
            if (effect.running()) {
                TFxDuration effectRemaining = effect.process(duration, buffer, area);
                
                if (effectRemaining == null) {
                    // Effect is still running
                    remaining = null;
                } else if (remaining != null) {
                    // Take the minimum remaining duration
                    if (effectRemaining.asMillis() < remaining.asMillis()) {
                        remaining = effectRemaining;
                    }
                }
            }
        }
        
        return remaining;
    }
    
    @Override
    public boolean done() {
        // All effects must be done
        for (Effect effect : effects) {
            if (!effect.done()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public Rect area() {
        return null; // Parallel effects don't have a fixed area
    }
    
    @Override
    public void setArea(Rect area) {
        // Effects are immutable, so we can't modify them
        // Area should be set when creating the effects
    }
    
    @Override
    public EffectTimer timer() {
        // We can't easily access the timer from Effect, so return null
        // The timer will be managed by individual effects
        return null;
    }
    
    @Override
    public EffectTimer mutableTimer() {
        return timer();
    }
    
    @Override
    public CellFilter cellFilter() {
        return null; // Parallel effects don't have a single filter
    }
    
    @Override
    public void setCellFilter(CellFilter filter) {
        // Effects are immutable, so we can't modify them
        // Filter should be set when creating the effects
    }
    
    @Override
    public TFxColorSpace colorSpace() {
        return null; // Parallel effects don't have a single color space
    }
    
    @Override
    public void setColorSpace(TFxColorSpace colorSpace) {
        // Effects are immutable, so we can't modify them
        // ColorSpace should be set when creating the effects
    }
    
    @Override
    public Shader copy() {
        List<Effect> copiedEffects = new ArrayList<>();
        for (Effect effect : effects) {
            // Effects are immutable, so we can reuse them
            copiedEffects.add(effect);
        }
        return new ParallelEffect(copiedEffects);
    }
}


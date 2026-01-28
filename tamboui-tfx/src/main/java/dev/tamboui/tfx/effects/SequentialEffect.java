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
import dev.tamboui.tfx.Shader;
import dev.tamboui.layout.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * Shader implementation that runs multiple effects sequentially, one after another.
 * <p>
 * SequentialEffect composes multiple effects into a chain, where each effect runs
 * to completion before the next one begins. This allows creating complex animations
 * by combining simple effects.
 * <p>
 * <b>Design Philosophy:</b>
 * <p>
 * Sequential composition enables building sophisticated animations from simple
 * building blocks. Each effect in the sequence runs independently, and overflow
 * time from one effect is automatically passed to the next.
 * <p>
 * <b>Behavior:</b>
 * <ul>
 *   <li>Effects run in the order they are provided</li>
 *   <li>Each effect must complete before the next begins</li>
 *   <li>Overflow time from completed effects is passed to the next effect</li>
 *   <li>The sequence completes when all effects have completed</li>
 * </ul>
 * <p>
 * <b>Usage Pattern:</b>
 * <pre>{@code
 * // Chain fade-in followed by dissolve
 * Effect sequence = Fx.sequence(
 *     Fx.fadeFromFg(Color.BLACK, 500, Interpolation.QuadOut),
 *     Fx.dissolve(800, Interpolation.Linear)
 * );
 * }</pre>
 * <p>
 * This shader is typically created through {@link dev.tamboui.tfx.Fx#sequence(Effect...)}
 * rather than directly.
 */
public final class SequentialEffect implements Shader {
    
    private final List<Effect> effects;
    private int current;
    
    /**
     * Creates a sequential effect that runs the given effects one after another.
     *
     * @param effects the list of effects to run sequentially
     * @return a new sequential effect
     */
    public static SequentialEffect of(List<Effect> effects) {
        return new SequentialEffect(effects);
    }
    
    /**
     * Creates a sequential effect that runs the given effects one after another.
     *
     * @param effects the effects to run sequentially
     * @return a new sequential effect
     */
    public static SequentialEffect of(Effect... effects) {
        List<Effect> effectList = new ArrayList<>();
        for (Effect effect : effects) {
            effectList.add(effect);
        }
        return new SequentialEffect(effectList);
    }
    
    private SequentialEffect(List<Effect> effects) {
        this.effects = new ArrayList<>(effects);
        this.current = 0;
    }
    
    @Override
    public String name() {
        return "sequence";
    }
    
    @Override
    public TFxDuration process(TFxDuration duration, Buffer buffer, Rect area) {
        TFxDuration remaining = duration;
        
        while (remaining != null && !done()) {
            Effect effect = effects.get(current);
            
            remaining = effect.process(remaining, buffer, area);
            
            if (effect.done()) {
                current++;
            }
        }
        
        return remaining;
    }
    
    @Override
    public boolean done() {
        return current >= effects.size();
    }
    
    @Override
    public Rect area() {
        return null; // Sequential effects don't have a fixed area
    }
    
    @Override
    public void setArea(Rect area) {
        // Effects are immutable, so we can't modify them
        // Area should be set when creating the effects
    }
    
    @Override
    public EffectTimer timer() {
        // Sum up all effect durations
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
        return null; // Sequential effects don't have a single filter
    }
    
    @Override
    public void setCellFilter(CellFilter filter) {
        // Effects are immutable, so we can't modify them
        // Filter should be set when creating the effects
    }
    
    @Override
    public TFxColorSpace colorSpace() {
        return null; // Sequential effects don't have a single color space
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
        SequentialEffect copy = new SequentialEffect(copiedEffects);
        copy.current = current;
        return copy;
    }
}


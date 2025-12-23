/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;

/**
 * Represents an effect that can be applied to terminal cells.
 * <p>
 * The Effect class is a wrapper around a {@link Shader} that provides a fluent,
 * immutable API for configuring effects. It separates the effect implementation
 * (Shader) from the effect configuration (Effect), allowing effects to be easily
 * composed and customized without modifying the underlying shader.
 * <p>
 * <b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Immutability:</b> All configuration methods return new Effect instances,
 *       making effects safe to share and compose.</li>
 *   <li><b>Separation of Concerns:</b> Effects handle configuration and lifecycle,
 *       while Shaders handle the actual rendering logic.</li>
 *   <li><b>Fluent API:</b> Method chaining allows for readable effect construction.</li>
 * </ul>
 * <p>
 * <b>Usage Pattern:</b>
 * <pre>{@code
 * Effect fade = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut)
 *     .withFilter(CellFilter.text())
 *     .withColorSpace(TFxColorSpace.HSL)
 *     .withPattern(SweepPattern.leftToRight(10.0f));
 * }</pre>
 * <p>
 * Effects are stateful and should be processed each frame until they complete.
 * Use {@link EffectManager} to manage multiple effects and their lifecycle.
 */
public final class Effect {
    
    private final Shader shader;
    
    /**
     * Creates a new Effect with the specified shader.
     */
    public static Effect of(Shader shader) {
        return new Effect(shader);
    }
    
    private Effect(Shader shader) {
        this.shader = shader;
    }
    
    /**
     * Creates a new Effect with the specified area.
     * 
     * @param area The rectangular area where the effect will be applied
     * @return A new Effect instance with the specified area
     */
    public Effect withArea(Rect area) {
        Shader newShader = shader.copy();
        newShader.setArea(area);
        return new Effect(newShader);
    }
    
    /**
     * Creates a new Effect with the specified cell filter.
     * 
     * @param filter The cell filter to apply
     * @return A new Effect instance with the specified filter
     */
    public Effect withFilter(CellFilter filter) {
        Shader newShader = shader.copy();
        newShader.setCellFilter(filter);
        return new Effect(newShader);
    }
    
    /**
     * Creates a new Effect with the specified color space.
     * 
     * @param colorSpace The color space to use for interpolation
     * @return A new Effect instance with the specified color space
     */
    public Effect withColorSpace(TFxColorSpace colorSpace) {
        Shader newShader = shader.copy();
        newShader.setColorSpace(colorSpace);
        return new Effect(newShader);
    }
    
    /**
     * Creates a new Effect with the specified pattern.
     * 
     * @param pattern The pattern to use for spatial effects
     * @return A new Effect instance with the specified pattern
     */
    public Effect withPattern(dev.tamboui.tfx.pattern.Pattern pattern) {
        Shader newShader = shader.copy();
        newShader.setPattern(pattern);
        return new Effect(newShader);
    }
    
    /**
     * Processes the effect for the given duration.
     * <p>
     * This method:
     * 1. Updates the shader's timer with the given duration
     * 2. Executes the shader effect
     * 3. Returns any overflow duration
     * 
     * @param duration The duration to process the effect for
     * @param buffer The buffer where the effect will be applied
     * @param area The rectangular area within the buffer where the effect will be applied
     * @return The overflow duration if the effect is done, or null if still running
     */
    public TFxDuration process(TFxDuration duration, Buffer buffer, Rect area) {
        Rect effectArea = shader.area();
        if (effectArea != null) {
            area = effectArea;
        }
        return shader.process(duration, buffer, area);
    }
    
    /**
     * Returns true if the effect is done.
     */
    public boolean done() {
        return shader.done();
    }
    
    /**
     * Returns true if the effect is still running.
     */
    public boolean running() {
        return shader.running();
    }
    
    /**
     * Returns the name of the underlying shader.
     */
    public String name() {
        return shader.name();
    }
    
    /**
     * Returns the shader wrapped by this effect.
     */
    Shader shader() {
        return shader;
    }
}


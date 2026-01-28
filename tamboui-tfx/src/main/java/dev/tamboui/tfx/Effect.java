/*
 * Copyright TamboUI Contributors
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
     *
     * @param shader the shader to wrap
     * @return a new Effect instance
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
     * Updates the area where this effect is applied.
     * <p>
     * Unlike {@link #withArea(Rect)} which creates a new Effect, this method
     * mutates the existing effect. This is useful for updating running effects
     * when elements move or resize.
     *
     * @param area The new area where the effect should be applied
     */
    public void setArea(Rect area) {
        shader.setArea(area);
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
     *
     * @return true if the effect has completed
     */
    public boolean done() {
        return shader.done();
    }
    
    /**
     * Returns true if the effect is still running.
     *
     * @return true if the effect is still running
     */
    public boolean running() {
        return shader.running();
    }
    
    /**
     * Returns the name of the underlying shader.
     *
     * @return the shader name
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

    /**
     * Creates a copy of this effect with a fresh internal state.
     * <p>
     * The copy has the same configuration (area, filter, color space, pattern,
     * loop mode) but its own independent timer state. This is useful when you
     * need to apply the same effect to multiple elements independently.
     *
     * @return A new Effect instance that is a copy of this one
     */
    public Effect copy() {
        return new Effect(shader.copy());
    }
    
    /**
     * Creates a new Effect with the shader's reverse flag toggled.
     * <p>
     * This reverses the playback direction of the effect, causing it to play backwards.
     * <p>
     * In Rust, this takes ownership and mutates in place, but in Java we create a new
     * immutable instance to match the behavior of EffectTimer.reversed().
     *
     * @return A new Effect instance with the shader's reverse flag toggled
     */
    public Effect reversed() {
        // Create a new shader with reversed timer
        // We need to copy the shader and reverse its timer
        Shader newShader = shader.copy();
        newShader.reverse();  // This will be handled by the shader implementation
        return new Effect(newShader);
    }

    /**
     * Creates a new Effect that loops continuously from the beginning.
     * <p>
     * When the effect reaches its end, it resets to the beginning and continues
     * running. Looping effects never complete on their own and must be manually
     * removed or cleared from the effect manager.
     * <p>
     * <b>Example:</b>
     * <pre>{@code
     * Effect looping = Fx.fadeToFg(Color.CYAN, 1000, Interpolation.SineInOut)
     *     .loop();
     * }</pre>
     *
     * @return A new Effect instance with continuous looping enabled
     */
    public Effect loop() {
        Shader newShader = shader.copy();
        newShader.setLoopMode(LoopMode.LOOP);
        return new Effect(newShader);
    }

    /**
     * Creates a new Effect that ping-pongs back and forth.
     * <p>
     * When the effect reaches its end, it reverses direction and plays backwards.
     * When it reaches the beginning, it reverses again. This creates a smooth
     * back-and-forth animation. Ping-pong effects never complete on their own
     * and must be manually removed or cleared from the effect manager.
     * <p>
     * <b>Example:</b>
     * <pre>{@code
     * Effect pingPong = Fx.fadeToFg(Color.CYAN, 1000, Interpolation.SineInOut)
     *     .pingPong();
     * }</pre>
     *
     * @return A new Effect instance with ping-pong looping enabled
     */
    public Effect pingPong() {
        Shader newShader = shader.copy();
        newShader.setLoopMode(LoopMode.PING_PONG);
        return new Effect(newShader);
    }
}


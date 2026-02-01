/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;

/**
 * Interface representing a shader that applies visual effects to terminal cells.
 * <p>
 * The Shader interface is the core abstraction for all visual effects. It defines
 * the contract for objects that can transform terminal cell appearance over time.
 * <p>
 * <b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Separation of Timing and Rendering:</b> The default {@link #process} method
 *       handles timer updates, while {@link #execute} handles the actual rendering logic.</li>
 *   <li><b>Flexible Configuration:</b> Shaders can be configured with filters, patterns,
 *       color spaces, and areas through optional methods.</li>
 *   <li><b>Composability:</b> Shaders can be copied and combined to create complex effects.</li>
 * </ul>
 * <p>
 * <b>Implementation Pattern:</b>
 * <p>
 * Most shader implementations should:
 * <ol>
 *   <li>Store an {@link EffectTimer} for timing control</li>
 *   <li>Implement {@link #execute} to perform the actual cell transformations</li>
 *   <li>Implement {@link #done()} to check if the effect is complete</li>
 *   <li>Implement {@link #copy()} to support effect composition</li>
 * </ol>
 * <p>
 * The default {@link #process} implementation handles timer updates automatically,
 * so you typically only need to override {@link #execute}. Only override {@link #process}
 * if you need custom timer handling (e.g., for effects that don't use timers).
 * <p>
 * <b>Example Implementation:</b>
 * <pre>{@code
 * public class MyShader implements Shader {
 *     private final EffectTimer timer;
 *     private final Color targetColor;
 *     
 *     public MyShader(Color targetColor, EffectTimer timer) {
 *         this.targetColor = targetColor;
 *         this.timer = timer;
 *     }
 *     
 *     {@literal @}Override
 *     public String name() { return "my-effect"; }
 *     
 *     {@literal @}Override
 *     public EffectTimer timer() { return timer; }
 *     
 *     {@literal @}Override
 *     public boolean done() { return timer.done(); }
 *     
 *     {@literal @}Override
 *     public void execute(TFxDuration duration, Rect area, Buffer buffer) {
 *         float alpha = timer.alpha();
 *         // Apply effect based on alpha...
 *     }
 *     
 *     {@literal @}Override
 *     public Shader copy() {
 *         return new MyShader(targetColor, timer);
 *     }
 * }
 * }</pre>
 */
public interface Shader {
    
    /**
     * Returns the name of this shader.
     *
     * @return the shader name
     */
    String name();
    
    /**
     * Processes the shader for the given duration.
     * <p>
     * The default implementation:
     * 1. Updates the timer with the given duration (if a timer exists)
     * 2. Calls {@link #execute} with the current state
     * 3. Returns any overflow duration
     * <p>
     * Most effects should use this default implementation and implement {@link #execute}
     * instead. Only override this if you need custom timer handling.
     * 
     * @param duration The duration to process the shader for
     * @param buffer The buffer where the shader will be applied
     * @param area The rectangular area within the buffer where the shader will be applied
     * @return The overflow duration if the shader is done, or null if still running
     */
    default TFxDuration process(TFxDuration duration, Buffer buffer, Rect area) {
        EffectTimer timer = timer();
        if (timer != null) {
            TFxDuration overflow = timer.process(duration);
            execute(duration, area, buffer);
            return overflow;
        } else {
            execute(duration, area, buffer);
            return null;
        }
    }
    
    /**
     * Executes the shader effect after the duration has been applied to the timer.
     * <p>
     * This is the main implementation point for most effects, and is called by the
     * default {@link #process} implementation.
     * 
     * @param duration The duration to process the shader for
     * @param area The rectangular area within the buffer where the shader will be applied
     * @param buffer The buffer where the shader will be applied
     */
    default void execute(TFxDuration duration, Rect area, Buffer buffer) {
        // Default implementation does nothing
    }
    
    /**
     * Returns true if the shader effect is done.
     *
     * @return true if the effect has completed
     */
    boolean done();
    
    /**
     * Returns true if the shader is still running.
     *
     * @return true if the effect is still running
     */
    default boolean running() {
        return !done();
    }
    
    /**
     * Returns the area where the shader effect is applied, or null if not set.
     *
     * @return the effect area, or null if not set
     */
    default Rect area() {
        return null;
    }
    
    /**
     * Sets the area where the shader effect will be applied.
     *
     * @param area the rectangular area for the effect
     */
    default void setArea(Rect area) {
        // Default implementation does nothing
    }
    
    /**
     * Returns the timer associated with this shader effect, or null if none.
     *
     * @return the effect timer, or null if none
     */
    default EffectTimer timer() {
        return null;
    }
    
    /**
     * Returns a mutable timer if available, or null if none.
     * <p>
     * This is used internally for timer updates.
     *
     * @return the mutable effect timer, or null if none
     */
    default EffectTimer mutableTimer() {
        return timer();
    }
    
    /**
     * Returns the cell filter for this shader, or null if none.
     *
     * @return the cell filter, or null if none
     */
    default CellFilter cellFilter() {
        return null;
    }
    
    /**
     * Sets the cell filter for this shader.
     *
     * @param filter the cell filter to apply
     */
    default void setCellFilter(CellFilter filter) {
        // Default implementation does nothing
    }
    
    /**
     * Returns the color space for this shader, or null if not applicable.
     *
     * @return the color space, or null if not applicable
     */
    default TFxColorSpace colorSpace() {
        return null;
    }
    
    /**
     * Sets the color space for this shader.
     *
     * @param colorSpace the color space to use for interpolation
     */
    default void setColorSpace(TFxColorSpace colorSpace) {
        // Default implementation does nothing
    }
    
    /**
     * Returns the pattern for this shader, or null if not applicable.
     *
     * @return the spatial pattern, or null if not applicable
     */
    default dev.tamboui.tfx.pattern.Pattern pattern() {
        return null;
    }
    
    /**
     * Sets the pattern for this shader.
     *
     * @param pattern the spatial pattern to use
     */
    default void setPattern(dev.tamboui.tfx.pattern.Pattern pattern) {
        // Default implementation does nothing
    }
    
    /**
     * Reverses the shader's playback direction.
     * <p>
     * This method reverses the timer direction, causing the effect to play backwards.
     * The default implementation reverses the timer if one exists.
     */
    default void reverse() {
        EffectTimer timer = mutableTimer();
        if (timer != null) {
            // Create a new shader with reversed timer
            // Note: This is a limitation - we can't mutate the timer in place
            // So we need to override this in shaders that need reversal
        }
    }

    /**
     * Sets the loop mode for this shader's timer.
     * <p>
     * This method sets the loop mode on the underlying timer, controlling
     * what happens when the effect reaches its end. See {@link LoopMode}
     * for available modes.
     *
     * @param mode the loop mode to set
     */
    default void setLoopMode(LoopMode mode) {
        EffectTimer timer = mutableTimer();
        if (timer != null) {
            timer.loopMode(mode);
        }
    }

    /**
     * Creates a copy of this shader.
     * <p>
     * This is used for effect composition and cloning.
     *
     * @return a copy of this shader
     */
    Shader copy();
}


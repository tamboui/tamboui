/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

import dev.tamboui.style.Color;
import dev.tamboui.tfx.effects.FadeShader;

/**
 * Factory class providing static methods for creating common effects.
 * <p>
 * The Fx class serves as the primary entry point for creating effects. It provides
 * convenient factory methods that encapsulate the creation of shaders and their
 * wrapping in Effect instances.
 * <p>
 * <b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Convenience:</b> Simplifies effect creation with sensible defaults</li>
 *   <li><b>Consistency:</b> Provides a uniform API for all effect types</li>
 *   <li><b>Discoverability:</b> All effects are accessible through a single class</li>
 * </ul>
 * <p>
 * <b>Effect Categories:</b>
 * <ul>
 *   <li><b>Color Effects:</b> {@code fadeTo}, {@code fadeFrom}, {@code fadeToFg},
 *       {@code fadeFromFg}, {@code paint}, {@code paintFg}, {@code paintBg}</li>
 *   <li><b>Text Effects:</b> {@code dissolve}, {@code dissolveTo}, {@code coalesce},
 *       {@code coalesceFrom}</li>
 *   <li><b>Motion Effects:</b> {@code sweepIn}, {@code sweepOut}, {@code slideIn}, {@code slideOut}</li>
 *   <li><b>Composition:</b> {@code sequence}, {@code parallel}</li>
 * </ul>
 * <p>
 * <b>Usage Pattern:</b>
 * <pre>{@code
 * // Simple effect with duration and interpolation
 * Effect fade = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut);
 * 
 * // Effect with custom timer for more control
 * EffectTimer timer = EffectTimer.fromMs(2000, Interpolation.BounceOut);
 * Effect fadeCustom = Fx.fadeToFg(Color.CYAN, timer);
 * 
 * // Composed effects
 * Effect sequence = Fx.sequence(
 *     Fx.fadeFromFg(Color.BLACK, 500, Interpolation.QuadOut),
 *     Fx.dissolve(800, Interpolation.Linear)
 * );
 * }</pre>
 * <p>
 * All factory methods return {@link Effect} instances that can be further configured
 * using methods like {@link Effect#withFilter(CellFilter)},
 * {@link Effect#withColorSpace(TFxColorSpace)}, and {@link Effect#withPattern(dev.tamboui.tfx.pattern.Pattern)}.
 */
public final class Fx {
    
    private Fx() {
        // Utility class
    }
    
    /**
     * Creates a fade effect that transitions from one color to another.
     * 
     * @param fromColor The starting color
     * @param toColor The target color
     * @param timer The effect timer
     * @return A fade effect
     */
    public static Effect fadeTo(Color fromColor, Color toColor, EffectTimer timer) {
        return Effect.of(FadeShader.fadeTo(fromColor, toColor, timer));
    }
    
    /**
     * Creates a fade effect that transitions from one color to another.
     * 
     * @param fromColor The starting color
     * @param toColor The target color
     * @param durationMs Duration in milliseconds
     * @param interpolation The interpolation method
     * @return A fade effect
     */
    public static Effect fadeTo(Color fromColor, Color toColor, long durationMs, Interpolation interpolation) {
        return fadeTo(fromColor, toColor, EffectTimer.fromMs(durationMs, interpolation));
    }
    
    /**
     * Creates a fade effect that transitions the foreground color.
     * 
     * @param toColor The target foreground color
     * @param timer The effect timer
     * @return A fade effect
     */
    public static Effect fadeToFg(Color toColor, EffectTimer timer) {
        return fadeTo(Color.BLACK, toColor, timer);
    }
    
    /**
     * Creates a fade effect that transitions the foreground color.
     * 
     * @param toColor The target foreground color
     * @param durationMs Duration in milliseconds
     * @param interpolation The interpolation method
     * @return A fade effect
     */
    public static Effect fadeToFg(Color toColor, long durationMs, Interpolation interpolation) {
        return fadeToFg(toColor, EffectTimer.fromMs(durationMs, interpolation));
    }
    
    /**
     * Creates a fade effect that transitions from the specified color.
     * 
     * @param fromColor The starting color
     * @param toColor The target color
     * @param timer The effect timer
     * @return A fade effect
     */
    public static Effect fadeFrom(Color fromColor, Color toColor, EffectTimer timer) {
        return Effect.of(FadeShader.fadeTo(fromColor, toColor, timer.reversed()));
    }
    
    /**
     * Creates a fade effect that transitions from the specified color.
     * 
     * @param fromColor The starting color
     * @param toColor The target color
     * @param durationMs Duration in milliseconds
     * @param interpolation The interpolation method
     * @return A fade effect
     */
    public static Effect fadeFrom(Color fromColor, Color toColor, long durationMs, Interpolation interpolation) {
        return fadeFrom(fromColor, toColor, EffectTimer.fromMs(durationMs, interpolation));
    }
    
    /**
     * Creates a fade effect that transitions the foreground color from the specified color.
     * 
     * @param fromColor The starting foreground color
     * @param timer The effect timer
     * @return A fade effect
     */
    public static Effect fadeFromFg(Color fromColor, EffectTimer timer) {
        return fadeFrom(fromColor, Color.BLACK, timer);
    }
    
    /**
     * Creates a fade effect that transitions the foreground color from the specified color.
     * 
     * @param fromColor The starting foreground color
     * @param durationMs Duration in milliseconds
     * @param interpolation The interpolation method
     * @return A fade effect
     */
    public static Effect fadeFromFg(Color fromColor, long durationMs, Interpolation interpolation) {
        return fadeFromFg(fromColor, EffectTimer.fromMs(durationMs, interpolation));
    }
    
    /**
     * Creates a dissolve effect that dissolves text over time.
     * 
     * @param timer The effect timer
     * @return A dissolve effect
     */
    public static Effect dissolve(EffectTimer timer) {
        return Effect.of(dev.tamboui.tfx.effects.DissolveShader.dissolve(timer));
    }
    
    /**
     * Creates a dissolve effect that dissolves text over time.
     * 
     * @param durationMs Duration in milliseconds
     * @param interpolation The interpolation method
     * @return A dissolve effect
     */
    public static Effect dissolve(long durationMs, Interpolation interpolation) {
        return dissolve(EffectTimer.fromMs(durationMs, interpolation));
    }
    
    /**
     * Creates a dissolve effect that dissolves text to a specific style.
     * 
     * @param style The style to dissolve to
     * @param timer The effect timer
     * @return A dissolve effect
     */
    public static Effect dissolveTo(dev.tamboui.style.Style style, EffectTimer timer) {
        return Effect.of(dev.tamboui.tfx.effects.DissolveShader.dissolveTo(style, timer));
    }
    
    /**
     * Creates a dissolve effect that dissolves text to a specific style.
     * 
     * @param style The style to dissolve to
     * @param durationMs Duration in milliseconds
     * @param interpolation The interpolation method
     * @return A dissolve effect
     */
    public static Effect dissolveTo(dev.tamboui.style.Style style, long durationMs, Interpolation interpolation) {
        return dissolveTo(style, EffectTimer.fromMs(durationMs, interpolation));
    }
    
    /**
     * Creates a coalesce effect (reverse dissolve) that coalesces text from random positions.
     * 
     * @param timer The effect timer
     * @return A coalesce effect
     */
    public static Effect coalesce(EffectTimer timer) {
        return Effect.of(dev.tamboui.tfx.effects.DissolveShader.dissolve(timer.mirrored()));
    }
    
    /**
     * Creates a coalesce effect (reverse dissolve) that coalesces text from random positions.
     * 
     * @param durationMs Duration in milliseconds
     * @param interpolation The interpolation method
     * @return A coalesce effect
     */
    public static Effect coalesce(long durationMs, Interpolation interpolation) {
        return coalesce(EffectTimer.fromMs(durationMs, interpolation));
    }
    
    /**
     * Creates a coalesce effect that coalesces from a specific style.
     * 
     * @param style The style to coalesce from
     * @param timer The effect timer
     * @return A coalesce effect
     */
    public static Effect coalesceFrom(dev.tamboui.style.Style style, EffectTimer timer) {
        return Effect.of(dev.tamboui.tfx.effects.DissolveShader.dissolveTo(style, timer.mirrored()));
    }
    
    /**
     * Creates a coalesce effect that coalesces from a specific style.
     * 
     * @param style The style to coalesce from
     * @param durationMs Duration in milliseconds
     * @param interpolation The interpolation method
     * @return A coalesce effect
     */
    public static Effect coalesceFrom(dev.tamboui.style.Style style, long durationMs, Interpolation interpolation) {
        return coalesceFrom(style, EffectTimer.fromMs(durationMs, interpolation));
    }
    
    /**
     * Creates a sweep effect that sweeps in from a specified color.
     * 
     * @param direction The direction of the sweep
     * @param gradientLength The length of the gradient transition
     * @param randomness The maximum random offset (0 for uniform)
     * @param fadedColor The color to sweep from
     * @param timer The effect timer
     * @return A sweep effect
     */
    public static Effect sweepIn(Motion direction, int gradientLength, int randomness, 
                                 Color fadedColor, EffectTimer timer) {
        return Effect.of(dev.tamboui.tfx.effects.SweepShader.sweepIn(
            direction, gradientLength, randomness, fadedColor, timer));
    }
    
    /**
     * Creates a sweep effect that sweeps in from a specified color.
     * 
     * @param direction The direction of the sweep
     * @param gradientLength The length of the gradient transition
     * @param randomness The maximum random offset (0 for uniform)
     * @param fadedColor The color to sweep from
     * @param durationMs Duration in milliseconds
     * @param interpolation The interpolation method
     * @return A sweep effect
     */
    public static Effect sweepIn(Motion direction, int gradientLength, int randomness,
                                 Color fadedColor, long durationMs, Interpolation interpolation) {
        return sweepIn(direction, gradientLength, randomness, fadedColor,
            EffectTimer.fromMs(durationMs, interpolation));
    }
    
    /**
     * Creates a sweep effect that sweeps out to a specified color.
     * 
     * @param direction The direction of the sweep
     * @param gradientLength The length of the gradient transition
     * @param randomness The maximum random offset (0 for uniform)
     * @param fadedColor The color to sweep to
     * @param timer The effect timer
     * @return A sweep effect
     */
    public static Effect sweepOut(Motion direction, int gradientLength, int randomness,
                                  Color fadedColor, EffectTimer timer) {
        // Sweep out is sweep in with reversed direction and timer
        return sweepIn(direction.flipped(), gradientLength, randomness, fadedColor, timer.reversed());
    }
    
    /**
     * Creates a sweep effect that sweeps out to a specified color.
     * 
     * @param direction The direction of the sweep
     * @param gradientLength The length of the gradient transition
     * @param randomness The maximum random offset (0 for uniform)
     * @param fadedColor The color to sweep to
     * @param durationMs Duration in milliseconds
     * @param interpolation The interpolation method
     * @return A sweep effect
     */
    public static Effect sweepOut(Motion direction, int gradientLength, int randomness,
                                  Color fadedColor, long durationMs, Interpolation interpolation) {
        return sweepOut(direction, gradientLength, randomness, fadedColor,
            EffectTimer.fromMs(durationMs, interpolation));
    }
    
    /**
     * Creates a slide effect that slides in using block characters (shutter animation).
     * 
     * @param direction The direction of the slide
     * @param gradientLength The length of the gradient transition
     * @param randomness The maximum random offset (0 for uniform)
     * @param colorBehindCell The color behind the sliding cells
     * @param timer The effect timer
     * @return A slide effect
     */
    public static Effect slideIn(Motion direction, int gradientLength, int randomness,
                                 Color colorBehindCell, EffectTimer timer) {
        // Slide in is slide out with reversed direction and timer (matching Rust)
        return slideOut(direction.flipped(), gradientLength, randomness, colorBehindCell, timer).reversed();
    }
    
    /**
     * Creates a slide effect that slides in using block characters (shutter animation).
     * 
     * @param direction The direction of the slide
     * @param gradientLength The length of the gradient transition
     * @param randomness The maximum random offset (0 for uniform)
     * @param colorBehindCell The color behind the sliding cells
     * @param durationMs Duration in milliseconds
     * @param interpolation The interpolation method
     * @return A slide effect
     */
    public static Effect slideIn(Motion direction, int gradientLength, int randomness,
                                 Color colorBehindCell, long durationMs, Interpolation interpolation) {
        return slideIn(direction, gradientLength, randomness, colorBehindCell,
            EffectTimer.fromMs(durationMs, interpolation));
    }
    
    /**
     * Creates a slide effect that slides out using block characters (shutter animation).
     * 
     * @param direction The direction of the slide
     * @param gradientLength The length of the gradient transition
     * @param randomness The maximum random offset (0 for uniform)
     * @param colorBehindCell The color behind the sliding cells
     * @param timer The effect timer
     * @return A slide effect
     */
    public static Effect slideOut(Motion direction, int gradientLength, int randomness,
                                  Color colorBehindCell, EffectTimer timer) {
        // Apply timer mirroring if direction flips timer (matching Rust)
        EffectTimer finalTimer = direction.flipsTimer() ? timer.mirrored() : timer;
        return Effect.of(dev.tamboui.tfx.effects.SlideShader.slideOut(
            direction, gradientLength, randomness, colorBehindCell, finalTimer));
    }
    
    /**
     * Creates a slide effect that slides out using block characters (shutter animation).
     * 
     * @param direction The direction of the slide
     * @param gradientLength The length of the gradient transition
     * @param randomness The maximum random offset (0 for uniform)
     * @param colorBehindCell The color behind the sliding cells
     * @param durationMs Duration in milliseconds
     * @param interpolation The interpolation method
     * @return A slide effect
     */
    public static Effect slideOut(Motion direction, int gradientLength, int randomness,
                                  Color colorBehindCell, long durationMs, Interpolation interpolation) {
        return slideOut(direction, gradientLength, randomness, colorBehindCell,
            EffectTimer.fromMs(durationMs, interpolation));
    }
    
    /**
     * Creates a paint effect that applies foreground and/or background colors.
     * 
     * @param fg The foreground color to apply (null to skip)
     * @param bg The background color to apply (null to skip)
     * @param timer The effect timer
     * @return A paint effect
     */
    public static Effect paint(Color fg, Color bg, EffectTimer timer) {
        return Effect.of(dev.tamboui.tfx.effects.PaintShader.paint(fg, bg, timer));
    }
    
    /**
     * Creates a paint effect that applies foreground and/or background colors.
     * 
     * @param fg The foreground color to apply (null to skip)
     * @param bg The background color to apply (null to skip)
     * @param durationMs Duration in milliseconds
     * @param interpolation The interpolation method
     * @return A paint effect
     */
    public static Effect paint(Color fg, Color bg, long durationMs, Interpolation interpolation) {
        return paint(fg, bg, EffectTimer.fromMs(durationMs, interpolation));
    }
    
    /**
     * Creates a paint effect that applies only the foreground color.
     * 
     * @param fg The foreground color to apply
     * @param timer The effect timer
     * @return A paint effect
     */
    public static Effect paintFg(Color fg, EffectTimer timer) {
        return Effect.of(dev.tamboui.tfx.effects.PaintShader.paintFg(fg, timer));
    }
    
    /**
     * Creates a paint effect that applies only the foreground color.
     * 
     * @param fg The foreground color to apply
     * @param durationMs Duration in milliseconds
     * @param interpolation The interpolation method
     * @return A paint effect
     */
    public static Effect paintFg(Color fg, long durationMs, Interpolation interpolation) {
        return paintFg(fg, EffectTimer.fromMs(durationMs, interpolation));
    }
    
    /**
     * Creates a paint effect that applies only the background color.
     * 
     * @param bg The background color to apply
     * @param timer The effect timer
     * @return A paint effect
     */
    public static Effect paintBg(Color bg, EffectTimer timer) {
        return Effect.of(dev.tamboui.tfx.effects.PaintShader.paintBg(bg, timer));
    }
    
    /**
     * Creates a paint effect that applies only the background color.
     * 
     * @param bg The background color to apply
     * @param durationMs Duration in milliseconds
     * @param interpolation The interpolation method
     * @return A paint effect
     */
    public static Effect paintBg(Color bg, long durationMs, Interpolation interpolation) {
        return paintBg(bg, EffectTimer.fromMs(durationMs, interpolation));
    }
    
    /**
     * Runs the effects in sequence, one after the other.
     * Reports completion once the last effect has completed.
     * 
     * @param effects The effects to run sequentially
     * @return A sequential effect
     */
    public static Effect sequence(Effect... effects) {
        return Effect.of(dev.tamboui.tfx.effects.SequentialEffect.of(effects));
    }
    
    /**
     * Runs the effects in sequence, one after the other.
     * Reports completion once the last effect has completed.
     * 
     * @param effects The list of effects to run sequentially
     * @return A sequential effect
     */
    public static Effect sequence(java.util.List<Effect> effects) {
        return Effect.of(dev.tamboui.tfx.effects.SequentialEffect.of(effects));
    }
    
    /**
     * Runs the effects in parallel, all at the same time.
     * Reports completion once all effects have completed.
     * 
     * @param effects The effects to run in parallel
     * @return A parallel effect
     */
    public static Effect parallel(Effect... effects) {
        return Effect.of(dev.tamboui.tfx.effects.ParallelEffect.of(effects));
    }
    
    /**
     * Runs the effects in parallel, all at the same time.
     * Reports completion once all effects have completed.
     * 
     * @param effects The list of effects to run in parallel
     * @return A parallel effect
     */
    public static Effect parallel(java.util.List<Effect> effects) {
        return Effect.of(dev.tamboui.tfx.effects.ParallelEffect.of(effects));
    }
    
    /**
     * Creates an expand effect that grows outward from the center in both directions.
     * <p>
     * The expand effect fills the area with the specified style (including background color),
     * then reveals the content as it expands. It uses block characters for smooth transitions.
     * 
     * @param direction The direction of expansion (horizontal or vertical)
     * @param style The style to apply during expansion (foreground and background colors)
     * @param timer The effect timer
     * @return An expand effect
     */
    public static Effect expand(ExpandDirection direction, dev.tamboui.style.Style style, EffectTimer timer) {
        return Effect.of(new dev.tamboui.tfx.effects.ExpandShader(direction, style, timer));
    }
    
    /**
     * Creates an expand effect that grows outward from the center in both directions.
     * 
     * @param direction The direction of expansion (horizontal or vertical)
     * @param style The style to apply during expansion (foreground and background colors)
     * @param durationMs Duration in milliseconds
     * @param interpolation The interpolation method
     * @return An expand effect
     */
    public static Effect expand(ExpandDirection direction, dev.tamboui.style.Style style, long durationMs, Interpolation interpolation) {
        return expand(direction, style, EffectTimer.fromMs(durationMs, interpolation));
    }
}

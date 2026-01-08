/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

import java.util.Objects;

/**
 * Manages the timing and interpolation of effects.
 * <p>
 * The EffectTimer is responsible for:
 * <ul>
 *   <li><b>Duration Tracking:</b> Tracking total duration and remaining time</li>
 *   <li><b>Progress Calculation:</b> Computing alpha values (0.0 to 1.0) based on elapsed time</li>
 *   <li><b>Easing Application:</b> Applying interpolation functions for smooth animations</li>
 *   <li><b>Direction Control:</b> Supporting forward and reverse playback</li>
 * </ul>
 * <p>
 * <b>Design Philosophy:</b>
 * <p>
 * EffectTimer separates timing concerns from rendering logic. Effects use the timer's
 * {@link #alpha()} method to get a normalized progress value (0.0 to 1.0) that has
 * been transformed by the interpolation function. This allows effects to focus on
 * "what to render" rather than "when to render it."
 * <p>
 * <b>Key Concepts:</b>
 * <ul>
 *   <li><b>Alpha Value:</b> A normalized progress value (0.0 = start, 1.0 = end)
 *       that has been transformed by the interpolation function.</li>
 *   <li><b>Interpolation:</b> An easing function that transforms linear progress
 *       into smooth animation curves (e.g., ease-in, ease-out, bounce).</li>
 *   <li><b>Overflow:</b> When processing exceeds the timer's duration, the excess
 *       time is returned as overflow for use by subsequent effects in sequences.</li>
 * </ul>
 * <p>
 * <b>Usage Pattern:</b>
 * <pre>{@code
 * EffectTimer timer = EffectTimer.fromMs(2000, Interpolation.SineInOut);
 * 
 * // In render loop
 * TFxDuration delta = TFxDuration.fromMillis(frameTimeMs);
 * TFxDuration overflow = timer.process(delta);
 * 
 * if (timer.done()) {
 *     // Effect complete
 * } else {
 *     float alpha = timer.alpha(); // Use alpha to drive effect
 * }
 * }</pre>
 * <p>
 * <b>Reversing Effects:</b>
 * <p>
 * Use {@link #reversed()} to play an effect backwards, or {@link #mirrored()} to
 * reverse while preserving the visual curve shape (useful for ping-pong effects).
 */
public final class EffectTimer {
    
    private TFxDuration remaining;
    private final TFxDuration total;
    private Interpolation interpolation;
    private boolean reverse;
    
    /**
     * Creates a new EffectTimer with the specified duration in milliseconds and interpolation.
     */
    public static EffectTimer fromMs(long milliseconds, Interpolation interpolation) {
        return new EffectTimer(TFxDuration.fromMillis(milliseconds), interpolation);
    }
    
    /**
     * Creates a new EffectTimer with the specified duration and interpolation.
     */
    public static EffectTimer of(TFxDuration duration, Interpolation interpolation) {
        return new EffectTimer(duration, interpolation);
    }
    
    /**
     * Creates a new EffectTimer with the specified duration and Linear interpolation.
     */
    public static EffectTimer of(TFxDuration duration) {
        return new EffectTimer(duration, Interpolation.Linear);
    }
    
    private EffectTimer(TFxDuration duration, Interpolation interpolation) {
        this.remaining = duration;
        this.total = duration;
        this.interpolation = Objects.requireNonNull(interpolation);
        this.reverse = false;
    }
    
    /**
     * Returns a new timer with reversed direction.
     */
    public EffectTimer reversed() {
        EffectTimer timer = new EffectTimer(total, interpolation);
        timer.remaining = remaining;
        timer.reverse = !this.reverse;
        return timer;
    }
    
    /**
     * Returns true if the timer is reversed.
     */
    public boolean isReversed() {
        return reverse;
    }
    
    /**
     * Returns a mirrored timer that runs in reverse direction with flipped interpolation.
     * <p>
     * This preserves the visual curve shape when used with effects that reverse at
     * construction time. Unlike reversed(), which flips both direction and interpolation
     * type, mirrored() flips the interpolation to compensate for the reversed direction.
     */
    public EffectTimer mirrored() {
        EffectTimer timer = new EffectTimer(total, interpolation.flipped());
        timer.remaining = remaining;
        timer.reverse = !this.reverse;
        return timer;
    }
    
    /**
     * Returns true if the timer has started (i.e., remaining != total).
     */
    public boolean started() {
        return !total.equals(remaining);
    }
    
    /**
     * Resets the timer to its initial duration.
     */
    public void reset() {
        this.remaining = total;
    }
    
    /**
     * Computes the current alpha value based on the elapsed time and interpolation method.
     * 
     * @return The current alpha value (0.0 to 1.0)
     */
    public float alpha() {
        float totalMs = total.asMillis();
        if (totalMs == 0.0f) {
            return reverse ? 0.0f : 1.0f;
        }
        
        float remainingMs = remaining.asMillis();
        float invAlpha = remainingMs / totalMs;
        
        float a = reverse ? invAlpha : 1.0f - invAlpha;
        return interpolation.alpha(a);
    }
    
    /**
     * Returns the remaining duration.
     */
    public TFxDuration remaining() {
        return remaining;
    }
    
    /**
     * Returns the total duration.
     */
    public TFxDuration duration() {
        return total;
    }
    
    /**
     * Processes the timer by reducing the remaining duration by the specified amount.
     * 
     * @param duration The amount of time to process
     * @return The overflow duration if the timer has completed, or null if still running
     */
    public TFxDuration process(TFxDuration duration) {
        if (remaining.asMillis() >= duration.asMillis()) {
            remaining = remaining.sub(duration);
            return null;
        } else {
            TFxDuration overflow = duration.sub(remaining);
            remaining = TFxDuration.ZERO;
            return overflow;
        }
    }
    
    /**
     * Returns true if the timer has completed.
     */
    public boolean done() {
        return remaining.isZero();
    }
    
    /**
     * Returns the interpolation method.
     */
    public Interpolation interpolation() {
        return interpolation;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EffectTimer)) return false;
        EffectTimer that = (EffectTimer) o;
        return reverse == that.reverse &&
               Objects.equals(remaining, that.remaining) &&
               Objects.equals(total, that.total) &&
               interpolation == that.interpolation;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(remaining, total, interpolation, reverse);
    }
    
    @Override
    public String toString() {
        return "EffectTimer{remaining=" + remaining + ", total=" + total + 
               ", interpolation=" + interpolation + ", reverse=" + reverse + "}";
    }
}


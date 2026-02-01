/*
 * Copyright TamboUI Contributors
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
    private LoopMode loopMode;
    
    /**
     * Creates a new EffectTimer with the specified duration in milliseconds and interpolation.
     *
     * @param milliseconds the duration in milliseconds
     * @param interpolation the interpolation function to apply
     * @return a new EffectTimer instance
     */
    public static EffectTimer fromMs(long milliseconds, Interpolation interpolation) {
        return new EffectTimer(TFxDuration.fromMillis(milliseconds), interpolation);
    }
    
    /**
     * Creates a new EffectTimer with the specified duration and interpolation.
     *
     * @param duration the total duration
     * @param interpolation the interpolation function to apply
     * @return a new EffectTimer instance
     */
    public static EffectTimer of(TFxDuration duration, Interpolation interpolation) {
        return new EffectTimer(duration, interpolation);
    }
    
    /**
     * Creates a new EffectTimer with the specified duration and Linear interpolation.
     *
     * @param duration the total duration
     * @return a new EffectTimer instance with Linear interpolation
     */
    public static EffectTimer of(TFxDuration duration) {
        return new EffectTimer(duration, Interpolation.Linear);
    }
    
    private EffectTimer(TFxDuration duration, Interpolation interpolation) {
        this.remaining = duration;
        this.total = duration;
        this.interpolation = Objects.requireNonNull(interpolation);
        this.reverse = false;
        this.loopMode = LoopMode.ONCE;
    }

    /**
     * Sets the loop mode for this timer.
     * <p>
     * Loop mode controls what happens when the timer completes:
     * <ul>
     *   <li>{@link LoopMode#ONCE}: Timer completes and stays done (default)</li>
     *   <li>{@link LoopMode#LOOP}: Timer resets to start and continues</li>
     *   <li>{@link LoopMode#PING_PONG}: Timer reverses direction and continues</li>
     * </ul>
     *
     * @param mode the loop mode
     * @return this timer for chaining
     */
    public EffectTimer loopMode(LoopMode mode) {
        this.loopMode = Objects.requireNonNull(mode);
        return this;
    }

    /**
     * Returns the current loop mode.
     *
     * @return the loop mode
     */
    public LoopMode loopMode() {
        return loopMode;
    }
    
    /**
     * Returns a new timer with reversed direction.
     *
     * @return a new timer with the reverse flag toggled
     */
    public EffectTimer reversed() {
        EffectTimer timer = new EffectTimer(total, interpolation);
        timer.remaining = remaining;
        timer.reverse = !this.reverse;
        timer.loopMode = this.loopMode;
        return timer;
    }
    
    /**
     * Returns true if the timer is reversed.
     *
     * @return true if the timer is playing in reverse
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
     *
     * @return a new timer with reversed direction and flipped interpolation
     */
    public EffectTimer mirrored() {
        EffectTimer timer = new EffectTimer(total, interpolation.flipped());
        timer.remaining = remaining;
        timer.reverse = !this.reverse;
        timer.loopMode = this.loopMode;
        return timer;
    }
    
    /**
     * Returns true if the timer has started (i.e., remaining != total).
     *
     * @return true if the timer has started processing
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
     *
     * @return the remaining duration
     */
    public TFxDuration remaining() {
        return remaining;
    }
    
    /**
     * Returns the total duration.
     *
     * @return the total duration
     */
    public TFxDuration duration() {
        return total;
    }
    
    /**
     * Processes the timer by reducing the remaining duration by the specified amount.
     * <p>
     * For looping timers, this method handles resetting or reversing the timer
     * when it completes:
     * <ul>
     *   <li>{@link LoopMode#ONCE}: Returns overflow when complete</li>
     *   <li>{@link LoopMode#LOOP}: Resets to start, returns null</li>
     *   <li>{@link LoopMode#PING_PONG}: Reverses direction, returns null</li>
     * </ul>
     *
     * @param duration The amount of time to process
     * @return The overflow duration if the timer has completed (ONCE mode only), or null if still running
     */
    public TFxDuration process(TFxDuration duration) {
        if (remaining.asMillis() >= duration.asMillis()) {
            remaining = remaining.sub(duration);
            return null;
        } else {
            TFxDuration overflow = duration.sub(remaining);

            switch (loopMode) {
                case LOOP:
                    // Reset to start and continue
                    remaining = total;
                    // Process any overflow time recursively (for very fast processing)
                    if (overflow.asMillis() > 0 && total.asMillis() > 0) {
                        return process(overflow);
                    }
                    return null;

                case PING_PONG:
                    // Reverse direction and reset
                    reverse = !reverse;
                    remaining = total;
                    // Process any overflow time recursively
                    if (overflow.asMillis() > 0 && total.asMillis() > 0) {
                        return process(overflow);
                    }
                    return null;

                case ONCE:
                default:
                    remaining = TFxDuration.ZERO;
                    return overflow;
            }
        }
    }

    /**
     * Returns true if the timer has completed.
     * <p>
     * For looping timers ({@link LoopMode#LOOP} and {@link LoopMode#PING_PONG}),
     * this method always returns false since looping timers never complete on their own.
     *
     * @return true if the timer has completed
     */
    public boolean done() {
        if (loopMode != LoopMode.ONCE) {
            return false;
        }
        return remaining.isZero();
    }
    
    /**
     * Returns the interpolation method.
     *
     * @return the interpolation function
     */
    public Interpolation interpolation() {
        return interpolation;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EffectTimer)) {
            return false;
        }
        EffectTimer that = (EffectTimer) o;
        return reverse == that.reverse &&
               Objects.equals(remaining, that.remaining) &&
               Objects.equals(total, that.total) &&
               interpolation == that.interpolation &&
               loopMode == that.loopMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(remaining, total, interpolation, reverse, loopMode);
    }

    @Override
    public String toString() {
        return "EffectTimer{remaining=" + remaining + ", total=" + total +
               ", interpolation=" + interpolation + ", reverse=" + reverse +
               ", loopMode=" + loopMode + "}";
    }
}


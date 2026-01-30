/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.wavetext;

/**
 * State for the {@link WaveText} widget, tracking animation progress.
 * <p>
 * The tick value controls where the bright peak appears in the wave effect.
 * Call {@link #tick()} each frame to advance the animation.
 *
 * <pre>{@code
 * WaveTextState state = new WaveTextState();
 *
 * // In your render loop:
 * state.tick(); // Advance animation
 * frame.renderStatefulWidget(waveText, area, state);
 * }</pre>
 */
public final class WaveTextState {

    private long tick;

    /**
     * Creates a new state with tick starting at 0.
     */
    public WaveTextState() {
        this.tick = 0;
    }

    /**
     * Creates a new state with the given initial tick.
     *
     * @param initialTick
     *            the initial tick value
     */
    public WaveTextState(long initialTick) {
        this.tick = initialTick;
    }

    /**
     * Returns the current tick value.
     *
     * @return the tick
     */
    public long tick() {
        return tick;
    }

    /**
     * Advances the tick by 1 and returns the new value.
     * <p>
     * Call this once per frame to animate the wave.
     *
     * @return the new tick value
     */
    public long advance() {
        return ++tick;
    }

    /**
     * Advances the tick by the given amount.
     *
     * @param amount
     *            the amount to advance
     */
    public void advance(long amount) {
        tick += amount;
    }

    /**
     * Sets the tick to a specific value.
     *
     * @param tick
     *            the new tick value
     */
    public void setTick(long tick) {
        this.tick = tick;
    }

    /**
     * Resets the tick to 0.
     */
    public void reset() {
        this.tick = 0;
    }
}

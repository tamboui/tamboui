/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

/**
 * Defines how an effect should behave when it reaches its end.
 * <p>
 * Loop modes control whether effects run once and complete, restart from the
 * beginning, or reverse direction for continuous animations.
 * <p>
 * <b>Usage:</b>
 * 
 * <pre>{@code
 * // Run once and complete (default)
 * Effect fade = Fx.fadeToFg(Color.CYAN, 1000, Interpolation.SineInOut);
 *
 * // Loop continuously from beginning
 * Effect looping = Fx.fadeToFg(Color.CYAN, 1000, Interpolation.SineInOut).loop();
 *
 * // Ping-pong back and forth
 * Effect pingPong = Fx.fadeToFg(Color.CYAN, 1000, Interpolation.SineInOut).pingPong();
 * }</pre>
 */
public enum LoopMode {

    /**
     * Run once and complete (default behavior).
     * <p>
     * The effect runs from start to finish and then marks itself as done.
     */
    ONCE,

    /**
     * Restart from beginning when done.
     * <p>
     * When the effect reaches its end, it resets to the beginning and continues
     * running. Looping effects never complete on their own.
     */
    LOOP,

    /**
     * Reverse direction when reaching end.
     * <p>
     * When the effect reaches its end, it reverses and plays backwards. When it
     * reaches the beginning, it reverses again. This creates a smooth
     * back-and-forth animation. Ping-pong effects never complete on their own.
     */
    PING_PONG
}

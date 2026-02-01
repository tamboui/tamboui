/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

/**
 * Specifies the direction of movement for directional visual effects.
 * <p>
 * Motion defines the four cardinal directions used by sweep and slide effects
 * to control the direction of animation progression across the terminal area.
 * <p>
 * <b>Design Philosophy:</b>
 * <p>
 * Motion provides a type-safe way to specify animation direction, avoiding magic
 * strings or integers. It also handles the complexity of timer reversal for
 * certain directions to maintain consistent visual behavior.
 * <p>
 * <b>Directions:</b>
 * <ul>
 *   <li><b>LEFT_TO_RIGHT:</b> Animation progresses from left edge to right edge</li>
 *   <li><b>RIGHT_TO_LEFT:</b> Animation progresses from right edge to left edge</li>
 *   <li><b>UP_TO_DOWN:</b> Animation progresses from top edge to bottom edge</li>
 *   <li><b>DOWN_TO_UP:</b> Animation progresses from bottom edge to top edge</li>
 * </ul>
 * <p>
 * <b>Timer Reversal:</b>
 * <p>
 * Some directions ({@code RIGHT_TO_LEFT} and {@code DOWN_TO_UP}) require the
 * effect timer to be reversed to maintain consistent animation behavior. The
 * {@link #flipsTimer()} method indicates when this is necessary.
 * <p>
 * <b>Usage Pattern:</b>
 * <pre>{@code
 * // Sweep from left to right
 * Effect sweep = Fx.sweepIn(Motion.LEFT_TO_RIGHT, 10, 0, Color.BLUE, 
 *     2000, Interpolation.QuadOut);
 * 
 * // Get opposite direction
 * Motion opposite = Motion.LEFT_TO_RIGHT.flipped(); // RIGHT_TO_LEFT
 * }</pre>
 */
public enum Motion {
    /**
     * Movement from left to right
     */
    LEFT_TO_RIGHT,
    
    /**
     * Movement from right to left
     */
    RIGHT_TO_LEFT,
    
    /**
     * Movement from top to bottom
     */
    UP_TO_DOWN,
    
    /**
     * Movement from bottom to top
     */
    DOWN_TO_UP;
    
    /**
     * Returns the opposite direction of the current motion.
     *
     * @return the opposite motion direction
     */
    public Motion flipped() {
        switch (this) {
            case LEFT_TO_RIGHT:
                return RIGHT_TO_LEFT;
            case RIGHT_TO_LEFT:
                return LEFT_TO_RIGHT;
            case UP_TO_DOWN:
                return DOWN_TO_UP;
            case DOWN_TO_UP:
                return UP_TO_DOWN;
            default:
                return this;
        }
    }
    
    /**
     * Determines whether this motion direction requires timer reversal.
     * <p>
     * Some motions (RIGHT_TO_LEFT and DOWN_TO_UP) require the effect timer to be reversed
     * to maintain consistent animation behavior.
     *
     * @return true if this motion direction requires timer reversal
     */
    public boolean flipsTimer() {
        return this == RIGHT_TO_LEFT || this == DOWN_TO_UP;
    }
}


/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

import dev.tamboui.layout.Position;

/**
 * Generates random variances for directional effects.
 * <p>
 * DirectionalVariance is used by sweep and slide effects to add randomness to
 * the animation, creating irregular patterns. It generates random offsets based
 * on the effect's direction.
 */
public final class DirectionalVariance {
    
    private final SimpleRng rng;
    private final Motion direction;
    private final int max;
    
    /**
     * Creates a new DirectionalVariance instance with a provided RNG.
     * 
     * @param rng The RNG to use for generating variances
     * @param direction The direction of the effect
     * @param max The maximum variance that can be generated
     * @return a new DirectionalVariance instance
     */
    public static DirectionalVariance withRng(SimpleRng rng, Motion direction, int max) {
        return new DirectionalVariance(rng, direction, max);
    }
    
    private DirectionalVariance(SimpleRng rng, Motion direction, int max) {
        this.rng = rng;
        this.direction = direction;
        this.max = max;
    }
    
    /**
     * Generates the next variance value.
     * <p>
     * Returns a Position offset representing the (x, y) variance to be applied.
     * The generated variance is always within the range [-max, max] for the relevant
     * axis, and 0 for the other axis.
     * 
     * @return A Position representing the (x, y) variance offset
     */
    public Position next() {
        if (max == 0) {
            return new Position(0, 0);
        }
        
        int variance = rng.genRange(0, max);
        switch (direction) {
            case LEFT_TO_RIGHT:
                return new Position(variance, 0);
            case RIGHT_TO_LEFT:
                return new Position(-variance, 0);
            case UP_TO_DOWN:
                return new Position(0, variance);
            case DOWN_TO_UP:
                return new Position(0, -variance);
            default:
                return new Position(0, 0);
        }
    }
    
    /**
     * Generates the next variance value as separate x and y components.
     * 
     * @return A tuple (x, y) representing the variance offset
     */
    public int[] nextXY() {
        Position pos = next();
        return new int[]{pos.x(), pos.y()};
    }
}


/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.pattern;

/**
 * Encapsulates alpha progression with smooth transitions.
 * <p>
 * This class handles the common pattern of mapping global animation progress to
 * position-specific alpha values with smooth gradient transitions.
 */
public final class TransitionProgress {
    
    private final float transitionWidth;
    
    /**
     * Creates a new transition progress handler with the specified transition width.
     * 
     * @param width Width of the gradient transition zone in terminal cells (minimum 0.1)
     */
    public TransitionProgress(float width) {
        this.transitionWidth = java.lang.Math.max(0.1f, width);
    }
    
    /**
     * Maps spatial patterns where positions vary along a continuous dimension.
     * <p>
     * Used for patterns like diagonal and radial where positions have a natural
     * progression along some spatial dimension with a defined range.
     * 
     * @param globalAlpha Global animation progress (0.0-1.0)
     * @param position Position along the pattern dimension
     * @param maxRange Maximum value for the position dimension
     * @return The mapped alpha value (0.0-1.0)
     */
    public float mapSpatial(float globalAlpha, float position, float maxRange) {
        // Scale global_alpha to include transition zone
        float scaledAlpha = globalAlpha * (maxRange + 2.0f * transitionWidth) - transitionWidth;
        
        if (position <= scaledAlpha) {
            // Fully active (positions before threshold)
            return 1.0f;
        } else if (position <= scaledAlpha + transitionWidth) {
            // Transition zone with correct falloff
            float distanceIntoTransition = position - scaledAlpha;
            float progress = 1.0f - (distanceIntoTransition / transitionWidth);
            return java.lang.Math.max(0.0f, java.lang.Math.min(1.0f, progress));
        } else {
            // Inactive
            return 0.0f;
        }
    }
    
    /**
     * Maps radial patterns where smaller distances (closer to center) should be more active.
     * 
     * @param globalAlpha Global animation progress (0.0-1.0)
     * @param distance Distance from center point
     * @param maxRange Maximum distance value
     * @return The mapped alpha value (0.0-1.0)
     */
    public float mapRadial(float globalAlpha, float distance, float maxRange) {
        float threshold = (globalAlpha * (maxRange + 2.0f * transitionWidth)) - transitionWidth;
        
        // For radial: we want 1.0 when distance <= threshold, 0.0 when distance > threshold + transition
        if (distance <= threshold) {
            return 1.0f;
        } else if (distance <= threshold + transitionWidth) {
            // In transition zone - linear falloff
            float distanceIntoTransition = distance - threshold;
            return 1.0f - java.lang.Math.max(0.0f, java.lang.Math.min(1.0f, distanceIntoTransition / transitionWidth));
        } else {
            return 0.0f;
        }
    }
    
    /**
     * Maps discrete threshold patterns where cells have distinct activation thresholds.
     * <p>
     * Used for patterns like checkerboard where cells belong to discrete categories
     * with specific threshold values.
     * 
     * @param globalAlpha Global animation progress (0.0-1.0)
     * @param cellThreshold The discrete threshold for this cell (e.g., 0.0 for white, 0.5 for black)
     * @return The mapped alpha value (0.0-1.0)
     */
    public float mapThreshold(float globalAlpha, float cellThreshold) {
        // Scale the alpha range to include transition zones
        float scaledAlpha = globalAlpha * (1.0f + transitionWidth) - (transitionWidth / 2.0f);
        
        if (scaledAlpha >= cellThreshold + transitionWidth) {
            return 1.0f;
        } else if (scaledAlpha >= cellThreshold) {
            // In transition zone
            float distanceIntoTransition = scaledAlpha - cellThreshold;
            return java.lang.Math.max(0.0f, java.lang.Math.min(1.0f, distanceIntoTransition / transitionWidth));
        } else {
            return 0.0f;
        }
    }
    
    /**
     * Returns the transition width.
     *
     * @return the transition width in terminal cells
     */
    public float transitionWidth() {
        return transitionWidth;
    }
}



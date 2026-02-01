/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.pattern;

import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;

/**
 * Diagonal pattern that sweeps diagonally across the area.
 */
public final class DiagonalPattern implements Pattern {
    
    /**
     * Direction variants for diagonal sweep patterns.
     */
    public enum DiagonalDirection {
        /** Sweeps diagonally from top-left corner to bottom-right corner */
        TOP_LEFT_TO_BOTTOM_RIGHT,
        /** Sweeps diagonally from top-right corner to bottom-left corner */
        TOP_RIGHT_TO_BOTTOM_LEFT,
        /** Sweeps diagonally from bottom-left corner to top-right corner */
        BOTTOM_LEFT_TO_TOP_RIGHT,
        /** Sweeps diagonally from bottom-right corner to top-left corner */
        BOTTOM_RIGHT_TO_TOP_LEFT
    }
    
    private final DiagonalDirection direction;
    private final TransitionProgress transition;
    
    /**
     * Creates a diagonal pattern from top-left to bottom-right.
     *
     * @return a new diagonal pattern sweeping from top-left to bottom-right
     */
    public static DiagonalPattern topLeftToBottomRight() {
        return new DiagonalPattern(DiagonalDirection.TOP_LEFT_TO_BOTTOM_RIGHT, 2.0f);
    }
    
    /**
     * Creates a diagonal pattern from top-right to bottom-left.
     *
     * @return a new diagonal pattern sweeping from top-right to bottom-left
     */
    public static DiagonalPattern topRightToBottomLeft() {
        return new DiagonalPattern(DiagonalDirection.TOP_RIGHT_TO_BOTTOM_LEFT, 2.0f);
    }
    
    /**
     * Creates a diagonal pattern from bottom-left to top-right.
     *
     * @return a new diagonal pattern sweeping from bottom-left to top-right
     */
    public static DiagonalPattern bottomLeftToTopRight() {
        return new DiagonalPattern(DiagonalDirection.BOTTOM_LEFT_TO_TOP_RIGHT, 2.0f);
    }
    
    /**
     * Creates a diagonal pattern from bottom-right to top-left.
     *
     * @return a new diagonal pattern sweeping from bottom-right to top-left
     */
    public static DiagonalPattern bottomRightToTopLeft() {
        return new DiagonalPattern(DiagonalDirection.BOTTOM_RIGHT_TO_TOP_LEFT, 2.0f);
    }
    
    /**
     * Creates a diagonal pattern with specified direction and transition width.
     *
     * @param direction the diagonal sweep direction
     * @param transitionWidth the width of the transition zone
     * @return a new diagonal pattern with the specified configuration
     */
    public static DiagonalPattern newPattern(DiagonalDirection direction, float transitionWidth) {
        return new DiagonalPattern(direction, transitionWidth);
    }
    
    private DiagonalPattern(DiagonalDirection direction, float transitionWidth) {
        this.direction = direction;
        this.transition = new TransitionProgress(transitionWidth);
    }
    
    /**
     * Creates a new pattern with a different transition width.
     *
     * @param width the new transition width
     * @return a new diagonal pattern with the specified transition width
     */
    public DiagonalPattern withTransitionWidth(float width) {
        return new DiagonalPattern(direction, width);
    }
    
    @Override
    public float mapAlpha(float globalAlpha, Position position, Rect area) {
        // Normalize position to 0.0-1.0 range
        float normX = (position.x() - area.x()) / (float) area.width();
        float normY = (position.y() - area.y()) / (float) area.height();
        
        // Calculate position along diagonal (0.0 to 1.0)
        float positionValue;
        float maxRange;
        
        switch (direction) {
            case TOP_LEFT_TO_BOTTOM_RIGHT:
                // Diagonal from (0,0) to (1,1): x + y
                positionValue = normX + normY;
                maxRange = 2.0f; // Maximum is 2.0 (1.0 + 1.0)
                break;
            case TOP_RIGHT_TO_BOTTOM_LEFT:
                // Diagonal from (1,0) to (0,1): (1-x) + y
                positionValue = (1.0f - normX) + normY;
                maxRange = 2.0f;
                break;
            case BOTTOM_LEFT_TO_TOP_RIGHT:
                // Diagonal from (0,1) to (1,0): x + (1-y)
                positionValue = normX + (1.0f - normY);
                maxRange = 2.0f;
                break;
            case BOTTOM_RIGHT_TO_TOP_LEFT:
                // Diagonal from (1,1) to (0,0): (1-x) + (1-y)
                positionValue = (1.0f - normX) + (1.0f - normY);
                maxRange = 2.0f;
                break;
            default:
                return globalAlpha;
        }
        
        return transition.mapSpatial(globalAlpha, positionValue, maxRange);
    }
    
    @Override
    public String name() {
        return "diagonal_" + direction.name().toLowerCase();
    }
}



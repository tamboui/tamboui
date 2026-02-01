/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;

/**
 * Calculates alpha values for positions based on a sliding window gradient.
 * <p>
 * SlidingWindowAlpha is a helper class used by directional sweep effects (like
 * {@link dev.tamboui.tfx.effects.SweepShader}) to calculate position-specific alpha
 * values based on a "sliding window" that moves across the terminal area.
 * <p>
 * <b>Design Philosophy:</b>
 * <p>
 * The sliding window creates a gradient zone that moves across the area as the
 * animation progresses. Cells before the window are fully inactive (alpha = 0.0),
 * cells within the window have a gradient from 0.0 to 1.0, and cells after the
 * window are fully active (alpha = 1.0). This creates smooth directional transitions.
 * <p>
 * <b>Key Concepts:</b>
 * <ul>
 *   <li><b>Sliding Window:</b> A gradient zone that moves across the area based on
 *       animation progress</li>
 *   <li><b>Gradient Length:</b> The width of the transition zone in terminal cells</li>
 *   <li><b>Progress:</b> The global animation progress (0.0 to 1.0) that determines
 *       window position</li>
 * </ul>
 * <p>
 * <b>Usage:</b>
 * <p>
 * This class is primarily used internally by {@link dev.tamboui.tfx.effects.SweepShader}
 * to implement sweep effects. It's not typically used directly by application code.
 * <p>
 * <b>Note:</b> For pattern-based spatial effects (like {@link dev.tamboui.tfx.pattern.SweepPattern}),
 * use {@link dev.tamboui.tfx.pattern.TransitionProgress} instead, which provides a
 * different approach to spatial mapping that works better with the pattern system.
 */
public final class SlidingWindowAlpha {
    
    private final Motion direction;
    private final Rect area;
    private final float progress;
    private final float gradientLength;
    private final Gradient gradient;
    private final float alphaPerCell;
    
    private SlidingWindowAlpha(Motion direction, Rect area, float progress, float gradientLength) {
        this.direction = direction;
        this.area = area;
        this.progress = progress;
        this.gradientLength = gradientLength;
        
        // Calculate gradient based on direction
        if (direction == Motion.LEFT_TO_RIGHT || direction == Motion.RIGHT_TO_LEFT) {
            this.gradient = calculateGradient(progress, area.x(), area.width(), gradientLength);
        } else {
            this.gradient = calculateGradient(progress, area.y(), area.height(), gradientLength);
        }
        
        float gradientRange = gradient.end - gradient.start;
        this.alphaPerCell = gradientRange > 0 ? 1.0f / gradientRange : 1.0f;
    }
    
    /**
     * Creates a new sliding window alpha calculator.
     *
     * @param direction the direction of the sliding window
     * @param area the rectangular area to apply the effect to
     * @param progress the global animation progress (0.0 to 1.0)
     * @param gradientLength the width of the gradient transition zone
     * @return a new sliding window alpha calculator
     */
    public static SlidingWindowAlpha create(Motion direction, Rect area, float progress, float gradientLength) {
        return new SlidingWindowAlpha(direction, area, progress, gradientLength);
    }
    
    /**
     * Calculates the alpha value for a given position.
     *
     * @param position the position to calculate alpha for
     * @return the alpha value (0.0 to 1.0) for the given position
     */
    public float alpha(Position position) {
        switch (direction) {
            case LEFT_TO_RIGHT:
                return moveLeftToRight(position);
            case RIGHT_TO_LEFT:
                return moveRightToLeft(position);
            case UP_TO_DOWN:
                return moveUpToDown(position);
            case DOWN_TO_UP:
                return moveDownToUp(position);
            default:
                return 0.0f;
        }
    }
    
    private float moveLeftToRight(Position position) {
        return 1.0f - moveRightToLeft(position);
    }
    
    private float moveRightToLeft(Position position) {
        float x = position.x();
        if (x < gradient.start) {
            return 0.0f;
        } else if (x > gradient.end) {
            return 1.0f;
        } else {
            return alphaPerCell * (x - gradient.start);
        }
    }
    
    private float moveUpToDown(Position position) {
        return 1.0f - moveDownToUp(position);
    }
    
    private float moveDownToUp(Position position) {
        float y = position.y();
        if (y < gradient.start) {
            return 0.0f;
        } else if (y > gradient.end) {
            return 1.0f;
        } else {
            return alphaPerCell * (y - gradient.start);
        }
    }
    
    private static Gradient calculateGradient(float progress, int coordinate, int areaLen, float gradientLen) {
        float start = (coordinate - gradientLen) + ((areaLen + gradientLen) * progress);
        float end = start + gradientLen;
        return new Gradient(start, end);
    }
    
    private static final class Gradient {
        final float start;
        final float end;
        
        Gradient(float start, float end) {
            this.start = start;
            this.end = end;
        }
    }
}



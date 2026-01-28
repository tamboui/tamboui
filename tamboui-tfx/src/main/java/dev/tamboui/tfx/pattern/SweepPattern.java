/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.pattern;

import dev.tamboui.tfx.Motion;
import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;

/**
 * Pattern implementation for linear sweep effects in cardinal directions.
 * <p>
 * SweepPattern creates effects that progress linearly across the terminal area
 * in one of the four cardinal directions (left-to-right, right-to-left, up-to-down,
 * down-to-up). The effect creates a "wave" or "wipe" animation that moves
 * across the screen.
 * <p>
 * <b>Design Philosophy:</b>
 * <p>
 * Sweep patterns are ideal for directional reveals, transitions, and wipes. They
 * provide a simple, predictable way to control effect progression based on
 * position along one axis.
 * <p>
 * <b>Key Concepts:</b>
 * <ul>
 *   <li><b>Direction:</b> The cardinal direction of the sweep (horizontal or vertical)</li>
 *   <li><b>Transition Width:</b> Controls the gradient zone where the effect transitions
 *       from inactive to active (in terminal cells). Larger values create smoother,
 *       more gradual transitions.</li>
 *   <li><b>Position Calculation:</b> Uses cell position along the sweep axis to
 *       determine effect strength</li>
 * </ul>
 * <p>
 * <b>Usage Pattern:</b>
 * <pre>{@code
 * // Sweep from left to right
 * Effect sweep = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut)
 *     .withPattern(SweepPattern.leftToRight(15.0f));
 * 
 * // Sweep from top to bottom
 * Effect verticalSweep = Fx.dissolve(2000, Interpolation.QuadOut)
 *     .withPattern(SweepPattern.upToDown(10.0f));
 * }</pre>
 * <p>
 * The pattern uses {@link TransitionProgress#mapSpatial(float, float, float)} internally
 * to calculate position-specific alpha values based on the cell's position along the
 * sweep axis and the global animation progress. This provides smooth gradient transitions
 * controlled by the transition width parameter.
 * <p>
 * <b>Note:</b> {@link dev.tamboui.tfx.SlidingWindowAlpha} is used by {@link dev.tamboui.tfx.effects.SweepShader}
 * for sweep effects, but {@code SweepPattern} uses {@code TransitionProgress} for
 * pattern-based spatial mapping when applied to other effects (like fade effects).
 */
public final class SweepPattern implements Pattern {
    
    private final Motion direction;
    private final TransitionProgress transition;
    
    /**
     * Creates a left-to-right sweep pattern.
     *
     * @param transitionWidth width of the gradient transition zone in terminal cells
     * @return a new left-to-right sweep pattern
     */
    public static SweepPattern leftToRight(float transitionWidth) {
        return new SweepPattern(Motion.LEFT_TO_RIGHT, transitionWidth);
    }
    
    /**
     * Creates a right-to-left sweep pattern.
     *
     * @param transitionWidth width of the gradient transition zone in terminal cells
     * @return a new right-to-left sweep pattern
     */
    public static SweepPattern rightToLeft(float transitionWidth) {
        return new SweepPattern(Motion.RIGHT_TO_LEFT, transitionWidth);
    }
    
    /**
     * Creates an up-to-down sweep pattern.
     *
     * @param transitionWidth width of the gradient transition zone in terminal cells
     * @return a new up-to-down sweep pattern
     */
    public static SweepPattern upToDown(float transitionWidth) {
        return new SweepPattern(Motion.UP_TO_DOWN, transitionWidth);
    }
    
    /**
     * Creates a down-to-up sweep pattern.
     *
     * @param transitionWidth width of the gradient transition zone in terminal cells
     * @return a new down-to-up sweep pattern
     */
    public static SweepPattern downToUp(float transitionWidth) {
        return new SweepPattern(Motion.DOWN_TO_UP, transitionWidth);
    }
    
    private SweepPattern(Motion direction, float transitionWidth) {
        this.direction = direction;
        this.transition = new TransitionProgress(transitionWidth);
    }
    
    @Override
    public float mapAlpha(float globalAlpha, Position position, Rect area) {
        float positionValue;
        float maxRange;
        
        switch (direction) {
            case LEFT_TO_RIGHT:
                positionValue = position.x() - area.x();
                maxRange = area.width();
                break;
            case RIGHT_TO_LEFT:
                positionValue = (area.x() + area.width()) - position.x();
                maxRange = area.width();
                break;
            case UP_TO_DOWN:
                positionValue = position.y() - area.y();
                maxRange = area.height();
                break;
            case DOWN_TO_UP:
                positionValue = (area.y() + area.height()) - position.y();
                maxRange = area.height();
                break;
            default:
                return globalAlpha;
        }
        
        return transition.mapSpatial(globalAlpha, positionValue, maxRange);
    }
    
    @Override
    public String name() {
        return "sweep_" + direction.name().toLowerCase();
    }
}



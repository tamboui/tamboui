/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.pattern;

import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;

/**
 * Pattern implementation for radial (circular) expansion effects.
 * <p>
 * RadialPattern creates effects that expand outward from a center point in a
 * circular or elliptical pattern. The effect progresses based on the distance
 * from the center point, creating a radial wave or expansion animation.
 * <p>
 * <b>Design Philosophy:</b>
 * <p>
 * Radial patterns are useful for creating "explosion" or "ripple" effects where
 * the animation spreads outward from a focal point. The transition width controls
 * how smoothly the effect transitions from inactive to active regions.
 * <p>
 * <b>Key Concepts:</b>
 * <ul>
 *   <li><b>Center Point:</b> The origin of the radial expansion, specified in
 *       normalized coordinates (0.0-1.0) relative to the area</li>
 *   <li><b>Distance Calculation:</b> Uses Euclidean distance from center to
 *       determine effect strength at each cell</li>
 *   <li><b>Transition Width:</b> Controls the gradient zone where the effect
 *       transitions from inactive to active (in terminal cells)</li>
 * </ul>
 * <p>
 * <b>Usage Pattern:</b>
 * <pre>{@code
 * // Radial expansion from center
 * Effect radial = Fx.dissolve(2000, Interpolation.QuadOut)
 *     .withPattern(RadialPattern.center().withTransitionWidth(10.0f));
 * 
 * // Custom center point (top-left corner)
 * Effect customRadial = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut)
 *     .withPattern(RadialPattern.at(0.0f, 0.0f).withTransitionWidth(8.0f));
 * }</pre>
 * <p>
 * The pattern calculates the distance from each cell to the center point and
 * uses {@link TransitionProgress} to map that distance to an alpha value based
 * on the global animation progress.
 */
public final class RadialPattern implements Pattern {
    
    private final float centerX;
    private final float centerY;
    private final TransitionProgress transition;
    
    /**
     * Creates a radial pattern centered at the middle of the area.
     *
     * @return a new radial pattern centered at (0.5, 0.5)
     */
    public static RadialPattern center() {
        return new RadialPattern(0.5f, 0.5f, 2.0f);
    }
    
    /**
     * Creates a radial pattern with custom center point (0.0-1.0 normalized coordinates).
     *
     * @param centerX Center X position (0.0-1.0)
     * @param centerY Center Y position (0.0-1.0)
     * @return a new radial pattern with the specified center
     */
    public static RadialPattern newPattern(float centerX, float centerY) {
        return new RadialPattern(centerX, centerY, 2.0f);
    }
    
    /**
     * Creates a radial pattern with custom center and transition width.
     *
     * @param centerX Center X position (0.0-1.0)
     * @param centerY Center Y position (0.0-1.0)
     * @param transitionWidth Width of the gradient transition zone in terminal cells
     * @return a new radial pattern with the specified center and transition width
     */
    public static RadialPattern withTransition(float centerX, float centerY, float transitionWidth) {
        return new RadialPattern(centerX, centerY, transitionWidth);
    }
    
    private RadialPattern(float centerX, float centerY, float transitionWidth) {
        this.centerX = java.lang.Math.max(0.0f, java.lang.Math.min(1.0f, centerX));
        this.centerY = java.lang.Math.max(0.0f, java.lang.Math.min(1.0f, centerY));
        this.transition = new TransitionProgress(transitionWidth);
    }
    
    /**
     * Creates a new pattern with a different transition width.
     *
     * @param width the new transition width in terminal cells
     * @return a new radial pattern with the specified transition width
     */
    public RadialPattern withTransitionWidth(float width) {
        return new RadialPattern(centerX, centerY, width);
    }
    
    /**
     * Creates a new pattern with a different center point.
     *
     * @param centerX the new center X position (0.0-1.0)
     * @param centerY the new center Y position (0.0-1.0)
     * @return a new radial pattern with the specified center point
     */
    public RadialPattern withCenter(float centerX, float centerY) {
        return new RadialPattern(centerX, centerY, transition.transitionWidth());
    }
    
    @Override
    public float mapAlpha(float globalAlpha, Position position, Rect area) {
        // Calculate center position in cell coordinates
        float centerXCell = area.x() + (centerX * area.width());
        float centerYCell = area.y() + (centerY * area.height());
        
        // Calculate distance from center in cell coordinates
        float dx = position.x() - centerXCell;
        float dy = position.y() - centerYCell;
        
        // Compensate for terminal cell aspect ratio (typically 2:1 height to width)
        float distance = dev.tamboui.tfx.TFxMath.sqrt(dx * dx + 2.0f * dy * 2.0f * dy);
        
        // Maximum distance is from center to corner
        float maxDistance = dev.tamboui.tfx.TFxMath.sqrt(
            (area.width() * area.width()) + 
            (2.0f * area.height() * 2.0f * area.height())
        );
        
        return transition.mapRadial(globalAlpha, distance, maxDistance);
    }
    
    @Override
    public String name() {
        return "radial";
    }
}


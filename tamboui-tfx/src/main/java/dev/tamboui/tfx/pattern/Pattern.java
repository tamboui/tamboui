/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.pattern;

import dev.tamboui.layout.Position;
import dev.tamboui.layout.Rect;

/// Interface for patterns that define spatial distribution of effects.
///
///
///
/// Patterns transform global animation progress (0.0-1.0) into position-specific alpha
/// values, controlling how effects spread across the terminal area. This allows effects
/// to progress in specific spatial patterns rather than uniformly across all cells.
///
///
///
/// **Design Philosophy:**
///
///
///
/// Patterns separate spatial concerns from effect logic. An effect defines "what happens"
/// while a pattern defines "where it happens first." This allows the same effect to
/// produce different visual results with different patterns.
///
///
///
/// **Key Concepts:**
///
/// - **Global Alpha:** The overall animation progress (0.0 = start, 1.0 = end)
/// - **Position Alpha:** The effect strength at a specific cell position
/// <li>**Transition Width:** The distance over which the effect transitions
/// from inactive to active (used by some patterns)
///
///
///
///
/// **Pattern Types:**
///
/// - **IdentityPattern:** No spatial transformation (uniform effect)
/// - **SweepPattern:** Linear progression in cardinal directions
/// - **RadialPattern:** Expansion outward from a center point
/// - **DiagonalPattern:** Diagonal sweeps across the area
///
///
///
///
/// **Usage Pattern:**
/// ```java
/// // Sweep from left to right
/// Effect sweep = Fx.fadeToFg(Color.CYAN, 2000, Interpolation.SineInOut)
///     .withPattern(SweepPattern.leftToRight(15.0f));
///
/// // Radial expansion from center
/// Effect radial = Fx.dissolve(2000, Interpolation.QuadOut)
///     .withPattern(RadialPattern.center().withTransitionWidth(10.0f));
///
/// // Diagonal sweep
/// Effect diagonal = Fx.fadeToFg(Color.MAGENTA, 2000, Interpolation.SineInOut)
///     .withPattern(DiagonalPattern.topLeftToBottomRight().withTransitionWidth(15.0f));
/// }
/// ```
///
///
///
/// Patterns are evaluated during effect execution for each cell, allowing effects to
/// apply different strengths based on cell position relative to the pattern's geometry.
public interface Pattern {

    /// Maps a global alpha value to a position-specific alpha value.
    ///
    /// @param globalAlpha The global animation progress (0.0-1.0)
    /// @param position The position of the cell
    /// @param area The rectangular area where the pattern is applied
    /// @return The position-specific alpha value (0.0-1.0)
    float mapAlpha(float globalAlpha, Position position, Rect area);

    /// Maps a global alpha value to a position-specific alpha value using primitive coordinates.
    ///
    ///
    ///
    /// This overload avoids Position object allocation in performance-critical loops.
    /// The default implementation creates a Position and delegates to
    /// {@link #mapAlpha(float, Position, Rect)}.
    ///
    /// @param globalAlpha The global animation progress (0.0-1.0)
    /// @param x The x coordinate of the cell
    /// @param y The y coordinate of the cell
    /// @param area The rectangular area where the pattern is applied
    /// @return The position-specific alpha value (0.0-1.0)
    default float mapAlpha(float globalAlpha, int x, int y, Rect area) {
        return mapAlpha(globalAlpha, new Position(x, y), area);
    }

    /// Returns the name of this pattern.
    ///
    /// @return the pattern name
    String name();
}




/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

/// Specifies the direction of movement for directional visual effects.
///
///
///
/// Motion defines the four cardinal directions used by sweep and slide effects
/// to control the direction of animation progression across the terminal area.
///
///
///
/// **Design Philosophy:**
///
///
///
/// Motion provides a type-safe way to specify animation direction, avoiding magic
/// strings or integers. It also handles the complexity of timer reversal for
/// certain directions to maintain consistent visual behavior.
///
///
///
/// **Directions:**
///
/// - **LEFT_TO_RIGHT:** Animation progresses from left edge to right edge
/// - **RIGHT_TO_LEFT:** Animation progresses from right edge to left edge
/// - **UP_TO_DOWN:** Animation progresses from top edge to bottom edge
/// - **DOWN_TO_UP:** Animation progresses from bottom edge to top edge
///
///
///
///
/// **Timer Reversal:**
///
///
///
/// Some directions ({@code RIGHT_TO_LEFT} and {@code DOWN_TO_UP}) require the
/// effect timer to be reversed to maintain consistent animation behavior. The
/// {@link #flipsTimer()} method indicates when this is necessary.
///
///
///
/// **Usage Pattern:**
/// ```java
/// // Sweep from left to right
/// Effect sweep = Fx.sweepIn(Motion.LEFT_TO_RIGHT, 10, 0, Color.BLUE, 
///     2000, Interpolation.QuadOut);
///
/// // Get opposite direction
/// Motion opposite = Motion.LEFT_TO_RIGHT.flipped(); // RIGHT_TO_LEFT
/// }
/// ```
public enum Motion {
    /// Movement from left to right
    LEFT_TO_RIGHT,
    
    /// Movement from right to left
    RIGHT_TO_LEFT,
    
    /// Movement from top to bottom
    UP_TO_DOWN,
    
    /// Movement from bottom to top
    DOWN_TO_UP;
    
    /// Returns the opposite direction of the current motion.
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
    
    /// Determines whether this motion direction requires timer reversal.
    ///
    ///
    ///
    /// Some motions (RIGHT_TO_LEFT and DOWN_TO_UP) require the effect timer to be reversed
    /// to maintain consistent animation behavior.
    public boolean flipsTimer() {
        return this == RIGHT_TO_LEFT || this == DOWN_TO_UP;
    }
}



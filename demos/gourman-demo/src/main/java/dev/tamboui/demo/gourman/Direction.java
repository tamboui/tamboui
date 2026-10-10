/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.gourman;

/** The four movement directions on the maze grid. Rows grow downward, so UP is dy = -1. */
public enum Direction {

    /** Toward the top of the maze. */
    UP(0, -1),
    /** Toward the bottom of the maze. */
    DOWN(0, 1),
    /** Toward the left edge of the maze. */
    LEFT(-1, 0),
    /** Toward the right edge of the maze. */
    RIGHT(1, 0);

    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /**
     * The column delta of one step in this direction.
     *
     * @return -1, 0 or 1
     */
    public int dx() {
        return dx;
    }

    /**
     * The row delta of one step in this direction.
     *
     * @return -1, 0 or 1
     */
    public int dy() {
        return dy;
    }

    /**
     * The reverse of this direction.
     *
     * @return the opposite direction
     */
    public Direction opposite() {
        switch (this) {
        case UP:
            return DOWN;
        case DOWN:
            return UP;
        case LEFT:
            return RIGHT;
        default:
            return LEFT;
        }
    }

}

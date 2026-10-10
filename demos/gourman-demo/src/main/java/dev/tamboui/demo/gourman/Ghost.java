/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo.gourman;

import dev.tamboui.style.Color;

/** One ghost: its identity (color, scatter corner, release delay) and its mutable position/state. */
public final class Ghost {

    /** The four ghosts. Scatter corners are just outside the maze, as in the arcade original. */
    public enum Kind {

        /** The red ghost: chases Gourman directly and is released immediately. */
        BLINKY(Color.RED, Maze.COLUMNS - 3, -1, 0),
        /** The pink ghost: ambushes four tiles ahead of Gourman. */
        PINKY(Color.rgb(255, 184, 255), 2, -1, 2),
        /** The cyan ghost: doubles Blinky's pincer vector. */
        INKY(Color.CYAN, Maze.COLUMNS - 1, Maze.ROWS, 4),
        /** The orange ghost: chases only while more than eight tiles away. */
        CLYDE(Color.rgb(255, 184, 82), 0, Maze.ROWS, 6);

        private final Color color;
        private final int scatterColumn;
        private final int scatterRow;
        private final int releaseSeconds;

        Kind(Color color, int scatterColumn, int scatterRow, int releaseSeconds) {
            this.color = color;
            this.scatterColumn = scatterColumn;
            this.scatterRow = scatterRow;
            this.releaseSeconds = releaseSeconds;
        }

        Color color() {
            return color;
        }

        int scatterColumn() {
            return scatterColumn;
        }

        int scatterRow() {
            return scatterRow;
        }

        int releaseSeconds() {
            return releaseSeconds;
        }

    }

    /** Where a ghost is in its life cycle. */
    public enum State {
        /** Parked inside the ghost house, waiting for its release time. */
        WAITING,
        /** Roaming the maze (chasing, scattering or frightened). */
        ACTIVE,
        /** Eaten: only the eyes remain, racing back to the ghost house. */
        EATEN
    }

    private final Kind kind;
    private int column;
    private int row;
    private Direction direction = Direction.LEFT;
    private State state = State.WAITING;
    private boolean frightened;
    private long nextMoveNanos;
    private long releaseAtNanos;

    Ghost(Kind kind) {
        this.kind = kind;
    }

    Kind kind() {
        return kind;
    }

    int column() {
        return column;
    }

    int row() {
        return row;
    }

    void position(int column, int row) {
        this.column = column;
        this.row = row;
    }

    Direction direction() {
        return direction;
    }

    void direction(Direction direction) {
        this.direction = direction;
    }

    State state() {
        return state;
    }

    void state(State state) {
        this.state = state;
    }

    boolean frightened() {
        return frightened;
    }

    void frightened(boolean frightened) {
        this.frightened = frightened;
    }

    long nextMoveNanos() {
        return nextMoveNanos;
    }

    void nextMoveNanos(long nextMoveNanos) {
        this.nextMoveNanos = nextMoveNanos;
    }

    long releaseAtNanos() {
        return releaseAtNanos;
    }

    void releaseAtNanos(long releaseAtNanos) {
        this.releaseAtNanos = releaseAtNanos;
    }

}

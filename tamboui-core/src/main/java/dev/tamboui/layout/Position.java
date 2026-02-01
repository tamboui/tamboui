/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

/**
 * A position in 2D space, representing x and y coordinates.
 */
public final class Position {

    /** The origin position at (0, 0). */
    public static final Position ORIGIN = new Position(0, 0);

    private final int x;
    private final int y;
    private final int cachedHashCode;

    /**
     * Creates a position.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public Position(int x, int y) {
        this.x = x;
        this.y = y;
        this.cachedHashCode = 31 * x + y;
    }

    /**
     * Returns the x coordinate.
     *
     * @return the x coordinate
     */
    public int x() {
        return x;
    }

    /**
     * Returns the y coordinate.
     *
     * @return the y coordinate
     */
    public int y() {
        return y;
    }

    /**
     * Returns a new position offset from this one.
     *
     * @param dx delta x
     * @param dy delta y
     * @return the offset position
     */
    public Position offset(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Position)) {
            return false;
        }
        Position that = (Position) o;
        if (cachedHashCode != that.cachedHashCode) {
            return false;
        }
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    @Override
    public String toString() {
        return String.format("Position[x=%d, y=%d]", x, y);
    }
}

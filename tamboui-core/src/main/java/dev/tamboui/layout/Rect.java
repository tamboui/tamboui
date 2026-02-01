/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A rectangular area defined by its position and size.
 */
public final class Rect {

    /** A zero-sized rectangle at the origin. */
    public static final Rect ZERO = new Rect(0, 0, 0, 0);

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int cachedHashCode;

    /**
     * Creates a rectangle at the given position and size.
     *
     * @param x      left coordinate
     * @param y      top coordinate
     * @param width  width in cells
     * @param height height in cells
     */
    public Rect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.cachedHashCode = computeHashCode();
    }

    private int computeHashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + width;
        result = 31 * result + height;
        return result;
    }

    /**
     * Creates a rectangle at origin with the given size.
     *
     * @param width  width in cells
     * @param height height in cells
     * @return a rectangle starting at (0,0)
     */
    public static Rect of(int width, int height) {
        return new Rect(0, 0, width, height);
    }

    /**
     * Creates a rectangle from a position and size.
     *
     * @param position top-left position
     * @param size     size of the rectangle
     * @return a rectangle at the given position and size
     */
    public static Rect of(Position position, Size size) {
        return new Rect(position.x(), position.y(), size.width(), size.height());
    }

    /** Returns the x coordinate.
     * @return x coordinate */
    public int x() {
        return x;
    }

    /** Returns the y coordinate.
     * @return y coordinate */
    public int y() {
        return y;
    }

    /** Returns the width.
     * @return width */
    public int width() {
        return width;
    }

    /** Returns the height.
     * @return height */
    public int height() {
        return height;
    }

    /** Returns the left edge (same as {@link #x()}).
     * @return left edge */
    public int left() {
        return x;
    }

    /** Returns the right edge (exclusive).
     * @return right edge */
    public int right() {
        return x + width;
    }

    /** Returns the top edge (same as {@link #y()}).
     * @return top edge */
    public int top() {
        return y;
    }

    /** Returns the bottom edge (exclusive).
     * @return bottom edge */
    public int bottom() {
        return y + height;
    }

    /**
     * Returns area (width * height).
     *
     * @return area in cells
     */
    public int area() {
        return width * height;
    }

    /**
     * Returns true if width or height is zero.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return width == 0 || height == 0;
    }

    /**
     * Returns the top-left position.
     *
     * @return position
     */
    public Position position() {
        return new Position(x, y);
    }

    /**
     * Returns the size.
     *
     * @return size
     */
    public Size size() {
        return new Size(width, height);
    }

    /**
     * Returns true if the given position lies inside this rectangle.
     *
     * @param pos position to test
     * @return true if contained
     */
    public boolean contains(Position pos) {
        return pos.x() >= x && pos.x() < right()
            && pos.y() >= y && pos.y() < bottom();
    }

    /**
     * Returns true if the given coordinates lie inside this rectangle.
     *
     * @param px x coordinate
     * @param py y coordinate
     * @return true if contained
     */
    public boolean contains(int px, int py) {
        return px >= x && px < right() && py >= y && py < bottom();
    }

    /**
     * Returns the inner area after applying the given margin.
     *
     * @param margin margin to subtract
     * @return inner rectangle (clamped to non-negative size)
     */
    public Rect inner(Margin margin) {
        int newX = x + margin.left();
        int newY = y + margin.top();
        int newWidth = Math.max(0, width - margin.horizontalTotal());
        int newHeight = Math.max(0, height - margin.verticalTotal());
        return new Rect(newX, newY, newWidth, newHeight);
    }

    /**
     * Returns the intersection of this rectangle with another.
     *
     * @param other rectangle to intersect with
     * @return overlapping rectangle, or {@link #ZERO} if none
     */
    public Rect intersection(Rect other) {
        int x1 = Math.max(this.x, other.x);
        int y1 = Math.max(this.y, other.y);
        int x2 = Math.min(this.right(), other.right());
        int y2 = Math.min(this.bottom(), other.bottom());

        if (x1 >= x2 || y1 >= y2) {
            return ZERO;
        }
        return new Rect(x1, y1, x2 - x1, y2 - y1);
    }

    /**
     * Returns the union of this rectangle with another.
     *
     * @param other rectangle to union with
     * @return bounding rectangle covering both
     */
    public Rect union(Rect other) {
        if (this.isEmpty()) {
            return other;
        }
        if (other.isEmpty()) {
            return this;
        }
        int x1 = Math.min(this.x, other.x);
        int y1 = Math.min(this.y, other.y);
        int x2 = Math.max(this.right(), other.right());
        int y2 = Math.max(this.bottom(), other.bottom());
        return new Rect(x1, y1, x2 - x1, y2 - y1);
    }

    /**
     * Returns a stream of all positions (cells) within this rectangle, row by row.
     *
     * @return stream of positions
     */
    public Stream<Position> positions() {
        return IntStream.range(y, bottom())
            .boxed()
            .flatMap(row -> IntStream.range(x, right())
                .mapToObj(col -> new Position(col, row)));
    }

    /**
     * Returns an iterator over rows in this rectangle.
     *
     * @return iterable of row rectangles (height=1)
     */
    public Iterable<Rect> rows() {
        return () -> new Iterator<Rect>() {
            private int currentY = y;

            @Override
            public boolean hasNext() {
                return currentY < bottom();
            }

            @Override
            public Rect next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Rect row = new Rect(x, currentY, width, 1);
                currentY++;
                return row;
            }
        };
    }

    /**
     * Returns an iterator over columns in this rectangle.
     *
     * @return iterable of column rectangles (width=1)
     */
    public Iterable<Rect> columns() {
        return () -> new Iterator<Rect>() {
            private int currentX = x;

            @Override
            public boolean hasNext() {
                return currentX < right();
            }

            @Override
            public Rect next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Rect col = new Rect(currentX, y, 1, height);
                currentX++;
                return col;
            }
        };
    }

    /**
     * Clamps another rectangle to fit within this one.
     *
     * @param inner rectangle to clamp
     * @return clamped rectangle
     */
    public Rect clamp(Rect inner) {
        int clampedX = Math.max(x, Math.min(inner.x, right() - inner.width));
        int clampedY = Math.max(y, Math.min(inner.y, bottom() - inner.height));
        int clampedWidth = Math.min(inner.width, width);
        int clampedHeight = Math.min(inner.height, height);
        return new Rect(clampedX, clampedY, clampedWidth, clampedHeight);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rect)) {
            return false;
        }
        Rect rect = (Rect) o;
        if (cachedHashCode != rect.cachedHashCode) {
            return false;
        }
        return x == rect.x
            && y == rect.y
            && width == rect.width
            && height == rect.height;
    }

    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    @Override
    public String toString() {
        return String.format("Rect[x=%d, y=%d, width=%d, height=%d]", x, y, width, height);
    }
}

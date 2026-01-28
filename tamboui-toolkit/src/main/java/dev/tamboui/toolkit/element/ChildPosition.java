/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

/**
 * Represents the position of a child element within its parent.
 * <p>
 * This is used with {@link RenderContext#childStyle(String, ChildPosition)} to enable
 * CSS pseudo-class matching based on position, such as {@code :first-child},
 * {@code :last-child}, and {@code :nth-child(even/odd)}.
 * <p>
 * Example usage:
 * <pre>{@code
 * for (int i = 0; i < rows.size(); i++) {
 *     ChildPosition pos = ChildPosition.of(i, rows.size());
 *     Style rowStyle = context.childStyle("row", pos);
 *     // CSS can now match :first-child, :last-child, :nth-child(even), etc.
 * }
 * }</pre>
 */
public final class ChildPosition {

    private final int index;
    private final int total;

    private ChildPosition(int index, int total) {
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }
        if (total <= 0) {
            throw new IllegalArgumentException("total must be > 0");
        }
        if (index >= total) {
            throw new IllegalArgumentException("index must be < total");
        }
        this.index = index;
        this.total = total;
    }

    /**
     * Creates a ChildPosition for the given index within a collection of the given size.
     *
     * @param index the zero-based index of the child
     * @param total the total number of children
     * @return the child position
     */
    public static ChildPosition of(int index, int total) {
        return new ChildPosition(index, total);
    }

    /**
     * Returns the zero-based index of this child.
     *
     * @return the zero-based index
     */
    public int index() {
        return index;
    }

    /**
     * Returns the total number of children.
     *
     * @return the total number of children
     */
    public int total() {
        return total;
    }

    /**
     * Returns true if this is the first child (index == 0).
     *
     * @return true if this is the first child
     */
    public boolean isFirst() {
        return index == 0;
    }

    /**
     * Returns true if this is the last child (index == total - 1).
     *
     * @return true if this is the last child
     */
    public boolean isLast() {
        return index == total - 1;
    }

    /**
     * Returns true if this child is at an even position (0, 2, 4, ...).
     * Note: This uses zero-based indexing, so the first child is "even".
     *
     * @return true if this child is at an even position
     */
    public boolean isEven() {
        return index % 2 == 0;
    }

    /**
     * Returns true if this child is at an odd position (1, 3, 5, ...).
     *
     * @return true if this child is at an odd position
     */
    public boolean isOdd() {
        return index % 2 == 1;
    }

    /**
     * Returns the 1-based position (CSS nth-child uses 1-based counting).
     *
     * @return the 1-based child position
     */
    public int nthChild() {
        return index + 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChildPosition)) return false;
        ChildPosition that = (ChildPosition) o;
        return index == that.index && total == that.total;
    }

    @Override
    public int hashCode() {
        return 31 * index + total;
    }

    @Override
    public String toString() {
        return "ChildPosition{index=" + index + ", total=" + total + "}";
    }
}

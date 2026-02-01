/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout;

import static dev.tamboui.util.CollectionUtil.listCopyOf;

import dev.tamboui.layout.cassowary.LayoutCache;
import dev.tamboui.layout.cassowary.LayoutSolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A layout defines how to split a rectangular area into smaller areas
 * based on constraints.
 *
 * <p>The layout uses a Cassowary constraint solver to compute sizes based on
 * the provided constraints, then positions the resulting rectangles according
 * to the {@link Flex} mode.
 *
 * <p>Example usage:
 * <pre>
 * Layout layout = Layout.horizontal()
 *     .constraints(
 *         Constraint.length(20),
 *         Constraint.fill(),
 *         Constraint.percentage(30))
 *     .spacing(1)
 *     .flex(Flex.CENTER);
 *
 * List&lt;Rect&gt; areas = layout.split(new Rect(0, 0, 100, 50));
 * </pre>
 *
 * @see Constraint
 * @see Flex
 */
public final class Layout {

    private final Direction direction;
    private final List<Constraint> constraints;
    private final Margin margin;
    private final int spacing;
    private final Flex flex;

    private Layout(Direction direction, List<Constraint> constraints,
                   Margin margin, int spacing, Flex flex) {
        this.direction = direction;
        this.constraints = listCopyOf(constraints);
        this.margin = margin;
        this.spacing = spacing;
        this.flex = flex;
    }

    /**
     * Creates a vertical layout (top-to-bottom).
     *
     * @return a new vertical layout
     */
    public static Layout vertical() {
        return new Layout(Direction.VERTICAL, listCopyOf(), Margin.NONE, 0, Flex.START);
    }

    /**
     * Creates a horizontal layout (left-to-right).
     *
     * @return a new horizontal layout
     */
    public static Layout horizontal() {
        return new Layout(Direction.HORIZONTAL, listCopyOf(), Margin.NONE, 0, Flex.START);
    }

    /**
     * Sets the constraints to apply when splitting.
     *
     * @param constraints ordered constraints corresponding to each split
     * @return a new layout with these constraints
     */
    public Layout constraints(Constraint... constraints) {
        return new Layout(direction, Arrays.asList(constraints), margin, spacing, flex);
    }

    /**
     * Sets the constraints to apply when splitting.
     *
     * @param constraints ordered constraints corresponding to each split
     * @return a new layout with these constraints
     */
    public Layout constraints(List<Constraint> constraints) {
        return new Layout(direction, constraints, margin, spacing, flex);
    }

    /**
     * Sets the outer margin.
     *
     * @param margin the margin to apply
     * @return a new layout with this margin
     */
    public Layout margin(Margin margin) {
        return new Layout(direction, constraints, margin, spacing, flex);
    }

    /**
     * Sets a uniform outer margin.
     *
     * @param value margin in cells applied to all sides
     * @return a new layout with this margin
     */
    public Layout margin(int value) {
        return new Layout(direction, constraints, Margin.uniform(value), spacing, flex);
    }

    /**
     * Sets the spacing (in cells) between split areas.
     *
     * @param spacing spacing between adjacent areas
     * @return a new layout with this spacing
     */
    public Layout spacing(int spacing) {
        return new Layout(direction, constraints, margin, spacing, flex);
    }

    /**
     * Sets the spacing between split areas using a Spacing object.
     *
     * @param spacing spacing between adjacent areas (can be Space or Overlap)
     * @return a new layout with this spacing
     */
    public Layout spacing(Spacing spacing) {
        return new Layout(direction, constraints, margin, spacing.value(), flex);
    }

    /**
     * Sets how remaining space is distributed.
     *
     * @param flex flex mode for distributing extra space
     * @return a new layout with this flex mode
     */
    public Layout flex(Flex flex) {
        return new Layout(direction, constraints, margin, spacing, flex);
    }

    /** Returns the layout direction.
     *
     * @return the layout direction
     */
    public Direction direction() {
        return direction;
    }

    /** Returns the constraints for the split.
     *
     * @return the constraints
     */
    public List<Constraint> constraints() {
        return constraints;
    }

    /** Returns the outer margin.
     *
     * @return the outer margin
     */
    public Margin margin() {
        return margin;
    }

    /** Returns the spacing between areas.
     *
     * @return the spacing between areas
     */
    public int spacing() {
        return spacing;
    }

    /** Returns the flex mode used to distribute extra space.
     *
     * @return the flex mode
     */
    public Flex flex() {
        return flex;
    }

    /**
     * Split the given area according to this layout's constraints.
     *
     * <p>The method uses the Cassowary constraint solver to compute optimal sizes
     * based on the provided constraints, then positions the resulting rectangles
     * according to the {@link Flex} mode.
     *
     * @param area the area to split
     * @return rectangles representing each split region, in order
     */
    public List<Rect> split(Rect area) {
        if (constraints.isEmpty()) {
            return listCopyOf();
        }

        // Apply margin first
        Rect inner = area.inner(margin);

        int available = direction == Direction.HORIZONTAL ? inner.width() : inner.height();
        int totalSpacing = spacing * (constraints.size() - 1);
        int distributable = Math.max(0, available - totalSpacing);

        // Use cached solver results, computing on miss
        int[] sizes = LayoutCache.instance().computeIfAbsent(
            constraints, distributable, spacing, flex,
            () -> new LayoutSolver().solve(constraints, distributable, spacing, flex));

        // Calculate total size used and remaining space for flex positioning
        int totalSize = 0;
        for (int size : sizes) {
            totalSize += size;
        }
        int usedSpace = totalSize + totalSpacing;
        int remainingSpace = Math.max(0, available - usedSpace);

        // Calculate starting position based on flex mode
        int startPos = direction == Direction.HORIZONTAL ? inner.x() : inner.y();
        int[] gaps = computeFlexGaps(sizes.length, remainingSpace, flex);

        // Build rectangles with flex positioning
        List<Rect> result = new ArrayList<>(constraints.size());
        int pos = startPos + gaps[0]; // Initial gap for CENTER/END/SPACE_AROUND

        for (int i = 0; i < sizes.length; i++) {
            Rect rect = direction == Direction.HORIZONTAL
                ? new Rect(pos, inner.y(), sizes[i], inner.height())
                : new Rect(inner.x(), pos, inner.width(), sizes[i]);
            result.add(rect);

            // Calculate next position: current pos + size + spacing + flex gap
            int flexGap = (i < sizes.length - 1) ? gaps[i + 1] : 0;
            pos += sizes[i] + spacing + flexGap;
        }

        return result;
    }

    /**
     * Computes the gaps for flex positioning.
     *
     * <p>Returns an array where:
     * <ul>
     *   <li>gaps[0] is the gap before the first element</li>
     *   <li>gaps[1..n-1] are the gaps between elements (added to spacing)</li>
     *   <li>gaps[n] is the gap after the last element (not used in positioning)</li>
     * </ul>
     *
     * @param count          number of elements
     * @param remainingSpace space available for distribution
     * @param flex           the flex mode
     * @return array of gaps
     */
    private int[] computeFlexGaps(int count, int remainingSpace, Flex flex) {
        int[] gaps = new int[count + 1];

        if (remainingSpace <= 0 || count == 0) {
            return gaps;
        }

        switch (flex) {
            case START:
                // All elements packed at start, remaining space at end
                gaps[count] = remainingSpace;
                break;

            case END:
                // All elements packed at end, remaining space at start
                gaps[0] = remainingSpace;
                break;

            case CENTER:
                // Elements centered, half the remaining space on each side
                gaps[0] = remainingSpace / 2;
                gaps[count] = remainingSpace - gaps[0];
                break;

            case SPACE_BETWEEN:
                // Space distributed between elements (not at edges)
                if (count > 1) {
                    int gapSize = remainingSpace / (count - 1);
                    int leftover = remainingSpace % (count - 1);
                    for (int i = 1; i < count; i++) {
                        gaps[i] = gapSize + (i <= leftover ? 1 : 0);
                    }
                } else {
                    // Single element: behave like CENTER
                    gaps[0] = remainingSpace / 2;
                    gaps[count] = remainingSpace - gaps[0];
                }
                break;

            case SPACE_AROUND:
                // Equal space around each element (half space at edges)
                if (count > 0) {
                    // Total units = count * 2 (each element has space on both sides)
                    // Edge elements share their edge space, so effective units = count * 2
                    // Edge space = gapSize / 2, between space = gapSize
                    int totalUnits = count * 2;
                    int unitSize = remainingSpace / totalUnits;
                    int leftover = remainingSpace % totalUnits;

                    // Start gap (half size)
                    gaps[0] = unitSize + (leftover > 0 ? 1 : 0);
                    if (leftover > 0) {
                        leftover--;
                    }

                    // Between gaps (full size)
                    for (int i = 1; i < count; i++) {
                        int extra = Math.min(2, leftover);
                        gaps[i] = unitSize * 2 + extra;
                        leftover -= extra;
                    }

                    // End gap (half size, remaining leftover)
                    gaps[count] = unitSize + leftover;
                }
                break;

            case SPACE_EVENLY:
                // Equal space everywhere: at edges and between all elements
                if (count > 0) {
                    int numGaps = count + 1;
                    int gapSize = remainingSpace / numGaps;
                    int leftover = remainingSpace % numGaps;
                    for (int i = 0; i <= count; i++) {
                        gaps[i] = gapSize + (i < leftover ? 1 : 0);
                    }
                }
                break;
        }

        return gaps;
    }
}

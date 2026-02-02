/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

import java.util.ArrayList;
import java.util.List;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Fraction;

/**
 * Bridge between TamboUI layout constraints and the Cassowary solver.
 *
 * <p>This class translates TamboUI's constraint types and Flex modes into
 * Cassowary constraints and solves them using the simplex method.
 *
 * <p>The solver creates variables for each segment's position and size,
 * then adds constraints based on the TamboUI constraint types and the
 * selected Flex distribution mode.
 *
 * <p>This implementation uses {@link Fraction} for exact arithmetic,
 * avoiding the cumulative rounding errors that occur with floating-point.
 *
 * @see Solver
 * @see dev.tamboui.layout.Layout
 */
public final class LayoutSolver {

    // Constraint strengths matching ratatui's hierarchy (higher = more important)
    // These ensure consistent priority ordering for constraint resolution
    private static final Strength LENGTH_SIZE_EQ = Strength.create(10, 0, 0);      // Fixed length
    private static final Strength PERCENTAGE_SIZE_EQ = Strength.STRONG;             // Percentage
    private static final Strength RATIO_SIZE_EQ = Strength.create(
            Fraction.of(1, 10), Fraction.ZERO, Fraction.ZERO);                      // Ratio
    private static final Strength MAX_SIZE_EQ = Strength.create(0, 10, 0);         // Max tries to reach value
    private static final Strength FILL_GROW = Strength.MEDIUM;                      // Fill/Min growth
    private static final Strength ALL_SEGMENT_GROW = Strength.WEAK;                 // Equal-size tiebreaker

    private final Solver solver;

    /**
     * Creates a new layout solver.
     */
    public LayoutSolver() {
        this.solver = new Solver();
    }

    /**
     * Solves layout constraints and returns the computed sizes.
     *
     * @param constraints TamboUI constraints for each segment
     * @param available   total available space
     * @param spacing     space between elements
     * @param flex        flex distribution mode
     * @return array of computed sizes for each constraint
     */
    public int[] solve(List<Constraint> constraints, int available, int spacing, Flex flex) {
        int n = constraints.size();
        if (n == 0) {
            return new int[0];
        }

        solver.reset();

        Variable[] sizes = new Variable[n];
        Variable[] positions = new Variable[n + 1];

        // Create variables
        for (int i = 0; i < n; i++) {
            sizes[i] = new Variable("size_" + i);
            positions[i] = new Variable("pos_" + i);
        }
        positions[n] = new Variable("pos_end");

        // Collect all constraints in a list for batch addition
        List<CassowaryConstraint> allConstraints = new ArrayList<>();

        // Add position and size relationship constraints (always required)
        collectStructuralConstraints(allConstraints, positions, sizes, n, spacing, available);

        // Convert TamboUI constraints to Cassowary constraints
        for (int i = 0; i < n; i++) {
            collectConstraintFor(allConstraints, constraints.get(i), sizes[i], available);
        }

        // Add fill proportionality constraints
        collectFillProportionalityConstraints(allConstraints, constraints, sizes);

        // Add equal-size tiebreaker constraints (all segments weakly prefer to be equal)
        collectEqualSizeTendency(allConstraints, sizes);

        // Add all constraints in batch (single optimization pass)
        solver.addConstraints(allConstraints);

        // Solve and extract results
        solver.updateVariables();

        // Get exact Fraction values and convert to integers.
        // Using Fraction arithmetic avoids cumulative rounding errors.
        Fraction[] fractionSizes = new Fraction[n];
        for (int i = 0; i < n; i++) {
            Fraction value = solver.valueOf(sizes[i]);
            fractionSizes[i] = value.isNegative() ? Fraction.ZERO : value;
        }

        return roundWithConstraint(fractionSizes, available);
    }

    /**
     * Collects structural constraints that define the relationship between positions and sizes.
     */
    private void collectStructuralConstraints(List<CassowaryConstraint> dest,
                                              Variable[] positions, Variable[] sizes,
                                              int n, int spacing, int available) {
        // All sizes must be non-negative
        for (int i = 0; i < n; i++) {
            dest.add(Expression.variable(sizes[i])
                    .greaterThanOrEqual(0, Strength.REQUIRED));
        }

        dest.add(Expression.variable(positions[0])
                .equalTo(0, Strength.REQUIRED));

        // Position relationships: pos[i+1] = pos[i] + size[i] + spacing
        for (int i = 0; i < n; i++) {
            int gap = (i < n - 1) ? spacing : 0;
            dest.add(Expression.variable(positions[i + 1])
                    .equalTo(Expression.variable(positions[i])
                                    .plus(Expression.variable(sizes[i]))
                                    .plus(gap),
                            Strength.REQUIRED));
        }

        // Total space constraint: last position <= available
        dest.add(Expression.variable(positions[n])
                .lessThanOrEqual(available, Strength.REQUIRED));
    }

    /**
     * Collects Cassowary constraints for a TamboUI constraint.
     */
    private void collectConstraintFor(List<CassowaryConstraint> dest, Constraint c, Variable size, int available) {
        if (c instanceof Constraint.Length) {
            // Fixed size: size == value (strong)
            int value = ((Constraint.Length) c).value();
            dest.add(Expression.variable(size)
                    .equalTo(value, LENGTH_SIZE_EQ));

        } else if (c instanceof Constraint.Percentage) {
            // Percentage: size == available * percent / 100
            // Use exact Fraction arithmetic: available * percent / 100
            int percent = ((Constraint.Percentage) c).value();
            Fraction target = Fraction.of(available).multiply(Fraction.of(percent, 100));
            dest.add(Expression.variable(size)
                    .equalTo(target, PERCENTAGE_SIZE_EQ));

        } else if (c instanceof Constraint.Ratio) {
            // Ratio: size == available * ratio
            Constraint.Ratio ratio = (Constraint.Ratio) c;
            Fraction target = Fraction.of(available).multiply(ratio.toFraction());
            dest.add(Expression.variable(size)
                    .equalTo(target, RATIO_SIZE_EQ));

        } else if (c instanceof Constraint.Min) {
            // Minimum: size >= value (hard constraint)
            int value = ((Constraint.Min) c).value();
            dest.add(Expression.variable(size)
                    .greaterThanOrEqual(value, Strength.REQUIRED));
            // Min tries to GROW to fill available space (like Fill)
            dest.add(Expression.variable(size)
                    .equalTo(available, FILL_GROW));

        } else if (c instanceof Constraint.Max) {
            // Maximum: size <= value (hard constraint)
            int value = ((Constraint.Max) c).value();
            dest.add(Expression.variable(size)
                    .lessThanOrEqual(value, Strength.REQUIRED));
            // Max tries to REACH its maximum value
            dest.add(Expression.variable(size)
                    .equalTo(value, MAX_SIZE_EQ));

        } else if (c instanceof Constraint.Fill) {
            // Fill: try to grow to fill available space
            // Proportionality is handled by collectFillProportionalityConstraints
            dest.add(Expression.variable(size)
                    .equalTo(available, FILL_GROW));
        }
    }

    /**
     * Collects proportionality constraints between Fill segments.
     * Makes Fill(2) twice as large as Fill(1), etc.
     */
    private void collectFillProportionalityConstraints(List<CassowaryConstraint> dest,
                                                       List<Constraint> constraints, Variable[] sizes) {
        int n = constraints.size();

        // Find all Fill and Min constraints (Min behaves like Fill in non-legacy mode)
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Fraction leftScale = getFillScale(constraints.get(i));
                Fraction rightScale = getFillScale(constraints.get(j));

                if (!leftScale.isZero() && !rightScale.isZero()) {
                    // rightScale * leftSize == leftScale * rightSize
                    // This ensures Fill(2) is twice as large as Fill(1)
                    dest.add(Expression.variable(sizes[i]).times(rightScale)
                            .equalTo(Expression.variable(sizes[j]).times(leftScale), FILL_GROW));
                }
            }
        }
    }

    /**
     * Returns the fill scale for a constraint, or 0 if it's not a fill-like constraint.
     */
    private Fraction getFillScale(Constraint c) {
        if (c instanceof Constraint.Fill) {
            int weight = ((Constraint.Fill) c).weight();
            // Use small fraction for weight 0 to allow proportional collapse
            return weight == 0 ? Fraction.of(1, 1_000_000) : Fraction.of(weight);
        } else if (c instanceof Constraint.Min) {
            // Min behaves like Fill(1) for proportionality
            return Fraction.ONE;
        }
        return Fraction.ZERO;
    }

    /**
     * Collects weak constraints that make all segments tend toward equal size.
     * This serves as a tiebreaker when other constraints don't fully determine sizes.
     */
    private void collectEqualSizeTendency(List<CassowaryConstraint> dest, Variable[] sizes) {
        int n = sizes.length;
        for (int i = 0; i < n - 1; i++) {
            // Each segment weakly prefers to be equal to the next
            dest.add(Expression.variable(sizes[i])
                    .equalTo(Expression.variable(sizes[i + 1]), ALL_SEGMENT_GROW));
        }
    }

    /**
     * Converts Fraction sizes to integers using the largest remainder method.
     *
     * <p>This method is necessary because simple rounding (Math.round on each value)
     * can produce totals that don't match the available space. For example, with
     * constraints [1/3, 2/3] of 100, naive rounding gives [33, 67] = 100, but
     * [0.333..., 0.666...] might round to [33, 66] = 99, losing a pixel.
     *
     * <p>The largest remainder method (also known as Hamilton's method) ensures fair
     * distribution: floor all values, then give +1 to those with the largest remainders.
     *
     * @param fractionSizes the exact Fraction sizes from the solver
     * @param target        the maximum sum (available space)
     * @return integer sizes that sum to at most target
     */
    private int[] roundWithConstraint(Fraction[] fractionSizes, int target) {
        int n = fractionSizes.length;
        int[] result = new int[n];

        // Floor all values and track remainders
        int sum = 0;
        Fraction[] remainders = new Fraction[n];
        for (int i = 0; i < n; i++) {
            result[i] = fractionSizes[i].toInt();
            remainders[i] = fractionSizes[i].subtract(Fraction.of(result[i]));
            sum += result[i];
        }

        // Distribute remaining space to segments with largest remainders
        int remaining = target - sum;
        while (remaining > 0) {
            int maxIdx = -1;
            Fraction maxRemainder = Fraction.ZERO;
            for (int i = 0; i < n; i++) {
                if (remainders[i].compareTo(maxRemainder) > 0) {
                    maxRemainder = remainders[i];
                    maxIdx = i;
                }
            }
            if (maxIdx < 0 || !maxRemainder.isPositive()) {
                break;
            }
            result[maxIdx]++;
            remainders[maxIdx] = Fraction.ZERO;
            remaining--;
        }

        return result;
    }
}

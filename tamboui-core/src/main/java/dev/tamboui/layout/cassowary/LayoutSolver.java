/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;

import java.util.List;

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
 * @see Solver
 * @see dev.tamboui.layout.Layout
 */
public final class LayoutSolver {

    // Constraint strengths matching ratatui's hierarchy (higher = more important)
    // These ensure consistent priority ordering for constraint resolution
    private static final Strength MIN_SIZE_GE = Strength.create(100.0, 0, 0);      // Hard minimum
    private static final Strength MAX_SIZE_LE = Strength.create(100.0, 0, 0);      // Hard maximum
    private static final Strength LENGTH_SIZE_EQ = Strength.create(10.0, 0, 0);    // Fixed length
    private static final Strength PERCENTAGE_SIZE_EQ = Strength.STRONG;             // Percentage
    private static final Strength RATIO_SIZE_EQ = Strength.create(0.1, 0, 0);      // Ratio
    private static final Strength MAX_SIZE_EQ = Strength.create(0, 10.0, 0);       // Max tries to reach value
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
        solver.reset();

        int n = constraints.size();
        if (n == 0) {
            return new int[0];
        }

        Variable[] sizes = new Variable[n];
        Variable[] positions = new Variable[n + 1];

        // Create variables
        for (int i = 0; i < n; i++) {
            sizes[i] = new Variable("size_" + i);
            positions[i] = new Variable("pos_" + i);
        }
        positions[n] = new Variable("pos_end");

        // Add position and size relationship constraints (always required)
        addStructuralConstraints(positions, sizes, n, spacing, available);

        // Convert TamboUI constraints to Cassowary constraints
        for (int i = 0; i < n; i++) {
            addConstraintFor(constraints.get(i), sizes[i], available);
        }

        // Add fill proportionality constraints
        addFillProportionalityConstraints(constraints, sizes);

        // Add equal-size tiebreaker constraints (all segments weakly prefer to be equal)
        addEqualSizeTendency(sizes);

        // Solve and extract results
        solver.updateVariables();

        int[] result = new int[n];
        for (int i = 0; i < n; i++) {
            result[i] = Math.max(0, (int) Math.round(solver.valueOf(sizes[i])));
        }

        return result;
    }

    /**
     * Adds structural constraints that define the relationship between positions and sizes.
     */
    private void addStructuralConstraints(Variable[] positions, Variable[] sizes,
                                          int n, int spacing, int available) {
        // All sizes must be non-negative
        for (int i = 0; i < n; i++) {
            solver.addConstraint(
                    Expression.variable(sizes[i])
                            .greaterThanOrEqual(0, Strength.REQUIRED));
        }

        solver.addConstraint(
                Expression.variable(positions[0])
                        .equalTo(0, Strength.REQUIRED));

        // Position relationships: pos[i+1] = pos[i] + size[i] + spacing
        for (int i = 0; i < n; i++) {
            int gap = (i < n - 1) ? spacing : 0;
            solver.addConstraint(
                    Expression.variable(positions[i + 1])
                            .equalTo(Expression.variable(positions[i])
                                            .plus(Expression.variable(sizes[i]))
                                            .plus(gap),
                                    Strength.REQUIRED));
        }

        // Total space constraint: last position <= available
        solver.addConstraint(
                Expression.variable(positions[n])
                        .lessThanOrEqual(available, Strength.REQUIRED));
    }

    /**
     * Converts a TamboUI constraint to Cassowary constraints.
     */
    private void addConstraintFor(Constraint c, Variable size, int available) {
        if (c instanceof Constraint.Length) {
            // Fixed size: size == value (strong)
            int value = ((Constraint.Length) c).value();
            solver.addConstraint(
                    Expression.variable(size)
                            .equalTo(value, LENGTH_SIZE_EQ));

        } else if (c instanceof Constraint.Percentage) {
            // Percentage: size == available * percent / 100
            int percent = ((Constraint.Percentage) c).value();
            int target = available * percent / 100;
            solver.addConstraint(
                    Expression.variable(size)
                            .equalTo(target, PERCENTAGE_SIZE_EQ));

        } else if (c instanceof Constraint.Ratio) {
            // Ratio: size == available * num / denom
            Constraint.Ratio ratio = (Constraint.Ratio) c;
            int target = available * ratio.numerator() / ratio.denominator();
            solver.addConstraint(
                    Expression.variable(size)
                            .equalTo(target, RATIO_SIZE_EQ));

        } else if (c instanceof Constraint.Min) {
            // Minimum: size >= value (hard constraint)
            int value = ((Constraint.Min) c).value();
            solver.addConstraint(
                    Expression.variable(size)
                            .greaterThanOrEqual(value, Strength.REQUIRED));
            // Min tries to GROW to fill available space (like Fill)
            solver.addConstraint(
                    Expression.variable(size)
                            .equalTo(available, FILL_GROW));

        } else if (c instanceof Constraint.Max) {
            // Maximum: size <= value (hard constraint)
            int value = ((Constraint.Max) c).value();
            solver.addConstraint(
                    Expression.variable(size)
                            .lessThanOrEqual(value, Strength.REQUIRED));
            // Max tries to REACH its maximum value
            solver.addConstraint(
                    Expression.variable(size)
                            .equalTo(value, MAX_SIZE_EQ));

        } else if (c instanceof Constraint.Fill) {
            // Fill: try to grow to fill available space
            // Proportionality is handled by addFillProportionalityConstraints
            solver.addConstraint(
                    Expression.variable(size)
                            .equalTo(available, FILL_GROW));
        }
    }

    /**
     * Adds proportionality constraints between Fill segments.
     * Makes Fill(2) twice as large as Fill(1), etc.
     */
    private void addFillProportionalityConstraints(List<Constraint> constraints, Variable[] sizes) {
        int n = constraints.size();

        // Find all Fill and Min constraints (Min behaves like Fill in non-legacy mode)
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double leftScale = getFillScale(constraints.get(i));
                double rightScale = getFillScale(constraints.get(j));

                if (leftScale > 0 && rightScale > 0) {
                    // rightScale * leftSize == leftScale * rightSize
                    // This ensures Fill(2) is twice as large as Fill(1)
                    solver.addConstraint(
                            Expression.variable(sizes[i]).times(rightScale)
                                    .equalTo(Expression.variable(sizes[j]).times(leftScale), FILL_GROW));
                }
            }
        }
    }

    /**
     * Returns the fill scale for a constraint, or 0 if it's not a fill-like constraint.
     */
    private double getFillScale(Constraint c) {
        if (c instanceof Constraint.Fill) {
            int weight = ((Constraint.Fill) c).weight();
            // Use epsilon for weight 0 to allow proportional collapse
            return weight == 0 ? 1e-6 : weight;
        } else if (c instanceof Constraint.Min) {
            // Min behaves like Fill(1) for proportionality
            return 1.0;
        }
        return 0;
    }

    /**
     * Adds weak constraints that make all segments tend toward equal size.
     * This serves as a tiebreaker when other constraints don't fully determine sizes.
     */
    private void addEqualSizeTendency(Variable[] sizes) {
        int n = sizes.length;
        for (int i = 0; i < n - 1; i++) {
            // Each segment weakly prefers to be equal to the next
            solver.addConstraint(
                    Expression.variable(sizes[i])
                            .equalTo(Expression.variable(sizes[i + 1]), ALL_SEGMENT_GROW));
        }
    }
}

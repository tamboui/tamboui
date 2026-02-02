/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;

import static org.assertj.core.api.Assertions.assertThat;

class LayoutSolverTest {

    @Test
    @DisplayName("Fixed length constraints")
    void fixedLengths() {
        LayoutSolver solver = new LayoutSolver();
        List<Constraint> constraints = Arrays.asList(
                Constraint.length(20),
                Constraint.length(30),
                Constraint.length(50)
        );

        int[] sizes = solver.solve(constraints, 100, 0, Flex.START);

        assertThat(sizes).containsExactly(20, 30, 50);
    }

    @Test
    @DisplayName("Percentage constraints")
    void percentages() {
        LayoutSolver solver = new LayoutSolver();
        List<Constraint> constraints = Arrays.asList(
                Constraint.percentage(25),
                Constraint.percentage(75)
        );

        int[] sizes = solver.solve(constraints, 100, 0, Flex.START);

        assertThat(sizes).containsExactly(25, 75);
    }

    @Test
    @DisplayName("Ratio constraints")
    void ratios() {
        LayoutSolver solver = new LayoutSolver();
        List<Constraint> constraints = Arrays.asList(
                Constraint.ratio(1, 3),
                Constraint.ratio(2, 3)
        );

        int[] sizes = solver.solve(constraints, 100, 0, Flex.START);

        // With exact Fraction arithmetic: 100*1/3=33.33..., 100*2/3=66.66...
        // Using largest remainder method preserves total of 100: [33, 67]
        assertThat(sizes).containsExactly(33, 67);
    }

    @Test
    @DisplayName("Fill with equal weights")
    void fillEqualWeights() {
        LayoutSolver solver = new LayoutSolver();
        List<Constraint> constraints = Arrays.asList(
                Constraint.fill(),
                Constraint.fill(),
                Constraint.fill()
        );

        int[] sizes = solver.solve(constraints, 90, 0, Flex.START);

        // Equal distribution: 90/3 = 30 each
        assertThat(sizes).containsExactly(30, 30, 30);
    }

    @Test
    @DisplayName("Fill with different weights")
    void fillDifferentWeights() {
        LayoutSolver solver = new LayoutSolver();
        List<Constraint> constraints = Arrays.asList(
                Constraint.fill(1),
                Constraint.fill(2),
                Constraint.fill(1)
        );

        int[] sizes = solver.solve(constraints, 100, 0, Flex.START);

        // 1:2:1 ratio = 25:50:25
        assertThat(sizes).containsExactly(25, 50, 25);
    }

    @Test
    @DisplayName("Min constraint is enforced")
    void minConstraint() {
        LayoutSolver solver = new LayoutSolver();
        List<Constraint> constraints = Arrays.asList(
                Constraint.min(30),
                Constraint.fill()
        );

        int[] sizes = solver.solve(constraints, 100, 0, Flex.START);

        assertThat(sizes[0]).isGreaterThanOrEqualTo(30);
    }

    @Test
    @DisplayName("Max constraint is enforced")
    void maxConstraint() {
        LayoutSolver solver = new LayoutSolver();
        List<Constraint> constraints = Arrays.asList(
                Constraint.max(30),
                Constraint.fill()
        );

        int[] sizes = solver.solve(constraints, 100, 0, Flex.START);

        assertThat(sizes[0]).isLessThanOrEqualTo(30);
    }

    @Test
    @DisplayName("Mixed constraints")
    void mixedConstraints() {
        LayoutSolver solver = new LayoutSolver();
        List<Constraint> constraints = Arrays.asList(
                Constraint.length(20),
                Constraint.fill(),
                Constraint.percentage(30)
        );

        int[] sizes = solver.solve(constraints, 100, 0, Flex.START);

        // 20 fixed + 30% of 100 = 50, fill gets the rest
        assertThat(sizes[0]).isEqualTo(20);
        assertThat(sizes[2]).isEqualTo(30);
    }

    @Test
    @DisplayName("Empty constraints returns empty array")
    void emptyConstraints() {
        LayoutSolver solver = new LayoutSolver();

        int[] sizes = solver.solve(Arrays.asList(), 100, 0, Flex.START);

        assertThat(sizes).isEmpty();
    }

    @Test
    @DisplayName("Single constraint")
    void singleConstraint() {
        LayoutSolver solver = new LayoutSolver();
        List<Constraint> constraints = Arrays.asList(
                Constraint.fill()
        );

        int[] sizes = solver.solve(constraints, 100, 0, Flex.START);

        assertThat(sizes).containsExactly(100);
    }

    @Test
    @DisplayName("Sizes are never negative")
    void sizesNeverNegative() {
        LayoutSolver solver = new LayoutSolver();
        List<Constraint> constraints = Arrays.asList(
                Constraint.length(80),
                Constraint.length(80)
        );

        // More requested than available
        int[] sizes = solver.solve(constraints, 100, 0, Flex.START);

        for (int size : sizes) {
            assertThat(size).isGreaterThanOrEqualTo(0);
        }
    }
}

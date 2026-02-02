/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Fraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SolverTest {

    @Test
    @DisplayName("Simple equality constraint")
    void simpleEquality() {
        Solver solver = new Solver();
        Variable x = new Variable("x");

        solver.addConstraint(
                Expression.variable(x)
                        .equalTo(100, Strength.REQUIRED));
        solver.updateVariables();

        assertThat(solver.valueOf(x)).isEqualTo(Fraction.of(100));
    }

    @Test
    @DisplayName("Two variables with relationship")
    void twoVariables() {
        Solver solver = new Solver();
        Variable left = new Variable("left");
        Variable right = new Variable("right");

        // right == left + 100
        solver.addConstraint(
                Expression.variable(right)
                        .equalTo(Expression.variable(left).plus(100),
                                Strength.REQUIRED));
        // left == 50
        solver.addConstraint(
                Expression.variable(left)
                        .equalTo(50, Strength.REQUIRED));
        solver.updateVariables();

        assertThat(solver.valueOf(left)).isEqualTo(Fraction.of(50));
        assertThat(solver.valueOf(right)).isEqualTo(Fraction.of(150));
    }

    @Test
    @DisplayName("Greater than or equal constraint")
    void greaterThanOrEqual() {
        Solver solver = new Solver();
        Variable x = new Variable("x");

        // x >= 100 (required)
        solver.addConstraint(
                Expression.variable(x)
                        .greaterThanOrEqual(100, Strength.REQUIRED));
        // x == 50 (weak - should be overridden)
        solver.addConstraint(
                Expression.variable(x)
                        .equalTo(50, Strength.WEAK));
        solver.updateVariables();

        assertThat(solver.valueOf(x).compareTo(Fraction.of(100))).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Less than or equal constraint")
    void lessThanOrEqual() {
        Solver solver = new Solver();
        Variable x = new Variable("x");

        // x <= 50 (required)
        solver.addConstraint(
                Expression.variable(x)
                        .lessThanOrEqual(50, Strength.REQUIRED));
        // x == 100 (weak - should be overridden)
        solver.addConstraint(
                Expression.variable(x)
                        .equalTo(100, Strength.WEAK));
        solver.updateVariables();

        assertThat(solver.valueOf(x).compareTo(Fraction.of(50))).isLessThanOrEqualTo(0);
    }

    @Test
    @DisplayName("Strength hierarchy - strong overrides weak")
    void strengthHierarchy() {
        Solver solver = new Solver();
        Variable x = new Variable("x");

        // x == 100 (strong)
        solver.addConstraint(
                Expression.variable(x)
                        .equalTo(100, Strength.STRONG));
        // x == 200 (weak - should be ignored)
        solver.addConstraint(
                Expression.variable(x)
                        .equalTo(200, Strength.WEAK));
        solver.updateVariables();

        assertThat(solver.valueOf(x)).isEqualTo(Fraction.of(100));
    }

    @Test
    @DisplayName("Unsatisfiable required constraint throws exception")
    void unsatisfiableThrows() {
        Solver solver = new Solver();
        Variable x = new Variable("x");

        solver.addConstraint(
                Expression.variable(x)
                        .equalTo(100, Strength.REQUIRED));

        assertThatThrownBy(() -> solver.addConstraint(
                Expression.variable(x)
                        .equalTo(200, Strength.REQUIRED)))
                .isInstanceOf(UnsatisfiableConstraintException.class);
    }

    @Test
    @DisplayName("Duplicate constraint throws exception")
    void duplicateThrows() {
        Solver solver = new Solver();
        Variable x = new Variable("x");

        CassowaryConstraint c = Expression.variable(x)
                .equalTo(100, Strength.REQUIRED);

        solver.addConstraint(c);

        assertThatThrownBy(() -> solver.addConstraint(c))
                .isInstanceOf(DuplicateConstraintException.class);
    }

    @Test
    @DisplayName("Remove constraint")
    void removeConstraint() {
        Solver solver = new Solver();
        Variable x = new Variable("x");

        CassowaryConstraint c1 = Expression.variable(x)
                .equalTo(100, Strength.REQUIRED);
        CassowaryConstraint c2 = Expression.variable(x)
                .equalTo(200, Strength.STRONG);

        solver.addConstraint(c1);
        solver.updateVariables();
        assertThat(solver.valueOf(x)).isEqualTo(Fraction.of(100));

        solver.removeConstraint(c1);
        solver.addConstraint(c2);
        solver.updateVariables();
        assertThat(solver.valueOf(x)).isEqualTo(Fraction.of(200));
    }

    @Test
    @DisplayName("Edit variable can be added and removed")
    void editVariable() {
        Solver solver = new Solver();
        Variable x = new Variable("x");

        // Add constraint first
        solver.addConstraint(
                Expression.variable(x)
                        .greaterThanOrEqual(0, Strength.REQUIRED));

        // Edit variables can be added
        solver.addEditVariable(x, Strength.STRONG);
        assertThat(solver.hasEditVariable(x)).isTrue();

        // Edit variables can be removed
        solver.removeEditVariable(x);
        assertThat(solver.hasEditVariable(x)).isFalse();
    }

    @Test
    @DisplayName("Three variables with chain relationship")
    void threeVariablesChain() {
        Solver solver = new Solver();
        Variable x = new Variable("x");
        Variable y = new Variable("y");
        Variable z = new Variable("z");

        // x == 10
        solver.addConstraint(
                Expression.variable(x).equalTo(10, Strength.REQUIRED));
        // y == x + 20
        solver.addConstraint(
                Expression.variable(y)
                        .equalTo(Expression.variable(x).plus(20), Strength.REQUIRED));
        // z == y + 30
        solver.addConstraint(
                Expression.variable(z)
                        .equalTo(Expression.variable(y).plus(30), Strength.REQUIRED));

        solver.updateVariables();

        assertThat(solver.valueOf(x)).isEqualTo(Fraction.of(10));
        assertThat(solver.valueOf(y)).isEqualTo(Fraction.of(30));
        assertThat(solver.valueOf(z)).isEqualTo(Fraction.of(60));
    }

    @Test
    @DisplayName("Linear expression with multiple variables")
    void linearExpressionMultipleVariables() {
        Solver solver = new Solver();
        Variable x = new Variable("x");
        Variable y = new Variable("y");
        Variable z = new Variable("z");

        // x == 10
        solver.addConstraint(
                Expression.variable(x).equalTo(10, Strength.REQUIRED));
        // y == 20
        solver.addConstraint(
                Expression.variable(y).equalTo(20, Strength.REQUIRED));
        // z == 2*x + 3*y (should be 2*10 + 3*20 = 80)
        solver.addConstraint(
                Expression.variable(z)
                        .equalTo(Expression.variable(x).times(2)
                                        .plus(Expression.variable(y).times(3)),
                                Strength.REQUIRED));

        solver.updateVariables();

        assertThat(solver.valueOf(z)).isEqualTo(Fraction.of(80));
    }

    @Test
    @DisplayName("Reset solver clears all state")
    void resetSolver() {
        Solver solver = new Solver();
        Variable x = new Variable("x");

        solver.addConstraint(
                Expression.variable(x).equalTo(100, Strength.REQUIRED));
        solver.updateVariables();
        assertThat(solver.valueOf(x)).isEqualTo(Fraction.of(100));

        solver.reset();
        solver.updateVariables();
        assertThat(solver.valueOf(x)).isEqualTo(Fraction.ZERO);
    }

    @Test
    @DisplayName("hasConstraint returns correct value")
    void hasConstraint() {
        Solver solver = new Solver();
        Variable x = new Variable("x");

        CassowaryConstraint c = Expression.variable(x)
                .equalTo(100, Strength.REQUIRED);

        assertThat(solver.hasConstraint(c)).isFalse();

        solver.addConstraint(c);
        assertThat(solver.hasConstraint(c)).isTrue();

        solver.removeConstraint(c);
        assertThat(solver.hasConstraint(c)).isFalse();
    }
}

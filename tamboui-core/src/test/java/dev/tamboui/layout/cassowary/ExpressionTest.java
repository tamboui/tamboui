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

class ExpressionTest {

    @Test
    @DisplayName("Zero expression")
    void zeroExpression() {
        Expression zero = Expression.zero();
        assertThat(zero.isConstant()).isTrue();
        assertThat(zero.constant()).isEqualTo(Fraction.ZERO);
        assertThat(zero.terms()).isEmpty();
    }

    @Test
    @DisplayName("Constant expression")
    void constantExpression() {
        Expression expr = Expression.constant(42);
        assertThat(expr.isConstant()).isTrue();
        assertThat(expr.constant()).isEqualTo(Fraction.of(42));
        assertThat(expr.terms()).isEmpty();
    }

    @Test
    @DisplayName("Variable expression")
    void variableExpression() {
        Variable x = new Variable("x");
        Expression expr = Expression.variable(x);

        assertThat(expr.isConstant()).isFalse();
        assertThat(expr.constant()).isEqualTo(Fraction.ZERO);
        assertThat(expr.terms()).hasSize(1);
        assertThat(expr.terms().get(0).variable()).isEqualTo(x);
        assertThat(expr.terms().get(0).coefficient()).isEqualTo(Fraction.ONE);
    }

    @Test
    @DisplayName("Add two expressions")
    void addExpressions() {
        Variable x = new Variable("x");
        Variable y = new Variable("y");

        Expression expr1 = Expression.variable(x).plus(10);
        Expression expr2 = Expression.variable(y).plus(20);
        Expression sum = expr1.plus(expr2);

        assertThat(sum.constant()).isEqualTo(Fraction.of(30));
        assertThat(sum.terms()).hasSize(2);
    }

    @Test
    @DisplayName("Subtract expressions")
    void subtractExpressions() {
        Variable x = new Variable("x");

        Expression expr = Expression.variable(x).plus(100).minus(30);

        assertThat(expr.constant()).isEqualTo(Fraction.of(70));
    }

    @Test
    @DisplayName("Multiply expression by scalar")
    void multiplyByScalar() {
        Variable x = new Variable("x");
        Expression expr = Expression.variable(x).plus(10).times(3);

        assertThat(expr.constant()).isEqualTo(Fraction.of(30));
        assertThat(expr.terms()).hasSize(1);
        assertThat(expr.terms().get(0).coefficient()).isEqualTo(Fraction.of(3));
    }

    @Test
    @DisplayName("Divide expression by scalar")
    void divideByScalar() {
        Variable x = new Variable("x");
        Expression expr = Expression.variable(x).times(6).plus(12).divide(3);

        assertThat(expr.constant()).isEqualTo(Fraction.of(4));
        assertThat(expr.terms().get(0).coefficient()).isEqualTo(Fraction.of(2));
    }

    @Test
    @DisplayName("Division by zero throws")
    void divisionByZeroThrows() {
        Expression expr = Expression.constant(10);
        assertThatThrownBy(() -> expr.divide(0))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    @DisplayName("Negate expression")
    void negateExpression() {
        Variable x = new Variable("x");
        Expression expr = Expression.variable(x).plus(10).negate();

        assertThat(expr.constant()).isEqualTo(Fraction.of(-10));
        assertThat(expr.terms().get(0).coefficient()).isEqualTo(Fraction.NEG_ONE);
    }

    @Test
    @DisplayName("Create equality constraint")
    void createEqualityConstraint() {
        Variable x = new Variable("x");
        CassowaryConstraint c = Expression.variable(x)
                .equalTo(100, Strength.REQUIRED);

        assertThat(c.relation()).isEqualTo(Relation.EQ);
        assertThat(c.strength()).isEqualTo(Strength.REQUIRED);
    }

    @Test
    @DisplayName("Create less than or equal constraint")
    void createLessThanOrEqualConstraint() {
        Variable x = new Variable("x");
        CassowaryConstraint c = Expression.variable(x)
                .lessThanOrEqual(100, Strength.STRONG);

        assertThat(c.relation()).isEqualTo(Relation.LE);
        assertThat(c.strength()).isEqualTo(Strength.STRONG);
    }

    @Test
    @DisplayName("Create greater than or equal constraint")
    void createGreaterThanOrEqualConstraint() {
        Variable x = new Variable("x");
        CassowaryConstraint c = Expression.variable(x)
                .greaterThanOrEqual(50, Strength.MEDIUM);

        assertThat(c.relation()).isEqualTo(Relation.GE);
        assertThat(c.strength()).isEqualTo(Strength.MEDIUM);
    }

    @Test
    @DisplayName("Combine like terms")
    void combineLikeTerms() {
        Variable x = new Variable("x");
        Expression expr = Expression.variable(x).plus(Expression.variable(x).times(2));

        assertThat(expr.terms()).hasSize(1);
        assertThat(expr.terms().get(0).coefficient()).isEqualTo(Fraction.of(3));
    }

    @Test
    @DisplayName("Terms that cancel out are removed")
    void cancellingTerms() {
        Variable x = new Variable("x");
        Expression expr = Expression.variable(x).minus(Expression.variable(x));

        assertThat(expr.terms()).isEmpty();
        assertThat(expr.constant()).isEqualTo(Fraction.ZERO);
    }

    @Test
    @DisplayName("Plus variable convenience method")
    void plusVariableConvenience() {
        Variable x = new Variable("x");
        Variable y = new Variable("y");
        Expression expr = Expression.variable(x).plus(y);

        assertThat(expr.terms()).hasSize(2);
    }

    @Test
    @DisplayName("Minus variable convenience method")
    void minusVariableConvenience() {
        Variable x = new Variable("x");
        Variable y = new Variable("y");
        Expression expr = Expression.variable(x).minus(y);

        assertThat(expr.terms()).hasSize(2);
    }
}

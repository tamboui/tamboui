/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.tamboui.layout.Fraction;

/**
 * A linear expression of the form: constant + c1*v1 + c2*v2 + ... + cn*vn.
 *
 * <p>
 * Expressions are immutable and support fluent operations for building complex
 * expressions. They can be combined using arithmetic operations and converted
 * to constraints.
 *
 * <p>
 * This implementation uses {@link Fraction} for exact arithmetic, avoiding the
 * cumulative rounding errors that occur with floating-point.
 *
 * <p>
 * Example usage:
 * 
 * <pre>
 * Variable x = new Variable("x");
 * Variable y = new Variable("y");
 *
 * // Build: 2*x + 3*y + 10
 * Expression expr = Expression.variable(x).times(2).plus(Expression.variable(y).times(3))
 *         .plus(Expression.constant(10));
 *
 * // Create constraint: 2*x + 3*y + 10 == 0
 * CassowaryConstraint c = expr.equalTo(0, Strength.REQUIRED);
 * </pre>
 */
public final class Expression {

    private static final Expression ZERO = new Expression(Collections.emptyList(), Fraction.ZERO);

    private final List<Term> terms;
    private final Fraction constant;

    private Expression(List<Term> terms, Fraction constant) {
        this.terms = terms;
        this.constant = constant;
    }

    /**
     * Returns an expression representing zero.
     *
     * @return the zero expression
     */
    public static Expression zero() {
        return ZERO;
    }

    /**
     * Creates an expression with only a constant value.
     *
     * @param value
     *            the constant value
     * @return an expression representing the constant
     */
    public static Expression constant(Fraction value) {
        if (value.isZero()) {
            return ZERO;
        }
        return new Expression(Collections.emptyList(), value);
    }

    /**
     * Creates an expression with only a constant value.
     *
     * @param value
     *            the constant value
     * @return an expression representing the constant
     */
    public static Expression constant(long value) {
        return constant(Fraction.of(value));
    }

    /**
     * Creates an expression containing a single variable with coefficient 1.
     *
     * @param variable
     *            the variable
     * @return an expression representing the variable
     */
    public static Expression variable(Variable variable) {
        return new Expression(Collections.singletonList(new Term(variable, Fraction.ONE)),
                Fraction.ZERO);
    }

    /**
     * Creates an expression containing a single term.
     *
     * @param term
     *            the term
     * @return an expression containing the term
     */
    public static Expression term(Term term) {
        return new Expression(Collections.singletonList(term), Fraction.ZERO);
    }

    /**
     * Returns the terms of this expression.
     *
     * @return unmodifiable list of terms
     */
    public List<Term> terms() {
        return Collections.unmodifiableList(terms);
    }

    /**
     * Returns the constant part of this expression.
     *
     * @return the constant
     */
    public Fraction constant() {
        return constant;
    }

    /**
     * Returns true if this expression has no terms (only a constant).
     *
     * @return true if this is a constant expression
     */
    public boolean isConstant() {
        return terms.isEmpty();
    }

    /**
     * Adds another expression to this one.
     *
     * @param other
     *            the expression to add
     * @return a new expression representing the sum
     */
    public Expression plus(Expression other) {
        Map<Variable, Fraction> coefficients = new LinkedHashMap<>();

        for (Term term : this.terms) {
            coefficients.merge(term.variable(), term.coefficient(), Fraction::add);
        }
        for (Term term : other.terms) {
            coefficients.merge(term.variable(), term.coefficient(), Fraction::add);
        }

        List<Term> newTerms = new ArrayList<>();
        for (Map.Entry<Variable, Fraction> entry : coefficients.entrySet()) {
            if (!entry.getValue().isZero()) {
                newTerms.add(new Term(entry.getKey(), entry.getValue()));
            }
        }

        return new Expression(newTerms, this.constant.add(other.constant));
    }

    /**
     * Adds a constant to this expression.
     *
     * @param value
     *            the constant to add
     * @return a new expression with the added constant
     */
    public Expression plus(Fraction value) {
        return new Expression(terms, constant.add(value));
    }

    /**
     * Adds a constant to this expression.
     *
     * @param value
     *            the constant to add
     * @return a new expression with the added constant
     */
    public Expression plus(long value) {
        return plus(Fraction.of(value));
    }

    /**
     * Adds a variable to this expression.
     *
     * @param variable
     *            the variable to add
     * @return a new expression with the added variable
     */
    public Expression plus(Variable variable) {
        return plus(Expression.variable(variable));
    }

    /**
     * Adds a term to this expression.
     *
     * @param term
     *            the term to add
     * @return a new expression with the added term
     */
    public Expression plus(Term term) {
        return plus(Expression.term(term));
    }

    /**
     * Subtracts another expression from this one.
     *
     * @param other
     *            the expression to subtract
     * @return a new expression representing the difference
     */
    public Expression minus(Expression other) {
        return plus(other.negate());
    }

    /**
     * Subtracts a constant from this expression.
     *
     * @param value
     *            the constant to subtract
     * @return a new expression with the subtracted constant
     */
    public Expression minus(Fraction value) {
        return new Expression(terms, constant.subtract(value));
    }

    /**
     * Subtracts a constant from this expression.
     *
     * @param value
     *            the constant to subtract
     * @return a new expression with the subtracted constant
     */
    public Expression minus(long value) {
        return minus(Fraction.of(value));
    }

    /**
     * Subtracts a variable from this expression.
     *
     * @param variable
     *            the variable to subtract
     * @return a new expression with the subtracted variable
     */
    public Expression minus(Variable variable) {
        return minus(Expression.variable(variable));
    }

    /**
     * Multiplies this expression by a scalar.
     *
     * @param coefficient
     *            the scalar to multiply by
     * @return a new scaled expression
     */
    public Expression times(Fraction coefficient) {
        if (coefficient.isZero()) {
            return ZERO;
        }
        if (coefficient.equals(Fraction.ONE)) {
            return this;
        }

        List<Term> newTerms = new ArrayList<>(terms.size());
        for (Term term : terms) {
            newTerms.add(term.times(coefficient));
        }
        return new Expression(newTerms, constant.multiply(coefficient));
    }

    /**
     * Multiplies this expression by a scalar.
     *
     * @param coefficient
     *            the scalar to multiply by
     * @return a new scaled expression
     */
    public Expression times(long coefficient) {
        return times(Fraction.of(coefficient));
    }

    /**
     * Divides this expression by a scalar.
     *
     * @param divisor
     *            the scalar to divide by
     * @return a new divided expression
     * @throws ArithmeticException
     *             if divisor is zero
     */
    public Expression divide(Fraction divisor) {
        if (divisor.isZero()) {
            throw new ArithmeticException("Cannot divide by zero");
        }
        return times(divisor.reciprocal());
    }

    /**
     * Divides this expression by a scalar.
     *
     * @param divisor
     *            the scalar to divide by
     * @return a new divided expression
     * @throws ArithmeticException
     *             if divisor is zero
     */
    public Expression divide(long divisor) {
        return divide(Fraction.of(divisor));
    }

    /**
     * Negates this expression.
     *
     * @return a new negated expression
     */
    public Expression negate() {
        return times(Fraction.NEG_ONE);
    }

    /**
     * Creates an equality constraint: this == other.
     *
     * @param other
     *            the right-hand side expression
     * @param strength
     *            the constraint strength
     * @return a new equality constraint
     */
    public CassowaryConstraint equalTo(Expression other, Strength strength) {
        return new CassowaryConstraint(this.minus(other), Relation.EQ, strength);
    }

    /**
     * Creates an equality constraint: this == value.
     *
     * @param value
     *            the right-hand side constant
     * @param strength
     *            the constraint strength
     * @return a new equality constraint
     */
    public CassowaryConstraint equalTo(Fraction value, Strength strength) {
        return equalTo(Expression.constant(value), strength);
    }

    /**
     * Creates an equality constraint: this == value.
     *
     * @param value
     *            the right-hand side constant
     * @param strength
     *            the constraint strength
     * @return a new equality constraint
     */
    public CassowaryConstraint equalTo(long value, Strength strength) {
        return equalTo(Fraction.of(value), strength);
    }

    /**
     * Creates a less-than-or-equal constraint: this &lt;= other.
     *
     * @param other
     *            the right-hand side expression
     * @param strength
     *            the constraint strength
     * @return a new inequality constraint
     */
    public CassowaryConstraint lessThanOrEqual(Expression other, Strength strength) {
        return new CassowaryConstraint(this.minus(other), Relation.LE, strength);
    }

    /**
     * Creates a less-than-or-equal constraint: this &lt;= value.
     *
     * @param value
     *            the right-hand side constant
     * @param strength
     *            the constraint strength
     * @return a new inequality constraint
     */
    public CassowaryConstraint lessThanOrEqual(Fraction value, Strength strength) {
        return lessThanOrEqual(Expression.constant(value), strength);
    }

    /**
     * Creates a less-than-or-equal constraint: this &lt;= value.
     *
     * @param value
     *            the right-hand side constant
     * @param strength
     *            the constraint strength
     * @return a new inequality constraint
     */
    public CassowaryConstraint lessThanOrEqual(long value, Strength strength) {
        return lessThanOrEqual(Fraction.of(value), strength);
    }

    /**
     * Creates a greater-than-or-equal constraint: this &gt;= other.
     *
     * @param other
     *            the right-hand side expression
     * @param strength
     *            the constraint strength
     * @return a new inequality constraint
     */
    public CassowaryConstraint greaterThanOrEqual(Expression other, Strength strength) {
        return new CassowaryConstraint(this.minus(other), Relation.GE, strength);
    }

    /**
     * Creates a greater-than-or-equal constraint: this &gt;= value.
     *
     * @param value
     *            the right-hand side constant
     * @param strength
     *            the constraint strength
     * @return a new inequality constraint
     */
    public CassowaryConstraint greaterThanOrEqual(Fraction value, Strength strength) {
        return greaterThanOrEqual(Expression.constant(value), strength);
    }

    /**
     * Creates a greater-than-or-equal constraint: this &gt;= value.
     *
     * @param value
     *            the right-hand side constant
     * @param strength
     *            the constraint strength
     * @return a new inequality constraint
     */
    public CassowaryConstraint greaterThanOrEqual(long value, Strength strength) {
        return greaterThanOrEqual(Fraction.of(value), strength);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Expression)) {
            return false;
        }
        Expression that = (Expression) o;
        return constant.equals(that.constant) && terms.equals(that.terms);
    }

    @Override
    public int hashCode() {
        int result = terms.hashCode();
        result = 31 * result + constant.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Term term : terms) {
            if (!first) {
                if (!term.coefficient().isNegative()) {
                    sb.append(" + ");
                } else {
                    sb.append(" - ");
                }
                Fraction absCoeff = term.coefficient().abs();
                sb.append(absCoeff.equals(Fraction.ONE)
                        ? term.variable().toString()
                        : absCoeff + "*" + term.variable());
            } else {
                sb.append(term);
                first = false;
            }
        }

        if (!constant.isZero() || terms.isEmpty()) {
            if (!first) {
                if (!constant.isNegative()) {
                    sb.append(" + ");
                } else {
                    sb.append(" - ");
                }
                sb.append(constant.abs());
            } else {
                sb.append(constant);
            }
        }

        return sb.toString();
    }
}

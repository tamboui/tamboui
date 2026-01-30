/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

import dev.tamboui.layout.Fraction;

/**
 * A term in a linear expression, representing a coefficient multiplied by a
 * variable.
 *
 * <p>
 * Terms are the building blocks of linear expressions. A term consists of a
 * variable and a coefficient, representing the product
 * {@code coefficient * variable}.
 *
 * <p>
 * This implementation uses {@link Fraction} for exact arithmetic, avoiding the
 * cumulative rounding errors that occur with floating-point.
 */
public final class Term {

    private final Variable variable;
    private final Fraction coefficient;

    /**
     * Creates a new term with the given variable and coefficient.
     *
     * @param variable
     *            the variable
     * @param coefficient
     *            the coefficient
     */
    public Term(Variable variable, Fraction coefficient) {
        if (variable == null) {
            throw new IllegalArgumentException("Variable cannot be null");
        }
        if (coefficient == null) {
            throw new IllegalArgumentException("Coefficient cannot be null");
        }
        this.variable = variable;
        this.coefficient = coefficient;
    }

    /**
     * Creates a new term with coefficient 1.
     *
     * @param variable
     *            the variable
     */
    public Term(Variable variable) {
        this(variable, Fraction.ONE);
    }

    /**
     * Returns the variable of this term.
     *
     * @return the variable
     */
    public Variable variable() {
        return variable;
    }

    /**
     * Returns the coefficient of this term.
     *
     * @return the coefficient
     */
    public Fraction coefficient() {
        return coefficient;
    }

    /**
     * Returns a new term with the coefficient negated.
     *
     * @return a term with the negated coefficient
     */
    public Term negate() {
        return new Term(variable, coefficient.negate());
    }

    /**
     * Returns a new term with the coefficient multiplied by the given factor.
     *
     * @param factor
     *            the factor to multiply by
     * @return a new term with the scaled coefficient
     */
    public Term times(Fraction factor) {
        return new Term(variable, coefficient.multiply(factor));
    }

    /**
     * Converts this term to an expression.
     *
     * @return an expression containing only this term
     */
    public Expression toExpression() {
        return Expression.term(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Term)) {
            return false;
        }
        Term term = (Term) o;
        return coefficient.equals(term.coefficient) && variable.equals(term.variable);
    }

    @Override
    public int hashCode() {
        int result = variable.hashCode();
        result = 31 * result + coefficient.hashCode();
        return result;
    }

    @Override
    public String toString() {
        if (coefficient.equals(Fraction.ONE)) {
            return variable.toString();
        }
        if (coefficient.equals(Fraction.NEG_ONE)) {
            return "-" + variable;
        }
        return coefficient + "*" + variable;
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

/**
 * A term in a linear expression, representing a coefficient multiplied by a variable.
 *
 * <p>Terms are the building blocks of linear expressions. A term consists of
 * a variable and a coefficient, representing the product {@code coefficient * variable}.
 */
public final class Term {

    private final Variable variable;
    private final double coefficient;

    /**
     * Creates a new term with the given variable and coefficient.
     *
     * @param variable    the variable
     * @param coefficient the coefficient
     */
    public Term(Variable variable, double coefficient) {
        if (variable == null) {
            throw new IllegalArgumentException("Variable cannot be null");
        }
        this.variable = variable;
        this.coefficient = coefficient;
    }

    /**
     * Creates a new term with coefficient 1.
     *
     * @param variable the variable
     */
    public Term(Variable variable) {
        this(variable, 1.0);
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
    public double coefficient() {
        return coefficient;
    }

    /**
     * Returns a new term with the coefficient negated.
     *
     * @return a term with the negated coefficient
     */
    public Term negate() {
        return new Term(variable, -coefficient);
    }

    /**
     * Returns a new term with the coefficient multiplied by the given factor.
     *
     * @param factor the factor to multiply by
     * @return a new term with the scaled coefficient
     */
    public Term times(double factor) {
        return new Term(variable, coefficient * factor);
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
        return Double.compare(term.coefficient, coefficient) == 0
                && variable.equals(term.variable);
    }

    @Override
    public int hashCode() {
        int result = variable.hashCode();
        result = 31 * result + Double.hashCode(coefficient);
        return result;
    }

    @Override
    public String toString() {
        if (coefficient == 1.0) {
            return variable.toString();
        }
        if (coefficient == -1.0) {
            return "-" + variable;
        }
        return coefficient + "*" + variable;
    }
}

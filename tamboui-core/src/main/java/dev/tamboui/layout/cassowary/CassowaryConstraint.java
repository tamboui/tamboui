/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

/**
 * A constraint in the Cassowary system.
 *
 * <p>A constraint represents a linear relationship between variables with an
 * associated strength. The constraint has the form:
 * <pre>
 * expression relation 0
 * </pre>
 * where the relation is one of ==, &lt;=, or &gt;=.
 *
 * <p>Constraints should be created using the fluent API on {@link Expression}:
 * <pre>
 * // x + y == 100
 * Expression.variable(x).plus(y).equalTo(100, Strength.REQUIRED)
 *
 * // x >= 10
 * Expression.variable(x).greaterThanOrEqual(10, Strength.STRONG)
 * </pre>
 */
public final class CassowaryConstraint {

    private final Expression expression;
    private final Relation relation;
    private final Strength strength;

    /**
     * Creates a new constraint.
     *
     * <p>Typically constraints are created through the {@link Expression} API
     * rather than using this constructor directly.
     *
     * @param expression the linear expression (left-hand side)
     * @param relation   the constraint relation (==, &lt;=, &gt;=)
     * @param strength   the constraint strength/priority
     */
    public CassowaryConstraint(Expression expression, Relation relation, Strength strength) {
        if (expression == null) {
            throw new IllegalArgumentException("Expression cannot be null");
        }
        if (relation == null) {
            throw new IllegalArgumentException("Relation cannot be null");
        }
        if (strength == null) {
            throw new IllegalArgumentException("Strength cannot be null");
        }
        this.expression = expression;
        this.relation = relation;
        this.strength = strength;
    }

    /**
     * Returns the expression (left-hand side) of this constraint.
     *
     * <p>The constraint is satisfied when {@code expression relation 0}.
     *
     * @return the constraint expression
     */
    public Expression expression() {
        return expression;
    }

    /**
     * Returns the relation type of this constraint.
     *
     * @return the relation (EQ, LE, or GE)
     */
    public Relation relation() {
        return relation;
    }

    /**
     * Returns the strength of this constraint.
     *
     * @return the constraint strength
     */
    public Strength strength() {
        return strength;
    }

    /**
     * Returns true if this is a required constraint.
     *
     * @return true if the constraint must be satisfied
     */
    public boolean isRequired() {
        return strength.isRequired();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CassowaryConstraint)) {
            return false;
        }
        CassowaryConstraint that = (CassowaryConstraint) o;
        return expression.equals(that.expression)
                && relation == that.relation
                && strength.equals(that.strength);
    }

    @Override
    public int hashCode() {
        int result = expression.hashCode();
        result = 31 * result + relation.hashCode();
        result = 31 * result + strength.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return expression + " " + relation.symbol() + " 0 [" + strength + "]";
    }
}

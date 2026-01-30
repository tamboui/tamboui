/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

import java.util.concurrent.atomic.AtomicLong;

import dev.tamboui.layout.Fraction;

/**
 * A variable in the Cassowary constraint system.
 *
 * <p>
 * Variables represent unknown values to be solved for, such as widget positions
 * or sizes. Each variable has a unique identifier and an optional name for
 * debugging purposes.
 *
 * <p>
 * Variables use identity semantics - two variables are equal only if they are
 * the same instance.
 */
public final class Variable {

    private static final AtomicLong ID_GENERATOR = new AtomicLong();

    private final long id;
    private final String name;

    /**
     * Creates a new variable with the given name.
     *
     * @param name
     *            the variable name (for debugging)
     */
    public Variable(String name) {
        this.id = ID_GENERATOR.incrementAndGet();
        this.name = name != null ? name : "";
    }

    /**
     * Creates a new variable with no name.
     */
    public Variable() {
        this("");
    }

    /**
     * Returns the unique identifier of this variable.
     *
     * @return the variable ID
     */
    public long id() {
        return id;
    }

    /**
     * Returns the name of this variable.
     *
     * @return the variable name, or empty string if unnamed
     */
    public String name() {
        return name;
    }

    /**
     * Creates an expression containing only this variable with coefficient 1.
     *
     * @return an expression representing this variable
     */
    public Expression toExpression() {
        return Expression.variable(this);
    }

    /**
     * Creates a term with this variable and the given coefficient.
     *
     * @param coefficient
     *            the coefficient to multiply by
     * @return a term representing coefficient * this
     */
    public Term times(Fraction coefficient) {
        return new Term(this, coefficient);
    }

    /**
     * Creates a term with this variable and the given coefficient.
     *
     * @param coefficient
     *            the coefficient to multiply by
     * @return a term representing coefficient * this
     */
    public Term times(long coefficient) {
        return new Term(this, Fraction.of(coefficient));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Variable)) {
            return false;
        }
        Variable variable = (Variable) o;
        return id == variable.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        if (name.isEmpty()) {
            return "v" + id;
        }
        return name + "(" + id + ")";
    }
}

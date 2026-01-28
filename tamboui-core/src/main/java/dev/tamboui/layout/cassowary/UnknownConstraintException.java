/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

/**
 * Thrown when attempting to remove a constraint that does not exist in the solver.
 */
public final class UnknownConstraintException extends SolverException {

    /** The constraint that was not found. */
    private final CassowaryConstraint constraint;

    /**
     * Creates a new exception for the unknown constraint.
     *
     * @param constraint the constraint that was not found
     */
    public UnknownConstraintException(CassowaryConstraint constraint) {
        super("Constraint not found in solver: " + constraint);
        this.constraint = constraint;
    }

    /**
     * Returns the unknown constraint.
     *
     * @return the constraint that was not present
     */
    public CassowaryConstraint constraint() {
        return constraint;
    }
}

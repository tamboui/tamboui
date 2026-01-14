/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

/**
 * Thrown when attempting to add a constraint that already exists in the solver.
 */
public final class DuplicateConstraintException extends SolverException {

    private final CassowaryConstraint constraint;

    /**
     * Creates a new exception for the duplicate constraint.
     *
     * @param constraint the duplicate constraint
     */
    public DuplicateConstraintException(CassowaryConstraint constraint) {
        super("Constraint already exists in solver: " + constraint);
        this.constraint = constraint;
    }

    /**
     * Returns the duplicate constraint.
     *
     * @return the constraint that was already present
     */
    public CassowaryConstraint constraint() {
        return constraint;
    }
}

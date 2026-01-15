/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

/**
 * Thrown when a required constraint cannot be satisfied.
 *
 * <p>This exception indicates that the constraint system is inconsistent.
 * The conflicting constraint cannot be added without violating other
 * required constraints.
 */
public final class UnsatisfiableConstraintException extends SolverException {

    private final CassowaryConstraint constraint;

    /**
     * Creates a new exception for the unsatisfiable constraint.
     *
     * @param constraint the constraint that cannot be satisfied
     */
    public UnsatisfiableConstraintException(CassowaryConstraint constraint) {
        super("Required constraint cannot be satisfied: " + constraint);
        this.constraint = constraint;
    }

    /**
     * Returns the unsatisfiable constraint.
     *
     * @return the constraint that could not be added
     */
    public CassowaryConstraint constraint() {
        return constraint;
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

/**
 * Thrown when an internal solver error occurs.
 *
 * <p>This exception indicates a bug in the solver implementation
 * and should not occur during normal operation.
 */
public final class InternalSolverException extends SolverException {

    /**
     * Creates a new internal solver exception.
     *
     * @param message description of the internal error
     */
    public InternalSolverException(String message) {
        super("Internal solver error: " + message);
    }
}

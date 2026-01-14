/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

/**
 * Base exception for Cassowary solver errors.
 */
public class SolverException extends RuntimeException {

    /**
     * Creates a new solver exception with the given message.
     *
     * @param message the error message
     */
    public SolverException(String message) {
        super(message);
    }

    /**
     * Creates a new solver exception with the given message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public SolverException(String message, Throwable cause) {
        super(message, cause);
    }
}

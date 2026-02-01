/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.error;

/**
 * Abstract base exception for all TamboUI framework errors.
 * <p>
 * This exception serves as the abstract base class for all TamboUI-specific exceptions.
 * It provides a consistent exception hierarchy for the framework.
 * <p>
 * All TamboUI exceptions extend this class, which in turn extends {@link RuntimeException}
 * to provide unchecked exception behavior.
 * 
 * Intent is to have usecase specific exceptions while still allow for general catching of framework errors.
 *
 */
public abstract class TamboUIException extends RuntimeException {

    /**
     * Creates a new TamboUI exception with the given message.
     *
     * @param message the error message
     */
    protected TamboUIException(String message) {
        super(message);
    }

    /**
     * Creates a new TamboUI exception with the given message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    protected TamboUIException(String message, Throwable cause) {
        super(message, cause);
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.pilot;

/**
 * Exception thrown when an element cannot be found by ID.
 */
public class ElementNotFoundException extends Exception {
    /**
     * Creates a new exception with the given message.
     *
     * @param message the error message
     */
    public ElementNotFoundException(String message) {
        super(message);
    }
}

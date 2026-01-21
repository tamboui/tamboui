/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.cascade;

/**
 * Defines behavior when encountering unknown CSS properties.
 * <p>
 * An unknown property is one that is not registered in
 * {@link dev.tamboui.style.StandardProperties}.
 */
public enum UnknownPropertyBehavior {
    /**
     * Silently ignore unknown properties.
     * This is the default behavior for production use.
     */
    IGNORE,

    /**
     * Log a warning for unknown properties.
     * Useful for debugging CSS issues during development.
     */
    WARN,

    /**
     * Throw an exception when an unknown property is encountered.
     * Useful for strict validation during testing.
     */
    FAIL
}

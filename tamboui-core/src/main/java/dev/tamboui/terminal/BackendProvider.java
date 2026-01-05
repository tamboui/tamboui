/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.IOException;

/**
 * Service provider interface for creating {@link Backend} instances.
 * <p>
 * Implementations of this interface should be registered via the
 * Java {@link java.util.ServiceLoader} mechanism by creating a file
 * {@code META-INF/services/terminal.dev.tamboui.BackendProvider} containing
 * the fully qualified class name of the implementation.
 * <p>
 * Applications should include exactly one backend provider on the classpath
 * (e.g., tamboui-jline).
 *
 * @see BackendFactory
 * @see Backend
 */
public interface BackendProvider {

    /**
     * Creates a new backend instance.
     *
     * @return a new backend instance
     * @throws IOException if the backend cannot be created
     */
    Backend create() throws IOException;
}

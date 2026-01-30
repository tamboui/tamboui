/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.IOException;

/**
 * Service provider interface for creating {@link Backend} instances.
 * <p>
 * Implementations of this interface should be registered via the Java
 * {@link java.util.ServiceLoader} mechanism by creating a file
 * {@code META-INF/services/dev.tamboui.terminal.BackendProvider} containing the
 * fully qualified class name of the implementation.
 * <p>
 * Applications should include exactly one backend provider on the classpath
 * (e.g., tamboui-jline). If multiple providers are present, use the
 * {@code tamboui.backend} system property or {@code TAMBOUI_BACKEND}
 * environment variable to specify which provider to use (by simple name or
 * fully qualified class name).
 *
 * @see BackendFactory
 * @see Backend
 */
public interface BackendProvider {

    /**
     * Returns a simple identifier for this backend provider.
     * <p>
     * This name is used when selecting a provider via the {@code tamboui.backend}
     * system property or {@code TAMBOUI_BACKEND} environment variable. The default
     * implementation derives a name from the class name by removing
     * "BackendProvider" suffix and converting to lowercase.
     * <p>
     * Examples:
     * <ul>
     * <li>{@code JLineBackendProvider} → "jline"</li>
     * <li>{@code PanamaBackendProvider} → "panama"</li>
     * </ul>
     *
     * @return a simple identifier for this provider (e.g., "jline", "panama")
     */
    default String name() {
        String className = getClass().getSimpleName();
        if (className.endsWith("BackendProvider")) {
            return className.substring(0, className.length() - "BackendProvider".length())
                    .toLowerCase();
        }
        return className.toLowerCase();
    }

    /**
     * Creates a new backend instance.
     *
     * @return a new backend instance
     * @throws IOException
     *             if the backend cannot be created
     */
    Backend create() throws IOException;
}

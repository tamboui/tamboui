/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Factory for creating {@link Backend} instances using the {@link ServiceLoader} mechanism.
 * <p>
 * This factory discovers {@link BackendProvider} implementations on the classpath
 * and uses them to create backend instances. Applications should include exactly
 * one backend provider on the classpath (e.g., tamboui-jline).
 *
 * @see BackendProvider
 * @see Backend
 */
public final class BackendFactory {

    private BackendFactory() {
        // Utility class
    }

    /**
     * Creates a new backend instance using the discovered provider.
     * <p>
     * This method expects exactly one {@link BackendProvider} to be present
     * on the classpath. If none or multiple providers are found, an exception is thrown.
     *
     * @return a new backend instance
     * @throws IOException if backend creation fails
     * @throws IllegalStateException if no provider is found or multiple providers are found
     */
    public static Backend create() throws IOException {
        ServiceLoader<BackendProvider> loader = ServiceLoader.load(BackendProvider.class);
        List<BackendProvider> providers = new ArrayList<>();
        loader.forEach(providers::add);

        if (providers.isEmpty()) {
            throw new IllegalStateException(
                "No BackendProvider found on classpath. " +
                "Add a backend dependency such as tamboui-jline."
            );
        }

        if (providers.size() > 1) {
            throw new IllegalStateException(
                "Multiple BackendProviders found on classpath: " + providers +
                ". Include only one backend dependency."
            );
        }

        return providers.get(0).create();
    }
}

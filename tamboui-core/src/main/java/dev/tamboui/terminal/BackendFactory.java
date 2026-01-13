/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import dev.tamboui.internal.record.RecordingBackend;
import dev.tamboui.internal.record.RecordingConfig;

import java.io.IOException;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Factory for creating {@link Backend} instances using the {@link ServiceLoader} mechanism.
 * <p>
 * This factory discovers {@link BackendProvider} implementations on the classpath
 * and uses them to create backend instances. When multiple providers are available,
 * they are tried in order until one successfully creates a backend.
 * <p>
 * The provider order can be explicitly controlled via a comma-separated list
 * (e.g., "panama,jline") in the system property or environment variable.
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
     * This method discovers {@link BackendProvider} implementations on the classpath
     * and selects one based on the following priority:
     * <ol>
     *   <li>System property {@code tamboui.backend} (if set)</li>
     *   <li>Environment variable {@code TAMBOUI_BACKEND} (if set)</li>
     *   <li>Auto-discovery via ServiceLoader</li>
     * </ol>
     * <p>
     * The provider can be specified by:
     * <ul>
     *   <li>Simple name (e.g., "jline", "panama") - matches the provider's {@link BackendProvider#name()}</li>
     *   <li>Fully qualified class name (e.g., "dev.tamboui.backend.jline.JLineBackendProvider")</li>
     *   <li>Comma-separated list (e.g., "panama,jline") - tries each in order until one succeeds</li>
     * </ul>
     * <p>
     * Providers are tried in order until one successfully creates a backend. If a provider
     * fails (throws an exception), the next provider is attempted. This applies both to
     * explicitly specified providers and auto-discovered ones.
     *
     * @return a new backend instance
     * @throws IOException if backend creation fails
     * @throws IllegalStateException if no provider is found or all providers fail
     */
    public static Backend create() throws IOException {
        // Check system property first, then environment variable
        String userSelectedProvider = System.getProperty("tamboui.backend");
        if (userSelectedProvider == null || userSelectedProvider.isEmpty()) {
            userSelectedProvider = System.getenv("TAMBOUI_BACKEND");
        }

        // Load all available providers
        ServiceLoader<BackendProvider> loader = ServiceLoader.load(BackendProvider.class);
        List<BackendProvider> allProviders = StreamSupport.stream(loader.spliterator(), false)
                .collect(Collectors.toList());

        List<BackendProvider> providers = (userSelectedProvider != null && !userSelectedProvider.isEmpty())
                ? resolveProviders(userSelectedProvider, allProviders)
                : allProviders;

        Backend backend = tryProviders(providers);

        // Check if recording is enabled and wrap the backend
        RecordingConfig recordingConfig = RecordingConfig.load();
        if (recordingConfig != null) {
            backend = new RecordingBackend(backend, recordingConfig);
        }

        return backend;
    }

    /**
     * Resolves providers from a user specification, returning them in the specified order.
     *
     * @param providerSpec the provider specification (may be comma-separated)
     * @param allProviders all available providers from ServiceLoader
     * @return list of matching providers in the specified order
     * @throws IllegalStateException if a specified provider is not found
     */
    private static List<BackendProvider> resolveProviders(String providerSpec, List<BackendProvider> allProviders) {
        List<BackendProvider> resolved = new java.util.ArrayList<>();
        for (String spec : providerSpec.split(",")) {
            String trimmedSpec = spec.trim();
            if (trimmedSpec.isEmpty()) {
                continue;
            }
            BackendProvider provider = allProviders.stream()
                    .filter(p -> p.name().equals(trimmedSpec))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No BackendProvider found on classpath for provider name" +
                            " '" + trimmedSpec + "'.\n" +
                            "Add a backend dependency such as tamboui-jline."
                    ));
            resolved.add(provider);
        }
        return resolved;
    }

    /**
     * Tries each provider in order until one successfully creates a backend.
     *
     * @param providers the providers to try
     * @return a new backend instance
     * @throws IllegalStateException if no provider succeeds
     */
    private static Backend tryProviders(List<BackendProvider> providers) {
        if (providers.isEmpty()) {
            throw new IllegalStateException(
                "No BackendProvider found on classpath.\n" +
                "Add a backend dependency such as tamboui-jline."
            );
        }

        StringBuilder errors = new StringBuilder();
        for (BackendProvider provider : providers) {
            try {
                return provider.create();
            } catch (Exception e) {
                if (errors.length() > 0) {
                    errors.append("\n");
                }
                errors.append("  ").append(provider.name()).append(": ").append(e.getMessage());
            }
        }

        throw new IllegalStateException(
            "All backend providers failed to create a backend.\n" +
            "Tried: " + formatAvailableProviders(providers) + "\n" +
            "Errors:\n" + errors
        );
    }

    /**
     * Formats a list of providers into a user-friendly string showing both names and class names.
     *
     * @param providers the list of providers
     * @return a formatted string listing available providers
     */
    private static String formatAvailableProviders(List<BackendProvider> providers) {
        return providers.stream()
            .map(p -> p.name() + " (" + p.getClass().getName() + ")")
            .collect(Collectors.joining("\n"));
    }
}


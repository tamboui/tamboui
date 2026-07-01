/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import dev.tamboui.internal.record.RecordingBackend;
import dev.tamboui.internal.record.RecordingConfig;
import dev.tamboui.util.SafeServiceLoader;

/**
 * Factory for creating {@link Backend} instances using the {@link java.util.ServiceLoader} mechanism.
 * 
 * This uses the {@link SafeServiceLoader} to load the providers and skip providers that fails to load.
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
     * Initializes recording if enabled via system properties.
     * <p>
     * Call this method early in your application (before any System.out usage)
     * to ensure all console output is captured when recording is enabled.
     * This is especially important for inline demos that print to System.out
     * before creating a Backend.
     * <p>
     * If recording is not enabled, this method does nothing.
     * If recording is already initialized, this method does nothing.
     */
    public static void initRecording() {
        RecordingConfig.load();
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
     * @throws IOException      if backend creation fails
     * @throws BackendException if no provider is found or all providers fail
     */
    public static Backend create() throws IOException {
        return create((ClassLoader) null);
    }

    /**
     * Creates a new backend instance, discovering providers using the given classloader.
     * <p>
     * Behaves like {@link #create()}, except that backend discovery uses {@code classLoader}
     * exclusively when it is non-null (deterministic for embedders that bundle the backend in a
     * known classloader). When {@code classLoader} is null, discovery falls back to the default
     * candidate classloaders (see {@link SafeServiceLoader#load(Class, ClassLoader, java.util.function.Consumer)}).
     *
     * @param classLoader the classloader to use exclusively for discovery, or null for the default
     * @return a new backend instance
     * @throws IOException      if backend creation fails
     * @throws BackendException if no provider is found or all providers fail
     */
    public static Backend create(ClassLoader classLoader) throws IOException {
        // Check system property first, then environment variable
        String userSelectedProvider = System.getProperty("tamboui.backend");
        if (userSelectedProvider == null || userSelectedProvider.isEmpty()) {
            userSelectedProvider = System.getenv("TAMBOUI_BACKEND");
        }

        // Load all available providers, capturing any that fail to instantiate so that a present
        // but broken backend is not mistaken for an absent one.
        List<Throwable> loadFailures = new ArrayList<>();
        List<BackendProvider> allProviders =
                SafeServiceLoader.load(BackendProvider.class, classLoader, loadFailures::add);

        List<BackendProvider> providers = (userSelectedProvider != null && !userSelectedProvider.isEmpty())
                ? resolveProviders(userSelectedProvider, allProviders, loadFailures)
                : allProviders;

        Backend backend = tryProviders(providers, loadFailures);

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
     * @param allProviders all successfully instantiated providers from ServiceLoader
     * @param loadFailures errors raised while discovering/instantiating providers (may be empty)
     * @return list of matching providers in the specified order
     * @throws BackendException if a requested provider is not usable
     */
    private static List<BackendProvider> resolveProviders(
            String providerSpec, List<BackendProvider> allProviders, List<Throwable> loadFailures) {
        List<BackendProvider> resolved = new ArrayList<>();
        for (String spec : providerSpec.split(",")) {
            String trimmedSpec = spec.trim();
            if (trimmedSpec.isEmpty()) {
                continue;
            }
            BackendProvider provider = allProviders.stream()
                    .filter(p -> p.name().equals(trimmedSpec))
                    .findFirst()
                    // A requested provider can be missing because it is absent, or because it is
                    // present on the classpath but failed to initialize. Only blame broken
                    // providers when there are no usable ones at all; otherwise an unrelated
                    // failure would wrongly claim that "none could be initialized" even though a
                    // working provider was discovered (the requested name is simply absent).
                    // Trade-off: if the requested provider itself is broken while a different one
                    // works (e.g. jline broken, panama fine), allProviders is non-empty so this
                    // reports "not found" rather than "broken". ServiceLoader does not tell us
                    // which class failed, so we cannot distinguish those cases here.
                    .orElseThrow(() -> (!loadFailures.isEmpty() && allProviders.isEmpty())
                            ? brokenProvidersException(loadFailures)
                            : new BackendException(
                                    "No BackendProvider found on classpath for provider name" +
                                            " '" + trimmedSpec + "'.\n" +
                                            "Add a backend dependency such as tamboui-jline3-backend "
                                            + "or tamboui-panama-backend."));
            resolved.add(provider);
        }
        return resolved;
    }

    /**
     * Tries each provider in order until one successfully creates a backend.
     *
     * @param providers    the providers to try
     * @param loadFailures errors raised while discovering/instantiating providers (may be empty)
     * @return a new backend instance
     * @throws BackendException if no provider succeeds
     */
    private static Backend tryProviders(List<BackendProvider> providers, List<Throwable> loadFailures) {
        if (providers.isEmpty()) {
            if (!loadFailures.isEmpty()) {
                throw brokenProvidersException(loadFailures);
            }
            throw new BackendException(
                    "No BackendProvider found on classpath.\n" +
                            "Add a backend dependency such as tamboui-jline3-backend or tamboui-panama-backend."
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

        throw new BackendException(
                "All backend providers failed to create a backend.\n" +
                        "Tried: " + formatAvailableProviders(providers) + "\n" +
                        "Errors:\n" + errors
        );
    }

    /**
     * Builds the exception describing providers that were discovered on the classpath but failed
     * to initialize, attaching the first failure as the cause and any remaining failures as
     * suppressed exceptions so their full stack traces are preserved for diagnostics.
     *
     * @param loadFailures the non-empty list of discovery/instantiation failures
     * @return a {@link BackendException} reporting the failures
     */
    private static BackendException brokenProvidersException(List<Throwable> loadFailures) {
        String detail = loadFailures.stream()
                .map(t -> "  " + t.getMessage())
                .collect(Collectors.joining("\n"));
        BackendException exception = new BackendException(
                "Found " + loadFailures.size() + " BackendProvider(s) on the classpath but none "
                        + "could be initialized (see cause).\nFailures:\n" + detail,
                loadFailures.get(0)
        );
        for (int i = 1; i < loadFailures.size(); i++) {
            exception.addSuppressed(loadFailures.get(i));
        }
        return exception;
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

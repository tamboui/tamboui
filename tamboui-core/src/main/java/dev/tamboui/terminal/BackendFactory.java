/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
     * Wraps a backend for Asciinema recording when recording is enabled via system properties.
     * <p>
     * {@link #create()} applies this automatically. Call it explicitly when you build a backend
     * yourself and pass it to {@code TuiConfig.Builder#backend(Backend)}, since that path bypasses
     * this factory and would otherwise silently ignore the {@code tamboui.record} properties.
     * <p>
     * If recording is not enabled, the backend is returned unchanged. The call is idempotent: a backend that is already
     * recording is returned as-is, so applying it twice does not stack wrappers.
     * <p>
     * Enablement is read from the {@code tamboui.record} system property on every call. This also changes
     * {@link #create()}, which previously wrapped whenever the config loaded: because the config is cached
     * process-wide, {@code create()} kept wrapping after the property had been cleared. Both paths now stop wrapping
     * as soon as the property goes away.
     *
     * @param  backend the backend to wrap
     * @return         a recording backend when recording is enabled, otherwise {@code backend}
     */
    public static Backend recordIfEnabled(Backend backend) {
        if (backend instanceof RecordingBackend) {
            return backend;
        }
        // Check the properties before load(), which returns its cached config without re-reading them
        if (!RecordingConfig.isEnabled()) {
            return backend;
        }
        RecordingConfig recordingConfig = RecordingConfig.load();
        return recordingConfig != null ? new RecordingBackend(backend, recordingConfig) : backend;
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
        String userSelectedProvider = getSelectedProviderSpec();

        // Load all available providers, capturing any that fail to instantiate so that a present
        // but broken backend is not mistaken for an absent one.
        List<Throwable> loadFailures = new ArrayList<>();
        List<BackendProvider> allProviders =
                SafeServiceLoader.load(BackendProvider.class, classLoader, loadFailures::add);

        List<BackendProvider> providers = userSelectedProvider != null
                ? resolveProviders(userSelectedProvider, allProviders, loadFailures)
                : allProviders;

        // Check if recording is enabled and wrap the backend
        return recordIfEnabled(tryProviders(providers, loadFailures));
    }

    /**
     * Returns the candidate backend provider that would be selected based on the current
     * configuration, without attempting to create a backend instance.
     * <p>
     * Selection follows the same priority as {@link #create()}:
     * <ol>
     *   <li>System property {@code tamboui.backend} (if set)</li>
     *   <li>Environment variable {@code TAMBOUI_BACKEND} (if set)</li>
     *   <li>First auto-discovered provider via ServiceLoader</li>
     * </ol>
     * <p>
     * When a comma-separated list is configured, the first matching provider on the classpath is returned.
     * Note that this represents the initial candidate; actual backend creation may still fail at runtime
     * (e.g. if no TTY is available) and fall back to subsequent providers if specified.
     *
     * @return the candidate provider, or {@link Optional#empty()} if no matching provider is available
     */
    public static Optional<BackendProvider> candidateProvider() {
        return candidateProvider((ClassLoader) null);
    }

    /**
     * Returns the candidate backend provider that would be selected using the given classloader,
     * without attempting to create a backend instance.
     * <p>
     * Behaves like {@link #candidateProvider()}, except that backend discovery uses {@code classLoader}
     * exclusively when non-null.
     *
     * @param classLoader the classloader to use exclusively for discovery, or null for default discovery
     * @return the candidate provider, or {@link Optional#empty()} if no matching provider is available
     */
    public static Optional<BackendProvider> candidateProvider(ClassLoader classLoader) {
        String userSelectedProvider = getSelectedProviderSpec();
        List<BackendProvider> allProviders =
                SafeServiceLoader.load(BackendProvider.class, classLoader, null);
        return resolveCandidate(userSelectedProvider, allProviders);
    }

    /**
     * Resolves the candidate provider from the given specification and available providers.
     *
     * @param providerSpec the provider specification (may be comma-separated, or null/empty for auto-discovery)
     * @param allProviders all available providers
     * @return the candidate provider, or {@link Optional#empty()} if none matched or none are available
     */
    static Optional<BackendProvider> resolveCandidate(String providerSpec, List<BackendProvider> allProviders) {
        if (allProviders == null || allProviders.isEmpty()) {
            return Optional.empty();
        }
        if (providerSpec == null || providerSpec.trim().isEmpty()) {
            return Optional.of(allProviders.get(0));
        }
        for (String spec : providerSpec.split(",")) {
            String trimmedSpec = spec.trim();
            if (trimmedSpec.isEmpty()) {
                continue;
            }
            for (BackendProvider provider : allProviders) {
                if (matches(provider, trimmedSpec)) {
                    return Optional.of(provider);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean matches(BackendProvider provider, String spec) {
        return provider.name().equals(spec) || provider.getClass().getName().equals(spec);
    }

    private static String getSelectedProviderSpec() {
        String userSelectedProvider = System.getProperty("tamboui.backend");
        if (userSelectedProvider == null || userSelectedProvider.trim().isEmpty()) {
            userSelectedProvider = System.getenv("TAMBOUI_BACKEND");
        }
        return (userSelectedProvider != null && !userSelectedProvider.trim().isEmpty())
                ? userSelectedProvider.trim()
                : null;
    }

    /**
     * Resolves providers from a user specification, returning them in the specified order.
     * Providers that appear in the specification but were not discovered (e.g. because
     * the backend jar targets a newer Java version) are silently skipped so that subsequent
     * entries in the comma-separated list can act as fallbacks.
     *
     * @param providerSpec the provider specification (may be comma-separated)
     * @param allProviders all successfully instantiated providers from ServiceLoader
     * @param loadFailures errors raised while discovering/instantiating providers (may be empty)
     * @return list of matching providers in the specified order
     * @throws BackendException if none of the specified providers were usable
     */
    private static List<BackendProvider> resolveProviders(
            String providerSpec, List<BackendProvider> allProviders, List<Throwable> loadFailures) {
        List<BackendProvider> resolved = new ArrayList<>();
        for (String spec : providerSpec.split(",")) {
            String trimmedSpec = spec.trim();
            if (trimmedSpec.isEmpty()) {
                continue;
            }
            allProviders.stream()
                    .filter(p -> matches(p, trimmedSpec))
                    .findFirst()
                    .ifPresent(resolved::add);
        }
        if (resolved.isEmpty()) {
            // A requested provider can be missing because it is absent, or because it is
            // present on the classpath but failed to initialize. Only blame broken
            // providers when there are no usable ones at all; otherwise an unrelated
            // failure would wrongly claim that "none could be initialized" even though a
            // working provider was discovered (the requested name is simply absent).
            if (!loadFailures.isEmpty() && allProviders.isEmpty()) {
                throw brokenProvidersException(loadFailures);
            }
            // Mixed case: a healthy provider exists, but the ones the user asked for may still
            // be broken rather than absent. ServiceLoader reports failed providers through
            // ServiceConfigurationError messages containing the provider class name, so failures
            // can be attributed to the requested spec entries.
            List<Throwable> attributed = failuresMentioning(providerSpec, loadFailures);
            if (!attributed.isEmpty()) {
                throw requestedProvidersBrokenException(providerSpec, attributed, allProviders);
            }
            throw new BackendException(
                    "No BackendProvider found on classpath for any of the specified providers: '" + providerSpec + "'.\n"
                            + "Available providers: " + formatAvailableProviders(allProviders) + "\n"
                            + loadFailureNote(loadFailures)
                            + "Add a backend dependency such as tamboui-jline3-backend or tamboui-panama-backend.");
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
     * Returns the load failures whose messages mention an entry of the provider specification.
     * ServiceLoader wraps a provider that fails to load in a {@link java.util.ServiceConfigurationError}
     * whose message contains the provider's fully qualified class name, which also embeds the
     * simple-name-derived provider name (see {@link BackendProvider#name()}).
     *
     * @param providerSpec the user-supplied provider specification (may be comma-separated)
     * @param loadFailures errors raised while discovering/instantiating providers
     * @return the failures attributable to the requested providers, possibly empty
     */
    private static List<Throwable> failuresMentioning(String providerSpec, List<Throwable> loadFailures) {
        List<Throwable> matches = new ArrayList<>();
        for (Throwable failure : loadFailures) {
            for (String spec : providerSpec.split(",")) {
                String needle = spec.trim().toLowerCase(Locale.ROOT);
                if (!needle.isEmpty() && mentions(failure, needle)) {
                    matches.add(failure);
                    break;
                }
            }
        }
        return matches;
    }

    /**
     * Formats a disclosure note for providers that were found on the classpath but dropped
     * because they failed to load, so they are never silently hidden from error messages
     * (e.g. when the user mistyped the name of a present-but-incompatible provider).
     *
     * @param loadFailures errors raised while discovering/instantiating providers
     * @return a note ending in a newline, or an empty string when there were no failures
     */
    private static String loadFailureNote(List<Throwable> loadFailures) {
        if (loadFailures.isEmpty()) {
            return "";
        }
        String detail = loadFailures.stream()
                .map(t -> "  " + t.getMessage())
                .collect(Collectors.joining("\n"));
        return "Note: " + loadFailures.size() + " additional provider(s) were found but "
                + "could not be loaded on this JVM:\n" + detail + "\n";
    }

    private static boolean mentions(Throwable failure, String needle) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            String message = t.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Builds the exception for requested providers that are present on the classpath but failed
     * to initialize while other, unrelated providers are healthy. Reports the real causes instead
     * of advising to add a dependency that is already present.
     *
     * @param providerSpec the user-supplied provider specification
     * @param attributed   the non-empty failures attributed to the requested providers
     * @param allProviders the successfully instantiated providers
     * @return a {@link BackendException} reporting the failures
     */
    private static BackendException requestedProvidersBrokenException(
            String providerSpec, List<Throwable> attributed, List<BackendProvider> allProviders) {
        String detail = attributed.stream()
                .map(t -> "  " + t.getMessage())
                .collect(Collectors.joining("\n"));
        BackendException exception = new BackendException(
                "BackendProvider(s) matching '" + providerSpec + "' were found on the classpath but "
                        + "could not be initialized (see cause).\nFailures:\n" + detail + "\n"
                        + "Available providers: " + formatAvailableProviders(allProviders) + "\n"
                        + "Use a compatible Java runtime, or append a working fallback to the list, e.g. "
                        + "-Dtamboui.backend=" + providerSpec + "," + allProviders.get(0).name(),
                attributed.get(0));
        for (int i = 1; i < attributed.size(); i++) {
            exception.addSuppressed(attributed.get(i));
        }
        return exception;
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

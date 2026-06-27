/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Consumer;

/**
 * ServiceLoader that is more resilient to errors by skipping broken providers
 * and continuing to try other providers.
 * 
 * It will skip providers that fail due to service configuration issues, newer
 * classfile versions,
 * or linkage problems (missing deps / binary incompatibilities), and continues
 * trying to
 * locate other providers. Has a stop-gap to prevent (theoretical possible)
 * infinite loop.
 * 
 * This works on Java 8+ relying on the ServiceLoader API that states:
 * "If an error is thrown then subsequent invocations of the iterator will make
 * a best effort to locate and instantiate the next available provider, but in
 * general such recovery cannot be guaranteed."
 *
 * 
 */
public final class SafeServiceLoader {

    private SafeServiceLoader() {
        // utility
    }

    // Defensive cap: prevents "best effort" from becoming an infinite loop
    private static final int MAX_CONSECUTIVE_ERRORS = 4;

    /**
     * Loads providers for the given service, skipping broken providers and continuing to try other providers.
     *
     * @param service the SPI interface/class
     * @param onError optional sink for errors encountered while iterating/instantiating providers
     * @param <S>     service type
     * @return list of successfully instantiated providers (no duplicates)
     */
    public static <S> List<S> load(Class<S> service, Consumer<Throwable> onError) {
        return load(service, null, onError);
    }

    /**
     * Loads providers for the given service, skipping broken providers and continuing to try other providers.
     *
     * @param service the SPI interface/class
     * @param <S>     service type
     * @return list of successfully instantiated providers
     */
    public static <S> List<S> load(Class<S> service) {
        return load(service, null, null);
    }

    /**
     * Loads providers for the given service across one or more classloaders.
     * <p>
     * When {@code classLoader} is non-null, discovery uses <em>only</em> that loader, so the
     * result is deterministic for callers that want to control which classloader is consulted.
     * When {@code classLoader} is null, discovery falls back to a de-duplicated, ordered set of
     * candidate loaders: the thread-context classloader, this class's own classloader and the
     * system classloader. This makes discovery robust in fat-jars, containers and app servers
     * where the thread-context classloader is frequently not the one that bundled the provider.
     * <p>
     * Successfully instantiated providers are de-duplicated by concrete class name (first
     * candidate loader wins). Errors are likewise forwarded to {@code onError} at most once per
     * distinct failure, so a broken provider visible to several candidate loaders is reported once.
     *
     * @param service     the SPI interface/class
     * @param classLoader the classloader to use exclusively, or null to use the candidate set
     * @param onError     optional sink for errors encountered while iterating/instantiating providers
     * @param <S>         service type
     * @return list of successfully instantiated providers (no duplicates)
     */
    public static <S> List<S> load(Class<S> service, ClassLoader classLoader, Consumer<Throwable> onError) {
        Objects.requireNonNull(service, "service");

        List<S> loaded = new ArrayList<S>();
        Set<String> seenClasses = new LinkedHashSet<String>();
        Set<String> seenFailures = new LinkedHashSet<String>();

        for (ClassLoader candidate : candidateLoaders(classLoader)) {
            loadFrom(service, candidate, onError, loaded, seenClasses, seenFailures);
        }

        return loaded;
    }

    /**
     * Builds the ordered, de-duplicated set of classloaders to consult.
     *
     * @param explicit the explicit loader to use exclusively, or null for the fallback set
     * @return the candidate loaders, never empty
     */
    private static Set<ClassLoader> candidateLoaders(ClassLoader explicit) {
        Set<ClassLoader> candidates = new LinkedHashSet<ClassLoader>();
        if (explicit != null) {
            candidates.add(explicit);
            return candidates;
        }
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        if (tccl != null) {
            candidates.add(tccl);
        }
        candidates.add(SafeServiceLoader.class.getClassLoader());
        ClassLoader system = ClassLoader.getSystemClassLoader();
        if (system != null) {
            candidates.add(system);
        }
        return candidates;
    }

    /**
     * Iterates providers from a single classloader, adding newly seen providers to {@code loaded}
     * and forwarding novel errors to {@code onError}. The consecutive-error cap resets per loader,
     * while {@code seenClasses} and {@code seenFailures} are shared across loaders to suppress
     * providers and failures already observed on an earlier candidate.
     */
    private static <S> void loadFrom(Class<S> service, ClassLoader classLoader, Consumer<Throwable> onError,
            List<S> loaded, Set<String> seenClasses, Set<String> seenFailures) {
        ServiceLoader<S> sl = ServiceLoader.load(service, classLoader);
        Iterator<S> it = sl.iterator();

        int consecutiveErrors = 0;

        while (true) {
            try {
                if (!it.hasNext()) {
                    break;
                }

                S provider = it.next(); // may throw
                consecutiveErrors = 0;

                if (seenClasses.add(provider.getClass().getName())) {
                    loaded.add(provider);
                }
            } catch (ServiceConfigurationError | LinkageError e) {
                if (onError != null && seenFailures.add(failureSignature(e))) {
                    onError.accept(e);
                }
                if (++consecutiveErrors > MAX_CONSECUTIVE_ERRORS) {
                    break;
                }
            }
        }
    }

    /**
     * Builds a stable signature for a failure so the same broken provider seen through several
     * candidate loaders is forwarded to {@code onError} only once.
     *
     * @param t the failure
     * @return a signature combining the throwable type and message
     */
    private static String failureSignature(Throwable t) {
        return t.getClass().getName() + ": " + t.getMessage();
    }
}
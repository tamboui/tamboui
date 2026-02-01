/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
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
        Objects.requireNonNull(service, "service");

        List<S> loaded = new ArrayList<S>();

        ServiceLoader<S> sl = ServiceLoader.load(service);
        Iterator<S> it = sl.iterator();

        int consecutiveErrors = 0;

        while (true) {
            try {
                if (!it.hasNext()) {
                    break;
                }

                S provider = it.next(); // may throw
                consecutiveErrors = 0;

                loaded.add(provider);
            } catch (ServiceConfigurationError | LinkageError  e) {
                if (onError != null) {
                    onError.accept(e);
                }
                if (++consecutiveErrors > MAX_CONSECUTIVE_ERRORS) {
                    break;
                }
            }
        }

        return loaded;
    }

    /**
     * Loads providers for the given service, skipping broken providers and continuing to try other providers.
     *
     * @param service the SPI interface/class
     * @param <S>     service type
     * @return list of successfully instantiated providers
     */
    public static <S> List<S> load(Class<S> service) {
        return load(service, null);
    }
}
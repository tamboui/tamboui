/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.layout.cassowary;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Thread-local cache for layout solver results.
 *
 * <p>This cache stores the results of Cassowary solver computations to avoid
 * redundant work when the same layout is computed multiple times per frame.
 * The cache uses LRU eviction to bound memory usage.
 *
 * <p>The cache is thread-local to avoid synchronization overhead, as TUI
 * rendering is typically single-threaded.
 */
public final class LayoutCache {
    private static final int MAX_SIZE = 256;
    private static final ThreadLocal<LayoutCache> INSTANCE =
        ThreadLocal.withInitial(() -> new LayoutCache(MAX_SIZE));

    private final LinkedHashMap<LayoutCacheKey, int[]> cache;

    private LayoutCache(int maxSize) {
        this.cache = new LinkedHashMap<LayoutCacheKey, int[]>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<LayoutCacheKey, int[]> eldest) {
                return size() > maxSize;
            }
        };
    }

    /**
     * Returns the thread-local layout cache instance.
     *
     * @return the layout cache for the current thread
     */
    public static LayoutCache instance() {
        return INSTANCE.get();
    }

    /**
     * Gets cached sizes for the given layout parameters, computing and caching if absent.
     *
     * @param constraints   the layout constraints
     * @param distributable the distributable space
     * @param spacing       the spacing between elements
     * @param flex          the flex mode
     * @param computer      supplier to compute sizes on cache miss
     * @return sizes array (cloned from cache)
     */
    public int[] computeIfAbsent(List<Constraint> constraints, int distributable, int spacing, Flex flex,
                                 Supplier<int[]> computer) {
        LayoutCacheKey key = new LayoutCacheKey(constraints, distributable, spacing, flex);
        // Avoid Map.computeIfAbsent - it has issues with access-order LinkedHashMap in Java 8
        // that can cause infinite loops or corruption during structural modification
        int[] cached = cache.get(key);
        if (cached == null) {
            cached = computer.get().clone();
            cache.put(key, cached);
        }
        return cached.clone();
    }

    /**
     * Clears the cache for the current thread.
     * This can be called at the end of a frame if desired.
     */
    public static void clearAll() {
        INSTANCE.remove();
    }
}

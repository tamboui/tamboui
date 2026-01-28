/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Small helpers to provide Java 9+ style collection factories while staying
 * compatible with Java 8.
 */
public final class CollectionUtil {

    private CollectionUtil() {
        // Utility class
    }

    /**
     * Returns an empty unmodifiable list.
     *
     * @param <T> the element type
     * @return an empty list
     */
    public static <T> List<T> listCopyOf() {
        return Collections.emptyList();
    }

    /**
     * Returns an unmodifiable list containing the given elements.
     *
     * @param <T>      the element type
     * @param elements the elements to include
     * @return an unmodifiable list
     */
    @SafeVarargs
    @SuppressWarnings("varargs") // caller-supplied array is defensively copied into an unmodifiable list
    public static <T> List<T> listCopyOf(T... elements) {
        Objects.requireNonNull(elements, "elements");
        if (elements.length == 0) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(elements)));
    }

    /**
     * Returns an unmodifiable list copied from the given collection.
     *
     * @param <T>    the element type
     * @param source the source collection
     * @return an unmodifiable list
     */
    public static <T> List<T> listCopyOf(Collection<? extends T> source) {
        Objects.requireNonNull(source, "source");
        if (source.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    /**
     * Collects a stream into a list.
     *
     * @param <T>    the element type
     * @param stream the source stream
     * @return a list containing the stream elements
     */
    public static <T> List<T> toList(Stream<T> stream) {
        Objects.requireNonNull(stream, "stream");
        return stream.collect(Collectors.toList());
    }

    /**
     * Returns an empty unmodifiable map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return an empty map
     */
    public static <K, V> Map<K, V> mapCopyOf() {
        return Collections.emptyMap();
    }

    /**
     * Returns an unmodifiable singleton map.
     *
     * @param <K>   the key type
     * @param <V>   the value type
     * @param key   the single key
     * @param value the single value
     * @return an unmodifiable singleton map
     */
    public static <K, V> Map<K, V> mapCopyOf(K key, V value) {
        Map<K, V> map = new HashMap<>(1);
        map.put(key, value);
        return Collections.unmodifiableMap(map);
    }

    /**
     * Returns an unmodifiable map copied from the given map.
     *
     * @param <K>    the key type
     * @param <V>    the value type
     * @param source the source map
     * @return an unmodifiable map
     */
    public static <K, V> Map<K, V> mapCopyOf(Map<? extends K, ? extends V> source) {
        Objects.requireNonNull(source, "source");
        if (source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(source));
    }
}

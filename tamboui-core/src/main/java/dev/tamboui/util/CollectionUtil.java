/*
 * Copyright (c) 2025 TamboUI Contributors
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

/// Small helpers to provide Java 9+ style collection factories while staying
/// compatible with Java 8.
public final class CollectionUtil {

    private CollectionUtil() {
        // Utility class
    }

    public static <T> List<T> listCopyOf() {
        return Collections.emptyList();
    }

    @SafeVarargs
    @SuppressWarnings("varargs") // caller-supplied array is defensively copied into an unmodifiable list
    public static <T> List<T> listCopyOf(T... elements) {
        Objects.requireNonNull(elements, "elements");
        if (elements.length == 0) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(elements)));
    }

    public static <T> List<T> listCopyOf(Collection<? extends T> source) {
        Objects.requireNonNull(source, "source");
        if (source.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(source));
    }

    public static <T> List<T> toList(Stream<T> stream) {
        Objects.requireNonNull(stream, "stream");
        return stream.collect(Collectors.toList());
    }

    public static <K, V> Map<K, V> mapCopyOf() {
        return Collections.emptyMap();
    }

    public static <K, V> Map<K, V> mapCopyOf(K key, V value) {
        Map<K, V> map = new HashMap<>(1);
        map.put(key, value);
        return Collections.unmodifiableMap(map);
    }

    public static <K, V> Map<K, V> mapCopyOf(Map<? extends K, ? extends V> source) {
        Objects.requireNonNull(source, "source");
        if (source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(source));
    }
}


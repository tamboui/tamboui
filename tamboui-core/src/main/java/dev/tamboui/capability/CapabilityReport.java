/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability;

import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Aggregated capability information from all discovered {@link CapabilityProvider}s.
 */
public final class CapabilityReport {
    private final Map<String, List<String>> environmentBySource;
    private final Map<String, List<String>> propertiesBySource;
    private final Map<String, Map<String, Object>> featuresBySource;

    CapabilityReport(
            Map<String, List<String>> environmentBySource,
            Map<String, List<String>> propertiesBySource,
            Map<String, Map<String, Object>> featuresBySource
    ) {
        this.environmentBySource = Collections.unmodifiableMap(new LinkedHashMap<>(environmentBySource));
        this.propertiesBySource = Collections.unmodifiableMap(new LinkedHashMap<>(propertiesBySource));
        this.featuresBySource = Collections.unmodifiableMap(new LinkedHashMap<>(featuresBySource));
    }

    /**
     * Environment entries grouped by provider source.
     * <p>
     * Each list entry is an environment variable name (value is resolved at print-time).
     *
     * @return unmodifiable map of source to environment variable names
     */
    public Map<String, List<String>> environmentBySource() {
        return environmentBySource;
    }

    /**
     * Java/system properties (and similar) grouped by provider source.
     * <p>
     * Each list entry is a property name (value is resolved at print-time).
     *
     * @return unmodifiable map of source to property names
     */
    public Map<String, List<String>> propertiesBySource() {
        return propertiesBySource;
    }

    /**
     * Free-form capability/features map, grouped by provider source.
     * <p>
     * Keys SHOULD use dotted notation to create stable namespaces, e.g. {@code image.protocol.kitty}.
     *
     * @return unmodifiable map of source to feature key-value pairs
     */
    public Map<String, Map<String, Object>> featuresBySource() {
        return featuresBySource;
    }

    /**
     * Looks up a feature value by source and key.
     *
     * @param source source id (e.g. {@code tamboui-core})
     * @param key    key within the source feature map
     * @return the feature value if present
     */
    public Optional<Object> feature(String source, String key) {
        if (source == null || key == null) {
            return Optional.empty();
        }
        Map<String, Object> m = featuresBySource.get(source);
        if (m == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(m.get(key));
    }

    /**
     * Looks up a feature by source and key, and returns it if it matches the requested type.
     *
     * @param source source id (e.g. {@code tamboui-core})
     * @param key    key within the source feature map (prefer dotted notation)
     * @param type         desired value type
     * @param <T>          value type
     * @return typed value if present and assignable to {@code type}
     */
    public <T> Optional<T> feature(String source, String key, Class<T> type) {
        if (type == null) {
            return Optional.empty();
        }
        Optional<Object> v = feature(source, key);
        if (!v.isPresent()) {
            return Optional.empty();
        }
        Object o = v.get();
        if (!type.isInstance(o)) {
            return Optional.empty();
        }
        return Optional.of(type.cast(o));
    }

    /**
     * Prints this capability report to the given stream.
     *
     * @param out the output stream
     */
    public void print(PrintStream out) {
        out.println("TamboUI capability report");
        out.println();

        for (String source : sources()) {
            List<String> env = environmentBySource.get(source);
            if (env != null && !env.isEmpty()) {
                out.println("== " + source + ":environment");
                for (String key : env) {
                    out.println(key + ": " + safeEnv(key));
                }
                out.println();
            }

            List<String> props = propertiesBySource.get(source);
            if (props != null && !props.isEmpty()) {
                out.println("== " + source + ":properties");
                for (String key : props) {
                    out.println(key + ": " + safeProperty(key));
                }
                out.println();
            }

            Map<String, Object> features = featuresBySource.get(source);
            if (features != null && !features.isEmpty()) {
                out.println("== " + source + ":features");
                for (Map.Entry<String, Object> entry : features.entrySet()) {
                    out.println(entry.getKey() + ": " + entry.getValue());
                }
                out.println();
            }
        }
    }

    private Set<String> sources() {
        Map<String, Boolean> all = new LinkedHashMap<>();
        for (String s : environmentBySource.keySet()) {
            all.put(s, Boolean.TRUE);
        }
        for (String s : propertiesBySource.keySet()) {
            all.put(s, Boolean.TRUE);
        }
        for (String s : featuresBySource.keySet()) {
            all.put(s, Boolean.TRUE);
        }
        return all.keySet();
    }

    private static String safeEnv(String key) {
        try {
            String v = System.getenv(key);
            return v == null ? "<unset>" : v;
        } catch (SecurityException e) {
            return "<restricted>";
        }
    }

    private static String safeProperty(String key) {
        try {
            String v = System.getProperty(key);
            return v == null ? "<unset>" : v;
        } catch (SecurityException e) {
            return "<restricted>";
        }
    }
}



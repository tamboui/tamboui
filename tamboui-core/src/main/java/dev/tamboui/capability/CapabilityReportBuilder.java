/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.capability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builder for assembling a {@link CapabilityReport}.
 */
public final class CapabilityReportBuilder {

    /** Creates a new empty builder. */
    public CapabilityReportBuilder() {
    }

    private final Map<String, List<String>> environmentBySource = new LinkedHashMap<>();
    private final Map<String, List<String>> propertiesBySource = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> featuresBySource = new LinkedHashMap<>();

    /**
     * Adds an environment key that should be printed (value resolved at print-time).
     * <p>
     * Prefer using environment variable names as keys.
     *
     * @param source the provider source name
     * @param key    the environment variable name
     * @return this builder
     */
    public CapabilityReportBuilder env(String source, String key) {
        Objects.requireNonNull(source, "source");
        if (key == null || key.isEmpty()) {
            return this;
        }
        environmentBySource
                .computeIfAbsent(source, s -> new ArrayList<String>())
                .add(key);
        return this;
    }

    /**
     * Adds a property key that should be printed (value resolved at print-time).
     * <p>
     * Prefer using property names as keys (e.g. {@code java.version}).
     *
     * @param source the provider source name
     * @param key    the system property name
     * @return this builder
     */
    public CapabilityReportBuilder property(String source, String key) {
        Objects.requireNonNull(source, "source");
        if (key == null || key.isEmpty()) {
            return this;
        }
        propertiesBySource
                .computeIfAbsent(source, s -> new ArrayList<String>())
                .add(key);
        return this;
    }

    /**
     * Adds a general capability/feature. Keys SHOULD use dotted notation to create stable namespaces.
     *
     * @param source the provider source name
     * @param key    the feature key (prefer dotted notation)
     * @param value  the feature value
     * @return this builder
     */
    public CapabilityReportBuilder feature(String source, String key, Object value) {
        Objects.requireNonNull(source, "source");
        if (key == null || key.isEmpty()) {
            return this;
        }
        featuresBySource
                .computeIfAbsent(source, s -> new LinkedHashMap<String, Object>())
                .put(key, value);
        return this;
    }

    /**
     * Builds the capability report from the accumulated data.
     *
     * @return the assembled capability report
     */
    public CapabilityReport build() {
        return new CapabilityReport(environmentBySource, propertiesBySource, featuresBySource);
    }
}



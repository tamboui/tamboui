/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A simple StylePropertyResolver for testing purposes. Allows setting property
 * values by name.
 */
public final class TestStylePropertyResolver implements StylePropertyResolver {

    private final Map<String, Object> properties = new HashMap<>();

    private TestStylePropertyResolver() {
    }

    /**
     * Creates a new builder for TestStylePropertyResolver.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a resolver with a single property.
     */
    public static TestStylePropertyResolver of(String name, Object value) {
        return builder().set(name, value).build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(PropertyDefinition<T> property) {
        Object value = properties.get(property.name());
        if (value != null) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }

    public static final class Builder {
        private final Map<String, Object> properties = new HashMap<>();

        private Builder() {
        }

        /**
         * Sets a property value.
         */
        public Builder set(String name, Object value) {
            properties.put(name, value);
            return this;
        }

        public TestStylePropertyResolver build() {
            TestStylePropertyResolver resolver = new TestStylePropertyResolver();
            resolver.properties.putAll(properties);
            return resolver;
        }
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.cascade;

import dev.tamboui.style.PropertyDefinition;

import java.util.*;

/**
 * A type-safe heterogeneous map for CSS property values.
 * <p>
 * This map uses {@link PropertyDefinition} objects as keys and stores values
 * of the corresponding types. The type safety is enforced at compile time
 * through the generic type parameter on PropertyDefinition.
 * <p>
 * Example usage:
 * <pre>{@code
 * TypedPropertyMap props = TypedPropertyMap.builder()
 *     .put(StandardProperties.COLOR, Color.RED)
 *     .put(StandardProperties.PADDING, Padding.of(1))
 *     .build();
 *
 * Optional<Color> color = props.get(StandardProperties.COLOR);
 * }</pre>
 */
public final class TypedPropertyMap {

    private static final TypedPropertyMap EMPTY = new TypedPropertyMap(Collections.emptyMap());

    private final Map<PropertyDefinition<?>, Object> values;

    private TypedPropertyMap(Map<PropertyDefinition<?>, Object> values) {
        this.values = values;
    }

    /**
     * Package-private constructor for creating a TypedPropertyMap from a pre-built map.
     * Used by CssStyleResolver for advanced inheritance scenarios.
     */
    TypedPropertyMap(Map<PropertyDefinition<?>, Object> values, boolean packagePrivate) {
        this.values = Collections.unmodifiableMap(values);
    }

    /**
     * Returns an empty property map.
     *
     * @return an empty property map
     */
    public static TypedPropertyMap empty() {
        return EMPTY;
    }

    /**
     * Creates a new builder for a typed property map.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Retrieves the value for the given property definition.
     *
     * @param property the property definition
     * @param <T>      the type of the property value
     * @return the property value, or empty if not set
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(PropertyDefinition<T> property) {
        Object value = values.get(property);
        return Optional.ofNullable((T) value);
    }

    /**
     * Returns true if this map contains a value for the given property.
     *
     * @param property the property definition
     * @return true if a value is present
     */
    public boolean contains(PropertyDefinition<?> property) {
        return values.containsKey(property);
    }

    /**
     * Returns true if this map is empty.
     *
     * @return true if no properties are set
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Returns the number of properties in this map.
     *
     * @return the property count
     */
    public int size() {
        return values.size();
    }

    /**
     * Returns all property definitions in this map.
     *
     * @return an unmodifiable set of property definitions
     */
    public Set<PropertyDefinition<?>> properties() {
        return values.keySet();
    }

    /**
     * Creates a new property map that uses this map's values, falling back to
     * the given fallback map for inherited properties that are not set in this map.
     * <p>
     * Only properties marked as {@link PropertyDefinition#isInheritable()} are
     * inherited from the fallback. Non-inheritable properties are not inherited.
     *
     * @param fallback the fallback map (typically from parent element)
     * @return a new map with fallback behavior for inheritable properties
     */
    public TypedPropertyMap withFallback(TypedPropertyMap fallback) {
        if (fallback == null || fallback.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            // Only inherit inheritable properties from fallback
            Map<PropertyDefinition<?>, Object> filtered = new HashMap<>();
            for (Map.Entry<PropertyDefinition<?>, Object> entry : fallback.values.entrySet()) {
                if (entry.getKey().isInheritable()) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
            if (filtered.isEmpty()) {
                return EMPTY;
            }
            return new TypedPropertyMap(Collections.unmodifiableMap(filtered));
        }

        Map<PropertyDefinition<?>, Object> merged = new HashMap<>();

        // First, add inheritable properties from fallback
        for (Map.Entry<PropertyDefinition<?>, Object> entry : fallback.values.entrySet()) {
            if (entry.getKey().isInheritable()) {
                merged.put(entry.getKey(), entry.getValue());
            }
        }

        // Then overlay with this map's properties (all properties)
        merged.putAll(this.values);

        return new TypedPropertyMap(Collections.unmodifiableMap(merged));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TypedPropertyMap that = (TypedPropertyMap) o;
        return values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("TypedPropertyMap{");
        boolean first = true;
        for (Map.Entry<PropertyDefinition<?>, Object> entry : values.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey().name()).append("=").append(entry.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Builder for TypedPropertyMap.
     */
    public static final class Builder {
        private final Map<PropertyDefinition<?>, Object> values = new HashMap<>();

        private Builder() {
        }

        /**
         * Puts a property value into the map.
         *
         * @param property the property definition
         * @param value    the property value
         * @param <T>      the type of the property value
         * @return this builder
         */
        public <T> Builder put(PropertyDefinition<T> property, T value) {
            if (value != null) {
                values.put(property, value);
            }
            return this;
        }

        /**
         * Builds the typed property map.
         *
         * @return the property map
         */
        public TypedPropertyMap build() {
            if (values.isEmpty()) {
                return EMPTY;
            }
            return new TypedPropertyMap(Collections.unmodifiableMap(new HashMap<>(values)));
        }
    }
}

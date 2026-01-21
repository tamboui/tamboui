/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Optional;

/**
 * A resolver for retrieving typed property values by definition.
 * <p>
 * This is the minimal abstraction that allows widgets to resolve style properties
 * without depending on any specific styling system.
 * <p>
 * Use {@link #empty()} when no styling is needed.
 *
 * <h2>Usage in widgets:</h2>
 * <pre>{@code
 * public void render(Rect area, Buffer buffer, StylePropertyResolver resolver) {
 *     Color borderColor = resolver.resolve(StandardProperties.BORDER_COLOR, this.borderColor);
 *     // render with borderColor...
 * }
 * }</pre>
 */
@FunctionalInterface
public interface StylePropertyResolver {

    /**
     * Retrieves the value for the given property definition.
     *
     * @param property the property definition
     * @param <T>      the type of the property value
     * @return the property value, or empty if not found
     */
    <T> Optional<T> get(PropertyDefinition<T> property);

    /**
     * Resolves the effective value for a property.
     * <p>
     * Resolution order: <strong>programmatic → resolver → property default</strong>
     * <ol>
     *   <li>If {@code programmaticValue} is non-null, return it</li>
     *   <li>Otherwise, try to resolve from this resolver</li>
     *   <li>Otherwise, return {@link PropertyDefinition#defaultValue()}</li>
     * </ol>
     *
     * @param property          the property definition
     * @param programmaticValue the value set programmatically, or null
     * @param <T>               the type of the property value
     * @return the resolved value (may be null if property has no default)
     */
    default <T> T resolve(PropertyDefinition<T> property, T programmaticValue) {
        if (programmaticValue != null) {
            return programmaticValue;
        }
        return get(property).orElse(property.defaultValue());
    }

    /**
     * Returns an empty resolver that never resolves any properties.
     * <p>
     * Use this when rendering widgets without any styling system.
     *
     * @return an empty property resolver
     */
    static StylePropertyResolver empty() {
        return EmptyStylePropertyResolver.INSTANCE;
    }
}

/**
 * Singleton empty resolver implementation.
 */
final class EmptyStylePropertyResolver implements StylePropertyResolver {

    static final EmptyStylePropertyResolver INSTANCE = new EmptyStylePropertyResolver();

    private EmptyStylePropertyResolver() {
    }

    @Override
    public <T> Optional<T> get(PropertyDefinition<T> property) {
        return Optional.empty();
    }
}

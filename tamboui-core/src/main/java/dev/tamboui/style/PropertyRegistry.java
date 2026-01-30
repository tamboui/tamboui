/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe registry for style property definitions.
 * <p>
 * This registry allows widgets and components to register their custom
 * {@link PropertyDefinition}s so that style resolvers can recognize them as
 * valid properties.
 * <p>
 * Properties are registered by name. If a property with the same name is
 * registered multiple times, the last registration wins.
 * <p>
 * Example usage in a widget:
 * 
 * <pre>{@code
 * public class MyWidget {
 *     public static final PropertyDefinition<Color> BAR_COLOR = PropertyDefinition.of("bar-color",
 *             ColorConverter.INSTANCE);
 *
 *     static {
 *         PropertyRegistry.register(BAR_COLOR);
 *     }
 * }
 * }</pre>
 *
 * @see PropertyDefinition
 * @see StandardProperties
 */
public final class PropertyRegistry {

    private static final ConcurrentMap<String, PropertyDefinition<?>> REGISTRY = new ConcurrentHashMap<>();

    private PropertyRegistry() {
        // Utility class
    }

    /**
     * Registers a property definition.
     * <p>
     * Once registered, the property will be recognized by style resolvers and will
     * not trigger unknown property warnings or errors.
     *
     * @param property
     *            the property definition to register
     * @throws NullPointerException
     *             if property is null
     */
    public static void register(PropertyDefinition<?> property) {
        if (property == null) {
            throw new NullPointerException("property must not be null");
        }
        REGISTRY.put(property.name(), property);
    }

    /**
     * Registers multiple property definitions.
     *
     * @param properties
     *            the property definitions to register
     * @throws NullPointerException
     *             if any property is null
     */
    public static void registerAll(PropertyDefinition<?>... properties) {
        for (PropertyDefinition<?> property : properties) {
            register(property);
        }
    }

    /**
     * Looks up a property definition by name.
     *
     * @param name
     *            the property name (e.g., "color", "border-type")
     * @return the property definition, or empty if not registered
     */
    public static Optional<PropertyDefinition<?>> byName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(REGISTRY.get(name));
    }

    /**
     * Checks if a property with the given name is registered.
     *
     * @param name
     *            the property name
     * @return true if the property is registered
     */
    public static boolean isRegistered(String name) {
        return name != null && REGISTRY.containsKey(name);
    }

    /**
     * Clears all registered properties.
     * <p>
     * This method is primarily intended for testing purposes.
     */
    public static void clear() {
        REGISTRY.clear();
    }
}
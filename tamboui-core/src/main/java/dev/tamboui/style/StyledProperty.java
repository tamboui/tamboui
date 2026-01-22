/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A property value that can be set programmatically or resolved from a style source.
 * <p>
 * StyledProperty encapsulates the common pattern of having a property that can be:
 * <ol>
 *   <li>Set explicitly via a fluent API (programmatic value)</li>
 *   <li>Resolved from a styling system</li>
 *   <li>Falling back to a default value</li>
 * </ol>
 * <p>
 * Resolution order: <strong>programmatic → resolver → default</strong>
 *
 * <h2>Usage in toolkit elements:</h2>
 * <pre>{@code
 * public class MyElement extends StyledElement<MyElement> {
 *     private final StyledProperty<Color> borderColor =
 *         styledProperty(StandardPropertyKeys.BORDER_COLOR, Color.WHITE);
 *
 *     public MyElement borderColor(Color color) {
 *         borderColor.set(color);
 *         return this;
 *     }
 *
 *     protected void renderContent(Frame frame, Rect area, RenderContext context) {
 *         Color color = borderColor.resolve();
 *         // render with color...
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage in standalone widgets:</h2>
 * <pre>{@code
 * StyledProperty<Color> color = StyledProperty.of(BORDER_COLOR, Color.WHITE);
 * color.resolve(resolver);
 * }</pre>
 *
 * @param <T> the type of the property value
 */
public final class StyledProperty<T> {

    private final PropertyKey<T> key;
    private final T defaultValue;
    private final Supplier<StylePropertyResolver> resolverSupplier;
    private T value;

    private StyledProperty(PropertyKey<T> key, T defaultValue, Supplier<StylePropertyResolver> resolverSupplier) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.defaultValue = defaultValue;
        this.resolverSupplier = resolverSupplier;
    }

    /**
     * Creates a styled property with the given key and no default value.
     * <p>
     * The returned property is unbound - {@link #resolve()} will use an empty resolver.
     * Use {@link #of(PropertyKey, Object, Supplier)} to create a bound property.
     *
     * @param key the property key for resolution
     * @param <T> the type of the property value
     * @return the styled property
     */
    public static <T> StyledProperty<T> of(PropertyKey<T> key) {
        return new StyledProperty<>(key, null, null);
    }

    /**
     * Creates a styled property with the given key and default value.
     * <p>
     * The returned property is unbound - {@link #resolve()} will use an empty resolver.
     * Use {@link #of(PropertyKey, Object, Supplier)} to create a bound property.
     *
     * @param key          the property key for resolution
     * @param defaultValue the default value when neither programmatic nor resolved value is available
     * @param <T>          the type of the property value
     * @return the styled property
     */
    public static <T> StyledProperty<T> of(PropertyKey<T> key, T defaultValue) {
        return new StyledProperty<>(key, defaultValue, null);
    }

    /**
     * Creates a styled property bound to a resolver supplier.
     * <p>
     * When {@link #resolve()} is called, it will use the resolver from the supplier.
     * This is typically used by elements that maintain a current resolver during rendering.
     *
     * @param key              the property key for resolution
     * @param defaultValue     the default value when neither programmatic nor resolved value is available
     * @param resolverSupplier supplies the property resolver to use during resolution
     * @param <T>              the type of the property value
     * @return the styled property
     */
    public static <T> StyledProperty<T> of(PropertyKey<T> key, T defaultValue,
                                           Supplier<StylePropertyResolver> resolverSupplier) {
        return new StyledProperty<>(key, defaultValue, resolverSupplier);
    }

    /**
     * Sets the programmatic value for this property.
     * <p>
     * When set, this value takes precedence over any resolved value.
     *
     * @param value the value to set, or null to clear
     */
    public void set(T value) {
        this.value = value;
    }

    /**
     * Returns the programmatic value, if set.
     *
     * @return the programmatic value, or null if not set
     */
    public T get() {
        return value;
    }

    /**
     * Returns the property key associated with this property.
     *
     * @return the property key
     */
    public PropertyKey<T> key() {
        return key;
    }

    /**
     * Returns the default value for this property.
     *
     * @return the default value, or null if none
     */
    public T defaultValue() {
        return defaultValue;
    }

    /**
     * Resolves the effective value for this property.
     * <p>
     * Resolution order:
     * <ol>
     *   <li>Programmatic value (if set via {@link #set})</li>
     *   <li>Resolved value from the resolver</li>
     *   <li>Default value</li>
     * </ol>
     *
     * @param resolver the property resolver to use for resolution
     * @return the resolved value, or null if no value is available
     */
    public T resolve(StylePropertyResolver resolver) {
        if (value != null) {
            return value;
        }
        return resolver.get(key).orElse(defaultValue);
    }

    /**
     * Resolves the effective value using the bound resolver, or empty if unbound.
     * <p>
     * For bound properties (created with a resolver supplier), this uses the current
     * resolver from the supplier. For unbound properties, this is equivalent to
     * returning the programmatic value or default.
     *
     * @return the resolved value, or null if no value is available
     */
    public T resolve() {
        if (value != null) {
            return value;
        }
        StylePropertyResolver resolver = resolverSupplier != null
                ? resolverSupplier.get()
                : StylePropertyResolver.empty();
        return resolver.get(key).orElse(defaultValue);
    }
}

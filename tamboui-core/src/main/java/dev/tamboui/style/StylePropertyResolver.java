/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import java.util.Optional;

/// A resolver for retrieving typed property values by key.
///
///
///
/// This is the minimal abstraction that allows widgets to resolve style properties
/// without depending on any specific styling system.
///
///
///
/// Use {@link #empty()} when no styling is needed.
///
/// ## Usage in widgets:
/// ```java
/// public void render(Rect area, Buffer buffer, PropertyResolver resolver) {
///     Color borderColor = resolver.get(BORDER_COLOR).orElse(Color.WHITE);
///     // render with borderColor...
/// }
/// }
/// ```
@FunctionalInterface
public interface StylePropertyResolver {

    /// Retrieves the value for the given property key.
    ///
    /// @param key the property key
    /// @param <T> the type of the property value
    /// @return the property value, or empty if not found
    <T> Optional<T> get(PropertyKey<T> key);

    /// Returns an empty resolver that never resolves any properties.
    ///
    ///
    ///
    /// Use this when rendering widgets without any styling system.
    ///
    /// @return an empty property resolver
    static StylePropertyResolver empty() {
        return EmptyStylePropertyResolver.INSTANCE;
    }
}

/// Singleton empty resolver implementation.
final class EmptyStylePropertyResolver implements StylePropertyResolver {

    static final EmptyStylePropertyResolver INSTANCE = new EmptyStylePropertyResolver();

    private EmptyStylePropertyResolver() {
    }

    @Override
    public <T> Optional<T> get(PropertyKey<T> key) {
        return Optional.empty();
    }
}


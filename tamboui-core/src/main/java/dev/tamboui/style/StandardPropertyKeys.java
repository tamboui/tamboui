/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import dev.tamboui.layout.Alignment;

/// Standard property keys for style-aware widgets.
///
///
///
/// This class defines the canonical property keys for common styling properties.
/// Widgets should use these keys to ensure consistency.
///
/// ## Usage in widgets:
/// ```java
/// Block.builder()
///     .resolver(styleResolver)
///     .background(Color.BLUE)
///     .build();
/// }
/// ```
public final class StandardPropertyKeys {

    private StandardPropertyKeys() {
        // Utility class
    }

    // ═══════════════════════════════════════════════════════════════
    // Color properties
    // ═══════════════════════════════════════════════════════════════

    /// The {@code color} property for foreground/text color.
    public static final PropertyKey<Color> COLOR =
            PropertyKey.of("color", ColorConverter.INSTANCE);

    /// The {@code background} property for background color.
    public static final PropertyKey<Color> BACKGROUND =
            PropertyKey.of("background", ColorConverter.INSTANCE);

    /// The {@code background-color} property (alias for background).
    public static final PropertyKey<Color> BACKGROUND_COLOR =
            PropertyKey.of("background-color", ColorConverter.INSTANCE);

    /// The {@code border-color} property.
    public static final PropertyKey<Color> BORDER_COLOR =
            PropertyKey.of("border-color", ColorConverter.INSTANCE);

    // ═══════════════════════════════════════════════════════════════
    // Alignment property
    // ═══════════════════════════════════════════════════════════════

    /// The {@code text-align} property for text alignment.
    ///
    ///
    ///
    /// Supported values: {@code left}, {@code center}, {@code right}
    public static final PropertyKey<Alignment> TEXT_ALIGN =
            PropertyKey.of("text-align", AlignmentConverter.INSTANCE);
}


/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.ConstraintConverter;
import dev.tamboui.layout.Direction;
import dev.tamboui.layout.DirectionConverter;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.FlexConverter;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.MarginConverter;
import dev.tamboui.layout.Padding;
import dev.tamboui.layout.PaddingConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Central registry of all standard property definitions.
 * <p>
 * This class defines all properties understood by TamboUI, including their
 * inheritance behavior. Properties marked as inheritable are automatically
 * passed from parent to child elements if not explicitly set.
 *
 * <h2>Inheritable properties:</h2>
 * <ul>
 *   <li>{@code color} - foreground/text color</li>
 *   <li>{@code text-style} - text modifiers (bold, italic, etc.)</li>
 *   <li>{@code border-type} - border style (for nested containers)</li>
 * </ul>
 *
 * <h2>Non-inheritable properties:</h2>
 * <ul>
 *   <li>{@code background}, {@code background-color} - background color</li>
 *   <li>{@code padding} - inner spacing</li>
 *   <li>{@code margin} - outer spacing</li>
 *   <li>{@code flex}, {@code direction}, {@code spacing} - layout properties</li>
 *   <li>{@code width}, {@code height} - size constraints</li>
 *   <li>{@code text-align} - text alignment</li>
 * </ul>
 */
public final class StandardProperties {

    private StandardProperties() {
        // Utility class
    }

    // ═══════════════════════════════════════════════════════════════
    // Property name constants
    // ═══════════════════════════════════════════════════════════════

    /** Property name for foreground/text color. */
    public static final String NAME_COLOR = "color";
    /** Property name for background color. */
    public static final String NAME_BACKGROUND = "background";
    /** Property name for background color (alias). */
    public static final String NAME_BACKGROUND_COLOR = "background-color";
    /** Property name for text modifiers (bold, italic, etc.). */
    public static final String NAME_TEXT_STYLE = "text-style";
    /** Property name for inner spacing. */
    public static final String NAME_PADDING = "padding";
    /** Property name for outer spacing. */
    public static final String NAME_MARGIN = "margin";
    /** Property name for text alignment. */
    public static final String NAME_TEXT_ALIGN = "text-align";
    /** Property name for flex layout mode. */
    public static final String NAME_FLEX = "flex";
    /** Property name for layout direction. */
    public static final String NAME_DIRECTION = "direction";
    /** Property name for gap between elements. */
    public static final String NAME_SPACING = "spacing";
    /** Property name for element width constraint. */
    public static final String NAME_WIDTH = "width";
    /** Property name for element height constraint. */
    public static final String NAME_HEIGHT = "height";

    // ═══════════════════════════════════════════════════════════════
    // Inheritable properties
    // ═══════════════════════════════════════════════════════════════

    /**
     * The {@code color} property for foreground/text color.
     * This property is inheritable - children inherit their parent's color.
     */
    public static final PropertyDefinition<Color> COLOR =
            PropertyDefinition.builder(NAME_COLOR, ColorConverter.INSTANCE)
                    .inheritable()
                    .build();

    /**
     * The {@code text-style} property for text modifiers.
     * This property is inheritable - children inherit their parent's text style.
     */
    public static final PropertyDefinition<Set<Modifier>> TEXT_STYLE =
            PropertyDefinition.builder(NAME_TEXT_STYLE, ModifierConverter.INSTANCE)
                    .inheritable()
                    .build();

    // ═══════════════════════════════════════════════════════════════
    // Non-inheritable properties
    // ═══════════════════════════════════════════════════════════════

    /**
     * The {@code background} property for background color.
     * This property is NOT inheritable per CSS spec.
     */
    public static final PropertyDefinition<Color> BACKGROUND =
            PropertyDefinition.of(NAME_BACKGROUND, ColorConverter.INSTANCE);

    /**
     * The {@code background-color} property (alias for background).
     * This property is NOT inheritable per CSS spec.
     */
    public static final PropertyDefinition<Color> BACKGROUND_COLOR =
            PropertyDefinition.of(NAME_BACKGROUND_COLOR, ColorConverter.INSTANCE);

    /**
     * The {@code padding} property for inner spacing.
     * This property is NOT inheritable.
     * Default: {@link Padding#NONE}
     */
    public static final PropertyDefinition<Padding> PADDING =
            PropertyDefinition.builder(NAME_PADDING, PaddingConverter.INSTANCE)
                    .defaultValue(Padding.NONE)
                    .build();

    /**
     * The {@code margin} property for outer spacing.
     * This property is NOT inheritable.
     * Default: {@link Margin#NONE}
     */
    public static final PropertyDefinition<Margin> MARGIN =
            PropertyDefinition.builder(NAME_MARGIN, MarginConverter.INSTANCE)
                    .defaultValue(Margin.NONE)
                    .build();

    /**
     * The {@code text-align} property for text alignment.
     * This property is NOT inheritable.
     * Default: {@link Alignment#LEFT}
     */
    public static final PropertyDefinition<Alignment> TEXT_ALIGN =
            PropertyDefinition.builder(NAME_TEXT_ALIGN, AlignmentConverter.INSTANCE)
                    .defaultValue(Alignment.LEFT)
                    .build();

    /**
     * The {@code flex} property for flex layout mode.
     * This property is NOT inheritable.
     */
    public static final PropertyDefinition<Flex> FLEX =
            PropertyDefinition.of(NAME_FLEX, FlexConverter.INSTANCE);

    /**
     * The {@code direction} property for layout direction.
     * This property is NOT inheritable.
     */
    public static final PropertyDefinition<Direction> DIRECTION =
            PropertyDefinition.of(NAME_DIRECTION, DirectionConverter.INSTANCE);

    /**
     * The {@code spacing} property for gap between elements.
     * This property is NOT inheritable.
     */
    public static final PropertyDefinition<Integer> SPACING =
            PropertyDefinition.of(NAME_SPACING, IntegerConverter.INSTANCE);

    /**
     * The {@code width} property for element width constraint.
     * Supports: fill, fit, percentages (50%), fixed lengths (20), min/max, ratios (1/3).
     * This property is NOT inheritable.
     */
    public static final PropertyDefinition<Constraint> WIDTH =
            PropertyDefinition.of(NAME_WIDTH, ConstraintConverter.INSTANCE);

    /**
     * The {@code height} property for height constraint.
     * This property is NOT inheritable.
     */
    public static final PropertyDefinition<Constraint> HEIGHT =
            PropertyDefinition.of(NAME_HEIGHT, ConstraintConverter.INSTANCE);

    /**
     * The {@code text-overflow} property for text wrapping/truncation behavior.
     * This property is NOT inheritable.
     * Default: {@link Overflow#CLIP}
     */
    public static final PropertyDefinition<Overflow> TEXT_OVERFLOW =
            PropertyDefinition.builder("text-overflow", OverflowConverter.INSTANCE)
                    .defaultValue(Overflow.CLIP)
                    .build();

    // ═══════════════════════════════════════════════════════════════
    // Registry lookup
    // ═══════════════════════════════════════════════════════════════

    private static final Map<String, PropertyDefinition<?>> BY_NAME = new HashMap<>();

    static {
        register(COLOR);
        register(TEXT_STYLE);
        register(BACKGROUND);
        register(BACKGROUND_COLOR);
        register(PADDING);
        register(MARGIN);
        register(TEXT_ALIGN);
        register(FLEX);
        register(DIRECTION);
        register(SPACING);
        register(WIDTH);
        register(HEIGHT);
        register(TEXT_OVERFLOW);
    }

    private static void register(PropertyDefinition<?> property) {
        BY_NAME.put(property.name(), property);
        PropertyRegistry.register(property);
    }

    /**
     * Looks up a property definition by its name.
     *
     * @param name the property name (e.g., "color", "padding")
     * @return the property definition, or empty if not registered
     */
    public static Optional<PropertyDefinition<?>> byName(String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }

    /**
     * Returns all registered property definitions.
     *
     * @return an unmodifiable collection of all property definitions
     */
    public static Collection<PropertyDefinition<?>> all() {
        return Collections.unmodifiableCollection(BY_NAME.values());
    }

    /**
     * Returns all inheritable property definitions.
     *
     * @return a collection of inheritable property definitions
     */
    public static Collection<PropertyDefinition<?>> inheritable() {
        List<PropertyDefinition<?>> result = new ArrayList<>();
        for (PropertyDefinition<?> prop : BY_NAME.values()) {
            if (prop.isInheritable()) {
                result.add(prop);
            }
        }
        return result;
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.cascade;

import dev.tamboui.style.StandardProperties;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Direction;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Margin;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.style.Style;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.layout.Padding;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents the computed style for an element after CSS cascade resolution.
 * <p>
 * Implements {@link StylePropertyResolver} to allow typed property access using
 * {@link PropertyDefinition}s. Properties can be retrieved either through the
 * typed getters (e.g., {@link #foreground()}) or through the generic
 * {@link #get(PropertyDefinition)} method.
 * <p>
 * Example usage with PropertyDefinition:
 * <pre>{@code
 * CssStyleResolver style = cascadeResolver.resolve(element, rules);
 * Color borderColor = style.get(StandardProperties.BORDER_COLOR).orElse(Color.WHITE);
 * }</pre>
 */
public final class CssStyleResolver implements StylePropertyResolver {

    private static final CssStyleResolver EMPTY = new CssStyleResolver(
            TypedPropertyMap.empty(),
            Collections.emptyMap(),
            Collections.emptySet()
    );

    private final TypedPropertyMap properties;
    private final Map<String, String> rawValues;
    private final Set<String> inheritedProperties;   // properties set to "inherit"

    private CssStyleResolver(TypedPropertyMap properties,
                             Map<String, String> rawValues,
                             Set<String> inheritedProperties) {
        this.properties = properties;
        this.rawValues = rawValues;
        this.inheritedProperties = inheritedProperties;
    }

    /**
     * Creates an empty resolved style.
     *
     * @return an empty CssStyleResolver
     */
    public static CssStyleResolver empty() {
        return EMPTY;
    }

    // ═══════════════════════════════════════════════════════════════
    // PropertyResolver implementation
    // ═══════════════════════════════════════════════════════════════

    /**
     * Retrieves a typed property value using the given property definition.
     * <p>
     * The resolution order is:
     * <ol>
     *   <li>Check for a pre-converted value in the typed property map</li>
     *   <li>Check for a raw CSS value and convert using the property's converter</li>
     * </ol>
     * This allows widget-defined properties to be resolved from CSS without
     * requiring the CSS engine to know about all properties upfront.
     *
     * @param property the property definition
     * @param <T>      the type of the property value
     * @return the property value, or empty if not found
     */
    @Override
    public <T> Optional<T> get(PropertyDefinition<T> property) {
        // First, check the typed property map
        Optional<T> typed = properties.get(property);
        if (typed.isPresent()) {
            return typed;
        }

        // Fall back to lazy conversion from raw values
        String rawValue = rawValues.get(property.name());
        if (rawValue != null) {
            return property.convert(rawValue);
        }

        return Optional.empty();
    }

    // ═══════════════════════════════════════════════════════════════
    // Typed property accessors
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns the foreground color, if set.
     *
     * @return the foreground color
     */
    public Optional<Color> foreground() {
        return get(StandardProperties.COLOR);
    }

    /**
     * Returns the background color, if set.
     *
     * @return the background color
     */
    public Optional<Color> background() {
        // Check both background and background-color
        Optional<Color> bg = get(StandardProperties.BACKGROUND);
        if (bg.isPresent()) {
            return bg;
        }
        return get(StandardProperties.BACKGROUND_COLOR);
    }

    /**
     * Returns the text modifiers.
     *
     * @return the set of modifiers
     */
    public Set<Modifier> modifiers() {
        return get(StandardProperties.TEXT_STYLE).orElse(Collections.emptySet());
    }

    /**
     * Returns the padding, if set.
     *
     * @return the padding
     */
    public Optional<Padding> padding() {
        return get(StandardProperties.PADDING);
    }

    /**
     * Returns the text alignment, if set.
     *
     * @return the alignment
     */
    public Optional<Alignment> alignment() {
        return get(StandardProperties.TEXT_ALIGN);
    }

    /**
     * Returns the border type, if set.
     *
     * @return the border type
     */
    public Optional<BorderType> borderType() {
        return get(Block.BORDER_TYPE);
    }

    /**
     * Returns the border color, if set.
     *
     * @return the border color
     */
    public Optional<Color> borderColor() {
        return get(Block.BORDER_COLOR);
    }

    /**
     * Returns the width constraint, if set.
     *
     * @return the width constraint
     */
    public Optional<Constraint> widthConstraint() {
        return get(StandardProperties.WIDTH);
    }

    /**
     * Returns the flex layout mode, if set.
     *
     * @return the flex mode
     */
    public Optional<Flex> flex() {
        return get(StandardProperties.FLEX);
    }

    /**
     * Returns the layout direction, if set.
     *
     * @return the direction
     */
    public Optional<Direction> direction() {
        return get(StandardProperties.DIRECTION);
    }

    /**
     * Returns the margin, if set.
     *
     * @return the margin
     */
    public Optional<Margin> margin() {
        return get(StandardProperties.MARGIN);
    }

    /**
     * Returns the spacing (gap between elements), if set.
     *
     * @return the spacing
     */
    public Optional<Integer> spacing() {
        return get(StandardProperties.SPACING);
    }

    /**
     * Returns the height constraint (for vertical layouts), if set.
     *
     * @return the height constraint
     */
    public Optional<Constraint> heightConstraint() {
        return get(StandardProperties.HEIGHT);
    }

    /**
     * Returns the text-overflow behavior, if set.
     *
     * @return the text-overflow value
     */
    public Optional<Overflow> textOverflow() {
        return get(StandardProperties.TEXT_OVERFLOW);
    }

    // ═══════════════════════════════════════════════════════════════
    // Border character accessors
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns the border-chars string, if set.
     * Format: 8 quoted strings (top-h, bottom-h, left-v, right-v, tl, tr, bl, br).
     *
     * @return the border-chars value
     */
    public Optional<String> borderChars() {
        return get(Block.BORDER_CHARS);
    }

    /**
     * Returns the top border character, if set.
     *
     * @return the border-top value
     */
    public Optional<String> borderTop() {
        return get(Block.BORDER_TOP);
    }

    /**
     * Returns the bottom border character, if set.
     *
     * @return the border-bottom value
     */
    public Optional<String> borderBottom() {
        return get(Block.BORDER_BOTTOM);
    }

    /**
     * Returns the left border character, if set.
     *
     * @return the border-left value
     */
    public Optional<String> borderLeft() {
        return get(Block.BORDER_LEFT);
    }

    /**
     * Returns the right border character, if set.
     *
     * @return the border-right value
     */
    public Optional<String> borderRight() {
        return get(Block.BORDER_RIGHT);
    }

    /**
     * Returns the top-left corner character, if set.
     *
     * @return the border-top-left value
     */
    public Optional<String> borderTopLeft() {
        return get(Block.BORDER_TOP_LEFT);
    }

    /**
     * Returns the top-right corner character, if set.
     *
     * @return the border-top-right value
     */
    public Optional<String> borderTopRight() {
        return get(Block.BORDER_TOP_RIGHT);
    }

    /**
     * Returns the bottom-left corner character, if set.
     *
     * @return the border-bottom-left value
     */
    public Optional<String> borderBottomLeft() {
        return get(Block.BORDER_BOTTOM_LEFT);
    }

    /**
     * Returns the bottom-right corner character, if set.
     *
     * @return the border-bottom-right value
     */
    public Optional<String> borderBottomRight() {
        return get(Block.BORDER_BOTTOM_RIGHT);
    }

    // ═══════════════════════════════════════════════════════════════
    // Utility methods
    // ═══════════════════════════════════════════════════════════════

    /**
     * Converts this resolved style to a TamboUI Style object.
     *
     * @return the Style object
     */
    public Style toStyle() {
        Style style = Style.EMPTY;

        Optional<Color> fg = foreground();
        if (fg.isPresent()) {
            style = style.fg(fg.get());
        }
        Optional<Color> bg = background();
        if (bg.isPresent()) {
            style = style.bg(bg.get());
        }
        for (Modifier modifier : modifiers()) {
            style = style.addModifier(modifier);
        }

        return style;
    }

    /**
     * Returns true if this style has any properties set.
     *
     * @return true if any properties are set
     */
    public boolean hasProperties() {
        return !properties.isEmpty() || !rawValues.isEmpty();
    }

    /**
     * Creates a new resolver that uses this resolver's properties but falls back
     * to the given resolver for CSS-inheritable properties not set in this resolver.
     * <p>
     * Per CSS semantics, only certain properties inherit from parent to child.
     * The inheritance behavior is defined per-property in {@link PropertyDefinition#isInheritable()}.
     * <p>
     * Additionally, this method handles the {@code inherit} keyword: properties
     * explicitly requesting parent's value.
     *
     * @param fallback the fallback resolver for missing inheritable properties
     * @return a new resolver with fallback behavior for inheritable properties only
     */
    public CssStyleResolver withFallback(CssStyleResolver fallback) {
        if (fallback == null) {
            return this;
        }

        // Merge raw values: start with child's own values
        Map<String, String> mergedRaw = new HashMap<>();

        // Handle explicit "inherit" keyword - copy from parent regardless of inheritability
        for (String prop : this.inheritedProperties) {
            if (fallback.rawValues.containsKey(prop)) {
                mergedRaw.put(prop, fallback.rawValues.get(prop));
            }
        }

        // Add child's own raw values (override any inherited)
        for (Map.Entry<String, String> entry : this.rawValues.entrySet()) {
            // Skip properties that are set to "inherit" (already handled above from parent)
            if (!this.inheritedProperties.contains(entry.getKey())) {
                mergedRaw.put(entry.getKey(), entry.getValue());
            }
        }

        // Use TypedPropertyMap with standard inheritance
        TypedPropertyMap merged = this.properties.withFallback(fallback.properties);

        // For explicit "inherit" keyword, we need to copy typed properties from parent
        TypedPropertyMap.Builder inheritBuilder = TypedPropertyMap.builder();
        for (String prop : this.inheritedProperties) {
            Optional<PropertyDefinition<?>> propDef = PropertyRegistry.byName(prop);
            if (propDef.isPresent()) {
                copyTypedProperty(inheritBuilder, fallback.properties, propDef.get());
            }
        }
        TypedPropertyMap inheritedTyped = inheritBuilder.build();

        // Merge: start with merged (has fallback inheritable), overlay with explicit inherited
        if (!inheritedTyped.isEmpty()) {
            Map<PropertyDefinition<?>, Object> finalMerged = new HashMap<>();
            for (PropertyDefinition<?> p : merged.properties()) {
                merged.get(p).ifPresent(v -> finalMerged.put(p, v));
            }
            for (PropertyDefinition<?> p : inheritedTyped.properties()) {
                inheritedTyped.get(p).ifPresent(v -> finalMerged.put(p, v));
            }
            // Overlay with child's own non-inherited properties
            for (PropertyDefinition<?> p : this.properties.properties()) {
                this.properties.get(p).ifPresent(v -> finalMerged.put(p, v));
            }
            merged = new TypedPropertyMap(finalMerged, true);
        }

        return new CssStyleResolver(merged, mergedRaw, Collections.emptySet());
    }

    @SuppressWarnings("unchecked")
    private <T> void copyTypedProperty(TypedPropertyMap.Builder builder,
                                       TypedPropertyMap source,
                                       PropertyDefinition<T> property) {
        source.get(property).ifPresent(v -> builder.put(property, v));
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for CssStyleResolver.
     */
    public static final class Builder {
        private final TypedPropertyMap.Builder properties = TypedPropertyMap.builder();
        private final Map<String, String> rawValues = new HashMap<>();
        private final Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
        private final Set<String> inheritedProperties = new HashSet<>();

        private Builder() {
        }

        /**
         * Sets a typed property value.
         *
         * @param property the property definition
         * @param value    the property value
         * @param <T>      the type of the property value
         * @return this builder
         */
        public <T> Builder set(PropertyDefinition<T> property, T value) {
            if (value != null) {
                properties.put(property, value);
            }
            return this;
        }

        /**
         * Sets a raw property value for lazy conversion.
         * <p>
         * Raw values are stored by property name and converted lazily when
         * accessed via {@link CssStyleResolver#get(PropertyDefinition)}.
         * This enables widget-defined properties to be resolved from CSS
         * without requiring the CSS engine to know all property definitions.
         *
         * @param propertyName the CSS property name
         * @param value        the raw CSS value (already variable-resolved)
         * @return this builder
         */
        public Builder setRaw(String propertyName, String value) {
            if (propertyName != null && value != null) {
                rawValues.put(propertyName, value);
            }
            return this;
        }

        /**
         * Sets the foreground color.
         *
         * @param color the foreground color
         * @return this builder
         */
        public Builder foreground(Color color) {
            return set(StandardProperties.COLOR, color);
        }

        /**
         * Sets the background color.
         *
         * @param color the background color
         * @return this builder
         */
        public Builder background(Color color) {
            return set(StandardProperties.BACKGROUND, color);
        }

        /**
         * Adds a text modifier.
         *
         * @param modifier the modifier to add
         * @return this builder
         */
        public Builder addModifier(Modifier modifier) {
            this.modifiers.add(modifier);
            return this;
        }

        /**
         * Adds multiple text modifiers.
         *
         * @param modifiers the modifiers to add
         * @return this builder
         */
        public Builder addModifiers(Set<Modifier> modifiers) {
            if (modifiers != null) {
                this.modifiers.addAll(modifiers);
            }
            return this;
        }

        /**
         * Sets the padding.
         *
         * @param padding the padding
         * @return this builder
         */
        public Builder padding(Padding padding) {
            return set(StandardProperties.PADDING, padding);
        }

        /**
         * Sets the text alignment.
         *
         * @param alignment the alignment
         * @return this builder
         */
        public Builder alignment(Alignment alignment) {
            return set(StandardProperties.TEXT_ALIGN, alignment);
        }

        /**
         * Sets the border type.
         *
         * @param borderType the border type
         * @return this builder
         */
        public Builder borderType(BorderType borderType) {
            return set(Block.BORDER_TYPE, borderType);
        }

        /**
         * Sets the width constraint.
         *
         * @param constraint the width constraint
         * @return this builder
         */
        public Builder width(Constraint constraint) {
            return set(StandardProperties.WIDTH, constraint);
        }

        /**
         * Sets the flex layout mode.
         *
         * @param flex the flex mode
         * @return this builder
         */
        public Builder flex(Flex flex) {
            return set(StandardProperties.FLEX, flex);
        }

        /**
         * Sets the layout direction.
         *
         * @param direction the direction
         * @return this builder
         */
        public Builder direction(Direction direction) {
            return set(StandardProperties.DIRECTION, direction);
        }

        /**
         * Sets the margin.
         *
         * @param margin the margin
         * @return this builder
         */
        public Builder margin(Margin margin) {
            return set(StandardProperties.MARGIN, margin);
        }

        /**
         * Sets the spacing (gap between elements).
         *
         * @param spacing the spacing
         * @return this builder
         */
        public Builder spacing(Integer spacing) {
            return set(StandardProperties.SPACING, spacing);
        }

        /**
         * Sets the height constraint (for vertical layouts).
         *
         * @param constraint the height constraint
         * @return this builder
         */
        public Builder heightConstraint(Constraint constraint) {
            return set(StandardProperties.HEIGHT, constraint);
        }

        /**
         * Marks a property as using the "inherit" keyword.
         * <p>
         * This indicates that the child element wants to explicitly inherit
         * this property's value from its parent, regardless of whether the
         * property is normally inheritable.
         *
         * @param propertyName the CSS property name
         * @return this builder
         */
        public Builder markAsInherited(String propertyName) {
            if (propertyName != null) {
                inheritedProperties.add(propertyName);
            }
            return this;
        }

        /**
         * Builds the CssStyleResolver.
         *
         * @return the built resolver
         */
        public CssStyleResolver build() {
            // Add accumulated modifiers to properties if any
            if (!modifiers.isEmpty()) {
                properties.put(StandardProperties.TEXT_STYLE, modifiers);
            }
            Map<String, String> finalRawValues = rawValues.isEmpty()
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(rawValues));
            Set<String> finalInherited = inheritedProperties.isEmpty()
                    ? Collections.emptySet()
                    : Collections.unmodifiableSet(new HashSet<>(inheritedProperties));
            return new CssStyleResolver(properties.build(), finalRawValues, finalInherited);
        }
    }

    @Override
    public String toString() {
        return "CssStyleResolver{properties=" + properties + ", rawValues=" + rawValues + "}";
    }
}

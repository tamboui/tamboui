/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.cascade;

import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Direction;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Margin;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.PropertyKey;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.style.Style;
import dev.tamboui.style.Width;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Padding;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents the computed style for an element after CSS cascade resolution.
 * <p>
 * Implements {@link StylePropertyResolver} to allow typed property access using
 * {@link PropertyKey}s. Properties can be retrieved either through the legacy
 * typed getters (e.g., {@link #foreground()}) or through the generic
 * {@link #get(PropertyKey)} method.
 * <p>
 * Example usage with PropertyKey:
 * <pre>{@code
 * CssStyleResolver style = cascadeResolver.resolve(element, rules);
 * Color borderColor = style.get(StandardPropertyKeys.BORDER_COLOR).orElse(Color.WHITE);
 * }</pre>
 */
public final class CssStyleResolver implements StylePropertyResolver {

    private final Color foreground;
    private final Color background;
    private final Set<Modifier> modifiers;
    private final Padding padding;
    private final Alignment alignment;
    private final BorderType borderType;
    private final Width width;
    private final Flex flex;
    private final Direction direction;
    private final Margin margin;
    private final Integer spacing;
    private final Constraint heightConstraint;
    private final Constraint widthConstraint;
    private final Map<String, String> additionalProperties;

    private CssStyleResolver(Color foreground,
                             Color background,
                             Set<Modifier> modifiers,
                             Padding padding,
                             Alignment alignment,
                             BorderType borderType,
                             Width width,
                             Flex flex,
                             Direction direction,
                             Margin margin,
                             Integer spacing,
                             Constraint heightConstraint,
                             Constraint widthConstraint,
                             Map<String, String> additionalProperties) {
        this.foreground = foreground;
        this.background = background;
        this.modifiers = modifiers != null
                ? Collections.unmodifiableSet(EnumSet.copyOf(modifiers))
                : Collections.<Modifier>emptySet();
        this.padding = padding;
        this.alignment = alignment;
        this.borderType = borderType;
        this.width = width;
        this.flex = flex;
        this.direction = direction;
        this.margin = margin;
        this.spacing = spacing;
        this.heightConstraint = heightConstraint;
        this.widthConstraint = widthConstraint;
        this.additionalProperties = Collections.unmodifiableMap(new HashMap<>(additionalProperties));
    }

    /**
     * Creates an empty resolved style.
     *
     * @return an empty CssStyleResolver
     */
    public static CssStyleResolver empty() {
        return new CssStyleResolver(null, null, null, null, null, null, null, null,
                null, null, null, null, null, Collections.<String, String>emptyMap());
    }

    // ═══════════════════════════════════════════════════════════════
    // PropertyResolver implementation
    // ═══════════════════════════════════════════════════════════════

    /**
     * Retrieves a typed property value using the given property key.
     * <p>
     * Standard properties (color, background, border-color, text-align, border-type)
     * are retrieved from dedicated fields. Other properties are looked up in
     * {@link #additionalProperties} and converted using the key's converter.
     *
     * @param key the property key
     * @param <T> the type of the property value
     * @return the converted property value, or empty if not found or conversion fails
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(PropertyKey<T> key) {
        String name = key.name();
        // Check dedicated fields for standard properties
        switch (name) {
            case "color":
                return (Optional<T>) Optional.ofNullable(foreground);
            case "background":
            case "background-color":
                return (Optional<T>) Optional.ofNullable(background);
            case "border-color":
                // Border color is stored in additionalProperties or can be derived from foreground
                Optional<String> borderColorStr = getProperty("border-color");
                if (borderColorStr.isPresent()) {
                    return borderColorStr.flatMap(key::convert);
                }
                return Optional.empty();
            case "text-align":
                return (Optional<T>) Optional.ofNullable(alignment);
            case "border-type":
                return (Optional<T>) Optional.ofNullable(borderType);
            case "flex":
                return (Optional<T>) Optional.ofNullable(flex);
            case "direction":
                return (Optional<T>) Optional.ofNullable(direction);
            case "margin":
                return (Optional<T>) Optional.ofNullable(margin);
            case "spacing":
                return (Optional<T>) Optional.ofNullable(spacing);
            case "height":
                return (Optional<T>) Optional.ofNullable(heightConstraint);
            case "width":
                return (Optional<T>) Optional.ofNullable(widthConstraint);
            default:
                return getProperty(name).flatMap(key::convert);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Legacy typed property accessors
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns the foreground color, if set.
     *
     * @return the foreground color
     */
    public Optional<Color> foreground() {
        return Optional.ofNullable(foreground);
    }

    /**
     * Returns the background color, if set.
     *
     * @return the background color
     */
    public Optional<Color> background() {
        return Optional.ofNullable(background);
    }

    /**
     * Returns the text modifiers.
     *
     * @return the set of modifiers
     */
    public Set<Modifier> modifiers() {
        return modifiers;
    }

    /**
     * Returns the padding, if set.
     *
     * @return the padding
     */
    public Optional<Padding> padding() {
        return Optional.ofNullable(padding);
    }

    /**
     * Returns the text alignment, if set.
     *
     * @return the alignment
     */
    public Optional<Alignment> alignment() {
        return Optional.ofNullable(alignment);
    }

    /**
     * Returns the border type, if set.
     *
     * @return the border type
     */
    public Optional<BorderType> borderType() {
        return Optional.ofNullable(borderType);
    }

    /**
     * Returns the width behavior.
     * Defaults to {@link Width#FILL} if not explicitly set.
     *
     * @return the width
     */
    public Width width() {
        return width != null ? width : Width.FILL;
    }

    /**
     * Returns the flex layout mode, if set.
     *
     * @return the flex mode
     */
    public Optional<Flex> flex() {
        return Optional.ofNullable(flex);
    }

    /**
     * Returns the layout direction, if set.
     *
     * @return the direction
     */
    public Optional<Direction> direction() {
        return Optional.ofNullable(direction);
    }

    /**
     * Returns the margin, if set.
     *
     * @return the margin
     */
    public Optional<Margin> margin() {
        return Optional.ofNullable(margin);
    }

    /**
     * Returns the spacing (gap between elements), if set.
     *
     * @return the spacing
     */
    public Optional<Integer> spacing() {
        return Optional.ofNullable(spacing);
    }

    /**
     * Returns the height constraint (for vertical layouts), if set.
     *
     * @return the height constraint
     */
    public Optional<Constraint> heightConstraint() {
        return Optional.ofNullable(heightConstraint);
    }

    /**
     * Returns the width constraint (for horizontal layouts), if set.
     *
     * @return the width constraint
     */
    public Optional<Constraint> widthConstraint() {
        return Optional.ofNullable(widthConstraint);
    }

    /**
     * Returns additional properties not mapped to specific fields.
     *
     * @return the additional properties map
     */
    public Map<String, String> additionalProperties() {
        return additionalProperties;
    }

    /**
     * Gets an additional property value by name.
     *
     * @param name the property name
     * @return the property value, or empty if not found
     */
    public Optional<String> getProperty(String name) {
        return Optional.ofNullable(additionalProperties.get(name));
    }

    /**
     * Converts this resolved style to a TamboUI Style object.
     * <p>
     * The {@link Width} property is stored as a Style extension and can be
     * retrieved via {@code style.extension(Width.class, Width.FILL)}.
     *
     * @return the Style object
     */
    public Style toStyle() {
        Style style = Style.EMPTY;

        if (foreground != null) {
            style = style.fg(foreground);
        }
        if (background != null) {
            style = style.bg(background);
        }
        for (Modifier modifier : modifiers) {
            style = style.addModifier(modifier);
        }
        if (width != null) {
            style = style.withExtension(Width.class, width);
        }

        return style;
    }

    /**
     * Returns true if this style has any properties set.
     *
     * @return true if any properties are set
     */
    public boolean hasProperties() {
        return foreground != null || background != null ||
                !modifiers.isEmpty() || padding != null || alignment != null ||
                borderType != null || width != null || flex != null ||
                direction != null || margin != null || spacing != null ||
                heightConstraint != null || widthConstraint != null ||
                !additionalProperties.isEmpty();
    }

    /**
     * Creates a new resolver that uses this resolver's properties but falls back
     * to the given resolver for CSS-inheritable properties not set in this resolver.
     * <p>
     * Per CSS semantics, only certain properties inherit from parent to child:
     * <ul>
     *   <li>Inheritable: color (foreground), text-style (modifiers), border-type</li>
     *   <li>Non-inheritable: spacing, flex, direction, margin, padding, alignment, width, background</li>
     * </ul>
     *
     * @param fallback the fallback resolver for missing inheritable properties
     * @return a new resolver with fallback behavior for inheritable properties only
     */
    public CssStyleResolver withFallback(CssStyleResolver fallback) {
        if (fallback == null) {
            return this;
        }
        // For modifiers, pass null if both are empty to avoid EnumSet.copyOf issue
        Set<Modifier> mergedModifiers = !modifiers.isEmpty() ? modifiers
                : !fallback.modifiers.isEmpty() ? fallback.modifiers
                : null;
        return new CssStyleResolver(
                // Inheritable properties - fall back to parent
                foreground != null ? foreground : fallback.foreground,
                background,  // NOT inherited per CSS spec
                mergedModifiers,
                // Non-inheritable properties - use only this element's values
                padding,
                alignment,
                borderType != null ? borderType : fallback.borderType,  // Inherit for nested panels
                width,
                flex,       // Layout-specific, not inherited
                direction,  // Layout-specific, not inherited
                margin,           // Element-specific, not inherited
                spacing,          // Layout-specific, not inherited
                heightConstraint, // Element-specific, not inherited
                widthConstraint,  // Element-specific, not inherited
                mergeProperties(additionalProperties, fallback.additionalProperties)
        );
    }

    private static Map<String, String> mergeProperties(Map<String, String> primary, Map<String, String> fallback) {
        if (fallback.isEmpty()) {
            return primary;
        }
        if (primary.isEmpty()) {
            return fallback;
        }
        Map<String, String> merged = new HashMap<>(fallback);
        merged.putAll(primary);
        return merged;
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
        private Color foreground;
        private Color background;
        private final Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
        private Padding padding;
        private Alignment alignment;
        private BorderType borderType;
        private Width width;
        private Flex flex;
        private Direction direction;
        private Margin margin;
        private Integer spacing;
        private Constraint heightConstraint;
        private Constraint widthConstraint;
        private final Map<String, String> additionalProperties = new HashMap<>();

        private Builder() {
        }

        /**
         * Sets the foreground color.
         *
         * @param color the foreground color
         * @return this builder
         */
        public Builder foreground(Color color) {
            this.foreground = color;
            return this;
        }

        /**
         * Sets the background color.
         *
         * @param color the background color
         * @return this builder
         */
        public Builder background(Color color) {
            this.background = color;
            return this;
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
            this.modifiers.addAll(modifiers);
            return this;
        }

        /**
         * Sets the padding.
         *
         * @param padding the padding
         * @return this builder
         */
        public Builder padding(Padding padding) {
            this.padding = padding;
            return this;
        }

        /**
         * Sets the text alignment.
         *
         * @param alignment the alignment
         * @return this builder
         */
        public Builder alignment(Alignment alignment) {
            this.alignment = alignment;
            return this;
        }

        /**
         * Sets the border type.
         *
         * @param borderType the border type
         * @return this builder
         */
        public Builder borderType(BorderType borderType) {
            this.borderType = borderType;
            return this;
        }

        /**
         * Sets the width behavior.
         *
         * @param width the width
         * @return this builder
         */
        public Builder width(Width width) {
            this.width = width;
            return this;
        }

        /**
         * Sets the flex layout mode.
         *
         * @param flex the flex mode
         * @return this builder
         */
        public Builder flex(Flex flex) {
            this.flex = flex;
            return this;
        }

        /**
         * Sets the layout direction.
         *
         * @param direction the direction
         * @return this builder
         */
        public Builder direction(Direction direction) {
            this.direction = direction;
            return this;
        }

        /**
         * Sets the margin.
         *
         * @param margin the margin
         * @return this builder
         */
        public Builder margin(Margin margin) {
            this.margin = margin;
            return this;
        }

        /**
         * Sets the spacing (gap between elements).
         *
         * @param spacing the spacing
         * @return this builder
         */
        public Builder spacing(Integer spacing) {
            this.spacing = spacing;
            return this;
        }

        /**
         * Sets the height constraint (for vertical layouts).
         *
         * @param constraint the height constraint
         * @return this builder
         */
        public Builder heightConstraint(Constraint constraint) {
            this.heightConstraint = constraint;
            return this;
        }

        /**
         * Sets the width constraint (for horizontal layouts).
         *
         * @param constraint the width constraint
         * @return this builder
         */
        public Builder widthConstraint(Constraint constraint) {
            this.widthConstraint = constraint;
            return this;
        }

        /**
         * Adds an additional property.
         *
         * @param name  the property name
         * @param value the property value
         * @return this builder
         */
        public Builder property(String name, String value) {
            this.additionalProperties.put(name, value);
            return this;
        }

        /**
         * Builds the CssStyleResolver.
         *
         * @return the built resolver
         */
        public CssStyleResolver build() {
            return new CssStyleResolver(foreground, background, modifiers, padding,
                    alignment, borderType, width, flex, direction, margin, spacing,
                    heightConstraint, widthConstraint, additionalProperties);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CssStyleResolver{");
        if (foreground != null) {
            sb.append("fg=").append(foreground).append(", ");
        }
        if (background != null) {
            sb.append("bg=").append(background).append(", ");
        }
        if (!modifiers.isEmpty()) {
            sb.append("modifiers=").append(modifiers).append(", ");
        }
        if (padding != null) {
            sb.append("padding=").append(padding).append(", ");
        }
        if (alignment != null) {
            sb.append("alignment=").append(alignment).append(", ");
        }
        if (borderType != null) {
            sb.append("borderType=").append(borderType).append(", ");
        }
        if (flex != null) {
            sb.append("flex=").append(flex).append(", ");
        }
        if (direction != null) {
            sb.append("direction=").append(direction).append(", ");
        }
        if (margin != null) {
            sb.append("margin=").append(margin).append(", ");
        }
        if (spacing != null) {
            sb.append("spacing=").append(spacing).append(", ");
        }
        if (heightConstraint != null) {
            sb.append("height=").append(heightConstraint).append(", ");
        }
        if (widthConstraint != null) {
            sb.append("width=").append(widthConstraint).append(", ");
        }
        if (!additionalProperties.isEmpty()) {
            sb.append("properties=").append(additionalProperties);
        }
        sb.append("}");
        return sb.toString();
    }
}

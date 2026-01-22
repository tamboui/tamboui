/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Direction;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Margin;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Width;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Padding;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Registry of CSS property converters.
 * <p>
 * Maps CSS property names to their converters and provides
 * convenience methods for converting property values.
 */
public final class PropertyRegistry {

    private final Map<String, PropertyConverter<?>> converters;
    private final dev.tamboui.style.ColorConverter coreColorConverter;
    private final ModifierConverter modifierConverter;
    private final SpacingConverter spacingConverter;
    private final AlignmentConverter alignmentConverter;
    private final BorderTypeConverter borderTypeConverter;
    private final WidthConverter widthConverter;
    private final FlexConverter flexConverter;
    private final DirectionConverter directionConverter;
    private final MarginConverter marginConverter;
    private final IntegerConverter integerConverter;
    private final ConstraintConverter constraintConverter;

    private PropertyRegistry() {
        this.converters = new HashMap<>();
        this.coreColorConverter = dev.tamboui.style.ColorConverter.INSTANCE;
        this.modifierConverter = new ModifierConverter();
        this.spacingConverter = new SpacingConverter();
        this.alignmentConverter = new AlignmentConverter();
        this.borderTypeConverter = new BorderTypeConverter();
        this.widthConverter = new WidthConverter();
        this.flexConverter = new FlexConverter();
        this.directionConverter = new DirectionConverter();
        this.marginConverter = new MarginConverter();
        this.integerConverter = new IntegerConverter();
        this.constraintConverter = new ConstraintConverter();

        // Register default converters - create adapter for core color converter
        PropertyConverter<Color> colorAdapter = (value, variables) -> {
            String resolved = PropertyConverter.resolveVariables(value, variables);
            return coreColorConverter.convert(resolved);
        };
        converters.put("color", colorAdapter);
        converters.put("background", colorAdapter);
        converters.put("background-color", colorAdapter);
        converters.put("border-color", colorAdapter);
        converters.put("text-style", modifierConverter);
        converters.put("padding", spacingConverter);
        converters.put("text-align", alignmentConverter);
        converters.put("border-type", borderTypeConverter);
        converters.put("width", constraintConverter);
        converters.put("flex", flexConverter);
        converters.put("direction", directionConverter);
        converters.put("margin", marginConverter);
        converters.put("spacing", integerConverter);
        converters.put("height", constraintConverter);
    }

    /**
     * Creates a new property registry with default converters.
     *
     * @return a new registry
     */
    public static PropertyRegistry createDefault() {
        return new PropertyRegistry();
    }

    /**
     * Converts a CSS color value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted color, or empty if conversion fails
     */
    public Optional<Color> convertColor(String value, Map<String, String> variables) {
        String resolved = PropertyConverter.resolveVariables(value, variables);
        return coreColorConverter.convert(resolved);
    }

    /**
     * Converts CSS text-style value to modifiers.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted modifiers, or empty if conversion fails
     */
    public Optional<Set<Modifier>> convertModifiers(String value, Map<String, String> variables) {
        return modifierConverter.convert(value, variables);
    }

    /**
     * Converts CSS padding value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted padding, or empty if conversion fails
     */
    public Optional<Padding> convertPadding(String value, Map<String, String> variables) {
        return spacingConverter.convert(value, variables);
    }

    /**
     * Converts CSS text-align value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted alignment, or empty if conversion fails
     */
    public Optional<Alignment> convertAlignment(String value, Map<String, String> variables) {
        return alignmentConverter.convert(value, variables);
    }

    /**
     * Converts CSS border-type value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted border type, or empty if conversion fails
     */
    public Optional<BorderType> convertBorderType(String value, Map<String, String> variables) {
        return borderTypeConverter.convert(value, variables);
    }

    /**
     * Converts CSS width value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted width, or empty if conversion fails
     */
    public Optional<Width> convertWidth(String value, Map<String, String> variables) {
        return widthConverter.convert(value, variables);
    }

    /**
     * Converts CSS flex value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted flex, or empty if conversion fails
     */
    public Optional<Flex> convertFlex(String value, Map<String, String> variables) {
        return flexConverter.convert(value, variables);
    }

    /**
     * Converts CSS direction value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted direction, or empty if conversion fails
     */
    public Optional<Direction> convertDirection(String value, Map<String, String> variables) {
        return directionConverter.convert(value, variables);
    }

    /**
     * Converts CSS margin value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted margin, or empty if conversion fails
     */
    public Optional<Margin> convertMargin(String value, Map<String, String> variables) {
        return marginConverter.convert(value, variables);
    }

    /**
     * Converts CSS spacing (integer) value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted spacing, or empty if conversion fails
     */
    public Optional<Integer> convertSpacing(String value, Map<String, String> variables) {
        return integerConverter.convert(value, variables);
    }

    /**
     * Converts CSS height (constraint) value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted constraint, or empty if conversion fails
     */
    public Optional<Constraint> convertConstraint(String value, Map<String, String> variables) {
        return constraintConverter.convert(value, variables);
    }

    /**
     * Gets a converter for the given property name.
     *
     * @param propertyName the CSS property name
     * @return the converter, or empty if not registered
     */
    public Optional<PropertyConverter<?>> getConverter(String propertyName) {
        return Optional.ofNullable(converters.get(propertyName));
    }

    /**
     * Registers a custom converter for a property.
     *
     * @param propertyName the CSS property name
     * @param converter    the converter
     */
    public void register(String propertyName, PropertyConverter<?> converter) {
        converters.put(propertyName, converter);
    }
}

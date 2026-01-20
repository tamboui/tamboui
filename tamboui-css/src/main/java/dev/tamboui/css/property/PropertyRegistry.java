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
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Width;
import dev.tamboui.widgets.block.BorderSet;
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

    private PropertyRegistry() {
        this.converters = new HashMap<>();

        // Register default converters - create adapter for core color converter
        PropertyConverter<Color> colorAdapter = (value, variables) -> {
            String resolved = PropertyConverter.resolveVariables(value, variables);
            return ColorConverter.INSTANCE.convert(resolved);
        };
        converters.put("color", colorAdapter);
        converters.put("background", colorAdapter);
        converters.put("background-color", colorAdapter);
        converters.put("border-color", colorAdapter);
        converters.put("text-style", ModifierConverter.INSTANCE);
        converters.put("padding", SpacingConverter.INSTANCE);
        converters.put("text-align", AlignmentConverter.INSTANCE);
        converters.put("border-type", BorderTypeConverter.INSTANCE);
        converters.put("width", ConstraintConverter.INSTANCE);
        converters.put("flex", FlexConverter.INSTANCE);
        converters.put("direction", DirectionConverter.INSTANCE);
        converters.put("margin", MarginConverter.INSTANCE);
        converters.put("spacing", IntegerConverter.INSTANCE);
        converters.put("height", ConstraintConverter.INSTANCE);
        converters.put("border-chars", BorderSetConverter.INSTANCE);
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
        return ColorConverter.INSTANCE.convert(resolved);
    }

    /**
     * Converts CSS text-style value to modifiers.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted modifiers, or empty if conversion fails
     */
    public Optional<Set<Modifier>> convertModifiers(String value, Map<String, String> variables) {
        return ModifierConverter.INSTANCE.convert(value, variables);
    }

    /**
     * Converts CSS padding value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted padding, or empty if conversion fails
     */
    public Optional<Padding> convertPadding(String value, Map<String, String> variables) {
        return SpacingConverter.INSTANCE.convert(value, variables);
    }

    /**
     * Converts CSS text-align value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted alignment, or empty if conversion fails
     */
    public Optional<Alignment> convertAlignment(String value, Map<String, String> variables) {
        return AlignmentConverter.INSTANCE.convert(value, variables);
    }

    /**
     * Converts CSS border-type value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted border type, or empty if conversion fails
     */
    public Optional<BorderType> convertBorderType(String value, Map<String, String> variables) {
        return BorderTypeConverter.INSTANCE.convert(value, variables);
    }

    /**
     * Converts CSS border-chars value to a BorderSet.
     *
     * @param value     the CSS value (8 quoted strings)
     * @param variables the CSS variables
     * @return the converted border set, or empty if conversion fails
     */
    public Optional<BorderSet> convertBorderSet(String value, Map<String, String> variables) {
        return BorderSetConverter.INSTANCE.convert(value, variables);
    }

    /**
     * Converts CSS width value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted width, or empty if conversion fails
     */
    public Optional<Width> convertWidth(String value, Map<String, String> variables) {
        return WidthConverter.INSTANCE.convert(value, variables);
    }

    /**
     * Converts CSS flex value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted flex, or empty if conversion fails
     */
    public Optional<Flex> convertFlex(String value, Map<String, String> variables) {
        return FlexConverter.INSTANCE.convert(value, variables);
    }

    /**
     * Converts CSS direction value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted direction, or empty if conversion fails
     */
    public Optional<Direction> convertDirection(String value, Map<String, String> variables) {
        return DirectionConverter.INSTANCE.convert(value, variables);
    }

    /**
     * Converts CSS margin value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted margin, or empty if conversion fails
     */
    public Optional<Margin> convertMargin(String value, Map<String, String> variables) {
        return MarginConverter.INSTANCE.convert(value, variables);
    }

    /**
     * Converts CSS spacing (integer) value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted spacing, or empty if conversion fails
     */
    public Optional<Integer> convertSpacing(String value, Map<String, String> variables) {
        return IntegerConverter.INSTANCE.convert(value, variables);
    }

    /**
     * Converts CSS height (constraint) value.
     *
     * @param value     the CSS value
     * @param variables the CSS variables
     * @return the converted constraint, or empty if conversion fails
     */
    public Optional<Constraint> convertConstraint(String value, Map<String, String> variables) {
        return ConstraintConverter.INSTANCE.convert(value, variables);
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

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.cascade;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.model.PropertyValue;
import dev.tamboui.css.model.Rule;
import dev.tamboui.css.property.PropertyConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;
import dev.tamboui.style.StandardProperties;

/**
 * Resolves CSS cascade and specificity to produce final computed styles.
 * <p>
 * The cascade algorithm:
 * <ol>
 *   <li>Find all rules whose selectors match the element</li>
 *   <li>Sort by specificity (higher wins)</li>
 *   <li>For equal specificity, later rules win (source order)</li>
 *   <li>!important declarations override all non-important</li>
 *   <li>Merge all matching declarations into a final style, ignoring those
 *       whose value cannot be converted, as in CSS</li>
 * </ol>
 * <p>
 * Unknown properties (those not registered in {@link PropertyRegistry}) are handled
 * according to the configured {@link UnknownPropertyBehavior}.
 */
public final class CascadeResolver {

    private static final Logger LOGGER = Logger.getLogger(CascadeResolver.class.getName());

    private final UnknownPropertyBehavior unknownPropertyBehavior;

    /**
     * Creates a new cascade resolver with default behavior (IGNORE unknown properties).
     */
    public CascadeResolver() {
        this(UnknownPropertyBehavior.IGNORE);
    }

    /**
     * Creates a new cascade resolver with the specified unknown property behavior.
     *
     * @param unknownPropertyBehavior how to handle unknown CSS properties
     */
    public CascadeResolver(UnknownPropertyBehavior unknownPropertyBehavior) {
        this.unknownPropertyBehavior = unknownPropertyBehavior != null
                ? unknownPropertyBehavior
                : UnknownPropertyBehavior.IGNORE;
    }

    /**
     * Resolves the final computed style for an element.
     *
     * @param element   the element to style
     * @param state     the pseudo-class state (focus, hover, etc.)
     * @param ancestors the ancestor chain from root to parent
     * @param rules     all rules from the stylesheet
     * @param variables CSS variables for value resolution
     * @return the resolved style
     */
    public CssStyleResolver resolve(Styleable element,
                                     PseudoClassState state,
                                     List<Styleable> ancestors,
                                     List<Rule> rules,
                                     Map<String, String> variables) {
        // 1. Find matching rules
        List<MatchedRule> matches = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule.selector().matches(element, state, ancestors)) {
                matches.add(new MatchedRule(rule));
            }
        }

        if (matches.isEmpty()) {
            return CssStyleResolver.empty();
        }

        // 2. Sort by specificity, then source order
        Collections.sort(matches);

        // 3. Merge declarations
        Map<String, PropertyValue> normalProps = new LinkedHashMap<>();
        Map<String, PropertyValue> importantProps = new LinkedHashMap<>();

        for (MatchedRule match : matches) {
            for (Map.Entry<String, PropertyValue> entry : match.rule.declarations().entrySet()) {
                String prop = entry.getKey();
                PropertyValue value = entry.getValue();

                // As in CSS, an invalid value is ignored, so it cannot override a valid one
                if (hasInvalidValue(prop, value, variables)) {
                    continue;
                }

                if (value.important()) {
                    importantProps.put(prop, value);
                } else {
                    normalProps.put(prop, value);
                }
            }
        }

        // Important overrides normal
        Map<String, PropertyValue> finalProps = new LinkedHashMap<>(normalProps);
        finalProps.putAll(importantProps);

        // 4. Convert to CssStyleResolver
        return buildCssStyleResolver(finalProps, variables);
    }

    private CssStyleResolver buildCssStyleResolver(Map<String, PropertyValue> props,
                                                    Map<String, String> variables) {
        CssStyleResolver.Builder builder = CssStyleResolver.builder();

        for (Map.Entry<String, PropertyValue> entry : props.entrySet()) {
            String prop = entry.getKey();
            PropertyValue pv = entry.getValue();
            String value = pv.raw();

            // Handle "inherit" keyword (child wants parent's value)
            if (pv.isInherit()) {
                builder.markAsInherited(prop);
                continue;
            }

            String resolvedValue = PropertyConverter.resolveVariables(value, variables);

            // Try to look up the property in the registry
            Optional<PropertyDefinition<?>> propDef = findProperty(prop);

            if (propDef.isPresent()) {
                // Handle known properties through the registry
                convertAndSet(builder, propDef.get(), resolvedValue);
            } else {
                // Handle unknown properties according to configured behavior
                handleUnknownProperty(builder, prop, resolvedValue);
            }
        }

        return builder.build();
    }

    /**
     * Converts and sets a property value using the PropertyDefinition.
     */
    @SuppressWarnings("unchecked")
    private <T> void convertAndSet(CssStyleResolver.Builder builder,
                                   PropertyDefinition<T> property,
                                   String resolvedValue) {
        Optional<T> converted = convert(property, resolvedValue);
        converted.ifPresent(v -> builder.set(property, v));
    }

    /**
     * Returns whether a declaration sets a known property to a value that cannot
     * be converted. Unknown properties are not checked: they are kept as raw values
     * for the widget that defines them.
     */
    private static boolean hasInvalidValue(String prop, PropertyValue value, Map<String, String> variables) {
        if (value.isInherit()) {
            return false;
        }
        Optional<PropertyDefinition<?>> propDef = findProperty(prop);
        if (!propDef.isPresent()) {
            return false;
        }
        String resolvedValue = PropertyConverter.resolveVariables(value.raw(), variables);
        return !convert(propDef.get(), resolvedValue).isPresent();
    }

    /**
     * Looks up a property definition by name. The standard properties are checked
     * as well, because they are only added to the registry once
     * {@link StandardProperties} has been used, which can be after the first cascade.
     *
     * @param prop the property name
     * @return the property definition, or empty if not registered
     */
    public static Optional<PropertyDefinition<?>> findProperty(String prop) {
        Optional<PropertyDefinition<?>> registered = PropertyRegistry.byName(prop);
        return registered.isPresent() ? registered : StandardProperties.byName(prop);
    }

    /**
     * Converts a CSS value with the property's converter. A converter that throws
     * is treated like one that returns empty, so an invalid value is ignored
     * instead of failing the rendering.
     *
     * @param property the property definition
     * @param value    the CSS value, with variables resolved
     * @param <T>      the type of the property value
     * @return the converted value, or empty if the value is invalid
     */
    static <T> Optional<T> convert(PropertyDefinition<T> property, String value) {
        try {
            Optional<T> result = property.convert(value);
            if (!result.isPresent() && dev.tamboui.css.jfr.InvalidValueEvent.enabled()) {
                String reason = dev.tamboui.css.parser.CssParser.isOutOfRange(value) ? "out-of-range" : "unconvertible";
                dev.tamboui.css.jfr.InvalidValueEvent.commit(property.name(), value, reason);
            }
            return result;
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            LOGGER.log(Level.FINE, e, () -> "Ignoring invalid value of CSS property " + property.name() + ": " + value);
            if (dev.tamboui.css.jfr.InvalidValueEvent.enabled()) {
                dev.tamboui.css.jfr.InvalidValueEvent.commit(property.name(), value, msg);
            }
            return Optional.empty();
        }
    }

    /**
     * Handles unknown properties according to the configured behavior.
     * <p>
     * Unknown properties are stored as raw values for lazy conversion by
     * widget-defined properties. The behavior setting controls whether to
     * also log a warning or throw an exception.
     */
    private void handleUnknownProperty(CssStyleResolver.Builder builder, String prop, String value) {
        switch (unknownPropertyBehavior) {
            case IGNORE:
                // Store as raw value for lazy conversion
                builder.setRaw(prop, value);
                break;
            case WARN:
                // Store as raw value AND log a warning
                builder.setRaw(prop, value);
                LOGGER.log(Level.WARNING, "Unknown CSS property: {0}", prop);
                break;
            case FAIL:
                throw new UnknownCssPropertyException(prop, value);
                // no default needed - all enum values covered
        }
    }

    /**
     * Helper class for sorting matched rules.
     */
    private static final class MatchedRule implements Comparable<MatchedRule> {
        final Rule rule;
        final int specificity;
        final int sourceOrder;

        MatchedRule(Rule rule) {
            this.rule = rule;
            this.specificity = rule.specificity();
            this.sourceOrder = rule.sourceOrder();
        }

        @Override
        public int compareTo(MatchedRule other) {
            // Lower specificity first (so higher specificity wins when iterating)
            int specCompare = Integer.compare(this.specificity, other.specificity);
            if (specCompare != 0) {
                return specCompare;
            }
            // Lower source order first (so later rules win)
            return Integer.compare(this.sourceOrder, other.sourceOrder);
        }
    }
}

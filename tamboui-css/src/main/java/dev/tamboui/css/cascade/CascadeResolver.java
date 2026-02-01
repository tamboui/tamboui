/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.cascade;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.model.PropertyValue;
import dev.tamboui.css.model.Rule;
import dev.tamboui.css.property.PropertyConverter;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.PropertyRegistry;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves CSS cascade and specificity to produce final computed styles.
 * <p>
 * The cascade algorithm:
 * <ol>
 *   <li>Find all rules whose selectors match the element</li>
 *   <li>Sort by specificity (higher wins)</li>
 *   <li>For equal specificity, later rules win (source order)</li>
 *   <li>!important declarations override all non-important</li>
 *   <li>Merge all matching declarations into a final style</li>
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
            Optional<PropertyDefinition<?>> propDef = PropertyRegistry.byName(prop);

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
        Optional<T> converted = property.convert(resolvedValue);
        converted.ifPresent(v -> builder.set(property, v));
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

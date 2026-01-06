/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.cascade;

import dev.tamboui.css.Styleable;
import dev.tamboui.css.model.PropertyValue;
import dev.tamboui.css.model.Rule;
import dev.tamboui.css.property.PropertyConverter;
import dev.tamboui.css.property.PropertyRegistry;

import java.util.*;

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
 */
public final class CascadeResolver {

    private final PropertyRegistry propertyRegistry;

    /**
     * Creates a new cascade resolver with the default property registry.
     */
    public CascadeResolver() {
        this.propertyRegistry = PropertyRegistry.createDefault();
    }

    /**
     * Creates a cascade resolver with a custom property registry.
     */
    public CascadeResolver(PropertyRegistry propertyRegistry) {
        this.propertyRegistry = propertyRegistry;
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
            String value = entry.getValue().raw();

            switch (prop) {
                case "color":
                    propertyRegistry.convertColor(value, variables)
                            .ifPresent(builder::foreground);
                    break;
                case "background":
                case "background-color":
                    propertyRegistry.convertColor(value, variables)
                            .ifPresent(builder::background);
                    break;
                case "text-style":
                    propertyRegistry.convertModifiers(value, variables)
                            .ifPresent(builder::addModifiers);
                    break;
                case "padding":
                    propertyRegistry.convertPadding(value, variables)
                            .ifPresent(builder::padding);
                    break;
                case "text-align":
                    propertyRegistry.convertAlignment(value, variables)
                            .ifPresent(builder::alignment);
                    break;
                case "border-type":
                    propertyRegistry.convertBorderType(value, variables)
                            .ifPresent(builder::borderType);
                    break;
                case "width":
                    propertyRegistry.convertWidth(value, variables)
                            .ifPresent(builder::width);
                    break;
                default:
                    // Store as additional property for later use
                    builder.property(prop, PropertyConverter.resolveVariables(value, variables));
                    break;
            }
        }

        return builder.build();
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

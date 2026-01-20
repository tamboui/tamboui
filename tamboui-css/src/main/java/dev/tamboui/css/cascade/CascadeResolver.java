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
                case "border-chars":
                    // Store as additional property - Panel will parse it
                    builder.property(prop, PropertyConverter.resolveVariables(value, variables));
                    break;
                case "border-top":
                case "border-bottom":
                case "border-left":
                case "border-right":
                case "border-top-left":
                case "border-top-right":
                case "border-bottom-left":
                case "border-bottom-right":
                    // Store individual border character overrides in additionalProperties
                    // Only store if it's a valid border character:
                    // - Quoted string (any content, including empty)
                    // - Single character or short string (for unicode)
                    // Skip values that look like parser errors (e.g., "height: 3" when
                    // the parser consumed the next property due to missing semicolon)
                    String resolvedBorderValue = PropertyConverter.resolveVariables(value, variables);
                    if (isValidBorderChar(resolvedBorderValue)) {
                        builder.property(prop, parseQuotedChar(resolvedBorderValue));
                    }
                    break;
                case "width":
                    propertyRegistry.convertConstraint(value, variables)
                            .ifPresent(builder::widthConstraint);
                    break;
                case "flex":
                    propertyRegistry.convertFlex(value, variables)
                            .ifPresent(builder::flex);
                    break;
                case "direction":
                    propertyRegistry.convertDirection(value, variables)
                            .ifPresent(builder::direction);
                    break;
                case "margin":
                    propertyRegistry.convertMargin(value, variables)
                            .ifPresent(builder::margin);
                    break;
                case "spacing":
                    propertyRegistry.convertSpacing(value, variables)
                            .ifPresent(builder::spacing);
                    break;
                case "height":
                    propertyRegistry.convertConstraint(value, variables)
                            .ifPresent(builder::heightConstraint);
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
     * Checks if a value is quoted (starts and ends with matching quotes).
     */
    private boolean isQuoted(String value) {
        if (value == null || value.length() < 2) {
            return false;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        return (first == '"' || first == '\'') && first == last;
    }

    /**
     * Checks if a value is a valid border character.
     * Valid border characters are:
     * - Quoted strings (any content, including empty - explicit intent)
     * - Single characters or short unicode sequences (up to 4 chars)
     * - NOT empty unquoted strings
     * - NOT long strings that look like parser errors (e.g., "height: 3")
     */
    private boolean isValidBorderChar(String value) {
        if (value == null) {
            return false;
        }
        // Quoted strings are always valid (explicit user intent)
        if (isQuoted(value)) {
            return true;
        }
        // Unquoted: must be non-empty and short (single char or unicode grapheme)
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        // Allow short strings (single char, or unicode that might be multiple code units)
        // Reject anything longer than 4 characters - likely a parser error
        return trimmed.length() <= 4 && !trimmed.contains(":");
    }

    /**
     * Parses a quoted character value like {@code 'x'} or {@code "─"}.
     * Returns the content without quotes, or the original value if not quoted.
     */
    private String parseQuotedChar(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' || first == '\'') && first == last) {
            return value.substring(1, value.length() - 1);
        }
        return value;
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

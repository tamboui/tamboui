/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts CSS property values to typed objects.
 *
 * @param <T>
 *            the target type
 */
public interface PropertyConverter<T> {

    /**
     * Pattern matching CSS variable references like $variable-name. Variable names
     * can contain letters, digits, underscores, and hyphens, but must start with a
     * letter or underscore.
     */
    Pattern VAR_PATTERN = Pattern.compile("\\$([a-zA-Z_][a-zA-Z0-9_-]*)");

    /**
     * Converts a CSS value string to the target type.
     *
     * @param value
     *            the CSS value string
     * @param variables
     *            the CSS variables for resolving $references
     * @return the converted value, or empty if conversion fails
     */
    Optional<T> convert(String value, Map<String, String> variables);

    /**
     * Resolves variable references in a value.
     * <p>
     * This method extracts all $variable patterns from the input string and
     * replaces each with its value from the variables map. This approach is
     * deterministic regardless of the iteration order of the variables map, because
     * resolution is driven by the patterns found in the input string rather than by
     * iterating over the map.
     *
     * @param value
     *            the value that may contain $variable references
     * @param variables
     *            the variables map
     * @return the resolved value
     */
    static String resolveVariables(String value, Map<String, String> variables) {
        if (value == null || !value.contains("$")) {
            return value;
        }

        Matcher matcher = VAR_PATTERN.matcher(value);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(value, lastEnd, matcher.start());

            String varName = matcher.group(1);
            String replacement = variables.get(varName);
            if (replacement != null) {
                result.append(replacement);
            } else {
                result.append(matcher.group(0));
            }
            lastEnd = matcher.end();
        }

        result.append(value.substring(lastEnd));
        return result.toString();
    }
}

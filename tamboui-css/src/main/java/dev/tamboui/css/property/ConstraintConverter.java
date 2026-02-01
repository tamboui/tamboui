/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import dev.tamboui.layout.Constraint;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts CSS constraint values to Constraint objects.
 * <p>
 * Supports the following formats:
 * <ul>
 *   <li>{@code fill} - Fill with weight 1</li>
 *   <li>{@code fill(2)} - Fill with specified weight</li>
 *   <li>{@code 10} - Fixed length of 10 cells</li>
 *   <li>{@code 50%} - 50% of available space</li>
 *   <li>{@code min(10)} - Minimum 10 cells</li>
 *   <li>{@code max(20)} - Maximum 20 cells</li>
 *   <li>{@code 1/3} - Ratio (1 part of 3)</li>
 * </ul>
 */
public final class ConstraintConverter implements PropertyConverter<Constraint> {

    /** Singleton instance. */
    public static final ConstraintConverter INSTANCE = new ConstraintConverter();

    private ConstraintConverter() {
    }

    private static final Pattern FILL_PATTERN = Pattern.compile("fill(?:\\((\\d+)\\))?");
    private static final Pattern FR_PATTERN = Pattern.compile("(\\d+)fr");
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+)%");
    private static final Pattern MIN_PATTERN = Pattern.compile("min\\((\\d+)\\)");
    private static final Pattern MAX_PATTERN = Pattern.compile("max\\((\\d+)\\)");
    private static final Pattern RATIO_PATTERN = Pattern.compile("(\\d+)/(\\d+)");
    private static final Pattern LENGTH_PATTERN = Pattern.compile("\\d+");

    @Override
    public Optional<Constraint> convert(String value, Map<String, String> variables) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String resolved = PropertyConverter.resolveVariables(value.trim(), variables).toLowerCase();

        // Try "fit" keyword
        if ("fit".equals(resolved)) {
            return Optional.of(Constraint.fit());
        }

        // Try fill pattern: "fill" or "fill(2)"
        Matcher fillMatcher = FILL_PATTERN.matcher(resolved);
        if (fillMatcher.matches()) {
            String weight = fillMatcher.group(1);
            if (weight != null) {
                return Optional.of(Constraint.fill(Integer.parseInt(weight)));
            }
            return Optional.of(Constraint.fill());
        }

        // Try fr pattern: "1fr", "2fr" (alias for fill)
        Matcher frMatcher = FR_PATTERN.matcher(resolved);
        if (frMatcher.matches()) {
            int weight = Integer.parseInt(frMatcher.group(1));
            return Optional.of(Constraint.fill(weight));
        }

        // Try percentage pattern: "50%"
        Matcher percentMatcher = PERCENT_PATTERN.matcher(resolved);
        if (percentMatcher.matches()) {
            int percent = Integer.parseInt(percentMatcher.group(1));
            return Optional.of(Constraint.percentage(percent));
        }

        // Try min pattern: "min(10)"
        Matcher minMatcher = MIN_PATTERN.matcher(resolved);
        if (minMatcher.matches()) {
            int minValue = Integer.parseInt(minMatcher.group(1));
            return Optional.of(Constraint.min(minValue));
        }

        // Try max pattern: "max(20)"
        Matcher maxMatcher = MAX_PATTERN.matcher(resolved);
        if (maxMatcher.matches()) {
            int maxValue = Integer.parseInt(maxMatcher.group(1));
            return Optional.of(Constraint.max(maxValue));
        }

        // Try ratio pattern: "1/3"
        Matcher ratioMatcher = RATIO_PATTERN.matcher(resolved);
        if (ratioMatcher.matches()) {
            int numerator = Integer.parseInt(ratioMatcher.group(1));
            int denominator = Integer.parseInt(ratioMatcher.group(2));
            return Optional.of(Constraint.ratio(numerator, denominator));
        }

        // Try plain number (length): "10"
        if (LENGTH_PATTERN.matcher(resolved).matches()) {
            int length = Integer.parseInt(resolved);
            return Optional.of(Constraint.length(length));
        }

        return Optional.empty();
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.css.property;

import dev.tamboui.widgets.block.BorderSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Converts CSS border-chars values to BorderSet.
 * <p>
 * Format: 8 quoted strings representing (in order):
 * top-horizontal, bottom-horizontal, left-vertical, right-vertical,
 * top-left, top-right, bottom-left, bottom-right.
 * <p>
 * Empty strings ({@code ""}) indicate that character should not be rendered.
 * <p>
 * Examples:
 * <pre>
 * border-chars: "─" "─" "│" "│" "┌" "┐" "└" "┘";   // full border
 * border-chars: "" "" "" "" "┌" "┐" "└" "┘";       // corners only
 * border-chars: "~" "~" "|" "|" "+" "+" "+" "+";   // custom chars
 * </pre>
 */
public final class BorderSetConverter implements PropertyConverter<BorderSet> {

    /** Singleton instance. */
    public static final BorderSetConverter INSTANCE = new BorderSetConverter();

    private BorderSetConverter() {
    }

    @Override
    public Optional<BorderSet> convert(String value, Map<String, String> variables) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String resolved = PropertyConverter.resolveVariables(value.trim(), variables);
        List<String> chars = parseQuotedStrings(resolved);

        if (chars.size() != 8) {
            return Optional.empty();
        }

        return Optional.of(new BorderSet(
                chars.get(0), // topHorizontal
                chars.get(1), // bottomHorizontal
                chars.get(2), // leftVertical
                chars.get(3), // rightVertical
                chars.get(4), // topLeft
                chars.get(5), // topRight
                chars.get(6), // bottomLeft
                chars.get(7)  // bottomRight
        ));
    }

    /**
     * Parses a string containing quoted values.
     * Supports both single and double quotes.
     */
    private List<String> parseQuotedStrings(String input) {
        List<String> result = new ArrayList<>();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '"' || c == '\'') {
                char quote = c;
                int start = i + 1;
                int end = input.indexOf(quote, start);
                if (end == -1) {
                    // Unterminated quote - return empty to signal parse error
                    return new ArrayList<>();
                }
                result.add(input.substring(start, end));
                i = end + 1;
            } else {
                i++;
            }
        }
        return result;
    }
}

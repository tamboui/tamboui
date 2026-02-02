/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.spinner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import dev.tamboui.style.PropertyConverter;

/**
 * Converts string values to {@link SpinnerFrameSet}.
 * <p>
 * Format: variable number of quoted strings representing the animation frames.
 * <p>
 * Examples:
 * <pre>
 * spinner-frames: "-" "\\" "|" "/";                   // classic line spinner
 * spinner-frames: "*" "+" "x" "+";                    // custom frames
 * spinner-frames: "⠋" "⠙" "⠹" "⠸" "⠼" "⠴" "⠦" "⠧";  // braille dots
 * </pre>
 */
public final class SpinnerFrameSetConverter implements PropertyConverter<SpinnerFrameSet> {

    /**
     * Singleton instance of the spinner frame set converter.
     */
    public static final SpinnerFrameSetConverter INSTANCE = new SpinnerFrameSetConverter();

    private SpinnerFrameSetConverter() {
    }

    @Override
    public Optional<SpinnerFrameSet> convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        List<String> frames = parseQuotedStrings(value.trim());

        if (frames.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(SpinnerFrameSet.of(frames));
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

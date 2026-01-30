/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.tamboui.layout.grid.GridArea;
import dev.tamboui.style.PropertyConverter;

/**
 * Converts CSS grid-template-areas value to {@link GridArea}.
 * <p>
 * Supports two formats:
 * <ul>
 * <li>Semicolon-separated rows: {@code "A A B; A A C; D D D"}</li>
 * <li>Quoted strings (CSS format): {@code "A A B" "A A C" "D D D"}</li>
 * </ul>
 * <p>
 * Example CSS:
 * 
 * <pre>
 * .dashboard {
 *     grid-template-areas: "header header header; nav main main; footer footer footer";
 * }
 * </pre>
 */
public final class GridAreaConverter implements PropertyConverter<GridArea> {

    /**
     * Singleton instance.
     */
    public static final GridAreaConverter INSTANCE = new GridAreaConverter();

    private static final Pattern QUOTED_STRING_PATTERN = Pattern.compile("\"([^\"]+)\"");

    private GridAreaConverter() {
    }

    @Override
    public Optional<GridArea> convert(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Optional.empty();
        }

        String trimmed = value.trim();
        String[] rows;

        if (trimmed.contains(";")) {
            // Semicolon-separated format: "A A B; A A C"
            rows = trimmed.split(";");
        } else if (trimmed.contains("\"")) {
            // Quoted string format: "A A B" "A A C"
            rows = parseQuotedStrings(trimmed);
        } else {
            // Single row
            rows = new String[]{trimmed};
        }

        // Trim each row
        for (int i = 0; i < rows.length; i++) {
            rows[i] = rows[i].trim();
        }

        // Remove empty rows
        rows = filterEmptyRows(rows);

        if (rows.length == 0) {
            return Optional.empty();
        }

        try {
            return Optional.of(GridArea.parse(rows));
        } catch (Exception e) {
            // Invalid template - return empty
            return Optional.empty();
        }
    }

    private String[] parseQuotedStrings(String value) {
        List<String> rows = new ArrayList<>();
        Matcher matcher = QUOTED_STRING_PATTERN.matcher(value);
        while (matcher.find()) {
            rows.add(matcher.group(1));
        }
        return rows.toArray(new String[0]);
    }

    private String[] filterEmptyRows(String[] rows) {
        List<String> nonEmpty = new ArrayList<>();
        for (String row : rows) {
            if (row != null && !row.isEmpty()) {
                nonEmpty.add(row);
            }
        }
        return nonEmpty.toArray(new String[0]);
    }
}

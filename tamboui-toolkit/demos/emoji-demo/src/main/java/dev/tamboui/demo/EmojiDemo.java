///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS dev.tamboui:tamboui-toolkit:LATEST
//DEPS dev.tamboui:tamboui-jline3-backend:LATEST

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Flex;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.Emoji;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.table.Row;
import dev.tamboui.widgets.table.TableState;
import dev.tamboui.style.Color;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static dev.tamboui.toolkit.Toolkit.*;
import static dev.tamboui.layout.Constraint.*;

/**
 * Demo showcasing emoji table with filtering.
 * <p>
 * Features:
 * <ul>
 *   <li>Text input for filtering emojis by name</li>
 *   <li>Scrollable table displaying filtered emojis</li>
 *   <li>Keyboard navigation (arrow keys, j/k for table, Tab for focus)</li>
 *   <li>Mouse scrollwheel support</li>
 * </ul>
 * <p>
 * Controls:
 * <ul>
 *   <li>Tab - Switch focus between filter input and table</li>
 *   <li>Type in filter - Filter emojis by name</li>
 *   <li>j/↓ - Move down in table</li>
 *   <li>k/↑ - Move up in table</li>
 *   <li>g - Go to first row</li>
 *   <li>G - Go to last row</li>
 *   <li>Mouse wheel - Scroll table</li>
 *   <li>q - Quit</li>
 * </ul>
 */
public class EmojiDemo {

    private final TextInputState filterState;
    private final TableState tableState;
    private final List<EmojiItem> allEmojis;
    private List<EmojiItem> filteredEmojis;
    private boolean useJavaFormat = false;  // Toggle between Java (uXXXX) and Unicode (U+XXXX) format

    private static class EmojiItem {
        final String emoji;
        final String name;

        EmojiItem(String emoji, String name) {
            this.emoji = emoji;
            this.name = name;
        }
    }

    private EmojiDemo() {
        this.filterState = new TextInputState();
        this.tableState = new TableState();
        
        // Load all emojis once
        Map<String, String> emojis = Emoji.emojis();
        this.allEmojis = emojis.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> new EmojiItem(entry.getValue(), entry.getKey()))
            .collect(Collectors.toList());
        
        this.filteredEmojis = new ArrayList<>(allEmojis);
        
        // Select first row
        if (!filteredEmojis.isEmpty()) {
            tableState.selectFirst();
        }
    }

    /**
     * Formats the Unicode codepoints of a string.
     * @param s the string to format
     * @param javaFormat if true, uses Java escape sequences (uXXXX), otherwise uses Unicode format (U+XXXX)
     */
    private String formatCodepoints(String s, boolean javaFormat) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            int codePoint = s.codePointAt(i);
            if (sb.length() > 0) {
                sb.append(' ');
            }
            if (javaFormat) {
                // Java format: uXXXX for BMP, \\uDXXX\\uDXXX for supplementary plane
                if (codePoint < 0x10000) {
                    sb.append(String.format("\\u%04X", codePoint));
                } else {
                    // Supplementary plane: convert to surrogate pair
                    int high = 0xD800 + ((codePoint - 0x10000) >> 10);
                    int low = 0xDC00 + ((codePoint - 0x10000) & 0x3FF);
                    sb.append(String.format("\\u%04X\\u%04X", high, low));
                }
            } else {
                // Unicode format: U+XXXX
                sb.append(String.format("U+%04X", codePoint));
            }
            i += Character.charCount(codePoint);
        }
        return sb.toString();
    }

    /**
     * Demo entry point.
     * @param args the CLI arguments
     * @throws Exception on unexpected error
     */
    public static void main(String[] args) throws Exception {
        new EmojiDemo().run();
    }

    /**
     * Runs the demo application.
     *
     * @throws Exception if an error occurs
     */
     public void run() throws Exception {
        var config = TuiConfig.builder()
            .tickRate(Duration.ofMillis(100))
            .mouseCapture(true)
            .build();

        try (var runner = ToolkitRunner.create(config)) {
            runner.run(() -> {
                // Apply text filter - match all words anywhere in the name
                String filterText = filterState.text().toLowerCase(Locale.ROOT).trim();
                if (filterText.isEmpty()) {
                    filteredEmojis = new ArrayList<>(allEmojis);
                } else {
                    // Split filter text into words and match all of them
                    String[] filterWords = filterText.split("\\s+");
                    filteredEmojis = allEmojis.stream()
                        .filter(item -> {
                            String itemName = item.name.toLowerCase(Locale.ROOT);
                            // All filter words must be present in the item name
                            for (String word : filterWords) {
                                if (!itemName.contains(word)) {
                                    return false;
                                }
                            }
                            return true;
                        })
                        .collect(Collectors.toList());
                }
                
                // Reset selection if current selection is out of bounds
                Integer selected = tableState.selected();
                if (selected != null && selected >= filteredEmojis.size()) {
                    if (filteredEmojis.isEmpty()) {
                        tableState.clearSelection();
                    } else {
                        tableState.selectLast(filteredEmojis.size());
                    }
                }
                
                // Build table rows with emoji, name, codepoints, and char width
                List<Row> tableRows = filteredEmojis.stream()
                    .map(item -> {
                        String codepoints = formatCodepoints(item.emoji, useJavaFormat);
                        int charWidth = CharWidth.of(item.emoji);
                        return Row.from(item.emoji, item.name, codepoints, String.valueOf(charWidth));
                    })
                    .collect(Collectors.toList());
                
                // Create table element with event handlers
                Element tableElement = table()
                    .header("Emoji", "Name", "Codepoints", "Width")
                    .rows(tableRows)
                    .widths(Constraint.length(4), Constraint.fill(), Constraint.percentage(40), Constraint.length(5))
                    .state(tableState)
                    .title("Emojis: " + filteredEmojis.size() + " / " + allEmojis.size())
                    .rounded()
                    .borderColor(Color.CYAN)
                    .focusable()
                    .id("emoji-table")
                    .fill()  // Table takes remaining space
                    .onKeyEvent(event -> {
                        // Handle table navigation
                        if (event.isDown() || event.isUp()) {
                            if (event.isDown()) {
                                tableState.selectNext(filteredEmojis.size());
                            } else {
                                tableState.selectPrevious();
                            }
                            return EventResult.HANDLED;
                        }
                        
                        // Handle vim-style navigation (j/k)
                        if (event.isChar('j') || event.isChar('J')) {
                            tableState.selectNext(filteredEmojis.size());
                            return EventResult.HANDLED;
                        }
                        if (event.isChar('k') || event.isChar('K')) {
                            tableState.selectPrevious();
                            return EventResult.HANDLED;
                        }
                        
                        // Handle first/last navigation
                        if (event.isChar('g')) {
                            tableState.selectFirst();
                            return EventResult.HANDLED;
                        }
                        if (event.isChar('G')) {
                            tableState.selectLast(filteredEmojis.size());
                            return EventResult.HANDLED;
                        }
                        
                        // Toggle codepoint format (Java vs Unicode)
                        if (event.isChar('w')) {
                            useJavaFormat = !useJavaFormat;
                            return EventResult.HANDLED;
                        }
                        
                        return EventResult.UNHANDLED;
                    })
                    .onMouseEvent(event -> {
                        // Handle mouse scroll wheel for table navigation
                        if (filteredEmojis.isEmpty()) {
                            return EventResult.UNHANDLED;
                        }

                        if (event.kind() == MouseEventKind.SCROLL_UP) {
                            // Scroll up - move selection up (similar to ListElement behavior)
                            for (int i = 0; i < 3; i++) {
                                tableState.selectPrevious();
                            }
                            return EventResult.HANDLED;
                        }

                        if (event.kind() == MouseEventKind.SCROLL_DOWN) {
                            // Scroll down - move selection down (similar to ListElement behavior)
                            for (int i = 0; i < 3; i++) {
                                tableState.selectNext(filteredEmojis.size());
                            }
                            return EventResult.HANDLED;
                        }

                        return EventResult.UNHANDLED;
                    });
                // Create UI with global key handler for ZWJ toggle
                return column(
                        column(
                            text("Filter:").bold().cyan(),
                            textInput(filterState).id("filter-text")
                                .placeholder("Type to filter emojis...")
                        ).length(2),
                        column(tableElement), // Main panel fills available space
                        row(
                            text("Tab - Switch focus"),
                            text("↑/↓/ j/k - Move up/down"),
                            text("g/G - Go to first/last"),
                            text("w - Format: " + (useJavaFormat ? "Java" : "Unicode")).cyan(),
                            text("Mouse wheel - Scroll"),
                            text("q - Quit")
                        ).length(1).spacing(1)  // Controls panel takes minimal space
                ).spacing(0);
            });
        }
    }
}

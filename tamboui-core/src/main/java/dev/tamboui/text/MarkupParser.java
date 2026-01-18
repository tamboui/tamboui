/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses BBCode-style markup text and converts it to styled {@link Text} objects.
 * <p>
 * The parser supports:
 * <ul>
 *   <li>Built-in modifier tags: {@code [bold]}, {@code [italic]}, {@code [underlined]},
 *       {@code [dim]}, {@code [reversed]}, {@code [crossed-out]}</li>
 *   <li>Built-in color tags: {@code [red]}, {@code [green]}, {@code [blue]}, {@code [yellow]},
 *       {@code [cyan]}, {@code [magenta]}, {@code [white]}, {@code [black]}, {@code [gray]}</li>
 *   <li>Hyperlinks: {@code [link=URL]text[/link]}</li>
 *   <li>Custom tags: resolved via a {@link StyleResolver}</li>
 *   <li>Escaped brackets: {@code [[} produces {@code [}, and {@code ]]} produces {@code ]}</li>
 *   <li>Nested tags: {@code [red][bold]text[/bold][/red]}</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * // Simple parsing with built-in styles
 * Text text = MarkupParser.parse("This is [red]red[/red] and [bold]bold[/bold].");
 *
 * // With custom tag resolver
 * Text text = MarkupParser.parse(
 *     "This is [keyword]styled[/keyword].",
 *     tagName -> {
 *         if ("keyword".equals(tagName)) {
 *             return Style.EMPTY.fg(Color.CYAN).bold();
 *         }
 *         return null;
 *     }
 * );
 * }</pre>
 * <p>
 * Unknown tags without a resolver are rendered as plain text (tags are preserved).
 * Unclosed tags apply their style to the remaining content.
 */
public final class MarkupParser {

    private static final Map<String, Style> BUILT_IN_STYLES;

    static {
        Map<String, Style> styles = new HashMap<>();

        // Modifier tags
        styles.put("bold", Style.EMPTY.bold());
        styles.put("b", Style.EMPTY.bold());
        styles.put("italic", Style.EMPTY.italic());
        styles.put("i", Style.EMPTY.italic());
        styles.put("underlined", Style.EMPTY.underlined());
        styles.put("u", Style.EMPTY.underlined());
        styles.put("dim", Style.EMPTY.dim());
        styles.put("reversed", Style.EMPTY.reversed());
        styles.put("crossed-out", Style.EMPTY.crossedOut());
        styles.put("strikethrough", Style.EMPTY.crossedOut());
        styles.put("s", Style.EMPTY.crossedOut());

        // Color tags
        styles.put("red", Style.EMPTY.fg(Color.RED));
        styles.put("green", Style.EMPTY.fg(Color.GREEN));
        styles.put("blue", Style.EMPTY.fg(Color.BLUE));
        styles.put("yellow", Style.EMPTY.fg(Color.YELLOW));
        styles.put("cyan", Style.EMPTY.fg(Color.CYAN));
        styles.put("magenta", Style.EMPTY.fg(Color.MAGENTA));
        styles.put("white", Style.EMPTY.fg(Color.WHITE));
        styles.put("black", Style.EMPTY.fg(Color.BLACK));
        styles.put("gray", Style.EMPTY.fg(Color.GRAY));
        styles.put("grey", Style.EMPTY.fg(Color.GRAY));

        BUILT_IN_STYLES = Collections.unmodifiableMap(styles);
    }

    private MarkupParser() {
        // Utility class
    }

    /**
     * Functional interface for resolving custom tag styles.
     */
    @FunctionalInterface
    public interface StyleResolver {
        /**
         * Resolves a style for the given tag name.
         *
         * @param tagName the tag name (without brackets)
         * @return the style for this tag, or null if not recognized
         */
        Style resolve(String tagName);
    }

    /**
     * Parses markup text using only built-in styles.
     * <p>
     * Custom tags are rendered as plain text (the tag markers are preserved).
     *
     * @param markup the markup text to parse
     * @return the parsed styled text
     */
    public static Text parse(String markup) {
        return parse(markup, null);
    }

    /**
     * Parses markup text with custom style resolution.
     *
     * @param markup the markup text to parse
     * @param resolver optional resolver for custom tags
     * @return the parsed styled text
     */
    public static Text parse(String markup, StyleResolver resolver) {
        if (markup == null || markup.isEmpty()) {
            return Text.empty();
        }

        Parser parser = new Parser(markup, resolver);
        return parser.parse();
    }

    /**
     * Internal parser implementation.
     */
    private static class Parser {
        private final String input;
        private final StyleResolver resolver;
        private int pos;
        private final Deque<StyleEntry> styleStack;
        private final List<Line> lines;
        private List<Span> currentLineSpans;
        private StringBuilder currentText;
        private Style currentStyle;

        Parser(String input, StyleResolver resolver) {
            this.input = input;
            this.resolver = resolver;
            this.pos = 0;
            this.styleStack = new ArrayDeque<>();
            this.lines = new ArrayList<>();
            this.currentLineSpans = new ArrayList<>();
            this.currentText = new StringBuilder();
            this.currentStyle = Style.EMPTY;
        }

        Text parse() {
            while (pos < input.length()) {
                char c = input.charAt(pos);

                if (c == '[') {
                    if (pos + 1 < input.length() && input.charAt(pos + 1) == '[') {
                        // Escaped opening bracket
                        currentText.append('[');
                        pos += 2;
                    } else {
                        // Potential tag
                        handleTag();
                    }
                } else if (c == ']') {
                    if (pos + 1 < input.length() && input.charAt(pos + 1) == ']') {
                        // Escaped closing bracket
                        currentText.append(']');
                        pos += 2;
                    } else {
                        // Unmatched closing bracket, treat as text
                        currentText.append(c);
                        pos++;
                    }
                } else if (c == '\n') {
                    // End of line
                    flushCurrentText();
                    lines.add(Line.from(new ArrayList<>(currentLineSpans)));
                    currentLineSpans.clear();
                    pos++;
                } else {
                    currentText.append(c);
                    pos++;
                }
            }

            // Flush remaining text
            flushCurrentText();
            if (!currentLineSpans.isEmpty()) {
                lines.add(Line.from(currentLineSpans));
            } else if (lines.isEmpty()) {
                // Empty input results in empty text
                return Text.empty();
            }

            return Text.from(lines);
        }

        private void handleTag() {
            int tagStart = pos;
            pos++; // Skip '['

            // Check if it's a closing tag
            boolean isClosing = false;
            if (pos < input.length() && input.charAt(pos) == '/') {
                isClosing = true;
                pos++;
            }

            // Read tag name (and optional attribute)
            StringBuilder tagNameBuilder = new StringBuilder();
            String attribute = null;

            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == ']') {
                    pos++; // Skip ']'
                    break;
                } else if (c == '=' && !isClosing) {
                    // Attribute value follows
                    pos++;
                    attribute = readAttributeValue();
                    if (pos < input.length() && input.charAt(pos) == ']') {
                        pos++;
                    }
                    break;
                } else if (c == '\n' || c == '[') {
                    // Malformed tag, treat as text
                    pos = tagStart;
                    currentText.append(input.charAt(pos));
                    pos++;
                    return;
                } else {
                    tagNameBuilder.append(c);
                    pos++;
                }
            }

            String tagName = tagNameBuilder.toString().toLowerCase().trim();

            if (tagName.isEmpty()) {
                // Empty tag, treat as text
                currentText.append(input.substring(tagStart, pos));
                return;
            }

            if (isClosing) {
                handleClosingTag(tagName);
            } else {
                handleOpeningTag(tagName, attribute, tagStart);
            }
        }

        private String readAttributeValue() {
            StringBuilder value = new StringBuilder();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if (c == ']') {
                    break;
                } else if (c == '\n') {
                    break;
                } else {
                    value.append(c);
                    pos++;
                }
            }
            return value.toString();
        }

        private void handleOpeningTag(String tagName, String attribute, int tagStart) {
            // Flush current text with current style
            flushCurrentText();

            // Check for link tag
            if ("link".equals(tagName) && attribute != null) {
                Style linkStyle = currentStyle.hyperlink(attribute);
                styleStack.push(new StyleEntry(tagName, linkStyle));
                currentStyle = linkStyle;
                return;
            }

            // Check built-in styles
            Style builtInStyle = BUILT_IN_STYLES.get(tagName);
            if (builtInStyle != null) {
                Style newStyle = currentStyle.patch(builtInStyle);
                styleStack.push(new StyleEntry(tagName, newStyle));
                currentStyle = newStyle;
                return;
            }

            // Check custom resolver
            if (resolver != null) {
                Style customStyle = resolver.resolve(tagName);
                if (customStyle != null) {
                    Style newStyle = currentStyle.patch(customStyle);
                    styleStack.push(new StyleEntry(tagName, newStyle));
                    currentStyle = newStyle;
                    return;
                }
            }

            // Unknown tag, render as text
            currentText.append(input.substring(tagStart, pos));
        }

        private void handleClosingTag(String tagName) {
            // Flush current text
            flushCurrentText();

            // Find matching opening tag
            StyleEntry found = null;
            Deque<StyleEntry> temp = new ArrayDeque<>();

            while (!styleStack.isEmpty()) {
                StyleEntry entry = styleStack.pop();
                if (entry.tagName.equals(tagName)) {
                    found = entry;
                    break;
                }
                temp.push(entry);
            }

            // Restore unmatched entries
            while (!temp.isEmpty()) {
                styleStack.push(temp.pop());
            }

            if (found != null) {
                // Pop entries up to and including the found one
                Deque<StyleEntry> toPop = new ArrayDeque<>();
                while (!styleStack.isEmpty()) {
                    StyleEntry entry = styleStack.peek();
                    if (entry.tagName.equals(tagName)) {
                        styleStack.pop();
                        break;
                    }
                    toPop.push(styleStack.pop());
                }

                // Recalculate current style from remaining stack
                currentStyle = Style.EMPTY;
                for (StyleEntry entry : styleStack) {
                    // Entries are in reverse order (top of stack = most recent)
                    // We need to apply from bottom to top
                }
                // Rebuild style from bottom of stack to top
                List<StyleEntry> entries = new ArrayList<>(styleStack);
                Collections.reverse(entries);
                currentStyle = Style.EMPTY;
                for (StyleEntry entry : entries) {
                    currentStyle = entry.style;
                }
                if (currentStyle == null) {
                    currentStyle = Style.EMPTY;
                }

                // Re-push inner entries
                while (!toPop.isEmpty()) {
                    StyleEntry entry = toPop.pop();
                    Style newStyle = currentStyle.patch(getBaseStyle(entry.tagName));
                    styleStack.push(new StyleEntry(entry.tagName, newStyle));
                    currentStyle = newStyle;
                }
            }
            // If no matching tag found, ignore the closing tag
        }

        private Style getBaseStyle(String tagName) {
            if ("link".equals(tagName)) {
                return Style.EMPTY; // Link style is handled specially
            }
            Style builtIn = BUILT_IN_STYLES.get(tagName);
            if (builtIn != null) {
                return builtIn;
            }
            if (resolver != null) {
                Style custom = resolver.resolve(tagName);
                if (custom != null) {
                    return custom;
                }
            }
            return Style.EMPTY;
        }

        private void flushCurrentText() {
            if (currentText.length() > 0) {
                currentLineSpans.add(new Span(currentText.toString(), currentStyle));
                currentText = new StringBuilder();
            }
        }

        private static class StyleEntry {
            final String tagName;
            final Style style;

            StyleEntry(String tagName, Style style) {
                this.tagName = tagName;
                this.style = style;
            }
        }
    }
}

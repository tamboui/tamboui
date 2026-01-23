/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import dev.tamboui.style.Color;
import dev.tamboui.style.ColorConverter;
import dev.tamboui.style.Style;
import dev.tamboui.style.Tags;

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
 *   <li>True color tokens: {@code [#RGB]}, {@code [#RRGGBB]}, {@code [rgb(r,g,b)]} (spaces allowed)</li>
 *   <li>Compound style specs: multiple tokens in a single tag, e.g.
 *       {@code [bold #ff5733 on rgb(10, 20, 30)]text[/]}. Foreground/background tokens are parsed
 *       using {@link dev.tamboui.style.ColorConverter} and modifiers are applied in addition to colors.</li>
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

                if (c == '\\') {
                    // Backslash escape sequences
                    if (pos + 1 < input.length()) {
                        char next = input.charAt(pos + 1);
                        if (next == '[' || next == ']' || next == '\\') {
                            currentText.append(next);
                            pos += 2;
                            continue;
                        }
                    }
                    // Lone backslash or unknown escape, treat as literal
                    currentText.append(c);
                    pos++;
                } else if (c == '[') {
                    if (pos + 1 < input.length() && input.charAt(pos + 1) == '[') {
                        // Escaped opening bracket (legacy)
                        currentText.append('[');
                        pos += 2;
                    } else {
                        // Potential tag
                        handleTag();
                    }
                } else if (c == ']') {
                    if (pos + 1 < input.length() && input.charAt(pos + 1) == ']') {
                        // Escaped closing bracket (legacy)
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

            if (tagName.isEmpty() && !isClosing) {
                // Empty opening tag, treat as text
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

            // Parse all tokens for CSS class targeting
            String[] tokens = tokenizeStyleSpec(tagName);
            String primaryTag = tokens[0];  // First token used for closing tag matching

            // Collect all tokens as CSS class tags (excluding "on" keyword and explicit colors)
            List<String> tagList = new ArrayList<>();
            for (String token : tokens) {
                if (!token.isEmpty() && 
                !"on".equals(token) && 
                !token.startsWith("#")&& 
                !token.startsWith("rgb(") &&
                !token.startsWith("indexed(")) {
                    tagList.add(token);
                }
            }

            // Create a Tags extension with all tokens
            Style tagStyle = Style.EMPTY.withExtension(Tags.class, Tags.of(tagList.toArray(new String[0])));

            // Check for link tag
            if ("link".equals(primaryTag) && attribute != null) {
                Style linkStyle = currentStyle.hyperlink(attribute).patch(tagStyle);
                styleStack.push(new StyleEntry(primaryTag, linkStyle));
                currentStyle = linkStyle;
                return;
            }

            // 1. Start with resolver style for primary tag (resolver has priority)
            Style baseStyle = Style.EMPTY;
            boolean resolverHandledPrimaryTag = false;
            if (resolver != null) {
                Style resolved = resolver.resolve(primaryTag);
                if (resolved != null) {
                    baseStyle = resolved;
                    resolverHandledPrimaryTag = true;
                }
            }

            // 2. Parse compound style spec and patch on top (inline overrides base)
            // Skip primary tag in parsing only if resolver already handled it
            String skipToken = resolverHandledPrimaryTag ? primaryTag : null;
            Style parsedStyle = parseStyleSpec(tokens, skipToken);
            Style combined = baseStyle.patch(parsedStyle);

            Style withTags = combined.patch(tagStyle);
            Style newStyle = currentStyle.patch(withTags);
            styleStack.push(new StyleEntry(primaryTag, newStyle));
            currentStyle = newStyle;
        }

        private void handleClosingTag(String tagName) {
            // Flush current text
            flushCurrentText();

            // Implicit close: pop most recent tag
            if (tagName.isEmpty()) {
                if (!styleStack.isEmpty()) {
                    styleStack.pop();
                    recalculateCurrentStyle();
                }
                return;
            }

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
                recalculateCurrentStyle();

                // Re-push inner entries
                while (!toPop.isEmpty()) {
                    StyleEntry entry = toPop.pop();
                    // Preserve the accumulated style the inner tag had before the outer close.
                    // Recomputing from base style would lose inherited styling from the
                    // (now closed) outer tag, which is undesirable for mismatched tags.
                    styleStack.push(entry);
                    currentStyle = entry.style;
                }
            }
            // If no matching tag found, ignore the closing tag
        }

        private void recalculateCurrentStyle() {
            List<StyleEntry> entries = new ArrayList<>(styleStack);
            Collections.reverse(entries);
            currentStyle = Style.EMPTY;
            for (StyleEntry entry : entries) {
                currentStyle = entry.style;
            }
        }

        private Style parseStyleSpec(String[] tokens, String skipToken) {
            Style result = Style.EMPTY;

            boolean expectBg = false;
            boolean skippedFirst = false;
            for (String token : tokens) {
                if (token.isEmpty()) {
                    continue;
                }

                // Skip the primary tag (first token) - it's handled separately by resolver
                if (!skippedFirst && skipToken != null && token.equals(skipToken)) {
                    skippedFirst = true;
                    continue;
                }

                if (expectBg) {
                    Color bg = parseColor(token);
                    if (bg != null) {
                        result = result.bg(bg);
                    }
                    expectBg = false;
                    continue;
                }

                if ("on".equals(token)) {
                    expectBg = true;
                    continue;
                }

                // Check modifiers
                Style modifier = BUILT_IN_STYLES.get(token);
                if (modifier != null) {
                    result = result.patch(modifier);
                    continue;
                }

                // Check foreground color
                Color fg = parseColor(token);
                if (fg != null) {
                    result = result.fg(fg);
                }
            }
            return result;
        }

        private static String[] tokenizeStyleSpec(String spec) {
            // Split on whitespace, but keep tokens like rgb(...) intact even if they contain spaces.
            if (spec == null || spec.isEmpty()) {
                return new String[0];
            }

            String lower = spec.toLowerCase();
            StringBuilder current = new StringBuilder();
            List<String> tokens = new ArrayList<>();

            int parenDepth = 0;
            for (int i = 0; i < lower.length(); i++) {
                char ch = lower.charAt(i);
                if (ch == '(') {
                    parenDepth++;
                    current.append(ch);
                    continue;
                }
                if (ch == ')') {
                    if (parenDepth > 0) {
                        parenDepth--;
                    }
                    current.append(ch);
                    continue;
                }

                if (Character.isWhitespace(ch) && parenDepth == 0) {
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                        current.setLength(0);
                    }
                    continue;
                }

                current.append(ch);
            }

            if (current.length() > 0) {
                tokens.add(current.toString());
            }

            return tokens.toArray(new String[0]);
        }

        private Color parseColor(String name) {
            return ColorConverter.INSTANCE.convert(name).orElse(null);
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

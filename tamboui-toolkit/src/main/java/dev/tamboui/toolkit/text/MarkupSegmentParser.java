/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.text;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.MarkupParser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses BBCode-style markup text and converts it to segments with tag metadata preserved.
 * <p>
 * Unlike {@link MarkupParser} which converts markup directly to styled {@link dev.tamboui.text.Text},
 * this parser preserves the tag names as metadata on each segment. This enables using tag names
 * as CSS classes for TFX effects targeting.
 * <p>
 * Example usage:
 * <pre>{@code
 * // Parse markup to segments
 * List<ParsedLine> lines = MarkupSegmentParser.parse("[gray]Hello [/gray][looping]World[/looping]");
 *
 * // Each segment contains:
 * // - text: "Hello "
 * // - tags: {"gray"}
 * // - style: Style.EMPTY.fg(Color.GRAY)
 * //
 * // - text: "World"
 * // - tags: {"looping"}
 * // - style: Style.EMPTY (unknown tag, no built-in style)
 * }</pre>
 *
 * @see MarkupParser for standard markup parsing without tag metadata
 */
public final class MarkupSegmentParser {

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

    private MarkupSegmentParser() {
        // Utility class
    }

    /**
     * A segment of text with its associated tags and computed style.
     */
    public static final class Segment {
        private final String text;
        private final Set<String> tags;
        private final Style style;

        /**
         * Creates a new segment.
         *
         * @param text the text content
         * @param tags the active tag names when this text was emitted
         * @param style the computed style for this segment
         */
        public Segment(String text, Set<String> tags, Style style) {
            this.text = text;
            this.tags = Collections.unmodifiableSet(new HashSet<>(tags));
            this.style = style;
        }

        /**
         * Returns the text content of this segment.
         *
         * @return the text content
         */
        public String text() {
            return text;
        }

        /**
         * Returns the set of tag names active when this segment was created.
         * These tag names can be used as CSS classes.
         *
         * @return unmodifiable set of tag names
         */
        public Set<String> tags() {
            return tags;
        }

        /**
         * Returns the computed style for this segment.
         *
         * @return the computed style
         */
        public Style style() {
            return style;
        }

        @Override
        public String toString() {
            return "Segment{text='" + text + "', tags=" + tags + ", style=" + style + "}";
        }
    }

    /**
     * A parsed line containing one or more segments.
     */
    public static final class ParsedLine {
        private final List<Segment> segments;

        /**
         * Creates a new parsed line.
         *
         * @param segments the segments in this line
         */
        public ParsedLine(List<Segment> segments) {
            this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
        }

        /**
         * Returns the segments in this line.
         *
         * @return unmodifiable list of segments
         */
        public List<Segment> segments() {
            return segments;
        }

        @Override
        public String toString() {
            return "ParsedLine{segments=" + segments + "}";
        }
    }

    /**
     * Parses markup text using only built-in styles.
     * <p>
     * Unknown tags are preserved as tag metadata but without a built-in style.
     *
     * @param markup the markup text to parse
     * @return list of parsed lines with segments
     */
    public static List<ParsedLine> parse(String markup) {
        return parse(markup, null);
    }

    /**
     * Parses markup text with custom style resolution.
     *
     * @param markup the markup text to parse
     * @param resolver optional resolver for custom tags
     * @return list of parsed lines with segments
     */
    public static List<ParsedLine> parse(String markup, MarkupParser.StyleResolver resolver) {
        if (markup == null || markup.isEmpty()) {
            return Collections.singletonList(new ParsedLine(Collections.emptyList()));
        }

        Parser parser = new Parser(markup, resolver);
        return parser.parse();
    }

    /**
     * Internal parser implementation.
     */
    private static class Parser {
        private final String input;
        private final MarkupParser.StyleResolver resolver;
        private int pos;
        private final Deque<TagEntry> tagStack;
        private final List<ParsedLine> lines;
        private List<Segment> currentLineSegments;
        private StringBuilder currentText;

        Parser(String input, MarkupParser.StyleResolver resolver) {
            this.input = input;
            this.resolver = resolver;
            this.pos = 0;
            this.tagStack = new ArrayDeque<>();
            this.lines = new ArrayList<>();
            this.currentLineSegments = new ArrayList<>();
            this.currentText = new StringBuilder();
        }

        List<ParsedLine> parse() {
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
                    lines.add(new ParsedLine(new ArrayList<>(currentLineSegments)));
                    currentLineSegments.clear();
                    pos++;
                } else {
                    currentText.append(c);
                    pos++;
                }
            }

            // Flush remaining text
            flushCurrentText();
            if (!currentLineSegments.isEmpty()) {
                lines.add(new ParsedLine(currentLineSegments));
            } else if (lines.isEmpty()) {
                // Empty input results in one empty line
                lines.add(new ParsedLine(Collections.emptyList()));
            }

            return lines;
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
            // Flush current text with current style and tags
            flushCurrentText();

            // Check for link tag
            if ("link".equals(tagName) && attribute != null) {
                Style linkStyle = Style.EMPTY.hyperlink(attribute);
                tagStack.push(new TagEntry(tagName, linkStyle));
                return;
            }

            // Check built-in styles
            Style builtInStyle = BUILT_IN_STYLES.get(tagName);
            if (builtInStyle != null) {
                tagStack.push(new TagEntry(tagName, builtInStyle));
                return;
            }

            // Check custom resolver
            if (resolver != null) {
                Style customStyle = resolver.resolve(tagName);
                if (customStyle != null) {
                    tagStack.push(new TagEntry(tagName, customStyle));
                    return;
                }
            }

            // Unknown tag - still push it so the tag name is tracked for CSS classes
            // but with no style contribution
            tagStack.push(new TagEntry(tagName, Style.EMPTY));
        }

        private void handleClosingTag(String tagName) {
            // Flush current text
            flushCurrentText();

            // Find matching opening tag
            TagEntry found = null;
            Deque<TagEntry> temp = new ArrayDeque<>();

            while (!tagStack.isEmpty()) {
                TagEntry entry = tagStack.pop();
                if (entry.tagName.equals(tagName)) {
                    found = entry;
                    break;
                }
                temp.push(entry);
            }

            // Restore unmatched entries
            while (!temp.isEmpty()) {
                tagStack.push(temp.pop());
            }

            if (found != null) {
                // Pop entries up to and including the found one
                Deque<TagEntry> toPop = new ArrayDeque<>();
                while (!tagStack.isEmpty()) {
                    TagEntry entry = tagStack.peek();
                    if (entry.tagName.equals(tagName)) {
                        tagStack.pop();
                        break;
                    }
                    toPop.push(tagStack.pop());
                }

                // Re-push inner entries
                while (!toPop.isEmpty()) {
                    tagStack.push(toPop.pop());
                }
            }
            // If no matching tag found, ignore the closing tag
        }

        private void flushCurrentText() {
            if (currentText.length() > 0) {
                // Collect all active tags and compute combined style
                Set<String> activeTags = new HashSet<>();
                Style combinedStyle = Style.EMPTY;

                // Iterate from bottom to top of stack (reverse order)
                List<TagEntry> entries = new ArrayList<>(tagStack);
                Collections.reverse(entries);
                for (TagEntry entry : entries) {
                    activeTags.add(entry.tagName);
                    combinedStyle = combinedStyle.patch(entry.style);
                }

                currentLineSegments.add(new Segment(currentText.toString(), activeTags, combinedStyle));
                currentText = new StringBuilder();
            }
        }

        private static class TagEntry {
            final String tagName;
            final Style style;

            TagEntry(String tagName, Style style) {
                this.tagName = tagName;
                this.style = style;
            }
        }
    }
}

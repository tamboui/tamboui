/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import dev.tamboui.layout.Alignment;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import static dev.tamboui.util.CollectionUtil.listCopyOf;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Multi-line styled text, composed of Lines.
 */
public final class Text {

    private final List<Line> lines;
    private final Alignment alignment;

    /**
     * Creates a new text with the given lines and alignment.
     *
     * @param lines     the lines of styled text
     * @param alignment the text alignment, or null for default
     */
    public Text(List<Line> lines, Alignment alignment) {
        this.lines = listCopyOf(lines);
        this.alignment = alignment;
    }

    /**
     * Creates an empty text with no lines.
     *
     * @return a new empty text
     */
    public static Text empty() {
        return new Text(listCopyOf(), null);
    }

    /**
     * Creates a text from a raw string, splitting on newlines.
     *
     * @param text the raw text content
     * @return a new text
     */
    public static Text raw(String text) {
        List<Line> linesList = new BufferedReader(new StringReader(text))
            .lines()
            .map(Line::from)
            .collect(Collectors.toList());
        return new Text(linesList, null);
    }

    /**
     * Creates a text from a string, splitting on newlines.
     *
     * @param text the text content
     * @return a new text
     */
    public static Text from(String text) {
        return raw(text);
    }

    /**
     * Creates a text containing a single line.
     *
     * @param line the line
     * @return a new text
     */
    public static Text from(Line line) {
        return new Text(listCopyOf(line), null);
    }

    /**
     * Creates a text from one or more lines.
     *
     * @param lines the lines
     * @return a new text
     */
    public static Text from(Line... lines) {
        return new Text(Arrays.asList(lines), null);
    }

    /**
     * Creates a text from a list of lines.
     *
     * @param lines the lines
     * @return a new text
     */
    public static Text from(List<Line> lines) {
        return new Text(lines, null);
    }

    /**
     * Creates a text containing a single span on one line.
     *
     * @param span the span
     * @return a new text
     */
    public static Text from(Span span) {
        return new Text(listCopyOf(Line.from(span)), null);
    }

    /**
     * Creates a styled text from a string, splitting on newlines.
     *
     * @param text  the text content
     * @param style the style to apply to all lines
     * @return a new styled text
     */
    public static Text styled(String text, Style style) {
        List<Line> linesList = new BufferedReader(new StringReader(text))
            .lines()
            .map(line -> Line.styled(line, style))
            .collect(Collectors.toList());
        return new Text(linesList, null);
    }

    /**
     * Returns the height (number of lines).
     *
     * @return the number of lines
     */
    public int height() {
        return lines.size();
    }

    /**
     * Returns the maximum width of any line.
     *
     * @return the maximum line width in terminal columns
     */
    public int width() {
        return lines.stream()
            .mapToInt(Line::width)
            .max()
            .orElse(0);
    }

    /**
     * Returns true if this text has no content.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return lines.isEmpty() || lines.stream().allMatch(Line::isEmpty);
    }

    /**
     * Returns a new text with the given alignment.
     *
     * @param alignment the alignment
     * @return a new text with the specified alignment
     */
    public Text alignment(Alignment alignment) {
        return new Text(lines, alignment);
    }

    /**
     * Returns a new text with left alignment.
     *
     * @return a new left-aligned text
     */
    public Text left() {
        return alignment(Alignment.LEFT);
    }

    /**
     * Returns a new text with center alignment.
     *
     * @return a new center-aligned text
     */
    public Text centered() {
        return alignment(Alignment.CENTER);
    }

    /**
     * Returns a new text with right alignment.
     *
     * @return a new right-aligned text
     */
    public Text right() {
        return alignment(Alignment.RIGHT);
    }

    /**
     * Applies a style patch to all lines.
     *
     * @param style the style to apply
     * @return a new text with the patched style
     */
    public Text patchStyle(Style style) {
        List<Line> newLines = lines.stream()
            .map(line -> line.patchStyle(style))
            .collect(Collectors.toList());
        return new Text(newLines, alignment);
    }

    /**
     * Returns a new text with the given foreground color.
     *
     * @param color the foreground color
     * @return a new text with the foreground color applied
     */
    public Text fg(Color color) {
        return patchStyle(Style.EMPTY.fg(color));
    }

    /**
     * Returns a new text with the given background color.
     *
     * @param color the background color
     * @return a new text with the background color applied
     */
    public Text bg(Color color) {
        return patchStyle(Style.EMPTY.bg(color));
    }

    /**
     * Returns a new text with bold style.
     *
     * @return a new bold text
     */
    public Text bold() {
        return patchStyle(Style.EMPTY.bold());
    }

    /**
     * Returns a new text with italic style.
     *
     * @return a new italic text
     */
    public Text italic() {
        return patchStyle(Style.EMPTY.italic());
    }

    /**
     * Returns a new text with underlined style.
     *
     * @return a new underlined text
     */
    public Text underlined() {
        return patchStyle(Style.EMPTY.underlined());
    }

    /**
     * Returns a new text with a hyperlink.
     *
     * @param url the hyperlink URL
     * @return a new text with the hyperlink applied
     */
    public Text hyperlink(String url) {
        return patchStyle(Style.EMPTY.hyperlink(url));
    }

    /**
     * Returns a new text with a hyperlink and explicit ID.
     *
     * @param url the hyperlink URL
     * @param id  the hyperlink ID
     * @return a new text with the hyperlink applied
     */
    public Text hyperlink(String url, String id) {
        return patchStyle(Style.EMPTY.hyperlink(url, id));
    }

    /**
     * Returns the raw text content without styling.
     *
     * @return the plain text content
     */
    public String rawContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i).rawContent());
        }
        return sb.toString();
    }

    /**
     * Appends another text's lines to this text.
     *
     * @param other the text to append
     * @return a new text with the appended lines
     */
    public Text append(Text other) {
        List<Line> newLines = new ArrayList<>(lines);
        newLines.addAll(other.lines);
        return new Text(newLines, alignment);
    }

    /**
     * Appends a line to this text.
     *
     * @param line the line to append
     * @return a new text with the appended line
     */
    public Text append(Line line) {
        List<Line> newLines = new ArrayList<>(lines);
        newLines.add(line);
        return new Text(newLines, alignment);
    }

    /**
     * Extends the last line with the given span, or adds a new line if empty.
     *
     * @param span the span to push
     * @return a new text with the span added to the last line
     */
    public Text push(Span span) {
        if (lines.isEmpty()) {
            return append(Line.from(span));
        }
        List<Line> newLines = new ArrayList<>(lines);
        Line lastLine = newLines.remove(newLines.size() - 1);
        newLines.add(lastLine.append(span));
        return new Text(newLines, alignment);
    }

    /**
     * Returns the lines of this text.
     *
     * @return the lines
     */
    public List<Line> lines() {
        return lines;
    }

    /**
     * Returns the alignment of this text, if set.
     *
     * @return the alignment, or empty if not set
     */
    public Optional<Alignment> alignment() {
        return Optional.ofNullable(alignment);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Text)) {
            return false;
        }
        Text text = (Text) o;
        return lines.equals(text.lines) && Objects.equals(alignment, text.alignment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lines, alignment);
    }

    @Override
    public String toString() {
        return String.format("Text[lines=%s, alignment=%s]", lines, alignment);
    }
}

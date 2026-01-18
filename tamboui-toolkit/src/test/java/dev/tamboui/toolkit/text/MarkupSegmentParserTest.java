/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.text;

import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.toolkit.text.MarkupSegmentParser.ParsedLine;
import dev.tamboui.toolkit.text.MarkupSegmentParser.Segment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MarkupSegmentParser.
 */
class MarkupSegmentParserTest {

    @Test
    @DisplayName("parse plain text returns single segment with no tags")
    void parsePlainText() {
        List<ParsedLine> lines = MarkupSegmentParser.parse("Hello World");

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).segments()).hasSize(1);

        Segment segment = lines.get(0).segments().get(0);
        assertThat(segment.text()).isEqualTo("Hello World");
        assertThat(segment.tags()).isEmpty();
        assertThat(segment.style()).isEqualTo(Style.EMPTY);
    }

    @Test
    @DisplayName("parse bold markup preserves tag name")
    void parseBoldMarkup() {
        List<ParsedLine> lines = MarkupSegmentParser.parse("[bold]Hello[/bold]");

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).segments()).hasSize(1);

        Segment segment = lines.get(0).segments().get(0);
        assertThat(segment.text()).isEqualTo("Hello");
        assertThat(segment.tags()).containsExactly("bold");
        assertThat(segment.style().addModifiers()).contains(Modifier.BOLD);
    }

    @Test
    @DisplayName("parse color markup preserves tag name")
    void parseColorMarkup() {
        List<ParsedLine> lines = MarkupSegmentParser.parse("[red]Error[/red]");

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).segments()).hasSize(1);

        Segment segment = lines.get(0).segments().get(0);
        assertThat(segment.text()).isEqualTo("Error");
        assertThat(segment.tags()).containsExactly("red");
        assertThat(segment.style().fg()).contains(Color.RED);
    }

    @Test
    @DisplayName("parse nested tags preserves all tag names")
    void parseNestedTags() {
        List<ParsedLine> lines = MarkupSegmentParser.parse("[red][bold]Important[/bold][/red]");

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).segments()).hasSize(1);

        Segment segment = lines.get(0).segments().get(0);
        assertThat(segment.text()).isEqualTo("Important");
        assertThat(segment.tags()).containsExactlyInAnyOrder("red", "bold");
        assertThat(segment.style().fg()).contains(Color.RED);
        assertThat(segment.style().addModifiers()).contains(Modifier.BOLD);
    }

    @Test
    @DisplayName("parse multiple segments on same line")
    void parseMultipleSegments() {
        List<ParsedLine> lines = MarkupSegmentParser.parse("Normal [red]Red[/red] and [blue]Blue[/blue]");

        assertThat(lines).hasSize(1);
        List<Segment> segments = lines.get(0).segments();
        assertThat(segments).hasSize(4);

        assertThat(segments.get(0).text()).isEqualTo("Normal ");
        assertThat(segments.get(0).tags()).isEmpty();

        assertThat(segments.get(1).text()).isEqualTo("Red");
        assertThat(segments.get(1).tags()).containsExactly("red");

        assertThat(segments.get(2).text()).isEqualTo(" and ");
        assertThat(segments.get(2).tags()).isEmpty();

        assertThat(segments.get(3).text()).isEqualTo("Blue");
        assertThat(segments.get(3).tags()).containsExactly("blue");
    }

    @Test
    @DisplayName("parse multi-line markup")
    void parseMultiLineMarkup() {
        List<ParsedLine> lines = MarkupSegmentParser.parse("Line 1\n[bold]Line 2[/bold]\n[red]Line 3[/red]");

        assertThat(lines).hasSize(3);

        assertThat(lines.get(0).segments().get(0).text()).isEqualTo("Line 1");
        assertThat(lines.get(0).segments().get(0).tags()).isEmpty();

        assertThat(lines.get(1).segments().get(0).text()).isEqualTo("Line 2");
        assertThat(lines.get(1).segments().get(0).tags()).containsExactly("bold");

        assertThat(lines.get(2).segments().get(0).text()).isEqualTo("Line 3");
        assertThat(lines.get(2).segments().get(0).tags()).containsExactly("red");
    }

    @Test
    @DisplayName("parse unknown tag preserves tag name for CSS class")
    void parseUnknownTag() {
        List<ParsedLine> lines = MarkupSegmentParser.parse("[looping]animated[/looping]");

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).segments()).hasSize(1);

        Segment segment = lines.get(0).segments().get(0);
        assertThat(segment.text()).isEqualTo("animated");
        // Unknown tag is still tracked as a tag name (for CSS class)
        assertThat(segment.tags()).containsExactly("looping");
        // But no built-in style is applied
        assertThat(segment.style()).isEqualTo(Style.EMPTY);
    }

    @Test
    @DisplayName("parse with custom resolver applies style")
    void parseWithCustomResolver() {
        List<ParsedLine> lines = MarkupSegmentParser.parse(
            "[custom]text[/custom]",
            tagName -> "custom".equals(tagName) ? Style.EMPTY.fg(Color.CYAN) : null
        );

        assertThat(lines).hasSize(1);
        Segment segment = lines.get(0).segments().get(0);
        assertThat(segment.text()).isEqualTo("text");
        assertThat(segment.tags()).containsExactly("custom");
        assertThat(segment.style().fg()).contains(Color.CYAN);
    }

    @Test
    @DisplayName("parse escaped brackets")
    void parseEscapedBrackets() {
        List<ParsedLine> lines = MarkupSegmentParser.parse("Use [[tag]] for brackets");

        assertThat(lines).hasSize(1);
        Segment segment = lines.get(0).segments().get(0);
        assertThat(segment.text()).isEqualTo("Use [tag] for brackets");
    }

    @Test
    @DisplayName("parse empty input returns empty line")
    void parseEmptyInput() {
        List<ParsedLine> lines = MarkupSegmentParser.parse("");

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).segments()).isEmpty();
    }

    @Test
    @DisplayName("parse null input returns empty line")
    void parseNullInput() {
        List<ParsedLine> lines = MarkupSegmentParser.parse(null);

        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).segments()).isEmpty();
    }

    @Test
    @DisplayName("Segment toString includes all fields")
    void segmentToString() {
        java.util.Set<String> tags = new java.util.HashSet<>();
        tags.add("bold");
        Segment segment = new Segment("Hello", tags, Style.EMPTY.bold());

        String str = segment.toString();
        assertThat(str).contains("Hello");
        assertThat(str).contains("bold");
    }

    @Test
    @DisplayName("ParsedLine toString includes segments")
    void parsedLineToString() {
        java.util.Set<String> tags = new java.util.HashSet<>();
        tags.add("bold");
        List<Segment> segments = new java.util.ArrayList<>();
        segments.add(new Segment("Hello", tags, Style.EMPTY.bold()));
        ParsedLine line = new ParsedLine(segments);

        String str = line.toString();
        assertThat(str).contains("Hello");
    }
}

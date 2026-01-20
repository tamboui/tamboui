/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.style.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

class MarkupParserTest {

    @Test
    @DisplayName("parse plain text without markup")
    void parsePlainText() {
        Text text = MarkupParser.parse("Hello World");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans()).hasSize(1);
        assertThat(text.lines().get(0).spans().get(0).content()).isEqualTo("Hello World");
        assertThat(text.lines().get(0).spans().get(0).style()).isEqualTo(Style.EMPTY);
    }

    @Test
    @DisplayName("parse text with bold tag")
    void parseBoldTag() {
        Text text = MarkupParser.parse("This is [bold]bold[/bold] text");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans()).hasSize(3);
        assertThat(text.lines().get(0).spans().get(0).content()).isEqualTo("This is ");
        assertThat(text.lines().get(0).spans().get(1).content()).isEqualTo("bold");
        assertThat(text.lines().get(0).spans().get(1).style().addModifiers()).contains(dev.tamboui.style.Modifier.BOLD);
        assertThat(text.lines().get(0).spans().get(2).content()).isEqualTo(" text");
    }

    @Test
    @DisplayName("parse text with shorthand bold tag")
    void parseShortBoldTag() {
        Text text = MarkupParser.parse("[b]bold[/b]");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans().get(0).style().addModifiers()).contains(dev.tamboui.style.Modifier.BOLD);
    }

    @Test
    @DisplayName("parse text with italic tag")
    void parseItalicTag() {
        Text text = MarkupParser.parse("[italic]italic[/italic]");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans().get(0).style().addModifiers()).contains(dev.tamboui.style.Modifier.ITALIC);
    }

    @Test
    @DisplayName("parse text with underlined tag")
    void parseUnderlinedTag() {
        Text text = MarkupParser.parse("[underlined]underlined[/underlined]");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans().get(0).style().addModifiers()).contains(dev.tamboui.style.Modifier.UNDERLINED);
    }

    @Test
    @DisplayName("parse text with color tag")
    void parseColorTag() {
        Text text = MarkupParser.parse("[red]red text[/red]");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans().get(0).style().fg()).contains(Color.RED);
    }

    @Test
    @DisplayName("parse text with multiple color tags")
    void parseMultipleColorTags() {
        Text text = MarkupParser.parse("[red]red[/red] and [blue]blue[/blue]");

        assertThat(text.lines()).hasSize(1);
        // Spans: "red" (red), " and " (default), "blue" (blue)
        assertThat(text.lines().get(0).spans()).hasSize(3);
        assertThat(text.lines().get(0).spans().get(0).content()).isEqualTo("red");
        assertThat(text.lines().get(0).spans().get(0).style().fg()).contains(Color.RED);
        assertThat(text.lines().get(0).spans().get(1).content()).isEqualTo(" and ");
        assertThat(text.lines().get(0).spans().get(2).content()).isEqualTo("blue");
        assertThat(text.lines().get(0).spans().get(2).style().fg()).contains(Color.BLUE);
    }

    @Test
    @DisplayName("parse nested tags")
    void parseNestedTags() {
        Text text = MarkupParser.parse("[red][bold]red bold[/bold][/red]");

        assertThat(text.lines()).hasSize(1);
        Span span = text.lines().get(0).spans().get(0);
        assertThat(span.style().fg()).contains(Color.RED);
        assertThat(span.style().addModifiers()).contains(dev.tamboui.style.Modifier.BOLD);
    }

    @Test
    @DisplayName("parse deeply nested tags")
    void parseDeeplyNestedTags() {
        Text text = MarkupParser.parse("[red][bold][italic]styled[/italic][/bold][/red]");

        assertThat(text.lines()).hasSize(1);
        Span span = text.lines().get(0).spans().get(0);
        assertThat(span.style().fg()).contains(Color.RED);
        assertThat(span.style().addModifiers()).contains(dev.tamboui.style.Modifier.BOLD);
        assertThat(span.style().addModifiers()).contains(dev.tamboui.style.Modifier.ITALIC);
    }

    @Test
    @DisplayName("parse escaped opening bracket")
    void parseEscapedOpeningBracket() {
        Text text = MarkupParser.parse("Use [[tag]] for literal brackets");

        assertThat(text.lines()).hasSize(1);
        // [[ produces [ and ]] produces ]
        assertThat(text.lines().get(0).rawContent()).isEqualTo("Use [tag] for literal brackets");
    }

    @Test
    @DisplayName("parse escaped closing bracket")
    void parseEscapedClosingBracket() {
        Text text = MarkupParser.parse("Content with ]] closing");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).rawContent()).isEqualTo("Content with ] closing");
    }

    @Test
    @DisplayName("parse multi-line text")
    void parseMultiLineText() {
        Text text = MarkupParser.parse("Line 1\n[bold]Line 2[/bold]\nLine 3");

        assertThat(text.lines()).hasSize(3);
        assertThat(text.lines().get(0).rawContent()).isEqualTo("Line 1");
        assertThat(text.lines().get(1).rawContent()).isEqualTo("Line 2");
        assertThat(text.lines().get(1).spans().get(0).style().addModifiers()).contains(dev.tamboui.style.Modifier.BOLD);
        assertThat(text.lines().get(2).rawContent()).isEqualTo("Line 3");
    }

    @Test
    @DisplayName("parse link tag with URL attribute")
    void parseLinkTag() {
        Text text = MarkupParser.parse("[link=https://example.com]click here[/link]");

        assertThat(text.lines()).hasSize(1);
        Span span = text.lines().get(0).spans().get(0);
        assertThat(span.content()).isEqualTo("click here");
        assertThat(span.style().hyperlink()).isPresent();
        assertThat(span.style().hyperlink().get().url()).isEqualTo("https://example.com");
    }

    @Test
    @DisplayName("parse unknown tag tracks it for CSS class targeting")
    void parseUnknownTagTracksForCssClass() {
        Text text = MarkupParser.parse("[unknown]text[/unknown]");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans()).hasSize(1);
        Span span = text.lines().get(0).spans().get(0);
        // Unknown tags are parsed and content is extracted (no visible tag brackets)
        assertThat(span.content()).isEqualTo("text");
        // Unknown tags are tracked via Tags extension for CSS class targeting
        Tags tags = span.style().extension(Tags.class, Tags.empty());
        assertThat(tags.contains("unknown")).isTrue();
    }

    @Test
    @DisplayName("parse with custom style resolver")
    void parseWithCustomResolver() {
        MarkupParser.StyleResolver resolver = tagName -> {
            if ("keyword".equals(tagName)) {
                return Style.EMPTY.fg(Color.CYAN).bold();
            }
            return null;
        };

        Text text = MarkupParser.parse("[keyword]function[/keyword]", resolver);

        assertThat(text.lines()).hasSize(1);
        Span span = text.lines().get(0).spans().get(0);
        assertThat(span.content()).isEqualTo("function");
        assertThat(span.style().fg()).contains(Color.CYAN);
        assertThat(span.style().addModifiers()).contains(dev.tamboui.style.Modifier.BOLD);
    }

    @Test
    @DisplayName("parse empty string returns empty text")
    void parseEmptyString() {
        Text text = MarkupParser.parse("");

        assertThat(text.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("parse null string returns empty text")
    void parseNullString() {
        Text text = MarkupParser.parse(null);

        assertThat(text.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("parse case insensitive tags")
    void parseCaseInsensitiveTags() {
        Text text = MarkupParser.parse("[BOLD]text[/BOLD]");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans().get(0).style().addModifiers()).contains(dev.tamboui.style.Modifier.BOLD);
    }

    @Test
    @DisplayName("parse all supported color tags")
    void parseAllColorTags() {
        String[] colors = {"red", "green", "blue", "yellow", "cyan", "magenta", "white", "black", "gray"};
        Color[] expected = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.WHITE, Color.BLACK, Color.GRAY};

        for (int i = 0; i < colors.length; i++) {
            Text text = MarkupParser.parse("[" + colors[i] + "]text[/" + colors[i] + "]");
            assertThat(text.lines().get(0).spans().get(0).style().fg()).contains(expected[i]);
        }
    }

    @Test
    @DisplayName("parse grey as alias for gray")
    void parseGreyAlias() {
        Text text = MarkupParser.parse("[grey]text[/grey]");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans().get(0).style().fg()).contains(Color.GRAY);
    }

    @Test
    @DisplayName("parse crossed-out tag")
    void parseCrossedOutTag() {
        Text text = MarkupParser.parse("[crossed-out]struck[/crossed-out]");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans().get(0).style().addModifiers()).contains(dev.tamboui.style.Modifier.CROSSED_OUT);
    }

    @Test
    @DisplayName("parse strikethrough alias")
    void parseStrikethroughAlias() {
        Text text = MarkupParser.parse("[strikethrough]struck[/strikethrough]");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans().get(0).style().addModifiers()).contains(dev.tamboui.style.Modifier.CROSSED_OUT);
    }

    @Test
    @DisplayName("parse dim tag")
    void parseDimTag() {
        Text text = MarkupParser.parse("[dim]dimmed[/dim]");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans().get(0).style().addModifiers()).contains(dev.tamboui.style.Modifier.DIM);
    }

    @Test
    @DisplayName("parse reversed tag")
    void parseReversedTag() {
        Text text = MarkupParser.parse("[reversed]reversed[/reversed]");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans().get(0).style().addModifiers()).contains(dev.tamboui.style.Modifier.REVERSED);
    }

    @Test
    @DisplayName("unclosed tag applies style to remaining text")
    void unclosedTagAppliesToRemaining() {
        Text text = MarkupParser.parse("Normal [bold]bold till end");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans()).hasSize(2);
        assertThat(text.lines().get(0).spans().get(0).content()).isEqualTo("Normal ");
        assertThat(text.lines().get(0).spans().get(1).content()).isEqualTo("bold till end");
        assertThat(text.lines().get(0).spans().get(1).style().addModifiers()).contains(dev.tamboui.style.Modifier.BOLD);
    }
}

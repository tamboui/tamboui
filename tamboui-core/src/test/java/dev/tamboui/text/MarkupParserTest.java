/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.style.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

class MarkupParserTest {

    @Test
    @DisplayName("parse hex color token in markup tag")
    void parseHexColorToken() {
        Text text = MarkupParser.parse("[#ff5733]X[/]");

        assertThat(text.lines()).hasSize(1);
        Span span = text.lines().get(0).spans().get(0);
        assertThat(span.content()).isEqualTo("X");
        assertThat(span.style().fg()).contains(Color.rgb(0xff, 0x57, 0x33));

        Tags tags = span.style().extension(Tags.class, Tags.empty());
        assertThat(tags.contains("#ff5733")).isFalse();
    }

    @Test
    @DisplayName("parse rgb(r,g,b) color token in markup tag")
    void parseRgbColorToken() {
        Text text = MarkupParser.parse("[rgb(1,2,3)]X[/]");

        assertThat(text.lines()).hasSize(1);
        Span span = text.lines().get(0).spans().get(0);
        assertThat(span.content()).isEqualTo("X");
        assertThat(span.style().fg()).contains(Color.rgb(1, 2, 3));

        Tags tags = span.style().extension(Tags.class, Tags.empty());
        assertThat(tags.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("parse rgb(r, g, b) color token with spaces in markup tag")
    void parseRgbColorTokenWithSpaces() {
        Text text = MarkupParser.parse("[rgb(10, 20, 30)]X[/]");

        assertThat(text.lines()).hasSize(1);
        Span span = text.lines().get(0).spans().get(0);
        assertThat(span.content()).isEqualTo("X");
        assertThat(span.style().fg()).contains(Color.rgb(10, 20, 30));

        Tags tags = span.style().extension(Tags.class, Tags.empty());
        assertThat(tags.isEmpty()).isTrue();
    }

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
    @DisplayName("parse backslash escaped opening bracket")
    void parseBackslashEscapedOpeningBracket() {
        Text text = MarkupParser.parse("Use \\[tag] for literal bracket");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).rawContent()).isEqualTo("Use [tag] for literal bracket");
    }

    @Test
    @DisplayName("parse backslash escaped closing bracket")
    void parseBackslashEscapedClosingBracket() {
        Text text = MarkupParser.parse("Content with \\] closing");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).rawContent()).isEqualTo("Content with ] closing");
    }

    @Test
    @DisplayName("parse backslash escaped backslash")
    void parseBackslashEscapedBackslash() {
        Text text = MarkupParser.parse("Path: C:\\\\Users\\\\name");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).rawContent()).isEqualTo("Path: C:\\Users\\name");
    }

    @Test
    @DisplayName("lone backslash is preserved")
    void loneBackslashPreserved() {
        Text text = MarkupParser.parse("Lone \\ backslash");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).rawContent()).isEqualTo("Lone \\ backslash");
    }

    @Test
    @DisplayName("backslash before other characters is preserved")
    void backslashBeforeOtherCharsPreserved() {
        Text text = MarkupParser.parse("Newline: \\n Tab: \\t");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).rawContent()).isEqualTo("Newline: \\n Tab: \\t");
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

    @Test
    @DisplayName("implicit close pops most recent tag")
    void implicitClose() {
        Text text = MarkupParser.parse("[bold]Hello[/] World");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans()).hasSize(2);
        assertThat(text.lines().get(0).spans().get(0).content()).isEqualTo("Hello");
        assertThat(text.lines().get(0).spans().get(0).style().addModifiers())
            .contains(dev.tamboui.style.Modifier.BOLD);
        assertThat(text.lines().get(0).spans().get(1).content()).isEqualTo(" World");
        assertThat(text.lines().get(0).spans().get(1).style().addModifiers())
            .doesNotContain(dev.tamboui.style.Modifier.BOLD);
    }

    @Test
    @DisplayName("compound style with multiple modifiers")
    void compoundStyleModifiers() {
        Text text = MarkupParser.parse("[bold italic]styled[/]");

        assertThat(text.lines()).hasSize(1);
        Style style = text.lines().get(0).spans().get(0).style();
        assertThat(style.addModifiers()).contains(dev.tamboui.style.Modifier.BOLD, dev.tamboui.style.Modifier.ITALIC);
    }

    @Test
    @DisplayName("compound style with color and modifier")
    void compoundStyleColorAndModifier() {
        Text text = MarkupParser.parse("[bold red]styled[/]");

        assertThat(text.lines()).hasSize(1);
        Style style = text.lines().get(0).spans().get(0).style();
        assertThat(style.addModifiers()).contains(dev.tamboui.style.Modifier.BOLD);
        assertThat(style.fg()).contains(Color.RED);
    }

    @Test
    @DisplayName("background color with on keyword")
    void backgroundColorOnKeyword() {
        Text text = MarkupParser.parse("[white on blue]highlighted[/]");

        assertThat(text.lines()).hasSize(1);
        Style style = text.lines().get(0).spans().get(0).style();
        assertThat(style.fg()).contains(Color.WHITE);
        assertThat(style.bg()).contains(Color.BLUE);
    }

    @Test
    @DisplayName("compound style preserves all Tags for CSS targeting")
    void compoundStylePreservesTags() {
        Text text = MarkupParser.parse("[bold red]styled[/]");

        Tags tags = text.lines().get(0).spans().get(0).style()
            .extension(Tags.class, Tags.empty());
        // All tokens become CSS classes
        assertThat(tags.contains("bold")).isTrue();
        assertThat(tags.contains("red")).isTrue();
    }

    @Test
    @DisplayName("compound style with multiple tokens all become tags")
    void compoundStyleAllTokensBecomeTags() {
        Text text = MarkupParser.parse("[bold underlined yellow]Warning![/]");

        Tags tags = text.lines().get(0).spans().get(0).style()
            .extension(Tags.class, Tags.empty());
        assertThat(tags.contains("bold")).isTrue();
        assertThat(tags.contains("underlined")).isTrue();
        assertThat(tags.contains("yellow")).isTrue();
    }

    @Test
    @DisplayName("compound style with background excludes 'on' keyword from tags")
    void compoundStyleExcludesOnKeyword() {
        Text text = MarkupParser.parse("[white on blue]highlighted[/]");

        Tags tags = text.lines().get(0).spans().get(0).style()
            .extension(Tags.class, Tags.empty());
        assertThat(tags.contains("white")).isTrue();
        assertThat(tags.contains("blue")).isTrue();
        assertThat(tags.contains("on")).isFalse();
    }

    @Test
    @DisplayName("nested implicit closes")
    void nestedImplicitCloses() {
        Text text = MarkupParser.parse("[red][bold]RB[/][/] plain");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans()).hasSize(2);
        Style first = text.lines().get(0).spans().get(0).style();
        assertThat(first.fg()).contains(Color.RED);
        assertThat(first.addModifiers()).contains(dev.tamboui.style.Modifier.BOLD);
        assertThat(text.lines().get(0).spans().get(1).content()).isEqualTo(" plain");
    }

    @Test
    @DisplayName("closing outer tag preserves inner accumulated style, aka. dont close unless explicitly closed")
    void closingOuterTagPreservesInnerAccumulatedStyle() {
        Text text = MarkupParser.parse("[red]text[bold]bold-red[/red]more");

        assertThat(text.lines()).hasSize(1);
        assertThat(text.lines().get(0).spans()).hasSize(3);

        Span red = text.lines().get(0).spans().get(0);
        assertThat(red.content()).isEqualTo("text");
        assertThat(red.style().fg()).contains(Color.RED);

        Span boldRed = text.lines().get(0).spans().get(1);
        assertThat(boldRed.content()).isEqualTo("bold-red");
        assertThat(boldRed.style().fg()).contains(Color.RED);
        assertThat(boldRed.style().addModifiers()).contains(Modifier.BOLD);

        Span more = text.lines().get(0).spans().get(2);
        assertThat(more.content()).isEqualTo("more");
        assertThat(more.style().fg()).contains(Color.RED);
        assertThat(more.style().addModifiers()).contains(Modifier.BOLD);
    }

    // ===== Edge cases for compound styles with custom tags =====

    @Test
    @DisplayName("unknown tag as background target is ignored")
    void unknownTagAsBackgroundIsIgnored() {
        // [red on foo] - "foo" is not a known color, so no background is set
        Text text = MarkupParser.parse("[red on foo]text[/]");

        Style style = text.lines().get(0).spans().get(0).style();
        assertThat(style.fg()).contains(Color.RED);
        assertThat(style.bg()).isEmpty();  // "foo" is not a color
        // All tokens (except "on") become CSS classes
        Tags tags = style.extension(Tags.class, Tags.empty());
        assertThat(tags.contains("red")).isTrue();
        assertThat(tags.contains("foo")).isTrue();
        assertThat(tags.contains("on")).isFalse();
    }

    @Test
    @DisplayName("custom tag with background color - all tokens become CSS classes")
    void customTagWithBackgroundColor() {
        // [foo on red] - both "foo" and "red" become CSS classes, red becomes background
        Text text = MarkupParser.parse("[foo on red]text[/]");

        Style style = text.lines().get(0).spans().get(0).style();
        assertThat(style.fg()).isEmpty();  // "foo" is not a color
        assertThat(style.bg()).contains(Color.RED);
        // All tokens (except "on") become CSS classes
        Tags tags = style.extension(Tags.class, Tags.empty());
        assertThat(tags.contains("foo")).isTrue();
        assertThat(tags.contains("red")).isTrue();
        assertThat(tags.contains("on")).isFalse();
    }

    @Test
    @DisplayName("custom resolver is merged with compound style - inline overrides base")
    void customResolverMergedWithCompoundStyle() {
        // Resolver defines "error" as cyan+bold
        // Compound style adds red background
        // Result: resolver's style + compound's background (merged)
        MarkupParser.StyleResolver resolver = tagName -> {
            if ("error".equals(tagName)) {
                return Style.EMPTY.fg(Color.CYAN).bold();
            }
            return null;
        };

        Text text = MarkupParser.parse("[error on red]text[/]", resolver);

        Style style = text.lines().get(0).spans().get(0).style();
        // Resolver's cyan foreground IS applied
        assertThat(style.fg()).contains(Color.CYAN);
        // Resolver's bold IS applied
        assertThat(style.addModifiers()).contains(dev.tamboui.style.Modifier.BOLD);
        // Compound's red background IS applied on top
        assertThat(style.bg()).contains(Color.RED);
    }

    @Test
    @DisplayName("custom resolver works for pure custom tag")
    void customResolverWorksForPureCustomTag() {
        // When there's no compound parsing, resolver is consulted
        MarkupParser.StyleResolver resolver = tagName -> {
            if ("error".equals(tagName)) {
                return Style.EMPTY.fg(Color.CYAN).bold();
            }
            return null;
        };

        Text text = MarkupParser.parse("[error]text[/]", resolver);

        Style style = text.lines().get(0).spans().get(0).style();
        assertThat(style.fg()).contains(Color.CYAN);
        assertThat(style.addModifiers()).contains(dev.tamboui.style.Modifier.BOLD);
    }

    @Test
    @DisplayName("resolver can override built-in color")
    void resolverCanOverrideBuiltInColor() {
        // Resolver has priority: can redefine what "red" means
        MarkupParser.StyleResolver resolver = tagName -> {
            if ("red".equals(tagName)) {
                return Style.EMPTY.fg(Color.BLUE).italic();  // Redefine "red" as blue+italic
            }
            return null;
        };

        Text text = MarkupParser.parse("[red]text[/]", resolver);

        Style style = text.lines().get(0).spans().get(0).style();
        // Resolver wins: "red" now means blue foreground
        assertThat(style.fg()).contains(Color.BLUE);
        // Resolver's italic IS applied
        assertThat(style.addModifiers()).contains(dev.tamboui.style.Modifier.ITALIC);
    }

    @Test
    @DisplayName("compound style can override resolver's color")
    void compoundStyleOverridesResolverColor() {
        // Resolver defines "error" as cyan foreground
        // Compound style overrides with yellow foreground
        // Inline style wins (like CSS)
        MarkupParser.StyleResolver resolver = tagName -> {
            if ("error".equals(tagName)) {
                return Style.EMPTY.fg(Color.CYAN).bold();
            }
            return null;
        };

        Text text = MarkupParser.parse("[error yellow]text[/]", resolver);

        Style style = text.lines().get(0).spans().get(0).style();
        // Compound's yellow overrides resolver's cyan
        assertThat(style.fg()).contains(Color.YELLOW);
        // Resolver's bold is still applied (not overridden)
        assertThat(style.addModifiers()).contains(dev.tamboui.style.Modifier.BOLD);
    }

    @Test
    @DisplayName("compound style adds modifier to resolver style")
    void compoundStyleAddsModifierToResolverStyle() {
        // Resolver defines "error" as cyan+bold
        // Compound adds italic
        MarkupParser.StyleResolver resolver = tagName -> {
            if ("error".equals(tagName)) {
                return Style.EMPTY.fg(Color.CYAN).bold();
            }
            return null;
        };

        Text text = MarkupParser.parse("[error italic]text[/]", resolver);

        Style style = text.lines().get(0).spans().get(0).style();
        assertThat(style.fg()).contains(Color.CYAN);
        assertThat(style.addModifiers()).contains(
            dev.tamboui.style.Modifier.BOLD,
            dev.tamboui.style.Modifier.ITALIC
        );
    }
}

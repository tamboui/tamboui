/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.paragraph;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Hyperlink;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.text.Overflow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class ParagraphTest {

    @Test
    @DisplayName("Paragraph renders text")
    void rendersText() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("Hello"))
            .build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("e");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("l");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("l");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("o");
    }

    @Test
    @DisplayName("Paragraph with style applies to background")
    void withStyle() {
        Style style = Style.EMPTY.fg(Color.RED);
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("Hi"))
            .style(style)
            .build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        // Style applies to area background - cells without text retain the style
        assertThat(buffer.get(5, 0).style().fg()).contains(Color.RED);
        // Text cells use the span's style
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
    }

    @Test
    @DisplayName("Paragraph with block")
    void withBlock() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("Hi"))
            .block(Block.bordered())
            .build();
        Rect area = new Rect(0, 0, 10, 3);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        // Block borders (Plain border type is default)
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("┌");
        // Text inside block
        assertThat(buffer.get(1, 1).symbol()).isEqualTo("H");
    }

    @Test
    @DisplayName("Paragraph with center alignment")
    void centerAlignment() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("Hi"))
            .alignment(Alignment.CENTER)
            .build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        // "Hi" is 2 chars, centered in 10 chars = starts at position 4
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(5, 0).symbol()).isEqualTo("i");
    }

    @Test
    @DisplayName("Paragraph with right alignment")
    void rightAlignment() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("Hi"))
            .alignment(Alignment.RIGHT)
            .build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        // "Hi" is 2 chars, right-aligned in 10 chars = starts at position 8
        assertThat(buffer.get(8, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("i");
    }

    @Test
    @DisplayName("Paragraph with multi-line text")
    void multiLine() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("Line1\nLine2"))
            .build();
        Rect area = new Rect(0, 0, 10, 2);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("L");
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("L");
    }

    // ========== Overflow Mode Tests ==========

    @Test
    @DisplayName("CLIP overflow truncates text at boundary without indicator")
    void clipOverflow() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("Hello World"))
            .overflow(Overflow.CLIP)
            .build();
        Rect area = new Rect(0, 0, 5, 1);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        // Should show "Hello" (5 chars)
        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Hello", Style.EMPTY);
        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("CLIP overflow preserves multiple span styles")
    void clipOverflowPreservesSpanStyles() {
        Style redStyle = Style.EMPTY.fg(Color.RED);
        Style blueStyle = Style.EMPTY.fg(Color.BLUE);
        Line line = Line.from(
            new Span("Red", redStyle),
            new Span("Blue", blueStyle)
        );
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from(line))
            .overflow(Overflow.CLIP)
            .build();
        Rect area = new Rect(0, 0, 5, 1);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        // "RedBl" - Red keeps red style, Bl keeps blue style
        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Red", redStyle);
        expected.setString(3, 0, "Bl", blueStyle);
        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("CLIP overflow does not modify text that fits")
    void clipOverflowTextFits() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("Hi"))
            .overflow(Overflow.CLIP)
            .build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Hi", Style.EMPTY);
        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("WRAP_CHARACTER wraps long text at character boundaries")
    void wrapCharacterOverflow() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("HelloWorld"))
            .overflow(Overflow.WRAP_CHARACTER)
            .build();
        Rect area = new Rect(0, 0, 5, 2);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Hello", Style.EMPTY);
        expected.setString(0, 1, "World", Style.EMPTY);
        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("WRAP_WORD wraps at word boundaries")
    void wrapWordOverflow() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("Hello World"))
            .overflow(Overflow.WRAP_WORD)
            .build();
        Rect area = new Rect(0, 0, 7, 2);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Hello", Style.EMPTY);
        expected.setString(0, 1, "World", Style.EMPTY);
        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("WRAP_WORD breaks long words by character when necessary")
    void wrapWordBreaksLongWords() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("Supercalifragilistic"))
            .overflow(Overflow.WRAP_WORD)
            .build();
        Rect area = new Rect(0, 0, 5, 4);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Super", Style.EMPTY);
        expected.setString(0, 1, "calif", Style.EMPTY);
        expected.setString(0, 2, "ragil", Style.EMPTY);
        expected.setString(0, 3, "istic", Style.EMPTY);
        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("ELLIPSIS truncates with ellipsis at end")
    void ellipsisOverflow() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("Hello World"))
            .overflow(Overflow.ELLIPSIS)
            .build();
        Rect area = new Rect(0, 0, 8, 1);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        // Should show "Hello..." (5 chars + 3 dots = 8)
        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Hello...", Style.EMPTY);
        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("ELLIPSIS does not modify text that fits")
    void ellipsisOverflowTextFits() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("Hello"))
            .overflow(Overflow.ELLIPSIS)
            .build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Hello", Style.EMPTY);
        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("ELLIPSIS_START truncates with ellipsis at start")
    void ellipsisStartOverflow() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("Hello World"))
            .overflow(Overflow.ELLIPSIS_START)
            .build();
        Rect area = new Rect(0, 0, 8, 1);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        // Should show "...World" (3 dots + 5 chars = 8)
        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "...World", Style.EMPTY);
        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("ELLIPSIS_MIDDLE truncates with ellipsis in middle")
    void ellipsisMiddleOverflow() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("Hello World"))
            .overflow(Overflow.ELLIPSIS_MIDDLE)
            .build();
        Rect area = new Rect(0, 0, 8, 1);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        // Should show "Hel...ld" (3 left + 3 dots + 2 right = 8)
        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Hel...ld", Style.EMPTY);
        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("ELLIPSIS clips when width is too small for ellipsis")
    void ellipsisWithTinyWidth() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from("Hello"))
            .overflow(Overflow.ELLIPSIS)
            .build();
        Rect area = new Rect(0, 0, 2, 1);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        // Width 2 is less than ellipsis length (3), so just clip
        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "He", Style.EMPTY);
        BufferAssertions.assertThat(buffer).isEqualTo(expected);
    }

    // ========== Hyperlink Tests ==========

    @Test
    @DisplayName("Hyperlink wraps across multiple lines with WRAP_CHARACTER")
    void hyperlinkWrapsAcrossLinesCharacter() {
        String longUrl = "https://example.com/very/long/url/that/will/wrap";
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from(Line.from(
                Span.raw("Visit "),
                Span.raw(longUrl).hyperlink("https://example.com/very/long/url/that/will/wrap")
            )))
            .overflow(Overflow.WRAP_CHARACTER)
            .build();
        Rect area = new Rect(0, 0, 15, 5);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        // Find hyperlink cells across all lines
        List<Hyperlink> foundHyperlinks = new ArrayList<>();
        for (int y = 0; y < area.height(); y++) {
            for (int x = 0; x < area.width(); x++) {
                Hyperlink link = buffer.get(x, y).style().hyperlink().orElse(null);
                if (link != null) {
                    foundHyperlinks.add(link);
                }
            }
        }
        
        // Should have found hyperlink cells
        assertThat(foundHyperlinks).isNotEmpty();
        
        // All hyperlinks should have the same URL and ID
        Hyperlink firstLink = foundHyperlinks.get(0);
        assertThat(firstLink.url()).isEqualTo("https://example.com/very/long/url/that/will/wrap");
        assertThat(firstLink.id()).isPresent();
        
        for (Hyperlink link : foundHyperlinks) {
            assertThat(link.url()).isEqualTo("https://example.com/very/long/url/that/will/wrap");
            assertThat(link.id()).isPresent();
            assertThat(link.id()).isEqualTo(firstLink.id());
        }
        
        // Verify hyperlink spans multiple lines by checking we have links on different rows
        Set<Integer> rowsWithLinks = new HashSet<>();
        for (int y = 0; y < area.height(); y++) {
            for (int x = 0; x < area.width(); x++) {
                if (buffer.get(x, y).style().hyperlink().isPresent()) {
                    rowsWithLinks.add(y);
                }
            }
        }
        assertThat(rowsWithLinks.size()).isGreaterThan(1); // Should span multiple lines
    }

    @Test
    @DisplayName("Hyperlink wraps across multiple lines with WRAP_WORD")
    void hyperlinkWrapsAcrossLinesWord() {
        String longUrl = "https://example.com/very/long/url/that/will/wrap";
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from(Line.from(
                Span.raw("Visit "),
                Span.raw(longUrl).hyperlink("https://example.com/very/long/url/that/will/wrap")
            )))
            .overflow(Overflow.WRAP_WORD)
            .build();
        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        // Find hyperlink cells across all lines
        List<Hyperlink> foundHyperlinks = new ArrayList<>();
        for (int y = 0; y < area.height(); y++) {
            for (int x = 0; x < area.width(); x++) {
                Hyperlink link = buffer.get(x, y).style().hyperlink().orElse(null);
                if (link != null) {
                    foundHyperlinks.add(link);
                }
            }
        }
        
        // Should have found hyperlink cells
        assertThat(foundHyperlinks).isNotEmpty();
        
        // All hyperlinks should have the same URL and ID
        Hyperlink firstLink = foundHyperlinks.get(0);
        assertThat(firstLink.url()).isEqualTo("https://example.com/very/long/url/that/will/wrap");
        assertThat(firstLink.id()).isPresent();
        
        for (Hyperlink link : foundHyperlinks) {
            assertThat(link.url()).isEqualTo("https://example.com/very/long/url/that/will/wrap");
            assertThat(link.id()).isPresent();
            assertThat(link.id()).isEqualTo(firstLink.id());
        }
        
        // Verify hyperlink spans multiple lines
        Set<Integer> rowsWithLinks = new HashSet<>();
        for (int y = 0; y < area.height(); y++) {
            for (int x = 0; x < area.width(); x++) {
                if (buffer.get(x, y).style().hyperlink().isPresent()) {
                    rowsWithLinks.add(y);
                }
            }
        }
        assertThat(rowsWithLinks.size()).isGreaterThan(1); // Should span multiple lines
    }

    @Test
    @DisplayName("Hyperlink with explicit ID wraps correctly")
    void hyperlinkWithIdWrapsCorrectly() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from(Line.from(
                Span.raw("See "),
                Span.raw("https://example.com/documentation").hyperlink("https://example.com/documentation", "doc-link"),
                Span.raw(" for more info")
            )))
            .overflow(Overflow.WRAP_CHARACTER)
            .build();
        Rect area = new Rect(0, 0, 15, 3);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        // Find hyperlink cells and verify they all share the same ID
        Hyperlink link1 = null;
        Hyperlink link2 = null;
        for (int y = 0; y < area.height(); y++) {
            for (int x = 0; x < area.width(); x++) {
                Hyperlink link = buffer.get(x, y).style().hyperlink().orElse(null);
                if (link != null) {
                    if (link1 == null) {
                        link1 = link;
                    } else if (link2 == null && !link.equals(link1)) {
                        link2 = link;
                    }
                }
            }
        }
        
        assertThat(link1).isNotNull();
        assertThat(link1.url()).isEqualTo("https://example.com/documentation");
        assertThat(link1.id()).contains("doc-link");
        // All hyperlink cells should share the same ID
        assertThat(link2).isNull(); // Only one unique hyperlink should exist
    }

    @Test
    @DisplayName("Multiple hyperlinks in same line wrap independently")
    void multipleHyperlinksWrapIndependently() {
        Paragraph paragraph = Paragraph.builder()
            .text(Text.from(Line.from(
                Span.raw("Visit "),
                Span.raw("https://site1.com").hyperlink("https://site1.com"),
                Span.raw(" and "),
                Span.raw("https://site2.com").hyperlink("https://site2.com")
            )))
            .overflow(Overflow.WRAP_CHARACTER)
            .build();
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);

        paragraph.render(area, buffer);

        // Both hyperlinks should be preserved and have different IDs
        // (since they have different URLs, they'll get different auto-generated IDs)
        boolean foundLink1 = false;
        boolean foundLink2 = false;
        for (int y = 0; y < area.height(); y++) {
            for (int x = 0; x < area.width(); x++) {
                Hyperlink link = buffer.get(x, y).style().hyperlink().orElse(null);
                if (link != null) {
                    if (link.url().equals("https://site1.com")) {
                        foundLink1 = true;
                    } else if (link.url().equals("https://site2.com")) {
                        foundLink2 = true;
                    }
                }
            }
        }
        
        assertThat(foundLink1).isTrue();
        assertThat(foundLink2).isTrue();
    }
}

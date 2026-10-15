/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.style.ContinuationIndent;
import dev.tamboui.style.Hyperlink;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.Style;

import static org.assertj.core.api.Assertions.assertThat;

class TextWrapperTest {

    @Nested
    @DisplayName("isWrapping")
    class IsWrappingTests {

        @Test
        @DisplayName("WRAP_WORD and WRAP_CHARACTER return true; others return false")
        void testIsWrapping() {
            assertThat(TextWrapper.isWrapping(Overflow.WRAP_WORD)).isTrue();
            assertThat(TextWrapper.isWrapping(Overflow.WRAP_CHARACTER)).isTrue();
            assertThat(TextWrapper.isWrapping(Overflow.CLIP)).isFalse();
            assertThat(TextWrapper.isWrapping(Overflow.ELLIPSIS)).isFalse();
            assertThat(TextWrapper.isWrapping(Overflow.ELLIPSIS_START)).isFalse();
            assertThat(TextWrapper.isWrapping(Overflow.ELLIPSIS_MIDDLE)).isFalse();
            assertThat(TextWrapper.isWrapping(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("Character wrap (WRAP_CHARACTER)")
    class CharacterWrapTests {

        @Test
        @DisplayName("splits ASCII text by display columns")
        void wrapAsciiByCharacter() {
            Line line = Line.from("abcdefghij");
            List<Line> result = TextWrapper.wrap(line, 4, Overflow.WRAP_CHARACTER);

            assertThat(result).extracting(Line::rawContent)
                    .containsExactly("abcd", "efgh", "ij");
        }

        @Test
        @DisplayName("splits wide CJK characters by display width without splitting glyphs")
        void wrapCjkByCharacter() {
            // "世界你好": 4 CJK chars * 2 cols = 8 cols
            Line line = Line.from("世界你好");
            List<Line> result = TextWrapper.wrap(line, 5, Overflow.WRAP_CHARACTER);

            assertThat(result).extracting(Line::rawContent)
                    .containsExactly("世界", "你好");
        }

        @Test
        @DisplayName("single glyph wider than area takes its own line without looping")
        void singleOverwideGlyph() {
            Line line = Line.from("世界");
            List<Line> result = TextWrapper.wrap(line, 1, Overflow.WRAP_CHARACTER);

            assertThat(result).extracting(Line::rawContent)
                    .containsExactly("世", "界");
        }

        @Test
        @DisplayName("keeps ZWJ sequences and emoji flags whole")
        void keepsGraphemeClustersWhole() {
            for (String cluster : new String[] {"👩‍💻", "👨‍🦲", "🇫🇷"}) {
                Line line = Line.from(cluster + "X");
                List<Line> result = TextWrapper.wrap(line, 1, Overflow.WRAP_CHARACTER);

                assertThat(result).hasSize(2);
                assertThat(result.get(0).rawContent()).isEqualTo(cluster);
                assertThat(result.get(1).rawContent()).isEqualTo("X");
            }
        }
    }

    @Nested
    @DisplayName("Word wrap (WRAP_WORD)")
    class WordWrapTests {

        @Test
        @DisplayName("wraps at whitespace boundaries")
        void wrapAtWordBoundaries() {
            Line line = Line.from("one two three four five");
            List<Line> result = TextWrapper.wrap(line, 10, Overflow.WRAP_WORD);

            assertThat(result).extracting(Line::rawContent)
                    .containsExactly("one two", "three four", "five");
        }

        @Test
        @DisplayName("consumes trailing whitespace at the break")
        void consumesTrailingWhitespace() {
            Line line = Line.from("one two three");
            List<Line> result = TextWrapper.wrap(line, 7, Overflow.WRAP_WORD);

            assertThat(result).extracting(Line::rawContent)
                    .containsExactly("one two", "three");
        }

        @Test
        @DisplayName("breaks at punctuation when no whitespace fits")
        void breaksAtPunctuation() {
            Line line = Line.from("foo-bar-baz");
            List<Line> result = TextWrapper.wrap(line, 5, Overflow.WRAP_WORD);

            assertThat(result).extracting(Line::rawContent)
                    .containsExactly("foo-", "bar-", "baz");
        }

        @Test
        @DisplayName("breaks long words when no break point exists")
        void breaksLongWords() {
            Line line = Line.from("Supercalifragilistic");
            List<Line> result = TextWrapper.wrap(line, 5, Overflow.WRAP_WORD);

            assertThat(result).extracting(Line::rawContent)
                    .containsExactly("Super", "calif", "ragil", "istic");
        }

        @Test
        @DisplayName("preserves styles across word boundaries")
        void preservesStylesAcrossWordBoundaries() {
            Style red = Style.EMPTY.fg(Color.RED);
            Style blue = Style.EMPTY.fg(Color.BLUE);
            Line line = Line.from(Span.styled("hello ", red), Span.styled("world", blue));

            List<Line> result = TextWrapper.wrap(line, 5, Overflow.WRAP_WORD);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).spans()).hasSize(1);
            assertThat(result.get(0).spans().get(0).content()).isEqualTo("hello");
            assertThat(result.get(0).spans().get(0).style()).isEqualTo(red);

            assertThat(result.get(1).spans()).hasSize(1);
            assertThat(result.get(1).spans().get(0).content()).isEqualTo("world");
            assertThat(result.get(1).spans().get(0).style()).isEqualTo(blue);
        }
    }

    @Nested
    @DisplayName("Truncation and Ellipsis")
    class TruncationTests {

        @Test
        @DisplayName("CLIP truncates cleanly at maxWidth")
        void clipTruncation() {
            Line line = Line.from("Hello World");
            List<Line> result = TextWrapper.wrap(line, 5, Overflow.CLIP);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).rawContent()).isEqualTo("Hello");
        }

        @Test
        @DisplayName("ELLIPSIS truncates at end with ...")
        void ellipsisEnd() {
            Line line = Line.from("Hello World");
            List<Line> result = TextWrapper.wrap(line, 8, Overflow.ELLIPSIS);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).rawContent()).isEqualTo("Hello...");
        }

        @Test
        @DisplayName("ELLIPSIS_START truncates at start with ...")
        void ellipsisStart() {
            Line line = Line.from("Hello World");
            List<Line> result = TextWrapper.wrap(line, 8, Overflow.ELLIPSIS_START);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).rawContent()).isEqualTo("...World");
        }

        @Test
        @DisplayName("ELLIPSIS_MIDDLE truncates in the middle with ...")
        void ellipsisMiddle() {
            Line line = Line.from("Hello Beautiful World");
            List<Line> result = TextWrapper.wrap(line, 11, Overflow.ELLIPSIS_MIDDLE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).rawContent()).isEqualTo("Hell...orld");
        }
    }

    @Nested
    @DisplayName("Continuation Indent")
    class ContinuationIndentTests {

        @Test
        @DisplayName("HANGING strategy continues wrapped rows under first non-blank character")
        void hangingIndentPreserved() {
            Line line = Line.from(Span.raw("  - "), Span.styled("one two three four", Style.EMPTY.bold()));
            TextWrapOptions options = TextWrapOptions.of(Overflow.WRAP_WORD, ContinuationIndent.HANGING);

            List<Line> result = TextWrapper.wrap(line, 12, options);

            assertThat(result).extracting(Line::rawContent)
                    .containsExactly("  - one two", "  three four");
            // Indent padding is unstyled, text retains its bold modifier
            assertThat(result.get(1).spans().get(0).style().addModifiers()).isEmpty();
            assertThat(result.get(1).spans().get(1).content()).isEqualTo("three four");
            assertThat(result.get(1).spans().get(1).style().addModifiers()).contains(Modifier.BOLD);
        }

        @Test
        @DisplayName("FLUSH_LEFT strategy restarts wrapped rows at column 0")
        void flushLeftRestartsAtEdge() {
            Line line = Line.from(Span.raw("  - "), Span.styled("one two three four", Style.EMPTY.bold()));
            TextWrapOptions options = TextWrapOptions.of(Overflow.WRAP_WORD, ContinuationIndent.FLUSH_LEFT);

            List<Line> result = TextWrapper.wrap(line, 12, options);

            assertThat(result).extracting(Line::rawContent)
                    .containsExactly("  - one two", "three four");
            assertThat(result.get(1).spans().get(0).content()).isEqualTo("three four");
        }

        @Test
        @DisplayName("an indent that leaves no room for text falls back to flush left")
        void hangingIndentFallsBackWhenNoRoom() {
            Line line = Line.from("          abc");
            TextWrapOptions options = TextWrapOptions.of(Overflow.WRAP_CHARACTER, ContinuationIndent.HANGING);

            List<Line> result = TextWrapper.wrap(line, 10, options);

            assertThat(result).extracting(Line::rawContent)
                    .containsExactly("          ", "abc");
        }
    }

    @Nested
    @DisplayName("Hyperlink handling")
    class HyperlinkTests {

        @Test
        @DisplayName("assigns consistent hyperlink ID across multi-line wrapped chunks")
        void hyperlinkIdAssignedAcrossWrappedLines() {
            String url = "https://example.com/very/long/url/that/wraps";
            Line line = Line.from(Span.raw(url).hyperlink(url));

            List<Line> result = TextWrapper.wrap(line, 20, Overflow.WRAP_CHARACTER);

            assertThat(result.size()).isGreaterThan(1);
            Optional<Hyperlink> firstLink = result.get(0).spans().get(0).style().hyperlink();
            assertThat(firstLink).isPresent();
            String id = firstLink.get().id().orElse(null);
            assertThat(id).isNotNull();

            for (Line l : result) {
                Optional<Hyperlink> link = l.spans().get(0).style().hyperlink();
                assertThat(link).isPresent();
                assertThat(link.get().id()).contains(id);
                assertThat(link.get().url()).isEqualTo(url);
            }
        }

        @Test
        @DisplayName("preserves pre-existing explicit hyperlink ID")
        void preservesExistingHyperlinkId() {
            String url = "https://example.com/url";
            Line line = Line.from(Span.raw(url).hyperlink(url, "custom-id"));

            List<Line> result = TextWrapper.wrap(line, 10, Overflow.WRAP_CHARACTER);

            for (Line l : result) {
                Optional<Hyperlink> link = l.spans().get(0).style().hyperlink();
                assertThat(link).isPresent();
                assertThat(link.get().id()).contains("custom-id");
            }
        }
    }

    @Nested
    @DisplayName("wrapOffsets")
    class WrapOffsetsTests {

        @Test
        @DisplayName("returns correct start/end/indent for word wrapping")
        void wrapOffsetsWord() {
            String text = "one two three";
            List<int[]> segments = TextWrapper.wrapOffsets(text, 7, Overflow.WRAP_WORD);

            assertThat(segments).hasSize(2);
            assertThat(segments.get(0)).containsExactly(0, 7, 0); // "one two"
            assertThat(segments.get(1)).containsExactly(8, 13, 0); // "three"
        }

        @Test
        @DisplayName("returns correct offsets and indent for hanging indent")
        void wrapOffsetsHangingIndent() {
            String text = "  - one two three";
            TextWrapOptions options = TextWrapOptions.of(Overflow.WRAP_WORD, ContinuationIndent.HANGING);
            List<int[]> segments = TextWrapper.wrapOffsets(text, 11, options);

            assertThat(segments).hasSize(2);
            assertThat(segments.get(0)).containsExactly(0, 11, 0); // "  - one two" (length 11)
            assertThat(segments.get(1)).containsExactly(12, 17, 2); // "three" (indent = 2)
        }

        @Test
        @DisplayName("empty text returns single empty segment")
        void wrapOffsetsEmpty() {
            List<int[]> segments = TextWrapper.wrapOffsets("", 10, Overflow.WRAP_WORD);

            assertThat(segments).hasSize(1);
            assertThat(segments.get(0)).containsExactly(0, 0, 0);
        }
    }
}

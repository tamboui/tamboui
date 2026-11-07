/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.commonmark.parser.Parser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.markdown.internal.MarkdownInlineRenderer;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.Style;
import dev.tamboui.style.StylePropertyResolver;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class MarkdownViewOverflowTest {

    private static final String LONG_TEXT =
        "the quick brown fox jumps over the lazy dog and the moon and the stars";

    private static final String CODER = "\uD83D\uDC69\u200D\uD83D\uDCBB";
    private static final String FAMILY = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67";

    private static final Predicate<Style> BOLD = style -> style.addModifiers().contains(Modifier.BOLD);
    private static final Predicate<Style> LINKED = style -> style.hyperlink().isPresent();

    private static Buffer renderInto(MarkdownView view, int width, int height) {
        Rect area = new Rect(0, 0, width, height);
        Buffer buffer = Buffer.empty(area);
        view.render(area, buffer);
        return buffer;
    }

    private static Buffer renderLine(String source, Overflow overflow, int width) {
        return renderInto(MarkdownView.builder().source(source).overflow(overflow).build(), width, 1);
    }

    private static String row(Buffer buffer, int y) {
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < buffer.width(); x++) {
            sb.append(buffer.get(x, y).symbol());
        }
        return sb.toString().replaceAll("\\s+$", "");
    }

    /** Like {@link #row}, with a space for each cell whose style does not pass {@code test}. */
    private static String cellsWhere(Buffer buffer, int y, Predicate<Style> test) {
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < buffer.width(); x++) {
            Cell cell = buffer.get(x, y);
            if (cell.isContinuation()) {
                continue;
            }
            sb.append(test.test(cell.style()) ? cell.symbol() : " ");
        }
        return sb.toString().replaceAll("\\s+$", "");
    }

    @Test
    @DisplayName("WRAP_CHARACTER does not hang on a wide character wider than the line")
    void wrapCharacterWideCharWiderThanLine() {
        MarkdownView view = MarkdownView.builder().source("世界").overflow(Overflow.WRAP_CHARACTER).build();

        // each wide character takes a line of its own; it used to loop until out of memory
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> assertThat(view.computeHeight(1)).isEqualTo(2));
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> renderInto(view, 1, 4));
    }

    @Test
    @DisplayName("WRAP_WORD keeps a wide character wider than the line instead of dropping the word")
    void wrapWordKeepsWideCharWiderThanLine() {
        MarkdownView view = MarkdownView.builder().source("世界").build();

        assertThat(view.computeHeight(1)).isEqualTo(2);
    }

    @Test
    @DisplayName("WRAP_CHARACTER keeps a too-wide grapheme on one line")
    void wrapCharacterKeepsGraphemeWhole() {
        assertClusterStaysWhole(Overflow.WRAP_CHARACTER);
    }

    @Test
    @DisplayName("WRAP_WORD keeps a too-wide grapheme on one line")
    void wrapWordKeepsGraphemeWhole() {
        assertClusterStaysWhole(Overflow.WRAP_WORD);
    }

    private static void assertClusterStaysWhole(Overflow overflow) {
        for (String cluster : new String[] {"👩‍💻", "🇫🇷"}) {
            List<Line> lines = MarkdownInlineRenderer.render(
                Parser.builder().build().parse(cluster + "X").getFirstChild(),
                Style.EMPTY, 1, MarkdownStyles.DEFAULTS, overflow);

            assertThat(lines).hasSize(2);
            assertThat(lines.get(0).spans()).extracting(Span::content).containsExactly(cluster);
            assertThat(lines.get(1).spans()).extracting(Span::content).containsExactly("X");
        }
    }

    @Test
    @DisplayName("default WRAP_WORD wraps at word boundaries")
    void wrapWordIsDefault() {
        MarkdownView view = MarkdownView.builder().source(LONG_TEXT).build();
        Buffer buffer = renderInto(view, 20, 5);

        assertThat(row(buffer, 0)).doesNotContain(" jumps over");
        assertThat(row(buffer, 0)).startsWith("the quick brown fox");
    }

    @Test
    @DisplayName("CLIP renders one logical line per source line, cut at the right edge")
    void clipKeepsOneLine() {
        MarkdownView view = MarkdownView.builder()
            .source(LONG_TEXT)
            .overflow(Overflow.CLIP)
            .build();
        Buffer buffer = renderInto(view, 20, 3);

        assertThat(row(buffer, 0)).startsWith("the quick brown fox");
        assertThat(buffer.get(19, 0).symbol()).isEqualTo(" ");
        // Second row should be empty: CLIP collapses the paragraph to one line.
        assertThat(row(buffer, 1)).isEmpty();
    }

    @Test
    @DisplayName("ELLIPSIS truncates the line with a trailing \"...\"")
    void ellipsisEnd() {
        MarkdownView view = MarkdownView.builder()
            .source(LONG_TEXT)
            .overflow(Overflow.ELLIPSIS)
            .build();
        Buffer buffer = renderInto(view, 20, 1);

        String rendered = row(buffer, 0);
        assertThat(rendered).hasSize(20);
        assertThat(rendered).endsWith("...");
        assertThat(rendered).startsWith("the quick brown");
    }

    @Test
    @DisplayName("ELLIPSIS_START prefixes \"...\"")
    void ellipsisStart() {
        MarkdownView view = MarkdownView.builder()
            .source(LONG_TEXT)
            .overflow(Overflow.ELLIPSIS_START)
            .build();
        Buffer buffer = renderInto(view, 20, 1);

        assertThat(row(buffer, 0)).startsWith("...");
        assertThat(row(buffer, 0)).hasSize(20);
    }

    @Test
    @DisplayName("ELLIPSIS_MIDDLE collapses with \"...\" in the middle")
    void ellipsisMiddle() {
        MarkdownView view = MarkdownView.builder()
            .source(LONG_TEXT)
            .overflow(Overflow.ELLIPSIS_MIDDLE)
            .build();
        Buffer buffer = renderInto(view, 20, 1);

        String rendered = row(buffer, 0);
        assertThat(rendered).contains("...");
        assertThat(rendered.indexOf("...")).isGreaterThan(0);
        assertThat(rendered).hasSize(20);
    }

    @Test
    @DisplayName("ELLIPSIS keeps the style of each span, and the ellipsis takes the style of the text it replaces")
    void ellipsisEndKeepsSpanStyles() {
        Buffer buffer = renderLine("**bold** plain text that is too long", Overflow.ELLIPSIS, 12);

        assertThat(row(buffer, 0)).isEqualTo("bold plai...");
        assertThat(cellsWhere(buffer, 0, BOLD)).isEqualTo("bold");
    }

    @Test
    @DisplayName("ELLIPSIS_START keeps the style of each span")
    void ellipsisStartKeepsSpanStyles() {
        Buffer buffer = renderLine("plain text that is too long and ends **bold**", Overflow.ELLIPSIS_START, 12);

        assertThat(row(buffer, 0)).isEqualTo("...ends bold");
        assertThat(cellsWhere(buffer, 0, BOLD)).isEqualTo("        bold");
    }

    @Test
    @DisplayName("ELLIPSIS_MIDDLE keeps the style of each span")
    void ellipsisMiddleKeepsSpanStyles() {
        Buffer buffer = renderLine("**bold** plain text that is too long **end**", Overflow.ELLIPSIS_MIDDLE, 12);

        assertThat(row(buffer, 0)).isEqualTo("bold ... end");
        assertThat(cellsWhere(buffer, 0, BOLD)).isEqualTo("bold     end");
    }

    @Test
    @DisplayName("ELLIPSIS keeps a leading link's hyperlink on the link text only")
    void ellipsisKeepsHyperlinkOnLinkText() {
        Buffer buffer = renderLine("[link](https://example.com) plain text that is too long", Overflow.ELLIPSIS, 12);

        assertThat(row(buffer, 0)).isEqualTo("link plai...");
        assertThat(cellsWhere(buffer, 0, LINKED)).isEqualTo("link");
    }

    @Test
    @DisplayName("ELLIPSIS in a view narrower than the ellipsis clips the line, keeping the style of each span")
    void ellipsisNarrowerThanEllipsisKeepsSpanStyles() {
        Buffer buffer = renderLine("a **bold** text", Overflow.ELLIPSIS, 3);

        assertThat(row(buffer, 0)).isEqualTo("a b");
        assertThat(cellsWhere(buffer, 0, BOLD)).isEqualTo("  b");
    }

    @Test
    @DisplayName("ELLIPSIS modes keep a wide character at the cut whole or leave it out, keeping span styles")
    void ellipsisWideCharacterAtCut() {
        Buffer end = renderLine("**ab**\u4E16\u754C and more", Overflow.ELLIPSIS, 8);
        assertThat(row(end, 0)).isEqualTo("ab\u4E16...");
        assertThat(cellsWhere(end, 0, BOLD)).isEqualTo("ab");

        Buffer start = renderLine("more and \u4E16\u754C**end**", Overflow.ELLIPSIS_START, 7);
        assertThat(row(start, 0)).isEqualTo("...end");
        assertThat(cellsWhere(start, 0, BOLD)).isEqualTo("   end");

        Buffer middle = renderLine("**ab**\u4E16\u754C more text **end**", Overflow.ELLIPSIS_MIDDLE, 9);
        assertThat(row(middle, 0)).isEqualTo("ab...end");
        assertThat(cellsWhere(middle, 0, BOLD)).isEqualTo("ab   end");
    }

    @Test
    @DisplayName("ELLIPSIS modes keep a ZWJ emoji at the cut whole or leave it out, keeping span styles")
    void ellipsisZwjEmojiAtCut() {
        String end = "**ab** " + CODER + " and more";
        assertThat(row(renderLine(end, Overflow.ELLIPSIS, 7), 0)).isEqualTo("ab ...");
        Buffer endKept = renderLine(end, Overflow.ELLIPSIS, 8);
        assertThat(row(endKept, 0)).isEqualTo("ab " + CODER + "...");
        assertThat(cellsWhere(endKept, 0, BOLD)).isEqualTo("ab");

        String start = "more text " + FAMILY + " **end**";
        assertThat(row(renderLine(start, Overflow.ELLIPSIS_START, 8), 0)).isEqualTo("... end");
        Buffer startKept = renderLine(start, Overflow.ELLIPSIS_START, 9);
        assertThat(row(startKept, 0)).isEqualTo("..." + FAMILY + " end");
        assertThat(cellsWhere(startKept, 0, BOLD)).isEqualTo("     end");

        String middle = "**ab** " + CODER + " text that is long **end**";
        assertThat(row(renderLine(middle, Overflow.ELLIPSIS_MIDDLE, 11), 0)).isEqualTo("ab ... end");
        Buffer middleKept = renderLine(middle, Overflow.ELLIPSIS_MIDDLE, 12);
        assertThat(row(middleKept, 0)).isEqualTo("ab " + CODER + "... end");
        assertThat(cellsWhere(middleKept, 0, BOLD)).isEqualTo("ab      end");
    }

    @Test
    @DisplayName("WRAP_CHARACTER breaks mid-word")
    void wrapCharacter() {
        MarkdownView view = MarkdownView.builder()
            .source("supercalifragilisticexpialidocious")
            .overflow(Overflow.WRAP_CHARACTER)
            .build();
        Buffer buffer = renderInto(view, 10, 5);

        assertThat(row(buffer, 0)).hasSize(10);
        assertThat(row(buffer, 1)).isNotEmpty();
    }

    @Test
    @DisplayName("CSS text-overflow drives overflow when no programmatic value is set")
    void cssTextOverflow() {
        Map<String, String> rules = new HashMap<>();
        rules.put("text-overflow", "ellipsis");

        StylePropertyResolver resolver = new StylePropertyResolver() {
            @Override
            public <T> Optional<T> get(PropertyDefinition<T> property) {
                String value = rules.get(property.name());
                if (value == null) {
                    return Optional.empty();
                }
                return property.convert(value);
            }
        };

        MarkdownView view = MarkdownView.builder()
            .source(LONG_TEXT)
            .styleResolver(resolver)
            .build();
        Buffer buffer = renderInto(view, 20, 1);

        assertThat(row(buffer, 0)).endsWith("...");
    }
}

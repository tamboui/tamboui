/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Overflow;
import dev.tamboui.style.PropertyDefinition;
import dev.tamboui.style.StylePropertyResolver;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownViewOverflowTest {

    private static final String LONG_TEXT =
        "the quick brown fox jumps over the lazy dog and the moon and the stars";

    private static Buffer renderInto(MarkdownView view, int width, int height) {
        Rect area = new Rect(0, 0, width, height);
        Buffer buffer = Buffer.empty(area);
        view.render(area, buffer);
        return buffer;
    }

    private static String row(Buffer buffer, int y) {
        StringBuilder sb = new StringBuilder();
        for (int x = 0; x < buffer.width(); x++) {
            sb.append(buffer.get(x, y).symbol());
        }
        return sb.toString().replaceAll("\\s+$", "");
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

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Alignment;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;

import static org.assertj.core.api.Assertions.*;

class LineTest {

    @Test
    @DisplayName("Line.from(String) creates line with single raw span")
    void fromString() {
        Line line = Line.from("Hello");
        assertThat(line.spans()).hasSize(1);
        assertThat(line.spans().get(0).content()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("Line.from(Span...) creates line with multiple spans")
    void fromSpans() {
        Line line = Line.from(
            Span.raw("Hello "),
            Span.styled("World", Style.EMPTY.fg(Color.RED))
        );
        assertThat(line.spans()).hasSize(2);
    }

    @Test
    @DisplayName("Line width is sum of span widths")
    void width() {
        Line line = Line.from(
            Span.raw("Hello"),
            Span.raw(" "),
            Span.raw("World")
        );
        assertThat(line.width()).isEqualTo(11);
    }

    @Test
    @DisplayName("Line alignment can be set")
    void alignment() {
        Line line = Line.from("Text").alignment(Alignment.CENTER);
        assertThat(line.alignment()).contains(Alignment.CENTER);
    }

    @Test
    @DisplayName("Line default has no alignment")
    void defaultAlignment() {
        Line line = Line.from("Text");
        assertThat(line.alignment()).isEmpty();
    }

    @Test
    @DisplayName("Line fg applies to all spans")
    void fg() {
        Line line = Line.from("Text").fg(Color.RED);
        assertThat(line.spans().get(0).style().fg()).contains(Color.RED);
    }

    @Test
    @DisplayName("Line bg applies to all spans")
    void bg() {
        Line line = Line.from("Text").bg(Color.BLUE);
        assertThat(line.spans().get(0).style().bg()).contains(Color.BLUE);
    }

    @Test
    @DisplayName("Line expandTabs with default tabSize 8")
    void expandTabsDefault() {
        Line line = Line.from("at\tcom.example");
        Line expanded = line.expandTabs();
        assertThat(expanded.rawContent()).isEqualTo("at      com.example");
        assertThat(expanded.width()).isEqualTo(19);
    }

    @Test
    @DisplayName("Line expandTabs with custom tabSize 4")
    void expandTabsCustom() {
        Line line = Line.from("a\tb\tc");
        Line expanded = line.expandTabs(4);
        assertThat(expanded.rawContent()).isEqualTo("a   b   c");
        assertThat(expanded.width()).isEqualTo(9);

        Line boundary = Line.from("1234\t");
        Line boundaryExpanded = boundary.expandTabs(4);
        assertThat(boundaryExpanded.rawContent()).isEqualTo("1234    ");
        assertThat(boundaryExpanded.width()).isEqualTo(8);
    }

    @Test
    @DisplayName("Line expandTabs with tabSize 0 strips tabs")
    void expandTabsZero() {
        Line line = Line.from("a\tb\tc");
        Line expanded = line.expandTabs(0);
        assertThat(expanded.rawContent()).isEqualTo("abc");
        assertThat(expanded.width()).isEqualTo(3);
    }

    @Test
    @DisplayName("Line expandTabs throws on negative tabSize")
    void expandTabsNegative() {
        Line line = Line.from("a\tb");
        assertThatThrownBy(() -> line.expandTabs(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Line expandTabs returns same instance when no tabs exist")
    void expandTabsNoTabs() {
        Line line = Line.from("Hello world");
        assertThat(line.expandTabs(8)).isSameAs(line);

        Line empty = Line.empty();
        assertThat(empty.expandTabs(8)).isSameAs(empty);
    }

    @Test
    @DisplayName("Line expandTabs across multiple spans tracks visual column position")
    void expandTabsMultiSpan() {
        Style bold = Style.EMPTY.bold();
        Style italic = Style.EMPTY.italic();
        Line line = Line.from(
            Span.styled("at", bold),
            Span.styled("\tcom.example", italic)
        ).alignment(Alignment.CENTER);

        Line expanded = line.expandTabs(8);
        assertThat(expanded.spans()).hasSize(2);
        assertThat(expanded.spans().get(0).content()).isEqualTo("at");
        assertThat(expanded.spans().get(0).style()).isEqualTo(bold);
        assertThat(expanded.spans().get(1).content()).isEqualTo("      com.example");
        assertThat(expanded.spans().get(1).style()).isEqualTo(italic);
        assertThat(expanded.alignment()).contains(Alignment.CENTER);
        assertThat(expanded.width()).isEqualTo(19);
    }

    @Test
    @DisplayName("Line expandTabs handles wide characters before tab")
    void expandTabsWideCharacters() {
        Line line = Line.from("你好\t");
        Line expanded = line.expandTabs(8);
        // "你好" has visual width 4, so tab needs 4 spaces to reach column 8
        assertThat(expanded.rawContent()).isEqualTo("你好    ");
        assertThat(expanded.width()).isEqualTo(8);
    }

    @Test
    @DisplayName("Line expandTabs handles emoji presentation sequences before tab")
    void expandTabsEmojiSequence() {
        Line line = Line.from("⌨️\t");
        Line expanded = line.expandTabs(8);
        // "⌨️" has visual width 2, so tab needs 6 spaces to reach column 8
        assertThat(expanded.rawContent()).isEqualTo("⌨️      ");
        assertThat(expanded.width()).isEqualTo(8);
    }
}

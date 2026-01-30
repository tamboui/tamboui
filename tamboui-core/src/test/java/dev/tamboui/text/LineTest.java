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
        Line line = Line.from(Span.raw("Hello "), Span.styled("World", Style.EMPTY.fg(Color.RED)));
        assertThat(line.spans()).hasSize(2);
    }

    @Test
    @DisplayName("Line width is sum of span widths")
    void width() {
        Line line = Line.from(Span.raw("Hello"), Span.raw(" "), Span.raw("World"));
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
}

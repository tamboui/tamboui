/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;

import static org.assertj.core.api.Assertions.*;

class SpanTest {

    @Test
    @DisplayName("Span.raw creates span with empty style")
    void raw() {
        Span span = Span.raw("Hello");
        assertThat(span.content()).isEqualTo("Hello");
        assertThat(span.style()).isEqualTo(Style.EMPTY);
    }

    @Test
    @DisplayName("Span.styled creates span with given style")
    void styled() {
        Style style = Style.EMPTY.fg(Color.RED);
        Span span = Span.styled("Hello", style);
        assertThat(span.content()).isEqualTo("Hello");
        assertThat(span.style()).isEqualTo(style);
    }

    @Test
    @DisplayName("Span width returns character count")
    void width() {
        assertThat(Span.raw("Hello").width()).isEqualTo(5);
        assertThat(Span.raw("").width()).isEqualTo(0);
        assertThat(Span.raw("A").width()).isEqualTo(1);
    }

    @Test
    @DisplayName("Span fg sets foreground color")
    void fg() {
        Span span = Span.raw("Text").fg(Color.RED);
        assertThat(span.style().fg()).contains(Color.RED);
    }

    @Test
    @DisplayName("Span bg sets background color")
    void bg() {
        Span span = Span.raw("Text").bg(Color.BLUE);
        assertThat(span.style().bg()).contains(Color.BLUE);
    }

    @Test
    @DisplayName("Span bold adds bold modifier")
    void bold() {
        Span span = Span.raw("Text").bold();
        assertThat(span.style().addModifiers()).contains(Modifier.BOLD);
    }

    @Test
    @DisplayName("Span style method applies new style")
    void styleMethod() {
        Style style = Style.EMPTY.fg(Color.GREEN).bold();
        Span span = Span.raw("Text").style(style);
        assertThat(span.style()).isEqualTo(style);
    }
}

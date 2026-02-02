/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Alignment;
import dev.tamboui.style.Color;

import static org.assertj.core.api.Assertions.*;

class TextTest {

    @Test
    @DisplayName("Text.from(String) splits on newlines")
    void fromString() {
        Text text = Text.from("Line 1\nLine 2\nLine 3");
        assertThat(text.lines()).hasSize(3);
        assertThat(text.lines().get(0).spans().get(0).content()).isEqualTo("Line 1");
        assertThat(text.lines().get(1).spans().get(0).content()).isEqualTo("Line 2");
        assertThat(text.lines().get(2).spans().get(0).content()).isEqualTo("Line 3");
    }

    @Test
    @DisplayName("Text height is number of lines")
    void height() {
        Text text = Text.from("Line 1\nLine 2\nLine 3");
        assertThat(text.height()).isEqualTo(3);
    }

    @Test
    @DisplayName("Text width is max line width")
    void width() {
        Text text = Text.from("Short\nA longer line\nMedium");
        assertThat(text.width()).isEqualTo(13); // "A longer line"
    }

    @Test
    @DisplayName("Text.from(Line...) creates text from lines")
    void fromLines() {
        Text text = Text.from(
            Line.from("First"),
            Line.from("Second")
        );
        assertThat(text.lines()).hasSize(2);
    }

    @Test
    @DisplayName("Text alignment can be set")
    void alignment() {
        Text text = Text.from("Test").alignment(Alignment.CENTER);
        assertThat(text.alignment()).contains(Alignment.CENTER);
    }

    @Test
    @DisplayName("Text fg applies to all lines")
    void fg() {
        Text text = Text.from("Line 1\nLine 2").fg(Color.RED);
        assertThat(text.lines().get(0).spans().get(0).style().fg()).contains(Color.RED);
        assertThat(text.lines().get(1).spans().get(0).style().fg()).contains(Color.RED);
    }

    @Test
    @DisplayName("Empty text has zero height and width")
    void emptyText() {
        Text text = Text.from("");
        // Empty string splits to empty list of lines
        assertThat(text.height()).isEqualTo(0);
        assertThat(text.width()).isEqualTo(0);
    }
}

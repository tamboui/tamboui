/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.table;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;

import static org.assertj.core.api.Assertions.*;

class CellTest {

    @Test
    @DisplayName("Cell.from(String) creates cell with text content")
    void fromString() {
        Cell cell = Cell.from("Hello");
        assertThat(cell.content().width()).isEqualTo(5);
        assertThat(cell.height()).isEqualTo(1);
        assertThat(cell.style()).isEqualTo(Style.EMPTY);
    }

    @Test
    @DisplayName("Cell.from(Span) creates cell with styled span")
    void fromSpan() {
        Span span = Span.raw("Styled").bold();
        Cell cell = Cell.from(span);
        assertThat(cell.content().width()).isEqualTo(6);
    }

    @Test
    @DisplayName("Cell.from(Line) creates cell with line content")
    void fromLine() {
        Line line = Line.from(Span.raw("First"), Span.raw("Second"));
        Cell cell = Cell.from(line);
        assertThat(cell.content().width()).isEqualTo(11);
    }

    @Test
    @DisplayName("Cell.from(Text) creates cell with multi-line content")
    void fromText() {
        Text text = Text.from("Line 1\nLine 2\nLine 3");
        Cell cell = Cell.from(text);
        assertThat(cell.height()).isEqualTo(3);
        assertThat(cell.width()).isEqualTo(6);
    }

    @Test
    @DisplayName("Cell.empty creates empty cell")
    void emptyCell() {
        Cell cell = Cell.empty();
        assertThat(cell.width()).isEqualTo(0);
        assertThat(cell.height()).isEqualTo(1);
    }

    @Test
    @DisplayName("Cell.style returns new cell with style")
    void withStyle() {
        Cell cell = Cell.from("Test");
        Cell styled = cell.style(Style.EMPTY.fg(Color.RED));

        assertThat(styled.style().fg()).contains(Color.RED);
        // Original is unchanged
        assertThat(cell.style()).isEqualTo(Style.EMPTY);
    }

    @Test
    @DisplayName("Cell alignment methods set and preserve alignment")
    void alignmentMethods() {
        Cell cell = Cell.from("Test");
        assertThat(cell.alignment()).isEmpty();

        Cell right = cell.rightAligned();
        assertThat(right.alignment()).contains(dev.tamboui.layout.Alignment.RIGHT);

        Cell center = cell.centered();
        assertThat(center.alignment()).contains(dev.tamboui.layout.Alignment.CENTER);

        Cell left = cell.leftAligned();
        assertThat(left.alignment()).contains(dev.tamboui.layout.Alignment.LEFT);

        Cell styled = right.style(Style.EMPTY.fg(Color.RED));
        assertThat(styled.alignment()).contains(dev.tamboui.layout.Alignment.RIGHT);
        assertThat(styled.style().fg()).contains(Color.RED);
    }

    @Test
    @DisplayName("Cell falls back to content alignment when cell alignment is not explicitly set")
    void fallbackToContentAlignment() {
        Text text = Text.from("Centered").centered();
        Cell cell = Cell.from(text);
        assertThat(cell.alignment()).contains(dev.tamboui.layout.Alignment.CENTER);

        // Explicit cell alignment overrides content alignment
        Cell overridden = cell.rightAligned();
        assertThat(overridden.alignment()).contains(dev.tamboui.layout.Alignment.RIGHT);
    }
}

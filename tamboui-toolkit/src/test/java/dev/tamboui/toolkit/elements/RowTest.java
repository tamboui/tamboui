/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Flex;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.RenderContext;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Row.
 */
class RowTest {

    @Test
    @DisplayName("preferredWidth() returns 0 for empty row")
    void preferredWidth_emptyRow() {
        Row row = row();
        assertThat(row.preferredWidth()).isEqualTo(0);
    }

    @Test
    @DisplayName("preferredWidth() calculates width for single child")
    void preferredWidth_singleChild() {
        Row row = row(text("Hello"));
        // "Hello" = 5 characters
        assertThat(row.preferredWidth()).isEqualTo(5);
    }

    @Test
    @DisplayName("preferredWidth() sums widths of multiple children")
    void preferredWidth_multipleChildren() {
        Row row = row(
            text("A"),      // 1
            text("BB"),     // 2
            text("CCC")     // 3
        );
        // 1 + 2 + 3 = 6
        assertThat(row.preferredWidth()).isEqualTo(6);
    }

    @Test
    @DisplayName("preferredWidth() includes spacing")
    void preferredWidth_withSpacing() {
        Row row = row(
            text("A"),      // 1
            text("B"),      // 1
            text("C")       // 1
        ).spacing(2);
        // 1 + 2 + 1 + 2 + 1 = 7 (three children, two gaps of 2)
        assertThat(row.preferredWidth()).isEqualTo(7);
    }

    @Test
    @DisplayName("preferredWidth() includes margin")
    void preferredWidth_withMargin() {
        Row row = row(
            text("Hello")   // 5
        ).margin(new Margin(1, 2, 1, 3)); // top, right, bottom, left
        // 5 + 2 (right) + 3 (left) = 10
        assertThat(row.preferredWidth()).isEqualTo(10);
    }

    @Test
    @DisplayName("preferredWidth() with uniform margin")
    void preferredWidth_withUniformMargin() {
        Row row = row(
            text("Test")    // 4
        ).margin(1);
        // 4 + 1 (left) + 1 (right) = 6
        assertThat(row.preferredWidth()).isEqualTo(6);
    }

    @Test
    @DisplayName("preferredWidth() with spacing and margin")
    void preferredWidth_withSpacingAndMargin() {
        Row row = row(
            text("A"),      // 1
            text("B")       // 1
        ).spacing(2).margin(1);
        // 1 + 2 + 1 + 1 (left) + 1 (right) = 6
        assertThat(row.preferredWidth()).isEqualTo(6);
    }

    @Test
    @DisplayName("preferredWidth() with nested rows")
    void preferredWidth_nested() {
        Row innerRow = row(text("AB"), text("CD"));  // 2 + 2 = 4
        Row outerRow = row(text("X"), innerRow);     // 1 + 4 = 5
        assertThat(outerRow.preferredWidth()).isEqualTo(5);
    }

    @Test
    @DisplayName("preferredWidth() with tabs element")
    void preferredWidth_withTabs() {
        Row row = row(
            text("Title"),                              // 5
            tabs("App", "Logs").divider(" | "),        // 10
            text("Status")                              // 6
        );
        // 5 + 10 + 6 = 21
        assertThat(row.preferredWidth()).isEqualTo(21);
    }

    @Test
    @DisplayName("preferredWidth() with wave text")
    void preferredWidth_withWaveText() {
        Row row = row(
            waveText("Loading..."),     // 10
            text(" Done")                // 5
        );
        // 10 + 5 = 15
        assertThat(row.preferredWidth()).isEqualTo(15);
    }

    @Test
    @DisplayName("Row renders to buffer")
    void rendersToBuffer() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        row(text("A"), text("B"), text("C"))
            .spacing(1)
            .render(frame, area, RenderContext.empty());

        assertThat(buffer).isNotNull();
    }

    @Test
    @DisplayName("Row with flex center")
    void withFlexCenter() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        row(text("A"), text("B"))
            .flex(Flex.CENTER)
            .render(frame, area, RenderContext.empty());

        assertThat(buffer).isNotNull();
    }

    @Test
    @DisplayName("width: fit constraint via CSS")
    void widthFitWithCss() {
        // Given CSS with width: fit
        String css = ".myrow { width: fit; }";
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Row row = row(text("Hello"), text("World")).addClass("myrow");

        // When queried
        int width = row.preferredWidth();

        // Then should return sum of children
        // "Hello" + "World" = 5 + 5 = 10
        assertThat(width).isEqualTo(10);
    }

    @Test
    @DisplayName("Empty area does not render")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 3));
        Frame frame = Frame.forTesting(buffer);

        // Should not throw
        row(text("Test")).render(frame, emptyArea, RenderContext.empty());
    }

    @Test
    @DisplayName("Row fluent API chains correctly")
    void fluentApiChaining() {
        Row row = row(text("A"), text("B"))
            .spacing(1)
            .flex(Flex.SPACE_BETWEEN)
            .margin(2);

        assertThat(row).isInstanceOf(Row.class);
    }
}
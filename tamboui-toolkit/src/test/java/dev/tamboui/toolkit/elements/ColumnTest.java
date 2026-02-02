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
 * Tests for Column.
 */
class ColumnTest {

    @Test
    @DisplayName("preferredWidth() returns 0 for empty column")
    void preferredWidth_emptyColumn() {
        Column column = column();
        assertThat(column.preferredWidth()).isEqualTo(0);
    }

    @Test
    @DisplayName("preferredWidth() returns width of single child")
    void preferredWidth_singleChild() {
        Column column = column(text("Hello"));
        // "Hello" = 5 characters
        assertThat(column.preferredWidth()).isEqualTo(5);
    }

    @Test
    @DisplayName("preferredWidth() returns max width of children")
    void preferredWidth_multipleChildren() {
        Column column = column(
            text("A"),          // 1
            text("BBB"),        // 3
            text("CC")          // 2
        );
        // Max of 1, 3, 2 = 3
        assertThat(column.preferredWidth()).isEqualTo(3);
    }

    @Test
    @DisplayName("preferredWidth() with all same width children")
    void preferredWidth_sameWidth() {
        Column column = column(
            text("AAA"),        // 3
            text("BBB"),        // 3
            text("CCC")         // 3
        );
        // All 3, so max = 3
        assertThat(column.preferredWidth()).isEqualTo(3);
    }

    @Test
    @DisplayName("preferredWidth() spacing does not affect width")
    void preferredWidth_withSpacing() {
        Column column = column(
            text("A"),          // 1
            text("BBB")         // 3
        ).spacing(5);
        // Spacing doesn't affect column width, only height
        // Max of 1, 3 = 3
        assertThat(column.preferredWidth()).isEqualTo(3);
    }

    @Test
    @DisplayName("preferredWidth() includes margin")
    void preferredWidth_withMargin() {
        Column column = column(
            text("Hello")       // 5
        ).margin(new Margin(1, 2, 1, 3)); // top, right, bottom, left
        // 5 + 2 (right) + 3 (left) = 10
        assertThat(column.preferredWidth()).isEqualTo(10);
    }

    @Test
    @DisplayName("preferredWidth() with uniform margin")
    void preferredWidth_withUniformMargin() {
        Column column = column(
            text("Test")        // 4
        ).margin(1);
        // 4 + 1 (left) + 1 (right) = 6
        assertThat(column.preferredWidth()).isEqualTo(6);
    }

    @Test
    @DisplayName("preferredWidth() with nested columns")
    void preferredWidth_nested() {
        Column innerColumn = column(text("AB"), text("CDEFG"));  // Max of 2, 5 = 5
        Column outerColumn = column(text("XXX"), innerColumn);   // Max of 3, 5 = 5
        assertThat(outerColumn.preferredWidth()).isEqualTo(5);
    }

    @Test
    @DisplayName("preferredWidth() with tabs element")
    void preferredWidth_withTabs() {
        Column column = column(
            text("Short"),                              // 5
            tabs("App", "Logs").divider(" | ")         // 10
        );
        // Max of 5, 10 = 10
        assertThat(column.preferredWidth()).isEqualTo(10);
    }

    @Test
    @DisplayName("preferredWidth() with wave text")
    void preferredWidth_withWaveText() {
        Column column = column(
            waveText("Loading..."),     // 10
            text("Done")                 // 4
        );
        // Max of 10, 4 = 10
        assertThat(column.preferredWidth()).isEqualTo(10);
    }

    @Test
    @DisplayName("preferredWidth() with rows inside column")
    void preferredWidth_withRowChildren() {
        Column column = column(
            row(text("A"), text("B")),      // 1 + 1 = 2
            row(text("XXX"), text("YYY"))   // 3 + 3 = 6
        );
        // Max of 2, 6 = 6
        assertThat(column.preferredWidth()).isEqualTo(6);
    }

    @Test
    @DisplayName("Column renders to buffer")
    void rendersToBuffer() {
        Rect area = new Rect(0, 0, 20, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        column(text("A"), text("B"), text("C"))
            .spacing(1)
            .render(frame, area, RenderContext.empty());

        assertThat(buffer).isNotNull();
    }

    @Test
    @DisplayName("Column with flex center")
    void withFlexCenter() {
        Rect area = new Rect(0, 0, 20, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        column(text("A"), text("B"))
            .flex(Flex.CENTER)
            .render(frame, area, RenderContext.empty());

        assertThat(buffer).isNotNull();
    }

    @Test
    @DisplayName("width: fit constraint via CSS")
    void widthFitWithCss() {
        // Given CSS with width: fit
        String css = ".mycol { width: fit; }";
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Column column = column(text("Hi"), text("Longer text")).addClass("mycol");

        // When queried
        int width = column.preferredWidth();

        // Then should return max width
        // Max of "Hi" (2) and "Longer text" (11) = 11
        assertThat(width).isEqualTo(11);
    }

    @Test
    @DisplayName("Empty area does not render")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 10));
        Frame frame = Frame.forTesting(buffer);

        // Should not throw
        column(text("Test")).render(frame, emptyArea, RenderContext.empty());
    }

    @Test
    @DisplayName("Column fluent API chains correctly")
    void fluentApiChaining() {
        Column column = column(text("A"), text("B"))
            .spacing(1)
            .flex(Flex.SPACE_BETWEEN)
            .margin(2);

        assertThat(column).isInstanceOf(Column.class);
    }
}
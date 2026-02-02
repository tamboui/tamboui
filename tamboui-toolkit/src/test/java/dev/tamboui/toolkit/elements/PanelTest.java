/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Direction;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Padding;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Panel.
 */
class PanelTest {

    @Test
    @DisplayName("Panel exposes title attribute")
    void styleAttributes_exposesTitle() {
        assertThat(panel("Test Tree").styleAttributes()).containsEntry("title", "Test Tree");
    }

    @Test
    @DisplayName("Panel exposes bottom-title attribute")
    void styleAttributes_exposesBottomTitle() {
        assertThat(panel().bottomTitle("Status").styleAttributes()).containsEntry("bottom-title", "Status");
    }

    @Test
    @DisplayName("Panel without title has empty styleAttributes")
    void styleAttributes_emptyWithoutTitle() {
        assertThat(panel().styleAttributes()).isEmpty();
    }

    @Test
    @DisplayName("Panel includes generic attr() attributes")
    void styleAttributes_includesGenericAttrs() {
        assertThat(panel("Test").attr("data-type", "info").styleAttributes())
            .containsEntry("title", "Test")
            .containsEntry("data-type", "info");
    }

    @Test
    @DisplayName("Attribute selector affects Panel border color")
    void attributeSelector_affectsBorderColor() {
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", "Panel[title=\"Test\"] { border-color: cyan; }");
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        panel("Test").render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.CYAN);
    }

    // ============ preferredWidth tests ============

    @Test
    @DisplayName("preferredWidth() returns border width for empty panel")
    void preferredWidth_emptyPanel() {
        Panel panel = panel();
        // Empty panel with default border = 2
        assertThat(panel.preferredWidth()).isEqualTo(2);
    }

    @Test
    @DisplayName("preferredWidth() vertical direction returns max child width")
    void preferredWidth_verticalChildren() {
        Panel panel = panel(
            text("A"),          // 1
            text("BBB"),        // 3
            text("CC")          // 2
        );
        // Max of 1, 3, 2 = 3, plus borders (2) = 5
        assertThat(panel.preferredWidth()).isEqualTo(5);
    }

    @Test
    @DisplayName("preferredWidth() horizontal direction sums child widths")
    void preferredWidth_horizontalChildren() {
        Panel panel = panel(
            text("A"),          // 1
            text("BB"),         // 2
            text("CCC")         // 3
        ).horizontal();
        // 1 + 2 + 3 = 6, plus borders (2) = 8
        assertThat(panel.preferredWidth()).isEqualTo(8);
    }

    @Test
    @DisplayName("preferredWidth() horizontal with spacing")
    void preferredWidth_horizontalWithSpacing() {
        Panel panel = panel(
            text("A"),          // 1
            text("B"),          // 1
            text("C")           // 1
        ).horizontal().spacing(2);
        // 1 + 2 + 1 + 2 + 1 = 7, plus borders (2) = 9
        assertThat(panel.preferredWidth()).isEqualTo(9);
    }

    @Test
    @DisplayName("preferredWidth() includes padding")
    void preferredWidth_withPadding() {
        Panel panel = panel(
            text("Hello")       // 5
        ).padding(new Padding(1, 2, 1, 3)); // top, right, bottom, left
        // 5 + 2 (right padding) + 3 (left padding) + 2 (borders) = 12
        assertThat(panel.preferredWidth()).isEqualTo(12);
    }

    @Test
    @DisplayName("preferredWidth() with uniform padding")
    void preferredWidth_withUniformPadding() {
        Panel panel = panel(
            text("Test")        // 4
        ).padding(1);
        // 4 + 1 (left) + 1 (right) + 2 (borders) = 8
        assertThat(panel.preferredWidth()).isEqualTo(8);
    }

    @Test
    @DisplayName("preferredWidth() includes margin")
    void preferredWidth_withMargin() {
        Panel panel = panel(
            text("Hi")          // 2
        ).margin(new Margin(1, 2, 1, 3)); // top, right, bottom, left
        // 2 + 2 (borders) + 2 (right margin) + 3 (left margin) = 9
        assertThat(panel.preferredWidth()).isEqualTo(9);
    }

    @Test
    @DisplayName("preferredWidth() with nested panels")
    void preferredWidth_nested() {
        Panel innerPanel = panel(text("ABCD"));     // 4 + 2 (borders) = 6
        Panel outerPanel = panel(innerPanel);       // 6 + 2 (borders) = 8
        assertThat(outerPanel.preferredWidth()).isEqualTo(8);
    }

    @Test
    @DisplayName("preferredWidth() with tabs in panel")
    void preferredWidth_withTabs() {
        Panel panel = panel(
            tabs("Home", "Settings").divider(" | ")  // 15
        );
        // 15 + 2 (borders) = 17
        assertThat(panel.preferredWidth()).isEqualTo(17);
    }

    @Test
    @DisplayName("preferredWidth() horizontal panel with mixed elements")
    void preferredWidth_horizontalMixed() {
        Panel panel = panel(
            text("Label:"),                         // 6
            tabs("A", "B").divider("|"),           // 3
            waveText("Loading")                     // 7
        ).horizontal().spacing(1);
        // 6 + 1 + 3 + 1 + 7 = 18, plus borders (2) = 20
        assertThat(panel.preferredWidth()).isEqualTo(20);
    }

    @Test
    @DisplayName("Panel with direction method")
    void withDirectionMethod() {
        Panel vertical = panel(text("A"), text("B"))
            .direction(Direction.VERTICAL);
        Panel horizontal = panel(text("A"), text("B"))
            .direction(Direction.HORIZONTAL);

        assertThat(vertical.preferredWidth()).isEqualTo(3); // max(1,1) + 2 borders
        assertThat(horizontal.preferredWidth()).isEqualTo(4); // 1+1 + 2 borders
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Alignment;
import dev.tamboui.layout.Direction;
import dev.tamboui.layout.Margin;
import dev.tamboui.layout.Padding;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.element.DefaultRenderContext;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Panel.
 */
class PanelTest extends AbstractElementTest {

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

    // ============ title alignment tests ============

    @Test
    @DisplayName("Title is left-aligned by default")
    void titleAlignment_defaultsToLeft() {
        Buffer buffer = renderPanel(panel("Hi"), new Rect(0, 0, 20, 3));
        // startX = left border + 1 = 1
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("i");
    }

    @Test
    @DisplayName("titleCenter() centers the title in the top border")
    void titleCenter_centersTitle() {
        Buffer buffer = renderPanel(panel("Hi").titleCenter(), new Rect(0, 0, 20, 3));
        // availableWidth = 20 - 2 = 18, startX = 1, x = 1 + (18 - 2) / 2 = 9
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(10, 0).symbol()).isEqualTo("i");
    }

    @Test
    @DisplayName("titleRight() right-aligns the title in the top border")
    void titleRight_rightAlignsTitle() {
        Buffer buffer = renderPanel(panel("Hi").titleRight(), new Rect(0, 0, 20, 3));
        // x = startX + availableWidth - titleWidth = 1 + 18 - 2 = 17
        assertThat(buffer.get(17, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(18, 0).symbol()).isEqualTo("i");
    }

    @Test
    @DisplayName("bottomTitleAlignment() aligns the bottom title")
    void bottomTitleAlignment_centersBottomTitle() {
        Buffer buffer = renderPanel(
            panel().bottomTitle("Hi").bottomTitleAlignment(Alignment.CENTER),
            new Rect(0, 0, 20, 3));
        // bottom border row = height - 1 = 2; centered x = 9
        assertThat(buffer.get(9, 2).symbol()).isEqualTo("H");
        assertThat(buffer.get(10, 2).symbol()).isEqualTo("i");
    }

    @Test
    @DisplayName("CSS text-align centers the title")
    void titleAlignment_fromCssTextAlign() {
        Buffer buffer = renderPanelStyled(panel("Hi"), new Rect(0, 0, 20, 3),
            "Panel { text-align: center; }");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(10, 0).symbol()).isEqualTo("i");
    }

    @Test
    @DisplayName("Explicit title alignment overrides CSS text-align")
    void titleAlignment_explicitOverridesCss() {
        Buffer buffer = renderPanelStyled(panel("Hi").titleRight(), new Rect(0, 0, 20, 3),
            "Panel { text-align: center; }");
        // explicit right wins over CSS center
        assertThat(buffer.get(17, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(18, 0).symbol()).isEqualTo("i");
    }

    // ============ preferredWidth tests ============

    @Test
    @DisplayName("preferredWidth() returns border width for empty panel")
    void preferredWidth_emptyPanel() {
        Panel panel = panel();
        // Empty panel with default border = 2
        assertThat(panel.preferredSize(-1, -1, null).widthOr(0)).isEqualTo(2);
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
        assertThat(panel.preferredSize(-1, -1, null).widthOr(0)).isEqualTo(5);
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
        assertThat(panel.preferredSize(-1, -1, null).widthOr(0)).isEqualTo(8);
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
        assertThat(panel.preferredSize(-1, -1, null).widthOr(0)).isEqualTo(9);
    }

    @Test
    @DisplayName("preferredWidth() includes padding")
    void preferredWidth_withPadding() {
        Panel panel = panel(
            text("Hello")       // 5
        ).padding(new Padding(1, 2, 1, 3)); // top, right, bottom, left
        // 5 + 2 (right padding) + 3 (left padding) + 2 (borders) = 12
        assertThat(panel.preferredSize(-1, -1, null).widthOr(0)).isEqualTo(12);
    }

    @Test
    @DisplayName("preferredWidth() with uniform padding")
    void preferredWidth_withUniformPadding() {
        Panel panel = panel(
            text("Test")        // 4
        ).padding(1);
        // 4 + 1 (left) + 1 (right) + 2 (borders) = 8
        assertThat(panel.preferredSize(-1, -1, null).widthOr(0)).isEqualTo(8);
    }

    @Test
    @DisplayName("preferredWidth() includes margin")
    void preferredWidth_withMargin() {
        Panel panel = panel(
            text("Hi")          // 2
        ).margin(new Margin(1, 2, 1, 3)); // top, right, bottom, left
        // 2 + 2 (borders) + 2 (right margin) + 3 (left margin) = 9
        assertThat(panel.preferredSize(-1, -1, null).widthOr(0)).isEqualTo(9);
    }

    @Test
    @DisplayName("preferredWidth() with nested panels")
    void preferredWidth_nested() {
        Panel innerPanel = panel(text("ABCD"));     // 4 + 2 (borders) = 6
        Panel outerPanel = panel(innerPanel);       // 6 + 2 (borders) = 8
        assertThat(outerPanel.preferredSize(-1, -1, null).widthOr(0)).isEqualTo(8);
    }

    @Test
    @DisplayName("preferredWidth() with tabs in panel")
    void preferredWidth_withTabs() {
        Panel panel = panel(
            tabs("Home", "Settings").divider(" | ")  // 15
        );
        // 15 + 2 (borders) = 17
        assertThat(panel.preferredSize(-1, -1, null).widthOr(0)).isEqualTo(17);
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
        assertThat(panel.preferredSize(-1, -1, null).widthOr(0)).isEqualTo(20);
    }

    @Test
    @DisplayName("Panel with direction method")
    void withDirectionMethod() {
        Panel vertical = panel(text("A"), text("B"))
            .direction(Direction.VERTICAL);
        Panel horizontal = panel(text("A"), text("B"))
            .direction(Direction.HORIZONTAL);

        assertThat(vertical.preferredSize(-1, -1, null).widthOr(0)).isEqualTo(3); // max(1,1) + 2 borders
        assertThat(horizontal.preferredSize(-1, -1, null).widthOr(0)).isEqualTo(4); // 1+1 + 2 borders
    }

    private static Buffer renderPanel(Panel panel, Rect area) {
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        panel.render(frame, area, DefaultRenderContext.createEmpty());
        return buffer;
    }

    private static Buffer renderPanelStyled(Panel panel, Rect area, String css) {
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        panel.render(frame, area, context);
        return buffer;
    }
}

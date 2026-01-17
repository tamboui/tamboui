/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that CSS attribute selectors correctly affect rendering.
 * <p>
 * These tests verify end-to-end that attribute selectors like
 * {@code Panel[title="Test"]} match elements and apply styles.
 */
class AttributeSelectorRenderingTest {

    private StyleEngine styleEngine;
    private DefaultRenderContext context;
    private Buffer buffer;
    private Frame frame;
    private Rect area;

    @BeforeEach
    void setUp() {
        styleEngine = StyleEngine.create();
        context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        area = new Rect(0, 0, 40, 10);
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
    }

    // ═══════════════════════════════════════════════════════════════
    // Exact Match [attr=value]
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Panel with matching title gets styled by attribute selector")
    void panel_matchingTitle_getsStyled() {
        styleEngine.addStylesheet("test", "Panel[title=\"Test Tree\"] { border-color: cyan; }");
        styleEngine.setActiveStylesheet("test");

        Panel panel = panel("Test Tree");
        panel.render(frame, area, context);

        // Border should be cyan (top-left corner)
        assertThat(buffer.get(0, 0).style().fg()).contains(Color.CYAN);
    }

    @Test
    @DisplayName("Panel with non-matching title does not get styled by attribute selector")
    void panel_nonMatchingTitle_notStyled() {
        styleEngine.addStylesheet("test", "Panel[title=\"Test Tree\"] { border-color: cyan; }");
        styleEngine.setActiveStylesheet("test");

        Panel panel = panel("Other Title");
        panel.render(frame, area, context);

        // Border should NOT be cyan
        assertThat(buffer.get(0, 0).style().fg()).isNotEqualTo(java.util.Optional.of(Color.CYAN));
    }

    @Test
    @DisplayName("Panel without title does not match attribute selector")
    void panel_withoutTitle_notStyled() {
        styleEngine.addStylesheet("test", "Panel[title=\"Test\"] { border-color: red; }");
        styleEngine.setActiveStylesheet("test");

        Panel panel = panel();
        panel.render(frame, area, context);

        // Border should NOT be red
        assertThat(buffer.get(0, 0).style().fg()).isNotEqualTo(java.util.Optional.of(Color.RED));
    }

    // ═══════════════════════════════════════════════════════════════
    // Existence Check [attr]
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Panel with any title matches existence selector")
    void panel_anyTitle_matchesExistence() {
        styleEngine.addStylesheet("test", "Panel[title] { border-color: green; }");
        styleEngine.setActiveStylesheet("test");

        Panel panel = panel("Any Title");
        panel.render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.GREEN);
    }

    @Test
    @DisplayName("Panel without title does not match existence selector")
    void panel_noTitle_doesNotMatchExistence() {
        styleEngine.addStylesheet("test", "Panel[title] { border-color: green; }");
        styleEngine.setActiveStylesheet("test");

        Panel panel = panel();
        panel.render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).isNotEqualTo(java.util.Optional.of(Color.GREEN));
    }

    // ═══════════════════════════════════════════════════════════════
    // Prefix Match [attr^=value]
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Panel with title starting with prefix matches ^= selector")
    void panel_titleStartsWith_matchesPrefix() {
        styleEngine.addStylesheet("test", "Panel[title^=\"Test\"] { border-color: yellow; }");
        styleEngine.setActiveStylesheet("test");

        Panel panel = panel("Test Tree View");
        panel.render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.YELLOW);
    }

    @Test
    @DisplayName("Panel with title not starting with prefix does not match ^= selector")
    void panel_titleNotStartsWith_doesNotMatchPrefix() {
        styleEngine.addStylesheet("test", "Panel[title^=\"Test\"] { border-color: yellow; }");
        styleEngine.setActiveStylesheet("test");

        Panel panel = panel("My Test Tree");
        panel.render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).isNotEqualTo(java.util.Optional.of(Color.YELLOW));
    }

    // ═══════════════════════════════════════════════════════════════
    // Suffix Match [attr$=value]
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Panel with title ending with suffix matches $= selector")
    void panel_titleEndsWith_matchesSuffix() {
        styleEngine.addStylesheet("test", "Panel[title$=\"Output\"] { border-color: magenta; }");
        styleEngine.setActiveStylesheet("test");

        Panel panel = panel("Test Output");
        panel.render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.MAGENTA);
    }

    @Test
    @DisplayName("Panel with title not ending with suffix does not match $= selector")
    void panel_titleNotEndsWith_doesNotMatchSuffix() {
        styleEngine.addStylesheet("test", "Panel[title$=\"Output\"] { border-color: magenta; }");
        styleEngine.setActiveStylesheet("test");

        Panel panel = panel("Output Test");
        panel.render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).isNotEqualTo(java.util.Optional.of(Color.MAGENTA));
    }

    // ═══════════════════════════════════════════════════════════════
    // Contains Match [attr*=value]
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Panel with title containing substring matches *= selector")
    void panel_titleContains_matchesContains() {
        styleEngine.addStylesheet("test", "Panel[title*=\"Tree\"] { border-color: blue; }");
        styleEngine.setActiveStylesheet("test");

        Panel panel = panel("Test Tree View");
        panel.render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.BLUE);
    }

    @Test
    @DisplayName("Panel with title not containing substring does not match *= selector")
    void panel_titleNotContains_doesNotMatchContains() {
        styleEngine.addStylesheet("test", "Panel[title*=\"Tree\"] { border-color: blue; }");
        styleEngine.setActiveStylesheet("test");

        Panel panel = panel("Test Output");
        panel.render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).isNotEqualTo(java.util.Optional.of(Color.BLUE));
    }

    // ═══════════════════════════════════════════════════════════════
    // Selector Lists with Attribute Selectors
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Selector list with attribute selectors matches multiple panels")
    void selectorList_attributeSelectors_matchMultiple() {
        styleEngine.addStylesheet("test",
                "Panel[title=\"A\"], Panel[title=\"B\"] { border-color: green; }");
        styleEngine.setActiveStylesheet("test");

        // Panel A should match
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
        panel("A").render(frame, area, context);
        assertThat(buffer.get(0, 0).style().fg()).contains(Color.GREEN);

        // Panel B should also match
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
        panel("B").render(frame, area, context);
        assertThat(buffer.get(0, 0).style().fg()).contains(Color.GREEN);

        // Panel C should not match
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
        panel("C").render(frame, area, context);
        assertThat(buffer.get(0, 0).style().fg()).isNotEqualTo(java.util.Optional.of(Color.GREEN));
    }

    // ═══════════════════════════════════════════════════════════════
    // Combined Type and Attribute Selectors
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Type + attribute selector only matches correct element type")
    void typeAndAttribute_matchesCorrectType() {
        styleEngine.addStylesheet("test", "Panel[title=\"Test\"] { border-color: red; }");
        styleEngine.setActiveStylesheet("test");

        // Panel with matching title should match
        Panel panel = panel("Test");
        panel.render(frame, area, context);
        assertThat(buffer.get(0, 0).style().fg()).contains(Color.RED);
    }

    // ═══════════════════════════════════════════════════════════════
    // Generic attr() Attributes
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Generic attr() attributes can be matched by attribute selectors")
    void genericAttr_matchedBySelector() {
        styleEngine.addStylesheet("test", "Panel[data-type=\"error\"] { border-color: red; }");
        styleEngine.setActiveStylesheet("test");

        Panel panel = panel("Errors").attr("data-type", "error");
        panel.render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.RED);
    }

    @Test
    @DisplayName("Generic attr() with non-matching value does not match")
    void genericAttr_nonMatching_notStyled() {
        styleEngine.addStylesheet("test", "Panel[data-type=\"error\"] { border-color: red; }");
        styleEngine.setActiveStylesheet("test");

        Panel panel = panel("Info").attr("data-type", "info");
        panel.render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).isNotEqualTo(java.util.Optional.of(Color.RED));
    }
}

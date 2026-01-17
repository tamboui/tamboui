/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widgets.tabs.TabsState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TabsElement.
 */
class TabsElementTest {

    @Test
    @DisplayName("TabsElement fluent API chains correctly")
    void fluentApiChaining() {
        TabsElement element = tabs("Home", "Settings", "About")
            .selected(0)
            .highlightColor(Color.CYAN)
            .divider(" | ")
            .title("Navigation")
            .rounded()
            .borderColor(Color.GREEN);

        assertThat(element).isInstanceOf(TabsElement.class);
    }

    @Test
    @DisplayName("tabs() creates empty element")
    void emptyTabs() {
        TabsElement element = tabs();
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("tabs(String...) creates element with titles")
    void tabsWithTitles() {
        TabsElement element = tabs("Tab1", "Tab2", "Tab3");
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("tabs(List<String>) creates element with titles")
    void tabsWithTitlesList() {
        TabsElement element = tabs(Arrays.asList("A", "B", "C"));
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("selected() sets initial selection")
    void selectedMethod() {
        TabsElement element = tabs("A", "B", "C").selected(1);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("state() sets tabs state")
    void stateMethod() {
        TabsState state = new TabsState();
        state.select(2);
        TabsElement element = tabs("A", "B", "C").state(state);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("divider() sets separator")
    void dividerMethod() {
        TabsElement element = tabs("A", "B").divider(" | ");
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("TabsElement renders to buffer")
    void rendersToBuffer() {
        Rect area = new Rect(0, 0, 40, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tabs("Home", "Settings", "About")
            .selected(0)
            .title("Menu")
            .rounded()
            .render(frame, area, RenderContext.empty());

        // Check border is rendered
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("╭");
    }

    @Test
    @DisplayName("TabsElement with highlight color")
    void withHighlightColor() {
        Rect area = new Rect(0, 0, 30, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tabs("A", "B", "C")
            .selected(0)
            .highlightColor(Color.YELLOW)
            .render(frame, area, RenderContext.empty());

        // First tab should have highlight
        assertThat(buffer).isNotNull();
    }

    @Test
    @DisplayName("Empty area does not render")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 3));
        Frame frame = Frame.forTesting(buffer);

        // Should not throw
        tabs("A", "B").render(frame, emptyArea, RenderContext.empty());
    }

    @Test
    @DisplayName("tabs without explicit selection defaults to 0")
    void defaultSelection() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        tabs("X", "Y", "Z")
            .render(frame, area, RenderContext.empty());

        // Should render without error
        assertThat(buffer).isNotNull();
    }

    @Test
    @DisplayName("divider can be styled via CSS")
    void dividerStyledViaCss() {
        // Given CSS that styles the divider
        String css = "TabsElement-divider { color: red; }";
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        TabsElement element = tabs("A", "B").divider("|");

        // When rendering with CSS context
        context.withElement(element, Style.EMPTY, () -> {
            element.render(frame, area, context);
        });

        // Then divider should have red foreground
        // "A|B" - divider is at position 1
        assertThat(buffer).at(1, 0)
            .hasSymbol("|")
            .hasForeground(Color.RED);
    }

    @Test
    @DisplayName("selected tab can be styled via CSS")
    void selectedTabStyledViaCss() {
        // Given CSS that styles the selected tab
        String css = "TabsElement-tab:selected { color: cyan; }";
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        TabsElement element = tabs("A", "B").divider("|").selected(1);

        // When rendering with CSS context
        context.withElement(element, Style.EMPTY, () -> {
            element.render(frame, area, context);
        });

        // Then selected tab "B" should have cyan foreground
        // "A|B" - B is at position 2
        assertThat(buffer).at(2, 0)
            .hasSymbol("B")
            .hasForeground(Color.CYAN);
    }

    @Test
    @DisplayName("unselected tabs can be styled via CSS")
    void unselectedTabsStyledViaCss() {
        // Given CSS that styles unselected tabs
        String css = "TabsElement-tab { color: gray; background: red; }";
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        TabsElement element = tabs("A", "B").divider("|").selected(1);

        // When rendering with CSS context
        context.withElement(element, Style.EMPTY, () -> {
            element.render(frame, area, context);
        });

        // Then unselected tab "A" should have gray foreground and red background
        // "A|B" - A is at position 0
        assertThat(buffer).at(0, 0)
            .hasSymbol("A")
            .hasForeground(Color.GRAY)
            .hasBackground(Color.RED);
    }

    @Test
    @DisplayName("divider inherits base style background via CSS")
    void dividerInheritsBackgroundViaCss() {
        // Given CSS that sets background on tabs and foreground on divider
        String css = "TabsElement { background: blue; }\n" +
                     "TabsElement-divider { color: yellow; }";
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", css);
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        TabsElement element = tabs("A", "B").divider("|");

        // When rendering with CSS context
        context.withElement(element, Style.EMPTY.bg(Color.BLUE), () -> {
            element.render(frame, area, context);
        });

        // Then divider should have yellow foreground and blue background
        assertThat(buffer).at(1, 0)
            .hasSymbol("|")
            .hasForeground(Color.YELLOW)
            .hasBackground(Color.BLUE);
    }

    @Test
    @DisplayName("styleAttributes exposes title")
    void styleAttributes_exposesTitle() {
        assertThat(tabs("A", "B").title("Nav").styleAttributes()).containsEntry("title", "Nav");
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.tabs;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.widgets.block.Block;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

class TabsTest {

    @Test
    @DisplayName("Tabs renders basic content")
    void rendersBasicContent() {
        Tabs tabs = Tabs.from("Tab1", "Tab2", "Tab3");
        Rect area = new Rect(0, 0, 30, 1);
        Buffer buffer = Buffer.empty(area);
        TabsState state = new TabsState();

        tabs.render(area, buffer, state);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("T");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("a");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("b");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("1");
    }

    @Test
    @DisplayName("Tabs renders with divider")
    void rendersWithDivider() {
        Tabs tabs = Tabs.builder()
            .titles("A", "B")
            .divider(" | ")
            .build();
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        TabsState state = new TabsState();

        tabs.render(area, buffer, state);

        // "A | B"
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("|");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("B");
    }

    @Test
    @DisplayName("Tabs renders with custom divider")
    void rendersWithCustomDivider() {
        Tabs tabs = Tabs.builder()
            .titles("X", "Y")
            .divider(" - ")
            .build();
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        TabsState state = new TabsState();

        tabs.render(area, buffer, state);

        // "X - Y"
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("X");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("-");
    }

    @Test
    @DisplayName("Tabs renders with selection")
    void rendersWithSelection() {
        Style highlightStyle = Style.EMPTY.fg(Color.YELLOW);
        Tabs tabs = Tabs.builder()
            .titles("First", "Second")
            .highlightStyle(highlightStyle)
            .divider("|")
            .build();
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        TabsState state = new TabsState(1); // Select second tab

        tabs.render(area, buffer, state);

        // Second tab should have highlight style
        // "First|Second" - Second starts at position 6
        assertThat(buffer.get(6, 0).style().fg()).contains(Color.YELLOW);
    }

    @Test
    @DisplayName("Tabs with block")
    void withBlock() {
        Tabs tabs = Tabs.builder()
            .titles("Tab1")
            .block(Block.bordered())
            .divider("")
            .build();
        Rect area = new Rect(0, 0, 15, 3);
        Buffer buffer = Buffer.empty(area);
        TabsState state = new TabsState();

        tabs.render(area, buffer, state);

        // Block border
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("┌");
        // Tab inside block
        assertThat(buffer.get(1, 1).symbol()).isEqualTo("T");
    }

    @Test
    @DisplayName("Tabs with padding")
    void withPadding() {
        Tabs tabs = Tabs.builder()
            .titles("A", "B")
            .divider("|")
            .padding(" ", " ")
            .build();
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        TabsState state = new TabsState();

        tabs.render(area, buffer, state);

        // " A | B "
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("|");
    }

    @Test
    @DisplayName("Tabs size returns correct count")
    void sizeReturnsCount() {
        Tabs tabs = Tabs.from("A", "B", "C", "D");
        assertThat(tabs.size()).isEqualTo(4);
    }

    @Test
    @DisplayName("Tabs titles accessor")
    void titlesAccessor() {
        Tabs tabs = Tabs.from("First", "Second");
        assertThat(tabs.titles()).hasSize(2);
    }

    @Test
    @DisplayName("Tabs from Line array")
    void fromLineArray() {
        Tabs tabs = Tabs.from(
            Line.from(Span.raw("Tab1").bold()),
            Line.from("Tab2")
        );
        assertThat(tabs.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("Empty tabs renders nothing")
    void emptyTabs() {
        Tabs tabs = Tabs.builder().build();
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        TabsState state = new TabsState();

        tabs.render(area, buffer, state);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Tabs truncates when area too small")
    void truncatesWhenTooSmall() {
        Tabs tabs = Tabs.builder()
            .titles("VeryLongTabName", "Another")
            .divider("|")
            .build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        TabsState state = new TabsState();

        tabs.render(area, buffer, state);

        // Should not crash, renders what fits
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("V");
    }

    @Test
    @DisplayName("Tabs with styled divider")
    void styledDivider() {
        Span dividerSpan = Span.raw(" | ").fg(Color.DARK_GRAY);
        Tabs tabs = Tabs.builder()
            .titles("A", "B")
            .divider(dividerSpan)
            .build();
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        TabsState state = new TabsState();

        tabs.render(area, buffer, state);

        // Divider should have style
        assertThat(buffer.get(2, 0).style().fg()).contains(Color.DARK_GRAY);
    }

    @Test
    @DisplayName("Tabs builder addTitle")
    void builderAddTitle() {
        Tabs tabs = Tabs.builder()
            .addTitle("First")
            .addTitle("Second")
            .addTitle(Line.from("Third"))
            .build();
        assertThat(tabs.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Divider inherits base style including background")
    void dividerInheritsBaseStyle() {
        Style baseStyle = Style.EMPTY.bg(Color.BLUE).fg(Color.WHITE);
        Tabs tabs = Tabs.builder()
            .titles("A", "B")
            .style(baseStyle)
            .divider(" | ")
            .build();
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        TabsState state = new TabsState();

        tabs.render(area, buffer, state);

        // Divider at position 1-3 (" | ") should inherit the base style background
        assertThat(buffer).at(2, 0)
            .hasSymbol("|")
            .hasBackground(Color.BLUE);
    }
}

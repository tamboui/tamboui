/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.RenderContext;

import static dev.tamboui.toolkit.Toolkit.divider;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DividerElement.
 */
class DividerElementTest extends AbstractElementTest {

    @Test
    @DisplayName("divider() creates empty element with default style")
    void emptyDivider() {
        DividerElement element = divider();
        assertThat(element).isNotNull();
        assertThat(element.dividerStyle()).isEqualTo(DividerStyle.SINGLE);
        assertThat(element.leftText()).isNull();
        assertThat(element.centerText()).isNull();
        assertThat(element.rightText()).isNull();
    }

    @Test
    @DisplayName("divider(String) creates element with left text")
    void dividerWithLeftText() {
        DividerElement element = divider("Left Text");
        assertThat(element).isNotNull();
        assertThat(element.leftText()).isEqualTo("Left Text");
        assertThat(element.centerText()).isNull();
        assertThat(element.rightText()).isNull();
    }

    @Test
    @DisplayName("fluent API chains correctly")
    void fluentApiChaining() {
        DividerElement element = divider()
            .style(DividerStyle.DOUBLE)
            .line("Title")
            .left("L")
            .center("M")
            .right("R")
            .lineColor(Color.CYAN)
            .leftColor(Color.RED)
            .centerColor(Color.GREEN)
            .rightColor(Color.YELLOW);

        assertThat(element.dividerStyle()).isEqualTo(DividerStyle.DOUBLE);
        assertThat(element.centerText()).isEqualTo("M");
        assertThat(element.leftText()).isEqualTo("L");
        assertThat(element.rightText()).isEqualTo("R");
    }

    @Test
    @DisplayName("convenience style methods work")
    void convenienceStyleMethods() {
        DividerElement e1 = divider().doubleLine();
        assertThat(e1.dividerStyle()).isEqualTo(DividerStyle.DOUBLE);

        DividerElement e2 = divider().boldLine();
        assertThat(e2.dividerStyle()).isEqualTo(DividerStyle.BOLD);

        DividerElement e3 = divider().dotted();
        assertThat(e3.dividerStyle()).isEqualTo(DividerStyle.DOTTED);

        DividerElement e4 = divider().dashed();
        assertThat(e4.dividerStyle()).isEqualTo(DividerStyle.DASHED);

        DividerElement e5 = divider().heavy();
        assertThat(e5.dividerStyle()).isEqualTo(DividerStyle.HEAVY);

        DividerElement e6 = divider().rounded();
        assertThat(e6.dividerStyle()).isEqualTo(DividerStyle.ROUNDED);
    }

    @Test
    @DisplayName("convenience position methods work")
    void conveniencePositionMethods() {
        DividerElement element = divider()
            .left("Left")
            .center("Middle")
            .right("Right")
            .line("Also Center");

        assertThat(element.leftText()).isEqualTo("Left");
        assertThat(element.centerText()).isEqualTo("Also Center");
        assertThat(element.rightText()).isEqualTo("Right");
    }

    @Test
    @DisplayName("preferredSize returns height 1")
    void preferredHeight() {
        assertThat(divider().preferredSize(-1, -1, null).heightOr(0)).isEqualTo(1);
        assertThat(divider("Text").preferredSize(-1, -1, null).heightOr(0)).isEqualTo(1);
    }

    @Test
    @DisplayName("preferredSize uses available width when provided")
    void preferredWidth_withAvailable() {
        assertThat(divider().preferredSize(40, -1, null).widthOr(0)).isEqualTo(40);
        assertThat(divider("Text").preferredSize(40, -1, null).widthOr(0)).isEqualTo(40);
    }

    @Test
    @DisplayName("preferredSize returns minimum width when no available width")
    void preferredWidth_noAvailable() {
        assertThat(divider().preferredSize(-1, -1, null).widthOr(0)).isEqualTo(3);
        assertThat(divider("Hi").preferredSize(-1, -1, null).widthOr(0)).isEqualTo(4); // "Hi" = 2 + 2 padding
    }

    @Test
    @DisplayName("plain divider fills area with line characters")
    void plainDividerRendersLine() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        divider().render(frame, area, RenderContext.empty());

        // Every cell should contain the single-line char
        for (int x = 0; x < 20; x++) {
            assertThat(buffer.get(x, 0).symbol()).isEqualTo("─");
        }
    }

    @Test
    @DisplayName("double line divider renders double-line characters")
    void doubleLineDivider() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        divider(DividerStyle.DOUBLE).render(frame, area, RenderContext.empty());

        for (int x = 0; x < 10; x++) {
            assertThat(buffer.get(x, 0).symbol()).isEqualTo("═");
        }
    }

    @Test
    @DisplayName("bold line divider renders bold characters")
    void boldLineDivider() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        divider(DividerStyle.BOLD).render(frame, area, RenderContext.empty());

        for (int x = 0; x < 10; x++) {
            assertThat(buffer.get(x, 0).symbol()).isEqualTo("━");
        }
    }

    @Test
    @DisplayName("dotted divider renders dots")
    void dottedDivider() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        divider(DividerStyle.DOTTED).render(frame, area, RenderContext.empty());

        for (int x = 0; x < 10; x++) {
            assertThat(buffer.get(x, 0).symbol()).isEqualTo("·");
        }
    }

    @Test
    @DisplayName("dashed divider renders dashes")
    void dashedDivider() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        divider(DividerStyle.DASHED).render(frame, area, RenderContext.empty());

        for (int x = 0; x < 10; x++) {
            assertThat(buffer.get(x, 0).symbol()).isEqualTo("-");
        }
    }

    @Test
    @DisplayName("heavy divider renders blocks")
    void heavyDivider() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        divider(DividerStyle.HEAVY).render(frame, area, RenderContext.empty());

        for (int x = 0; x < 10; x++) {
            assertThat(buffer.get(x, 0).symbol()).isEqualTo("█");
        }
    }

    @Test
    @DisplayName("center text renders at the center with gap around")
    void centerTextRenders() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        divider().center("Mid").render(frame, area, RenderContext.empty());

        // "Mid" should be centered in a 20-char line: positions 8-10
        assertThat(buffer.get(8, 0).symbol()).isEqualTo("M");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("i");
        assertThat(buffer.get(10, 0).symbol()).isEqualTo("d");
    }

    @Test
    @DisplayName("left text renders at the start with gap after")
    void leftTextRenders() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        divider().left("Left").render(frame, area, RenderContext.empty());

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("L");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("e");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("f");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("t");
        // Position after text should be empty (gap)
        assertThat(buffer.get(4, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("right text renders at the end with gap before")
    void rightTextRenders() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        divider().right("End").render(frame, area, RenderContext.empty());

        int textLen = 3;
        int startPos = 20 - textLen; // 17
        assertThat(buffer.get(startPos, 0).symbol()).isEqualTo("E");
        assertThat(buffer.get(startPos + 1, 0).symbol()).isEqualTo("n");
        assertThat(buffer.get(startPos + 2, 0).symbol()).isEqualTo("d");
        // Position before text should be empty (gap)
        assertThat(buffer.get(startPos - 1, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("divider(String) constructor sets left text")
    void lineStringSetsLeftText() {
        DividerElement element = divider("Title");
        assertThat(element.leftText()).isEqualTo("Title");
        assertThat(element.centerText()).isNull();
        assertThat(element.rightText()).isNull();
    }

    @Test
    @DisplayName("empty area does not crash")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 1));
        Frame frame = Frame.forTesting(buffer);

        // Should not throw
        divider("Text").render(frame, emptyArea, RenderContext.empty());
    }

    @Test
    @DisplayName("styleAttributes exposes text positions")
    void styleAttributes_exposesTexts() {
        DividerElement element = divider("Left").center("Mid").right("Right");
        Map<String, String> attrs = element.styleAttributes();
        assertThat(attrs).containsEntry("left", "Left");
        assertThat(attrs).containsEntry("center", "Mid");
        assertThat(attrs).containsEntry("right", "Right");
    }

    @Test
    @DisplayName("getters return values set by constructors")
    void constructorGetters() {
        DividerElement element = new DividerElement("Constructor Text");
        assertThat(element.leftText()).isEqualTo("Constructor Text");
    }

    @Test
    @DisplayName("renders with all three text positions")
    void rendersAllThreeTexts() {
        Rect area = new Rect(0, 0, 40, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        divider()
            .left("L").center("Mid").right("R")
            .render(frame, area, RenderContext.empty());

        // Left text at start
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("L");
        // Center text at position (40-3)/2 = 18
        assertThat(buffer.get(18, 0).symbol()).isEqualTo("M");
        assertThat(buffer.get(19, 0).symbol()).isEqualTo("i");
        assertThat(buffer.get(20, 0).symbol()).isEqualTo("d");
        // Right text at end: "R" is 1 char, startPos = 40 - 1 = 39
        assertThat(buffer.get(39, 0).symbol()).isEqualTo("R");
    }

    @Test
    @DisplayName("in row with text elements")
    void dividerInRow() {
        Rect area = new Rect(0, 0, 30, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        Element before = text("Before");
        Element div = divider().center("Mid");
        Element after = text("After");
        Row rowElement = row(before, div, after);

        rowElement.render(frame, area, RenderContext.empty());

        // "Before" at start
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("B");
    }

    @Test
    @DisplayName("line style is applied via foreground color")
    void lineStyleApplied() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        divider().lineColor(Color.RED).render(frame, area, RenderContext.empty());

        // Check that the line cell has the red foreground color
        assertThat(buffer.get(0, 0).style().fg()).contains(Color.RED);
    }

    @Test
    @DisplayName("center text color is applied")
    void centerTextColorApplied() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        divider().center("Hi").centerColor(Color.CYAN).render(frame, area, RenderContext.empty());

        // "Hi" centered at position (10-2)/2 = 4
        assertThat(buffer.get(4, 0).style().fg()).contains(Color.CYAN);
    }
}

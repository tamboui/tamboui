/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ScrollableElement}.
 */
class ScrollableElementTest extends AbstractElementTest {

    @Test
    @DisplayName("renders children within area")
    void rendersChildren() {
        // 10 wide, 3 tall — room for 3 items; last column is scrollbar
        Rect area = new Rect(0, 0, 10, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        scrollable(
            text("AAA").length(1),
            text("BBB").length(1),
            text("CCC").length(1)
        ).render(frame, area, RenderContext.empty());

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("B");
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("C");
    }

    @Test
    @DisplayName("all content fits — no scrolling needed")
    void allContentFitsNoScrolling() {
        ScrollableElement element = scrollable(
            text("A").length(1),
            text("B").length(1)
        );

        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        // Position should stay at 0 when all content fits
        assertThat(element.state().position()).isEqualTo(0);
    }

    @Test
    @DisplayName("scroll position advances on down key")
    void scrollPositionAdvancesOnDown() {
        ScrollableElement element = scrollable(
            text("A").length(1),
            text("B").length(1),
            text("C").length(1),
            text("D").length(1),
            text("E").length(1)
        );

        // Render first to initialize state
        Rect area = new Rect(0, 0, 10, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        assertThat(element.state().position()).isEqualTo(0);

        // Simulate down key
        element.handleKeyEvent(KeyEvent.ofKey(KeyCode.DOWN), true);
        assertThat(element.state().position()).isEqualTo(1);
    }

    @Test
    @DisplayName("scroll position decreases on up key")
    void scrollPositionDecreasesOnUp() {
        ScrollableElement element = scrollable(
            text("A").length(1),
            text("B").length(1),
            text("C").length(1),
            text("D").length(1),
            text("E").length(1)
        );

        // Render to initialize
        Rect area = new Rect(0, 0, 10, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        // Go down then back up
        element.handleKeyEvent(KeyEvent.ofKey(KeyCode.DOWN), true);
        element.handleKeyEvent(KeyEvent.ofKey(KeyCode.DOWN), true);
        assertThat(element.state().position()).isEqualTo(2);

        element.handleKeyEvent(KeyEvent.ofKey(KeyCode.UP), true);
        assertThat(element.state().position()).isEqualTo(1);
    }

    @Test
    @DisplayName("scroll wheel down advances position")
    void scrollWheelDownAdvancesPosition() {
        ScrollableElement element = scrollable(
            text("A").length(1),
            text("B").length(1),
            text("C").length(1),
            text("D").length(1)
        );

        Rect area = new Rect(0, 0, 10, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        element.handleMouseEvent(MouseEvent.scrollDown(0, 0));
        assertThat(element.state().position()).isEqualTo(1);
    }

    @Test
    @DisplayName("scroll wheel up decreases position")
    void scrollWheelUpDecreasesPosition() {
        ScrollableElement element = scrollable(
            text("A").length(1),
            text("B").length(1),
            text("C").length(1),
            text("D").length(1)
        );

        Rect area = new Rect(0, 0, 10, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        element.handleMouseEvent(MouseEvent.scrollDown(0, 0));
        element.handleMouseEvent(MouseEvent.scrollDown(0, 0));
        assertThat(element.state().position()).isEqualTo(2);

        element.handleMouseEvent(MouseEvent.scrollUp(0, 0));
        assertThat(element.state().position()).isEqualTo(1);
    }

    @Test
    @DisplayName("up indicator shown when scrolled down")
    void upIndicatorShownWhenScrolledDown() {
        ScrollableElement element = scrollable(
            text("A").length(1),
            text("B").length(1),
            text("C").length(1),
            text("D").length(1)
        ).scrollUpIndicator(text("^^^").length(1));

        Rect area = new Rect(0, 0, 10, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        // Render first to initialize content length, then scroll down
        element.render(frame, area, RenderContext.empty());
        element.state().next();

        // Re-render with scrolled position
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        // Top row should have the indicator
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("^");
    }

    @Test
    @DisplayName("up indicator not shown at top")
    void upIndicatorNotShownAtTop() {
        ScrollableElement element = scrollable(
            text("A").length(1),
            text("B").length(1),
            text("C").length(1)
        ).scrollUpIndicator(text("^^^").length(1));

        Rect area = new Rect(0, 0, 10, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        // At top, should show content not indicator
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
    }

    @Test
    @DisplayName("down indicator shown when more content below")
    void downIndicatorShownWhenMoreBelow() {
        ScrollableElement element = scrollable(
            text("A").length(1),
            text("B").length(1),
            text("C").length(1),
            text("D").length(1)
        ).scrollDownIndicator(text("vvv").length(1));

        Rect area = new Rect(0, 0, 10, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        element.render(frame, area, RenderContext.empty());

        // Bottom row should have the indicator
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("v");
    }

    @Test
    @DisplayName("empty area does not throw")
    void emptyAreaDoesNotThrow() {
        Rect area = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 1, 1));
        Frame frame = Frame.forTesting(buffer);

        scrollable(text("A")).render(frame, area, RenderContext.empty());
    }

    @Test
    @DisplayName("empty children does not throw")
    void emptyChildrenDoesNotThrow() {
        Rect area = new Rect(0, 0, 10, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        scrollable().render(frame, area, RenderContext.empty());
    }

    @Test
    @DisplayName("state() returns the scrollbar state")
    void stateReturnsScrollbarState() {
        ScrollableElement element = scrollable(text("A"), text("B"));
        assertThat(element.state()).isNotNull();
    }

    @Test
    @DisplayName("preferredSize accounts for scrollbar width")
    void preferredSizeAccountsForScrollbar() {
        ScrollableElement element = scrollable(
            text("Hello").length(1),
            text("World").length(1)
        );

        // "Hello" = 5 chars, + 1 for scrollbar = 6
        assertThat(element.preferredSize(20, 10, null).widthOr(0)).isEqualTo(6);
        // 2 children of height 1 each
        assertThat(element.preferredSize(20, 10, null).heightOr(0)).isEqualTo(2);
    }
}

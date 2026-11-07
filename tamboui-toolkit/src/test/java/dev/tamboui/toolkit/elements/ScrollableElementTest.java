/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.event.EventRouter;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.widgets.form.BooleanFieldState;
import dev.tamboui.widgets.form.SelectFieldState;

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

    @Test
    @DisplayName("Down and Up keys navigate focus between focusable children")
    void focusNavigationBetweenFocusableChildren() {
        FocusManager focusManager = new FocusManager();
        ElementRegistry registry = new ElementRegistry();
        EventRouter router = new EventRouter(focusManager, registry);
        DefaultRenderContext context = new DefaultRenderContext(focusManager, router);

        FormFieldElement f1 = formField("Field 1", new BooleanFieldState()).id("f1").length(1);
        FormFieldElement f2 = formField("Field 2", new BooleanFieldState()).id("f2").length(1);
        FormFieldElement f3 = formField("Field 3", new BooleanFieldState()).id("f3").length(1);

        ScrollableElement scrollable = scrollable(f1, f2, f3);

        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        scrollable.render(frame, area, context);

        // Initially f1 is auto-focused
        assertThat(focusManager.focusedId()).isEqualTo("f1");

        // Down key moves focus to f2
        EventResult result = router.route(KeyEvent.ofKey(KeyCode.DOWN));
        assertThat(result.isHandled()).isTrue();
        assertThat(focusManager.focusedId()).isEqualTo("f2");

        // Down key moves focus to f3
        result = router.route(KeyEvent.ofKey(KeyCode.DOWN));
        assertThat(result.isHandled()).isTrue();
        assertThat(focusManager.focusedId()).isEqualTo("f3");

        // Up key moves focus back to f2
        result = router.route(KeyEvent.ofKey(KeyCode.UP));
        assertThat(result.isHandled()).isTrue();
        assertThat(focusManager.focusedId()).isEqualTo("f2");
    }

    @Test
    @DisplayName("viewport auto-scrolls to keep focused child visible")
    void autoScrollsWhenFocusMovesOutsideViewport() {
        FocusManager focusManager = new FocusManager();
        ElementRegistry registry = new ElementRegistry();
        EventRouter router = new EventRouter(focusManager, registry);
        DefaultRenderContext context = new DefaultRenderContext(focusManager, router);

        FormFieldElement[] fields = new FormFieldElement[6];
        for (int i = 0; i < 6; i++) {
            fields[i] = formField("Row " + i, new BooleanFieldState()).id("row-" + i).length(1);
        }

        ScrollableElement scrollable = scrollable(fields);

        // Viewport height 3: can only fit 3 items at a time
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        scrollable.render(frame, area, context);
        assertThat(focusManager.focusedId()).isEqualTo("row-0");
        assertThat(scrollable.state().position()).isEqualTo(0);

        // Navigate down to row-3 (off-screen)
        router.route(KeyEvent.ofKey(KeyCode.DOWN)); // to row-1
        router.route(KeyEvent.ofKey(KeyCode.DOWN)); // to row-2
        router.route(KeyEvent.ofKey(KeyCode.DOWN)); // to row-3 (off-screen)
        assertThat(focusManager.focusedId()).isEqualTo("row-3");

        // Re-render: scrollable should auto-scroll to keep row-3 visible
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
        scrollable.render(frame, area, context);

        // Position should have scrolled so row-3 is visible
        assertThat(scrollable.state().position()).isGreaterThan(0);
        boolean row3Rendered = false;
        for (int y = 0; y < 3; y++) {
            StringBuilder line = new StringBuilder();
            for (int x = 0; x < 20; x++) {
                line.append(buffer.get(x, y).symbol());
            }
            if (line.toString().contains("Row 3")) {
                row3Rendered = true;
                break;
            }
        }
        assertThat(row3Rendered).isTrue();

        // Now navigate back up to row-0
        router.route(KeyEvent.ofKey(KeyCode.UP)); // to row-2
        router.route(KeyEvent.ofKey(KeyCode.UP)); // to row-1
        router.route(KeyEvent.ofKey(KeyCode.UP)); // to row-0
        assertThat(focusManager.focusedId()).isEqualTo("row-0");

        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
        scrollable.render(frame, area, context);

        assertThat(scrollable.state().position()).isEqualTo(0);
        boolean row0Rendered = false;
        for (int y = 0; y < 3; y++) {
            StringBuilder line = new StringBuilder();
            for (int x = 0; x < 20; x++) {
                line.append(buffer.get(x, y).symbol());
            }
            if (line.toString().contains("Row 0")) {
                row0Rendered = true;
                break;
            }
        }
        assertThat(row0Rendered).isTrue();
    }

    @Test
    @DisplayName("Tab navigation moves across all off-screen children and auto-scrolls")
    void tabNavigationAcrossOffScreenChildren() {
        FocusManager focusManager = new FocusManager();
        ElementRegistry registry = new ElementRegistry();
        EventRouter router = new EventRouter(focusManager, registry);
        DefaultRenderContext context = new DefaultRenderContext(focusManager, router);

        FormFieldElement[] fields = new FormFieldElement[5];
        for (int i = 0; i < 5; i++) {
            fields[i] = formField("Item " + i, new BooleanFieldState()).id("item-" + i).length(1);
        }

        ScrollableElement scrollable = scrollable(fields);

        // Viewport height 2
        Rect area = new Rect(0, 0, 20, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        scrollable.render(frame, area, context);

        // Focus order must contain all 5 elements, even though only 2 are visible
        assertThat(focusManager.focusOrder()).containsExactly("item-0", "item-1", "item-2", "item-3", "item-4");

        // Tab through to item-3
        router.route(KeyEvent.ofKey(KeyCode.TAB));
        router.route(KeyEvent.ofKey(KeyCode.TAB));
        router.route(KeyEvent.ofKey(KeyCode.TAB));
        assertThat(focusManager.focusedId()).isEqualTo("item-3");

        // Re-render: viewport auto-scrolls to item-3
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
        scrollable.render(frame, area, context);

        assertThat(scrollable.state().position()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("scroll wheel and Page keys scroll viewport without moving focus or snapping back")
    void scrollWheelAndPageKeysScrollIndependently() {
        FocusManager focusManager = new FocusManager();
        ElementRegistry registry = new ElementRegistry();
        EventRouter router = new EventRouter(focusManager, registry);
        DefaultRenderContext context = new DefaultRenderContext(focusManager, router);

        FormFieldElement[] fields = new FormFieldElement[10];
        for (int i = 0; i < 10; i++) {
            fields[i] = formField("Row " + i, new BooleanFieldState()).id("r-" + i).length(1);
        }

        ScrollableElement scrollable = scrollable(fields);

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        scrollable.render(frame, area, context);
        assertThat(focusManager.focusedId()).isEqualTo("r-0");
        assertThat(scrollable.state().position()).isEqualTo(0);

        // Scroll down via mouse wheel
        scrollable.handleMouseEvent(MouseEvent.scrollDown(5, 1));
        assertThat(scrollable.state().position()).isEqualTo(1);
        assertThat(focusManager.focusedId()).isEqualTo("r-0"); // focus hasn't changed

        // Render again: viewport should NOT snap back to r-0
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
        scrollable.render(frame, area, context);
        assertThat(scrollable.state().position()).isEqualTo(1);

        // Page Down via key
        scrollable.handleKeyEvent(KeyEvent.ofKey(KeyCode.PAGE_DOWN), true);
        assertThat(scrollable.state().position()).isGreaterThan(1);
        assertThat(focusManager.focusedId()).isEqualTo("r-0");

        // Render again: viewport should still not snap back
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
        scrollable.render(frame, area, context);
        assertThat(scrollable.state().position()).isGreaterThan(1);
    }

    @Test
    @DisplayName("child handling arrow keys takes precedence over container navigation")
    void childHandlesArrowKeyDirectly() {
        FocusManager focusManager = new FocusManager();
        ElementRegistry registry = new ElementRegistry();
        EventRouter router = new EventRouter(focusManager, registry);
        DefaultRenderContext context = new DefaultRenderContext(focusManager, router);

        SelectFieldState selectState = new SelectFieldState(Arrays.asList("Opt A", "Opt B", "Opt C"));
        FormFieldElement selectField = formField("Choice", selectState).id("sel").length(1);
        FormFieldElement nextField = formField("Other", new BooleanFieldState()).id("other").length(1);

        ScrollableElement scrollable = scrollable(selectField, nextField);

        Rect area = new Rect(0, 0, 20, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        scrollable.render(frame, area, context);
        assertThat(focusManager.focusedId()).isEqualTo("sel");
        assertThat(selectState.selectedIndex()).isEqualTo(0);

        // Down key should change selection in the select field, NOT navigate focus
        EventResult result = router.route(KeyEvent.ofKey(KeyCode.DOWN));
        assertThat(result.isHandled()).isTrue();
        assertThat(focusManager.focusedId()).isEqualTo("sel");
        assertThat(selectState.selectedIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("scrollIntoView scrolls by index and by element")
    void scrollIntoViewMethods() {
        ScrollableElement scrollable = scrollable(
            text("0").length(1),
            text("1").length(1),
            text("2").length(1),
            text("3").length(1),
            text("4").length(1),
            text("5").length(1)
        );

        Rect area = new Rect(0, 0, 10, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        scrollable.render(frame, area, RenderContext.empty());

        assertThat(scrollable.state().position()).isEqualTo(0);

        // scrollIntoView index 4
        scrollable.scrollIntoView(4);
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
        scrollable.render(frame, area, RenderContext.empty());

        assertThat(scrollable.state().position()).isGreaterThan(0);

        // scrollIntoView index 0
        scrollable.scrollIntoView(0);
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
        scrollable.render(frame, area, RenderContext.empty());

        assertThat(scrollable.state().position()).isEqualTo(0);
    }

    @Test
    @DisplayName("nested focusable children inside containers are detected and auto-scrolled")
    void nestedFocusableChildrenInsideRows() {
        FocusManager focusManager = new FocusManager();
        ElementRegistry registry = new ElementRegistry();
        EventRouter router = new EventRouter(focusManager, registry);
        DefaultRenderContext context = new DefaultRenderContext(focusManager, router);

        ScrollableElement scrollable = scrollable(
            row(text("Label 0: "), formField("F0", new BooleanFieldState()).id("f0")).length(1),
            row(text("Label 1: "), formField("F1", new BooleanFieldState()).id("f1")).length(1),
            row(text("Label 2: "), formField("F2", new BooleanFieldState()).id("f2")).length(1),
            row(text("Label 3: "), formField("F3", new BooleanFieldState()).id("f3")).length(1)
        );

        Rect area = new Rect(0, 0, 30, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        scrollable.render(frame, area, context);

        assertThat(focusManager.focusedId()).isEqualTo("f0");
        assertThat(scrollable.state().position()).isEqualTo(0);

        // Navigate down to f2 (off-screen)
        router.route(KeyEvent.ofKey(KeyCode.DOWN));
        router.route(KeyEvent.ofKey(KeyCode.DOWN));
        assertThat(focusManager.focusedId()).isEqualTo("f2");

        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
        scrollable.render(frame, area, context);

        assertThat(scrollable.state().position()).isGreaterThan(0);
    }

    @Test
    @DisplayName("scrollable wrapped in panel works with formField checkboxes like in demo")
    void demoPatternWithWrappedPanel() {
        FocusManager focusManager = new FocusManager();
        ElementRegistry registry = new ElementRegistry();
        EventRouter router = new EventRouter(focusManager, registry);
        DefaultRenderContext context = new DefaultRenderContext(focusManager, router);

        BooleanFieldState[] states = new BooleanFieldState[20];
        ScrollableElement scrollable = scrollable(
            IntStream.range(0, 20)
                .mapToObj(i -> {
                    states[i] = new BooleanFieldState();
                    return formField("Row " + i, states[i]).id("row-" + i).length(1);
                })
                .toArray(Element[]::new)
        );

        Panel panel = panel(scrollable)
            .id("root")
            .focusable();

        Rect area = new Rect(0, 0, 30, 4);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        panel.render(frame, area, context);

        // Initially first focusable child (row-0) inside panel is focused
        assertThat(focusManager.focusedId()).isEqualTo("row-0");

        // Space key toggles row-0 checkbox
        assertThat(states[0].value()).isFalse();
        router.route(KeyEvent.ofChar(' '));
        assertThat(states[0].value()).isTrue();

        // Down key moves to row-1
        router.route(KeyEvent.ofKey(KeyCode.DOWN));
        assertThat(focusManager.focusedId()).isEqualTo("row-1");

        // Navigate down past viewport to row-10
        for (int i = 2; i <= 10; i++) {
            router.route(KeyEvent.ofKey(KeyCode.DOWN));
        }
        assertThat(focusManager.focusedId()).isEqualTo("row-10");

        // Re-render: scrollable must auto-scroll so row-10 is visible
        buffer = Buffer.empty(area);
        frame = Frame.forTesting(buffer);
        panel.render(frame, area, context);

        assertThat(scrollable.state().position()).isGreaterThan(0);
        assertThat(states[10].value()).isFalse();
        router.route(KeyEvent.ofChar(' '));
        assertThat(states[10].value()).isTrue();
    }
}

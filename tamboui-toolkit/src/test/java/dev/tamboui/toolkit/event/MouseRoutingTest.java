/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.bindings.MouseTrigger;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.widgets.input.TextInputState;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.lazy;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.stack;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.textInput;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mouse events routed to a rendered element tree: the element under the pointer that is drawn on
 * top gets the event first, each element gets it once, and every kind of mouse event is routed.
 */
class MouseRoutingTest extends AbstractElementTest {

    private DefaultRenderContext context;

    @BeforeEach
    void setUp() {
        context = DefaultRenderContext.createEmpty();
    }

    @Test
    @DisplayName("Clicking an input inside a focusable panel focuses the input")
    void clickFocusesChildInsideFocusableContainer() {
        render(column(panel(textInput(new TextInputState("hello")).id("in")).id("p").focusable()), 20, 5);
        context.focusManager().setFocus("p");

        route(MouseEvent.press(MouseButton.LEFT, 2, 1));

        assertThat(context.focusManager().focusedId()).isEqualTo("in");
    }

    @Test
    @DisplayName("Clicking a focusable child with a click handler runs the handler once and focuses the child")
    void clickRunsHandlerAndFocusesClickedChild() {
        int[] clicks = new int[1];
        render(column(
            text("A").id("a").focusable(),
            text("B").id("b").focusable().on(MouseTrigger.click(), e -> clicks[0]++)), 10, 2);
        context.focusManager().setFocus("a");

        route(MouseEvent.press(MouseButton.LEFT, 0, 1));

        assertThat(clicks[0]).isEqualTo(1);
        assertThat(context.focusManager().focusedId()).isEqualTo("b");
    }

    @Test
    @DisplayName("Clicking a clickable text inside a focusable panel focuses the panel")
    void clickOnNonFocusableChildFocusesNearestFocusableAncestor() {
        int[] clicks = new int[1];
        render(column(
            panel(text("t").on(MouseTrigger.click(), e -> clicks[0]++)).id("p").focusable().length(3),
            text("other").id("o").focusable()), 20, 4);
        context.focusManager().setFocus("o");

        route(MouseEvent.press(MouseButton.LEFT, 1, 1));

        assertThat(clicks[0]).isEqualTo(1);
        assertThat(context.focusManager().focusedId()).isEqualTo("p");
    }

    @Test
    @DisplayName("In a stack, a click goes to the top layer")
    void clickGoesToTopLayerOfStack() {
        int[] base = new int[1];
        int[] overlay = new int[1];
        render(stack(
            text("base").on(MouseTrigger.click(), e -> base[0]++),
            text("overlay").on(MouseTrigger.click(), e -> overlay[0]++)), 10, 1);

        route(MouseEvent.press(MouseButton.LEFT, 0, 0));

        assertThat(overlay[0]).isEqualTo(1);
        assertThat(base[0]).isZero();
    }

    @Test
    @DisplayName("In a stack, a click the top layer ignores reaches the layer below")
    void clickIgnoredByTopLayerReachesLayerBelow() {
        int[] base = new int[1];
        render(stack(text("base").on(MouseTrigger.click(), e -> base[0]++), text("overlay")), 10, 1);

        route(MouseEvent.press(MouseButton.LEFT, 0, 0));

        assertThat(base[0]).isEqualTo(1);
    }

    @Test
    @DisplayName("One mouse event runs the handler of a nested element once")
    void oneMouseEventRunsHandlerOnce() {
        int[] calls = new int[1];
        render(column(row(text("k").id("k").focusable().onMouseEvent(e -> {
            calls[0]++;
            return EventResult.UNHANDLED;
        }))), 10, 1);

        route(MouseEvent.move(0, 0));

        assertThat(calls[0]).isEqualTo(1);
    }

    @Test
    @DisplayName("One mouse event runs the handler of a lazily supplied element once")
    void oneMouseEventRunsLazyElementHandlerOnce() {
        int[] calls = new int[1];
        render(column(lazy(() -> text("z").onMouseEvent(e -> {
            calls[0]++;
            return EventResult.UNHANDLED;
        }))), 10, 1);

        route(MouseEvent.move(0, 0));

        assertThat(calls[0]).isEqualTo(1);
    }

    @Test
    @DisplayName("Right click, button release and horizontal scroll reach the element under the pointer")
    void otherMouseEventsReachElements() {
        int[] rightClicks = new int[1];
        int[] releases = new int[1];
        int[] scrolls = new int[1];
        render(column(text("X")
            .on(MouseTrigger.rightClick(), e -> rightClicks[0]++)
            .on(MouseTrigger.release(MouseButton.LEFT), e -> releases[0]++)
            .on(MouseTrigger.scrollLeft(), e -> scrolls[0]++)), 10, 1);

        assertThat(route(MouseEvent.press(MouseButton.RIGHT, 0, 0))).isEqualTo(EventResult.HANDLED);
        assertThat(route(MouseEvent.release(MouseButton.LEFT, 0, 0))).isEqualTo(EventResult.HANDLED);
        assertThat(route(MouseEvent.scrollLeft(0, 0))).isEqualTo(EventResult.HANDLED);

        assertThat(rightClicks[0]).isEqualTo(1);
        assertThat(releases[0]).isEqualTo(1);
        assertThat(scrolls[0]).isEqualTo(1);
    }

    private void render(Element root, int width, int height) {
        Rect area = new Rect(0, 0, width, height);
        root.render(Frame.forTesting(Buffer.empty(area)), area, context);
    }

    private EventResult route(MouseEvent event) {
        return context.eventRouter().route(event);
    }
}

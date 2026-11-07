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
import dev.tamboui.toolkit.component.Component;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.widgets.input.TextInputState;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.lazy;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.row;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.textInput;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Key events routed to a rendered element tree: one key runs an element's key handler once.
 */
class KeyRoutingTest extends AbstractElementTest {

    private DefaultRenderContext context;

    @BeforeEach
    void setUp() {
        context = DefaultRenderContext.createEmpty();
    }

    @Test
    @DisplayName("One key runs the handler of a focused nested element once")
    void oneKeyRunsFocusedHandlerOnce() {
        int[] calls = new int[1];
        render(column(row(text("k").id("k").focusable().onKeyEvent(e -> {
            calls[0]++;
            return EventResult.UNHANDLED;
        }))));
        context.focusManager().setFocus("k");

        route(KeyEvent.ofChar('x'));

        assertThat(calls[0]).isEqualTo(1);
    }

    @Test
    @DisplayName("One key runs the handler of an unfocused nested element once")
    void oneKeyRunsUnfocusedHandlerOnce() {
        int[] calls = new int[1];
        render(column(
            row(text("k").onKeyEvent(e -> {
                calls[0]++;
                return EventResult.UNHANDLED;
            })),
            text("f").id("f").focusable()));
        context.focusManager().setFocus("f");

        route(KeyEvent.ofChar('x'));

        assertThat(calls[0]).isEqualTo(1);
    }

    @Test
    @DisplayName("One key runs the handler of a focused lazily supplied element once")
    void oneKeyRunsLazyFocusedHandlerOnce() {
        int[] calls = new int[1];
        render(column(lazy(() -> text("z").id("z").focusable().onKeyEvent(e -> {
            calls[0]++;
            return EventResult.UNHANDLED;
        }))));
        context.focusManager().setFocus("z");

        route(KeyEvent.ofChar('x'));

        assertThat(calls[0]).isEqualTo(1);
    }

    @Test
    @DisplayName("A focused container still passes keys on to the input inside it")
    void focusedContainerPassesKeysToItsChildren() {
        TextInputState state = new TextInputState("");
        render(column(panel(textInput(state).id("in")).id("p").focusable()));
        context.focusManager().setFocus("p");

        route(KeyEvent.ofChar('z'));

        assertThat(state.text()).isEqualTo("z");
    }

    @Test
    @DisplayName("A key a focused text input does not use reaches its key handler once")
    void unusedKeyReachesTextInputHandler() {
        int[] calls = new int[1];
        TextInputState state = new TextInputState("");
        render(column(textInput(state).id("in").onKeyEvent(e -> {
            calls[0]++;
            return EventResult.UNHANDLED;
        })));
        context.focusManager().setFocus("in");

        route(KeyEvent.ofChar('a'));
        route(KeyEvent.ofKey(KeyCode.F5));

        assertThat(state.text()).isEqualTo("a");
        assertThat(calls[0]).isEqualTo(1);
    }

    @Test
    @DisplayName("A focused component runs its key handler once, an unfocused one not at all")
    void componentKeyHandlerRunsWhenFocused() {
        int[] calls = new int[1];
        CountingComponent component = new CountingComponent();
        component.id("c").onKeyEvent(e -> {
            calls[0]++;
            return EventResult.UNHANDLED;
        });
        render(column(component, text("f").id("f").focusable()));

        context.focusManager().setFocus("c");
        route(KeyEvent.ofChar('x'));
        context.focusManager().setFocus("f");
        route(KeyEvent.ofChar('y'));

        assertThat(calls[0]).isEqualTo(1);
    }

    private void render(Element root) {
        Rect area = new Rect(0, 0, 20, 5);
        root.render(Frame.forTesting(Buffer.empty(area)), area, context);
    }

    private EventResult route(KeyEvent event) {
        return context.eventRouter().route(event);
    }

    private static final class CountingComponent extends Component<CountingComponent> {
        @Override
        protected Element render() {
            return text("component");
        }
    }
}

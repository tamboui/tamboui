/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.PasteEvent;
import dev.tamboui.widgets.input.TextInputState;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.lazy;
import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;
import static dev.tamboui.toolkit.Toolkit.textInput;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Paste events routed to a rendered element tree: a paste goes where typed keys go.
 */
class PasteRoutingTest extends AbstractElementTest {

    private DefaultRenderContext context;

    @BeforeEach
    void setUp() {
        context = DefaultRenderContext.createEmpty();
    }

    @Test
    @DisplayName("A paste into a focused panel reaches the text input inside it, as typed keys do")
    void pasteReachesInputInFocusedPanel() {
        TextInputState state = new TextInputState("");
        render(panel(textInput(state)).id("main").focusable());
        context.focusManager().setFocus("main");

        route(KeyEvent.ofChar('a'));
        EventResult result = route(new PasteEvent("bc"));

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(state.text()).isEqualTo("abc");
    }

    @Test
    @DisplayName("A paste into a focused panel reaches a text input the panel supplies lazily")
    void pasteReachesLazyInputInFocusedPanel() {
        TextInputState state = new TextInputState("");
        render(panel(() -> textInput(state)).id("main").focusable());
        context.focusManager().setFocus("main");

        EventResult result = route(new PasteEvent("bc"));

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(state.text()).isEqualTo("bc");
    }

    @Test
    @DisplayName("A paste does not reach the text input of a panel that is not focused")
    void pasteSkipsInputOfUnfocusedPanel() {
        TextInputState other = new TextInputState("");
        TextInputState state = new TextInputState("");
        render(column(
            panel(textInput(other)).id("other").focusable(),
            panel(textInput(state)).id("main").focusable()));
        context.focusManager().setFocus("main");

        route(new PasteEvent("bc"));

        assertThat(other.text()).isEmpty();
        assertThat(state.text()).isEqualTo("bc");
    }

    @Test
    @DisplayName("A paste into a lazily supplied text input is inserted once")
    void pasteIntoLazyInputIsInsertedOnce() {
        TextInputState state = new TextInputState("");
        render(column(lazy(() -> textInput(state).id("in"))));
        context.focusManager().setFocus("in");

        route(new PasteEvent("bc"));

        assertThat(state.text()).isEqualTo("bc");
    }

    @Test
    @DisplayName("A paste the focused element does not take goes to the global handlers")
    void unhandledPasteReachesGlobalHandlers() {
        List<Event> seen = new ArrayList<>();
        render(column(text("items").id("list").focusable()));
        context.focusManager().setFocus("list");
        context.eventRouter().addGlobalHandler(event -> {
            seen.add(event);
            return EventResult.HANDLED;
        });

        EventResult result = route(new PasteEvent("bc"));

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(seen).containsExactly(new PasteEvent("bc"));
    }

    @Test
    @DisplayName("A paste the focused text input takes does not reach the global handlers")
    void handledPasteSkipsGlobalHandlers() {
        List<Event> seen = new ArrayList<>();
        TextInputState state = new TextInputState("");
        render(column(textInput(state).id("in")));
        context.focusManager().setFocus("in");
        context.eventRouter().addGlobalHandler(event -> {
            seen.add(event);
            return EventResult.HANDLED;
        });

        route(new PasteEvent("bc"));

        assertThat(state.text()).isEqualTo("bc");
        assertThat(seen).isEmpty();
    }

    private void render(Element root) {
        Rect area = new Rect(0, 0, 20, 5);
        root.render(Frame.forTesting(Buffer.empty(area)), area, context);
    }

    private EventResult route(Event event) {
        return context.eventRouter().route(event);
    }
}

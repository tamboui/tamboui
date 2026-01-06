/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings;

import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ActionHandlerTest {

    private Bindings bindings;
    private ActionHandler handler;

    @BeforeEach
    void setUp() {
        bindings = BindingSets.standard();
        handler = new ActionHandler(bindings);
    }

    @Test
    void onRegistersHandler() {
        List<Event> received = new ArrayList<>();
        handler.on("moveUp", received::add);

        assertThat(handler.hasHandlers("moveUp")).isTrue();
        assertThat(handler.hasHandlers("moveDown")).isFalse();
    }

    @Test
    void onIsChainable() {
        ActionHandler result = handler
                .on("moveUp", e -> {})
                .on("moveDown", e -> {});

        assertThat(result).isSameAs(handler);
    }

    @Test
    void offRemovesHandlers() {
        handler.on("moveUp", e -> {});
        assertThat(handler.hasHandlers("moveUp")).isTrue();

        handler.off("moveUp");
        assertThat(handler.hasHandlers("moveUp")).isFalse();
    }

    @Test
    void dispatchInvokesMatchingHandlers() {
        List<Event> received = new ArrayList<>();
        handler.on(Actions.MOVE_UP, received::add);

        KeyEvent upEvent = KeyEvent.ofKey(KeyCode.UP, bindings);
        boolean handled = handler.dispatch(upEvent);

        assertThat(handled).isTrue();
        assertThat(received).hasSize(1);
        assertThat(received.get(0)).isEqualTo(upEvent);
    }

    @Test
    void dispatchInvokesMultipleHandlersInOrder() {
        List<String> callOrder = new ArrayList<>();
        handler.on(Actions.MOVE_UP, e -> callOrder.add("first"));
        handler.on(Actions.MOVE_UP, e -> callOrder.add("second"));

        KeyEvent upEvent = KeyEvent.ofKey(KeyCode.UP, bindings);
        handler.dispatch(upEvent);

        assertThat(callOrder).containsExactly("first", "second");
    }

    @Test
    void dispatchReturnsFalseWhenNoHandlers() {
        KeyEvent upEvent = KeyEvent.ofKey(KeyCode.UP, bindings);
        boolean handled = handler.dispatch(upEvent);

        assertThat(handled).isFalse();
    }

    @Test
    void dispatchReturnsFalseWhenNoMatchingAction() {
        handler.on("customAction", e -> {});

        KeyEvent upEvent = KeyEvent.ofKey(KeyCode.UP, bindings);
        boolean handled = handler.dispatch(upEvent);

        assertThat(handled).isFalse();
    }

    @Test
    void setBindingsChangesBindingsAtRuntime() {
        handler.on(Actions.QUIT, e -> {});

        // With standard bindings, 'q' triggers quit
        Bindings standard = BindingSets.standard();
        KeyEvent qEvent = KeyEvent.ofChar('q', standard);
        handler.setBindings(standard);
        assertThat(handler.dispatch(qEvent)).isTrue();

        // With custom bindings that only have Ctrl+C for quit, 'q' doesn't trigger quit
        Bindings noQuitOnQ = DefaultBindings.builder()
                .bind(Actions.QUIT, KeyTrigger.ctrl('c'))
                .build();
        handler.setBindings(noQuitOnQ);
        KeyEvent qEventNoQuit = KeyEvent.ofChar('q', noQuitOnQ);
        assertThat(handler.dispatch(qEventNoQuit)).isFalse();
    }

    @Test
    void bindingsReturnsCurrentBindings() {
        assertThat(handler.bindings()).isEqualTo(bindings);

        Bindings vim = BindingSets.vim();
        handler.setBindings(vim);
        assertThat(handler.bindings()).isEqualTo(vim);
    }

    @Test
    void hasHandlersReturnsFalseForUnknownAction() {
        assertThat(handler.hasHandlers("unknownAction")).isFalse();
    }

    @Test
    void hasHandlersReturnsFalseAfterRemovingAllHandlers() {
        handler.on("test", e -> {});
        handler.off("test");

        assertThat(handler.hasHandlers("test")).isFalse();
    }

    @Test
    void dispatchWorksWithCustomActions() {
        // Create bindings with custom action
        Bindings custom = BindingSets.standard()
                .toBuilder()
                .bind("save", KeyTrigger.ctrl('s'))
                .build();
        handler.setBindings(custom);

        List<Event> received = new ArrayList<>();
        handler.on("save", received::add);

        KeyEvent ctrlS = KeyEvent.ofChar('s', KeyModifiers.CTRL, custom);
        boolean handled = handler.dispatch(ctrlS);

        assertThat(handled).isTrue();
        assertThat(received).hasSize(1);
    }
}

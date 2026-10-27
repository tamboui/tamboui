/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.tamboui.annotations.bindings.OnAction;
import dev.tamboui.tui.event.ActionEvent;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;

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
        handler.on("moveUp", e -> received.add(e));

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
        handler.on(Actions.MOVE_UP, e -> received.add(e));

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
    void dispatchFiresConfirmOnEnter() {
        // With the standard bindings Enter is both "select" and "confirm"
        List<Event> received = new ArrayList<>();
        handler.on(Actions.CONFIRM, e -> received.add(e));

        boolean handled = handler.dispatch(KeyEvent.ofKey(KeyCode.ENTER, bindings));

        assertThat(handled).isTrue();
        assertThat(received).hasSize(1);
    }

    @Test
    void dispatchFiresScrollUpOnWheelUp() {
        // The wheel is both "scroll" and "scrollUp"
        List<Event> received = new ArrayList<>();
        handler.on(Actions.SCROLL_UP, e -> received.add(e));

        boolean handled = handler.dispatch(MouseEvent.scrollUp(0, 0));

        assertThat(handled).isTrue();
        assertThat(received).hasSize(1);
    }

    @Test
    void dispatchFiresCustomActionBoundToAKeyThatIsAlreadyBound() {
        Bindings custom = bindings.toBuilder()
                .bind(KeyTrigger.key(KeyCode.ENTER), "submit")
                .build();
        ActionHandler customHandler = new ActionHandler(custom);
        List<Event> received = new ArrayList<>();
        customHandler.on("submit", e -> received.add(e));

        boolean handled = customHandler.dispatch(KeyEvent.ofKey(KeyCode.ENTER, custom));

        assertThat(handled).isTrue();
        assertThat(received).hasSize(1);
    }

    @Test
    void dispatchFiresConfirmOnEnterWhenBothConfirmAndSelectHaveHandlers() {
        // Enter matches both "confirm" and "select". Regardless of handler registration order,
        // the deterministic precedence policy must choose "confirm".
        List<String> fired1 = new ArrayList<>();
        ActionHandler handler1 = new ActionHandler(bindings)
                .on(Actions.SELECT, e -> fired1.add(Actions.SELECT))
                .on(Actions.CONFIRM, e -> fired1.add(Actions.CONFIRM));

        boolean handled1 = handler1.dispatch(KeyEvent.ofKey(KeyCode.ENTER, bindings));
        assertThat(handled1).isTrue();
        assertThat(fired1).containsExactly(Actions.CONFIRM);

        List<String> fired2 = new ArrayList<>();
        ActionHandler handler2 = new ActionHandler(bindings)
                .on(Actions.CONFIRM, e -> fired2.add(Actions.CONFIRM))
                .on(Actions.SELECT, e -> fired2.add(Actions.SELECT));

        boolean handled2 = handler2.dispatch(KeyEvent.ofKey(KeyCode.ENTER, bindings));
        assertThat(handled2).isTrue();
        assertThat(fired2).containsExactly(Actions.CONFIRM);
    }

    @Test
    void dispatchFiresSelectOnEnterWhenOnlySelectHasHandler() {
        List<String> fired = new ArrayList<>();
        handler.on(Actions.SELECT, e -> fired.add(Actions.SELECT));

        boolean handled = handler.dispatch(KeyEvent.ofKey(KeyCode.ENTER, bindings));
        assertThat(handled).isTrue();
        assertThat(fired).containsExactly(Actions.SELECT);
    }

    @Test
    void dispatchFiresFocusPreviousOnShiftTabWhenBothFocusHandlersRegistered() {
        // Shift+Tab matches "focusPrevious", and plain-Tab trigger must not match Shift+Tab.
        // Regardless of handler registration order, "focusPrevious" must be fired.
        List<String> fired1 = new ArrayList<>();
        ActionHandler handler1 = new ActionHandler(bindings)
                .on(Actions.FOCUS_NEXT, e -> fired1.add(Actions.FOCUS_NEXT))
                .on(Actions.FOCUS_PREVIOUS, e -> fired1.add(Actions.FOCUS_PREVIOUS));

        boolean handled1 = handler1.dispatch(KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT, bindings));
        assertThat(handled1).isTrue();
        assertThat(fired1).containsExactly(Actions.FOCUS_PREVIOUS);

        List<String> fired2 = new ArrayList<>();
        ActionHandler handler2 = new ActionHandler(bindings)
                .on(Actions.FOCUS_PREVIOUS, e -> fired2.add(Actions.FOCUS_PREVIOUS))
                .on(Actions.FOCUS_NEXT, e -> fired2.add(Actions.FOCUS_NEXT));

        boolean handled2 = handler2.dispatch(KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT, bindings));
        assertThat(handled2).isTrue();
        assertThat(fired2).containsExactly(Actions.FOCUS_PREVIOUS);
    }

    @Test
    void dispatchFiresFocusNextOnPlainTabWhenBothFocusHandlersRegistered() {
        List<String> fired = new ArrayList<>();
        handler.on(Actions.FOCUS_PREVIOUS, e -> fired.add(Actions.FOCUS_PREVIOUS))
               .on(Actions.FOCUS_NEXT, e -> fired.add(Actions.FOCUS_NEXT));

        boolean handled = handler.dispatch(KeyEvent.ofKey(KeyCode.TAB, bindings));
        assertThat(handled).isTrue();
        assertThat(fired).containsExactly(Actions.FOCUS_NEXT);
    }

    @Test
    void dispatchFiresCustomActionOverStandardBindingsOnEnter() {
        Bindings custom = bindings.toBuilder()
                .bind(KeyTrigger.key(KeyCode.ENTER), "submit")
                .build();
        ActionHandler customHandler = new ActionHandler(custom);
        List<String> fired = new ArrayList<>();
        customHandler.on(Actions.CONFIRM, e -> fired.add(Actions.CONFIRM));
        customHandler.on(Actions.SELECT, e -> fired.add(Actions.SELECT));
        customHandler.on("submit", e -> fired.add("submit"));

        boolean handled = customHandler.dispatch(KeyEvent.ofKey(KeyCode.ENTER, custom));
        assertThat(handled).isTrue();
        assertThat(fired).containsExactly("submit");
    }

    @Test
    void dispatchFiresReboundActionOverEarlierActionOnEnter() {
        Bindings custom = bindings.toBuilder()
                .rebind(KeyTrigger.key(KeyCode.ENTER), Actions.SELECT)
                .build();
        ActionHandler customHandler = new ActionHandler(custom);
        List<String> fired = new ArrayList<>();
        customHandler.on(Actions.CONFIRM, e -> fired.add(Actions.CONFIRM));
        customHandler.on(Actions.SELECT, e -> fired.add(Actions.SELECT));

        boolean handled = customHandler.dispatch(KeyEvent.ofKey(KeyCode.ENTER, custom));
        assertThat(handled).isTrue();
        assertThat(fired).containsExactly(Actions.SELECT);
    }

    @Test
    void dispatchTieBreaksCustomActionsByLatestBindingOrder() {
        Bindings custom = DefaultBindings.builder()
                .bind(KeyTrigger.key(KeyCode.ENTER), "firstAction")
                .bind(KeyTrigger.key(KeyCode.ENTER), "secondAction")
                .build();

        // Registration order 1: firstAction then secondAction -> secondAction wins (later bound)
        List<String> fired1 = new ArrayList<>();
        ActionHandler handler1 = new ActionHandler(custom)
                .on("firstAction", e -> fired1.add("firstAction"))
                .on("secondAction", e -> fired1.add("secondAction"));
        handler1.dispatch(KeyEvent.ofKey(KeyCode.ENTER, custom));
        assertThat(fired1).containsExactly("secondAction");

        // Registration order 2: secondAction then firstAction -> secondAction still wins
        List<String> fired2 = new ArrayList<>();
        ActionHandler handler2 = new ActionHandler(custom)
                .on("secondAction", e -> fired2.add("secondAction"))
                .on("firstAction", e -> fired2.add("firstAction"));
        handler2.dispatch(KeyEvent.ofKey(KeyCode.ENTER, custom));
        assertThat(fired2).containsExactly("secondAction");
    }

    @Test
    void dispatchPermissiveShiftMatchingWhenUnbound() {
        // Standard bindings only bind moveUp = Up. Shift+Up should match moveUp permissively.
        List<String> fired = new ArrayList<>();
        handler.on(Actions.MOVE_UP, e -> fired.add(Actions.MOVE_UP));

        boolean handled = handler.dispatch(KeyEvent.ofKey(KeyCode.UP, KeyModifiers.SHIFT, bindings));
        assertThat(handled).isTrue();
        assertThat(fired).containsExactly(Actions.MOVE_UP);
    }

    @Test
    void dispatchExactShiftMatchingBeatsPermissiveShiftMatching() {
        Bindings custom = bindings.toBuilder()
                .bind(KeyTrigger.key(KeyCode.UP, false, false, true), "selectUp")
                .build();
        ActionHandler customHandler = new ActionHandler(custom);
        List<String> fired = new ArrayList<>();
        customHandler.on(Actions.MOVE_UP, e -> fired.add(Actions.MOVE_UP));
        customHandler.on("selectUp", e -> fired.add("selectUp"));

        // Shift+Up should dispatch selectUp (exact modifier match beats permissive)
        boolean handledShift = customHandler.dispatch(KeyEvent.ofKey(KeyCode.UP, KeyModifiers.SHIFT, custom));
        assertThat(handledShift).isTrue();
        assertThat(fired).containsExactly("selectUp");

        // Plain Up should dispatch moveUp
        fired.clear();
        boolean handledPlain = customHandler.dispatch(KeyEvent.ofKey(KeyCode.UP, custom));
        assertThat(handledPlain).isTrue();
        assertThat(fired).containsExactly(Actions.MOVE_UP);
    }

    @Test
    void dispatchFiresScrollUpOverScrollWhenBothHaveHandlers() {
        List<String> fired = new ArrayList<>();
        handler.on(Actions.SCROLL, e -> fired.add(Actions.SCROLL));
        handler.on(Actions.SCROLL_UP, e -> fired.add(Actions.SCROLL_UP));

        boolean handled = handler.dispatch(MouseEvent.scrollUp(0, 0));
        assertThat(handled).isTrue();
        assertThat(fired).containsExactly(Actions.SCROLL_UP);
    }

    @Test
    void dispatchFiresScrollWhenOnlyScrollHasHandler() {
        List<String> fired = new ArrayList<>();
        handler.on(Actions.SCROLL, e -> fired.add(Actions.SCROLL));

        boolean handled = handler.dispatch(MouseEvent.scrollUp(0, 0));
        assertThat(handled).isTrue();
        assertThat(fired).containsExactly(Actions.SCROLL);
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
                .bind(KeyTrigger.ctrl('c'), Actions.QUIT)
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
                .bind(KeyTrigger.ctrl('s'), "save")
                .build();
        handler.setBindings(custom);

        List<Event> received = new ArrayList<>();
        handler.on("save", e -> received.add(e));

        KeyEvent ctrlS = KeyEvent.ofChar('s', KeyModifiers.CTRL, custom);
        boolean handled = handler.dispatch(ctrlS);

        assertThat(handled).isTrue();
        assertThat(received).hasSize(1);
    }

    @Test
    void onWithBiConsumerReceivesActionName() {
        List<String> receivedActions = new ArrayList<>();
        handler.on(Actions.MOVE_UP, (event, action) -> receivedActions.add(action));

        KeyEvent upEvent = KeyEvent.ofKey(KeyCode.UP, bindings);
        handler.dispatch(upEvent);

        assertThat(receivedActions).containsExactly(Actions.MOVE_UP);
    }

    @Test
    void onWithBiConsumerWorksForMultipleActions() {
        List<String> receivedActions = new ArrayList<>();
        handler.on(Actions.MOVE_UP, (event, action) -> receivedActions.add(action));
        handler.on(Actions.MOVE_DOWN, (event, action) -> receivedActions.add(action));

        handler.dispatch(KeyEvent.ofKey(KeyCode.UP, bindings));
        handler.dispatch(KeyEvent.ofKey(KeyCode.DOWN, bindings));

        assertThat(receivedActions).containsExactly(Actions.MOVE_UP, Actions.MOVE_DOWN);
    }

    @Test
    void fireInvokesHandlersByActionName() {
        List<Event> received = new ArrayList<>();
        handler.on("save", e -> received.add(e));

        // Use any event — fire bypasses bindings, so the event content doesn't matter
        KeyEvent event = KeyEvent.ofChar('x', bindings);
        boolean handled = handler.fire("save", event);

        assertThat(handled).isTrue();
        assertThat(received).hasSize(1);
        assertThat(received.get(0)).isEqualTo(event);
    }

    @Test
    void fireReturnsFalseWhenNoHandlers() {
        KeyEvent event = KeyEvent.ofChar('x', bindings);
        boolean handled = handler.fire("refresh", event);

        assertThat(handled).isFalse();
    }

    @Test
    void firePassesActionNameToBiConsumerHandler() {
        List<String> receivedActions = new ArrayList<>();
        handler.on("save", (event, action) -> receivedActions.add(action));

        handler.fire("save", KeyEvent.ofChar('x', bindings));

        assertThat(receivedActions).containsExactly("save");
    }

    @Test
    void fireInvokesMultipleHandlersInOrder() {
        List<String> callOrder = new ArrayList<>();
        handler.on("save", e -> callOrder.add("first"));
        handler.on("save", e -> callOrder.add("second"));

        handler.fire("save", KeyEvent.ofChar('x', bindings));

        assertThat(callOrder).containsExactly("first", "second");
    }

    @Test
    void fireWithoutEventCreatesActionEvent() {
        List<Event> received = new ArrayList<>();
        handler.on("save", e -> received.add(e));

        boolean handled = handler.fire("save");

        assertThat(handled).isTrue();
        assertThat(received).hasSize(1);
        assertThat(received.get(0)).isInstanceOf(ActionEvent.class);
        assertThat(((ActionEvent) received.get(0)).action()).isEqualTo("save");
    }

    @Test
    void fireDoesNotGoThroughBindings() {
        // "refresh" is not in bindings, but fire should still work
        handler.on("refresh", e -> {});

        // dispatch would fail (no binding for 'r' → "refresh")
        KeyEvent rEvent = KeyEvent.ofChar('r', bindings);
        assertThat(handler.dispatch(rEvent)).isFalse();

        // fire works because it bypasses bindings
        assertThat(handler.fire("refresh", rEvent)).isTrue();
    }

    @Test
    void registerAnnotatedCallsAMethodTakingAnEventSubtypeOnlyForEventsOfThatType() {
        KeyHandlers target = new KeyHandlers();
        handler.registerAnnotated(target);
        KeyEvent key = KeyEvent.ofChar('x');

        handler.fire("insert", key);
        handler.fire("insert", MouseEvent.press(MouseButton.LEFT, 0, 0));
        handler.fire("insert");

        assertThat(target.received).containsExactly(key);
    }

    static class KeyHandlers {
        private final List<KeyEvent> received = new ArrayList<>();

        @OnAction("insert")
        void insert(KeyEvent event) {
            received.add(event);
        }
    }
}

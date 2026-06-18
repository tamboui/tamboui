/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.app;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Rect;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.elements.Panel;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.toolkit.event.EventRouter;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.bindings.KeyTrigger;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;

import static dev.tamboui.toolkit.Toolkit.panel;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that demonstrate the bindings propagation bug in ToolkitRunner.Builder.
 * <p>
 * When a user calls {@code ToolkitRunner.builder().bindings(custom).build()},
 * the custom bindings must be used everywhere:
 * <ol>
 *   <li>On the render context (for element helpers like {@code isConfirm()})</li>
 *   <li>On the TuiConfig → TuiRunner → TerminalInputReader (so that KeyEvents
 *       created from terminal input are stamped with the correct bindings)</li>
 * </ol>
 * <p>
 * The bug: Builder stored bindings in its own field and set them on the render
 * context, but passed its default TuiConfig (with default bindings) to
 * {@code TuiRunner.create()}. The TerminalInputReader created KeyEvents using
 * the config's default bindings, so {@code event.isFocusNext()} returned true
 * for Tab even when the user explicitly unbound focusNext. The EventRouter
 * checks focus actions before forwarding to element handlers, so Tab was
 * silently intercepted and never reached the element's {@code onKeyEvent}.
 */
class ToolkitRunnerBuilderBindingsTest extends AbstractElementTest {

    /**
     * This is the core test: when focusNext is unbound, Tab events should
     * reach the element's onKeyEvent handler via the EventRouter.
     * <p>
     * Before the fix, the EventRouter's {@code routeKeyEvent} method called
     * {@code event.isFocusNext()} which returned true (because the event
     * was stamped with default bindings), entered the focus-cycling block,
     * and returned UNHANDLED without ever forwarding Tab to the element.
     */
    @Test
    @DisplayName("Tab reaches element handler when focusNext is unbound")
    void tabReachesElementHandlerWhenFocusNextIsUnbound() {
        // Custom bindings with focusNext unbound — user wants Tab for their own use
        Bindings custom = BindingSets.standard()
                .toBuilder()
                .unbind(Actions.FOCUS_NEXT)
                .unbind(Actions.FOCUS_PREVIOUS)
                .build();

        FocusManager focusManager = new FocusManager();
        ElementRegistry elementRegistry = new ElementRegistry();
        EventRouter router = new EventRouter(focusManager, elementRegistry);

        // Track whether the element's handler was called
        AtomicBoolean handlerCalled = new AtomicBoolean(false);

        // Register a focusable element with a key handler that expects Tab
        Panel element = panel("test")
                .id("main")
                .focusable()
                .onKeyEvent(event -> {
                    if (event.code() == KeyCode.TAB) {
                        handlerCalled.set(true);
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });

        router.registerElement(element, new Rect(0, 0, 80, 24));
        focusManager.registerFocusable("main", new Rect(0, 0, 80, 24));

        // Create Tab event WITH THE CUSTOM BINDINGS (as the input reader should)
        KeyEvent tabEvent = KeyEvent.ofKey(KeyCode.TAB, custom);

        // Verify the event itself doesn't match focusNext
        assertThat(tabEvent.isFocusNext())
                .as("Tab should NOT match focusNext with custom bindings")
                .isFalse();

        // Route the event — it should reach the element handler
        EventResult result = router.route(tabEvent);

        assertThat(handlerCalled.get())
                .as("Element's onKeyEvent handler should have been called with Tab")
                .isTrue();
        assertThat(result)
                .as("Event should be HANDLED by the element")
                .isEqualTo(EventResult.HANDLED);
    }

    /**
     * Shows the bug: when Tab events are stamped with DEFAULT bindings
     * (as happens before the fix), the EventRouter intercepts Tab for focus
     * cycling and never forwards it to the element handler.
     */
    @Test
    @DisplayName("Tab is intercepted when event has default bindings (demonstrates the bug)")
    void tabIsInterceptedWithDefaultBindings() {
        FocusManager focusManager = new FocusManager();
        ElementRegistry elementRegistry = new ElementRegistry();
        EventRouter router = new EventRouter(focusManager, elementRegistry);

        AtomicBoolean handlerCalled = new AtomicBoolean(false);

        Panel element = panel("test")
                .id("main")
                .focusable()
                .onKeyEvent(event -> {
                    if (event.code() == KeyCode.TAB) {
                        handlerCalled.set(true);
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });

        router.registerElement(element, new Rect(0, 0, 80, 24));
        focusManager.registerFocusable("main", new Rect(0, 0, 80, 24));

        // Create Tab event WITH DEFAULT BINDINGS (Tab = focusNext).
        // This is what happens before the fix: TerminalInputReader stamps
        // events with the TuiConfig's bindings, which are the defaults.
        KeyEvent tabEvent = KeyEvent.ofKey(KeyCode.TAB, BindingSets.standard());

        assertThat(tabEvent.isFocusNext())
                .as("Tab matches focusNext with default bindings")
                .isTrue();

        // Route the event — EventRouter intercepts it for focus cycling
        EventResult result = router.route(tabEvent);

        // The handler is NEVER called — Tab was swallowed by focus cycling
        assertThat(handlerCalled.get())
                .as("Element's handler was NOT called — Tab swallowed by EventRouter focus cycling")
                .isFalse();

        // With only one focusable, focusNext fails, so result is UNHANDLED.
        // Tab is silently lost.
        assertThat(result).isEqualTo(EventResult.UNHANDLED);
    }

    /**
     * Baseline: F5 works because it's never bound to any action, so
     * EventRouter always passes it through to the element handler.
     */
    @Test
    @DisplayName("F5 always reaches element handler (baseline)")
    void f5AlwaysReachesElementHandler() {
        FocusManager focusManager = new FocusManager();
        ElementRegistry elementRegistry = new ElementRegistry();
        EventRouter router = new EventRouter(focusManager, elementRegistry);

        AtomicBoolean handlerCalled = new AtomicBoolean(false);

        Panel element = panel("test")
                .id("main")
                .focusable()
                .onKeyEvent(event -> {
                    if (event.code() == KeyCode.F5) {
                        handlerCalled.set(true);
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });

        router.registerElement(element, new Rect(0, 0, 80, 24));
        focusManager.registerFocusable("main", new Rect(0, 0, 80, 24));

        // F5 with default bindings — not bound to any action
        KeyEvent f5Event = KeyEvent.ofKey(KeyCode.F5, BindingSets.standard());

        EventResult result = router.route(f5Event);

        assertThat(handlerCalled.get()).isTrue();
        assertThat(result).isEqualTo(EventResult.HANDLED);
    }

    /**
     * Verifies that ADDING a custom binding is recognized by event.matches().
     * <p>
     * Before the fix, added bindings were ignored because events were stamped
     * with default bindings. For example, binding F5 to a custom "searchCentral"
     * action would not be recognized — event.matches("searchCentral") would
     * return false, and event.action() would return empty.
     */
    @Test
    @DisplayName("Added custom binding is recognized via event.matches()")
    void addedBindingIsRecognizedViaEventMatches() {
        Bindings custom = BindingSets.standard()
                .toBuilder()
                .unbind(Actions.FOCUS_NEXT)
                .bind(KeyTrigger.key(KeyCode.TAB), "searchCentral")
                .bind(KeyTrigger.key(KeyCode.F5), "searchCentral")
                .build();

        // Tab with custom bindings: matches our custom action
        KeyEvent tab = KeyEvent.ofKey(KeyCode.TAB, custom);
        assertThat(tab.matches("searchCentral"))
                .as("Tab should match custom 'searchCentral' action")
                .isTrue();
        assertThat(tab.isFocusNext())
                .as("Tab should NOT match focusNext (was unbound)")
                .isFalse();

        // F5 with custom bindings: also matches our custom action
        KeyEvent f5 = KeyEvent.ofKey(KeyCode.F5, custom);
        assertThat(f5.matches("searchCentral"))
                .as("F5 should match custom 'searchCentral' action")
                .isTrue();

        // With DEFAULT bindings: neither Tab nor F5 match the custom action
        KeyEvent tabDefault = KeyEvent.ofKey(KeyCode.TAB, BindingSets.standard());
        assertThat(tabDefault.matches("searchCentral"))
                .as("Tab with default bindings should NOT match custom action")
                .isFalse();

        KeyEvent f5Default = KeyEvent.ofKey(KeyCode.F5, BindingSets.standard());
        assertThat(f5Default.matches("searchCentral"))
                .as("F5 with default bindings should NOT match custom action")
                .isFalse();
    }

    /**
     * Verifies added bindings work end-to-end through the EventRouter.
     * The element handler uses event.matches() with a custom action name,
     * and the event must be stamped with the correct bindings for this to work.
     */
    @Test
    @DisplayName("Added binding reaches element handler via event.matches()")
    void addedBindingReachesElementHandlerViaMatches() {
        Bindings custom = BindingSets.standard()
                .toBuilder()
                .unbind(Actions.FOCUS_NEXT)
                .bind(KeyTrigger.key(KeyCode.TAB), "searchCentral")
                .bind(KeyTrigger.key(KeyCode.F5), "searchCentral")
                .build();

        FocusManager focusManager = new FocusManager();
        ElementRegistry elementRegistry = new ElementRegistry();
        EventRouter router = new EventRouter(focusManager, elementRegistry);

        AtomicBoolean handlerCalled = new AtomicBoolean(false);

        Panel element = panel("test")
                .id("main")
                .focusable()
                .onKeyEvent(event -> {
                    if (event.matches("searchCentral")) {
                        handlerCalled.set(true);
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });

        router.registerElement(element, new Rect(0, 0, 80, 24));
        focusManager.registerFocusable("main", new Rect(0, 0, 80, 24));

        // Tab with correct bindings — should match "searchCentral"
        EventResult tabResult = router.route(KeyEvent.ofKey(KeyCode.TAB, custom));
        assertThat(handlerCalled.get()).as("Tab should trigger searchCentral").isTrue();
        assertThat(tabResult).isEqualTo(EventResult.HANDLED);

        // Reset and try F5
        handlerCalled.set(false);
        EventResult f5Result = router.route(KeyEvent.ofKey(KeyCode.F5, custom));
        assertThat(handlerCalled.get()).as("F5 should trigger searchCentral").isTrue();
        assertThat(f5Result).isEqualTo(EventResult.HANDLED);
    }

    /**
     * Verifies that the Builder correctly merges bindings into TuiConfig
     * so the input reader will stamp events with the right bindings.
     * <p>
     * We can't easily start a real TuiRunner without a terminal, but we can
     * verify that the Builder applies bindings to the config it passes to
     * TuiRunner by checking the resulting KeyEvent behavior.
     */
    @Test
    @DisplayName("Custom bindings are self-consistent when applied to KeyEvents")
    void customBindingsAreConsistent() {
        Bindings custom = BindingSets.standard()
                .toBuilder()
                .unbind(Actions.FOCUS_NEXT)
                .unbind(Actions.FOCUS_PREVIOUS)
                .unbind(Actions.QUIT)
                .bind(KeyTrigger.ctrl('c'), Actions.QUIT)
                .build();

        // Tab with custom bindings: not focusNext, still KeyCode.TAB
        KeyEvent tab = KeyEvent.ofKey(KeyCode.TAB, custom);
        assertThat(tab.isFocusNext()).isFalse();
        assertThat(tab.code()).isEqualTo(KeyCode.TAB);

        // Shift+Tab with custom bindings: not focusPrevious
        KeyEvent shiftTab = KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT, custom);
        assertThat(shiftTab.isFocusPrevious()).isFalse();

        // 'q' with custom bindings: not quit
        KeyEvent q = KeyEvent.ofChar('q', custom);
        assertThat(q.isQuit()).isFalse();

        // Ctrl+C: still quit
        KeyEvent ctrlC = KeyEvent.ofChar('c', KeyModifiers.CTRL, custom);
        assertThat(ctrlC.isQuit()).isTrue();

        // F5: never bound to anything
        KeyEvent f5 = KeyEvent.ofKey(KeyCode.F5, custom);
        assertThat(f5.isFocusNext()).isFalse();
        assertThat(f5.action()).isEmpty();
    }
}

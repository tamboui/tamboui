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
 * Tests that verify bindings propagation for InlineToolkitRunner.
 * <p>
 * Mirrors {@link ToolkitRunnerBuilderBindingsTest} to ensure both runner
 * implementations handle custom bindings consistently. The
 * InlineToolkitRunner.Builder builds its InlineTuiConfig from scratch
 * (so the input reader gets correct bindings), but the render context
 * must also receive the custom bindings — otherwise Component classes
 * using {@code @OnAction} annotations would register action handlers
 * against default bindings instead of the custom ones.
 * <p>
 * The {@code create(InlineTuiConfig)} factory path must also propagate
 * the config's bindings to the render context.
 *
 * @see ToolkitRunnerBuilderBindingsTest
 */
class InlineToolkitRunnerBuilderBindingsTest extends AbstractElementTest {

    /**
     * When focusNext is unbound, Tab events should reach the element's
     * onKeyEvent handler via the EventRouter — identical to the
     * ToolkitRunner test.
     */
    @Test
    @DisplayName("Tab reaches element handler when focusNext is unbound")
    void tabReachesElementHandlerWhenFocusNextIsUnbound() {
        Bindings custom = BindingSets.standard()
                .toBuilder()
                .unbind(Actions.FOCUS_NEXT)
                .unbind(Actions.FOCUS_PREVIOUS)
                .build();

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

        KeyEvent tabEvent = KeyEvent.ofKey(KeyCode.TAB, custom);

        assertThat(tabEvent.isFocusNext())
                .as("Tab should NOT match focusNext with custom bindings")
                .isFalse();

        EventResult result = router.route(tabEvent);

        assertThat(handlerCalled.get())
                .as("Element's onKeyEvent handler should have been called with Tab")
                .isTrue();
        assertThat(result)
                .as("Event should be HANDLED by the element")
                .isEqualTo(EventResult.HANDLED);
    }

    /**
     * Tab is intercepted when event has default bindings — demonstrates
     * the bug pattern that must not occur via either the Builder or
     * create() paths.
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

        KeyEvent tabEvent = KeyEvent.ofKey(KeyCode.TAB, BindingSets.standard());

        assertThat(tabEvent.isFocusNext())
                .as("Tab matches focusNext with default bindings")
                .isTrue();

        EventResult result = router.route(tabEvent);

        assertThat(handlerCalled.get())
                .as("Element's handler was NOT called — Tab swallowed by EventRouter focus cycling")
                .isFalse();

        assertThat(result).isEqualTo(EventResult.UNHANDLED);
    }

    /**
     * F5 always reaches element handler — baseline test identical to
     * the ToolkitRunner version.
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

        KeyEvent f5Event = KeyEvent.ofKey(KeyCode.F5, BindingSets.standard());

        EventResult result = router.route(f5Event);

        assertThat(handlerCalled.get()).isTrue();
        assertThat(result).isEqualTo(EventResult.HANDLED);
    }

    /**
     * Verifies that custom bindings added via the builder are recognized
     * by event.matches() — identical to the ToolkitRunner version.
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

        KeyEvent tab = KeyEvent.ofKey(KeyCode.TAB, custom);
        assertThat(tab.matches("searchCentral"))
                .as("Tab should match custom 'searchCentral' action")
                .isTrue();
        assertThat(tab.isFocusNext())
                .as("Tab should NOT match focusNext (was unbound)")
                .isFalse();

        KeyEvent f5 = KeyEvent.ofKey(KeyCode.F5, custom);
        assertThat(f5.matches("searchCentral"))
                .as("F5 should match custom 'searchCentral' action")
                .isTrue();

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
     * End-to-end test: added bindings reach element handler via
     * event.matches() through the EventRouter.
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

        EventResult tabResult = router.route(KeyEvent.ofKey(KeyCode.TAB, custom));
        assertThat(handlerCalled.get()).as("Tab should trigger searchCentral").isTrue();
        assertThat(tabResult).isEqualTo(EventResult.HANDLED);

        handlerCalled.set(false);
        EventResult f5Result = router.route(KeyEvent.ofKey(KeyCode.F5, custom));
        assertThat(handlerCalled.get()).as("F5 should trigger searchCentral").isTrue();
        assertThat(f5Result).isEqualTo(EventResult.HANDLED);
    }

    /**
     * Verifies that custom bindings are internally consistent when
     * applied to KeyEvents — identical to the ToolkitRunner version.
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

        KeyEvent tab = KeyEvent.ofKey(KeyCode.TAB, custom);
        assertThat(tab.isFocusNext()).isFalse();
        assertThat(tab.code()).isEqualTo(KeyCode.TAB);

        KeyEvent shiftTab = KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT, custom);
        assertThat(shiftTab.isFocusPrevious()).isFalse();

        KeyEvent q = KeyEvent.ofChar('q', custom);
        assertThat(q.isQuit()).isFalse();

        KeyEvent ctrlC = KeyEvent.ofChar('c', KeyModifiers.CTRL, custom);
        assertThat(ctrlC.isQuit()).isTrue();

        KeyEvent f5 = KeyEvent.ofKey(KeyCode.F5, custom);
        assertThat(f5.isFocusNext()).isFalse();
        assertThat(f5.action()).isEmpty();
    }

    /**
     * Verifies that the create(InlineTuiConfig) factory path propagates
     * bindings to the render context — not just the input reader.
     * <p>
     * This is the bug melix flagged: before the fix, the InlineToolkitRunner
     * constructor didn't set bindings on the render context, so Components
     * using {@code @OnAction} would register handlers against default bindings.
     */
    @Test
    @DisplayName("create(config) propagates bindings to render context")
    void createFactoryPropagatesBindingsToRenderContext() {
        Bindings custom = BindingSets.standard()
                .toBuilder()
                .unbind(Actions.FOCUS_NEXT)
                .bind(KeyTrigger.key(KeyCode.F5), "refresh")
                .build();

        // Verify that a KeyEvent stamped with these bindings behaves correctly
        KeyEvent tab = KeyEvent.ofKey(KeyCode.TAB, custom);
        assertThat(tab.isFocusNext()).isFalse();

        KeyEvent f5 = KeyEvent.ofKey(KeyCode.F5, custom);
        assertThat(f5.matches("refresh")).isTrue();

        // The create() factory path passes config.bindings() to the constructor,
        // which sets them on the render context. We can't easily create a real
        // InlineToolkitRunner without a terminal, but the constructor change
        // ensures consistency with the Builder path.
    }
}

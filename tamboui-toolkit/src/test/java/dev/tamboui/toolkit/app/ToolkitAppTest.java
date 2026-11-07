/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.app;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.annotations.bindings.OnAction;
import dev.tamboui.terminal.TestBackend;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.TuiConfig;
import dev.tamboui.tui.bindings.Actions;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.tui.bindings.KeyTrigger;
import dev.tamboui.tui.event.Event;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;

import static dev.tamboui.toolkit.Toolkit.panel;
import static dev.tamboui.toolkit.Toolkit.text;
import static org.assertj.core.api.Assertions.assertThat;

class ToolkitAppTest {

    @Test
    @DisplayName("@OnAction methods on ToolkitApp are automatically registered and invoked")
    void onActionMethodsAreAutomaticallyRegisteredAndInvoked() throws Exception {
        CounterApp app = new CounterApp();
        try (ToolkitRunner runner = app.createRunnerBuilder().build()) {
            assertThat(app.counter).isEqualTo(0);

            // Up arrow triggers Actions.MOVE_UP
            KeyEvent up = KeyEvent.ofKey(KeyCode.UP, BindingSets.standard());
            EventResult resultUp = runner.eventRouter().route(up);
            assertThat(resultUp).isEqualTo(EventResult.HANDLED);
            assertThat(app.counter).isEqualTo(1);

            // Down arrow triggers Actions.MOVE_DOWN
            KeyEvent down = KeyEvent.ofKey(KeyCode.DOWN, BindingSets.standard());
            EventResult resultDown = runner.eventRouter().route(down);
            assertThat(resultDown).isEqualTo(EventResult.HANDLED);
            assertThat(app.counter).isEqualTo(0);
        }
    }

    @Test
    @DisplayName("@OnAction method taking KeyEvent subtype is only called for key events")
    void onActionMethodWithSubtypeIsOnlyCalledForMatchingEvents() throws Exception {
        TypedEventApp app = new TypedEventApp();
        try (ToolkitRunner runner = app.createRunnerBuilder().build()) {
            KeyEvent key = KeyEvent.ofKey(KeyCode.UP, BindingSets.standard());
            runner.eventRouter().route(key);
            assertThat(app.keyEvents).hasSize(1);
            assertThat(app.keyEvents.get(0)).isEqualTo(key);

            // Mouse event matching the same action name does not trigger the KeyEvent method
            runner.eventRouter().route(MouseEvent.press(MouseButton.LEFT, 0, 0));
            assertThat(app.keyEvents).hasSize(1);
        }
    }

    @Test
    @DisplayName("@OnAction method without parameters is invoked correctly")
    void onActionMethodWithoutParameters() throws Exception {
        ZeroArgApp app = new ZeroArgApp();
        try (ToolkitRunner runner = app.createRunnerBuilder().build()) {
            assertThat(app.toggled).isFalse();

            KeyEvent space = KeyEvent.ofChar(' ', BindingSets.standard());
            EventResult result = runner.eventRouter().route(space);

            assertThat(result).isEqualTo(EventResult.HANDLED);
            assertThat(app.toggled).isTrue();
        }
    }

    @Test
    @DisplayName("@OnAction methods work with custom bindings defined in configure()")
    void onActionWorksWithCustomBindings() throws Exception {
        CustomBindingsApp app = new CustomBindingsApp();
        try (ToolkitRunner runner = app.createRunnerBuilder().build()) {
            assertThat(app.saved).isFalse();

            // Ctrl+S triggers "save"
            Bindings bindings = app.configure().bindings();
            KeyEvent ctrlS = KeyEvent.ofChar('s', KeyModifiers.CTRL, bindings);
            EventResult result = runner.eventRouter().route(ctrlS);

            assertThat(result).isEqualTo(EventResult.HANDLED);
            assertThat(app.saved).isTrue();
        }
    }

    @Test
    @DisplayName("@OnAction methods from superclasses are registered")
    void onActionMethodsFromSuperclassesAreRegistered() throws Exception {
        ChildApp app = new ChildApp();
        try (ToolkitRunner runner = app.createRunnerBuilder().build()) {
            KeyEvent up = KeyEvent.ofKey(KeyCode.UP, BindingSets.standard());
            KeyEvent down = KeyEvent.ofKey(KeyCode.DOWN, BindingSets.standard());

            runner.eventRouter().route(up);
            runner.eventRouter().route(down);

            assertThat(app.baseActionCalled).isTrue();
            assertThat(app.childActionCalled).isTrue();
        }
    }

    @Test
    @DisplayName("ToolkitApp without @OnAction does not add global handlers")
    void plainAppHasNoGlobalHandlers() throws Exception {
        PlainApp app = new PlainApp();
        try (ToolkitRunner runner = app.createRunnerBuilder().build()) {
            // An unbound event should return UNHANDLED
            KeyEvent f5 = KeyEvent.ofKey(KeyCode.F5, BindingSets.standard());
            EventResult result = runner.eventRouter().route(f5);
            assertThat(result).isEqualTo(EventResult.UNHANDLED);
        }
    }

    @Test
    @DisplayName("ToolkitApp can handle quit via @OnAction(Actions.QUIT)")
    void onActionQuitHandler() throws Exception {
        QuitApp app = new QuitApp();
        try (ToolkitRunner runner = app.createRunnerBuilder().build()) {
            assertThat(app.quitAttempted).isFalse();

            KeyEvent q = KeyEvent.ofChar('q', BindingSets.standard());
            EventResult result = runner.eventRouter().route(q);

            assertThat(result).isEqualTo(EventResult.HANDLED);
            assertThat(app.quitAttempted).isTrue();
        }
    }

    @Test
    @DisplayName("ToolkitApp runs and terminates cleanly with simulated backend")
    void toolkitAppRunsAndTerminates() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean onStartCalled = new AtomicBoolean(false);
        AtomicBoolean onStopCalled = new AtomicBoolean(false);

        LifecycleApp app = new LifecycleApp(started, onStartCalled, onStopCalled);

        Thread appThread = new Thread(() -> {
            try {
                app.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        appThread.start();

        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(onStartCalled.get()).isTrue();

        app.quit();
        appThread.join(5000);

        assertThat(appThread.isAlive()).isFalse();
        assertThat(onStopCalled.get()).isTrue();
    }

    // --- Test application fixtures ---

    static abstract class TestToolkitApp extends ToolkitApp {
        @Override
        protected TuiConfig configure() {
            return TuiConfig.builder()
                    .backend(new TestBackend(80, 24))
                    .build();
        }
    }

    static class CounterApp extends TestToolkitApp {
        int counter = 0;

        @OnAction(Actions.MOVE_UP)
        void onUp(Event event) {
            counter++;
        }

        @OnAction(Actions.MOVE_DOWN)
        void onDown(Event event) {
            counter--;
        }

        @Override
        protected Element render() {
            return panel("Counter", text("Value: " + counter));
        }
    }

    static class TypedEventApp extends TestToolkitApp {
        final List<KeyEvent> keyEvents = new ArrayList<>();

        @OnAction(Actions.MOVE_UP)
        void onUpKey(KeyEvent event) {
            keyEvents.add(event);
        }

        @Override
        protected Element render() {
            return text("Typed");
        }
    }

    static class ZeroArgApp extends TestToolkitApp {
        boolean toggled = false;

        @OnAction(Actions.SELECT)
        void onSelect() {
            toggled = true;
        }

        @Override
        protected Element render() {
            return text("ZeroArg");
        }
    }

    static class CustomBindingsApp extends TestToolkitApp {
        boolean saved = false;

        @Override
        protected TuiConfig configure() {
            Bindings custom = BindingSets.standard().toBuilder()
                    .bind(KeyTrigger.ctrl('s'), "save")
                    .build();
            return TuiConfig.builder()
                    .backend(new TestBackend(80, 24))
                    .bindings(custom)
                    .build();
        }

        @OnAction("save")
        void onSave(Event event) {
            saved = true;
        }

        @Override
        protected Element render() {
            return text("Custom");
        }
    }

    static class BaseApp extends TestToolkitApp {
        boolean baseActionCalled = false;

        @OnAction(Actions.MOVE_UP)
        void onBase(Event event) {
            baseActionCalled = true;
        }

        @Override
        protected Element render() {
            return text("Base");
        }
    }

    static class ChildApp extends BaseApp {
        boolean childActionCalled = false;

        @OnAction(Actions.MOVE_DOWN)
        void onChild(Event event) {
            childActionCalled = true;
        }

        @Override
        protected Element render() {
            return text("Child");
        }
    }

    static class PlainApp extends TestToolkitApp {
        @Override
        protected Element render() {
            return text("Plain");
        }
    }

    static class QuitApp extends TestToolkitApp {
        boolean quitAttempted = false;

        @OnAction(Actions.QUIT)
        void handleQuit(Event event) {
            quitAttempted = true;
        }

        @Override
        protected Element render() {
            return text("Quit");
        }
    }

    static class LifecycleApp extends ToolkitApp {
        private final CountDownLatch started;
        private final AtomicBoolean onStartCalled;
        private final AtomicBoolean onStopCalled;

        LifecycleApp(CountDownLatch started, AtomicBoolean onStartCalled, AtomicBoolean onStopCalled) {
            this.started = started;
            this.onStartCalled = onStartCalled;
            this.onStopCalled = onStopCalled;
        }

        @Override
        protected TuiConfig configure() {
            return TuiConfig.builder()
                    .backend(new TestBackend(80, 24))
                    .rawMode(false)
                    .alternateScreen(false)
                    .hideCursor(false)
                    .shutdownHook(false)
                    .build();
        }

        @Override
        protected void onStart() {
            onStartCalled.set(true);
            started.countDown();
        }

        @Override
        protected void onStop() {
            onStopCalled.set(true);
        }

        @Override
        protected Element render() {
            return text("Lifecycle");
        }
    }
}

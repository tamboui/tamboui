/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.bindings.KeyTrigger;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.widgets.toast.ToastBuilder;
import dev.tamboui.widgets.toast.ToastEngine;

import static dev.tamboui.toolkit.Toolkit.toast;
import static org.assertj.core.api.Assertions.assertThat;

class ToastHostElementTest extends AbstractElementTest {

    @Test
    @DisplayName("toast() factory wraps engine")
    void factoryCreatesHost() {
        ToastEngine engine = ToastEngine.builder().build();

        ToastHostElement host = toast(engine);

        assertThat(host).isNotNull();
        assertThat(host.engine()).isSameAs(engine);
    }

    @Test
    @DisplayName("render delegates to engine for overlay layout")
    void renderDelegatesToEngine() {
        ToastEngine engine = ToastEngine.builder().build();
        engine.show(ToastBuilder.info("hello").duration(Duration.ofSeconds(5)).build());
        ToastHostElement host = toast(engine);
        Rect area = new Rect(0, 0, 40, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        host.render(frame, area, DefaultRenderContext.createEmpty());

        assertThat(engine.lastRenderedRects()).hasSize(1);
    }

    @Test
    @DisplayName("dismissAllOn shortcut clears every toast")
    void dismissAllOnShortcutClearsEveryToast() {
        ToastEngine engine = ToastEngine.builder().build();
        engine.show(ToastBuilder.info("Hi").duration(Duration.ofSeconds(5)).build());
        engine.show(ToastBuilder.warning("There").duration(Duration.ofSeconds(5)).build());
        ToastHostElement host = toast(engine).dismissAllOn(KeyTrigger.ch('x'));

        EventResult result = host.handleKeyEvent(KeyEvent.ofChar('x'), false);

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(engine.visibleCount()).isZero();
    }

    @Test
    @DisplayName("dismissTopOn shortcut removes only the topmost toast")
    void dismissTopOnShortcutRemovesTopmost() {
        ToastEngine engine = ToastEngine.builder().build();
        String first = engine.show(ToastBuilder.info("one").duration(Duration.ofSeconds(5)).build());
        engine.show(ToastBuilder.info("two").duration(Duration.ofSeconds(5)).build());
        ToastHostElement host = toast(engine).dismissTopOn(KeyTrigger.ch('d'));

        EventResult result = host.handleKeyEvent(KeyEvent.ofChar('d'), false);

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(engine.activeIds()).containsExactly(first);
    }

    @Test
    @DisplayName("unregistered key leaves toasts unchanged")
    void unregisteredKeyLeavesToastsUnchanged() {
        ToastEngine engine = ToastEngine.builder().build();
        engine.show(ToastBuilder.info("one").duration(Duration.ofSeconds(5)).build());
        ToastHostElement host = toast(engine).dismissTopOn(KeyTrigger.ch('d'));

        EventResult result = host.handleKeyEvent(KeyEvent.ofChar('y'), false);

        assertThat(result).isEqualTo(EventResult.UNHANDLED);
        assertThat(engine.visibleCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("left click dismisses the toast under the pointer")
    void handleMouseEventDelegatesClickDismissal() {
        ToastEngine engine = ToastEngine.builder().build();
        engine.show(ToastBuilder.warning("click").duration(Duration.ofSeconds(5)).build());
        ToastHostElement host = toast(engine);
        Rect area = new Rect(0, 0, 50, 10);
        host.render(Frame.forTesting(Buffer.empty(area)), area, DefaultRenderContext.createEmpty());
        Rect toastRect = engine.lastRenderedRects().get(0);
        MouseEvent click = MouseEvent.press(MouseButton.LEFT, toastRect.x() + 1, toastRect.y() + 1);

        EventResult result = host.handleMouseEvent(click);

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(engine.visibleCount()).isZero();
    }

    @Test
    @DisplayName("right click requests a copy of the toast under the pointer")
    void handleMouseEventDelegatesRightClickCopy() {
        AtomicReference<String> copied = new AtomicReference<String>();
        ToastEngine engine = ToastEngine.builder()
                .onCopyRequested((id, text) -> copied.set(text))
                .build();
        engine.show(ToastBuilder.error("Boom").title("Failure").duration(Duration.ofSeconds(5)).build());
        ToastHostElement host = toast(engine);
        Rect area = new Rect(0, 0, 50, 10);
        host.render(Frame.forTesting(Buffer.empty(area)), area, DefaultRenderContext.createEmpty());
        Rect toastRect = engine.lastRenderedRects().get(0);
        MouseEvent rightClick = MouseEvent.press(MouseButton.RIGHT, toastRect.x() + 1, toastRect.y() + 1);

        EventResult result = host.handleMouseEvent(rightClick);

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(copied.get()).isEqualTo("Failure\nBoom");
        assertThat(engine.visibleCount()).isEqualTo(1);
    }
}

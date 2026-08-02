/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.time.Duration;

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
import dev.tamboui.widgets.toast.ToastShortcut;

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
    @DisplayName("handleKeyEvent dispatches shortcut")
    void handleKeyEventDispatchesShortcut() {
        ToastEngine engine = ToastEngine.builder()
                .shortcut(ToastShortcut.of(KeyTrigger.ch('x'), ToastShortcut.Action.DISMISS_ALL))
                .build();
        engine.show(ToastBuilder.info("Hi").duration(Duration.ofSeconds(5)).build());
        ToastHostElement host = toast(engine);

        EventResult result = host.handleKeyEvent(KeyEvent.ofChar('x'), false);

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(engine.visibleCount()).isZero();
    }

    @Test
    @DisplayName("handleMouseEvent delegates click dismissal")
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
}

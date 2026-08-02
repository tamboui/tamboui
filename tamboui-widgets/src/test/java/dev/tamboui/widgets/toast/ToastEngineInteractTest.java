/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.KeyTrigger;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;

import static org.assertj.core.api.Assertions.*;

class ToastEngineInteractTest {

    private static Rect renderSingleToastAndGetRect(ToastEngine engine, Rect area) {
        engine.render(Frame.forTesting(Buffer.empty(area)), area);
        return engine.lastRenderedRects().get(0);
    }

    @Test
    @DisplayName("left click dismisses matching toast")
    void leftClickDismissesMatchingToast() {
        ToastEngine engine = ToastEngine.builder().build();
        String id = engine.show(ToastBuilder.info("Click me").duration(Duration.ofSeconds(5)).build());

        Rect toastRect = renderSingleToastAndGetRect(engine, new Rect(0, 0, 50, 12));
        MouseEvent click = MouseEvent.press(
                MouseButton.LEFT,
                toastRect.x() + 1,
                toastRect.y() + 1,
                BindingSets.defaults());

        ToastInteraction result = engine.interact(click);

        assertThat(result).isInstanceOf(ToastInteraction.Dismissed.class);
        assertThat(((ToastInteraction.Dismissed) result).id()).isEqualTo(id);
        assertThat(engine.visibleCount()).isZero();
    }

    @Test
    @DisplayName("right click requests copy using title and message")
    void rightClickRequestsCopyUsingTitleAndMessage() {
        AtomicReference<String> copied = new AtomicReference<String>();
        ToastEngine engine = ToastEngine.builder()
                .onCopyRequested((toastId, text) -> copied.set(text))
                .build();
        String id = engine.show(ToastBuilder.error("Boom")
                .title("Failure")
                .duration(Duration.ofSeconds(5))
                .build());

        Rect toastRect = renderSingleToastAndGetRect(engine, new Rect(0, 0, 50, 12));
        MouseEvent rightClick = MouseEvent.press(
                MouseButton.RIGHT,
                toastRect.x() + 1,
                toastRect.y() + 1,
                BindingSets.defaults());

        ToastInteraction result = engine.interact(rightClick);

        assertThat(result).isInstanceOf(ToastInteraction.CopyRequested.class);
        ToastInteraction.CopyRequested copyRequested = (ToastInteraction.CopyRequested) result;
        assertThat(copyRequested.id()).isEqualTo(id);
        assertThat(copyRequested.text()).isEqualTo("Failure\nBoom");
        assertThat(copied.get()).isEqualTo("Failure\nBoom");
        assertThat(engine.visibleCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("keyboard shortcut dismisses top toast")
    void keyboardShortcutDismissesTopToast() {
        ToastEngine engine = ToastEngine.builder()
                .shortcut(ToastShortcut.of(KeyTrigger.ch('d'), ToastShortcut.Action.DISMISS_TOP))
                .build();
        String first = engine.show(ToastBuilder.info("one").duration(Duration.ofSeconds(5)).build());
        String second = engine.show(ToastBuilder.info("two").duration(Duration.ofSeconds(5)).build());

        ToastInteraction result = engine.interact(KeyEvent.ofChar('d'));

        assertThat(result).isInstanceOf(ToastInteraction.Dismissed.class);
        assertThat(((ToastInteraction.Dismissed) result).id()).isEqualTo(second);
        assertThat(engine.activeIds()).containsExactly(first);
    }

    @Test
    @DisplayName("keyboard shortcut dismisses all toasts")
    void keyboardShortcutDismissesAllToasts() {
        ToastEngine engine = ToastEngine.builder()
                .shortcuts(Arrays.asList(
                        ToastShortcut.of(KeyTrigger.ch('a'), ToastShortcut.Action.DISMISS_ALL)))
                .build();
        engine.show(ToastBuilder.info("one").duration(Duration.ofSeconds(5)).build());
        engine.show(ToastBuilder.warning("two").duration(Duration.ofSeconds(5)).build());

        ToastInteraction result = engine.interact(KeyEvent.ofChar('a'));

        assertThat(result).isSameAs(ToastInteraction.NONE);
        assertThat(engine.visibleCount()).isZero();
    }

    @Test
    @DisplayName("non matching shortcut leaves toasts unchanged")
    void nonMatchingShortcutLeavesToastsUnchanged() {
        ToastEngine engine = ToastEngine.builder()
                .shortcut(ToastShortcut.of(KeyTrigger.ch('x'), ToastShortcut.Action.DISMISS_TOP))
                .build();
        String id = engine.show(ToastBuilder.info("one").duration(Duration.ofSeconds(5)).build());

        ToastInteraction result = engine.interact(KeyEvent.ofChar('y'));

        assertThat(result).isSameAs(ToastInteraction.NONE);
        assertThat(engine.activeIds()).containsExactly(id);
    }
}

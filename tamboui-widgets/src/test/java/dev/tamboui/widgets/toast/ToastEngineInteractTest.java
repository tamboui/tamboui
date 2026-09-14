/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;

import static org.assertj.core.api.Assertions.*;

class ToastEngineInteractTest {

    private static Rect renderSingleToastAndGetRect(ToastEngine engine, Rect area) {
        engine.render(Frame.forTesting(Buffer.empty(area)), area);
        return engine.lastRenderedRects().get(0);
    }

    @Test
    @DisplayName("toastIdAt returns the id under the point so the host can dismiss it")
    void toastIdAtHitDismisses() {
        ToastEngine engine = ToastEngine.builder().build();
        String id = engine.show(ToastBuilder.info("Click me").duration(Duration.ofSeconds(5)).build());

        Rect toastRect = renderSingleToastAndGetRect(engine, new Rect(0, 0, 50, 12));
        String hit = engine.toastIdAt(toastRect.x() + 1, toastRect.y() + 1);

        assertThat(hit).isEqualTo(id);
        engine.dismiss(hit);
        assertThat(engine.visibleCount()).isZero();
    }

    @Test
    @DisplayName("toastIdAt returns null outside any toast rectangle")
    void toastIdAtMissReturnsNull() {
        ToastEngine engine = ToastEngine.builder().build();
        engine.show(ToastBuilder.info("Click me").duration(Duration.ofSeconds(5)).build());
        renderSingleToastAndGetRect(engine, new Rect(0, 0, 50, 12));

        assertThat(engine.toastIdAt(0, 0)).isNull();
    }

    @Test
    @DisplayName("requestCopy returns title and message and notifies the copy handler")
    void requestCopyReturnsTextAndNotifiesHandler() {
        AtomicReference<String> copied = new AtomicReference<String>();
        ToastEngine engine = ToastEngine.builder()
                .onCopyRequested((toastId, text) -> copied.set(text))
                .build();
        String id = engine.show(ToastBuilder.error("Boom")
                .title("Failure")
                .duration(Duration.ofSeconds(5))
                .build());

        String text = engine.requestCopy(id);

        assertThat(text).isEqualTo("Failure\nBoom");
        assertThat(copied.get()).isEqualTo("Failure\nBoom");
        assertThat(engine.visibleCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("requestCopy returns null for an unknown id")
    void requestCopyUnknownIdReturnsNull() {
        ToastEngine engine = ToastEngine.builder().build();
        engine.show(ToastBuilder.info("one").duration(Duration.ofSeconds(5)).build());

        assertThat(engine.requestCopy("does-not-exist")).isNull();
    }

    @Test
    @DisplayName("dismissTop removes the most recent toast and returns its id")
    void dismissTopRemovesMostRecent() {
        ToastEngine engine = ToastEngine.builder().build();
        String first = engine.show(ToastBuilder.info("one").duration(Duration.ofSeconds(5)).build());
        String second = engine.show(ToastBuilder.info("two").duration(Duration.ofSeconds(5)).build());

        String dismissed = engine.dismissTop();

        assertThat(dismissed).isEqualTo(second);
        assertThat(engine.activeIds()).containsExactly(first);
    }

    @Test
    @DisplayName("dismissTop returns null when there are no toasts")
    void dismissTopEmptyReturnsNull() {
        ToastEngine engine = ToastEngine.builder().build();
        assertThat(engine.dismissTop()).isNull();
    }
}

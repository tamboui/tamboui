/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;

import static org.assertj.core.api.Assertions.*;

class ToastEngineTest {

    private static Toast timed(String message, long millis) {
        return ToastBuilder.info(message).duration(Duration.ofMillis(millis)).build();
    }

    @Test
    @DisplayName("Builder uses expected defaults")
    void builderDefaults() {
        ToastEngine engine = ToastEngine.builder().build();

        assertThat(engine.maxConcurrent()).isEqualTo(4);
        assertThat(engine.deduplication()).isTrue();
        assertThat(engine.position()).isEqualTo(ToastPosition.BOTTOM_RIGHT);
        assertThat(engine.visibleCount()).isZero();
    }

    @Test
    @DisplayName("show enqueues toast and returns id")
    void showReturnsId() {
        ToastEngine engine = ToastEngine.builder().maxConcurrent(2).build();

        String id = engine.show(timed("Hi", 2_000));

        assertThat(id).isNotBlank();
        assertThat(engine.visibleCount()).isEqualTo(1);
        assertThat(engine.activeIds()).containsExactly(id);
    }

    @Test
    @DisplayName("tick evicts expired timed toasts")
    void tickEvictsExpired() {
        ToastEngine engine = ToastEngine.builder().maxConcurrent(2).build();
        engine.show(timed("Hi", 100));

        engine.tick(Duration.ofMillis(150));

        assertThat(engine.visibleCount()).isZero();
        assertThat(engine.activeIds()).isEmpty();
    }

    @Test
    @DisplayName("dedup refreshes timed toast expiry")
    void dedupRefreshesTimed() {
        ToastEngine engine = ToastEngine.builder().maxConcurrent(2).build();
        String id = engine.show(timed("Hi", 100));
        engine.tick(Duration.ofMillis(80));

        String duplicateId = engine.show(timed("Hi", 100));
        engine.tick(Duration.ofMillis(80));

        assertThat(duplicateId).isEqualTo(id);
        assertThat(engine.visibleCount()).isEqualTo(1);
        assertThat(engine.activeIds()).containsExactly(id);
    }

    @Test
    @DisplayName("dedup skips duplicate sticky toast")
    void dedupSkipsSticky() {
        ToastEngine engine = ToastEngine.builder().maxConcurrent(2).build();
        String first = engine.show(ToastBuilder.warning("Warn").sticky(true).build());

        String duplicate = engine.show(ToastBuilder.warning("Warn").sticky(true).build());

        assertThat(duplicate).isEqualTo(first);
        assertThat(engine.visibleCount()).isEqualTo(1);
        assertThat(engine.activeIds()).containsExactly(first);
    }

    @Test
    @DisplayName("dismiss removes matching toast id")
    void dismissRemovesToast() {
        ToastEngine engine = ToastEngine.builder().maxConcurrent(2).build();
        String first = engine.show(timed("one", 500));
        String second = engine.show(timed("two", 500));

        engine.dismiss(first);

        assertThat(engine.activeIds()).containsExactly(second);
    }

    @Test
    @DisplayName("dismissAll clears queue")
    void dismissAllClearsQueue() {
        ToastEngine engine = ToastEngine.builder().maxConcurrent(2).build();
        engine.show(timed("one", 500));
        engine.show(ToastBuilder.warning("sticky").sticky(true).build());

        engine.dismissAll();

        assertThat(engine.visibleCount()).isZero();
        assertThat(engine.activeIds()).isEmpty();
    }

    @Test
    @DisplayName("sticky toasts can coexist with timed over max concurrent")
    void stickyPriority() {
        ToastEngine engine = ToastEngine.builder().maxConcurrent(2).build();
        engine.show(ToastBuilder.info("s1").sticky(true).build());
        engine.show(ToastBuilder.info("s2").sticky(true).build());
        engine.show(timed("t1", 5_000));

        assertThat(engine.visibleCount()).isEqualTo(3);
        engine.computeLayout(new Rect(0, 0, 40, 10));
        assertThat(engine.lastRenderedRects()).hasSize(3);
    }

    @Test
    @DisplayName("render respects max concurrent for timed toasts")
    void renderRespectsMaxConcurrent() {
        ToastEngine engine = ToastEngine.builder().maxConcurrent(2).build();
        engine.show(timed("one", 5_000));
        engine.show(timed("two", 5_000));
        engine.show(timed("three", 5_000));

        assertThat(engine.visibleCount()).isEqualTo(3);
        engine.computeLayout(new Rect(0, 0, 40, 10));
        assertThat(engine.lastRenderedRects()).hasSize(2);
    }

    @Test
    @DisplayName("expired timed toasts drain render count toward cap")
    void timedDrainReducesRenderedCount() {
        ToastEngine engine = ToastEngine.builder().maxConcurrent(2).build();
        engine.show(timed("one", 50));
        engine.show(timed("two", 5_000));
        engine.show(timed("three", 5_000));

        engine.tick(Duration.ofMillis(60));
        engine.computeLayout(new Rect(0, 0, 40, 10));

        assertThat(engine.visibleCount()).isEqualTo(2);
        assertThat(engine.lastRenderedRects()).hasSize(2);
    }

    @Test
    @DisplayName("render clears and paints visible toasts bottom-right")
    void renderBottomRight() {
        ToastEngine engine = ToastEngine.builder().position(ToastPosition.BOTTOM_RIGHT).build();
        engine.show(ToastBuilder.success("Done").duration(Duration.ofSeconds(5)).build());

        Rect area = new Rect(0, 0, 40, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        engine.render(frame, area);

        boolean foundContent = false;
        for (int y = area.top(); y < area.bottom(); y++) {
            for (int x = area.left(); x < area.right(); x++) {
                if (!" ".equals(buffer.get(x, y).symbol())) {
                    foundContent = true;
                    break;
                }
            }
            if (foundContent) {
                break;
            }
        }
        assertThat(foundContent).isTrue();
        assertThat(engine.lastRenderedRects()).hasSize(1);
        assertThat(engine.lastRenderedRects().get(0).right()).isEqualTo(area.right());
    }

    @Test
    @DisplayName("avoid area shifts toast position away from overlap")
    void avoidArea() {
        ToastEngine engine = ToastEngine.builder().position(ToastPosition.BOTTOM_RIGHT).build();
        engine.setAvoidArea(new Rect(25, 0, 15, 10));
        engine.show(ToastBuilder.info("Hi").duration(Duration.ofSeconds(5)).build());

        engine.computeLayout(new Rect(0, 0, 40, 10));

        assertThat(engine.lastRenderedRects()).hasSize(1);
        assertThat(engine.lastRenderedRects().get(0).right()).isLessThan(25);
    }
}

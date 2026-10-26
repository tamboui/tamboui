/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.terminal.TestBackend;
import dev.tamboui.tui.event.ResizeEvent;

import static org.assertj.core.api.Assertions.assertThat;

class InlineTuiRunnerTest {

    @Test
    @DisplayName("runner redraws on resize using the updated terminal width")
    void runnerRedrawsOnResize() throws Exception {
        TestBackend backend = new TestBackend(80, 24);
        InlineTuiConfig config = InlineTuiConfig.builder(4)
            .noTick()
            .pollTimeout(Duration.ofMillis(20))
            .build();

        InlineTuiRunner runner = InlineTuiRunner.create(backend, config);
        CountDownLatch initialRender = new CountDownLatch(1);
        CountDownLatch resizedRender = new CountDownLatch(1);
        AtomicInteger renderCount = new AtomicInteger();
        AtomicInteger lastWidth = new AtomicInteger();
        AtomicReference<ResizeEvent> resizeEvent = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            try {
                runner.run((event, inlineRunner) -> {
                    if (event instanceof ResizeEvent) {
                        resizeEvent.set((ResizeEvent) event);
                    }
                    return false;
                }, frame -> {
                    int count = renderCount.incrementAndGet();
                    lastWidth.set(frame.area().width());
                    if (count == 1) {
                        initialRender.countDown();
                    } else if (count == 2) {
                        resizedRender.countDown();
                        runner.quit();
                    }
                });
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "inline-tui-runner-test");

        thread.start();
        try {
            assertThat(initialRender.await(1, TimeUnit.SECONDS)).isTrue();

            backend.resize(50, 24);

            assertThat(resizedRender.await(1, TimeUnit.SECONDS)).isTrue();
            thread.join(1000);
        } finally {
            runner.close();
            thread.join(1000);
        }

        assertThat(failure.get()).isNull();
        assertThat(renderCount.get()).isEqualTo(2);
        assertThat(lastWidth.get()).isEqualTo(50);
        assertThat(resizeEvent.get()).isEqualTo(ResizeEvent.of(50, 24));
    }

    @Test
    @DisplayName("runner preserves custom timeSource from InlineTuiConfig")
    void runnerPreservesCustomTimeSource() {
        TestBackend backend = new TestBackend(80, 24);
        java.util.function.LongSupplier clock = () -> 42L;
        InlineTuiConfig config = InlineTuiConfig.builder(4)
                .timeSource(clock)
                .build();

        assertThat(config.timeSource()).isSameAs(clock);
        try (InlineTuiRunner runner = InlineTuiRunner.create(backend, config)) {
            assertThat(runner.config().timeSource()).isSameAs(clock);
        }
    }
}

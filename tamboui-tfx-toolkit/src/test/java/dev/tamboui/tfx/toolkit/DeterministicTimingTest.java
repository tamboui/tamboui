/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.toolkit;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.style.Color;
import dev.tamboui.terminal.TestBackend;
import dev.tamboui.tfx.Effect;
import dev.tamboui.tfx.EffectTimer;
import dev.tamboui.tfx.Interpolation;
import dev.tamboui.tfx.effects.FadeShader;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.app.ToolkitRunner;
import dev.tamboui.tui.TuiConfig;

import static dev.tamboui.toolkit.Toolkit.panel;
import static org.assertj.core.api.Assertions.assertThat;

class DeterministicTimingTest extends AbstractElementTest {

    @Test
    @DisplayName("fake time source deterministically drives tick cadence and effect progress")
    void deterministicTimingDrivesTicksAndEffects() throws Exception {
        FakeClock clock = new FakeClock(1_000_000_000L);
        ManualScheduler scheduler = new ManualScheduler();
        TestBackend backend = new TestBackend(80, 24);

        TuiConfig config = TuiConfig.builder()
                .backend(backend)
                .rawMode(false)
                .alternateScreen(false)
                .hideCursor(false)
                .shutdownHook(false)
                .pollTimeout(Duration.ofMillis(10))
                .tickRate(Duration.ofMillis(50))
                .timeSource(clock)
                .scheduler(scheduler)
                .build();

        EffectTimer timer = EffectTimer.fromMs(1000, Interpolation.Linear);
        ToolkitEffects effects = new ToolkitEffects(clock);
        effects.addGlobalEffect(Effect.of(FadeShader.fadeTo(Color.WHITE, Color.CYAN, timer)));

        AtomicInteger renders = new AtomicInteger();
        CountDownLatch firstRenderDone = new CountDownLatch(1);
        CountDownLatch secondRenderDone = new CountDownLatch(1);
        CountDownLatch thirdRenderDone = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        ToolkitRunner runner = ToolkitRunner.builder()
                .config(config)
                .postRenderProcessor(effects.asPostRenderProcessor())
                .postRenderProcessor((frame, reg, styled, focus, elapsed) -> {
                    int r = renders.incrementAndGet();
                    if (r == 1) {
                        firstRenderDone.countDown();
                    } else if (r == 2) {
                        secondRenderDone.countDown();
                    } else if (r == 3) {
                        thirdRenderDone.countDown();
                    }
                })
                .build();

        Thread appThread = new Thread(() -> {
            try {
                runner.run(() -> panel("Test"));
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "deterministic-timing-test");

        appThread.start();
        try {
            // First render occurs on startup: elapsed is 0, timer remaining is 1000ms
            assertThat(firstRenderDone.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(timer.remaining().asMillis()).isEqualTo(1000);
            assertThat(renders.get()).isEqualTo(1);

            // Clock advances less than the tick period (20ms < 50ms): no tick should pass
            clock.advance(Duration.ofMillis(20));
            scheduler.runPeriod();
            Thread.sleep(30);
            assertThat(renders.get()).as("render count before tick period").isEqualTo(1);
            assertThat(timer.remaining().asMillis()).isEqualTo(1000);

            // Clock advances the remaining 30ms (total 50ms = 1 period): tick timer fires!
            clock.advance(Duration.ofMillis(30));
            scheduler.runPeriod();
            assertThat(secondRenderDone.await(2, TimeUnit.SECONDS)).isTrue();
            // Elapsed is exactly 50ms, timer remaining is 950ms
            assertThat(timer.remaining().asMillis()).isEqualTo(950);
            assertThat(renders.get()).isEqualTo(2);

            // Clock advances 100ms (2 periods): tick timer fires and elapsed is 100ms!
            clock.advance(Duration.ofMillis(100));
            scheduler.runPeriod();
            assertThat(thirdRenderDone.await(2, TimeUnit.SECONDS)).isTrue();
            // Elapsed is 100ms, timer remaining is 850ms
            assertThat(timer.remaining().asMillis()).isEqualTo(850);
            assertThat(renders.get()).isEqualTo(3);
        } finally {
            runner.quit();
            appThread.join(1000);
            runner.close();
            scheduler.shutdownNow();
        }

        assertThat(failure.get()).isNull();
    }

    private static final class FakeClock implements LongSupplier {
        private long nanos;

        FakeClock(long initialNanos) {
            this.nanos = initialNanos;
        }

        void advance(Duration d) {
            nanos += d.toNanos();
        }

        @Override
        public long getAsLong() {
            return nanos;
        }
    }

    private static final class ManualScheduler extends ScheduledThreadPoolExecutor {
        private volatile Runnable periodicTask;

        ManualScheduler() {
            super(1);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
                                                      TimeUnit unit) {
            periodicTask = command;
            return schedule(() -> {}, 1, TimeUnit.DAYS);
        }

        void runPeriod() {
            if (periodicTask != null) {
                periodicTask.run();
            }
        }
    }
}

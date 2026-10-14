/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RunnerSchedulerTest {

    private static final Duration PERIOD = Duration.ofMinutes(1);

    private final ScheduledThreadPoolExecutor externalScheduler = new ScheduledThreadPoolExecutor(1);

    @AfterEach
    void tearDown() {
        externalScheduler.shutdownNow();
    }

    @Test
    @DisplayName("Creates internal scheduler when external is null and shuts down on close")
    void internalSchedulerLifecycle() {
        RunnerScheduler runnerScheduler = new RunnerScheduler(null);

        assertThat(runnerScheduler.isOwned()).isTrue();
        assertThat(runnerScheduler.service()).isNotNull();
        assertThat((Future<?>) runnerScheduler.task()).isNull();

        runnerScheduler.start(PERIOD, () -> {});

        assertThat((Future<?>) runnerScheduler.task()).isNotNull();
        assertThat(runnerScheduler.task().isCancelled()).isFalse();

        runnerScheduler.close();

        assertThat(runnerScheduler.task().isCancelled()).isTrue();
        assertThat(runnerScheduler.service().isShutdown()).isTrue();
    }

    @Test
    @DisplayName("Uses external scheduler when provided and cancels task on close without shutting down scheduler")
    void externalSchedulerLifecycle() {
        RunnerScheduler runnerScheduler = new RunnerScheduler(externalScheduler);

        assertThat(runnerScheduler.isOwned()).isFalse();
        assertThat(runnerScheduler.service()).isSameAs(externalScheduler);
        assertThat((Future<?>) runnerScheduler.task()).isNull();

        runnerScheduler.start(PERIOD, () -> {});

        assertThat((Future<?>) runnerScheduler.task()).isNotNull();
        assertThat(runnerScheduler.task().isCancelled()).isFalse();
        assertThat(externalScheduler.getQueue()).hasSize(1);

        runnerScheduler.close();

        assertThat(runnerScheduler.task().isCancelled()).isTrue();
        externalScheduler.purge();
        assertThat(externalScheduler.getQueue()).isEmpty();
        assertThat(externalScheduler.isShutdown()).isFalse();
    }

    @Test
    @DisplayName("Does not schedule task when period is null")
    void nullPeriodDoesNotScheduleTask() {
        RunnerScheduler runnerScheduler = new RunnerScheduler(externalScheduler);

        runnerScheduler.start(null, () -> {});

        assertThat((Future<?>) runnerScheduler.task()).isNull();
        assertThat(externalScheduler.getQueue()).isEmpty();

        runnerScheduler.close();
    }

    @Test
    @DisplayName("Throws when callback is null for non-null period")
    void nullCallbackThrowsException() {
        RunnerScheduler runnerScheduler = new RunnerScheduler(externalScheduler);

        assertThatThrownBy(() -> runnerScheduler.start(PERIOD, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("callback cannot be null");

        runnerScheduler.close();
    }

    @Test
    @DisplayName("Replaces previous task when start is called multiple times")
    void replacesPreviousTaskOnStart() {
        RunnerScheduler runnerScheduler = new RunnerScheduler(externalScheduler);

        AtomicInteger counter = new AtomicInteger();
        runnerScheduler.start(PERIOD, counter::incrementAndGet);
        ScheduledFuture<?> firstTask = runnerScheduler.task();
        assertThat((Future<?>) firstTask).isNotNull();

        runnerScheduler.start(PERIOD, counter::incrementAndGet);
        ScheduledFuture<?> secondTask = runnerScheduler.task();
        assertThat((Future<?>) secondTask).isNotNull();
        assertThat((Future<?>) secondTask).isNotSameAs(firstTask);
        assertThat(firstTask.isCancelled()).isTrue();
        assertThat(secondTask.isCancelled()).isFalse();

        runnerScheduler.close();
        assertThat(secondTask.isCancelled()).isTrue();
    }

    @Test
    @DisplayName("Close is safe and idempotent when called multiple times or without start")
    void closeIsIdempotent() {
        RunnerScheduler runnerScheduler = new RunnerScheduler(null);

        runnerScheduler.close();
        runnerScheduler.close();

        assertThat(runnerScheduler.service().isShutdown()).isTrue();
    }
}

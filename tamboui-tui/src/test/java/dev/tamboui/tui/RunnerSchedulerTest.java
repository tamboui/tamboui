/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RunnerSchedulerTest {

    @Test
    @DisplayName("creates owned scheduler when external scheduler is null and shuts down on close")
    void createsOwnedSchedulerWhenNull() {
        RunnerScheduler rs = new RunnerScheduler(null);
        assertThat(rs.isOwned()).isTrue();
        assertThat(rs.service()).isNotNull();
        assertThat(rs.service().isShutdown()).isFalse();

        rs.close();
        assertThat(rs.service().isShutdown()).isTrue();
    }

    @Test
    @DisplayName("uses external scheduler without ownership and does not shut down on close")
    void usesExternalSchedulerWithoutOwnership() {
        ScheduledThreadPoolExecutor external = new ScheduledThreadPoolExecutor(1);
        try {
            RunnerScheduler rs = new RunnerScheduler(external);
            assertThat(rs.isOwned()).isFalse();
            assertThat(rs.service()).isSameAs(external);

            rs.close();
            assertThat(external.isShutdown()).isFalse();
        } finally {
            external.shutdownNow();
        }
    }

    @Test
    @DisplayName("throws IllegalStateException when external scheduler is already shut down")
    void throwsWhenExternalSchedulerIsShutdown() {
        ScheduledThreadPoolExecutor external = new ScheduledThreadPoolExecutor(1);
        external.shutdown();

        assertThatThrownBy(() -> new RunnerScheduler(external))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("shut down");
    }

    @Test
    @DisplayName("start schedules periodic task when period is not null")
    void startSchedulesPeriodicTask() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);
        try (RunnerScheduler rs = new RunnerScheduler(null)) {
            rs.start(Duration.ofMillis(5), latch::countDown);

            assertThat((Future<?>) rs.task()).isNotNull();
            assertThat(rs.task().isCancelled()).isFalse();
            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("start with null period does not schedule a task")
    void startWithNullPeriodDoesNotScheduleTask() {
        try (RunnerScheduler rs = new RunnerScheduler(null)) {
            rs.start(null, () -> {});
            assertThat((Future<?>) rs.task()).isNull();
        }
    }

    @Test
    @DisplayName("calling start again replaces and cancels previous task")
    void startReplacesAndCancelsPreviousTask() {
        try (RunnerScheduler rs = new RunnerScheduler(null)) {
            rs.start(Duration.ofMinutes(1), () -> {});
            ScheduledFuture<?> task1 = rs.task();
            assertThat((Future<?>) task1).isNotNull();

            rs.start(Duration.ofMinutes(1), () -> {});
            ScheduledFuture<?> task2 = rs.task();

            assertThat(task1.isCancelled()).isTrue();
            assertThat((Future<?>) task2).isNotSameAs(task1);
            assertThat(task2.isCancelled()).isFalse();
        }
    }

    @Test
    @DisplayName("close cancels periodic task on external shared scheduler")
    void closeCancelsPeriodicTaskOnSharedScheduler() {
        ScheduledThreadPoolExecutor external = new ScheduledThreadPoolExecutor(1);
        try {
            RunnerScheduler rs = new RunnerScheduler(external);
            rs.start(Duration.ofMinutes(1), () -> {});

            assertThat(external.getQueue()).hasSize(1);
            assertThat(rs.task().isCancelled()).isFalse();

            rs.close();
            external.purge();

            assertThat(rs.task().isCancelled()).isTrue();
            assertThat(external.getQueue()).isEmpty();
            assertThat(external.isShutdown()).isFalse();
        } finally {
            external.shutdownNow();
        }
    }

    @Test
    @DisplayName("close is safe to call multiple times")
    void closeIsIdempotent() {
        RunnerScheduler rs = new RunnerScheduler(null);
        rs.start(Duration.ofMinutes(1), () -> {});

        rs.close();
        rs.close();

        assertThat(rs.service().isShutdown()).isTrue();
    }
}

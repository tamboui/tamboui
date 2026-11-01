/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RunnerSchedulerTest {

    @Test
    @DisplayName("service() returns underlying executor service")
    void serviceReturnsUnderlyingScheduler() {
        try (RunnerScheduler rs = new RunnerScheduler(null)) {
            assertThat(rs.service()).isNotNull();
            assertThat(rs.isOwned()).isTrue();
        }
    }

    @Test
    @DisplayName("isOwned() reflects ownership correctly")
    void isOwnedReflectsOwnership() {
        ScheduledThreadPoolExecutor ext = new ScheduledThreadPoolExecutor(1);
        try {
            try (RunnerScheduler owned = new RunnerScheduler(null)) {
                assertThat(owned.isOwned()).isTrue();
            }

            try (RunnerScheduler external = new RunnerScheduler(ext)) {
                assertThat(external.isOwned()).isFalse();
            }

            try (RunnerScheduler explicitOwned = new RunnerScheduler(ext, true)) {
                assertThat(explicitOwned.isOwned()).isTrue();
            }

            try (RunnerScheduler explicitUnowned = new RunnerScheduler(ext, false)) {
                assertThat(explicitUnowned.isOwned()).isFalse();
            }
        } finally {
            ext.shutdownNow();
        }
    }

    @Test
    @DisplayName("start() schedules periodic task with positive period")
    void startSchedulesPeriodicTask() {
        ScheduledThreadPoolExecutor ext = new ScheduledThreadPoolExecutor(1);
        try (RunnerScheduler rs = new RunnerScheduler(ext)) {
            AtomicBoolean ran = new AtomicBoolean(false);
            rs.start(Duration.ofMinutes(1), () -> ran.set(true));

            Future<?> task = rs.task();
            assertThat(task).isNotNull();
            assertThat(task.isCancelled()).isFalse();
            assertThat(ext.getQueue()).hasSize(1);
        } finally {
            ext.shutdownNow();
        }
    }

    @Test
    @DisplayName("start() with null or non-positive period does not schedule task")
    void startWithNullOrNonPositivePeriodDoesNotSchedule() {
        ScheduledThreadPoolExecutor ext = new ScheduledThreadPoolExecutor(1);
        try (RunnerScheduler rs = new RunnerScheduler(ext)) {
            rs.start(null, () -> {});
            assertThat((Object) rs.task()).isNull();

            rs.start(Duration.ZERO, () -> {});
            assertThat((Object) rs.task()).isNull();

            rs.start(Duration.ofMillis(-10), () -> {});
            assertThat((Object) rs.task()).isNull();

            rs.start(Duration.ofMinutes(1), null);
            assertThat((Object) rs.task()).isNull();
        } finally {
            ext.shutdownNow();
        }
    }

    @Test
    @DisplayName("start() cancels previously scheduled task before scheduling new one")
    void startCancelsPreviousTask() {
        ScheduledThreadPoolExecutor ext = new ScheduledThreadPoolExecutor(1);
        try (RunnerScheduler rs = new RunnerScheduler(ext)) {
            rs.start(Duration.ofMinutes(1), () -> {});
            Future<?> firstTask = rs.task();
            assertThat(firstTask).isNotNull();

            rs.start(Duration.ofMinutes(2), () -> {});
            Future<?> secondTask = rs.task();
            assertThat(secondTask).isNotNull();
            assertThat(secondTask).isNotSameAs(firstTask);

            assertThat(firstTask.isCancelled()).isTrue();
            assertThat(secondTask.isCancelled()).isFalse();
        } finally {
            ext.shutdownNow();
        }
    }

    @Test
    @DisplayName("close() cancels periodic task on external scheduler without shutting it down")
    void closeCancelsTaskWithoutShuttingDownExternalScheduler() {
        ScheduledThreadPoolExecutor ext = new ScheduledThreadPoolExecutor(1);
        try {
            RunnerScheduler rs = new RunnerScheduler(ext);
            rs.start(Duration.ofMinutes(1), () -> {});
            Future<?> task = rs.task();
            assertThat(task).isNotNull();

            rs.close();

            assertThat(task.isCancelled()).isTrue();
            assertThat((Object) rs.task()).isNull();
            assertThat(ext.isShutdown()).isFalse();
        } finally {
            ext.shutdownNow();
        }
    }

    @Test
    @DisplayName("close() shuts down owned scheduler")
    void closeShutsDownOwnedScheduler() {
        RunnerScheduler rs = new RunnerScheduler(null);
        assertThat(rs.service().isShutdown()).isFalse();

        rs.close();

        assertThat(rs.service().isShutdown()).isTrue();
    }

    @Test
    @DisplayName("close() is idempotent")
    void closeIsIdempotent() {
        RunnerScheduler rs = new RunnerScheduler(null);
        rs.start(Duration.ofMinutes(1), () -> {});

        rs.close();
        rs.close();

        assertThat(rs.service().isShutdown()).isTrue();
        assertThat((Object) rs.task()).isNull();
    }
}

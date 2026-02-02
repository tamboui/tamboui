/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UiRunnableTest {

    @Test
    @DisplayName("UiRunnable implements Event interface")
    void implementsEventInterface() {
        UiRunnable runnable = new UiRunnable(() -> {});
        assertThat(runnable).isInstanceOf(Event.class);
    }

    @Test
    @DisplayName("run executes the enclosed action")
    void runExecutesAction() {
        AtomicBoolean executed = new AtomicBoolean(false);
        UiRunnable runnable = new UiRunnable(() -> executed.set(true));

        runnable.run();

        assertThat(executed.get()).isTrue();
    }

    @Test
    @DisplayName("run can be called multiple times")
    void runCanBeCalledMultipleTimes() {
        AtomicInteger counter = new AtomicInteger(0);
        UiRunnable runnable = new UiRunnable(counter::incrementAndGet);

        runnable.run();
        runnable.run();
        runnable.run();

        assertThat(counter.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("run propagates exceptions from the action")
    void runPropagatesExceptions() {
        UiRunnable runnable = new UiRunnable(() -> {
            throw new RuntimeException("test error");
        });

        assertThatThrownBy(runnable::run)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("test error");
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TickEventTest {

    @Test
    @DisplayName("of creates tick event with frame count and elapsed time")
    void ofCreatesTickEvent() {
        Duration elapsed = Duration.ofMillis(16);
        TickEvent event = TickEvent.of(42, elapsed);
        assertThat(event.frameCount()).isEqualTo(42);
        assertThat(event.elapsed()).isEqualTo(elapsed);
    }

    @Test
    @DisplayName("tick event is an Event")
    void tickEventIsEvent() {
        Event event = TickEvent.of(1, Duration.ofMillis(10));
        assertThat(event).isInstanceOf(TickEvent.class);
    }

    @Test
    @DisplayName("frame count starts at expected values")
    void frameCountValues() {
        assertThat(TickEvent.of(0, Duration.ZERO).frameCount()).isEqualTo(0);
        assertThat(TickEvent.of(1000, Duration.ZERO).frameCount()).isEqualTo(1000);
        assertThat(TickEvent.of(Long.MAX_VALUE, Duration.ZERO).frameCount())
                .isEqualTo(Long.MAX_VALUE);
    }

    @Test
    @DisplayName("elapsed time can vary")
    void elapsedTimeVariations() {
        assertThat(TickEvent.of(1, Duration.ZERO).elapsed()).isEqualTo(Duration.ZERO);
        assertThat(TickEvent.of(1, Duration.ofSeconds(1)).elapsed())
                .isEqualTo(Duration.ofSeconds(1));
        assertThat(TickEvent.of(1, Duration.ofNanos(100)).elapsed())
                .isEqualTo(Duration.ofNanos(100));
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.animation.Interpolation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for EffectTimer.
 */
class EffectTimerTest {

    /** A night of laptop sleep: 288,000 cycles of a 100 ms timer. */
    private static final long EIGHT_HOURS_MS = 8L * 60 * 60 * 1000;

    @Test
    @DisplayName("a looping timer keeps its place in the cycle after a pause of many cycles")
    void loopTimerAfterLongPause() {
        EffectTimer timer = EffectTimer.fromMs(100, Interpolation.Linear).loopMode(LoopMode.LOOP);
        timer.process(TFxDuration.fromMillis(30));

        // Ends twice, then runs 80 ms into the third cycle
        assertThat(timer.process(TFxDuration.fromMillis(250))).isNull();
        assertThat(timer.remaining().asMillis()).isEqualTo(20);

        assertThat(timer.process(TFxDuration.fromMillis(EIGHT_HOURS_MS))).isNull();
        assertThat(timer.remaining().asMillis()).isEqualTo(20);
        assertThat(timer.isReversed()).isFalse();
        assertThat(timer.done()).isFalse();
    }

    @Test
    @DisplayName("a ping-pong timer keeps its place and direction after a pause of many cycles")
    void pingPongTimerAfterLongPause() {
        EffectTimer timer = EffectTimer.fromMs(100, Interpolation.Linear).loopMode(LoopMode.PING_PONG);
        timer.process(TFxDuration.fromMillis(30));

        // Reaches the end, turns, and runs 50 ms back
        assertThat(timer.process(TFxDuration.fromMillis(120))).isNull();
        assertThat(timer.isReversed()).isTrue();
        assertThat(timer.remaining().asMillis()).isEqualTo(50);

        // An even number of cycles keeps the direction, an odd number turns it
        assertThat(timer.process(TFxDuration.fromMillis(EIGHT_HOURS_MS))).isNull();
        assertThat(timer.isReversed()).isTrue();
        assertThat(timer.remaining().asMillis()).isEqualTo(50);

        assertThat(timer.process(TFxDuration.fromMillis(EIGHT_HOURS_MS + 100))).isNull();
        assertThat(timer.isReversed()).isFalse();
        assertThat(timer.remaining().asMillis()).isEqualTo(50);
        assertThat(timer.done()).isFalse();
    }
}

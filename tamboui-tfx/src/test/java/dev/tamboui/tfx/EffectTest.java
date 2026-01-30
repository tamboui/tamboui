/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Effect class.
 */
class EffectTest {

    @Nested
    @DisplayName("copy()")
    class CopyTests {

        @Test
        @DisplayName("copy creates independent effect instance")
        void copyCreatesIndependentInstance() {
            Effect original = Fx.fadeToFg(Color.CYAN, 500, Interpolation.Linear);

            Effect copy = original.copy();

            assertThat(copy).isNotSameAs(original);
            assertThat(copy.name()).isEqualTo(original.name());
        }

        @Test
        @DisplayName("copy has independent timer state")
        void copyHasIndependentTimerState() {
            Effect original = Fx.fadeToFg(Color.CYAN, 100, Interpolation.Linear);
            Effect copy = original.copy();

            // Advance original
            Rect area = new Rect(0, 0, 10, 5);
            Buffer buffer = Buffer.empty(area);
            original.process(TFxDuration.fromMillis(50), buffer, area);

            // Original is halfway done, copy should still be at start
            assertThat(original.running()).isTrue();
            assertThat(copy.running()).isTrue();

            // Advance original to completion
            original.process(TFxDuration.fromMillis(100), buffer, area);
            assertThat(original.done()).isTrue();

            // Copy should still be running (not advanced)
            assertThat(copy.done()).isFalse();
        }

        @Test
        @DisplayName("copy preserves loop mode")
        void copyPreservesLoopMode() {
            Effect original = Fx.fadeToFg(Color.CYAN, 100, Interpolation.Linear).pingPong();

            Effect copy = original.copy();

            // Both should never complete due to ping-pong
            Rect area = new Rect(0, 0, 10, 5);
            Buffer buffer = Buffer.empty(area);

            // Advance well past duration
            copy.process(TFxDuration.fromMillis(500), buffer, area);

            // Ping-pong effects never complete
            assertThat(copy.done()).isFalse();
            assertThat(copy.running()).isTrue();
        }

        @Test
        @DisplayName("copy preserves area")
        void copyPreservesArea() {
            Rect customArea = new Rect(5, 5, 20, 10);
            Effect original = Fx.fadeToFg(Color.CYAN, 500, Interpolation.Linear)
                    .withArea(customArea);

            Effect copy = original.copy();

            // The copy should have the same area as original
            assertThat(copy.shader().area()).isEqualTo(customArea);
        }
    }

    @Nested
    @DisplayName("loop modes")
    class LoopModeTests {

        @Test
        @DisplayName("loop() creates continuously looping effect")
        void loopCreatesLoopingEffect() {
            Effect effect = Fx.fadeToFg(Color.CYAN, 100, Interpolation.Linear).loop();

            Rect area = new Rect(0, 0, 10, 5);
            Buffer buffer = Buffer.empty(area);

            // Advance past several complete cycles
            effect.process(TFxDuration.fromMillis(350), buffer, area);

            // Should still be running
            assertThat(effect.done()).isFalse();
            assertThat(effect.running()).isTrue();
        }

        @Test
        @DisplayName("pingPong() creates ping-pong effect")
        void pingPongCreatesPingPongEffect() {
            Effect effect = Fx.fadeToFg(Color.CYAN, 100, Interpolation.Linear).pingPong();

            Rect area = new Rect(0, 0, 10, 5);
            Buffer buffer = Buffer.empty(area);

            // Advance past several complete cycles
            effect.process(TFxDuration.fromMillis(350), buffer, area);

            // Should still be running
            assertThat(effect.done()).isFalse();
            assertThat(effect.running()).isTrue();
        }
    }
}

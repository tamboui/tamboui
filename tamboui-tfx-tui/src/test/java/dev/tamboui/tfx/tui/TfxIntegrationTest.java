/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.tui;

import java.time.Duration;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.animation.Interpolation;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tfx.Effect;
import dev.tamboui.tfx.EffectTimer;
import dev.tamboui.tfx.effects.FadeShader;
import dev.tamboui.tui.EventHandler;
import dev.tamboui.tui.RenderClock;
import dev.tamboui.tui.Renderer;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.TickEvent;

import static org.assertj.core.api.Assertions.assertThat;

class TfxIntegrationTest {

    private final Frame frame = Frame.forTesting(Buffer.empty(new Rect(0, 0, 10, 2)));

    @Test
    @DisplayName("effects advance by real time, so redraws between ticks do not make them run faster")
    void redrawsBetweenTicksDoNotAdvanceEffects() {
        FakeClock clock = new FakeClock(0L);
        EffectTimer timer = EffectTimer.fromMs(1000, Interpolation.Linear);
        TfxIntegration tfx = new TfxIntegration(new RenderClock(clock));
        tfx.addEffect(Effect.of(FadeShader.fadeTo(Color.WHITE, Color.CYAN, timer)));
        EventHandler handler = tfx.wrapHandler((event, runner) -> false);
        Renderer renderer = tfx.wrapRenderer(f -> {
        });

        // First tick: the first render measures no time yet
        handler.handle(TickEvent.of(1), null);
        renderer.render(frame);
        assertThat(timer.remaining().asMillis()).isEqualTo(1000);

        // One tick period of real time passes before the next render
        clock.advance(Duration.ofMillis(16));
        handler.handle(TickEvent.of(2), null);
        renderer.render(frame);
        assertThat(timer.remaining().asMillis()).isEqualTo(984);

        // Key presses force redraws, but no real time passes: the effect does not advance
        for (int i = 0; i < 4; i++) {
            assertThat(handler.handle(KeyEvent.ofChar('x'), null)).isTrue();
            renderer.render(frame);
        }
        assertThat(timer.remaining().asMillis()).isEqualTo(984);

        clock.advance(Duration.ofMillis(16));
        handler.handle(TickEvent.of(3), null);
        renderer.render(frame);
        assertThat(timer.remaining().asMillis()).isEqualTo(968);
    }

    /**
     * A clock that only moves when the test advances it.
     */
    private static final class FakeClock implements LongSupplier {
        private long nanos;

        FakeClock(long nanos) {
            this.nanos = nanos;
        }

        void advance(Duration duration) {
            nanos += duration.toNanos();
        }

        @Override
        public long getAsLong() {
            return nanos;
        }
    }
}

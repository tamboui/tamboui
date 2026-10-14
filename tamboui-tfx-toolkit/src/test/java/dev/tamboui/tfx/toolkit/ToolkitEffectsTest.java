/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tfx.toolkit;

import java.time.Duration;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.animation.Interpolation;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.StyledAreaRegistry;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tfx.Effect;
import dev.tamboui.tfx.EffectTimer;
import dev.tamboui.tfx.effects.FadeShader;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.app.ToolkitPostRenderProcessor;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.focus.FocusManager;
import dev.tamboui.tui.EventHandler;
import dev.tamboui.tui.RenderClock;
import dev.tamboui.tui.Renderer;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.TickEvent;

import static org.assertj.core.api.Assertions.assertThat;

class ToolkitEffectsTest extends AbstractElementTest {

    private final Frame frame = Frame.forTesting(Buffer.empty(new Rect(0, 0, 10, 2)));
    private final ElementRegistry elementRegistry = new ElementRegistry();
    private final StyledAreaRegistry styledAreaRegistry = StyledAreaRegistry.create();
    private final FocusManager focusManager = new FocusManager();

    @Test
    @DisplayName("wrapRenderer advances effects by real time, so redraws between ticks do not speed them up")
    void redrawsBetweenTicksDoNotAdvanceEffects() {
        FakeClock clock = new FakeClock(0L);
        EffectTimer timer = EffectTimer.fromMs(1000, Interpolation.Linear);
        ToolkitEffects effects = new ToolkitEffects(new RenderClock(clock));
        effects.addGlobalEffect(Effect.of(FadeShader.fadeTo(Color.WHITE, Color.CYAN, timer)));
        EventHandler handler = effects.wrapHandler((event, runner) -> false);
        Renderer renderer = effects.wrapRenderer(f -> {
        }, elementRegistry, styledAreaRegistry, focusManager);

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

    @Test
    @DisplayName("asPostRenderProcessor advances effects by exactly the elapsed it is given")
    void postRenderProcessorAdvancesByGivenElapsed() {
        EffectTimer timer = EffectTimer.fromMs(1000, Interpolation.Linear);
        ToolkitEffects effects = new ToolkitEffects();
        effects.addGlobalEffect(Effect.of(FadeShader.fadeTo(Color.WHITE, Color.CYAN, timer)));
        ToolkitPostRenderProcessor processor = effects.asPostRenderProcessor();

        processor.process(frame, elementRegistry, styledAreaRegistry, focusManager, Duration.ofMillis(40));
        assertThat(timer.remaining().asMillis()).isEqualTo(960);

        // Zero elapsed (a redraw with no time passed) does not advance the effect
        processor.process(frame, elementRegistry, styledAreaRegistry, focusManager, Duration.ZERO);
        assertThat(timer.remaining().asMillis()).isEqualTo(960);

        processor.process(frame, elementRegistry, styledAreaRegistry, focusManager, Duration.ofMillis(60));
        assertThat(timer.remaining().asMillis()).isEqualTo(900);
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

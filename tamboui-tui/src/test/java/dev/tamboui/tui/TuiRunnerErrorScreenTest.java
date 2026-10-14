/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Size;
import dev.tamboui.terminal.TestBackend;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.pilot.TuiTestRunner;

import static org.assertj.core.api.Assertions.assertThat;

class TuiRunnerErrorScreenTest {

    @Test
    @DisplayName("The error screen quits on q with bindings that quit on Ctrl+C only")
    void errorScreenQuitsOnQWithEmacsBindings() throws Exception {
        CountDownLatch rendered = new CountDownLatch(1);
        TuiConfig config = TuiConfig.builder()
                .rawMode(false)
                .alternateScreen(false)
                .hideCursor(false)
                .shutdownHook(false)
                .pollTimeout(Duration.ofMillis(10))
                .noTick()
                .backend(new TestBackend(60, 12))
                .bindings(BindingSets.emacs())
                .build();
        Renderer failingRenderer = frame -> {
            rendered.countDown();
            throw new IllegalStateException("render failed");
        };

        try (TuiTestRunner test = TuiTestRunner.runTest((event, runner) -> false, failingRenderer,
                new Size(60, 12), config)) {
            assertThat(rendered.await(5, TimeUnit.SECONDS)).as("rendered").isTrue();

            test.runner().dispatch(KeyEvent.ofChar('q', BindingSets.emacs()));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (test.runner().isRunning() && System.nanoTime() < deadline) {
                test.pilot().pause(Duration.ofMillis(10));
            }
            assertThat(test.runner().isRunning()).as("running after q on the error screen").isFalse();
        }
    }
}

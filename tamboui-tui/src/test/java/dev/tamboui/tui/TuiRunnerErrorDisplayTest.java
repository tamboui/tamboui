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

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.DiffResult;
import dev.tamboui.layout.Rect;
import dev.tamboui.layout.Size;
import dev.tamboui.terminal.TestBackend;
import dev.tamboui.tui.pilot.TuiTestRunner;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

class TuiRunnerErrorDisplayTest {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 12;

    @Test
    @DisplayName("The error screen cuts a long message and stack trace lines at its right border")
    void errorScreenCutsLongLinesAtRightBorder() throws Exception {
        TuiConfig config = TuiConfig.builder()
                .rawMode(false)
                .alternateScreen(false)
                .hideCursor(false)
                .shutdownHook(false)
                .pollTimeout(Duration.ofMillis(10))
                .noTick()
                .backend(new ScreenBackend(WIDTH, HEIGHT))
                .build();
        Renderer failingRenderer = frame -> {
            throw new IllegalStateException("a message far too long for the error screen to show it whole");
        };

        try (TuiTestRunner test = TuiTestRunner.runTest((event, runner) -> false, failingRenderer,
                new Size(WIDTH, HEIGHT), config)) {
            Buffer screen = ((ScreenBackend) test.backend()).awaitFirstDraw();

            StringBuilder rightBorder = new StringBuilder();
            for (int y = 0; y < HEIGHT; y++) {
                rightBorder.append(screen.get(WIDTH - 1, y).symbol());
            }
            assertThat(rightBorder.toString()).as("right border").isEqualTo("╮││││││││││╯");
            assertThat(screen).hasRow(3, "│Message: a message far too long for th│");
        }
    }

    /** A test backend that keeps the cells of the first frame drawn. */
    private static final class ScreenBackend extends TestBackend {

        private final Buffer screen;
        private final CountDownLatch drawn = new CountDownLatch(1);

        ScreenBackend(int width, int height) {
            super(width, height);
            this.screen = Buffer.empty(new Rect(0, 0, width, height));
        }

        @Override
        public void draw(DiffResult diff) {
            if (drawn.getCount() == 0) {
                return;
            }
            for (int i = 0; i < diff.size(); i++) {
                screen.set(diff.getX(i), diff.getY(i), diff.getCell(i));
            }
            drawn.countDown();
        }

        Buffer awaitFirstDraw() throws InterruptedException {
            assertThat(drawn.await(5, TimeUnit.SECONDS)).as("error screen drawn").isTrue();
            return screen;
        }
    }
}

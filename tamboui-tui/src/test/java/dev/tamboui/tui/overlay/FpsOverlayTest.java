/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.overlay;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for FpsOverlay.
 */
class FpsOverlayTest {

    @Test
    @DisplayName("Overlay is initially not visible")
    void initiallyNotVisible() {
        FpsOverlay overlay = new FpsOverlay(Duration.ofMillis(100), Duration.ofMillis(100));
        assertThat(overlay.isVisible()).isFalse();
    }

    @Test
    @DisplayName("toggle() changes visibility")
    void toggleChangesVisibility() {
        FpsOverlay overlay = new FpsOverlay(Duration.ofMillis(100), Duration.ofMillis(100));

        overlay.toggle();
        assertThat(overlay.isVisible()).isTrue();

        overlay.toggle();
        assertThat(overlay.isVisible()).isFalse();
    }

    @Test
    @DisplayName("recordFrame() records FPS samples")
    void recordFrameRecordsFps() throws InterruptedException {
        FpsOverlay overlay = new FpsOverlay(Duration.ofMillis(100), Duration.ofMillis(100));

        // Simulate frame renders with delays
        for (int i = 0; i < 5; i++) {
            overlay.recordFrame();
            Thread.sleep(10); // Small delay between frames
        }

        // Should work without throwing
        overlay.toggle();
        assertThat(overlay.isVisible()).isTrue();
    }

    @Test
    @DisplayName("render() does nothing when not visible")
    void renderDoesNothingWhenNotVisible() {
        FpsOverlay overlay = new FpsOverlay(Duration.ofMillis(100), Duration.ofMillis(100));

        Rect area = new Rect(0, 0, 40, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        // Should not throw and buffer should remain empty
        overlay.render(frame, area);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("render() draws overlay when visible")
    void renderDrawsOverlayWhenVisible() throws InterruptedException {
        FpsOverlay overlay = new FpsOverlay(Duration.ofMillis(100), Duration.ofMillis(100));

        // Record some frames
        overlay.recordFrame();
        Thread.sleep(10);
        overlay.recordFrame();
        overlay.toggle();

        Rect area = new Rect(0, 0, 40, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        overlay.render(frame, area);

        // The overlay should be in the top-right corner
        // Check that something was rendered (rounded border corner)
        int overlayX = area.width() - 22 - 1; // OVERLAY_WIDTH = 22, margin = 1
        assertThat(buffer.get(overlayX, 1).symbol()).isEqualTo("╭");
    }

    @Test
    @DisplayName("render() handles empty area gracefully")
    void renderHandlesEmptyArea() {
        FpsOverlay overlay = new FpsOverlay(Duration.ofMillis(100), Duration.ofMillis(100));
        overlay.toggle();

        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 10));
        Frame frame = Frame.forTesting(buffer);

        // Should not throw
        overlay.render(frame, emptyArea);
    }

    @Test
    @DisplayName("FpsOverlay works with null tick rate")
    void worksWithNullTickRate() {
        FpsOverlay overlay = new FpsOverlay(Duration.ofMillis(100), null);
        overlay.toggle();

        Rect area = new Rect(0, 0, 40, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        // Should not throw with null tick rate
        overlay.render(frame, area);
        assertThat(overlay.isVisible()).isTrue();
    }

    @Test
    @DisplayName("FPS calculation uses actual frame timing")
    void fpsCalculationUsesActualFrameTiming() throws InterruptedException {
        FpsOverlay overlay = new FpsOverlay(Duration.ofMillis(100), Duration.ofMillis(100));

        // Record multiple frames with ~100ms intervals (roughly 10 FPS)
        for (int i = 0; i < 5; i++) {
            overlay.recordFrame();
            Thread.sleep(100);
        }

        // Should not throw
        overlay.toggle();

        Rect area = new Rect(0, 0, 40, 10);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        overlay.render(frame, area);
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.overlay;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.widgets.Clear;
import dev.tamboui.widgets.block.Block;
import dev.tamboui.widgets.block.BorderType;
import dev.tamboui.widgets.block.Borders;
import dev.tamboui.widgets.block.Title;
import dev.tamboui.widgets.paragraph.Paragraph;

import java.time.Duration;

/// Debug overlay that displays performance metrics and system information.
///
///
///
/// Shows the backend name, actual frame rate (computed from render timing),
/// configured poll timeout, and tick rate.
/// Toggle visibility with CTRL+SHIFT+F12.
public final class DebugOverlay {

    private static final int OVERLAY_HEIGHT = 7;
    private static final int MIN_OVERLAY_WIDTH = 18;
    private final int overlayWidth;

    private boolean visible;
    private final String backendName;
    private final Duration pollTimeout;
    private final Duration tickRate;
    private final long startTimeNanos;
    private long renderCount;

    /// Creates a new debug overlay.
    ///
    /// @param backendName the name of the backend being used
    /// @param pollTimeout the configured poll timeout
    /// @param tickRate the configured tick rate (may be null if ticks disabled)
    public DebugOverlay(String backendName, Duration pollTimeout, Duration tickRate) {
        this.backendName = backendName;
        this.pollTimeout = pollTimeout;
        this.tickRate = tickRate;
        this.visible = false;
        this.startTimeNanos = System.nanoTime();
        // Width = "Backend: " (9) + backend name + border (2)
        this.overlayWidth = Math.max(MIN_OVERLAY_WIDTH, 9 + backendName.length() + 2);
    }

    /// Toggles the visibility of the debug overlay.
    public void toggle() {
        this.visible = !this.visible;
    }

    /// Returns whether the overlay is currently visible.
    ///
    /// @return true if visible
    public boolean isVisible() {
        return visible;
    }

    /// Records a frame render for FPS calculation.
    ///
    ///
    ///
    /// This should be called once per actual frame render to compute
    /// the true frame rate.
    public void recordFrame() {
        renderCount++;
    }

    /// Computes runtime in seconds.
    private double computeRuntimeSeconds() {
        return (System.nanoTime() - startTimeNanos) / 1_000_000_000.0;
    }

    /// Computes actual average FPS over the entire runtime.
    private double computeFps() {
        double runtime = computeRuntimeSeconds();
        return runtime > 0 ? renderCount / runtime : 0;
    }

    /// Computes the color for the FPS display based on performance ratio.
    ///
    ///
    ///
    /// Returns RED if FPS is less than 50% of theoretical,
    /// YELLOW (orange) if less than 90%, GREEN otherwise.
    private Color computeFpsColor(double actualFps, double theoreticalFps) {
        if (theoreticalFps <= 0) {
            return Color.GREEN;
        }
        double ratio = actualFps / theoreticalFps;
        if (ratio < 0.5) {
            return Color.RED;
        } else if (ratio < 0.9) {
            return Color.YELLOW;
        }
        return Color.GREEN;
    }

    /// Renders the debug overlay in the top-right corner.
    ///
    /// @param frame the frame to render to
    /// @param area the total available area
    public void render(Frame frame, Rect area) {
        if (!visible || area.isEmpty()) {
            return;
        }

        // Position in top-right corner with margin
        int x = area.x() + area.width() - overlayWidth - 1;
        int y = area.y() + 1;

        // Ensure we don't go off-screen
        if (x < area.x()) {
            x = area.x();
        }

        int width = Math.min(overlayWidth, area.width());
        int height = Math.min(OVERLAY_HEIGHT, area.height());

        Rect overlayArea = new Rect(x, y, width, height);

        // Clear the area first
        frame.renderWidget(Clear.INSTANCE, overlayArea);

        // Build content
        double runtime = computeRuntimeSeconds();
        double fps = computeFps();
        double theoreticalFps = tickRate != null ? 1000.0 / tickRate.toMillis() : 0;

        // Determine FPS color based on performance ratio
        Color fpsColor = computeFpsColor(fps, theoreticalFps);

        String backendLine = String.format("Backend: %s", backendName);
        String runtimeLine = String.format("Runtime: %.1fs", runtime);
        String fpsLine = String.format("FPS: %.1f", fps);
        String pollLine = String.format("Poll: %dms", pollTimeout.toMillis());
        String tickLine = tickRate != null
                ? String.format("Tick: %dms", tickRate.toMillis())
                : "Tick: disabled";

        // Create block with rounded border
        Block block = Block.builder()
                .borders(Borders.ALL)
                .borderType(BorderType.ROUNDED)
                .borderStyle(Style.EMPTY.fg(Color.DARK_GRAY))
                .title(Title.from(Line.from(Span.styled("Debug", Style.EMPTY.fg(Color.CYAN).bold()))))
                .build();

        frame.renderWidget(block, overlayArea);

        // Get inner area for content
        Rect innerArea = block.inner(overlayArea);
        if (innerArea.isEmpty()) {
            return;
        }

        // Render debug text
        Text content = Text.from(
                Line.from(Span.styled(backendLine, Style.EMPTY.fg(Color.MAGENTA))),
                Line.from(Span.styled(runtimeLine, Style.EMPTY.fg(Color.WHITE))),
                Line.from(Span.styled(fpsLine, Style.EMPTY.fg(fpsColor).bold())),
                Line.from(Span.styled(pollLine, Style.EMPTY.fg(Color.GRAY))),
                Line.from(Span.styled(tickLine, Style.EMPTY.fg(Color.GRAY)))
        );

        Paragraph paragraph = Paragraph.builder()
                .text(content)
                .build();

        frame.renderWidget(paragraph, innerArea);
    }
}


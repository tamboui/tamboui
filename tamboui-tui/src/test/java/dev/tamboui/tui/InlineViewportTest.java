/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.inline.InlineDisplay;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.TestBackend;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for InlineViewport content height behavior.
 */
class InlineViewportTest {

    private TestBackend backend;

    @BeforeEach
    void setUp() {
        backend = new TestBackend(80, 24);
    }

    @Test
    @DisplayName("setContentHeight(0) causes draw to produce no visible content")
    void setContentHeightZero_producesNoContent() throws Exception {
        InlineDisplay display = InlineDisplay.withBackend(10, 80, backend);
        InlineViewport viewport = new InlineViewport(display);

        // Collapse viewport to 0
        viewport.setContentHeight(0);

        backend.reset();
        viewport.draw(frame -> {
            // Write content that should NOT be visible
            frame.buffer().setString(0, 0, "Hello", Style.EMPTY);
        });

        // No content should appear in the output since contentHeight is 0
        assertThat(backend.rawOutput()).doesNotContain("Hello");
    }

    @Test
    @DisplayName("draw renders content when contentHeight is positive")
    void positiveContentHeight_rendersContent() throws Exception {
        InlineDisplay display = InlineDisplay.withBackend(10, 80, backend);
        InlineViewport viewport = new InlineViewport(display);

        // Keep viewport at configured height
        viewport.setContentHeight(5);

        backend.reset();
        viewport.draw(frame -> {
            frame.buffer().setString(0, 0, "Hello", Style.EMPTY);
        });

        assertThat(backend.rawOutput()).contains("Hello");
    }

    @Test
    @DisplayName("contentHeight defaults to display height")
    void defaultContentHeight_matchesDisplayHeight() throws Exception {
        InlineDisplay display = InlineDisplay.withBackend(10, 80, backend);
        InlineViewport viewport = new InlineViewport(display);

        // Without calling setContentHeight, draw should render content
        backend.reset();
        viewport.draw(frame -> {
            frame.buffer().setString(0, 0, "Visible", Style.EMPTY);
        });

        assertThat(backend.rawOutput()).contains("Visible");
    }

    @Test
    @DisplayName("setContentHeight grows buffer beyond initial height")
    void setContentHeight_growsBeyondInitialHeight() throws Exception {
        InlineDisplay display = InlineDisplay.withBackend(5, 80, backend);
        InlineViewport viewport = new InlineViewport(display);

        // Grow beyond initial height
        viewport.setContentHeight(10);

        backend.reset();
        viewport.draw(frame -> {
            frame.buffer().setString(0, 9, "Line10", Style.EMPTY);
        });

        assertThat(backend.rawOutput()).contains("Line10");
    }
}

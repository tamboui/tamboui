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
import dev.tamboui.terminal.AnsiStringBuilder;
import dev.tamboui.terminal.TestBackend;
import dev.tamboui.widgets.input.TextInput;
import dev.tamboui.widgets.input.TextInputState;

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

    @Test
    @DisplayName("auto-width displays propagate resize to the viewport area")
    void autoWidthDisplayResize_updatesViewportArea() throws Exception {
        InlineDisplay display = InlineDisplay.withBackend(10, backend);
        InlineViewport viewport = new InlineViewport(display);

        backend.resize(40, 24);

        final int[] renderedWidth = {-1};
        viewport.draw(frame -> renderedWidth[0] = frame.area().width());

        assertThat(renderedWidth[0]).isEqualTo(40);
        assertThat(viewport.width()).isEqualTo(40);
    }

    @Test
    @DisplayName("draw shows the cursor of a text input when it is at the end of the text")
    void drawShowsTextInputCursorAtEndOfText() throws Exception {
        InlineDisplay display = InlineDisplay.withBackend(1, 20, backend);
        InlineViewport viewport = new InlineViewport(display);
        TextInput input = TextInput.builder().build();
        TextInputState state = new TextInputState("abc");

        backend.reset();
        viewport.draw(frame -> input.renderWithCursor(frame.area(), frame.buffer(), state, frame));

        String reversedSpace = AnsiStringBuilder.styleToAnsi(Style.EMPTY.reversed()) + " ";
        assertThat(backend.rawOutput()).contains("abc" + reversedSpace);
    }
}

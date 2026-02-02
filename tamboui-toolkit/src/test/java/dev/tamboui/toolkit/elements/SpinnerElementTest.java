/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.widgets.spinner.SpinnerState;
import dev.tamboui.widgets.spinner.SpinnerStyle;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link SpinnerElement}.
 */
class SpinnerElementTest {

    @Test
    @DisplayName("SpinnerElement fluent API chains correctly")
    void fluentApiChaining() {
        SpinnerElement element = spinner()
                .spinnerStyle(SpinnerStyle.LINE)
                .label("Loading...")
                .cyan()
                .bold();

        assertThat(element).isInstanceOf(SpinnerElement.class);
    }

    @Test
    @DisplayName("spinner() creates default DOTS element")
    void defaultSpinner() {
        SpinnerElement element = spinner();
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("spinner(SpinnerStyle) creates styled spinner")
    void spinnerWithStyle() {
        SpinnerElement element = spinner(SpinnerStyle.ARC);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("spinner(String) creates spinner with label")
    void spinnerWithLabel() {
        SpinnerElement element = spinner("Loading...");
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("spinner(SpinnerStyle, String) creates styled spinner with label")
    void spinnerWithStyleAndLabel() {
        SpinnerElement element = spinner(SpinnerStyle.CIRCLE, "Processing...");
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("SpinnerElement renders to buffer")
    void rendersToBuffer() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        spinner(SpinnerStyle.LINE, "Working...")
                .render(frame, area, RenderContext.empty());

        // Should render something (not all spaces)
        boolean hasContent = false;
        for (int x = 0; x < area.width(); x++) {
            if (!" ".equals(buffer.get(x, 0).symbol())) {
                hasContent = true;
                break;
            }
        }
        assertThat(hasContent).isTrue();
    }

    @Test
    @DisplayName("Empty area does not render")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 1));
        Frame frame = Frame.forTesting(buffer);

        // Should not throw
        spinner("Loading")
                .render(frame, emptyArea, RenderContext.empty());
    }

    @Test
    @DisplayName("Label renders next to spinner")
    void labelRendersNextToSpinner() {
        Rect area = new Rect(0, 0, 30, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        spinner(SpinnerStyle.LINE, "Hello")
                .render(frame, area, RenderContext.empty());

        // Find "Hello" in the buffer
        StringBuilder rendered = new StringBuilder();
        for (int x = 0; x < area.width(); x++) {
            rendered.append(buffer.get(x, 0).symbol());
        }
        assertThat(rendered.toString()).contains("Hello");
    }

    @Test
    @DisplayName("Custom frames override style")
    void customFramesOverrideStyle() {
        SpinnerElement element = spinner()
                .spinnerStyle(SpinnerStyle.LINE)
                .frames("*", "+", "x");

        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("preferredWidth includes spinner and label")
    void preferredWidthIncludesLabel() {
        SpinnerElement element = spinner("Test");
        // DOTS max frame width (1) + space (1) + "Test" (4) = 6
        assertThat(element.preferredWidth()).isEqualTo(6);
    }

    @Test
    @DisplayName("preferredWidth without label is just spinner width")
    void preferredWidthWithoutLabel() {
        SpinnerElement element = spinner();
        // DOTS max frame width = 1
        assertThat(element.preferredWidth()).isEqualTo(1);
    }

    @Test
    @DisplayName("preferredHeight is always 1")
    void preferredHeight() {
        assertThat(spinner().preferredHeight()).isEqualTo(1);
        assertThat(spinner("Loading").preferredHeight()).isEqualTo(1);
    }

    @Test
    @DisplayName("External state is used when set")
    void externalState() {
        SpinnerState state = new SpinnerState(5);
        SpinnerElement element = spinner()
                .state(state);

        assertThat(element).isNotNull();

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        element.render(frame, area, RenderContext.empty());
        // State should have advanced by 1
        assertThat(state.tick()).isEqualTo(6);
    }

    @Test
    @DisplayName("Spinner advances state on each render")
    void advancesStateOnRender() {
        SpinnerState state = new SpinnerState(0);
        SpinnerElement element = spinner().state(state);

        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        element.render(frame, area, RenderContext.empty());
        assertThat(state.tick()).isEqualTo(1);

        element.render(frame, area, RenderContext.empty());
        assertThat(state.tick()).isEqualTo(2);
    }
}

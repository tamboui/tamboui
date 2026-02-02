/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.widgets.wavetext.WaveTextState;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for WaveTextElement.
 */
class WaveTextElementTest {

    @Test
    @DisplayName("WaveTextElement fluent API chains correctly")
    void fluentApiChaining() {
        WaveTextElement element = waveText("Loading...")
            .color(Color.CYAN)
            .dimFactor(0.5)
            .peakWidth(5)
            .speed(1.5)
            .oscillate()
            .inverted();

        assertThat(element).isInstanceOf(WaveTextElement.class);
    }

    @Test
    @DisplayName("waveText() creates element with text")
    void waveTextWithString() {
        WaveTextElement element = waveText("Processing...");
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("waveText() with color creates element")
    void waveTextWithColor() {
        WaveTextElement element = waveText("Loading...", Color.YELLOW);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("preferredWidth() returns 0 for null text")
    void preferredWidth_nullText() {
        WaveTextElement element = waveText("test").text(null);
        assertThat(element.preferredWidth()).isEqualTo(0);
    }

    @Test
    @DisplayName("preferredWidth() returns text length")
    void preferredWidth_withText() {
        WaveTextElement element = waveText("Hello");
        assertThat(element.preferredWidth()).isEqualTo(5);
    }

    @Test
    @DisplayName("preferredWidth() handles empty text")
    void preferredWidth_emptyText() {
        WaveTextElement element = waveText("");
        assertThat(element.preferredWidth()).isEqualTo(0);
    }

    @Test
    @DisplayName("preferredWidth() handles longer text")
    void preferredWidth_longerText() {
        WaveTextElement element = waveText("Loading... Please wait");
        assertThat(element.preferredWidth()).isEqualTo(22);
    }

    @Test
    @DisplayName("preferredWidth() updates when text changes")
    void preferredWidth_textChanges() {
        WaveTextElement element = waveText("Short");
        assertThat(element.preferredWidth()).isEqualTo(5);

        element.text("Much longer text here");
        assertThat(element.preferredWidth()).isEqualTo(21);
    }

    @Test
    @DisplayName("WaveTextElement renders to buffer")
    void rendersToBuffer() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        waveText("Test")
            .color(Color.CYAN)
            .render(frame, area, RenderContext.empty());

        // Should render without error
        assertThat(buffer).isNotNull();
    }

    @Test
    @DisplayName("Empty area does not render")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 3));
        Frame frame = Frame.forTesting(buffer);

        // Should not throw
        waveText("Test").render(frame, emptyArea, RenderContext.empty());
    }

    @Test
    @DisplayName("state() sets wave text state")
    void stateMethod() {
        WaveTextState state = new WaveTextState();
        WaveTextElement element = waveText("Test").state(state);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("dimFactor() sets dim factor")
    void dimFactorMethod() {
        WaveTextElement element = waveText("Test").dimFactor(0.5);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("peakWidth() sets peak width")
    void peakWidthMethod() {
        WaveTextElement element = waveText("Test").peakWidth(5);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("speed() sets animation speed")
    void speedMethod() {
        WaveTextElement element = waveText("Test").speed(2.0);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("peakCount() sets number of peaks")
    void peakCountMethod() {
        WaveTextElement element = waveText("Test").peakCount(2);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("inverted() with boolean sets inversion")
    void invertedBooleanMethod() {
        WaveTextElement element = waveText("Test").inverted(true);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("inverted() shorthand works")
    void invertedShorthandMethod() {
        WaveTextElement element = waveText("Test").inverted();
        assertThat(element).isNotNull();
    }
}

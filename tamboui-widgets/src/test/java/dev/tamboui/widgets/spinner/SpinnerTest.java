/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.spinner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link Spinner}, {@link SpinnerStyle}, and {@link SpinnerState}.
 */
class SpinnerTest {

    // ═══════════════════════════════════════════════════════════════
    // SpinnerStyle tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Each SpinnerStyle has valid non-empty frames")
    void eachStyleHasValidFrames() {
        for (SpinnerStyle style : SpinnerStyle.values()) {
            assertThat(style.frames()).isNotEmpty();
            assertThat(style.frameCount()).isGreaterThan(0);
            for (String frame : style.frames()) {
                assertThat(frame).isNotNull();
                assertThat(frame).isNotEmpty();
            }
        }
    }

    @Test
    @DisplayName("SpinnerStyle.frame() wraps around")
    void frameWrapsAround() {
        SpinnerStyle style = SpinnerStyle.LINE;
        assertThat(style.frame(0)).isEqualTo("-");
        assertThat(style.frame(4)).isEqualTo("-"); // wraps back to first
        assertThat(style.frame(-1)).isEqualTo("/"); // negative wraps
    }

    @Test
    @DisplayName("SpinnerStyle.frames() returns a copy")
    void framesReturnsACopy() {
        SpinnerStyle style = SpinnerStyle.DOTS;
        String[] frames1 = style.frames();
        String[] frames2 = style.frames();
        assertThat(frames1).isNotSameAs(frames2);
        assertThat(frames1).isEqualTo(frames2);
    }

    // ═══════════════════════════════════════════════════════════════
    // SpinnerState tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("SpinnerState starts at 0")
    void stateStartsAtZero() {
        SpinnerState state = new SpinnerState();
        assertThat(state.tick()).isZero();
    }

    @Test
    @DisplayName("SpinnerState starts at initial tick")
    void stateStartsAtInitialTick() {
        SpinnerState state = new SpinnerState(42);
        assertThat(state.tick()).isEqualTo(42);
    }

    @Test
    @DisplayName("SpinnerState advance increments by 1")
    void advanceIncrementsBy1() {
        SpinnerState state = new SpinnerState();
        assertThat(state.advance()).isEqualTo(1);
        assertThat(state.advance()).isEqualTo(2);
        assertThat(state.tick()).isEqualTo(2);
    }

    @Test
    @DisplayName("SpinnerState advance by amount")
    void advanceByAmount() {
        SpinnerState state = new SpinnerState();
        state.advance(5);
        assertThat(state.tick()).isEqualTo(5);
    }

    @Test
    @DisplayName("SpinnerState setTick and reset")
    void setTickAndReset() {
        SpinnerState state = new SpinnerState();
        state.setTick(100);
        assertThat(state.tick()).isEqualTo(100);
        state.reset();
        assertThat(state.tick()).isZero();
    }

    // ═══════════════════════════════════════════════════════════════
    // Spinner widget tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Spinner renders correct frame for given tick")
    void rendersCorrectFrame() {
        Spinner spinner = Spinner.builder()
                .frames("A", "B", "C")
                .build();

        SpinnerState state = new SpinnerState(0);
        Rect area = new Rect(0, 0, 5, 1);
        Buffer buffer = Buffer.empty(area);

        spinner.render(area, buffer, state);
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");

        // Advance and render again
        state.setTick(1);
        buffer = Buffer.empty(area);
        spinner.render(area, buffer, state);
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("B");

        state.setTick(2);
        buffer = Buffer.empty(area);
        spinner.render(area, buffer, state);
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("C");
    }

    @Test
    @DisplayName("Spinner wraps frame index")
    void wrapsFrameIndex() {
        Spinner spinner = Spinner.builder()
                .frames("X", "Y")
                .build();

        SpinnerState state = new SpinnerState(2); // wraps to index 0
        Rect area = new Rect(0, 0, 5, 1);
        Buffer buffer = Buffer.empty(area);

        spinner.render(area, buffer, state);
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("X");
    }

    @Test
    @DisplayName("Spinner with DOTS style renders braille character")
    void dotsStyleRendersBraille() {
        Spinner spinner = Spinner.builder()
                .spinnerStyle(SpinnerStyle.DOTS)
                .build();

        SpinnerState state = new SpinnerState(0);
        Rect area = new Rect(0, 0, 5, 1);
        Buffer buffer = Buffer.empty(area);

        spinner.render(area, buffer, state);
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("\u2800");
    }

    @Test
    @DisplayName("Empty area is no-op")
    void emptyAreaIsNoOp() {
        Spinner spinner = Spinner.dots();
        SpinnerState state = new SpinnerState();
        Rect area = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 1));

        // Should not throw
        spinner.render(area, buffer, state);
    }

    @Test
    @DisplayName("Spinner applies style")
    void appliesStyle() {
        Spinner spinner = Spinner.builder()
                .frames("X")
                .style(Style.EMPTY.bold())
                .build();

        SpinnerState state = new SpinnerState(0);
        Rect area = new Rect(0, 0, 5, 1);
        Buffer buffer = Buffer.empty(area);

        spinner.render(area, buffer, state);
        assertThat(buffer.get(0, 0).style().addModifiers()).contains(dev.tamboui.style.Modifier.BOLD);
    }

    @Test
    @DisplayName("maxFrameWidth calculates maximum display width")
    void maxFrameWidth() {
        Spinner spinner = Spinner.builder()
                .frames("A", "AB", "ABC")
                .build();

        assertThat(spinner.maxFrameWidth()).isEqualTo(3);
    }

    @Test
    @DisplayName("Spinner default style uses DOTS")
    void defaultUsesDotsStyle() {
        Spinner spinner = Spinner.dots();
        assertThat(spinner.frames()).hasSize(SpinnerStyle.DOTS.frameCount());
    }

    @Test
    @DisplayName("Custom frames override style")
    void customFramesOverrideStyle() {
        Spinner spinner = Spinner.builder()
                .spinnerStyle(SpinnerStyle.LINE)
                .frames("1", "2", "3")
                .build();

        assertThat(spinner.frames()).containsExactly("1", "2", "3");
    }

    @Test
    @DisplayName("Spinner truncates frame to available width")
    void truncatesFrameToWidth() {
        Spinner spinner = Spinner.builder()
                .frames("[====]")
                .build();

        SpinnerState state = new SpinnerState(0);
        Rect area = new Rect(0, 0, 3, 1);
        Buffer buffer = Buffer.empty(area);

        spinner.render(area, buffer, state);
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("[");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("=");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("=");
    }
}

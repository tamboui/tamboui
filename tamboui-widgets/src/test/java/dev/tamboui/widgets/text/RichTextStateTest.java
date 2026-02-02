/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.style.RichTextState;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for RichTextState scroll management.
 */
class RichTextStateTest {

    @Test
    @DisplayName("RichTextState scroll methods work correctly")
    void scrollMethodsWork() {
        RichTextState state = new RichTextState();
        state.setContentHeight(100);
        state.setViewportHeight(10);

        assertThat(state.scrollRow()).isEqualTo(0);

        state.scrollDown(5);
        assertThat(state.scrollRow()).isEqualTo(5);

        state.scrollUp(2);
        assertThat(state.scrollRow()).isEqualTo(3);

        state.pageDown();
        assertThat(state.scrollRow()).isEqualTo(12);

        state.scrollToTop();
        assertThat(state.scrollRow()).isEqualTo(0);

        state.scrollToBottom();
        assertThat(state.scrollRow()).isEqualTo(90);
    }

    @Test
    @DisplayName("RichTextState clamps scroll values")
    void clampsScrollValues() {
        RichTextState state = new RichTextState();
        state.setContentHeight(10);
        state.setViewportHeight(5);

        // Try to scroll past content
        state.scrollDown(100);
        assertThat(state.scrollRow()).isEqualTo(5); // maxScrollRow

        // Try to scroll before start
        state.scrollUp(100);
        assertThat(state.scrollRow()).isEqualTo(0);
    }

    @Test
    @DisplayName("RichTextState ensureLineVisible works")
    void ensureLineVisibleWorks() {
        RichTextState state = new RichTextState();
        state.setContentHeight(100);
        state.setViewportHeight(10);

        // Line 5 should be visible without scrolling
        state.ensureLineVisible(5);
        assertThat(state.scrollRow()).isEqualTo(0);

        // Line 15 should require scrolling
        state.ensureLineVisible(15);
        assertThat(state.scrollRow()).isEqualTo(6);

        // Line 5 now requires scrolling up
        state.ensureLineVisible(5);
        assertThat(state.scrollRow()).isEqualTo(5);
    }

    @Test
    @DisplayName("RichTextState scrollToLine clamps to valid range")
    void scrollToLineClamps() {
        RichTextState state = new RichTextState();
        state.setContentHeight(10);
        state.setViewportHeight(5);

        state.scrollToLine(3);
        assertThat(state.scrollRow()).isEqualTo(3);

        // Beyond max scroll
        state.scrollToLine(100);
        assertThat(state.scrollRow()).isEqualTo(5);
    }

    @Test
    @DisplayName("RichTextState isScrollable returns correct value")
    void isScrollableWorks() {
        RichTextState state = new RichTextState();

        // Content smaller than viewport
        state.setContentHeight(5);
        state.setViewportHeight(10);
        assertThat(state.isScrollable()).isFalse();

        // Content larger than viewport
        state.setContentHeight(20);
        state.setViewportHeight(10);
        assertThat(state.isScrollable()).isTrue();
    }

    @Test
    @DisplayName("RichTextState canScrollUp and canScrollDown work")
    void canScrollUpDownWork() {
        RichTextState state = new RichTextState();
        state.setContentHeight(10);
        state.setViewportHeight(5);

        // At top
        assertThat(state.canScrollUp()).isFalse();
        assertThat(state.canScrollDown()).isTrue();

        // Scroll down
        state.scrollDown(3);
        assertThat(state.canScrollUp()).isTrue();
        assertThat(state.canScrollDown()).isTrue();

        // At bottom
        state.scrollToBottom();
        assertThat(state.canScrollUp()).isTrue();
        assertThat(state.canScrollDown()).isFalse();
    }

    @Test
    @DisplayName("RichTextState horizontal scroll methods work")
    void horizontalScrollMethodsWork() {
        RichTextState state = new RichTextState();
        state.setContentWidth(100);
        state.setViewportWidth(20);

        assertThat(state.scrollCol()).isEqualTo(0);

        state.scrollRight(10);
        assertThat(state.scrollCol()).isEqualTo(10);

        state.scrollLeft(3);
        assertThat(state.scrollCol()).isEqualTo(7);
    }
}

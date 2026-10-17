/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MarkdownViewStateTest {

    @Test
    @DisplayName("Default state has zero offsets and dimensions")
    void defaultState() {
        MarkdownViewState state = new MarkdownViewState();
        assertThat(state.offset()).isEqualTo(0);
        assertThat(state.scroll()).isEqualTo(0);
        assertThat(state.totalRows()).isEqualTo(0);
        assertThat(state.viewportHeight()).isEqualTo(0);
        assertThat(state.maxScroll()).isEqualTo(0);
        assertThat(state.canScrollUp()).isFalse();
        assertThat(state.canScrollDown()).isFalse();
        assertThat(state.isAutoScrollToBottom()).isFalse();
    }

    @Test
    @DisplayName("Initial offset constructor and factory clamp negative values")
    void initialOffset() {
        MarkdownViewState state = new MarkdownViewState(5);
        assertThat(state.offset()).isEqualTo(5);

        MarkdownViewState clamped = MarkdownViewState.of(-3);
        assertThat(clamped.offset()).isEqualTo(0);
    }

    @Test
    @DisplayName("scrollDown increments offset and clamps to maxScroll when dimensions are known")
    void scrollDown() {
        MarkdownViewState state = new MarkdownViewState();
        state.update(10, 4); // maxScroll = 6

        state.scrollDown();
        assertThat(state.offset()).isEqualTo(1);
        assertThat(state.canScrollUp()).isTrue();
        assertThat(state.canScrollDown()).isTrue();

        state.scrollDown(3);
        assertThat(state.offset()).isEqualTo(4);

        // Clamps at maxScroll (6)
        state.scrollDown(10);
        assertThat(state.offset()).isEqualTo(6);
        assertThat(state.canScrollDown()).isFalse();

        // Negative lines delegates to scrollUp
        state.scrollDown(-2);
        assertThat(state.offset()).isEqualTo(4);
    }

    @Test
    @DisplayName("scrollUp decrements offset and clamps to 0")
    void scrollUp() {
        MarkdownViewState state = new MarkdownViewState(5);
        state.update(10, 4); // maxScroll = 6

        state.scrollUp();
        assertThat(state.offset()).isEqualTo(4);

        state.scrollUp(2);
        assertThat(state.offset()).isEqualTo(2);

        // Clamps at 0
        state.scrollUp(10);
        assertThat(state.offset()).isEqualTo(0);
        assertThat(state.canScrollUp()).isFalse();

        // Negative lines delegates to scrollDown
        state.scrollUp(-3);
        assertThat(state.offset()).isEqualTo(3);
    }

    @Test
    @DisplayName("scrollBy handles positive, negative, and zero delta")
    void scrollBy() {
        MarkdownViewState state = new MarkdownViewState();
        state.update(20, 5); // maxScroll = 15

        state.scrollBy(4);
        assertThat(state.offset()).isEqualTo(4);

        state.scrollBy(-2);
        assertThat(state.offset()).isEqualTo(2);

        state.scrollBy(0);
        assertThat(state.offset()).isEqualTo(2);
    }

    @Test
    @DisplayName("scrollToTop resets offset to 0 and clears autoScrollToBottom")
    void scrollToTop() {
        MarkdownViewState state = new MarkdownViewState();
        state.update(20, 5);
        state.scrollToBottom();
        assertThat(state.isAutoScrollToBottom()).isTrue();

        state.scrollToTop();
        assertThat(state.offset()).isEqualTo(0);
        assertThat(state.isAutoScrollToBottom()).isFalse();
    }

    @Test
    @DisplayName("scrollToBottom jumps to maxScroll and follows content growth")
    void scrollToBottomAndFollow() {
        MarkdownViewState state = new MarkdownViewState();
        state.scrollToBottom();
        assertThat(state.isAutoScrollToBottom()).isTrue();

        // Initial render: 10 rows, viewport 4 -> maxScroll 6
        state.update(10, 4);
        assertThat(state.offset()).isEqualTo(6);

        // Content grows to 15 rows: viewport 4 -> maxScroll 11
        state.update(15, 4);
        assertThat(state.offset()).isEqualTo(11);

        // Scrolling up pauses auto-follow
        state.scrollUp(2);
        assertThat(state.offset()).isEqualTo(9);
        assertThat(state.isAutoScrollToBottom()).isFalse();

        // Further content growth does not interrupt user scroll position
        state.update(20, 4);
        assertThat(state.offset()).isEqualTo(9);
    }

    @Test
    @DisplayName("setOffset directly updates position and clamps")
    void setOffset() {
        MarkdownViewState state = new MarkdownViewState();
        state.update(10, 4); // maxScroll = 6

        state.setOffset(3);
        assertThat(state.offset()).isEqualTo(3);

        state.setOffset(20);
        assertThat(state.offset()).isEqualTo(6);

        state.setScroll(1);
        assertThat(state.scroll()).isEqualTo(1);

        state.withOffset(2);
        assertThat(state.offset()).isEqualTo(2);
    }

    @Test
    @DisplayName("toString includes state representation")
    void toStringContainsInfo() {
        MarkdownViewState state = new MarkdownViewState(2);
        state.update(10, 5);
        assertThat(state.toString())
            .contains("offset=2")
            .contains("totalRows=10")
            .contains("viewportHeight=5");
    }
}

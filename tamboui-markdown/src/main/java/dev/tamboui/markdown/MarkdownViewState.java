/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown;


/**
 * State for a {@link MarkdownView} widget, managing scroll position and content dimensions.
 * <p>
 * During rendering, the state records the total content rows and the visible viewport
 * height, and clamps the scroll offset so callers cannot scroll past the bounds of the
 * document.
 * <p>
 * Example usage:
 * <pre>{@code
 * MarkdownViewState state = new MarkdownViewState();
 *
 * // In event handling:
 * state.scrollDown();
 * state.scrollUp();
 *
 * // In render:
 * frame.renderStatefulWidget(markdownView, area, state);
 * }</pre>
 */
public final class MarkdownViewState {

    private int offset;
    private int totalRows;
    private int viewportHeight;
    private boolean autoScrollToBottom;

    /**
     * Creates a new markdown view state with default values.
     */
    public MarkdownViewState() {
        this.offset = 0;
        this.totalRows = 0;
        this.viewportHeight = 0;
        this.autoScrollToBottom = false;
    }

    /**
     * Creates a new markdown view state with an initial scroll offset.
     *
     * @param offset the initial scroll offset (in lines)
     */
    public MarkdownViewState(int offset) {
        this.offset = Math.max(0, offset);
        this.totalRows = 0;
        this.viewportHeight = 0;
        this.autoScrollToBottom = false;
    }

    /**
     * Creates a new markdown view state with an initial scroll offset.
     *
     * @param offset the initial scroll offset (in lines)
     * @return a new markdown view state
     */
    public static MarkdownViewState of(int offset) {
        return new MarkdownViewState(offset);
    }

    /**
     * Returns the current scroll offset (number of lines scrolled from top).
     *
     * @return the scroll offset
     */
    public int offset() {
        return offset;
    }

    /**
     * Returns the current scroll offset (alias for {@link #offset()}).
     *
     * @return the scroll offset
     */
    public int scroll() {
        return offset;
    }

    /**
     * Sets the scroll offset directly.
     *
     * @param offset the scroll offset
     */
    public void setOffset(int offset) {
        this.offset = Math.max(0, offset);
        this.autoScrollToBottom = false;
        clampOffset();
    }

    /**
     * Sets the scroll offset (alias for {@link #setOffset(int)}).
     *
     * @param scroll the scroll offset
     */
    public void setScroll(int scroll) {
        setOffset(scroll);
    }

    /**
     * Sets the scroll offset fluently.
     *
     * @param offset the scroll offset
     * @return this state
     */
    public MarkdownViewState withOffset(int offset) {
        setOffset(offset);
        return this;
    }

    /**
     * Returns the total content rows computed during the last render.
     *
     * @return total content rows
     */
    public int totalRows() {
        return totalRows;
    }

    /**
     * Returns the viewport height (visible content rows) from the last render.
     *
     * @return viewport height
     */
    public int viewportHeight() {
        return viewportHeight;
    }

    /**
     * Returns the maximum scroll offset, based on content and viewport dimensions.
     *
     * @return maximum scroll offset
     */
    public int maxScroll() {
        return Math.max(0, totalRows - viewportHeight);
    }

    /**
     * Returns whether the content can be scrolled up further.
     *
     * @return {@code true} if {@code offset > 0}
     */
    public boolean canScrollUp() {
        return offset > 0;
    }

    /**
     * Returns whether the content can be scrolled down further.
     *
     * @return {@code true} if {@code offset < maxScroll}
     */
    public boolean canScrollDown() {
        return offset < maxScroll();
    }

    /**
     * Scrolls down by one line.
     */
    public void scrollDown() {
        scrollDown(1);
    }

    /**
     * Scrolls down by the specified number of lines.
     *
     * @param lines the number of lines to scroll
     */
    public void scrollDown(int lines) {
        if (lines < 0) {
            scrollUp(-lines);
            return;
        }
        if (lines > 0) {
            this.offset += lines;
            clampOffset();
        }
    }

    /**
     * Scrolls up by one line.
     */
    public void scrollUp() {
        scrollUp(1);
    }

    /**
     * Scrolls up by the specified number of lines.
     *
     * @param lines the number of lines to scroll
     */
    public void scrollUp(int lines) {
        if (lines < 0) {
            scrollDown(-lines);
            return;
        }
        if (lines > 0) {
            this.offset = Math.max(0, this.offset - lines);
            this.autoScrollToBottom = false;
        }
    }

    /**
     * Adjusts the scroll offset by a delta (positive = down, negative = up).
     *
     * @param delta the amount to scroll
     */
    public void scrollBy(int delta) {
        if (delta > 0) {
            scrollDown(delta);
        } else if (delta < 0) {
            scrollUp(-delta);
        }
    }

    /**
     * Scrolls to the top of the content.
     */
    public void scrollToTop() {
        this.offset = 0;
        this.autoScrollToBottom = false;
    }

    /**
     * Scrolls to the bottom of the content. Also enables sticky bottom-following
     * for newly streamed content until scrolled up.
     */
    public void scrollToBottom() {
        if (totalRows > 0 && viewportHeight > 0) {
            this.offset = maxScroll();
        }
        this.autoScrollToBottom = true;
    }

    /**
     * Returns whether the state is set to automatically follow the bottom.
     *
     * @return {@code true} if auto-scrolling to bottom
     */
    public boolean isAutoScrollToBottom() {
        return autoScrollToBottom;
    }

    /**
     * Sets whether to automatically follow the bottom of the content.
     *
     * @param autoScrollToBottom {@code true} to enable auto-scrolling to bottom
     */
    public void setAutoScrollToBottom(boolean autoScrollToBottom) {
        this.autoScrollToBottom = autoScrollToBottom;
        if (autoScrollToBottom && totalRows > 0 && viewportHeight > 0) {
            this.offset = maxScroll();
        }
    }

    /**
     * Updates content dimensions and clamps or updates scroll position.
     * <p>
     * Called by {@link MarkdownView} during rendering.
     *
     * @param totalRows total content rows
     * @param viewportHeight visible viewport height
     */
    public void update(int totalRows, int viewportHeight) {
        this.totalRows = Math.max(0, totalRows);
        this.viewportHeight = Math.max(0, viewportHeight);
        if (autoScrollToBottom) {
            this.offset = maxScroll();
        } else {
            clampOffset();
        }
    }

    private void clampOffset() {
        this.offset = Math.max(0, this.offset);
        if (totalRows > 0 && viewportHeight > 0) {
            this.offset = Math.min(this.offset, maxScroll());
        }
    }

    @Override
    public String toString() {
        return "MarkdownViewState{" +
            "offset=" + offset +
            ", totalRows=" + totalRows +
            ", viewportHeight=" + viewportHeight +
            ", autoScrollToBottom=" + autoScrollToBottom +
            '}';
    }
}

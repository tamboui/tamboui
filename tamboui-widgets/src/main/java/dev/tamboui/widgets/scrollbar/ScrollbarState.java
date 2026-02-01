/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.scrollbar;

/**
 * State for a {@link Scrollbar} widget.
 * <p>
 * Tracks the scroll position and content dimensions for proper scrollbar rendering.
 * <p>
 * <b>Important:</b> You must set {@link #contentLength(int)} before rendering,
 * otherwise the scrollbar will appear blank.
 *
 * <pre>{@code
 * // Create state for a list of 100 items
 * ScrollbarState state = new ScrollbarState()
 *     .contentLength(100)
 *     .position(0);
 *
 * // Update position when scrolling
 * state.position(newScrollPosition);
 *
 * // Navigation helpers
 * state.next();     // Scroll down one item
 * state.prev();     // Scroll up one item
 * state.first();    // Jump to start
 * state.last();     // Jump to end
 * }</pre>
 */
public final class ScrollbarState {

    private int contentLength;
    private int position;
    private int viewportContentLength;

    /**
     * Creates a new scrollbar state with default values.
     * <p>
     * Content length defaults to 0, which will result in a blank scrollbar.
     * You must call {@link #contentLength(int)} before rendering.
     */
    public ScrollbarState() {
        this.contentLength = 0;
        this.position = 0;
        this.viewportContentLength = 0;
    }

    /**
     * Creates a new scrollbar state with the given content length.
     *
     * @param contentLength the total number of scrollable items
     */
    public ScrollbarState(int contentLength) {
        this.contentLength = Math.max(0, contentLength);
        this.position = 0;
        this.viewportContentLength = 0;
    }

    /**
     * Returns the total length of the scrollable content.
     *
     * @return the content length
     */
    public int contentLength() {
        return contentLength;
    }

    /**
     * Sets the total length of the scrollable content.
     * <p>
     * This is a fluent setter that returns this state for chaining.
     *
     * @param contentLength the total number of scrollable items
     * @return this state for chaining
     */
    public ScrollbarState contentLength(int contentLength) {
        this.contentLength = Math.max(0, contentLength);
        // Clamp position if content shrunk
        if (this.contentLength > 0 && this.position >= this.contentLength) {
            this.position = this.contentLength - 1;
        }
        return this;
    }

    /**
     * Returns the current scroll position.
     *
     * @return the scroll position
     */
    public int position() {
        return position;
    }

    /**
     * Sets the current scroll position.
     * <p>
     * This is a fluent setter that returns this state for chaining.
     * The position is clamped to valid bounds.
     *
     * @param position the scroll position (0-based index)
     * @return this state for chaining
     */
    public ScrollbarState position(int position) {
        this.position = clampPosition(position);
        return this;
    }

    /**
     * Returns the viewport content length.
     * <p>
     * If not set (0), the scrollbar will use the track size.
     *
     * @return the viewport content length
     */
    public int viewportContentLength() {
        return viewportContentLength;
    }

    /**
     * Sets the viewport content length.
     * <p>
     * This represents the number of items visible in the viewport.
     * If not set, the scrollbar will use the track size as the viewport length.
     * <p>
     * This is a fluent setter that returns this state for chaining.
     *
     * @param viewportContentLength the number of visible items
     * @return this state for chaining
     */
    public ScrollbarState viewportContentLength(int viewportContentLength) {
        this.viewportContentLength = Math.max(0, viewportContentLength);
        return this;
    }

    /**
     * Scrolls to the first position.
     */
    public void first() {
        this.position = 0;
    }

    /**
     * Scrolls to the last position.
     */
    public void last() {
        if (contentLength > 0) {
            this.position = contentLength - 1;
        }
    }

    /**
     * Scrolls up by one position.
     */
    public void prev() {
        scrollBy(-1);
    }

    /**
     * Scrolls down by one position.
     */
    public void next() {
        scrollBy(1);
    }

    /**
     * Scrolls by the given amount.
     * <p>
     * Positive values scroll down/right, negative values scroll up/left.
     * The position is clamped to valid bounds.
     *
     * @param amount the number of items to scroll
     */
    public void scrollBy(int amount) {
        this.position = clampPosition(this.position + amount);
    }

    /**
     * Scrolls up by a page (viewport length).
     * <p>
     * If viewport content length is not set, scrolls by 1.
     */
    public void pageUp() {
        int pageSize = viewportContentLength > 0 ? viewportContentLength : 1;
        scrollBy(-pageSize);
    }

    /**
     * Scrolls down by a page (viewport length).
     * <p>
     * If viewport content length is not set, scrolls by 1.
     */
    public void pageDown() {
        int pageSize = viewportContentLength > 0 ? viewportContentLength : 1;
        scrollBy(pageSize);
    }

    /**
     * Returns true if scrolled to the beginning (position 0).
     *
     * @return true if at the start
     */
    public boolean isAtStart() {
        return position == 0;
    }

    /**
     * Returns true if scrolled to the end.
     *
     * @return true if at the end
     */
    public boolean isAtEnd() {
        return contentLength == 0 || position >= contentLength - 1;
    }

    /**
     * Calculates the scroll percentage (0.0 to 1.0).
     *
     * @return the scroll percentage
     */
    public double scrollPercentage() {
        if (contentLength <= 1) {
            return 0.0;
        }
        return (double) position / (contentLength - 1);
    }

    private int clampPosition(int pos) {
        if (contentLength == 0) {
            return 0;
        }
        return Math.max(0, Math.min(pos, contentLength - 1));
    }
}

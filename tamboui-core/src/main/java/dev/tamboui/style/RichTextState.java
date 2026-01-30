/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.style;

/**
 * State for managing scroll position and viewport information in scrollable
 * text elements.
 * <p>
 * The state tracks:
 * <ul>
 * <li>Scroll position (row and column offsets)</li>
 * <li>Content dimensions (total lines and max line width)</li>
 * <li>Viewport dimensions (visible area size)</li>
 * </ul>
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * RichTextState state = new RichTextState();
 *
 * // Scroll down one line
 * state.scrollDown();
 *
 * // Scroll to a specific line
 * state.scrollToLine(50);
 *
 * // Ensure a line is visible
 * state.ensureLineVisible(100);
 * }</pre>
 */
public final class RichTextState {

    private int scrollRow = 0;
    private int scrollCol = 0;
    private int contentHeight = 0;
    private int contentWidth = 0;
    private int viewportHeight = 0;
    private int viewportWidth = 0;

    /**
     * Creates a new RichTextState with default values.
     */
    public RichTextState() {
    }

    /**
     * Returns the current vertical scroll offset (number of lines scrolled from
     * top).
     *
     * @return the scroll row offset
     */
    public int scrollRow() {
        return scrollRow;
    }

    /**
     * Returns the current horizontal scroll offset (number of columns scrolled from
     * left).
     *
     * @return the scroll column offset
     */
    public int scrollCol() {
        return scrollCol;
    }

    /**
     * Returns the total content height in lines.
     *
     * @return the content height
     */
    public int contentHeight() {
        return contentHeight;
    }

    /**
     * Returns the maximum content width (longest line).
     *
     * @return the content width
     */
    public int contentWidth() {
        return contentWidth;
    }

    /**
     * Returns the viewport height (visible lines).
     *
     * @return the viewport height
     */
    public int viewportHeight() {
        return viewportHeight;
    }

    /**
     * Returns the viewport width (visible columns).
     *
     * @return the viewport width
     */
    public int viewportWidth() {
        return viewportWidth;
    }

    /**
     * Sets the content height. Called by the widget during rendering.
     *
     * @param height
     *            the content height in lines
     */
    public void setContentHeight(int height) {
        this.contentHeight = Math.max(0, height);
        clampScroll();
    }

    /**
     * Sets the content width. Called by the widget during rendering.
     *
     * @param width
     *            the content width
     */
    public void setContentWidth(int width) {
        this.contentWidth = Math.max(0, width);
        clampScroll();
    }

    /**
     * Sets the viewport height. Called by the widget during rendering.
     *
     * @param height
     *            the viewport height
     */
    public void setViewportHeight(int height) {
        this.viewportHeight = Math.max(0, height);
        clampScroll();
    }

    /**
     * Sets the viewport width. Called by the widget during rendering.
     *
     * @param width
     *            the viewport width
     */
    public void setViewportWidth(int width) {
        this.viewportWidth = Math.max(0, width);
        clampScroll();
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
     * @param lines
     *            the number of lines to scroll
     */
    public void scrollDown(int lines) {
        scrollRow += lines;
        clampScroll();
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
     * @param lines
     *            the number of lines to scroll
     */
    public void scrollUp(int lines) {
        scrollRow -= lines;
        clampScroll();
    }

    /**
     * Scrolls right by one column.
     */
    public void scrollRight() {
        scrollRight(1);
    }

    /**
     * Scrolls right by the specified number of columns.
     *
     * @param cols
     *            the number of columns to scroll
     */
    public void scrollRight(int cols) {
        scrollCol += cols;
        clampScroll();
    }

    /**
     * Scrolls left by one column.
     */
    public void scrollLeft() {
        scrollLeft(1);
    }

    /**
     * Scrolls left by the specified number of columns.
     *
     * @param cols
     *            the number of columns to scroll
     */
    public void scrollLeft(int cols) {
        scrollCol -= cols;
        clampScroll();
    }

    /**
     * Scrolls down by one page (viewport height - 1).
     */
    public void pageDown() {
        scrollDown(Math.max(1, viewportHeight - 1));
    }

    /**
     * Scrolls up by one page (viewport height - 1).
     */
    public void pageUp() {
        scrollUp(Math.max(1, viewportHeight - 1));
    }

    /**
     * Scrolls to the top of the content.
     */
    public void scrollToTop() {
        scrollRow = 0;
        scrollCol = 0;
    }

    /**
     * Scrolls to the bottom of the content.
     */
    public void scrollToBottom() {
        scrollRow = maxScrollRow();
    }

    /**
     * Scrolls to show the specified line.
     *
     * @param line
     *            the line number (0-based)
     */
    public void scrollToLine(int line) {
        scrollRow = line;
        clampScroll();
    }

    /**
     * Ensures the specified line is visible, scrolling if necessary.
     *
     * @param line
     *            the line number (0-based)
     */
    public void ensureLineVisible(int line) {
        if (line < scrollRow) {
            scrollRow = line;
        } else if (line >= scrollRow + viewportHeight) {
            scrollRow = line - viewportHeight + 1;
        }
        clampScroll();
    }

    /**
     * Returns the maximum scroll row position.
     *
     * @return the maximum scroll row
     */
    public int maxScrollRow() {
        return Math.max(0, contentHeight - viewportHeight);
    }

    /**
     * Returns the maximum scroll column position.
     *
     * @return the maximum scroll column
     */
    public int maxScrollCol() {
        return Math.max(0, contentWidth - viewportWidth);
    }

    /**
     * Returns true if the content can be scrolled (content exceeds viewport).
     *
     * @return true if scrollable
     */
    public boolean isScrollable() {
        return contentHeight > viewportHeight || contentWidth > viewportWidth;
    }

    /**
     * Returns true if there is content below the current viewport.
     *
     * @return true if can scroll down
     */
    public boolean canScrollDown() {
        return scrollRow < maxScrollRow();
    }

    /**
     * Returns true if there is content above the current viewport.
     *
     * @return true if can scroll up
     */
    public boolean canScrollUp() {
        return scrollRow > 0;
    }

    /**
     * Returns true if there is content to the right of the current viewport.
     *
     * @return true if can scroll right
     */
    public boolean canScrollRight() {
        return scrollCol < maxScrollCol();
    }

    /**
     * Returns true if there is content to the left of the current viewport.
     *
     * @return true if can scroll left
     */
    public boolean canScrollLeft() {
        return scrollCol > 0;
    }

    private void clampScroll() {
        scrollRow = Math.max(0, Math.min(scrollRow, maxScrollRow()));
        scrollCol = Math.max(0, Math.min(scrollCol, maxScrollCol()));
    }
}

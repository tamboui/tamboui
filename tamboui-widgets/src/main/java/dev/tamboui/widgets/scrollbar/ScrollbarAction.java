/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.scrollbar;

/**
 * Mouse actions that a {@link Scrollbar} can handle.
 * <p>
 * Consumers map their mouse events to these actions and pass them to
 * {@link Scrollbar#handleMouseAction(int, int, ScrollbarAction, dev.tamboui.layout.Rect, ScrollbarState)}.
 *
 * <pre>{@code
 * // Example mapping from a MouseEvent:
 * ScrollbarAction action = null;
 * switch (mouseEvent.kind()) {
 *     case PRESS:
 *         if (mouseEvent.isLeftButton()) action = ScrollbarAction.PRESS;
 *         break;
 *     case DRAG:
 *         if (mouseEvent.isLeftButton()) action = ScrollbarAction.DRAG;
 *         break;
 *     case RELEASE:
 *         action = ScrollbarAction.RELEASE;
 *         break;
 *     case SCROLL_UP:
 *         action = ScrollbarAction.SCROLL_UP;
 *         break;
 *     case SCROLL_DOWN:
 *         action = ScrollbarAction.SCROLL_DOWN;
 *         break;
 * }
 * if (action != null) {
 *     scrollbar.handleMouseAction(mouseEvent.x(), mouseEvent.y(), action, area, state);
 * }
 * }</pre>
 *
 * @see Scrollbar#handleMouseAction(int, int, ScrollbarAction, dev.tamboui.layout.Rect, ScrollbarState)
 */
public enum ScrollbarAction {

    /**
     * Left mouse button pressed.
     * <p>
     * When pressed on the thumb, initiates a drag. When pressed on the track
     * above/before the thumb, pages up/left. When pressed below/after, pages down/right.
     */
    PRESS,

    /**
     * Mouse moved while the left button is held (drag).
     * <p>
     * Moves the thumb proportionally to the mouse position within the track.
     * Drag events are consumed even when the mouse moves outside the scrollbar bounds,
     * as long as a drag is in progress.
     */
    DRAG,

    /**
     * Mouse button released.
     * <p>
     * Ends any in-progress drag operation.
     */
    RELEASE,

    /**
     * Scroll wheel moved up (or left for horizontal scrollbars).
     * <p>
     * Scrolls the content by one position.
     */
    SCROLL_UP,

    /**
     * Scroll wheel moved down (or right for horizontal scrollbars).
     * <p>
     * Scrolls the content by one position.
     */
    SCROLL_DOWN
}

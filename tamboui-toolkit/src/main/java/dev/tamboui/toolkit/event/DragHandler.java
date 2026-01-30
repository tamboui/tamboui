/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

/**
 * Handler for drag operations on an element.
 */
public interface DragHandler {
    /**
     * Called when a drag operation starts.
     *
     * @param startX
     *            the starting X position
     * @param startY
     *            the starting Y position
     */
    void onDragStart(int startX, int startY);

    /**
     * Called during a drag operation with the current position.
     *
     * @param currentX
     *            the current X position
     * @param currentY
     *            the current Y position
     * @param deltaX
     *            the change in X from start
     * @param deltaY
     *            the change in Y from start
     */
    void onDrag(int currentX, int currentY, int deltaX, int deltaY);

    /**
     * Called when a drag operation ends.
     *
     * @param endX
     *            the ending X position
     * @param endY
     *            the ending Y position
     */
    void onDragEnd(int endX, int endY);
}

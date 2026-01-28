/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.focus;

import dev.tamboui.layout.Rect;
import static dev.tamboui.util.CollectionUtil.listCopyOf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages focus state for focusable elements.
 * Handles tab navigation and click-to-focus.
 */
public final class FocusManager {

    /**
     * Creates a new focus manager with no focused element.
     */
    public FocusManager() {
    }

    private String focusedId;
    private final List<String> focusOrder = new ArrayList<>();
    private final Map<String, Rect> focusableAreas = new LinkedHashMap<>();

    /**
     * Returns the ID of the currently focused element.
     *
     * @return the focused element ID, or null if nothing is focused
     */
    public String focusedId() {
        return focusedId;
    }

    /**
     * Returns whether the element with the given ID is currently focused.
     *
     * @param elementId the element ID to check
     * @return true if focused
     */
    public boolean isFocused(String elementId) {
        return elementId != null && elementId.equals(focusedId);
    }

    /**
     * Sets focus to the element with the given ID.
     *
     * @param elementId the element ID to focus, or null to clear focus
     */
    public void setFocus(String elementId) {
        this.focusedId = elementId;
    }

    /**
     * Clears the current focus.
     */
    public void clearFocus() {
        this.focusedId = null;
    }

    /**
     * Registers a focusable element with its rendered area.
     * Called during rendering to track focusable elements.
     * <p>
     * If nothing is currently focused, the first registered element
     * will be auto-focused.
     *
     * @param elementId the element ID
     * @param area the rendered area
     */
    public void registerFocusable(String elementId, Rect area) {
        if (elementId != null) {
            boolean isFirst = focusOrder.isEmpty();
            if (!focusOrder.contains(elementId)) {
                focusOrder.add(elementId);
            }
            focusableAreas.put(elementId, area);

            // Auto-focus first focusable element if nothing is focused
            if (isFirst && focusedId == null) {
                focusedId = elementId;
            }
        }
    }

    /**
     * Clears all registered focusable elements.
     * Should be called at the start of each render cycle.
     */
    public void clearFocusables() {
        focusOrder.clear();
        focusableAreas.clear();
    }

    /**
     * Moves focus to the next focusable element.
     *
     * @return true if focus changed
     */
    public boolean focusNext() {
        if (focusOrder.isEmpty()) {
            return false;
        }

        if (focusedId == null) {
            focusedId = focusOrder.get(0);
            return true;
        }

        int index = focusOrder.indexOf(focusedId);
        if (index < 0) {
            focusedId = focusOrder.get(0);
            return true;
        }

        int nextIndex = (index + 1) % focusOrder.size();
        String nextId = focusOrder.get(nextIndex);
        if (!nextId.equals(focusedId)) {
            focusedId = nextId;
            return true;
        }
        return false;
    }

    /**
     * Moves focus to the previous focusable element.
     *
     * @return true if focus changed
     */
    public boolean focusPrevious() {
        if (focusOrder.isEmpty()) {
            return false;
        }

        if (focusedId == null) {
            focusedId = focusOrder.get(focusOrder.size() - 1);
            return true;
        }

        int index = focusOrder.indexOf(focusedId);
        if (index < 0) {
            focusedId = focusOrder.get(focusOrder.size() - 1);
            return true;
        }

        int prevIndex = (index - 1 + focusOrder.size()) % focusOrder.size();
        String prevId = focusOrder.get(prevIndex);
        if (!prevId.equals(focusedId)) {
            focusedId = prevId;
            return true;
        }
        return false;
    }

    /**
     * Attempts to focus the element at the given screen position.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     * @return true if an element was focused
     */
    public boolean focusAt(int x, int y) {
        for (Map.Entry<String, Rect> entry : focusableAreas.entrySet()) {
            if (entry.getValue().contains(x, y)) {
                focusedId = entry.getKey();
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the area of the focused element, if any.
     *
     * @return the focused element's area, or null
     */
    public Rect focusedArea() {
        return focusedId != null ? focusableAreas.get(focusedId) : null;
    }

    /**
     * Returns the list of focusable element IDs in tab order.
     *
     * @return the focus order
     */
    public List<String> focusOrder() {
        return listCopyOf(focusOrder);
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings;

/**
 * String constants for common semantic actions.
 * <p>
 * These constants provide type-safe references to well-known actions while
 * allowing custom actions to be defined as arbitrary strings.
 *
 * <pre>{@code
 * // Using predefined actions
 * if (event.matches(Actions.MOVE_UP)) { ... }
 *
 * // Using custom string actions
 * if (event.matches("myApp.customAction")) { ... }
 * }</pre>
 */
public final class Actions {

    private Actions() {
    }

    // Navigation
    /**
     * Move up (arrow up, vim 'k', emacs Ctrl+p, etc.).
     */
    public static final String MOVE_UP = "moveUp";

    /**
     * Move down (arrow down, vim 'j', emacs Ctrl+n, etc.).
     */
    public static final String MOVE_DOWN = "moveDown";

    /**
     * Move left (arrow left, vim 'h', emacs Ctrl+b, etc.).
     */
    public static final String MOVE_LEFT = "moveLeft";

    /**
     * Move right (arrow right, vim 'l', emacs Ctrl+f, etc.).
     */
    public static final String MOVE_RIGHT = "moveRight";

    // Page navigation
    /**
     * Page up (PageUp, vim Ctrl+u, emacs Alt+v, etc.).
     */
    public static final String PAGE_UP = "pageUp";

    /**
     * Page down (PageDown, vim Ctrl+d, emacs Ctrl+v, etc.).
     */
    public static final String PAGE_DOWN = "pageDown";

    /**
     * Go to beginning (Home, vim 'g', emacs Alt+&lt;, etc.).
     */
    public static final String HOME = "home";

    /**
     * Go to end (End, vim 'G', emacs Alt+&gt;, etc.).
     */
    public static final String END = "end";

    // Selection / Confirmation
    /**
     * Select or confirm (Enter or Space).
     */
    public static final String SELECT = "select";

    /**
     * Confirm (Enter only).
     */
    public static final String CONFIRM = "confirm";

    /**
     * Cancel (Escape, emacs Ctrl+g, etc.).
     */
    public static final String CANCEL = "cancel";

    // Focus navigation
    /**
     * Move focus to next element (Tab).
     */
    public static final String FOCUS_NEXT = "focusNext";

    /**
     * Move focus to previous element (Shift+Tab).
     */
    public static final String FOCUS_PREVIOUS = "focusPrevious";

    // Editing
    /**
     * Delete character backward (Backspace).
     */
    public static final String DELETE_BACKWARD = "deleteBackward";

    /**
     * Delete character forward (Delete).
     */
    public static final String DELETE_FORWARD = "deleteForward";

    // Application
    /**
     * Quit the application (q, Ctrl+c, etc.).
     */
    public static final String QUIT = "quit";

    // Mouse-specific actions
    /**
     * Primary click (left mouse button press).
     */
    public static final String CLICK = "click";

    /**
     * Secondary click (right mouse button press).
     */
    public static final String RIGHT_CLICK = "rightClick";

    /**
     * Scroll (mouse scroll wheel).
     */
    public static final String SCROLL = "scroll";

    /**
     * Scroll up (mouse scroll wheel up).
     */
    public static final String SCROLL_UP = "scrollUp";

    /**
     * Scroll down (mouse scroll wheel down).
     */
    public static final String SCROLL_DOWN = "scrollDown";

    // Debug / Development
    /**
     * Toggle the FPS overlay.
     */
    public static final String TOGGLE_FPS_OVERLAY = "toggleFpsOverlay";
}

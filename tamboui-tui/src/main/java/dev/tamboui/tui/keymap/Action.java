/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.keymap;

/**
 * Semantic actions that can be triggered by key events.
 * <p>
 * Actions represent the intent behind a key press, allowing the same
 * action to be bound to different keys depending on the keymap.
 */
public enum Action {
    // Navigation
    /**
     * Move up (arrow up, vim 'k', emacs Ctrl+p, etc.).
     */
    MOVE_UP,

    /**
     * Move down (arrow down, vim 'j', emacs Ctrl+n, etc.).
     */
    MOVE_DOWN,

    /**
     * Move left (arrow left, vim 'h', emacs Ctrl+b, etc.).
     */
    MOVE_LEFT,

    /**
     * Move right (arrow right, vim 'l', emacs Ctrl+f, etc.).
     */
    MOVE_RIGHT,

    // Page navigation
    /**
     * Page up (PageUp, vim Ctrl+u, emacs Alt+v, etc.).
     */
    PAGE_UP,

    /**
     * Page down (PageDown, vim Ctrl+d, emacs Ctrl+v, etc.).
     */
    PAGE_DOWN,

    /**
     * Go to beginning (Home, vim 'g', emacs Alt+&lt;, etc.).
     */
    HOME,

    /**
     * Go to end (End, vim 'G', emacs Alt+&gt;, etc.).
     */
    END,

    // Selection / Confirmation
    /**
     * Select or confirm (Enter or Space).
     */
    SELECT,

    /**
     * Confirm (Enter only).
     */
    CONFIRM,

    /**
     * Cancel (Escape, emacs Ctrl+g, etc.).
     */
    CANCEL,

    // Focus navigation
    /**
     * Move focus to next element (Tab).
     */
    FOCUS_NEXT,

    /**
     * Move focus to previous element (Shift+Tab).
     */
    FOCUS_PREVIOUS,

    // Editing
    /**
     * Delete character backward (Backspace).
     */
    DELETE_BACKWARD,

    /**
     * Delete character forward (Delete).
     */
    DELETE_FORWARD,

    // Application
    /**
     * Quit the application (q, Ctrl+c, etc.).
     */
    QUIT
}

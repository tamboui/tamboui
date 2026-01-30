/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.error;

/**
 * Defines the actions that can be taken when a render error occurs.
 * <p>
 * When a rendering exception is caught, the error handler returns one of these
 * actions to indicate how the TuiRunner should proceed.
 *
 * @see RenderErrorHandler
 */
public enum ErrorAction {

    /**
     * Display the error in the UI, wait for user dismissal, then quit.
     * <p>
     * This is the default behavior. The error display shows the exception type,
     * message, and a scrollable stack trace. Users can press 'q' to quit or use
     * arrow keys to scroll through the stack trace.
     */
    DISPLAY_AND_QUIT,

    /**
     * Clean up the terminal and quit immediately without displaying the error.
     * <p>
     * Use this when you want to log the error elsewhere (e.g., to a file) and exit
     * cleanly without user interaction.
     */
    QUIT_IMMEDIATELY,

    /**
     * Suppress the error and attempt to continue rendering.
     * <p>
     * <strong>Warning:</strong> This is dangerous and should only be used by
     * advanced users who understand the implications. Continuing after a render
     * error may leave the UI in an inconsistent state.
     */
    SUPPRESS
}

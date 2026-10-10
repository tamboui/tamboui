/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

/** Callback invoked when the user requests copying toast text. */
@FunctionalInterface
public interface ToastCopyHandler {
    /**
     * Called when the user requests copying toast text.
     *
     * @param toastId the identifier of the toast whose text was requested
     * @param text the text to copy
     */
    void onCopyRequested(String toastId, String text);
}

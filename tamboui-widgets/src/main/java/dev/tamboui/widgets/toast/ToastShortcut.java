/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

import java.util.Objects;

/** Keyboard shortcut bound to a toast dismiss action. */
public final class ToastShortcut {
    /** Dismiss action performed by a keyboard shortcut. */
    public enum Action {
        /** Dismiss the topmost visible toast. */
        DISMISS_TOP,
        /** Dismiss all visible toasts. */
        DISMISS_ALL
    }

    private final ToastShortcutTrigger trigger;
    private final Action action;

    private ToastShortcut(ToastShortcutTrigger trigger, Action action) {
        this.trigger = Objects.requireNonNull(trigger);
        this.action = Objects.requireNonNull(action);
    }

    /**
     * Creates a toast keyboard shortcut.
     *
     * @param trigger the key trigger that activates the shortcut
     * @param action the dismiss action to perform
     * @return a new shortcut binding
     */
    public static ToastShortcut of(ToastShortcutTrigger trigger, Action action) {
        return new ToastShortcut(trigger, action);
    }

    /**
     * Returns the key trigger for this shortcut.
     *
     * @return the key trigger
     */
    public ToastShortcutTrigger trigger() {
        return trigger;
    }

    /**
     * Returns the dismiss action bound to this shortcut.
     *
     * @return the dismiss action
     */
    public Action action() {
        return action;
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.toast;

/**
 * Marker for a keyboard trigger bound to a toast shortcut.
 * <p>
 * Implementations are provided by the TUI bindings layer (for example
 * {@code dev.tamboui.tui.bindings.KeyTrigger}) and must be present on the
 * runtime classpath when toast shortcuts are used.
 */
public interface ToastShortcutTrigger {}

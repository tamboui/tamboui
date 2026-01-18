/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

/// Represents the kind of mouse event.
public enum MouseEventKind {
    /// Mouse button was pressed down.
    PRESS,

    /// Mouse button was released.
    RELEASE,

    /// Mouse was moved while a button is held (drag).
    DRAG,

    /// Mouse was moved without any button pressed.
    MOVE,

    /// Scroll wheel was scrolled up.
    SCROLL_UP,

    /// Scroll wheel was scrolled down.
    SCROLL_DOWN
}


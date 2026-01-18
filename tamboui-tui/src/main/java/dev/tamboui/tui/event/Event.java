/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import dev.tamboui.tui.EventHandler;

/// Base interface for all TUI events.
///
///
///
/// Events are delivered to the application through the event handler.
/// This is a sealed interface with the following implementations:
///
/// - {@link KeyEvent} - Keyboard input
/// - {@link MouseEvent} - Mouse input
/// - {@link ResizeEvent} - Terminal window resize
/// - {@link TickEvent} - Animation timer tick
///
///
/// @see EventHandler
public interface Event {
}


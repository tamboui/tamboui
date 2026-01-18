/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.error;

/// Handler for render errors.
///
///
///
/// Implementations decide what action to take when a rendering exception occurs.
/// The handler receives the error details and a context for interacting with the
/// runner, and returns an action indicating how to proceed.
///
/// ```java
/// // Custom handler that logs and quits immediately
/// RenderErrorHandler handler = (error, context) -> {
///     error.cause().printStackTrace(context.errorOutput());
///     return ErrorAction.QUIT_IMMEDIATELY;
/// };
/// }
/// ```
///
/// @see RenderErrorHandlers
/// @see ErrorAction
@FunctionalInterface
public interface RenderErrorHandler {

    /// Handles a render error.
    ///
    /// @param error   the error information
    /// @param context the error context providing output streams and runner control
    /// @return the action to take
    ErrorAction handle(RenderError error, ErrorContext context);
}


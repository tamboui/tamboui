/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import dev.tamboui.terminal.Frame;

/// Processor called after each frame is rendered.
///
///
///
/// Post-render processors can apply effects, overlays, or other post-processing
/// to the rendered buffer before it is displayed.
///
///
///
/// Processors are called in the order they are added, after the main renderer
/// completes but before the frame is flushed to the terminal.
///
/// @see TuiConfig.Builder#postRenderProcessor(PostRenderProcessor)
@FunctionalInterface
public interface PostRenderProcessor {

    /// Processes the frame after rendering.
    ///
    /// @param frame the rendered frame
    void process(Frame frame);
}


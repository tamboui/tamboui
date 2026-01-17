/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.app;

import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.ElementRegistry;

import java.time.Duration;

/**
 * Processor called after each frame is rendered.
 * <p>
 * Post-render processors can apply effects, overlays, or other post-processing
 * to the rendered buffer before it is displayed.
 * <p>
 * Processors are called in the order they are added, after the main renderer
 * completes and element areas are registered in the ElementRegistry.
 *
 * @see ToolkitRunner.Builder#postRenderProcessor(ToolkitPostRenderProcessor)
 */
@FunctionalInterface
public interface ToolkitPostRenderProcessor {

    /**
     * Processes the frame after rendering.
     *
     * @param frame the rendered frame
     * @param elementRegistry the registry of rendered element areas
     * @param elapsed the time elapsed since the last frame (from TickEvent)
     */
    void process(Frame frame, ElementRegistry elementRegistry, Duration elapsed);
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.app;

import java.time.Duration;

import dev.tamboui.style.StyledAreaRegistry;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.ElementRegistry;
import dev.tamboui.toolkit.focus.FocusManager;

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
     * @param frame
     *            the rendered frame
     * @param elementRegistry
     *            the registry of rendered element areas
     * @param styledAreaRegistry
     *            the registry of styled areas (for CSS targeting)
     * @param focusManager
     *            the focus manager for pseudo-class state
     * @param elapsed
     *            the time elapsed since the last frame (from TickEvent)
     */
    void process(Frame frame, ElementRegistry elementRegistry,
            StyledAreaRegistry styledAreaRegistry, FocusManager focusManager, Duration elapsed);
}

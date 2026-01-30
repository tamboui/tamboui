/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui;

import dev.tamboui.terminal.Frame;

/**
 * Functional interface for rendering the TUI.
 * <p>
 * The renderer is called whenever the UI needs to be redrawn, typically after
 * handling an event that returns {@code true}.
 *
 * @see TuiRunner#run(EventHandler, Renderer)
 */
@FunctionalInterface
public interface Renderer {

    /**
     * Renders the user interface to the given frame.
     *
     * @param frame
     *            the frame to render to
     */
    void render(Frame frame);
}

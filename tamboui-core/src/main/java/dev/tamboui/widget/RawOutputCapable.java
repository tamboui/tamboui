/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widget;

import java.io.OutputStream;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;

/**
 * Interface for widgets that can render using raw terminal output.
 * <p>
 * Widgets implementing this interface can use native terminal protocols (such
 * as Sixel, Kitty, or iTerm2) that require sending escape sequences directly to
 * the terminal output stream.
 * <p>
 * When a widget implements both {@link Widget} and {@code RawOutputCapable},
 * the frame will call {@link #render(Rect, Buffer, OutputStream)} instead of
 * the standard {@link Widget#render(Rect, Buffer)} method.
 *
 * @see Widget
 */
public interface RawOutputCapable {

    /**
     * Renders this widget to the given area with raw output support.
     *
     * @param area
     *            the area to render within
     * @param buffer
     *            the buffer for character-based rendering
     * @param rawOutput
     *            the output stream for native protocol escape sequences, may be
     *            {@code null} if raw output is not available
     */
    void render(Rect area, Buffer buffer, OutputStream rawOutput);
}
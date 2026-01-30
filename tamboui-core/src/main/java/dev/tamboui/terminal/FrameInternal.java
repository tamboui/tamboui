/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.terminal;

import java.io.OutputStream;

/**
 * Utility class exposing internal Frame capabilities.
 * <p>
 * This class provides access to raw terminal output for widgets that need to
 * send escape sequences directly (e.g., native image protocols like Sixel,
 * Kitty, or iTerm2).
 * <p>
 * <b>Warning:</b> This is an internal API. Widgets should only use this if they
 * understand the implications of writing raw bytes to the terminal output
 * stream.
 *
 * @see Frame
 */
public final class FrameInternal {

    private FrameInternal() {
    }

    /**
     * Returns the raw output stream for the given frame.
     * <p>
     * This stream writes directly to the terminal backend, bypassing the normal
     * cell-based rendering. Use with caution.
     * <p>
     * The returned stream may be {@code null} if the backend does not support raw
     * output.
     *
     * @param frame
     *            the frame to get raw output from
     * @return an output stream for raw terminal output, or {@code null} if not
     *         supported
     */
    public static OutputStream rawOutput(Frame frame) {
        return frame.rawOutput();
    }
}
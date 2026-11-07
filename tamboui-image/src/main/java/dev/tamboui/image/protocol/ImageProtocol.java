/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.image.protocol;

import java.io.IOException;
import java.io.OutputStream;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.image.ImageData;
import dev.tamboui.image.capability.TerminalImageProtocol;
import dev.tamboui.layout.Rect;

/**
 * Protocol for rendering images to a terminal.
 * <p>
 * Implementations may render images using character-based approximations
 * (half-blocks, Braille patterns) or native terminal protocols (Sixel, Kitty, iTerm2).
 */
public interface ImageProtocol {

    /**
     * Renders the image to the given terminal area.
     * <p>
     * Character-based protocols write directly to the buffer.
     * Native protocols write to the raw output stream.
     *
     * @param image     the image data to render
     * @param area      the terminal area (in character cells) to render into
     * @param buffer    the buffer for character-based rendering
     * @param rawOutput the output stream for native protocol escape sequences (may be null)
     * @throws IOException if writing to rawOutput fails
     */
    void render(ImageData image, Rect area, Buffer buffer, OutputStream rawOutput) throws IOException;

    /**
     * Returns true if this protocol requires raw byte output.
     * <p>
     * Character-based protocols (half-block, braille) return false and use the buffer.
     * Native protocols (Sixel, Kitty, iTerm2) return true and use rawOutput.
     *
     * @return true if raw output is required
     */
    boolean requiresRawOutput();

    /**
     * Returns true if this protocol handles its own pixel scaling via cell dimensions
     * (e.g. Kitty's c=/r= or iTerm2's width=/height=).
     * <p>
     * Protocols that return true receive the original full-resolution image and a
     * display area computed in cells. Protocols that return false (Sixel, character-based)
     * receive a pre-scaled image.
     *
     * @return true if the protocol handles scaling internally
     */
    default boolean handlesOwnScaling() {
        return false;
    }

    /**
     * Returns the resolution multiplier for this protocol.
     * <p>
     * This indicates how many "virtual pixels" each terminal cell can represent:
     * <ul>
     *   <li>Half-block: 1x2 (1 column, 2 rows per cell)</li>
     *   <li>Braille: 2x4 (2 columns, 4 rows per cell)</li>
     *   <li>Native protocols: depends on cell pixel size</li>
     * </ul>
     *
     * @return the resolution multiplier
     */
    Resolution resolution();

    /**
     * Returns a human-readable name for this protocol.
     *
     * @return the protocol name
     */
    String name();

    /**
     * Returns the terminal image protocol type for capability checking.
     *
     * @return the protocol type
     */
    TerminalImageProtocol protocolType();

    /**
     * Resolution multiplier for a rendering protocol.
     */
    final class Resolution {
        private final int widthMultiplier;
        private final int heightMultiplier;

        /**
         * Creates a resolution multiplier.
         *
         * @param widthMultiplier  virtual pixels per cell horizontally
         * @param heightMultiplier virtual pixels per cell vertically
         */
        public Resolution(int widthMultiplier, int heightMultiplier) {
            this.widthMultiplier = widthMultiplier;
            this.heightMultiplier = heightMultiplier;
        }

        /**
         * Returns the width multiplier.
         *
         * @return virtual pixels per cell horizontally
         */
        public int widthMultiplier() {
            return widthMultiplier;
        }

        /**
         * Returns the height multiplier.
         *
         * @return virtual pixels per cell vertically
         */
        public int heightMultiplier() {
            return heightMultiplier;
        }
    }

    /**
     * Marks the cells covered by a native image in the buffer as continuation cells.
     * <p>
     * Marking covered cells prevents the terminal diff from overwriting the rendered image
     * in the current frame, and ensures that when the image is moved, resized, or removed in
     * subsequent frames, the diff naturally detects the uncovered cells and erases them by
     * writing spaces to the terminal.
     *
     * @param buffer the buffer to mark cells in (may be null)
     * @param area   the bounding box in character cells to mark
     */
    static void markCoveredCells(Buffer buffer, Rect area) {
        if (buffer == null || area == null || area.isEmpty()) {
            return;
        }
        Rect bufArea = buffer.area();
        int right = Math.min(area.right(), bufArea.right());
        int bottom = Math.min(area.bottom(), bufArea.bottom());
        int startX = Math.max(area.x(), bufArea.x());
        int startY = Math.max(area.y(), bufArea.y());

        for (int y = startY; y < bottom; y++) {
            for (int x = startX; x < right; x++) {
                buffer.set(x, y, Cell.CONTINUATION);
            }
        }
    }
}

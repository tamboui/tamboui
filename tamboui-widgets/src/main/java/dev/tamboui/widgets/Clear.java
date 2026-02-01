/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets;

import dev.tamboui.terminal.Terminal;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.buffer.Cell;
import dev.tamboui.layout.Rect;
import dev.tamboui.widget.Widget;

/**
 * A widget that clears/resets the area it is rendered to.
 * <p>
 * This widget is useful for layering widgets on top of each other,
 * such as rendering popups or modal dialogs over existing content.
 * By first rendering {@code Clear} to an area, you can ensure the
 * area is reset before rendering other widgets on top.
 *
 * <pre>{@code
 * // Clear an area before rendering a popup
 * frame.renderWidget(Clear.INSTANCE, popupArea);
 * frame.renderWidget(popupContent, popupArea);
 * }</pre>
 *
 * <p><b>Note:</b> This widget cannot be used to clear the terminal on the
 * first render, as the rendering system assumes the render area starts empty.
 * Use {@code Terminal.clear()} instead for initial screen clearing.
 *
 * @see Terminal#clear()
 */
public final class Clear implements Widget {

    /**
     * Singleton instance of the Clear widget.
     * <p>
     * Since Clear has no configuration, a single instance can be reused.
     */
    public static final Clear INSTANCE = new Clear();

    private Clear() {
        // Singleton
    }

    /**
     * Returns the singleton Clear widget instance.
     *
     * @return the singleton Clear instance
     */
    public static Clear clear() {
        return INSTANCE;
    }

    @Override
    public void render(Rect area, Buffer buffer) {
        for (int y = area.y(); y < area.bottom(); y++) {
            for (int x = area.x(); x < area.right(); x++) {
                buffer.set(x, y, Cell.EMPTY);
            }
        }
    }
}

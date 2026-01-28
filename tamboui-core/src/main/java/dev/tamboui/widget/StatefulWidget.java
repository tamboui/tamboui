/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widget;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;

/**
 * A widget that maintains state between renders.
 *
 * @param <S> the state type
 */
public interface StatefulWidget<S> {

    /**
     * Renders this widget to the given buffer area with the provided state.
     *
     * @param area   the area to render into
     * @param buffer the buffer to render to
     * @param state  the widget state
     */
    void render(Rect area, Buffer buffer, S state);
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widget;

/**
 * Side channel exposed by the raw output stream handed to {@link RawOutputCapable} widgets,
 * letting them detect when the terminal screen has been cleared.
 * <p>
 * Native image protocols (Kitty, iTerm2, Sixel) cache which image they have already
 * transmitted so they can skip redundant re-transmission on every frame of the render loop.
 * That cache is only valid as long as the terminal keeps showing what was last drawn. When
 * the screen is wiped — by an explicit {@link dev.tamboui.terminal.Terminal#clear()} or by a
 * resize — that assumption breaks and everything must be redrawn.
 * <p>
 * {@link #generation()} returns a counter that increments every time the screen is cleared.
 * A raw-output widget that observes a changed generation since its last emission must
 * retransmit, rather than assuming its previous output is still on screen.
 *
 * @see RawOutputCapable
 */
public interface RawOutputContext {

    /**
     * Returns the current screen generation.
     * <p>
     * The value increments whenever the terminal screen is cleared (via
     * {@link dev.tamboui.terminal.Terminal#clear()} or a resize). A widget that caches
     * already-transmitted output should treat a change in this value as an instruction to
     * redraw.
     *
     * @return the current screen generation counter
     */
    long generation();
}

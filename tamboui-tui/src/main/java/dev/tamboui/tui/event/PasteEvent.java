/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

/**
 * Represents a paste event triggered by bracketed paste mode.
 * <p>
 * When the terminal has bracketed paste mode enabled, pasted text is wrapped
 * with {@code ESC[200~} and {@code ESC[201~} markers, allowing the full
 * pasted string to be delivered as a single event rather than a sequence of
 * individual {@link KeyEvent}s.
 * <p>
 * This preserves multi-codepoint sequences such as ZWJ emoji (e.g. 👨‍👨‍👧‍👧)
 * and any other Unicode content that cannot survive character-by-character delivery.
 */
public final class PasteEvent implements Event {

    private final String text;

    public PasteEvent(String text) {
        this.text = text;
    }

    /**
     * Returns the pasted text.
     *
     * @return the full pasted string, never null
     */
    public String text() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PasteEvent)) return false;
        return text.equals(((PasteEvent) o).text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return "PasteEvent[text=" + text + "]";
    }
}

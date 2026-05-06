/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.input;

/**
 * Utilities for navigating grapheme clusters in a {@link StringBuilder}.
 * A grapheme cluster is the smallest user-perceived unit of text; it may span
 * multiple Unicode code points joined by Zero Width Joiners (U+200D), e.g.
 * the family emoji 👨‍👨‍👧‍👧.
 */
final class GraphemeClusters {

    private GraphemeClusters() {
    }

    /**
     * Returns the char offset of the start of the grapheme cluster whose last
     * code point ends at {@code pos}.  Walks backward across the full ZWJ
     * sequence so that an entire joined emoji is treated as one unit.
     */
    static int clusterStart(StringBuilder text, int pos) {
        int start = prevCpStart(text, pos);
        while (start > 0) {
            int zwjStart = prevCpStart(text, start);
            if (text.codePointAt(zwjStart) == 0x200D && zwjStart > 0) {
                start = prevCpStart(text, zwjStart);
            } else {
                break;
            }
        }
        return start;
    }

    /**
     * Returns the char offset past the end of the grapheme cluster that starts
     * at {@code pos}.  Walks forward across the full ZWJ sequence.
     */
    static int clusterEnd(StringBuilder text, int pos) {
        int cp = text.codePointAt(pos);
        int end = pos + Character.charCount(cp);
        while (end < text.length()) {
            int nextCp = text.codePointAt(end);
            if (nextCp == 0x200D) {
                end += Character.charCount(nextCp);
                if (end < text.length()) {
                    end += Character.charCount(text.codePointAt(end));
                }
            } else {
                break;
            }
        }
        return end;
    }

    // Returns the char offset of the start of the code point ending just before pos.
    private static int prevCpStart(StringBuilder text, int pos) {
        char c = text.charAt(pos - 1);
        if (Character.isLowSurrogate(c) && pos >= 2 && Character.isHighSurrogate(text.charAt(pos - 2))) {
            return pos - 2;
        }
        return pos - 1;
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.input;

/**
 * Utilities for navigating grapheme clusters in a {@link StringBuilder}.
 * <p>
 * A grapheme cluster is the smallest user-perceived unit of text and may span
 * multiple Unicode code points. This class implements a pragmatic subset of
 * UAX #29 sufficient for typing, deletion and cursor movement in interactive
 * widgets. The following extension cases are recognized:
 * <ul>
 *   <li>Combining marks of categories Mn (non-spacing), Mc (spacing combining)
 *       and Me (enclosing), e.g. {@code e} + U+0301 = {@code é}</li>
 *   <li>Variation selectors (e.g. U+FE0F), which fall under category Mn</li>
 *   <li>Emoji modifier (skin tone) sequences, U+1F3FB&ndash;U+1F3FF</li>
 *   <li>Zero Width Joiner sequences, e.g. the family emoji 👨‍👨‍👧‍👧</li>
 *   <li>Regional Indicator pairs forming flag emoji (e.g. 🇫🇷)</li>
 * </ul>
 * <p>
 * Cases not handled include complex Indic conjuncts and some less common
 * cluster shapes; for those, the algorithm falls back to per-code-point
 * navigation.
 */
final class GraphemeClusters {

    private static final int ZWJ = 0x200D;
    private static final int RI_FIRST = 0x1F1E6;
    private static final int RI_LAST = 0x1F1FF;
    private static final int SKIN_TONE_FIRST = 0x1F3FB;
    private static final int SKIN_TONE_LAST = 0x1F3FF;

    private GraphemeClusters() {
    }

    /**
     * Returns the char offset of the start of the grapheme cluster whose last
     * code point ends at {@code pos}.
     */
    static int clusterStart(StringBuilder text, int pos) {
        int start = prevCpStart(text, pos);
        boolean changed = true;
        while (changed && start > 0) {
            changed = false;
            int currCp = text.codePointAt(start);

            // Combining marks, variation selectors, skin tone modifiers extend backward.
            if (isExtender(currCp)) {
                start = prevCpStart(text, start);
                changed = true;
                continue;
            }

            // Regional Indicator: pair with the preceding RI iff the run of
            // consecutive RIs ending here has odd length.
            if (isRegionalIndicator(currCp)) {
                if (countRegionalIndicatorsBefore(text, start) % 2 == 1) {
                    start = prevCpStart(text, start);
                    changed = true;
                }
                continue;
            }

            // ZWJ-joined element: walk back over the joiner and the preceding element.
            int prevStart = prevCpStart(text, start);
            if (text.codePointAt(prevStart) == ZWJ && prevStart > 0) {
                start = prevCpStart(text, prevStart);
                changed = true;
            }
        }
        return start;
    }

    /**
     * Returns the char offset past the end of the grapheme cluster that starts
     * at {@code pos}.
     */
    static int clusterEnd(StringBuilder text, int pos) {
        int cp = text.codePointAt(pos);
        int end = pos + Character.charCount(cp);

        // Regional Indicator pair (flag): consume the second RI as part of the cluster.
        if (isRegionalIndicator(cp) && end < text.length()) {
            int nextCp = text.codePointAt(end);
            if (isRegionalIndicator(nextCp)) {
                return end + Character.charCount(nextCp);
            }
        }

        while (end < text.length()) {
            int nextCp = text.codePointAt(end);
            if (isExtender(nextCp)) {
                end += Character.charCount(nextCp);
            } else if (nextCp == ZWJ) {
                int afterZwj = end + Character.charCount(nextCp);
                if (afterZwj >= text.length()) {
                    break;
                }
                int joinedCp = text.codePointAt(afterZwj);
                end = afterZwj + Character.charCount(joinedCp);
            } else {
                break;
            }
        }
        return end;
    }

    private static boolean isExtender(int cp) {
        int type = Character.getType(cp);
        if (type == Character.NON_SPACING_MARK
            || type == Character.COMBINING_SPACING_MARK
            || type == Character.ENCLOSING_MARK) {
            return true;
        }
        return cp >= SKIN_TONE_FIRST && cp <= SKIN_TONE_LAST;
    }

    private static boolean isRegionalIndicator(int cp) {
        return cp >= RI_FIRST && cp <= RI_LAST;
    }

    private static int countRegionalIndicatorsBefore(StringBuilder text, int pos) {
        int count = 0;
        int p = pos;
        while (p > 0) {
            int prev = prevCpStart(text, p);
            if (!isRegionalIndicator(text.codePointAt(prev))) {
                break;
            }
            count++;
            p = prev;
        }
        return count;
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

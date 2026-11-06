/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import java.text.BreakIterator;

/**
 * Finds word boundaries for word-wise cursor movement, the way Ctrl+Left and Ctrl+Right move in a
 * text field.
 * <p>
 * Built on the JDK's {@link BreakIterator#getWordInstance()}, so word breaking follows the Unicode
 * text-segmentation rules (UAX&nbsp;#29): it handles scripts without spaces (CJK), keeps
 * identifiers and numbers together, and treats punctuation as its own boundary. "Next/previous
 * word" means the start of the next/previous run of letters or digits; whitespace and punctuation
 * runs are skipped. Boundaries always fall on code-point edges, so they never split a surrogate
 * pair.
 */
public final class WordBoundaries {

    private WordBoundaries() {
    }

    /**
     * Returns the index of the start of the next word at or after {@code pos}. At or past the end it
     * returns the text length.
     *
     * @param text the text to scan
     * @param pos the current index
     * @return the index of the next word start, or the text length
     */
    public static int nextWord(CharSequence text, int pos) {
        String s = text.toString();
        int n = s.length();
        int i = clamp(pos, n);
        BreakIterator words = BreakIterator.getWordInstance();
        words.setText(s);
        for (int b = words.following(i); b != BreakIterator.DONE; b = words.next()) {
            if (isWordStart(s, b)) {
                return b;
            }
        }
        return n;
    }

    /**
     * Returns the index of the start of the previous word before {@code pos}. At or before the
     * start it returns {@code 0}.
     *
     * @param text the text to scan
     * @param pos the current index
     * @return the index of the previous word start, or {@code 0}
     */
    public static int prevWord(CharSequence text, int pos) {
        String s = text.toString();
        int i = clamp(pos, s.length());
        BreakIterator words = BreakIterator.getWordInstance();
        words.setText(s);
        for (int b = words.preceding(i); b != BreakIterator.DONE; b = words.previous()) {
            if (isWordStart(s, b)) {
                return b;
            }
        }
        return 0;
    }

    // A boundary starts a word when the segment beginning there is a run of letters or digits,
    // as opposed to whitespace or punctuation.
    private static boolean isWordStart(String s, int boundary) {
        return boundary < s.length() && Character.isLetterOrDigit(s.codePointAt(boundary));
    }

    private static int clamp(int pos, int length) {
        if (pos < 0) {
            return 0;
        }
        return Math.min(pos, length);
    }
}

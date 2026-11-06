/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WordBoundariesTest {

    @Test
    @DisplayName("nextWord moves to the start of the following word, then to the end of the text")
    void nextWord() {
        String s = "foo bar baz";
        assertThat(WordBoundaries.nextWord(s, 0)).isEqualTo(4);  // |foo -> |bar
        assertThat(WordBoundaries.nextWord(s, 1)).isEqualTo(4);  // f|oo -> |bar
        assertThat(WordBoundaries.nextWord(s, 4)).isEqualTo(8);  // |bar -> |baz
        assertThat(WordBoundaries.nextWord(s, 8)).isEqualTo(11); // |baz -> end
        assertThat(WordBoundaries.nextWord(s, 11)).isEqualTo(11);
    }

    @Test
    @DisplayName("prevWord moves to the start of the preceding word, then to the start of the text")
    void prevWord() {
        String s = "foo bar baz";
        assertThat(WordBoundaries.prevWord(s, 11)).isEqualTo(8); // baz| -> |baz
        assertThat(WordBoundaries.prevWord(s, 8)).isEqualTo(4);  // |baz -> |bar
        assertThat(WordBoundaries.prevWord(s, 6)).isEqualTo(4);  // ba|r -> |bar
        assertThat(WordBoundaries.prevWord(s, 4)).isEqualTo(0);  // |bar -> |foo
        assertThat(WordBoundaries.prevWord(s, 0)).isEqualTo(0);
    }

    @Test
    @DisplayName("runs of whitespace are skipped as one gap")
    void multipleSpaces() {
        String s = "foo    bar";
        assertThat(WordBoundaries.nextWord(s, 0)).isEqualTo(7); // skip foo + 4 spaces
        assertThat(WordBoundaries.prevWord(s, 7)).isEqualTo(0); // skip spaces + foo
    }

    @Test
    @DisplayName("out-of-range positions are clamped")
    void clamped() {
        String s = "ab cd";
        assertThat(WordBoundaries.nextWord(s, -5)).isEqualTo(3);
        assertThat(WordBoundaries.prevWord(s, 99)).isEqualTo(3);
    }

    @Test
    @DisplayName("punctuation is a boundary (Unicode word segmentation), not just whitespace")
    void punctuation() {
        String s = "a+b (c)";
        assertThat(WordBoundaries.nextWord(s, 0)).isEqualTo(2); // a|+b -> stops at b
        assertThat(WordBoundaries.nextWord(s, 2)).isEqualTo(5); // -> c
        assertThat(WordBoundaries.prevWord(s, 5)).isEqualTo(2); // c -> b
    }

    @Test
    @DisplayName("scripts without spaces are segmented (CJK)")
    void cjk() {
        String s = "foo\u65e5\u672c"; // foo + two CJK ideographs, no spaces
        // BreakIterator segments the CJK run from the latin run rather than treating it all as one word.
        assertThat(WordBoundaries.nextWord(s, 0)).isEqualTo(3);
    }

    @Test
    @DisplayName("word jumps always land on a grapheme-cluster boundary (no splitting emoji, flags, combining marks)")
    void landsOnGraphemeBoundaries() {
        String[] samples = {
            "a\uD83D\uDC69\u200D\uD83D\uDCBBb",          // a + ZWJ emoji (woman technologist) + b
            "x\uD83C\uDDEF\uD83C\uDDF5y",                // x + flag (JP regional-indicator pair) + y
            "e\u0301 cafe\u0301",                          // combining acute accents
            "\u65e5\u672c \u8a9e",                         // wide CJK
            "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67 hi" // family emoji + text
        };
        for (String s : samples) {
            Set<Integer> graphemeBoundaries = new HashSet<>();
            for (int i = 0; i < s.length(); i = GraphemeClusters.clusterEnd(s, i)) {
                graphemeBoundaries.add(i);
            }
            graphemeBoundaries.add(s.length());
            for (int i = 0; i <= s.length(); i++) {
                assertThat(graphemeBoundaries).as("nextWord(%d) in %s", i, s).contains(WordBoundaries.nextWord(s, i));
                assertThat(graphemeBoundaries).as("prevWord(%d) in %s", i, s).contains(WordBoundaries.prevWord(s, i));
            }
        }
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.input;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GraphemeClustersTest {

    private static StringBuilder sb(String s) {
        return new StringBuilder(s);
    }

    @Nested
    @DisplayName("clusterStart")
    class ClusterStart {

        @Test
        @DisplayName("walks back one ASCII code point")
        void asciiCodePoint() {
            assertThat(GraphemeClusters.clusterStart(sb("ab"), 2)).isEqualTo(1);
            assertThat(GraphemeClusters.clusterStart(sb("ab"), 1)).isEqualTo(0);
        }

        @Test
        @DisplayName("walks back across a surrogate pair")
        void surrogatePair() {
            // 👍 (U+1F44D): 2 chars, 1 code point.
            String s = "👍";
            assertThat(GraphemeClusters.clusterStart(sb(s), 2)).isEqualTo(0);
        }

        @Test
        @DisplayName("treats a lone low surrogate as a single unit")
        void loneLowSurrogate() {
            // X + lone low surrogate
            String s = "X\uDC4D";
            assertThat(GraphemeClusters.clusterStart(sb(s), 2)).isEqualTo(1);
        }

        @Test
        @DisplayName("walks back across combining diacritical marks")
        void combiningMark() {
            // e + U+0301 (combining acute) = é decomposed
            String s = "é";
            assertThat(GraphemeClusters.clusterStart(sb(s), 2)).isEqualTo(0);
        }

        @Test
        @DisplayName("walks back across multiple combining marks")
        void multipleCombiningMarks() {
            // a + U+0301 + U+0308 (acute + diaeresis)
            String s = "á̈";
            assertThat(GraphemeClusters.clusterStart(sb(s), 3)).isEqualTo(0);
        }

        @Test
        @DisplayName("walks back across a variation selector")
        void variationSelector() {
            // ❤ (U+2764) + VS16 (U+FE0F) → red heart emoji presentation
            String s = "❤️";
            assertThat(GraphemeClusters.clusterStart(sb(s), 2)).isEqualTo(0);
        }

        @Test
        @DisplayName("walks back across an emoji + skin tone modifier")
        void skinToneModifier() {
            // 👍 (U+1F44D) + 🏽 (U+1F3FD, medium skin tone) → 👍🏽
            String s = "👍🏽";
            assertThat(GraphemeClusters.clusterStart(sb(s), 4)).isEqualTo(0);
        }

        @Test
        @DisplayName("walks back across a ZWJ sequence")
        void zwjSequence() {
            // 👨 + ZWJ + 👨 + ZWJ + 👧 + ZWJ + 👧 (family emoji)
            String s = "👨‍👨‍👧‍👧";
            assertThat(GraphemeClusters.clusterStart(sb(s), s.length())).isZero();
        }

        @Test
        @DisplayName("walks back across a ZWJ sequence with embedded variation selector")
        void zwjSequenceWithVariationSelector() {
            // 👨 + ZWJ + ❤ + VS16 + ZWJ + 👨
            String s = "👨‍❤️‍👨";
            assertThat(GraphemeClusters.clusterStart(sb(s), s.length())).isZero();
        }

        @Test
        @DisplayName("walks back across a regional indicator pair (flag)")
        void regionalIndicatorPair() {
            // 🇫🇷 (FR flag): regional indicator F + R
            String s = "🇫🇷";
            assertThat(GraphemeClusters.clusterStart(sb(s), 4)).isZero();
        }

        @Test
        @DisplayName("regional indicator after a flag stands alone")
        void regionalIndicatorAfterFlag() {
            // 🇫🇷 + 🇩 (lone): flag (4 chars) + lone RI (2 chars)
            String s = "🇫🇷🇩";
            // From end of lone RI: stays as lone RI (preceding RI run is even).
            assertThat(GraphemeClusters.clusterStart(sb(s), 6)).isEqualTo(4);
            // From end of flag: walks back to start.
            assertThat(GraphemeClusters.clusterStart(sb(s), 4)).isZero();
        }

        @Test
        @DisplayName("two adjacent flags")
        void twoFlags() {
            // 🇫🇷 + 🇩🇪
            String s = "🇫🇷🇩🇪";
            assertThat(GraphemeClusters.clusterStart(sb(s), 8)).isEqualTo(4);
            assertThat(GraphemeClusters.clusterStart(sb(s), 4)).isZero();
        }

        @Test
        @DisplayName("plain text has no cluster extension")
        void plainText() {
            String s = "hello";
            assertThat(GraphemeClusters.clusterStart(sb(s), 5)).isEqualTo(4);
            assertThat(GraphemeClusters.clusterStart(sb(s), 1)).isZero();
        }
    }

    @Nested
    @DisplayName("clusterEnd")
    class ClusterEnd {

        @Test
        @DisplayName("returns the offset past a single ASCII char")
        void ascii() {
            assertThat(GraphemeClusters.clusterEnd(sb("ab"), 0)).isEqualTo(1);
        }

        @Test
        @DisplayName("returns the offset past a surrogate pair")
        void surrogatePair() {
            String s = "👍";
            assertThat(GraphemeClusters.clusterEnd(sb(s), 0)).isEqualTo(2);
        }

        @Test
        @DisplayName("includes a trailing combining mark")
        void combiningMark() {
            String s = "é";
            assertThat(GraphemeClusters.clusterEnd(sb(s), 0)).isEqualTo(2);
        }

        @Test
        @DisplayName("includes a trailing variation selector")
        void variationSelector() {
            String s = "❤️";
            assertThat(GraphemeClusters.clusterEnd(sb(s), 0)).isEqualTo(2);
        }

        @Test
        @DisplayName("includes a trailing skin tone modifier")
        void skinToneModifier() {
            String s = "👍🏽";
            assertThat(GraphemeClusters.clusterEnd(sb(s), 0)).isEqualTo(4);
        }

        @Test
        @DisplayName("includes the entire ZWJ sequence")
        void zwjSequence() {
            String s = "👨‍👨‍👧‍👧";
            assertThat(GraphemeClusters.clusterEnd(sb(s), 0)).isEqualTo(s.length());
        }

        @Test
        @DisplayName("includes a regional indicator pair")
        void regionalIndicatorPair() {
            String s = "🇫🇷";
            assertThat(GraphemeClusters.clusterEnd(sb(s), 0)).isEqualTo(4);
        }

        @Test
        @DisplayName("trailing ZWJ without follower stops at the joiner")
        void trailingZwjWithoutFollower() {
            // a + ZWJ at end of text — nothing to join, end stops before the dangling ZWJ.
            String s = "a‍";
            assertThat(GraphemeClusters.clusterEnd(sb(s), 0)).isEqualTo(1);
        }
    }
}

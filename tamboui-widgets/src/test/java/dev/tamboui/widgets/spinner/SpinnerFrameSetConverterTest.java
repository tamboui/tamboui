/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.spinner;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SpinnerFrameSetConverterTest {

    @Test
    void convertsDoubleQuotedFrames() {
        // Input: "-" "|" "/" "X" (4 frames)
        Optional<SpinnerFrameSet> result = SpinnerFrameSetConverter.INSTANCE.convert("\"-\" \"|\" \"/\" \"X\"");
        assertThat(result).isPresent();
        assertThat(result.get().frames()).containsExactly("-", "|", "/", "X");
    }

    @Test
    void convertsSingleQuotedFrames() {
        Optional<SpinnerFrameSet> result = SpinnerFrameSetConverter.INSTANCE.convert("'-' '|' '/' 'X'");
        assertThat(result).isPresent();
        assertThat(result.get().frames()).containsExactly("-", "|", "/", "X");
    }

    @Test
    void convertsMixedQuotes() {
        Optional<SpinnerFrameSet> result = SpinnerFrameSetConverter.INSTANCE.convert("'-' \"|\" '/' \"X\"");
        assertThat(result).isPresent();
        assertThat(result.get().frames()).containsExactly("-", "|", "/", "X");
    }

    @Test
    void preservesContentBetweenQuotes() {
        // The converter extracts content between quotes verbatim, without escape processing
        // Input CSS: "ab" "cd" - two frames with content "ab" and "cd"
        Optional<SpinnerFrameSet> result = SpinnerFrameSetConverter.INSTANCE.convert("\"ab\" \"cd\"");
        assertThat(result).isPresent();
        assertThat(result.get().frames()).containsExactly("ab", "cd");
    }

    @Test
    void convertsUnicodeFrames() {
        Optional<SpinnerFrameSet> result = SpinnerFrameSetConverter.INSTANCE.convert("\"⠋\" \"⠙\" \"⠹\" \"⠸\"");
        assertThat(result).isPresent();
        assertThat(result.get().frames()).containsExactly("⠋", "⠙", "⠹", "⠸");
    }

    @Test
    void convertsSimpleFrames() {
        Optional<SpinnerFrameSet> result = SpinnerFrameSetConverter.INSTANCE.convert("\"*\" \"+\" \"x\" \"+\"");
        assertThat(result).isPresent();
        assertThat(result.get().frames()).containsExactly("*", "+", "x", "+");
    }

    @Test
    void trimsWhitespace() {
        Optional<SpinnerFrameSet> result = SpinnerFrameSetConverter.INSTANCE.convert("  \"-\" \"|\"  ");
        assertThat(result).isPresent();
        assertThat(result.get().frames()).containsExactly("-", "|");
    }

    @Test
    void returnsEmptyForNullOrEmpty() {
        assertThat(SpinnerFrameSetConverter.INSTANCE.convert(null)).isEmpty();
        assertThat(SpinnerFrameSetConverter.INSTANCE.convert("")).isEmpty();
        assertThat(SpinnerFrameSetConverter.INSTANCE.convert("   ")).isEmpty();
    }

    @Test
    void returnsEmptyForUnterminatedQuote() {
        assertThat(SpinnerFrameSetConverter.INSTANCE.convert("\"unterminated")).isEmpty();
        assertThat(SpinnerFrameSetConverter.INSTANCE.convert("'also unterminated")).isEmpty();
    }

    @Test
    void returnsEmptyForNoQuotedStrings() {
        assertThat(SpinnerFrameSetConverter.INSTANCE.convert("no quotes here")).isEmpty();
    }

    @Test
    void handlesEmptyStrings() {
        Optional<SpinnerFrameSet> result = SpinnerFrameSetConverter.INSTANCE.convert("\"\" \"x\" \"\"");
        assertThat(result).isPresent();
        assertThat(result.get().frames()).containsExactly("", "x", "");
    }

    @Test
    void handlesMultiCharacterFrames() {
        Optional<SpinnerFrameSet> result = SpinnerFrameSetConverter.INSTANCE.convert("\"[    ]\" \"[=   ]\" \"[==  ]\" \"[=== ]\"");
        assertThat(result).isPresent();
        assertThat(result.get().frames()).containsExactly("[    ]", "[=   ]", "[==  ]", "[=== ]");
    }
}

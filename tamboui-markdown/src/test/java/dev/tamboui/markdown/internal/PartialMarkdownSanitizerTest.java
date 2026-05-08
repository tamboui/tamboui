/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.markdown.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PartialMarkdownSanitizerTest {

    @Test
    @DisplayName("returns null and empty inputs unchanged")
    void emptyInputs() {
        assertThat(PartialMarkdownSanitizer.sanitize(null)).isNull();
        assertThat(PartialMarkdownSanitizer.sanitize("")).isEmpty();
    }

    @Test
    @DisplayName("trims an unmatched trailing pair of asterisks")
    void unmatchedTrailingStrong() {
        assertThat(PartialMarkdownSanitizer.sanitize("hello **")).isEqualTo("hello ");
    }

    @Test
    @DisplayName("trims a single trailing asterisk that opens emphasis with no closer")
    void unmatchedTrailingEmphasis() {
        assertThat(PartialMarkdownSanitizer.sanitize("hello *world")).isEqualTo("hello *world");
        assertThat(PartialMarkdownSanitizer.sanitize("hello *")).isEqualTo("hello ");
    }

    @Test
    @DisplayName("trims a trailing single backtick used to open inline code")
    void unmatchedTrailingBacktick() {
        assertThat(PartialMarkdownSanitizer.sanitize("see `foo")).isEqualTo("see `foo");
        assertThat(PartialMarkdownSanitizer.sanitize("see `")).isEqualTo("see ");
    }

    @Test
    @DisplayName("trims a trailing strikethrough opener")
    void unmatchedTrailingStrikethrough() {
        assertThat(PartialMarkdownSanitizer.sanitize("ok ~~")).isEqualTo("ok ");
    }

    @Test
    @DisplayName("rewrites a dangling link [label]( to plain [label]")
    void danglingLink() {
        assertThat(PartialMarkdownSanitizer.sanitize("see [docs](https://exa"))
            .isEqualTo("see [docs]");
    }

    @Test
    @DisplayName("drops a trailing ATX header marker with no content")
    void trailingAtxHeader() {
        assertThat(PartialMarkdownSanitizer.sanitize("body\n##")).isEqualTo("body");
        assertThat(PartialMarkdownSanitizer.sanitize("body\n###  ")).isEqualTo("body");
    }

    @Test
    @DisplayName("returns balanced markup byte-identical")
    void balancedMarkupUnchanged() {
        String[] corpus = {
            "# Title\n\nplain text\n",
            "**bold** and *italic* and ~~strike~~ and `code`",
            "see [docs](https://example.com)",
            "```\nint x = 1;\n```",
            "1. one\n2. two\n",
            "- a\n- b\n",
            "> quote\n\nbody",
            "| h1 | h2 |\n| -- | -- |\n| a  | b  |",
            "---",
            ""
        };
        for (String input : corpus) {
            assertThat(PartialMarkdownSanitizer.sanitize(input))
                .as("balanced input must be byte-identical: %s", input)
                .isEqualTo(input);
        }
    }

    @Test
    @DisplayName("only touches the trailing fragment, not earlier balanced markup")
    void onlyTrailing() {
        String input = "**done** then *open";
        // Earlier balanced **done** stays; trailing single * gets trimmed.
        assertThat(PartialMarkdownSanitizer.sanitize(input))
            .startsWith("**done**");
    }

    @Test
    @DisplayName("treats unterminated code fence as code-block-to-EOF (left alone)")
    void unterminatedFence() {
        String input = "before\n\n```\nint x = 1;";
        assertThat(PartialMarkdownSanitizer.sanitize(input)).isEqualTo(input);
    }
}

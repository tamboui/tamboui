/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.text;

import static dev.tamboui.text.MarkupParser.containsEmojiCodes;
import static dev.tamboui.text.MarkupParser.replaceEmoji;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmojiMarkupParserTest {

    @Test
    @DisplayName("parse with custom emoji codes")
    void parseWithEmojiCodes() {
        Text text = MarkupParser.parse(":warning: :doesnotexist:Alert!");
        assertThat(text.rawContent()).contains("⚠");
        assertThat(text.rawContent()).contains(":doesnotexist:");

        text = MarkupParser.parse(":warning: :doesnotexist: Alert!", null, (string) -> "ALL!");
        assertThat(text.rawContent()).contains("ALL!");
        assertThat(text.rawContent()).doesNotContain("⚠");
        assertThat(text.rawContent()).doesNotContain(":warning:");
        assertThat(text.rawContent()).doesNotContain(":doesnotexist:");
    }

    @Test
    @DisplayName("replace basic emoji codes")
    void replaceBasicEmojiCodes() {
        assertThat(replaceEmoji("Hello :smiley:!")).isEqualTo("Hello 😃!");
        assertThat(replaceEmoji(":warning: Alert")).isEqualTo("⚠ Alert");
    }

    @Test
    @DisplayName("replace multiple emoji codes")
    void replaceMultipleEmojiCodes() {
        assertThat(replaceEmoji(":cross_mark: :warning:"))
            .isEqualTo("❌ ⚠");
    }

    @Test
    @DisplayName("unknown emoji codes are left unchanged")
    void unknownEmojiCodesLeftUnchanged() {
        assertThat(replaceEmoji(":unknown_emoji:")).isEqualTo(":unknown_emoji:");
        assertThat(replaceEmoji("Hello :xyz: world")).isEqualTo("Hello :xyz: world");
    }

    @Test
    @DisplayName("emoji codes are case-insensitive")
    void emojiCodesCaseInsensitive() {
        assertThat(replaceEmoji(":SMILEY:")).isEqualTo("😃");
        assertThat(replaceEmoji(":Warning:")).isEqualTo("⚠");
    }

    @Test
    @DisplayName("text without emoji codes is unchanged")
    void textWithoutEmojiCodesUnchanged() {
        assertThat(replaceEmoji("Hello world")).isEqualTo("Hello world");
        assertThat(replaceEmoji("")).isEqualTo("");
        assertThat(replaceEmoji(null)).isEqualTo("");
    }

    @Test
    @DisplayName("containsEmojiCodes detects emoji codes")
    void containsEmojiCodesDetects() {
        assertThat(containsEmojiCodes(":smiley:")).isTrue();
        assertThat(containsEmojiCodes("Hello :warning:!")).isTrue();
        assertThat(containsEmojiCodes("Hello world")).isFalse();
        assertThat(containsEmojiCodes("")).isFalse();
        assertThat(containsEmojiCodes(null)).isFalse();
    }

    @Test
    @DisplayName("MarkupParser.parse replaces emoji codes by default")
    void markupParserReplacesEmojiCodesByDefault() {
        Text text = MarkupParser.parse(":warning: Alert!");
        assertThat(text.rawContent()).contains("⚠");
    }

    @Test
    @DisplayName("MarkupParser.parse can disable emoji replacement")
    void markupParserCanDisableEmoji() {
        Text text = MarkupParser.parse(":warning: Alert!", null, false);
        assertThat(text.rawContent()).isEqualTo(":warning: Alert!");
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.buffer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;

import static org.assertj.core.api.Assertions.assertThat;

class BufferWideCharTest {

    @Test
    @DisplayName("CJK character occupies 2 cells with continuation")
    void cjkCharPlacesContinuation() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        buffer.setString(0, 0, "世", Style.EMPTY);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("世");
        assertThat(buffer.get(1, 0).isContinuation()).isTrue();
        assertThat(buffer.get(2, 0).symbol()).isEqualTo(" "); // unchanged
    }

    @Test
    @DisplayName("Emoji occupies 2 cells with continuation")
    void emojiPlacesContinuation() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        // 🔥 = U+1F525 (surrogate pair in UTF-16)
        buffer.setString(0, 0, "\uD83D\uDD25", Style.EMPTY);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("\uD83D\uDD25");
        assertThat(buffer.get(1, 0).isContinuation()).isTrue();
    }

    @Test
    @DisplayName("setString returns correct column position after wide characters")
    void setStringReturnsCorrectPosition() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 1));
        // "A世B" has widths [1, 2, 1] = total 4 columns
        int endCol = buffer.setString(0, 0, "A世B", Style.EMPTY);

        assertThat(endCol).isEqualTo(4);
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("世");
        assertThat(buffer.get(2, 0).isContinuation()).isTrue();
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("B");
    }

    @Test
    @DisplayName("Wide char at rightmost column is replaced with space")
    void wideCharAtEdgeReplacedWithSpace() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 1));
        // Place a wide char at column 4 (rightmost column), no room for continuation
        buffer.setString(4, 0, "世", Style.EMPTY);

        // Should be replaced with a space since there's no room for 2 columns
        assertThat(buffer.get(4, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Multiple CJK characters render correctly")
    void multipleCjkChars() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        buffer.setString(0, 0, "世界", Style.EMPTY);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("世");
        assertThat(buffer.get(1, 0).isContinuation()).isTrue();
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("界");
        assertThat(buffer.get(3, 0).isContinuation()).isTrue();
    }

    @Test
    @DisplayName("Overwriting continuation cell clears the wide char")
    void overwriteContinuationClearsWideChar() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        // Place a wide char
        buffer.setString(0, 0, "世", Style.EMPTY);
        // Overwrite the continuation cell (column 1)
        buffer.setString(1, 0, "X", Style.EMPTY);

        // The wide char at column 0 should be cleared to space
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("X");
    }

    @Test
    @DisplayName("toAnsiString skips continuation cells")
    void toAnsiStringSkipsContinuation() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 4, 1));
        buffer.setString(0, 0, "世界", Style.EMPTY);

        String result = buffer.toAnsiString();
        assertThat(result).contains("世界");
        // Should NOT contain empty strings where continuations would be
    }

    @Test
    @DisplayName("toAnsiStringTrimmed handles wide chars correctly")
    void toAnsiStringTrimmedWideChars() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        buffer.setString(0, 0, "世界", Style.EMPTY);

        String trimmed = buffer.toAnsiStringTrimmed();
        assertThat(trimmed).contains("世界");
    }

    @Test
    @DisplayName("Zero-width chars append to preceding cell")
    void zeroWidthCharsAppendToPreceding() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        // 'e' followed by combining acute accent (U+0301)
        buffer.setString(0, 0, "e\u0301", Style.EMPTY);

        // The combining mark should be appended to the 'e' cell
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("e\u0301");
        // Column 1 should still be empty (combining mark doesn't advance)
        assertThat(buffer.get(1, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Mixed ASCII and CJK in setString")
    void mixedAsciiAndCjk() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 1));
        buffer.setString(0, 0, "Hi世界OK", Style.EMPTY);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("i");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("世");
        assertThat(buffer.get(3, 0).isContinuation()).isTrue();
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("界");
        assertThat(buffer.get(5, 0).isContinuation()).isTrue();
        assertThat(buffer.get(6, 0).symbol()).isEqualTo("O");
        assertThat(buffer.get(7, 0).symbol()).isEqualTo("K");
    }

    @Test
    @DisplayName("withLines uses display width for buffer dimensions")
    void withLinesUsesDisplayWidth() {
        Buffer buffer = Buffer.withLines("世界", "AB");
        // "世界" = width 4, "AB" = width 2 -> buffer width should be 4
        assertThat(buffer.area().width()).isEqualTo(4);
        assertThat(buffer.area().height()).isEqualTo(2);
    }

    @Test
    @DisplayName("diff includes continuation cells")
    void diffIncludesContinuation() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer prev = Buffer.empty(area);
        Buffer curr = Buffer.empty(area);

        curr.setString(0, 0, "世", Style.EMPTY);

        DiffResult diff = new DiffResult();
        prev.diff(curr, diff);
        // Should include the wide char cell and the continuation cell
        assertThat(diff.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("ZWJ emoji occupies two cells")
    void zwjEmojiOccupiesTwoCells() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        // 👨‍👦 = man (U+1F468) + ZWJ (U+200D) + boy (U+1F466)
        String family = "\uD83D\uDC68\u200D\uD83D\uDC66";
        buffer.setString(0, 0, family, Style.EMPTY);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo(family);
        assertThat(buffer.get(1, 0).isContinuation()).isTrue();
        assertThat(buffer.get(2, 0).symbol()).isEqualTo(" "); // Empty, not another char
    }

    @Test
    @DisplayName("Flag emoji occupies two cells")
    void flagEmojiOccupiesTwoCells() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        // 🇬🇱 = Regional Indicator G (U+1F1EC) + Regional Indicator L (U+1F1F1)
        String greenland = "\uD83C\uDDEC\uD83C\uDDF1";
        buffer.setString(0, 0, greenland, Style.EMPTY);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo(greenland);
        assertThat(buffer.get(1, 0).isContinuation()).isTrue();
        assertThat(buffer.get(2, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Skin tone emoji occupies two cells")
    void skinToneEmojiOccupiesTwoCells() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        // 👋🏻 = waving hand (U+1F44B) + light skin tone (U+1F3FB)
        String wave = "\uD83D\uDC4B\uD83C\uDFFB";
        buffer.setString(0, 0, wave, Style.EMPTY);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo(wave);
        assertThat(buffer.get(1, 0).isContinuation()).isTrue();
        assertThat(buffer.get(2, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Emoji presentation sequence (base + VS16) occupies two cells")
    void emojiPresentationSequenceOccupiesTwoCells() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        // \u2328\uFE0F = keyboard (U+2328) + Variation Selector-16 (U+FE0F).
        // The base glyph is 1-wide on its own but renders as a 2-wide emoji with VS16.
        String keyboard = "\u2328\uFE0F";
        int endCol = buffer.setString(0, 0, keyboard, Style.EMPTY);

        assertThat(endCol).isEqualTo(2);
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(keyboard);
        assertThat(buffer.get(1, 0).isContinuation()).isTrue();
        assertThat(buffer.get(2, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Emoji presentation sequence does not leave stale cells when text follows")
    void emojiPresentationSequenceWithTrailingText() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 1));
        // "\u23FA\uFE0F Rec" : record (U+23FA) + VS16, space, then text.
        String label = "\u23FA\uFE0F Rec";
        buffer.setString(0, 0, label, Style.EMPTY);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("\u23FA\uFE0F");
        assertThat(buffer.get(1, 0).isContinuation()).isTrue();
        assertThat(buffer.get(2, 0).symbol()).isEqualTo(" ");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("R");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("e");
        assertThat(buffer.get(5, 0).symbol()).isEqualTo("c");
    }

    @Test
    @DisplayName("Already-wide emoji with VS16 stays two cells")
    void alreadyWideEmojiWithVariationSelectorStaysTwoCells() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        // \uD83D\uDDA5\uFE0F = desktop computer (U+1F5A5, already 2-wide) + VS16 must stay 2-wide
        String desktop = "\uD83D\uDDA5\uFE0F";
        int endCol = buffer.setString(0, 0, desktop, Style.EMPTY);

        assertThat(endCol).isEqualTo(2);
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(desktop);
        assertThat(buffer.get(1, 0).isContinuation()).isTrue();
        assertThat(buffer.get(2, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Emoji presentation sequence at the right edge degrades to its 1-wide base")
    void emojiPresentationSequenceAtRightEdgeTruncates() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 2, 1));
        // The base glyph (U+2328) lands on the last column, so the loop breaks before
        // VS16 is reached. Rather than overflowing or leaving a stale continuation, the
        // glyph degrades to its 1-wide text presentation, matching how wide characters
        // truncate at the right edge. This documents the one boundary where setString's
        // return value (1 column consumed) intentionally differs from CharWidth.of (2).
        int endCol = buffer.setString(1, 0, "⌨️", Style.EMPTY);

        assertThat(endCol).isEqualTo(2);
        // Only the base glyph (U+2328) is placed; VS16 is never appended.
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("⌨");
        assertThat(buffer.get(1, 0).isContinuation()).isFalse();
        // The cell to the left is untouched (still empty).
        assertThat(buffer.get(0, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("Multiple flag emoji render correctly")
    void multipleFlagEmojiRenderCorrectly() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        // 🇫🇷🇬🇱 = France + Greenland
        String france = "\uD83C\uDDEB\uD83C\uDDF7";
        String greenland = "\uD83C\uDDEC\uD83C\uDDF1";
        buffer.setString(0, 0, france + greenland, Style.EMPTY);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo(france);
        assertThat(buffer.get(1, 0).isContinuation()).isTrue();
        assertThat(buffer.get(2, 0).symbol()).isEqualTo(greenland);
        assertThat(buffer.get(3, 0).isContinuation()).isTrue();
    }

    @Test
    @DisplayName("ZWJ emoji with text before and after")
    void zwjEmojiWithSurroundingText() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 1));
        // 👨‍🌾 = farmer emoji
        String farmer = "\uD83E\uDDD1\u200D\uD83C\uDF3E";
        buffer.setString(0, 0, "A" + farmer + "B", Style.EMPTY);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("A");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo(farmer);
        assertThat(buffer.get(2, 0).isContinuation()).isTrue();
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("B");
    }

    @Test
    @DisplayName("setString returns correct position after ZWJ emoji")
    void setStringReturnsCorrectPositionAfterZwj() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 1));
        // 👨‍👦 = family emoji, width 2
        String family = "\uD83D\uDC68\u200D\uD83D\uDC66";
        int endCol = buffer.setString(0, 0, family, Style.EMPTY);

        // ZWJ emoji should occupy 2 columns
        assertThat(endCol).isEqualTo(2);
    }

    @Test
    @DisplayName("setString returns correct position after flag emoji")
    void setStringReturnsCorrectPositionAfterFlag() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 1));
        // 🇬🇱 = Greenland flag, width 2
        String greenland = "\uD83C\uDDEC\uD83C\uDDF1";
        int endCol = buffer.setString(0, 0, greenland, Style.EMPTY);

        assertThat(endCol).isEqualTo(2);
    }

    @Test
    @DisplayName("Flag emoji at edge is replaced with space")
    void flagEmojiAtEdgeReplacedWithSpace() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 5, 1));
        // Place a flag at column 4 (rightmost column), no room for 2-wide char
        String greenland = "\uD83C\uDDEC\uD83C\uDDF1";
        buffer.setString(4, 0, greenland, Style.EMPTY);

        // Should be replaced with a space since there's no room for 2 columns
        assertThat(buffer.get(4, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("bald_man ZWJ emoji occupies two cells and returns correct position")
    void baldManZwjEmojiOccupiesTwoCells() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        // 👨‍🦲 = man (U+1F468) + ZWJ (U+200D) + bald (U+1F9B2)
        String baldMan = "\uD83D\uDC68\u200D\uD83E\uDDB2";
        int endCol = buffer.setString(0, 0, baldMan, Style.EMPTY);

        // Should return position 2 (width of combined emoji)
        assertThat(endCol).isEqualTo(2);

        // Cell 0 should contain the FULL ZWJ sequence (all 3 codepoints)
        String cellSymbol = buffer.get(0, 0).symbol();
        assertThat(cellSymbol).isEqualTo(baldMan);

        // Verify the cell contains exactly 3 codepoints: man + ZWJ + bald
        int[] codepoints = cellSymbol.codePoints().toArray();
        assertThat(codepoints).hasSize(3);
        assertThat(codepoints[0]).isEqualTo(0x1F468); // man
        assertThat(codepoints[1]).isEqualTo(0x200D);  // ZWJ
        assertThat(codepoints[2]).isEqualTo(0x1F9B2); // bald

        // Cell 1 should be continuation
        assertThat(buffer.get(1, 0).isContinuation()).isTrue();

        // Cell 2 should be empty (space)
        assertThat(buffer.get(2, 0).symbol()).isEqualTo(" ");
    }

    @Test
    @DisplayName("diff between simple emoji and ZWJ emoji produces correct updates")
    void diffBetweenSimpleAndZwjEmoji() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer prev = Buffer.empty(area);
        Buffer curr = Buffer.empty(area);

        // Previous buffer has simple emoji
        String baby = "\uD83D\uDC76"; // 👶
        prev.setString(0, 0, baby, Style.EMPTY);

        // Current buffer has ZWJ emoji
        String baldMan = "\uD83D\uDC68\u200D\uD83E\uDDB2"; // 👨‍🦲
        curr.setString(0, 0, baldMan, Style.EMPTY);

        DiffResult diff = new DiffResult();
        prev.diff(curr, diff);

        // Should only have 1 update (cell 0 changed, cell 1 is CONTINUATION in both)
        // Cell 0: baby -> baldMan
        // Cell 1: CONTINUATION -> CONTINUATION (no change)
        assertThat(diff.size()).isEqualTo(1);
        assertThat(diff.getX(0)).isEqualTo(0);
        assertThat(diff.getCell(0).symbol()).isEqualTo(baldMan);
    }

    @Test
    @DisplayName("diff when previous cell had content where continuation will be")
    void diffWhenPreviousHadContentAtContinuationPosition() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer prev = Buffer.empty(area);
        Buffer curr = Buffer.empty(area);

        // Previous buffer has two 1-wide characters
        prev.setString(0, 0, "AB", Style.EMPTY);

        // Current buffer has ZWJ emoji
        String baldMan = "\uD83D\uDC68\u200D\uD83E\uDDB2"; // 👨‍🦲
        curr.setString(0, 0, baldMan, Style.EMPTY);

        DiffResult diff = new DiffResult();
        prev.diff(curr, diff);

        // Should have 2 updates:
        // Cell 0: "A" -> baldMan
        // Cell 1: "B" -> CONTINUATION
        assertThat(diff.size()).isEqualTo(2);

        // Check both updates are present
        boolean hasCell0 = false;
        boolean hasCell1 = false;
        for (int i = 0; i < diff.size(); i++) {
            if (diff.getX(i) == 0 && diff.getCell(i).symbol().equals(baldMan)) hasCell0 = true;
            if (diff.getX(i) == 1 && diff.getCell(i).isContinuation()) hasCell1 = true;
        }
        assertThat(hasCell0).isTrue();
        assertThat(hasCell1).isTrue();
    }
}

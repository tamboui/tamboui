/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.buffer;

import dev.tamboui.layout.Rect;
import dev.tamboui.style.Style;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

        java.util.List<CellUpdate> updates = prev.diff(curr);
        // Should include the wide char cell and the continuation cell
        assertThat(updates).hasSize(2);
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.input;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.style.ContinuationIndent;
import dev.tamboui.style.Overflow;
import dev.tamboui.text.CharWidth;
import dev.tamboui.text.TextWrapOptions;

import static org.assertj.core.api.Assertions.*;

class TextAreaStateTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("Default constructor creates empty state with one line")
        void defaultConstructor() {
            TextAreaState state = new TextAreaState();

            assertThat(state.text()).isEmpty();
            assertThat(state.lineCount()).isEqualTo(1);
            assertThat(state.cursorRow()).isEqualTo(0);
            assertThat(state.cursorCol()).isEqualTo(0);
        }

        @Test
        @DisplayName("Constructor with initial text")
        void constructorWithText() {
            TextAreaState state = new TextAreaState("Hello\nWorld");

            assertThat(state.text()).isEqualTo("Hello\nWorld");
            assertThat(state.lineCount()).isEqualTo(2);
            assertThat(state.getLine(0)).isEqualTo("Hello");
            assertThat(state.getLine(1)).isEqualTo("World");
        }

        @Test
        @DisplayName("Constructor with null text creates empty state")
        void constructorWithNull() {
            TextAreaState state = new TextAreaState(null);

            assertThat(state.text()).isEmpty();
            assertThat(state.lineCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Constructor with empty text creates single empty line")
        void constructorWithEmptyText() {
            TextAreaState state = new TextAreaState("");

            assertThat(state.text()).isEmpty();
            assertThat(state.lineCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Text Insertion")
    class TextInsertion {

        @Test
        @DisplayName("Insert single character")
        void insertChar() {
            TextAreaState state = new TextAreaState();
            state.insert('a');

            assertThat(state.text()).isEqualTo("a");
            assertThat(state.cursorCol()).isEqualTo(1);
        }

        @Test
        @DisplayName("Insert multiple characters")
        void insertMultipleChars() {
            TextAreaState state = new TextAreaState();
            state.insert('H');
            state.insert('i');

            assertThat(state.text()).isEqualTo("Hi");
            assertThat(state.cursorCol()).isEqualTo(2);
        }

        @Test
        @DisplayName("Insert string")
        void insertString() {
            TextAreaState state = new TextAreaState();
            state.insert("Hello");

            assertThat(state.text()).isEqualTo("Hello");
            assertThat(state.cursorCol()).isEqualTo(5);
        }

        @Test
        @DisplayName("Insert newline creates new line")
        void insertNewline() {
            TextAreaState state = new TextAreaState("Hello");
            state.insert('\n');

            assertThat(state.lineCount()).isEqualTo(2);
            assertThat(state.getLine(0)).isEqualTo("Hello");
            assertThat(state.getLine(1)).isEmpty();
            assertThat(state.cursorRow()).isEqualTo(1);
            assertThat(state.cursorCol()).isEqualTo(0);
        }

        @Test
        @DisplayName("Insert newline in middle of line splits it")
        void insertNewlineInMiddle() {
            TextAreaState state = new TextAreaState("HelloWorld");
            // Move cursor to middle
            state.moveCursorToStart();
            for (int i = 0; i < 5; i++) {
                state.moveCursorRight();
            }
            state.insert('\n');

            assertThat(state.lineCount()).isEqualTo(2);
            assertThat(state.getLine(0)).isEqualTo("Hello");
            assertThat(state.getLine(1)).isEqualTo("World");
            assertThat(state.cursorRow()).isEqualTo(1);
            assertThat(state.cursorCol()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Text Deletion")
    class TextDeletion {

        @Test
        @DisplayName("Delete backward removes character before cursor")
        void deleteBackward() {
            TextAreaState state = new TextAreaState("Hello");
            state.deleteBackward();

            assertThat(state.text()).isEqualTo("Hell");
            assertThat(state.cursorCol()).isEqualTo(4);
        }

        @Test
        @DisplayName("Delete backward at start of line merges with previous")
        void deleteBackwardMergesLines() {
            TextAreaState state = new TextAreaState("Hello\nWorld");
            // Cursor is at end of "World", move to start of second line
            state.moveCursorToStart();
            state.moveCursorDown();
            state.moveCursorToLineStart();
            state.deleteBackward();

            assertThat(state.text()).isEqualTo("HelloWorld");
            assertThat(state.lineCount()).isEqualTo(1);
            assertThat(state.cursorRow()).isEqualTo(0);
            assertThat(state.cursorCol()).isEqualTo(5);
        }

        @Test
        @DisplayName("Delete backward at start of first line does nothing")
        void deleteBackwardAtStart() {
            TextAreaState state = new TextAreaState("Hello");
            state.moveCursorToStart();
            state.deleteBackward();

            assertThat(state.text()).isEqualTo("Hello");
        }

        @Test
        @DisplayName("Delete forward removes character at cursor")
        void deleteForward() {
            TextAreaState state = new TextAreaState("Hello");
            state.moveCursorToStart();
            state.deleteForward();

            assertThat(state.text()).isEqualTo("ello");
        }

        @Test
        @DisplayName("Delete forward at end of line merges with next")
        void deleteForwardMergesLines() {
            TextAreaState state = new TextAreaState("Hello\nWorld");
            state.moveCursorToStart();
            state.moveCursorToLineEnd();
            state.deleteForward();

            assertThat(state.text()).isEqualTo("HelloWorld");
            assertThat(state.lineCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Delete forward at end of last line does nothing")
        void deleteForwardAtEnd() {
            TextAreaState state = new TextAreaState("Hello");
            state.deleteForward();

            assertThat(state.text()).isEqualTo("Hello");
        }
    }

    @Nested
    @DisplayName("Cursor Movement")
    class CursorMovement {

        @Test
        @DisplayName("Move cursor left")
        void moveCursorLeft() {
            TextAreaState state = new TextAreaState("Hello");
            state.moveCursorLeft();

            assertThat(state.cursorCol()).isEqualTo(4);
        }

        @Test
        @DisplayName("Move cursor left at start of line goes to previous line")
        void moveCursorLeftWrapsToPrevLine() {
            TextAreaState state = new TextAreaState("Hello\nWorld");
            state.moveCursorToStart();
            state.moveCursorDown();
            state.moveCursorToLineStart();
            state.moveCursorLeft();

            assertThat(state.cursorRow()).isEqualTo(0);
            assertThat(state.cursorCol()).isEqualTo(5); // End of "Hello"
        }

        @Test
        @DisplayName("Move cursor left at start of first line does nothing")
        void moveCursorLeftAtStart() {
            TextAreaState state = new TextAreaState("Hello");
            state.moveCursorToStart();
            state.moveCursorLeft();

            assertThat(state.cursorRow()).isEqualTo(0);
            assertThat(state.cursorCol()).isEqualTo(0);
        }

        @Test
        @DisplayName("Move cursor right")
        void moveCursorRight() {
            TextAreaState state = new TextAreaState("Hello");
            state.moveCursorToStart();
            state.moveCursorRight();

            assertThat(state.cursorCol()).isEqualTo(1);
        }

        @Test
        @DisplayName("Move cursor right at end of line goes to next line")
        void moveCursorRightWrapsToNextLine() {
            TextAreaState state = new TextAreaState("Hello\nWorld");
            state.moveCursorToStart();
            state.moveCursorToLineEnd();
            state.moveCursorRight();

            assertThat(state.cursorRow()).isEqualTo(1);
            assertThat(state.cursorCol()).isEqualTo(0);
        }

        @Test
        @DisplayName("Move cursor right at end of last line does nothing")
        void moveCursorRightAtEnd() {
            TextAreaState state = new TextAreaState("Hello");
            state.moveCursorRight();

            assertThat(state.cursorRow()).isEqualTo(0);
            assertThat(state.cursorCol()).isEqualTo(5);
        }

        @Test
        @DisplayName("Move cursor up")
        void moveCursorUp() {
            TextAreaState state = new TextAreaState("Hello\nWorld");
            state.moveCursorUp();

            assertThat(state.cursorRow()).isEqualTo(0);
        }

        @Test
        @DisplayName("Move cursor up clamps column to line length")
        void moveCursorUpClampsColumn() {
            TextAreaState state = new TextAreaState("Hi\nWorld");
            // Cursor at end of "World" (col 5)
            state.moveCursorUp();

            assertThat(state.cursorRow()).isEqualTo(0);
            assertThat(state.cursorCol()).isEqualTo(2); // "Hi" length
        }

        @Test
        @DisplayName("Move cursor up at first line does nothing")
        void moveCursorUpAtFirst() {
            TextAreaState state = new TextAreaState("Hello\nWorld");
            state.moveCursorToStart();
            state.moveCursorUp();

            assertThat(state.cursorRow()).isEqualTo(0);
        }

        @Test
        @DisplayName("Move cursor down")
        void moveCursorDown() {
            TextAreaState state = new TextAreaState("Hello\nWorld");
            state.moveCursorToStart();
            state.moveCursorDown();

            assertThat(state.cursorRow()).isEqualTo(1);
        }

        @Test
        @DisplayName("Move cursor down clamps column to line length")
        void moveCursorDownClampsColumn() {
            TextAreaState state = new TextAreaState("Hello\nHi");
            state.moveCursorToStart();
            state.moveCursorToLineEnd(); // col 5
            state.moveCursorDown();

            assertThat(state.cursorRow()).isEqualTo(1);
            assertThat(state.cursorCol()).isEqualTo(2); // "Hi" length
        }

        @Test
        @DisplayName("Move cursor down at last line does nothing")
        void moveCursorDownAtLast() {
            TextAreaState state = new TextAreaState("Hello\nWorld");
            state.moveCursorDown();

            assertThat(state.cursorRow()).isEqualTo(1);
        }

        @Test
        @DisplayName("Move cursor up onto an emoji line lands between two emoji, not inside one")
        void moveCursorUpOntoEmojiLine() {
            // Cursor at the end of "abc", screen column 3. Each "🔥" is 2 columns and 2 chars.
            TextAreaState state = new TextAreaState("🔥🔥\nabc");
            state.moveCursorUp();

            // Column 3 falls inside the second "🔥" (columns 2-3), so the caret stops before it.
            assertThat(state.cursorRow()).isEqualTo(0);
            assertThat(state.cursorCol()).isEqualTo(2);

            state.insert('x');
            assertThat(state.getLine(0)).isEqualTo("🔥x🔥");
        }

        @Test
        @DisplayName("Move cursor down onto an emoji line lands between two emoji, not inside one")
        void moveCursorDownOntoEmojiLine() {
            TextAreaState state = new TextAreaState("abc\n🔥🔥");
            state.moveCursorToStart();
            state.moveCursorToLineEnd(); // "abc|", screen column 3
            state.moveCursorDown();

            assertThat(state.cursorRow()).isEqualTo(1);
            assertThat(state.cursorCol()).isEqualTo(2);
        }

        @Test
        @DisplayName("Move cursor up and down keeps the screen column across wide (CJK) characters")
        void moveCursorUpDownKeepsScreenColumnAcrossWideChars() {
            // Cursor at the end of "中文": char offset 2, screen column 4.
            TextAreaState state = new TextAreaState("abcdef\n中文");
            state.moveCursorUp();

            assertThat(state.cursorCol()).isEqualTo(4); // "abcd|ef"

            state.moveCursorDown();

            assertThat(state.cursorCol()).isEqualTo(2); // back after "文"
        }

        @Test
        @DisplayName("Move cursor up preserves the visible column when scrolled past a wide character")
        void moveCursorUpKeepsViewportColumnWhenHorizontallyScrolled() {
            TextAreaState state = new TextAreaState("世abcdefghij\nabcdefghij");
            state.ensureCursorVisible(2, 4);
            int initialScrollCol = state.scrollCol();
            assertThat(initialScrollCol).isPositive();
            int before = CharWidth.of(state.getLine(state.cursorRow())
                .substring(initialScrollCol, state.cursorCol()));

            state.moveCursorUp();
            state.ensureCursorVisible(2, 4);

            assertThat(state.scrollCol()).isEqualTo(initialScrollCol);
            int after = CharWidth.of(state.getLine(state.cursorRow())
                .substring(state.scrollCol(), state.cursorCol()));
            assertThat(after).isEqualTo(before);

            state.moveCursorDown();
            state.ensureCursorVisible(2, 4);
            int back = CharWidth.of(state.getLine(state.cursorRow())
                .substring(state.scrollCol(), state.cursorCol()));
            assertThat(back).isEqualTo(before);
        }

        @Test
        @DisplayName("Move cursor up never lands inside a ZWJ sequence")
        void moveCursorUpOntoZwjSequence() {
            // "👨‍👩‍👧" is one 2-column glyph made of 8 chars; screen column 1 falls inside it.
            TextAreaState state = new TextAreaState("👨‍👩‍👧 family\na");
            state.moveCursorUp();

            assertThat(state.cursorCol()).isEqualTo(0);
        }

        @Test
        @DisplayName("Move to line start")
        void moveCursorToLineStart() {
            TextAreaState state = new TextAreaState("Hello");
            state.moveCursorToLineStart();

            assertThat(state.cursorCol()).isEqualTo(0);
        }

        @Test
        @DisplayName("Move to line end")
        void moveCursorToLineEnd() {
            TextAreaState state = new TextAreaState("Hello");
            state.moveCursorToStart();
            state.moveCursorToLineEnd();

            assertThat(state.cursorCol()).isEqualTo(5);
        }

        @Test
        @DisplayName("Move to document start")
        void moveCursorToStart() {
            TextAreaState state = new TextAreaState("Hello\nWorld");
            state.moveCursorToStart();

            assertThat(state.cursorRow()).isEqualTo(0);
            assertThat(state.cursorCol()).isEqualTo(0);
        }

        @Test
        @DisplayName("Move to document end")
        void moveCursorToEnd() {
            TextAreaState state = new TextAreaState("Hello\nWorld");
            state.moveCursorToStart();
            state.moveCursorToEnd();

            assertThat(state.cursorRow()).isEqualTo(1);
            assertThat(state.cursorCol()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Scrolling")
    class Scrolling {

        @Test
        @DisplayName("Ensure cursor visible adjusts scroll when cursor above viewport")
        void ensureCursorVisibleScrollsUp() {
            TextAreaState state = new TextAreaState("Line1\nLine2\nLine3\nLine4\nLine5");
            state.moveCursorToStart();
            // Manually set scroll to show lines 2-4
            state.scrollDown(2, 3);

            // Cursor is at row 0, but scroll is at 2
            state.ensureCursorVisible(3, 80);

            assertThat(state.scrollRow()).isEqualTo(0);
        }

        @Test
        @DisplayName("Ensure cursor visible adjusts scroll when cursor below viewport")
        void ensureCursorVisibleScrollsDown() {
            TextAreaState state = new TextAreaState("Line1\nLine2\nLine3\nLine4\nLine5");
            // Cursor is at end (row 4)
            state.ensureCursorVisible(3, 80);

            assertThat(state.scrollRow()).isEqualTo(2); // Shows lines 2, 3, 4
        }

        @Test
        @DisplayName("Scroll up reduces scroll row")
        void scrollUp() {
            TextAreaState state = new TextAreaState("L1\nL2\nL3\nL4\nL5\nL6\nL7");
            // 7 lines with 3 visible rows: maxScroll = 4
            state.scrollDown(3, 3); // scrollRow = min(4, 3) = 3
            state.scrollUp(2);      // scrollRow = max(0, 3-2) = 1

            assertThat(state.scrollRow()).isEqualTo(1);
        }

        @Test
        @DisplayName("Scroll up doesn't go below zero")
        void scrollUpClamped() {
            TextAreaState state = new TextAreaState("L1\nL2");
            state.scrollUp(10);

            assertThat(state.scrollRow()).isEqualTo(0);
        }

        @Test
        @DisplayName("Scroll down increases scroll row")
        void scrollDown() {
            TextAreaState state = new TextAreaState("L1\nL2\nL3\nL4\nL5");
            state.scrollDown(2, 3);

            assertThat(state.scrollRow()).isEqualTo(2);
        }

        @Test
        @DisplayName("Scroll down is clamped to max scroll")
        void scrollDownClamped() {
            TextAreaState state = new TextAreaState("L1\nL2\nL3");
            state.scrollDown(10, 2); // With 3 lines and 2 visible, max scroll is 1

            assertThat(state.scrollRow()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Wrapping")
    class Wrapping {

        @Test
        @DisplayName("computeDisplayRows with CLIP returns one row per logical line")
        void computeDisplayRowsClip() {
            TextAreaState state = new TextAreaState("Hello\nWorld");

            List<TextAreaState.DisplayRow> rows = state.computeDisplayRows(80, Overflow.CLIP);

            assertThat(rows).hasSize(2);
            assertThat(rows.get(0).logicalRow()).isEqualTo(0);
            assertThat(rows.get(0).startCol()).isEqualTo(0);
            assertThat(rows.get(0).endCol()).isEqualTo(5);
            assertThat(rows.get(1).logicalRow()).isEqualTo(1);
            assertThat(rows.get(1).startCol()).isEqualTo(0);
            assertThat(rows.get(1).endCol()).isEqualTo(5);
        }

        @Test
        @DisplayName("computeDisplayRows with WRAP_CHARACTER splits a long line by width")
        void computeDisplayRowsWrapCharacter() {
            TextAreaState state = new TextAreaState("HelloWorld");

            List<TextAreaState.DisplayRow> rows = state.computeDisplayRows(4, Overflow.WRAP_CHARACTER);

            assertThat(rows).hasSize(3);
            assertThat(rows.get(0).logicalRow()).isEqualTo(0);
            assertThat(rows.get(0).startCol()).isEqualTo(0);
            assertThat(rows.get(0).endCol()).isEqualTo(4);
            assertThat(rows.get(1).startCol()).isEqualTo(4);
            assertThat(rows.get(1).endCol()).isEqualTo(8);
            assertThat(rows.get(2).startCol()).isEqualTo(8);
            assertThat(rows.get(2).endCol()).isEqualTo(10);
        }

        @Test
        @DisplayName("computeDisplayRows with WRAP_WORD breaks at word boundaries")
        void computeDisplayRowsWrapWord() {
            TextAreaState state = new TextAreaState("one two three");

            List<TextAreaState.DisplayRow> rows = state.computeDisplayRows(7, Overflow.WRAP_WORD);

            assertThat(rows).hasSize(2);
            assertThat(state.getLine(0).substring(rows.get(0).startCol(), rows.get(0).endCol()))
                .isEqualTo("one two");
            assertThat(state.getLine(0).substring(rows.get(1).startCol(), rows.get(1).endCol()))
                .isEqualTo("three");
        }

        @Test
        @DisplayName("computeDisplayRows preserves hanging indent on continuation rows")
        void computeDisplayRowsHangingIndent() {
            TextAreaState state = new TextAreaState("  - one two three");
            TextWrapOptions options = TextWrapOptions.of(Overflow.WRAP_WORD, ContinuationIndent.HANGING);

            List<TextAreaState.DisplayRow> rows = state.computeDisplayRows(11, options);

            assertThat(rows).hasSize(2);
            assertThat(rows.get(0).indent()).isEqualTo(0);
            assertThat(state.getLine(0).substring(rows.get(0).startCol(), rows.get(0).endCol()))
                .isEqualTo("  - one two");
            assertThat(rows.get(1).indent()).isEqualTo(2);
            assertThat(state.getLine(0).substring(rows.get(1).startCol(), rows.get(1).endCol()))
                .isEqualTo("three");
        }

        @Test
        @DisplayName("computeDisplayRows with FLUSH_LEFT restarts continuation rows at 0 indent")
        void computeDisplayRowsFlushLeft() {
            TextAreaState state = new TextAreaState("  - one two three");
            TextWrapOptions options = TextWrapOptions.of(Overflow.WRAP_WORD, ContinuationIndent.FLUSH_LEFT);

            List<TextAreaState.DisplayRow> rows = state.computeDisplayRows(11, options);

            assertThat(rows).hasSize(2);
            assertThat(rows.get(0).indent()).isEqualTo(0);
            assertThat(rows.get(1).indent()).isEqualTo(0);
            assertThat(state.getLine(0).substring(rows.get(1).startCol(), rows.get(1).endCol()))
                .isEqualTo("three");
        }

        @Test
        @DisplayName("computeDisplayRows with WRAP_CHARACTER splits wide (CJK) characters by display width")
        void computeDisplayRowsWrapCharacterWideChars() {
            TextAreaState state = new TextAreaState("世界你好"); // 4 chars * 2 width = 8 cols

            // 5-wide area: "世界" (4 cols) fits, "你" (2 cols) would overflow to 6
            List<TextAreaState.DisplayRow> rows = state.computeDisplayRows(5, Overflow.WRAP_CHARACTER);

            assertThat(rows).hasSize(2);
            assertThat(state.getLine(0).substring(rows.get(0).startCol(), rows.get(0).endCol()))
                .isEqualTo("世界");
            assertThat(state.getLine(0).substring(rows.get(1).startCol(), rows.get(1).endCol()))
                .isEqualTo("你好");
        }

        @Test
        @DisplayName("computeDisplayRows keeps an empty logical line as a single empty row")
        void computeDisplayRowsEmptyLine() {
            TextAreaState state = new TextAreaState("Hello\n\nWorld");

            List<TextAreaState.DisplayRow> rows = state.computeDisplayRows(3, Overflow.WRAP_WORD);

            assertThat(rows).extracting(TextAreaState.DisplayRow::logicalRow)
                .containsExactly(0, 0, 1, 2, 2);
            TextAreaState.DisplayRow emptyLineRow = rows.stream()
                .filter(r -> r.logicalRow() == 1)
                .findFirst()
                .get();
            assertThat(emptyLineRow.startCol()).isEqualTo(0);
            assertThat(emptyLineRow.endCol()).isEqualTo(0);
        }

        @Test
        @DisplayName("ensureCursorVisible with CLIP behaves exactly as the 2-arg overload")
        void ensureCursorVisibleClipDelegates() {
            TextAreaState state = new TextAreaState("Line1\nLine2\nLine3\nLine4\nLine5");

            state.ensureCursorVisible(3, 80, Overflow.CLIP);

            assertThat(state.scrollRow()).isEqualTo(2); // Shows lines 2, 3, 4 - same as existing test
        }

        @Test
        @DisplayName("ensureCursorVisible with WRAP_WORD scrolls by display row, not logical row")
        void ensureCursorVisibleWrapScrollsByDisplayRow() {
            // "one two three four five" wraps to 5 rows at width 7:
            // "one two", "three", "four", "five" -- wait, compute precisely in-test via wrap.
            TextAreaState state = new TextAreaState("one two three four five six seven");
            state.moveCursorToEnd(); // cursor at the very end (last display row)

            state.ensureCursorVisible(2, 7, Overflow.WRAP_WORD);

            List<TextAreaState.DisplayRow> rows = state.computeDisplayRows(7, Overflow.WRAP_WORD);
            int cursorDisplayIndex = rows.size() - 1; // cursor is on the last display row
            assertThat(state.scrollRow()).isEqualTo(cursorDisplayIndex - 2 + 1);
            assertThat(state.scrollCol()).isEqualTo(0);
        }

        @Test
        @DisplayName("moveCursorDown with WRAP_WORD moves to the next visual row within the same logical line")
        void moveCursorDownWrapMovesByVisualRow() {
            // "one two three" wraps at width 7 to "one two" (row0) / "three" (row0, 2nd visual row)
            TextAreaState state = new TextAreaState("one two three");
            state.moveCursorToStart(); // row 0, col 0 -> on the first visual row ("one two")

            state.moveCursorDown(7, Overflow.WRAP_WORD);

            assertThat(state.cursorRow()).isEqualTo(0); // still the same logical line
            assertThat(state.cursorCol()).isEqualTo(8); // start of "three" segment
        }

        @Test
        @DisplayName("moveCursorDown with WRAP_WORD at the last visual row does nothing")
        void moveCursorDownWrapAtLastRowDoesNothing() {
            TextAreaState state = new TextAreaState("one two three");
            state.moveCursorToEnd(); // last visual row

            state.moveCursorDown(7, Overflow.WRAP_WORD);

            assertThat(state.cursorRow()).isEqualTo(0);
            assertThat(state.cursorCol()).isEqualTo(13); // unchanged, end of text
        }

        @Test
        @DisplayName("moveCursorUp with WRAP_WORD moves to the previous visual row within the same logical line")
        void moveCursorUpWrapMovesByVisualRow() {
            TextAreaState state = new TextAreaState("one two three");
            state.moveCursorToEnd(); // on "three" segment (second visual row)

            state.moveCursorUp(7, Overflow.WRAP_WORD);

            assertThat(state.cursorRow()).isEqualTo(0);
            // Same screen column (5, just past "three") in "one two": "one t|wo", not the row end.
            assertThat(state.cursorCol()).isEqualTo(5);
        }

        @Test
        @DisplayName("moveCursorDown with WRAP_WORD keeps the screen column, not the offset in the line")
        void moveCursorDownWrapKeepsScreenColumn() {
            TextAreaState state = new TextAreaState("one two three");
            state.moveCursorToStart();
            state.moveCursorRight(); // "o|ne two", screen column 1

            state.moveCursorDown(7, Overflow.WRAP_WORD);

            // Column 1 of "three" is offset 9 ("t|hree"). Clamping the old offset 1 into the
            // target row [8,13) would instead have reset the caret to column 0.
            assertThat(state.cursorCol()).isEqualTo(9);
        }

        @Test
        @DisplayName("moveCursorDown with wrapping keeps the screen column across wide (CJK) characters")
        void moveCursorDownWrapKeepsScreenColumnAcrossWideChars() {
            // Width 4: "abcd" / "世界" (each CJK char is 2 columns).
            TextAreaState state = new TextAreaState("abcd世界");
            state.moveCursorToStart();
            state.moveCursorRight();
            state.moveCursorRight();
            state.moveCursorRight(); // "abc|d", screen column 3

            state.moveCursorDown(4, Overflow.WRAP_CHARACTER);

            // Column 3 falls inside "界" (columns 2-3), so the caret stops before it, after "世".
            assertThat(state.cursorCol()).isEqualTo(5);
        }

        @Test
        @DisplayName("moveCursorUp onto a short word-wrapped row lands after its text, on that row")
        void moveCursorUpOntoShortWordWrappedRow() {
            // Width 6: "ab" [0,2) / "cdefgh" [3,9); the space at offset 2 is consumed by the break.
            TextAreaState state = new TextAreaState("ab cdefgh");
            state.moveCursorLeft(); // "cdefg|h", screen column 5 of the second row

            state.moveCursorUp(6, Overflow.WRAP_WORD);

            // "ab|": the end of "ab" leaves room on its row, so the caret is drawn right there.
            assertThat(state.cursorCol()).isEqualTo(2);
            List<TextAreaState.DisplayRow> rows = state.computeDisplayRows(6, Overflow.WRAP_WORD);
            assertThat(state.findCursorDisplayRowIndex(rows, 6)).isEqualTo(0);
        }

        @Test
        @DisplayName("moveCursorUp with WRAP_WORD at the first visual row does nothing")
        void moveCursorUpWrapAtFirstRowDoesNothing() {
            TextAreaState state = new TextAreaState("one two three");
            state.moveCursorToStart();

            state.moveCursorUp(7, Overflow.WRAP_WORD);

            assertThat(state.cursorRow()).isEqualTo(0);
            assertThat(state.cursorCol()).isEqualTo(0);
        }

        @Test
        @DisplayName("moveCursorDown with WRAP_WORD crosses into the next logical line's first visual row")
        void moveCursorDownWrapCrossesLogicalLine() {
            TextAreaState state = new TextAreaState("one two three\nfour");
            state.moveCursorToStart();
            state.moveCursorDown(7, Overflow.WRAP_WORD); // -> "three" segment (row 0)

            state.moveCursorDown(7, Overflow.WRAP_WORD); // -> "four" (row 1, only segment)

            assertThat(state.cursorRow()).isEqualTo(1);
            assertThat(state.cursorCol()).isEqualTo(0); // same screen column 0 as on "three"
        }

        @Test
        @DisplayName("computeDisplayRows treats the truncating overflow modes as CLIP, not as character wrap")
        void computeDisplayRowsTruncatingModesDoNotWrap() {
            TextAreaState state = new TextAreaState("one two three");

            for (Overflow truncating : new Overflow[] {
                Overflow.ELLIPSIS, Overflow.ELLIPSIS_START, Overflow.ELLIPSIS_MIDDLE}) {
                // A text area can move the caret into hidden text, so truncation makes no sense;
                // these must fall back to CLIP rather than silently wrapping by character.
                assertThat(state.computeDisplayRows(7, truncating))
                    .as("%s", truncating)
                    .hasSize(1);
            }
        }

        @Test
        @DisplayName("moveCursorDown with a truncating overflow mode moves by logical line, as with CLIP")
        void moveCursorDownTruncatingModeMovesByLogicalLine() {
            TextAreaState state = new TextAreaState("one two three\nfour");
            state.moveCursorToStart();

            state.moveCursorDown(7, Overflow.ELLIPSIS);

            assertThat(state.cursorRow()).isEqualTo(1); // next logical line, not the "three" segment
            assertThat(state.cursorCol()).isEqualTo(0);
        }

        @Test
        @DisplayName("computeDisplayRows re-wraps after the text changes")
        void computeDisplayRowsInvalidatedByTextChange() {
            TextAreaState state = new TextAreaState("one two");

            assertThat(state.computeDisplayRows(8, Overflow.WRAP_WORD)).hasSize(1);

            state.insert(" three"); // cursor is at the end after construction

            assertThat(state.computeDisplayRows(8, Overflow.WRAP_WORD)).hasSize(2);
        }

        @Test
        @DisplayName("computeDisplayRows re-wraps when the width or the mode changes")
        void computeDisplayRowsInvalidatedByWidthOrMode() {
            TextAreaState state = new TextAreaState("one two three");

            assertThat(state.computeDisplayRows(7, Overflow.WRAP_WORD)).hasSize(2);
            assertThat(state.computeDisplayRows(20, Overflow.WRAP_WORD)).hasSize(1);
            assertThat(state.computeDisplayRows(7, Overflow.WRAP_CHARACTER)).hasSize(2);
            assertThat(state.computeDisplayRows(7, Overflow.CLIP)).hasSize(1);
        }

        @Test
        @DisplayName("ensureCursorVisible scrolls to the row where a cursor on consumed wrap whitespace is drawn")
        void ensureCursorVisibleSnapsCursorOnConsumedWrapBreak() {
            TextAreaState state = new TextAreaState("one two three");
            state.moveCursorToStart();
            for (int i = 0; i < 7; i++) {
                state.moveCursorRight(); // up to offset 7, the space the wrap consumed
            }

            // Only one row visible: "one two" fills the width, so the caret is drawn at the start
            // of "three" (display row 1), and that row -- not "one two" -- is scrolled into view.
            state.ensureCursorVisible(1, 7, Overflow.WRAP_WORD);

            assertThat(state.scrollRow()).isEqualTo(1);
        }

        @Test
        @DisplayName("computeDisplayRows adds an empty caret row after a line that fills the width")
        void computeDisplayRowsAddsCaretRowAfterFullLine() {
            TextAreaState full = new TextAreaState("abcdefg");
            TextAreaState roomy = new TextAreaState("abc");

            // The caret after "abcdefg" would sit in column 7 of a 7-wide area, outside it, so
            // the end of the line gets a row of its own. "abc" leaves room and needs none.
            assertThat(segments(full, 7, Overflow.WRAP_CHARACTER)).containsExactly("abcdefg", "");
            assertThat(segments(roomy, 7, Overflow.WRAP_CHARACTER)).containsExactly("abc");
        }

        @Test
        @DisplayName("computeDisplayRows gives the end of a line a row when the wrap consumed trailing whitespace")
        void computeDisplayRowsAddsCaretRowAfterConsumedTrailingWhitespace() {
            TextAreaState state = new TextAreaState("one two "); // cursor at the end, offset 8

            List<TextAreaState.DisplayRow> rows = state.computeDisplayRows(7, Overflow.WRAP_WORD);

            // Without the trailing row, offset 8 would be drawn in column 8 and vanish.
            assertThat(rows).hasSize(2);
            assertThat(rows.get(1).startCol()).isEqualTo(8);
            assertThat(rows.get(1).endCol()).isEqualTo(8);
            assertThat(state.findCursorDisplayRowIndex(rows, 7)).isEqualTo(1);
        }

        @Test
        @DisplayName("computeDisplayRows gives a glyph wider than the area one row, plus only the caret row")
        void computeDisplayRowsOverWideGlyph() {
            TextAreaState state = new TextAreaState("中"); // 2 columns in a 1-column area

            List<TextAreaState.DisplayRow> rows = state.computeDisplayRows(1, Overflow.WRAP_CHARACTER);

            // The glyph gets a row of its own; the second, empty row exists only so the caret
            // after the glyph has a cell to be drawn in. There is no third, stray row.
            assertThat(segments(state, 1, Overflow.WRAP_CHARACTER)).containsExactly("中", "");
            assertThat(rows.get(1).startCol()).isEqualTo(1);
        }

        @Test
        @DisplayName("WRAP_CHARACTER never splits a ZWJ emoji sequence across rows")
        void computeDisplayRowsWrapCharacterKeepsZwjSequenceWhole() {
            // "👨‍🦲" is man + ZWJ + bald: one 2-column glyph. Counted per code point it is 4
            // columns wide, and the bald component would be pushed onto the next row.
            TextAreaState state = new TextAreaState("a👨‍🦲b");

            assertThat(segments(state, 3, Overflow.WRAP_CHARACTER))
                .containsExactly("a👨‍🦲", "b");
        }

        @Test
        @DisplayName("WRAP_WORD measures a ZWJ emoji sequence inside a word as one glyph")
        void computeDisplayRowsWrapWordKeepsZwjSequenceWhole() {
            TextAreaState state = new TextAreaState("x👨‍🦲 y");

            // "x👨‍🦲" is 3 columns and fits in 4; over-counting it as 5 would force a break
            // inside the emoji.
            assertThat(segments(state, 4, Overflow.WRAP_WORD))
                .containsExactly("x👨‍🦲", "y");
        }

        @Test
        @DisplayName("scrollDown with wrapping can scroll through the wrapped rows of a single line")
        void scrollDownWrapClampsToDisplayRows() {
            // One logical line, six display rows at width 7:
            // "one two" / "three" / "four" / "five" / "six" / "seven"
            TextAreaState state = new TextAreaState("one two three four five six seven");
            state.scrollDown(10, 2); // the logical-line overload cannot move past row 0
            assertThat(state.scrollRow()).isEqualTo(0);

            state.scrollDown(10, 2, 7, Overflow.WRAP_WORD);

            assertThat(state.scrollRow()).isEqualTo(4); // 6 display rows - 2 visible
        }

        @Test
        @DisplayName("scrollDown with a non-wrapping overflow clamps to logical lines, like the 2-arg overload")
        void scrollDownClipDelegates() {
            TextAreaState state = new TextAreaState("one two three four five six seven\nL2\nL3");

            state.scrollDown(10, 2, 7, Overflow.CLIP);

            assertThat(state.scrollRow()).isEqualTo(1); // 3 logical lines - 2 visible
        }

        private List<String> segments(TextAreaState state, int width, Overflow overflow) {
            List<String> result = new ArrayList<>();
            for (TextAreaState.DisplayRow row : state.computeDisplayRows(width, overflow)) {
                result.add(state.getLine(row.logicalRow()).substring(row.startCol(), row.endCol()));
            }
            return result;
        }
    }

    @Nested
    @DisplayName("Bulk Operations")
    class BulkOperations {

        @Test
        @DisplayName("Clear resets state")
        void clear() {
            TextAreaState state = new TextAreaState("Hello\nWorld");
            state.clear();

            assertThat(state.text()).isEmpty();
            assertThat(state.lineCount()).isEqualTo(1);
            assertThat(state.cursorRow()).isEqualTo(0);
            assertThat(state.cursorCol()).isEqualTo(0);
            assertThat(state.scrollRow()).isEqualTo(0);
        }

        @Test
        @DisplayName("setText replaces content")
        void setText() {
            TextAreaState state = new TextAreaState("Old");
            state.setText("New\nText");

            assertThat(state.text()).isEqualTo("New\nText");
            assertThat(state.lineCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("setText with null clears content")
        void setTextNull() {
            TextAreaState state = new TextAreaState("Hello");
            state.setText(null);

            assertThat(state.text()).isEmpty();
            assertThat(state.lineCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("setText positions cursor at end")
        void setTextCursorAtEnd() {
            TextAreaState state = new TextAreaState();
            state.setText("Hello\nWorld");

            assertThat(state.cursorRow()).isEqualTo(1);
            assertThat(state.cursorCol()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Line Access")
    class LineAccess {

        @Test
        @DisplayName("getLine returns line content")
        void getLine() {
            TextAreaState state = new TextAreaState("Line1\nLine2\nLine3");

            assertThat(state.getLine(0)).isEqualTo("Line1");
            assertThat(state.getLine(1)).isEqualTo("Line2");
            assertThat(state.getLine(2)).isEqualTo("Line3");
        }

        @Test
        @DisplayName("getLine with invalid index returns empty string")
        void getLineInvalidIndex() {
            TextAreaState state = new TextAreaState("Hello");

            assertThat(state.getLine(-1)).isEmpty();
            assertThat(state.getLine(10)).isEmpty();
        }

        @Test
        @DisplayName("lineCount returns number of lines")
        void lineCount() {
            TextAreaState state = new TextAreaState("A\nB\nC\nD");

            assertThat(state.lineCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("text preserves trailing empty lines")
        void textPreservesTrailingEmptyLines() {
            TextAreaState state = new TextAreaState("Hello\n\n");

            assertThat(state.lineCount()).isEqualTo(3);
            assertThat(state.text()).isEqualTo("Hello\n\n");
        }
    }
}

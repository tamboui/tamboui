/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.input;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
            state.scrollUp(2); // scrollRow = max(0, 3-2) = 1

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

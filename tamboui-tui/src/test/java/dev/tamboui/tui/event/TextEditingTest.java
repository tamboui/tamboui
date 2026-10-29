/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Overflow;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.bindings.Bindings;
import dev.tamboui.widgets.input.TextArea;
import dev.tamboui.widgets.input.TextAreaState;
import dev.tamboui.widgets.input.TextInputState;

import static org.assertj.core.api.Assertions.assertThat;

class TextEditingTest {

    private static final String WOMAN_TECHNOLOGIST = "👩‍💻";
    private static final String FAMILY = "👨‍👩‍👧";

    @Nested
    @DisplayName("TextInputState")
    class Input {

        @Test
        @DisplayName("an emoji outside the Basic Multilingual Plane and CJK text are inserted whole")
        void emojiAndCjkInsertedWhole() {
            TextInputState state = new TextInputState();

            type(state, "Hi 😀 日本", BindingSets.standard());

            assertThat(state.text()).isEqualTo("Hi 😀 日本");
            assertThat(state.cursorPosition()).isEqualTo(state.length());
            press(state, KeyCode.LEFT);
            press(state, KeyCode.LEFT);
            press(state, KeyCode.LEFT);
            press(state, KeyCode.BACKSPACE);
            assertThat(state.text()).isEqualTo("Hi  日本");
        }

        @Test
        @DisplayName("a ZWJ sequence typed one code point at a time ends up as one grapheme cluster")
        void zwjSequenceTyped() {
            TextInputState state = new TextInputState();

            type(state, "a" + WOMAN_TECHNOLOGIST + FAMILY, BindingSets.standard());

            assertThat(state.text()).isEqualTo("a" + WOMAN_TECHNOLOGIST + FAMILY);
            press(state, KeyCode.BACKSPACE);
            assertThat(state.text()).isEqualTo("a" + WOMAN_TECHNOLOGIST);
            press(state, KeyCode.LEFT);
            assertThat(state.cursorPosition()).isEqualTo(1);
            press(state, KeyCode.DELETE);
            assertThat(state.text()).isEqualTo("a");
        }

        @Test
        @DisplayName("Ctrl and Alt characters are not inserted, and unbound ones are left to the app")
        void ctrlAndAltCharactersIgnored() {
            TextInputState state = new TextInputState("abc");

            boolean ctrlK = TextEditing.handle(state, KeyEvent.ofChar('k', KeyModifiers.CTRL));
            boolean altX = TextEditing.handle(state, KeyEvent.ofChar('x', KeyModifiers.ALT));
            boolean ctrlC = TextEditing.handle(state, KeyEvent.ofChar('c', KeyModifiers.CTRL));

            assertThat(ctrlK).isFalse();
            assertThat(altX).isFalse();
            assertThat(ctrlC).isFalse();
            assertThat(state.text()).isEqualTo("abc");
            assertThat(state.cursorPosition()).isEqualTo(3);
        }

        @Test
        @DisplayName("the emacs bindings move the cursor and delete with Ctrl+A, Ctrl+E, Ctrl+B, Ctrl+F, Ctrl+H and Ctrl+D")
        void emacsBindings() {
            TextInputState state = new TextInputState("hello");
            Bindings emacs = BindingSets.emacs();

            assertThat(TextEditing.handle(state, KeyEvent.ofChar('a', KeyModifiers.CTRL, emacs))).isTrue();
            assertThat(state.cursorPosition()).isEqualTo(0);
            ctrl(state, 'f', emacs);
            ctrl(state, 'f', emacs);
            assertThat(state.cursorPosition()).isEqualTo(2);
            ctrl(state, 'd', emacs);
            assertThat(state.text()).isEqualTo("helo");
            ctrl(state, 'e', emacs);
            assertThat(state.cursorPosition()).isEqualTo(4);
            ctrl(state, 'b', emacs);
            ctrl(state, 'h', emacs);
            assertThat(state.text()).isEqualTo("heo");
            assertThat(state.cursorPosition()).isEqualTo(2);
        }

        @Test
        @DisplayName("letters stay text when the bindings bind them, as vim binds h, j, k, l, g, G and x")
        void vimLettersAreTyped() {
            TextInputState state = new TextInputState();

            type(state, "hjklgGx", BindingSets.vim());

            assertThat(state.text()).isEqualTo("hjklgGx");
            assertThat(state.cursorPosition()).isEqualTo(7);
        }

        @Test
        @DisplayName("the editing keys work with any bindings and modifiers, other keys are not handled")
        void editingKeys() {
            TextInputState state = new TextInputState("abc");

            assertThat(TextEditing.handle(state, KeyEvent.ofKey(KeyCode.HOME))).isTrue();
            assertThat(TextEditing.handle(state, KeyEvent.ofKey(KeyCode.RIGHT, KeyModifiers.CTRL))).isTrue();
            assertThat(state.cursorPosition()).isEqualTo(1);
            assertThat(TextEditing.handle(state, KeyEvent.ofKey(KeyCode.BACKSPACE, KeyModifiers.ALT))).isTrue();
            assertThat(state.text()).isEqualTo("bc");
            assertThat(TextEditing.handle(state, KeyEvent.ofKey(KeyCode.END))).isTrue();
            assertThat(state.cursorPosition()).isEqualTo(2);

            assertThat(TextEditing.handle(state, KeyEvent.ofKey(KeyCode.ENTER))).isFalse();
            assertThat(TextEditing.handle(state, KeyEvent.ofKey(KeyCode.TAB))).isFalse();
            assertThat(TextEditing.handle(state, KeyEvent.ofKey(KeyCode.ESCAPE))).isFalse();
            assertThat(TextEditing.handle(state, KeyEvent.ofKey(KeyCode.UP))).isFalse();
            assertThat(TextEditing.handle(state, ResizeEvent.of(80, 24))).isFalse();
            assertThat(state.text()).isEqualTo("bc");
        }

        @Test
        @DisplayName("a filter keeps out the characters it rejects, which are still handled")
        void filter() {
            TextInputState state = new TextInputState();

            for (int codePoint : "80a8😀0".codePoints().toArray()) {
                assertThat(TextEditing.handle(state, KeyEvent.ofChar(codePoint), Character::isDigit)).isTrue();
            }

            assertThat(state.text()).isEqualTo("8080");
        }

        @Test
        @DisplayName("a paste goes on one line: line breaks become spaces, and those at its ends are dropped")
        void pasteOnOneLine() {
            TextInputState state = new TextInputState("> ");

            boolean handled = TextEditing.handle(state, new PasteEvent("\r\none\r\ntwo\rthree\nfour " + FAMILY + "\n"));

            assertThat(handled).isTrue();
            assertThat(state.text()).isEqualTo("> one two three four " + FAMILY);
            assertThat(state.cursorPosition()).isEqualTo(state.length());
        }

        @Test
        @DisplayName("a filtered paste keeps or drops each grapheme cluster whole")
        void filteredPasteByCluster() {
            TextInputState port = new TextInputState();
            TextInputState name = new TextInputState();

            TextEditing.handle(port, new PasteEvent("Port: 8080\n"), Character::isDigit);
            TextEditing.handle(name, new PasteEvent("a" + WOMAN_TECHNOLOGIST + "b​c"),
                    codePoint -> Character.getType(codePoint) != Character.FORMAT);

            assertThat(port.text()).isEqualTo("8080");
            assertThat(name.text()).isEqualTo("abc");
        }
    }

    @Nested
    @DisplayName("TextAreaState")
    class Area {

        @Test
        @DisplayName("typed emoji, ZWJ sequences and line breaks from Enter are inserted whole")
        void typing() {
            TextAreaState state = new TextAreaState();

            type(state, "日本 😀", BindingSets.standard());
            TextEditing.handle(state, KeyEvent.ofKey(KeyCode.ENTER));
            type(state, WOMAN_TECHNOLOGIST, BindingSets.standard());

            assertThat(state.text()).isEqualTo("日本 😀\n" + WOMAN_TECHNOLOGIST);
            TextEditing.handle(state, KeyEvent.ofKey(KeyCode.BACKSPACE));
            assertThat(state.text()).isEqualTo("日本 😀\n");
        }

        @Test
        @DisplayName("Ctrl characters are not inserted, Tab is not handled, and vim letters are typed")
        void ignoredKeys() {
            TextAreaState state = new TextAreaState("ab");

            assertThat(TextEditing.handle(state, KeyEvent.ofChar('k', KeyModifiers.CTRL))).isFalse();
            assertThat(TextEditing.handle(state, KeyEvent.ofKey(KeyCode.TAB))).isFalse();
            type(state, "jk", BindingSets.vim());

            assertThat(state.text()).isEqualTo("abjk");
        }

        @Test
        @DisplayName("the emacs bindings move between lines and to the line's start and end")
        void emacsBindings() {
            TextAreaState state = new TextAreaState("first\nsecond");
            Bindings emacs = BindingSets.emacs();

            ctrl(state, 'p', emacs);
            assertThat(state.cursorRow()).isEqualTo(0);
            ctrl(state, 'a', emacs);
            assertThat(state.cursorCol()).isEqualTo(0);
            ctrl(state, 'n', emacs);
            ctrl(state, 'e', emacs);
            assertThat(state.cursorRow()).isEqualTo(1);
            assertThat(state.cursorCol()).isEqualTo(6);
        }

        @Test
        @DisplayName("Up and Down move by the rows on screen when the overflow wraps")
        void wrappedRows() {
            TextAreaState wrapped = new TextAreaState("abcdefgh");
            TextAreaState clipped = new TextAreaState("abcdefgh");
            render(wrapped, Overflow.WRAP_CHARACTER);
            render(clipped, Overflow.CLIP);

            TextEditing.handle(wrapped, KeyEvent.ofKey(KeyCode.UP), Overflow.WRAP_CHARACTER);
            TextEditing.handle(clipped, KeyEvent.ofKey(KeyCode.UP));

            assertThat(wrapped.cursorCol()).isEqualTo(4);
            assertThat(clipped.cursorCol()).isEqualTo(8);
        }

        @Test
        @DisplayName("a paste keeps its line breaks, also when the terminal sends them as CR or CRLF")
        void pasteLineBreaks() {
            TextAreaState state = new TextAreaState();

            boolean handled = TextEditing.handle(state, new PasteEvent("one\rtwo\r\n" + FAMILY + "\nfour"));

            assertThat(handled).isTrue();
            assertThat(state.lineCount()).isEqualTo(4);
            assertThat(state.text()).isEqualTo("one\ntwo\n" + FAMILY + "\nfour");
        }

        private void render(TextAreaState state, Overflow overflow) {
            Rect area = new Rect(0, 0, 4, 3);
            TextArea.builder().overflow(overflow).build().render(area, Buffer.empty(area), state);
        }
    }

    private static void type(TextInputState state, String text, Bindings bindings) {
        for (int codePoint : text.codePoints().toArray()) {
            assertThat(TextEditing.handle(state, KeyEvent.ofChar(codePoint, bindings))).isTrue();
        }
    }

    private static void type(TextAreaState state, String text, Bindings bindings) {
        for (int codePoint : text.codePoints().toArray()) {
            assertThat(TextEditing.handle(state, KeyEvent.ofChar(codePoint, bindings))).isTrue();
        }
    }

    private static void press(TextInputState state, KeyCode code) {
        assertThat(TextEditing.handle(state, KeyEvent.ofKey(code))).isTrue();
    }

    private static void ctrl(TextInputState state, char c, Bindings bindings) {
        assertThat(TextEditing.handle(state, KeyEvent.ofChar(c, KeyModifiers.CTRL, bindings))).isTrue();
    }

    private static void ctrl(TextAreaState state, char c, Bindings bindings) {
        assertThat(TextEditing.handle(state, KeyEvent.ofChar(c, KeyModifiers.CTRL, bindings))).isTrue();
    }
}

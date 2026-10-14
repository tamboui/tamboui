/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.PasteEvent;
import dev.tamboui.widgets.input.TextInputState;

import static dev.tamboui.toolkit.Toolkit.formField;
import static dev.tamboui.toolkit.Toolkit.handleTextInputKey;
import static dev.tamboui.toolkit.Toolkit.textArea;
import static dev.tamboui.toolkit.Toolkit.textInput;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Key and paste editing in the toolkit's text inputs, text areas and form fields.
 */
class TextFieldEditingTest extends AbstractElementTest {

    @Test
    @DisplayName("a text input follows the emacs bindings: Ctrl+A goes to the start, Ctrl+F moves right")
    void textInputFollowsEmacsBindings() {
        TextInputState state = new TextInputState("hello");
        TextInputElement element = textInput(state);

        EventResult home = element.handleKeyEvent(emacsCtrl('a'), true);
        EventResult right = element.handleKeyEvent(emacsCtrl('f'), true);

        assertThat(home).isEqualTo(EventResult.HANDLED);
        assertThat(right).isEqualTo(EventResult.HANDLED);
        assertThat(state.cursorPosition()).isEqualTo(1);
    }

    @Test
    @DisplayName("handleTextInputKey deletes with the emacs Ctrl+H and Ctrl+D")
    void handleTextInputKeyFollowsEmacsBindings() {
        TextInputState state = new TextInputState("hello");
        state.moveCursorLeft();

        assertThat(handleTextInputKey(state, emacsCtrl('h'))).isTrue();
        assertThat(handleTextInputKey(state, emacsCtrl('d'))).isTrue();

        assertThat(state.text()).isEqualTo("hel");
    }

    @Test
    @DisplayName("a multi-line paste goes into a text input on one line")
    void textInputPasteOnOneLine() {
        TextInputState state = new TextInputState();

        textInput(state).handlePasteEvent(new PasteEvent("SELECT *\r\nFROM routes\n"));

        assertThat(state.text()).isEqualTo("SELECT * FROM routes");
    }

    @Test
    @DisplayName("a multi-line paste goes into a form text field on one line")
    void formFieldPasteOnOneLine() {
        TextInputState state = new TextInputState();

        formField("Query", state).handlePasteEvent(new PasteEvent("SELECT *\rFROM routes"));

        assertThat(state.text()).isEqualTo("SELECT * FROM routes");
    }

    @Test
    @DisplayName("a text area follows the emacs bindings: Ctrl+P and Ctrl+N move between lines, Ctrl+E to the end")
    void textAreaFollowsEmacsBindings() {
        TextAreaElement element = textArea().text("first\nsecond");

        EventResult up = element.handleKeyEvent(emacsCtrl('p'), true);
        EventResult end = element.handleKeyEvent(emacsCtrl('e'), true);

        assertThat(up).isEqualTo(EventResult.HANDLED);
        assertThat(end).isEqualTo(EventResult.HANDLED);
        assertThat(element.getState().cursorRow()).isEqualTo(0);
        assertThat(element.getState().cursorCol()).isEqualTo(5);
        assertThat(element.handleKeyEvent(emacsCtrl('n'), true)).isEqualTo(EventResult.HANDLED);
        assertThat(element.getState().cursorRow()).isEqualTo(1);
    }

    @Test
    @DisplayName("a paste into a text area keeps its lines when the terminal sends CR line breaks")
    void textAreaPasteWithCarriageReturns() {
        TextAreaElement element = textArea();

        element.handlePasteEvent(new PasteEvent("- from:\r    uri: timer:tick\r\n"));

        assertThat(element.getState().lineCount()).isEqualTo(3);
        assertThat(element.getState().text()).isEqualTo("- from:\n    uri: timer:tick\n");
    }

    private static KeyEvent emacsCtrl(char c) {
        return KeyEvent.ofChar(c, KeyModifiers.CTRL, BindingSets.emacs());
    }
}

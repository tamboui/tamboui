/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.tui.bindings.BindingSets;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.widgets.input.TextInputState;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TextInputElement.
 */
class TextInputElementTest extends AbstractElementTest {

    @Test
    @DisplayName("styleAttributes exposes title")
    void styleAttributes_exposesTitle() {
        assertThat(textInput().title("Username").styleAttributes()).containsEntry("title", "Username");
    }

    @Test
    @DisplayName("styleAttributes exposes placeholder")
    void styleAttributes_exposesPlaceholder() {
        assertThat(textInput().placeholder("Enter...").styleAttributes()).containsEntry("placeholder", "Enter...");
    }

    @Test
    @DisplayName("styleAttributes with empty placeholder does not expose it")
    void styleAttributes_emptyPlaceholderNotExposed() {
        assertThat(textInput().title("Name").styleAttributes()).doesNotContainKey("placeholder");
    }

    @Test
    @DisplayName("Attribute selector [title] affects TextInput border color")
    void attributeSelector_title_affectsBorderColor() {
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", "TextInputElement[title=\"Username\"] { border-color: cyan; }");
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        textInput().title("Username").render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.CYAN);
    }

    @Test
    @DisplayName("Attribute selector [placeholder] affects TextInput border color")
    void attributeSelector_placeholder_affectsBorderColor() {
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test", "TextInputElement[placeholder=\"Enter...\"] { border-color: yellow; }");
        styleEngine.setActiveStylesheet("test");

        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        // Need rounded() to enable border rendering for border-color to take effect
        textInput().placeholder("Enter...").rounded().render(frame, area, context);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.YELLOW);
    }

    @Test
    @DisplayName("Ctrl+B moves cursor left with emacs bindings")
    void ctrlB_emacs_movesCursorLeft() {
        TextInputState state = new TextInputState("Hello");
        KeyEvent event = new KeyEvent(KeyCode.CHAR, KeyModifiers.CTRL, 'b', BindingSets.emacs());

        boolean handled = handleTextInputKey(state, event);

        assertThat(handled).isTrue();
        assertThat(state.cursorPosition()).isEqualTo(4);
    }

    @Test
    @DisplayName("Ctrl+F moves cursor right with emacs bindings")
    void ctrlF_emacs_movesCursorRight() {
        TextInputState state = new TextInputState("Hello");
        state.moveCursorToStart();
        KeyEvent event = new KeyEvent(KeyCode.CHAR, KeyModifiers.CTRL, 'f', BindingSets.emacs());

        boolean handled = handleTextInputKey(state, event);

        assertThat(handled).isTrue();
        assertThat(state.cursorPosition()).isEqualTo(1);
    }

    @Test
    @DisplayName("Ctrl+A moves cursor to start with emacs bindings")
    void ctrlA_emacs_movesToStart() {
        TextInputState state = new TextInputState("Hello");
        KeyEvent event = new KeyEvent(KeyCode.CHAR, KeyModifiers.CTRL, 'a', BindingSets.emacs());

        boolean handled = handleTextInputKey(state, event);

        assertThat(handled).isTrue();
        assertThat(state.cursorPosition()).isEqualTo(0);
    }

    @Test
    @DisplayName("Ctrl+E moves cursor to end with emacs bindings")
    void ctrlE_emacs_movesToEnd() {
        TextInputState state = new TextInputState("Hello");
        state.moveCursorToStart();
        KeyEvent event = new KeyEvent(KeyCode.CHAR, KeyModifiers.CTRL, 'e', BindingSets.emacs());

        boolean handled = handleTextInputKey(state, event);

        assertThat(handled).isTrue();
        assertThat(state.cursorPosition()).isEqualTo(5);
    }

    @Test
    @DisplayName("Ctrl+H deletes backward with emacs bindings")
    void ctrlH_emacs_deletesBackward() {
        TextInputState state = new TextInputState("Hello");
        KeyEvent event = new KeyEvent(KeyCode.CHAR, KeyModifiers.CTRL, 'h', BindingSets.emacs());

        boolean handled = handleTextInputKey(state, event);

        assertThat(handled).isTrue();
        assertThat(state.text()).isEqualTo("Hell");
    }

    @Test
    @DisplayName("Ctrl+D deletes forward with emacs bindings")
    void ctrlD_emacs_deletesForward() {
        TextInputState state = new TextInputState("Hello");
        state.moveCursorToStart();
        KeyEvent event = new KeyEvent(KeyCode.CHAR, KeyModifiers.CTRL, 'd', BindingSets.emacs());

        boolean handled = handleTextInputKey(state, event);

        assertThat(handled).isTrue();
        assertThat(state.text()).isEqualTo("ello");
    }

    @Test
    @DisplayName("Unbound Ctrl combo is not handled")
    void unboundCtrl_notHandled() {
        TextInputState state = new TextInputState("Hello");
        KeyEvent event = new KeyEvent(KeyCode.CHAR, KeyModifiers.CTRL, 'z', BindingSets.emacs());

        boolean handled = handleTextInputKey(state, event);

        assertThat(handled).isFalse();
        assertThat(state.text()).isEqualTo("Hello");
    }
}

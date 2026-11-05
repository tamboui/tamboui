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
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyEventKind;
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
    @DisplayName("TextInputElement ignores release key events")
    void ignoresReleaseKeyEvents() {
        TextInputState state = new TextInputState("Initial");
        TextInputElement element = textInput(state);
        KeyEvent release = KeyEvent.builder()
                .code(KeyCode.CHAR)
                .character('x')
                .kind(KeyEventKind.RELEASE)
                .build();

        EventResult result = element.handleKeyEvent(release, true);

        assertThat(result).isEqualTo(EventResult.UNHANDLED);
        assertThat(state.text()).isEqualTo("Initial");
    }

    @Test
    @DisplayName("TextInputElement inserts multi-code-point text whole (ZWJ emoji, flag, CJK)")
    void insertsMultiCodePointText() {
        String zwj = new String(Character.toChars(0x1F469)) + "\u200D" + new String(Character.toChars(0x1F4BB));
        String flag = new String(Character.toChars(0x1F1EB)) + new String(Character.toChars(0x1F1F7));
        String cjk = "\u4E2D";

        for (String text : new String[] {zwj, flag, cjk}) {
            TextInputState state = new TextInputState();
            TextInputElement element = textInput(state);
            KeyEvent event = KeyEvent.builder()
                    .code(KeyCode.CHAR)
                    .text(text)
                    .kind(KeyEventKind.PRESS)
                    .build();

            EventResult result = element.handleKeyEvent(event, true);

            assertThat(result).as("result for \"" + text + "\"").isEqualTo(EventResult.HANDLED);
            assertThat(state.text()).as("inserted text").isEqualTo(text);
        }
    }

    @Test
    @DisplayName("TextInputElement inserts text from event text() property")
    void insertsAssociatedText() {
        TextInputState state = new TextInputState();
        TextInputElement element = textInput(state);
        KeyEvent textEvent = KeyEvent.builder()
                .code(KeyCode.CHAR)
                .text("Hello")
                .kind(KeyEventKind.PRESS)
                .build();

        EventResult result = element.handleKeyEvent(textEvent, true);

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(state.text()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("TextInputElement does not fire onSubmit twice on an Enter press/release pair")
    void onSubmitNotFiredOnEnterRelease() {
        TextInputState state = new TextInputState();
        int[] submitCount = {0};
        TextInputElement element = textInput(state).onSubmit(() -> submitCount[0]++);
        KeyEvent enterRelease = KeyEvent.builder()
                .code(KeyCode.ENTER)
                .kind(KeyEventKind.RELEASE)
                .build();

        element.handleKeyEvent(enterRelease, true);

        assertThat(submitCount[0]).as("onSubmit must not fire on RELEASE").isZero();
    }

    @Test
    @DisplayName("TextInputElement inserts keypad digits (KP_1 under FULL inserts \"1\")")
    void insertsKeypadDigit() {
        TextInputState state = new TextInputState();
        TextInputElement element = textInput(state);
        KeyEvent kp1 = KeyEvent.builder()
                .code(KeyCode.KP_1)
                .character('1')
                .kind(KeyEventKind.PRESS)
                .build();

        EventResult result = element.handleKeyEvent(kp1, true);

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(state.text()).isEqualTo("1");
    }

    @Test
    @DisplayName("TextInputElement ignores character when Hyper is pressed")
    void ignoresHyperKey() {
        TextInputState state = new TextInputState();
        TextInputElement element = textInput(state);
        KeyEvent hyperEvent = KeyEvent.builder()
                .code(KeyCode.CHAR)
                .character('x')
                .modifiers(KeyModifiers.builder().hyper(true).build())
                .kind(KeyEventKind.PRESS)
                .build();

        EventResult result = element.handleKeyEvent(hyperEvent, true);

        assertThat(result).isEqualTo(EventResult.UNHANDLED);
        assertThat(state.text()).isEmpty();
    }

    @Test
    @DisplayName("TextInputElement inserts terminal-provided text even on chord (AltGr=Ctrl+Alt -> text)")
    void insertsTextOnChordWhenTerminalProvidesIt() {
        // AltGr+e on Windows/Linux reports as Ctrl+Alt with associated text "\u20ac"
        TextInputState state = new TextInputState();
        TextInputElement element = textInput(state);
        KeyEvent altGrEuro = KeyEvent.builder()
                .code(KeyCode.CHAR)
                .character('\u20ac')
                .text("\u20ac")
                .modifiers(KeyModifiers.builder().ctrl(true).alt(true).build())
                .kind(KeyEventKind.PRESS)
                .build();

        EventResult result = element.handleKeyEvent(altGrEuro, true);

        assertThat(result).isEqualTo(EventResult.HANDLED);
        assertThat(state.text()).isEqualTo("\u20ac");
    }

    @Test
    @DisplayName("TextInputElement ignores character when Super or Meta is pressed")
    void ignoresSuperOrMetaKey() {
        TextInputState state = new TextInputState();
        TextInputElement element = textInput(state);
        KeyEvent superEvent = KeyEvent.builder()
                .code(KeyCode.CHAR)
                .character('c')
                .modifiers(KeyModifiers.SUPER)
                .kind(KeyEventKind.PRESS)
                .build();

        EventResult result = element.handleKeyEvent(superEvent, true);

        assertThat(result).isEqualTo(EventResult.UNHANDLED);
        assertThat(state.text()).isEmpty();
    }
}

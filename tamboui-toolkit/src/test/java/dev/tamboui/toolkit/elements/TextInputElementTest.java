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
        dev.tamboui.widgets.input.TextInputState state = new dev.tamboui.widgets.input.TextInputState("Initial");
        TextInputElement element = textInput(state);
        dev.tamboui.tui.event.KeyEvent release = dev.tamboui.tui.event.KeyEvent.builder()
                .code(dev.tamboui.tui.event.KeyCode.CHAR)
                .character('x')
                .kind(dev.tamboui.tui.event.KeyEventKind.RELEASE)
                .build();

        dev.tamboui.toolkit.event.EventResult result = element.handleKeyEvent(release, true);

        assertThat(result).isEqualTo(dev.tamboui.toolkit.event.EventResult.UNHANDLED);
        assertThat(state.text()).isEqualTo("Initial");
    }

    @Test
    @DisplayName("TextInputElement inserts text from event text() property")
    void insertsAssociatedText() {
        dev.tamboui.widgets.input.TextInputState state = new dev.tamboui.widgets.input.TextInputState();
        TextInputElement element = textInput(state);
        dev.tamboui.tui.event.KeyEvent textEvent = dev.tamboui.tui.event.KeyEvent.builder()
                .code(dev.tamboui.tui.event.KeyCode.CHAR)
                .text("Hello")
                .kind(dev.tamboui.tui.event.KeyEventKind.PRESS)
                .build();

        dev.tamboui.toolkit.event.EventResult result = element.handleKeyEvent(textEvent, true);

        assertThat(result).isEqualTo(dev.tamboui.toolkit.event.EventResult.HANDLED);
        assertThat(state.text()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("TextInputElement ignores character when Super or Meta is pressed")
    void ignoresSuperOrMetaKey() {
        dev.tamboui.widgets.input.TextInputState state = new dev.tamboui.widgets.input.TextInputState();
        TextInputElement element = textInput(state);
        dev.tamboui.tui.event.KeyEvent superEvent = dev.tamboui.tui.event.KeyEvent.builder()
                .code(dev.tamboui.tui.event.KeyCode.CHAR)
                .character('c')
                .modifiers(dev.tamboui.tui.event.KeyModifiers.SUPER)
                .kind(dev.tamboui.tui.event.KeyEventKind.PRESS)
                .build();

        dev.tamboui.toolkit.event.EventResult result = element.handleKeyEvent(superEvent, true);

        assertThat(result).isEqualTo(dev.tamboui.toolkit.event.EventResult.UNHANDLED);
        assertThat(state.text()).isEmpty();
    }
}

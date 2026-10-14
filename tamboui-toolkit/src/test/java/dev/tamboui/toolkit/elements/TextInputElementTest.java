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
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.widgets.input.TextInputState;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
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
    @DisplayName("A focused border color alone leaves room for the border, so the text stays visible when focused")
    void focusedBorderColorAlone_keepsTextVisibleWhenFocused() {
        Buffer buffer = renderAboveNext(
            textInput(new TextInputState("hello")).id("ti").focusedBorderColor(Color.CYAN), "ti");

        assertThat(buffer).hasContent(
            "┌──────────────────┐",
            "│hello             │",
            "└──────────────────┘",
            "next                ");
        assertThat(buffer.get(0, 0).style().fg()).contains(Color.CYAN);
    }

    @Test
    @DisplayName("A border color alone leaves room for the border it draws")
    void borderColorAlone_leavesRoomForTheBorder() {
        Buffer buffer = renderAboveNext(
            textInput(new TextInputState("hello")).borderColor(Color.CYAN), null);

        assertThat(buffer).hasContent(
            "┌──────────────────┐",
            "│hello             │",
            "└──────────────────┘",
            "next                ");
    }

    /** Renders the element above a "next" text in a 20x4 column, with the given element id focused. */
    private static Buffer renderAboveNext(Element element, String focusedId) {
        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        if (focusedId != null) {
            context.focusManager().setFocus(focusedId);
        }
        Rect area = new Rect(0, 0, 20, 4);
        Buffer buffer = Buffer.empty(area);
        column(element, text("next")).render(Frame.forTesting(buffer), area, context);
        return buffer;
    }
}

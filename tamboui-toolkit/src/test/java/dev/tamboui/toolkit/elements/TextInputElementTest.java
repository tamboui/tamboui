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
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.tui.event.KeyEvent;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TextInputElement.
 */
class TextInputElementTest {

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
    @DisplayName("handles key events")
    void handlesKeyEvents() {
        String unicodeChars = "AĄÄÉÅŞÞ";

        TextInputElement input = textInput();
        for (char ch : unicodeChars.toCharArray()) {
            input.handleKeyEvent(KeyEvent.ofChar(ch), true);
        }

        DefaultRenderContext context = DefaultRenderContext.createEmpty();

        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        input.renderContent(frame, area, context);

        String content = extractBufferContent(buffer, 0, 0, 10, 1);

        assertThat(content).contains(unicodeChars);
    }

    private String extractBufferContent(Buffer buffer, int startX, int startY, int width, int height) {
        StringBuilder sb = new StringBuilder();
        for (int y = startY; y < startY + height; y++) {
            for (int x = startX; x < startX + width; x++) {
                sb.append(buffer.get(x, y).symbol());
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}

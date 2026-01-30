/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.widgets.input;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.style.TestStylePropertyResolver;

import static org.assertj.core.api.Assertions.assertThat;

class TextInputTest {

    @Test
    @DisplayName("TextInput renders text content")
    void rendersTextContent() {
        TextInput input = TextInput.builder().build();
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        TextInputState state = new TextInputState("Hello");

        input.render(area, buffer, state);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("e");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("l");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("l");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("o");
    }

    @Test
    @DisplayName("TextInput renders placeholder when empty")
    void rendersPlaceholder() {
        TextInput input = TextInput.builder().placeholder("Enter text...").build();
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        TextInputState state = new TextInputState();

        input.render(area, buffer, state);

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("E");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("n");
    }

    @Test
    @DisplayName("TextInput applies style")
    void appliesStyle() {
        Style style = Style.EMPTY.fg(Color.CYAN);
        TextInput input = TextInput.builder().style(style).build();
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        TextInputState state = new TextInputState("Test");

        input.render(area, buffer, state);

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.CYAN);
    }

    @Test
    @DisplayName("TextInput uses PLACEHOLDER_COLOR property from StylePropertyResolver")
    void usesPlaceholderColorProperty() {
        TextInput input = TextInput.builder().placeholder("Enter text...")
                .styleResolver(TestStylePropertyResolver.of("placeholder-color", Color.DARK_GRAY))
                .build();
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        TextInputState state = new TextInputState();

        input.render(area, buffer, state);

        // Placeholder should have dark-gray foreground
        BufferAssertions.assertThat(buffer).at(0, 0).hasForeground(Color.DARK_GRAY);
    }
}

/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ErrorPlaceholder element.
 */
class ErrorPlaceholderTest {

    @Test
    @DisplayName("from(Throwable, String) creates placeholder with element ID")
    void fromWithElementId() {
        RuntimeException cause = new RuntimeException("Test error");

        ErrorPlaceholder placeholder = ErrorPlaceholder.from(cause, "my-widget");

        assertThat(placeholder.cause()).isSameAs(cause);
        assertThat(placeholder.elementId()).isEqualTo("my-widget");
    }

    @Test
    @DisplayName("from(Throwable) creates placeholder without element ID")
    void fromWithoutElementId() {
        RuntimeException cause = new RuntimeException("Test error");

        ErrorPlaceholder placeholder = ErrorPlaceholder.from(cause);

        assertThat(placeholder.cause()).isSameAs(cause);
        assertThat(placeholder.elementId()).isNull();
    }

    @Test
    @DisplayName("render() draws error placeholder in the area")
    void renderDrawsPlaceholder() {
        Rect area = new Rect(0, 0, 30, 5);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        RenderContext context = RenderContext.empty();

        ErrorPlaceholder placeholder = ErrorPlaceholder.from(new RuntimeException("Widget failed"),
                "test-widget");

        placeholder.render(frame, area, context);

        // Check that border was rendered (top-left corner)
        String topLeft = buffer.get(0, 0).symbol();
        assertThat(topLeft).isIn("┌", "╭", "+");

        // Check that something was written inside (error indicator)
        String firstContent = buffer.get(1, 1).symbol();
        assertThat(firstContent).isNotEqualTo(" ");
    }

    @Test
    @DisplayName("render() handles empty area gracefully")
    void renderHandlesEmptyArea() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 10));
        Frame frame = Frame.forTesting(buffer);
        RenderContext context = RenderContext.empty();

        ErrorPlaceholder placeholder = ErrorPlaceholder.from(new RuntimeException("Error"));

        // Should not throw
        assertThatCode(() -> placeholder.render(frame, emptyArea, context))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("render() handles small area gracefully")
    void renderHandlesSmallArea() {
        Rect smallArea = new Rect(0, 0, 3, 3);
        Buffer buffer = Buffer.empty(smallArea);
        Frame frame = Frame.forTesting(buffer);
        RenderContext context = RenderContext.empty();

        ErrorPlaceholder placeholder = ErrorPlaceholder.from(new RuntimeException("Error"));

        // Should not throw
        assertThatCode(() -> placeholder.render(frame, smallArea, context))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("id() returns null (placeholder has no ID)")
    void idReturnsNull() {
        ErrorPlaceholder placeholder = ErrorPlaceholder.from(new RuntimeException("Error"));

        assertThat(placeholder.id()).isNull();
    }
}

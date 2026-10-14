/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.assertj.BufferAssertions;
import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.AbstractElementTest;
import dev.tamboui.toolkit.element.RenderContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for ErrorPlaceholder element.
 */
class ErrorPlaceholderTest extends AbstractElementTest {

    private static final String CODER = "👩‍💻";

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

        ErrorPlaceholder placeholder = ErrorPlaceholder.from(
                new RuntimeException("Widget failed"),
                "test-widget"
        );

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

    @Test
    @DisplayName("preferredWidth() accounts for title and message")
    void preferredWidth() {
        ErrorPlaceholder placeholder = ErrorPlaceholder.from(
                new RuntimeException("Short"),
                "my-long-widget-id"
        );

        // Width should accommodate the wider of title/message + border (2)
        int width = placeholder.preferredSize(-1, -1, null).widthOr(0);
        assertThat(width).isGreaterThan(2);
    }

    @Test
    @DisplayName("preferredHeight() returns 3 (borders + content)")
    void preferredHeight() {
        ErrorPlaceholder placeholder = ErrorPlaceholder.from(new RuntimeException("Error"));

        assertThat(placeholder.preferredSize(-1, -1, null).heightOr(0)).isEqualTo(3);
    }

    @Test
    @DisplayName("renders border around error content")
    void rendersBorderAroundContent() {
        Rect area = new Rect(0, 0, 30, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        RenderContext context = RenderContext.empty();

        ErrorPlaceholder placeholder = ErrorPlaceholder.from(
                new RuntimeException("Oops"),
                "widget-x"
        );

        placeholder.render(frame, area, context);

        // Top-left border corner
        BufferAssertions.assertThat(buffer).at(0, 0).hasSymbol("┌");
        // Top-right border corner
        BufferAssertions.assertThat(buffer).at(29, 0).hasSymbol("┐");
        // Bottom-left border corner
        BufferAssertions.assertThat(buffer).at(0, 2).hasSymbol("└");
        // Bottom-right border corner
        BufferAssertions.assertThat(buffer).at(29, 2).hasSymbol("┘");
    }

    @Test
    @DisplayName("render() cuts a long message at the right border, not drawing past the placeholder")
    void renderCutsLongMessageAtRightBorder() {
        Buffer buffer = Buffer.empty(new Rect(0, 0, 40, 3));
        Frame frame = Frame.forTesting(buffer);
        RenderContext context = RenderContext.empty();

        ErrorPlaceholder placeholder = ErrorPlaceholder.from(
                new IllegalStateException("the price column expects a number"),
                "prices"
        );

        placeholder.render(frame, new Rect(0, 0, 20, 3), context);

        // the right border is kept and nothing is drawn right of the placeholder
        BufferAssertions.assertThat(buffer).hasRow(1, "│! IllegalStateExce│                    ");
    }

    @Test
    @DisplayName("render() keeps a ZWJ emoji at the right border whole, or leaves it out when it does not fit")
    void renderCutsZwjEmojiAtRightBorder() {
        // "! RuntimeException: a" takes 21 columns, the emoji the next two
        ErrorPlaceholder placeholder = ErrorPlaceholder.from(new RuntimeException("a" + CODER + " is not a price"));

        Buffer fits = Buffer.empty(new Rect(0, 0, 30, 3));
        placeholder.render(Frame.forTesting(fits), new Rect(0, 0, 25, 3), RenderContext.empty());
        BufferAssertions.assertThat(fits).hasRow(1, "│! RuntimeException: a" + CODER + "│     ");

        Buffer oneColumnShort = Buffer.empty(new Rect(0, 0, 30, 3));
        placeholder.render(Frame.forTesting(oneColumnShort), new Rect(0, 0, 24, 3), RenderContext.empty());
        BufferAssertions.assertThat(oneColumnShort).hasRow(1, "│! RuntimeException: a │      ");
    }
}

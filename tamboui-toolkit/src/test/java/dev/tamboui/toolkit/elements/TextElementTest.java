/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.cascade.CssStyleResolver;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.widgets.text.Overflow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TextElement.
 */
class TextElementTest {

    @Test
    @DisplayName("TextElement fluent API chains correctly")
    void fluentApiChaining() {
        TextElement element = text("Hello, World!")
            .bold()
            .italic()
            .underlined()
            .fg(Color.CYAN)
            .bg(Color.BLACK)
            .dim();

        assertThat(element).isInstanceOf(TextElement.class);
    }

    @Test
    @DisplayName("text(String) creates element with content")
    void textWithString() {
        TextElement element = text("Hello");
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("text(Object) uses toString")
    void textWithObject() {
        TextElement element = text(42);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("Color shortcuts work")
    void colorShortcuts() {
        TextElement element = text("Colored")
            .red()
            .onBlue();

        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("TextElement renders to buffer")
    void rendersToBuffer() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        text("Hello")
            .render(frame, area, RenderContext.empty());

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("e");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("l");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("l");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("o");
    }

    @Test
    @DisplayName("TextElement with style renders correctly")
    void rendersWithStyle() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        text("Hi")
            .bold()
            .fg(Color.RED)
            .render(frame, area, RenderContext.empty());

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(0, 0).style().fg()).contains(Color.RED);
        assertThat(buffer.get(0, 0).style().effectiveModifiers().contains(Modifier.BOLD)).isTrue();
    }

    @Test
    @DisplayName("Empty area does not render")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        Frame frame = Frame.forTesting(buffer);

        // Should not throw
        text("Test").render(frame, emptyArea, RenderContext.empty());
    }

    @Test
    @DisplayName("length() sets constraint")
    void lengthConstraint() {
        TextElement element = text("Test").length(10);
        assertThat(element.constraint()).isEqualTo(Constraint.length(10));
    }

    @Test
    @DisplayName("percent() sets constraint")
    void percentConstraint() {
        TextElement element = text("Test").percent(50);
        assertThat(element.constraint()).isEqualTo(Constraint.percentage(50));
    }

    @Test
    @DisplayName("fill() sets constraint")
    void fillConstraint() {
        TextElement element = text("Test").fill();
        assertThat(element.constraint()).isEqualTo(Constraint.fill());
    }

    @Test
    @DisplayName("null value renders as empty string")
    void nullValue() {
        TextElement element = text((Object) null);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("All color methods work")
    void allColorMethods() {
        TextElement element = text("Colors")
            .cyan()
            .yellow()
            .green()
            .blue()
            .magenta()
            .white()
            .gray();

        // Should not throw
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("Background color methods work")
    void backgroundColorMethods() {
        TextElement element = text("BG")
            .onRed()
            .onGreen()
            .onYellow()
            .onBlue()
            .onMagenta()
            .onCyan()
            .onWhite()
            .onBlack();

        assertThat(element).isNotNull();
    }

    @Nested
    @DisplayName("Default constraint behavior")
    class DefaultConstraintTests {

        @Test
        @DisplayName("Single line text without overflow defaults to null (container decides)")
        void singleLineDefaultsToNull() {
            TextElement element = text("Hello, World!");
            assertThat(element.constraint()).isNull();
        }

        @Test
        @DisplayName("Multi-line text without overflow defaults to null (container decides)")
        void multiLineDefaultsToNull() {
            TextElement element = text("Line 1\nLine 2\nLine 3");
            assertThat(element.constraint()).isNull();
        }

        @Test
        @DisplayName("Empty text defaults to null (container decides)")
        void emptyTextDefaultsToNull() {
            TextElement element = text("");
            assertThat(element.constraint()).isNull();
        }

        @Test
        @DisplayName("Explicit constraint overrides default")
        void explicitConstraintOverridesDefault() {
            TextElement element = text("Hello").fill();
            assertThat(element.constraint()).isEqualTo(Constraint.fill());
        }

        @Test
        @DisplayName("WRAP_WORD overflow defaults to min(1)")
        void wrapWordDefaultsToMin() {
            TextElement element = text("Some wrapping text").overflow(Overflow.WRAP_WORD);
            assertThat(element.constraint()).isEqualTo(Constraint.min(1));
        }

        @Test
        @DisplayName("WRAP_CHARACTER overflow defaults to min(1)")
        void wrapCharacterDefaultsToMin() {
            TextElement element = text("Some wrapping text").overflow(Overflow.WRAP_CHARACTER);
            assertThat(element.constraint()).isEqualTo(Constraint.min(1));
        }

        @Test
        @DisplayName("WRAP_WORD with multi-line text defaults to min(lineCount)")
        void wrapWordMultiLineDefaultsToMinN() {
            TextElement element = text("Line 1\nLine 2").overflow(Overflow.WRAP_WORD);
            assertThat(element.constraint()).isEqualTo(Constraint.min(2));
        }

        @Test
        @DisplayName("ELLIPSIS overflow defaults to null (container decides)")
        void ellipsisDefaultsToNull() {
            TextElement element = text("Truncated text").overflow(Overflow.ELLIPSIS);
            assertThat(element.constraint()).isNull();
        }

        @Test
        @DisplayName("CLIP overflow defaults to null (container decides)")
        void clipDefaultsToNull() {
            TextElement element = text("Clipped text").overflow(Overflow.CLIP);
            assertThat(element.constraint()).isNull();
        }

        @Test
        @DisplayName("Column with multiple text elements renders compactly without extra spacing")
        void columnRendersTextCompactly() {
            Rect area = new Rect(0, 0, 20, 10);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            column(
                text("Line 1"),
                text("Line 2"),
                text("Line 3")
            ).render(frame, area, RenderContext.empty());

            // Build expected buffer - text should be on consecutive lines without gaps
            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "Line 1", Style.EMPTY);
            expected.setString(0, 1, "Line 2", Style.EMPTY);
            expected.setString(0, 2, "Line 3", Style.EMPTY);

            assertThat(buffer).isEqualTo(expected);
        }

        @Test
        @DisplayName("Column with wrapping text renders wrapped content")
        void columnRendersWrappingText() {
            Rect area = new Rect(0, 0, 10, 10);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            column(
                text("Short"),
                text("This is a long line that should wrap").overflow(Overflow.WRAP_WORD)
            ).render(frame, area, RenderContext.empty());

            // First line should be "Short" at line 0
            Buffer expected = Buffer.empty(area);
            expected.setString(0, 0, "Short", Style.EMPTY);
            // Wrapped text starts at line 1 and continues
            expected.setString(0, 1, "This is a", Style.EMPTY);
            expected.setString(0, 2, "long line", Style.EMPTY);
            expected.setString(0, 3, "that", Style.EMPTY);
            expected.setString(0, 4, "should", Style.EMPTY);
            expected.setString(0, 5, "wrap", Style.EMPTY);

            assertThat(buffer).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Width-aware preferred height")
    class PreferredHeightWithWidthTests {

        @Test
        @DisplayName("preferredHeight(width) returns 1 for short text without wrapping")
        void shortTextNoWrapping() {
            TextElement element = text("Hello");
            assertThat(element.preferredHeight(20, null)).isEqualTo(1);
        }

        @Test
        @DisplayName("preferredHeight(width) returns line count for non-wrapping overflow modes")
        void nonWrappingModes() {
            TextElement element = text("Hello\nWorld").overflow(Overflow.CLIP);
            assertThat(element.preferredHeight(5, null)).isEqualTo(2);

            TextElement ellipsis = text("Hello\nWorld\nTest").overflow(Overflow.ELLIPSIS);
            assertThat(ellipsis.preferredHeight(5, null)).isEqualTo(3);
        }

        @Test
        @DisplayName("preferredHeight(width) calculates wrapped lines for WRAP_CHARACTER")
        void wrapCharacterCalculation() {
            // 20 chars should wrap to 2 lines at width 10
            TextElement element = text("12345678901234567890").overflow(Overflow.WRAP_CHARACTER);
            assertThat(element.preferredHeight(10, null)).isEqualTo(2);

            // 25 chars should wrap to 3 lines at width 10
            TextElement longer = text("1234567890123456789012345").overflow(Overflow.WRAP_CHARACTER);
            assertThat(longer.preferredHeight(10, null)).isEqualTo(3);
        }

        @Test
        @DisplayName("preferredHeight(width) calculates wrapped lines for WRAP_WORD")
        void wrapWordCalculation() {
            // Same calculation as WRAP_CHARACTER for preferredHeight
            TextElement element = text("12345678901234567890").overflow(Overflow.WRAP_WORD);
            assertThat(element.preferredHeight(10, null)).isEqualTo(2);
        }

        @Test
        @DisplayName("preferredHeight(width) handles multi-line text with wrapping")
        void multiLineWithWrapping() {
            // Two lines, each 15 chars, should wrap to 4 lines at width 10
            TextElement element = text("123456789012345\n123456789012345").overflow(Overflow.WRAP_CHARACTER);
            assertThat(element.preferredHeight(10, null)).isEqualTo(4);
        }

        @Test
        @DisplayName("preferredHeight(width) returns 1 for empty content")
        void emptyContent() {
            TextElement element = text("").overflow(Overflow.WRAP_CHARACTER);
            assertThat(element.preferredHeight(10, null)).isEqualTo(1);
        }

        @Test
        @DisplayName("preferredHeight(width) returns 1 for zero or negative width")
        void zeroWidth() {
            TextElement element = text("Hello World").overflow(Overflow.WRAP_CHARACTER);
            assertThat(element.preferredHeight(0, null)).isEqualTo(1);
            assertThat(element.preferredHeight(-5, null)).isEqualTo(1);
        }

        @Test
        @DisplayName("preferredHeight(width) handles exact width match")
        void exactWidthMatch() {
            // 10 chars at width 10 should be exactly 1 line
            TextElement element = text("1234567890").overflow(Overflow.WRAP_CHARACTER);
            assertThat(element.preferredHeight(10, null)).isEqualTo(1);
        }

        @Test
        @DisplayName("preferredHeight(width) handles width of 1")
        void widthOfOne() {
            TextElement element = text("ABC").overflow(Overflow.WRAP_CHARACTER);
            assertThat(element.preferredHeight(1, null)).isEqualTo(3);
        }

        @Test
        @DisplayName("preferredHeight() without width delegates to line count")
        void preferredHeightWithoutWidth() {
            TextElement single = text("Hello World");
            assertThat(single.preferredHeight()).isEqualTo(1);

            TextElement multi = text("Line1\nLine2\nLine3");
            assertThat(multi.preferredHeight()).isEqualTo(3);
        }

        @Test
        @DisplayName("preferredHeight resolves overflow from CSS via RenderContext")
        void resolvesOverflowFromCss() {
            // Create element without programmatic overflow
            TextElement element = text("12345678901234567890");

            // Create CSS resolver with text-overflow: wrap-character
            CssStyleResolver cssResolver = CssStyleResolver.builder()
                    .property("text-overflow", "wrap-character")
                    .build();

            // Create mock RenderContext that returns the resolver for this element
            RenderContext context = new RenderContext() {
                @Override
                public boolean isFocused(String elementId) {
                    return false;
                }

                @Override
                public boolean hasFocus() {
                    return false;
                }

                @Override
                public java.util.Optional<CssStyleResolver> resolveStyle(dev.tamboui.css.Styleable styleable) {
                    return java.util.Optional.of(cssResolver);
                }
            };

            // Without CSS (null context), should return 1 (CLIP mode)
            assertThat(element.preferredHeight(10, null)).isEqualTo(1);

            // With CSS providing wrap-character, should wrap to 2 lines
            assertThat(element.preferredHeight(10, context)).isEqualTo(2);
        }
    }
}

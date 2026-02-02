/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.RichTextState;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Line;
import dev.tamboui.text.Span;
import dev.tamboui.text.Text;
import dev.tamboui.toolkit.element.RenderContext;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for RichTextAreaElement.
 */
class RichTextAreaElementTest {

    @Test
    @DisplayName("RichTextAreaElement fluent API chains correctly")
    void fluentApiChaining() {
        RichTextAreaElement element = richTextArea("Hello, World!")
            .bold()
            .fg(Color.CYAN)
            .wrapWord()
            .showLineNumbers();

        assertThat(element).isInstanceOf(RichTextAreaElement.class);
    }

    @Test
    @DisplayName("richTextArea(String) creates element with content")
    void richTextWithString() {
        RichTextAreaElement element = richTextArea("Hello");
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("richTextArea(Text) creates element with styled text")
    void richTextWithText() {
        Text styledText = Text.from(Line.from(
            Span.styled("Red", Style.EMPTY.fg(Color.RED)),
            Span.styled("Blue", Style.EMPTY.fg(Color.BLUE))
        ));
        RichTextAreaElement element = richTextArea(styledText);
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("richTextArea() creates empty element")
    void richTextEmpty() {
        RichTextAreaElement element = richTextArea();
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("RichTextAreaElement renders to buffer")
    void rendersToBuffer() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        richTextArea("Hello")
            .render(frame, area, RenderContext.empty());

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("e");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("l");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("l");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("o");
    }

    @Test
    @DisplayName("RichTextAreaElement renders styled spans")
    void rendersStyledSpans() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        Text text = Text.from(Line.from(
            Span.styled("Red", Style.EMPTY.fg(Color.RED)),
            Span.styled("Blue", Style.EMPTY.fg(Color.BLUE))
        ));

        richTextArea(text)
            .render(frame, area, RenderContext.empty());

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.RED);
        assertThat(buffer.get(3, 0).style().fg()).contains(Color.BLUE);
    }

    @Test
    @DisplayName("RichTextAreaElement renders multi-line text")
    void rendersMultiLineText() {
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        richTextArea("Line 1\nLine 2\nLine 3")
            .render(frame, area, RenderContext.empty());

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("L");
        assertThat(buffer.get(0, 1).symbol()).isEqualTo("L");
        assertThat(buffer.get(0, 2).symbol()).isEqualTo("L");
    }

    @Test
    @DisplayName("RichTextAreaElement word wrapping works")
    void wordWrappingWorks() {
        Rect area = new Rect(0, 0, 7, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        richTextArea("Hello World")
            .wrapWord()
            .render(frame, area, RenderContext.empty());

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Hello", Style.EMPTY);
        expected.setString(0, 1, "World", Style.EMPTY);
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("RichTextAreaElement ellipsis works")
    void ellipsisWorks() {
        Rect area = new Rect(0, 0, 8, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        richTextArea("Hello World")
            .ellipsis()
            .render(frame, area, RenderContext.empty());

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Hello...", Style.EMPTY);
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("RichTextAreaElement state allows scroll control")
    void stateAllowsScrollControl() {
        // Need to render first so state gets content dimensions
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        RichTextAreaElement element = richTextArea("Line 1\nLine 2\nLine 3\nLine 4\nLine 5");
        element.render(frame, area, RenderContext.empty());

        RichTextState state = element.state();

        assertThat(state.scrollRow()).isEqualTo(0);

        state.scrollDown(2);
        assertThat(state.scrollRow()).isEqualTo(2);

        state.scrollToTop();
        assertThat(state.scrollRow()).isEqualTo(0);
    }

    @Test
    @DisplayName("RichTextAreaElement scrollToLine works")
    void scrollToLineWorks() {
        // Need to render first so state gets content dimensions
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        RichTextAreaElement element = richTextArea("Line 1\nLine 2\nLine 3\nLine 4\nLine 5");
        element.render(frame, area, RenderContext.empty());

        element.scrollToLine(3);

        assertThat(element.state().scrollRow()).isEqualTo(2); // Clamped to maxScrollRow
    }

    @Test
    @DisplayName("RichTextAreaElement preferredHeight returns line count")
    void preferredHeightReturnsLineCount() {
        RichTextAreaElement element = richTextArea("Line 1\nLine 2\nLine 3");

        assertThat(element.preferredHeight()).isEqualTo(3);
    }

    @Test
    @DisplayName("RichTextAreaElement constraint defaults for wrapping modes")
    void constraintDefaultsForWrapping() {
        RichTextAreaElement wrapWord = richTextArea("Test").wrapWord();
        assertThat(wrapWord.constraint()).isEqualTo(Constraint.min(1));

        RichTextAreaElement wrapChar = richTextArea("Test").wrapCharacter();
        assertThat(wrapChar.constraint()).isEqualTo(Constraint.min(1));
    }

    @Test
    @DisplayName("RichTextAreaElement explicit constraint overrides default")
    void explicitConstraintOverridesDefault() {
        RichTextAreaElement element = richTextArea("Test").fill();
        assertThat(element.constraint()).isEqualTo(Constraint.fill());
    }

    @Test
    @DisplayName("Empty area does not render")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        Frame frame = Frame.forTesting(buffer);

        // Should not throw
        richTextArea("Test").render(frame, emptyArea, RenderContext.empty());
    }

    @Test
    @DisplayName("RichTextAreaElement alignment works")
    void alignmentWorks() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        richTextArea("Hi")
            .centered()
            .render(frame, area, RenderContext.empty());

        // "Hi" is 2 chars, centered in 10 chars = starts at position 4
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(5, 0).symbol()).isEqualTo("i");
    }

    @Test
    @DisplayName("RichTextAreaElement right alignment works")
    void rightAlignmentWorks() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        richTextArea("Hi")
            .right()
            .render(frame, area, RenderContext.empty());

        // "Hi" is 2 chars, right-aligned in 10 chars = starts at position 8
        assertThat(buffer.get(8, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(9, 0).symbol()).isEqualTo("i");
    }

    @Test
    @DisplayName("RichTextAreaElement title renders border")
    void titleRendersBorder() {
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        richTextArea("Content")
            .title("Title")
            .render(frame, area, RenderContext.empty());

        // Border should be rendered
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("┌");
    }

    @Test
    @DisplayName("RichTextAreaElement rounded border works")
    void roundedBorderWorks() {
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        richTextArea("Content")
            .rounded()
            .render(frame, area, RenderContext.empty());

        // Rounded border should be rendered
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("╭");
    }

    @Test
    @DisplayName("RichTextAreaElement shows line numbers")
    void showsLineNumbers() {
        Rect area = new Rect(0, 0, 15, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        richTextArea("Line A\nLine B\nLine C")
            .showLineNumbers()
            .render(frame, area, RenderContext.empty());

        // Line numbers should be rendered (1, 2, 3) followed by content
        // Default separator is single space
        assertThat(buffer).at(0, 0).hasSymbol("1");
        assertThat(buffer).at(1, 0).hasSymbol(" ");
        assertThat(buffer).at(2, 0).hasSymbol("L");
        assertThat(buffer).at(0, 1).hasSymbol("2");
        assertThat(buffer).at(2, 1).hasSymbol("L");
        assertThat(buffer).at(0, 2).hasSymbol("3");
        assertThat(buffer).at(2, 2).hasSymbol("L");
    }

    @Test
    @DisplayName("RichTextAreaElement line numbers are styled dim by default")
    void lineNumbersStyledDim() {
        Rect area = new Rect(0, 0, 15, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        richTextArea("Content")
            .showLineNumbers()
            .render(frame, area, RenderContext.empty());

        // Line number "1" should have DIM modifier
        assertThat(buffer.get(0, 0).style().addModifiers()).contains(Modifier.DIM);
    }

    @Test
    @DisplayName("RichTextAreaElement line numbers with custom style")
    void lineNumbersWithCustomStyle() {
        Rect area = new Rect(0, 0, 15, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        richTextArea("Content")
            .showLineNumbers()
            .lineNumberStyle(Style.EMPTY.fg(Color.YELLOW))
            .render(frame, area, RenderContext.empty());

        // Line number "1" should have YELLOW foreground
        assertThat(buffer).at(0, 0).hasSymbol("1").hasForeground(Color.YELLOW);
    }

    @Test
    @DisplayName("RichTextAreaElement preserves span styles with line numbers")
    void preservesSpanStylesWithLineNumbers() {
        Rect area = new Rect(0, 0, 15, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        Text text = Text.from(Line.from(
            Span.styled("Red", Style.EMPTY.fg(Color.RED))
        ));

        richTextArea(text)
            .showLineNumbers()
            .render(frame, area, RenderContext.empty());

        // Line number at 0, content "Red" starts at 2
        assertThat(buffer).at(0, 0).hasSymbol("1");
        assertThat(buffer).at(2, 0).hasSymbol("R").hasForeground(Color.RED);
    }

}

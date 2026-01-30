/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.RichTextState;
import dev.tamboui.style.Style;
import dev.tamboui.style.StyledAreaInfo;
import dev.tamboui.style.StyledAreaRegistry;
import dev.tamboui.terminal.Frame;
import dev.tamboui.text.Text;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.RenderContext;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MarkupTextAreaElement.
 */
class MarkupTextAreaElementTest {

    @Test
    @DisplayName("MarkupTextAreaElement fluent API chains correctly")
    void fluentApiChaining() {
        MarkupTextAreaElement element = markupTextArea("[bold]Hello[/bold]").wrapWord()
                .showLineNumbers();

        assertThat(element).isInstanceOf(MarkupTextAreaElement.class);
    }

    @Test
    @DisplayName("markupTextArea(String) creates element with content")
    void markupTextWithString() {
        MarkupTextAreaElement element = markupTextArea("Hello");
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("markupTextArea() creates empty element")
    void markupTextEmpty() {
        MarkupTextAreaElement element = markupTextArea();
        assertThat(element).isNotNull();
    }

    @Test
    @DisplayName("MarkupTextAreaElement renders plain text")
    void rendersPlainText() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        markupTextArea("Hello").render(frame, area, RenderContext.empty());

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(1, 0).symbol()).isEqualTo("e");
        assertThat(buffer.get(2, 0).symbol()).isEqualTo("l");
        assertThat(buffer.get(3, 0).symbol()).isEqualTo("l");
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("o");
    }

    @Test
    @DisplayName("MarkupTextAreaElement renders bold markup")
    void rendersBoldMarkup() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        markupTextArea("[bold]Hello[/bold]").render(frame, area, RenderContext.empty());

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(0, 0).style().addModifiers()).contains(Modifier.BOLD);
    }

    @Test
    @DisplayName("MarkupTextAreaElement renders color markup")
    void rendersColorMarkup() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        markupTextArea("[red]Red[/red] and [blue]Blue[/blue]").render(frame, area,
                RenderContext.empty());

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.RED);
        assertThat(buffer.get(8, 0).style().fg()).contains(Color.BLUE);
    }

    @Test
    @DisplayName("MarkupTextAreaElement renders nested markup")
    void rendersNestedMarkup() {
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        markupTextArea("[red][bold]Both[/bold][/red]").render(frame, area, RenderContext.empty());

        assertThat(buffer.get(0, 0).style().fg()).contains(Color.RED);
        assertThat(buffer.get(0, 0).style().addModifiers()).contains(Modifier.BOLD);
    }

    @Test
    @DisplayName("MarkupTextAreaElement renders multi-line markup")
    void rendersMultiLineMarkup() {
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        markupTextArea("Normal\n[bold]Bold[/bold]\n[red]Red[/red]").render(frame, area,
                RenderContext.empty());

        assertThat(buffer.get(0, 0).symbol()).isEqualTo("N");
        assertThat(buffer.get(0, 1).style().addModifiers()).contains(Modifier.BOLD);
        assertThat(buffer.get(0, 2).style().fg()).contains(Color.RED);
    }

    @Test
    @DisplayName("MarkupTextAreaElement word wrapping works")
    void wordWrappingWorks() {
        Rect area = new Rect(0, 0, 7, 2);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        markupTextArea("Hello World").wrapWord().render(frame, area, RenderContext.empty());

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Hello", Style.EMPTY);
        expected.setString(0, 1, "World", Style.EMPTY);
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("MarkupTextAreaElement ellipsis works")
    void ellipsisWorks() {
        Rect area = new Rect(0, 0, 8, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        markupTextArea("Hello World").ellipsis().render(frame, area, RenderContext.empty());

        Buffer expected = Buffer.empty(area);
        expected.setString(0, 0, "Hello...", Style.EMPTY);
        assertThat(buffer).isEqualTo(expected);
    }

    @Test
    @DisplayName("MarkupTextAreaElement state allows scroll control")
    void stateAllowsScrollControl() {
        // Need to render first so state gets content dimensions
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        MarkupTextAreaElement element = markupTextArea("Line 1\nLine 2\nLine 3\nLine 4\nLine 5");
        element.render(frame, area, RenderContext.empty());

        RichTextState state = element.state();

        assertThat(state.scrollRow()).isEqualTo(0);

        state.scrollDown(2);
        assertThat(state.scrollRow()).isEqualTo(2);

        state.scrollToTop();
        assertThat(state.scrollRow()).isEqualTo(0);
    }

    @Test
    @DisplayName("MarkupTextAreaElement scrollToLine works")
    void scrollToLineWorks() {
        // Need to render first so state gets content dimensions
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        MarkupTextAreaElement element = markupTextArea("Line 1\nLine 2\nLine 3\nLine 4\nLine 5");
        element.render(frame, area, RenderContext.empty());

        element.scrollToLine(3);

        assertThat(element.state().scrollRow()).isEqualTo(2); // Clamped to maxScrollRow
    }

    @Test
    @DisplayName("MarkupTextAreaElement preferredHeight returns line count")
    void preferredHeightReturnsLineCount() {
        MarkupTextAreaElement element = markupTextArea("Line 1\nLine 2\nLine 3");

        assertThat(element.preferredHeight()).isEqualTo(3);
    }

    @Test
    @DisplayName("MarkupTextAreaElement parsedText returns parsed Text object")
    void parsedTextReturnsParsedText() {
        MarkupTextAreaElement element = markupTextArea("[bold]Hello[/bold]");

        Text parsedText = element.parsedText();
        assertThat(parsedText.lines()).hasSize(1);
        assertThat(parsedText.lines().get(0).spans().get(0).content()).isEqualTo("Hello");
        assertThat(parsedText.lines().get(0).spans().get(0).style().addModifiers())
                .contains(Modifier.BOLD);
    }

    @Test
    @DisplayName("MarkupTextAreaElement title renders border")
    void titleRendersBorder() {
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        markupTextArea("Content").title("Title").render(frame, area, RenderContext.empty());

        // Border should be rendered
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("┌");
    }

    @Test
    @DisplayName("MarkupTextAreaElement rounded border works")
    void roundedBorderWorks() {
        Rect area = new Rect(0, 0, 20, 3);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        markupTextArea("Content").rounded().render(frame, area, RenderContext.empty());

        // Rounded border should be rendered
        assertThat(buffer.get(0, 0).symbol()).isEqualTo("╭");
    }

    @Test
    @DisplayName("MarkupTextAreaElement markup method updates content")
    void markupMethodUpdatesContent() {
        MarkupTextAreaElement element = markupTextArea("[bold]First[/bold]");

        Text first = element.parsedText();
        assertThat(first.lines().get(0).spans().get(0).content()).isEqualTo("First");

        element.markup("[italic]Second[/italic]");

        Text second = element.parsedText();
        assertThat(second.lines().get(0).spans().get(0).content()).isEqualTo("Second");
        assertThat(second.lines().get(0).spans().get(0).style().addModifiers())
                .contains(Modifier.ITALIC);
    }

    @Test
    @DisplayName("MarkupTextAreaElement alignment works")
    void alignmentWorks() {
        Rect area = new Rect(0, 0, 10, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        markupTextArea("Hi").centered().render(frame, area, RenderContext.empty());

        // "Hi" is 2 chars, centered in 10 chars = starts at position 4
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("H");
        assertThat(buffer.get(5, 0).symbol()).isEqualTo("i");
    }

    @Test
    @DisplayName("Empty area does not render")
    void emptyAreaNoRender() {
        Rect emptyArea = new Rect(0, 0, 0, 0);
        Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
        Frame frame = Frame.forTesting(buffer);

        // Should not throw
        markupTextArea("[bold]Test[/bold]").render(frame, emptyArea, RenderContext.empty());
    }

    @Test
    @DisplayName("MarkupTextAreaElement renders escaped brackets")
    void rendersEscapedBrackets() {
        Rect area = new Rect(0, 0, 30, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        markupTextArea("Use [[tag]] for brackets").render(frame, area, RenderContext.empty());

        // Should render literal bracket
        assertThat(buffer.get(4, 0).symbol()).isEqualTo("[");
    }

    @Test
    @DisplayName("MarkupTextAreaElement resolves custom tags via CSS classes")
    void resolvesCustomTagsViaCssClasses() {
        // Set up StyleEngine with CSS defining .keyword class
        StyleEngine styleEngine = StyleEngine.create();
        styleEngine.addStylesheet("test",
                ".keyword {\n" + "    color: magenta;\n" + "    text-style: bold;\n" + "}");
        styleEngine.setActiveStylesheet("test");

        // Create context with StyleEngine
        DefaultRenderContext context = DefaultRenderContext.createEmpty();
        context.setStyleEngine(styleEngine);

        // Render markup with custom [keyword] tag
        Rect area = new Rect(0, 0, 20, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);

        markupTextArea("[keyword]function[/keyword]").render(frame, area, context);

        // The text "function" should be styled with magenta color and bold
        assertThat(buffer).at(0, 0).hasSymbol("f").hasForeground(Color.MAGENTA);
    }

    @Test
    @DisplayName("MarkupTextAreaElement registers markup tags as CSS classes for TFX targeting")
    void registersMarkupTagsAsCssClasses() {
        // Create context with element registry
        DefaultRenderContext context = DefaultRenderContext.createEmpty();

        // Create styled area registry and configure frame
        StyledAreaRegistry styledAreaRegistry = StyledAreaRegistry.create();
        Rect area = new Rect(0, 0, 50, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        frame.setStyledAreaRegistry(styledAreaRegistry);

        // Render markup with custom tags
        markupTextArea("[looping]animated[/looping] text").render(frame, area, context);

        // Query the styled area registry for spans with the "looping" tag
        List<StyledAreaInfo> allAreas = styledAreaRegistry.all();
        boolean hasLoopingTag = allAreas.stream()
                .anyMatch(info -> info.tags().values().contains("looping"));

        // Should have a styled area with the "looping" tag
        assertThat(hasLoopingTag).isTrue();
    }

    @Test
    @DisplayName("MarkupTextAreaElement supports multiple CSS classes from nested tags")
    void supportsMultipleCssClassesFromNestedTags() {
        // Create context with element registry
        DefaultRenderContext context = DefaultRenderContext.createEmpty();

        // Create styled area registry and configure frame
        StyledAreaRegistry styledAreaRegistry = StyledAreaRegistry.create();
        Rect area = new Rect(0, 0, 50, 1);
        Buffer buffer = Buffer.empty(area);
        Frame frame = Frame.forTesting(buffer);
        frame.setStyledAreaRegistry(styledAreaRegistry);

        // Render markup with nested custom tags
        markupTextArea("[effect1][effect2]styled[/effect2][/effect1]").render(frame, area, context);

        // Query the styled area registry for spans with the effect tags
        List<StyledAreaInfo> allAreas = styledAreaRegistry.all();

        // With nested tags, the styled span should have both tags
        boolean hasEffect1 = allAreas.stream()
                .anyMatch(info -> info.tags().values().contains("effect1"));
        boolean hasEffect2 = allAreas.stream()
                .anyMatch(info -> info.tags().values().contains("effect2"));

        // Should have styled areas with both CSS classes
        assertThat(hasEffect1).isTrue();
        assertThat(hasEffect2).isTrue();
    }
}

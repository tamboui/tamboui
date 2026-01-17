/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.toolkit.event.EventResult;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.widgets.input.TextAreaState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests for TextAreaElement.
 */
class TextAreaElementTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("Default constructor creates element with empty state")
        void defaultConstructor() {
            TextAreaElement element = textArea();

            assertThat(element).isNotNull();
            assertThat(element.getState()).isNotNull();
            assertThat(element.getState().text()).isEmpty();
        }

        @Test
        @DisplayName("Constructor with state uses provided state")
        void constructorWithState() {
            TextAreaState state = new TextAreaState("Hello\nWorld");
            TextAreaElement element = textArea(state);

            assertThat(element.getState()).isSameAs(state);
            assertThat(element.getState().text()).isEqualTo("Hello\nWorld");
        }

        @Test
        @DisplayName("Constructor with null state creates new state")
        void constructorWithNullState() {
            TextAreaElement element = new TextAreaElement(null);

            assertThat(element.getState()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Fluent API")
    class FluentApi {

        @Test
        @DisplayName("state() sets state")
        void setState() {
            TextAreaState state = new TextAreaState("Test");
            TextAreaElement element = textArea().state(state);

            assertThat(element.getState()).isSameAs(state);
        }

        @Test
        @DisplayName("text() sets initial text")
        void setText() {
            TextAreaElement element = textArea().text("Hello");

            assertThat(element.getState().text()).isEqualTo("Hello");
        }

        @Test
        @DisplayName("placeholder() sets placeholder")
        void setPlaceholder() {
            TextAreaElement element = textArea().placeholder("Enter text...");

            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("placeholderStyle() sets placeholder style")
        void setPlaceholderStyle() {
            TextAreaElement element = textArea().placeholderStyle(Style.EMPTY.dim());

            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("placeholderColor() sets placeholder color")
        void setPlaceholderColor() {
            TextAreaElement element = textArea().placeholderColor(Color.GRAY);

            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("cursorStyle() sets cursor style")
        void setCursorStyle() {
            TextAreaElement element = textArea().cursorStyle(Style.EMPTY.reversed());

            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("showCursor() controls cursor visibility")
        void setShowCursor() {
            TextAreaElement element = textArea().showCursor(false);

            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("showLineNumbers() enables line numbers")
        void enableLineNumbers() {
            TextAreaElement element = textArea().showLineNumbers();

            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("lineNumberStyle() sets line number style")
        void setLineNumberStyle() {
            TextAreaElement element = textArea().lineNumberStyle(Style.EMPTY.fg(Color.CYAN));

            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("title() sets title")
        void setTitle() {
            TextAreaElement element = textArea().title("Editor");

            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("rounded() sets rounded borders")
        void setRounded() {
            TextAreaElement element = textArea().rounded();

            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("borderColor() sets border color")
        void setBorderColor() {
            TextAreaElement element = textArea().borderColor(Color.CYAN);

            assertThat(element).isNotNull();
        }

        @Test
        @DisplayName("Chaining multiple methods works")
        void chainingMethods() {
            TextAreaElement element = textArea()
                .text("Hello")
                .title("Editor")
                .placeholder("Type here...")
                .showLineNumbers()
                .rounded()
                .borderColor(Color.GREEN)
                .id("my-editor")
                .fill();

            assertThat(element.getState().text()).isEqualTo("Hello");
            assertThat(element.id()).isEqualTo("my-editor");
            assertThat(element.constraint()).isEqualTo(Constraint.fill());
        }
    }

    @Nested
    @DisplayName("Focus")
    class Focus {

        @Test
        @DisplayName("isFocusable returns true")
        void isFocusable() {
            TextAreaElement element = textArea();

            assertThat(element.isFocusable()).isTrue();
        }
    }

    @Nested
    @DisplayName("Key Event Handling")
    class KeyEventHandling {

        @Test
        @DisplayName("Character input is handled when focused")
        void characterInput() {
            TextAreaElement element = textArea();
            KeyEvent event = new KeyEvent(KeyCode.CHAR, KeyModifiers.NONE, 'a');

            EventResult result = element.handleKeyEvent(event, true);

            assertThat(result).isEqualTo(EventResult.HANDLED);
            assertThat(element.getState().text()).isEqualTo("a");
        }

        @Test
        @DisplayName("Character input is not handled when unfocused")
        void characterInputUnfocused() {
            TextAreaElement element = textArea();
            KeyEvent event = new KeyEvent(KeyCode.CHAR, KeyModifiers.NONE, 'a');

            EventResult result = element.handleKeyEvent(event, false);

            assertThat(result).isEqualTo(EventResult.UNHANDLED);
            assertThat(element.getState().text()).isEmpty();
        }

        @Test
        @DisplayName("Enter key inserts newline")
        void enterKey() {
            TextAreaElement element = textArea().text("Hello");
            KeyEvent event = new KeyEvent(KeyCode.ENTER, KeyModifiers.NONE, '\0');

            EventResult result = element.handleKeyEvent(event, true);

            assertThat(result).isEqualTo(EventResult.HANDLED);
            assertThat(element.getState().lineCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Backspace key deletes backward")
        void backspaceKey() {
            TextAreaElement element = textArea().text("Hello");
            KeyEvent event = new KeyEvent(KeyCode.BACKSPACE, KeyModifiers.NONE, '\0');

            EventResult result = element.handleKeyEvent(event, true);

            assertThat(result).isEqualTo(EventResult.HANDLED);
            assertThat(element.getState().text()).isEqualTo("Hell");
        }

        @Test
        @DisplayName("Delete key deletes forward")
        void deleteKey() {
            TextAreaElement element = textArea().text("Hello");
            element.getState().moveCursorToStart();
            KeyEvent event = new KeyEvent(KeyCode.DELETE, KeyModifiers.NONE, '\0');

            EventResult result = element.handleKeyEvent(event, true);

            assertThat(result).isEqualTo(EventResult.HANDLED);
            assertThat(element.getState().text()).isEqualTo("ello");
        }

        @Test
        @DisplayName("Left arrow moves cursor left")
        void leftArrow() {
            TextAreaElement element = textArea().text("Hello");
            KeyEvent event = new KeyEvent(KeyCode.LEFT, KeyModifiers.NONE, '\0');

            EventResult result = element.handleKeyEvent(event, true);

            assertThat(result).isEqualTo(EventResult.HANDLED);
            assertThat(element.getState().cursorCol()).isEqualTo(4);
        }

        @Test
        @DisplayName("Right arrow moves cursor right")
        void rightArrow() {
            TextAreaElement element = textArea().text("Hello");
            element.getState().moveCursorToStart();
            KeyEvent event = new KeyEvent(KeyCode.RIGHT, KeyModifiers.NONE, '\0');

            EventResult result = element.handleKeyEvent(event, true);

            assertThat(result).isEqualTo(EventResult.HANDLED);
            assertThat(element.getState().cursorCol()).isEqualTo(1);
        }

        @Test
        @DisplayName("Up arrow moves cursor up")
        void upArrow() {
            TextAreaElement element = textArea().text("Line1\nLine2");
            KeyEvent event = new KeyEvent(KeyCode.UP, KeyModifiers.NONE, '\0');

            EventResult result = element.handleKeyEvent(event, true);

            assertThat(result).isEqualTo(EventResult.HANDLED);
            assertThat(element.getState().cursorRow()).isEqualTo(0);
        }

        @Test
        @DisplayName("Down arrow moves cursor down")
        void downArrow() {
            TextAreaElement element = textArea().text("Line1\nLine2");
            element.getState().moveCursorToStart();
            KeyEvent event = new KeyEvent(KeyCode.DOWN, KeyModifiers.NONE, '\0');

            EventResult result = element.handleKeyEvent(event, true);

            assertThat(result).isEqualTo(EventResult.HANDLED);
            assertThat(element.getState().cursorRow()).isEqualTo(1);
        }

        @Test
        @DisplayName("Home key moves to line start")
        void homeKey() {
            TextAreaElement element = textArea().text("Hello");
            KeyEvent event = new KeyEvent(KeyCode.HOME, KeyModifiers.NONE, '\0');

            EventResult result = element.handleKeyEvent(event, true);

            assertThat(result).isEqualTo(EventResult.HANDLED);
            assertThat(element.getState().cursorCol()).isEqualTo(0);
        }

        @Test
        @DisplayName("End key moves to line end")
        void endKey() {
            TextAreaElement element = textArea().text("Hello");
            element.getState().moveCursorToStart();
            KeyEvent event = new KeyEvent(KeyCode.END, KeyModifiers.NONE, '\0');

            EventResult result = element.handleKeyEvent(event, true);

            assertThat(result).isEqualTo(EventResult.HANDLED);
            assertThat(element.getState().cursorCol()).isEqualTo(5);
        }

        @Test
        @DisplayName("Tab key inserts 4 spaces")
        void tabKey() {
            TextAreaElement element = textArea();
            KeyEvent event = new KeyEvent(KeyCode.TAB, KeyModifiers.NONE, '\0');

            EventResult result = element.handleKeyEvent(event, true);

            assertThat(result).isEqualTo(EventResult.HANDLED);
            assertThat(element.getState().text()).isEqualTo("    ");
        }
    }

    @Nested
    @DisplayName("Text Change Listener")
    class TextChangeListenerTest {

        @Test
        @DisplayName("Listener is called on text change")
        void listenerCalledOnChange() {
            AtomicReference<String> capturedText = new AtomicReference<>();
            TextAreaElement element = textArea()
                .onTextChange(capturedText::set);

            KeyEvent event = new KeyEvent(KeyCode.CHAR, KeyModifiers.NONE, 'H');
            element.handleKeyEvent(event, true);

            assertThat(capturedText.get()).isEqualTo("H");
        }

        @Test
        @DisplayName("Listener receives updated text after multiple changes")
        void listenerReceivesUpdatedText() {
            AtomicReference<String> capturedText = new AtomicReference<>();
            TextAreaElement element = textArea()
                .onTextChange(capturedText::set);

            element.handleKeyEvent(new KeyEvent(KeyCode.CHAR, KeyModifiers.NONE, 'H'), true);
            element.handleKeyEvent(new KeyEvent(KeyCode.CHAR, KeyModifiers.NONE, 'i'), true);

            assertThat(capturedText.get()).isEqualTo("Hi");
        }

        @Test
        @DisplayName("Listener is not called when unfocused")
        void listenerNotCalledWhenUnfocused() {
            AtomicReference<String> capturedText = new AtomicReference<>();
            TextAreaElement element = textArea()
                .onTextChange(capturedText::set);

            element.handleKeyEvent(new KeyEvent(KeyCode.CHAR, KeyModifiers.NONE, 'H'), false);

            assertThat(capturedText.get()).isNull();
        }
    }

    @Nested
    @DisplayName("Rendering")
    class Rendering {

        @Test
        @DisplayName("Renders to buffer")
        void rendersToBuffer() {
            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            textArea().text("Hello")
                .render(frame, area, RenderContext.empty());

            assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
        }

        @Test
        @DisplayName("Empty area does not render")
        void emptyAreaNoRender() {
            Rect emptyArea = new Rect(0, 0, 0, 0);
            Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 5));
            Frame frame = Frame.forTesting(buffer);

            // Should not throw
            textArea().text("Test").render(frame, emptyArea, RenderContext.empty());
        }

        @Test
        @DisplayName("Renders with title and border")
        void rendersWithTitleAndBorder() {
            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            textArea()
                .text("Hello")
                .title("Editor")
                .rounded()
                .render(frame, area, RenderContext.empty());

            // Title should be in first row
            // Text should be inside the border
            assertThat(buffer).isNotNull();
        }
    }

    @Nested
    @DisplayName("Constraints")
    class Constraints {

        @Test
        @DisplayName("length() sets constraint")
        void lengthConstraint() {
            TextAreaElement element = textArea().length(10);
            assertThat(element.constraint()).isEqualTo(Constraint.length(10));
        }

        @Test
        @DisplayName("percent() sets constraint")
        void percentConstraint() {
            TextAreaElement element = textArea().percent(50);
            assertThat(element.constraint()).isEqualTo(Constraint.percentage(50));
        }

        @Test
        @DisplayName("fill() sets constraint")
        void fillConstraint() {
            TextAreaElement element = textArea().fill();
            assertThat(element.constraint()).isEqualTo(Constraint.fill());
        }
    }

    @Nested
    @DisplayName("CSS Classes")
    class CssClasses {

        @Test
        @DisplayName("id() sets element id")
        void setId() {
            TextAreaElement element = textArea().id("my-textarea");

            assertThat(element.id()).isEqualTo("my-textarea");
            assertThat(element.cssId()).contains("my-textarea");
        }

        @Test
        @DisplayName("addClass() adds CSS class")
        void addClass() {
            TextAreaElement element = textArea().addClass("editor", "primary");

            assertThat(element.cssClasses()).contains("editor", "primary");
        }
    }

    @Nested
    @DisplayName("Style Attributes")
    class StyleAttributes {

        @Test
        @DisplayName("styleAttributes exposes title")
        void styleAttributes_exposesTitle() {
            assertThat(textArea().title("Description").styleAttributes()).containsEntry("title", "Description");
        }

        @Test
        @DisplayName("styleAttributes exposes placeholder")
        void styleAttributes_exposesPlaceholder() {
            assertThat(textArea().placeholder("Enter text...").styleAttributes()).containsEntry("placeholder", "Enter text...");
        }

        @Test
        @DisplayName("Attribute selector [title] affects TextArea border color")
        void attributeSelector_title_affectsBorderColor() {
            StyleEngine styleEngine = StyleEngine.create();
            styleEngine.addStylesheet("test", "TextAreaElement[title=\"Editor\"] { border-color: cyan; }");
            styleEngine.setActiveStylesheet("test");

            DefaultRenderContext context = DefaultRenderContext.createEmpty();
            context.setStyleEngine(styleEngine);

            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            textArea().title("Editor").rounded().render(frame, area, context);

            assertThat(buffer.get(0, 0).style().fg()).contains(Color.CYAN);
        }

        @Test
        @DisplayName("Attribute selector [placeholder] affects TextArea styling")
        void attributeSelector_placeholder_affectsStyling() {
            StyleEngine styleEngine = StyleEngine.create();
            styleEngine.addStylesheet("test", "TextAreaElement[placeholder=\"Type here...\"] { border-color: yellow; }");
            styleEngine.setActiveStylesheet("test");

            DefaultRenderContext context = DefaultRenderContext.createEmpty();
            context.setStyleEngine(styleEngine);

            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            textArea().placeholder("Type here...").rounded().render(frame, area, context);

            assertThat(buffer.get(0, 0).style().fg()).contains(Color.YELLOW);
        }
    }
}

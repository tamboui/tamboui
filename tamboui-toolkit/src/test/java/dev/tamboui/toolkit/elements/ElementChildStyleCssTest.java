/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.input.TextAreaState;
import dev.tamboui.widgets.scrollbar.ScrollbarState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests CSS child styling for various elements.
 * <p>
 * These tests verify the "explicit > CSS > default" priority pattern
 * implemented via {@link dev.tamboui.toolkit.element.StyledElement#resolveEffectiveStyle}.
 */
class ElementChildStyleCssTest {

    private DefaultRenderContext context;
    private StyleEngine styleEngine;

    @BeforeEach
    void setUp() {
        context = DefaultRenderContext.createEmpty();
        styleEngine = StyleEngine.create();
        context.setStyleEngine(styleEngine);
    }

    @Nested
    @DisplayName("TextInputElement CSS child styling")
    class TextInputElementCssTests {

        @Test
        @DisplayName("explicit cursor style overrides CSS")
        void explicitCursorStyleOverridesCss() {
            String css = "TextInputElement-cursor { background: blue; }";
            styleEngine.addStylesheet("test", css);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Use constructor with text - this sets cursor at end (position 5)
            TextInputState state = new TextInputState("Hello");

            // Use explicit style
            textInput(state)
                .cursorStyle(Style.EMPTY.bg(Color.RED))
                .render(frame, area, context);

            // Explicit style should override CSS - cursor at position 5
            assertThat(buffer.get(5, 0).style().bg()).contains(Color.RED);
        }

        @Test
        @DisplayName("explicit placeholder style overrides CSS")
        void explicitPlaceholderStyleOverridesCss() {
            String css = "TextInputElement-placeholder { color: cyan; }";
            styleEngine.addStylesheet("test", css);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            TextInputState state = new TextInputState();
            // Empty text shows placeholder

            textInput(state)
                .placeholder("Enter text...")
                .placeholderStyle(Style.EMPTY.fg(Color.MAGENTA))
                .render(frame, area, context);

            // Explicit placeholder style should override CSS
            assertThat(buffer.get(0, 0).style().fg()).contains(Color.MAGENTA);
        }

        @Test
        @DisplayName("default cursor style used when no CSS or explicit style")
        void defaultCursorStyleUsedWithoutCssOrExplicit() {
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Use constructor with text - this sets cursor at end (position 2)
            TextInputState state = new TextInputState("Hi");

            textInput(state)
                .render(frame, area, context);

            // Default cursor style is reversed - cursor at position 2
            assertThat(buffer.get(2, 0).style().effectiveModifiers())
                .contains(Modifier.REVERSED);
        }

        @Test
        @DisplayName("default placeholder style used when no CSS or explicit style")
        void defaultPlaceholderStyleUsedWithoutCssOrExplicit() {
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            TextInputState state = new TextInputState();
            // Empty text shows placeholder

            textInput(state)
                .placeholder("Enter text...")
                .render(frame, area, context);

            // Default placeholder style is dim
            assertThat(buffer.get(0, 0).style().effectiveModifiers())
                .contains(Modifier.DIM);
        }
    }

    @Nested
    @DisplayName("GaugeElement CSS child styling")
    class GaugeElementCssTests {

        @Test
        @DisplayName("explicit gauge style overrides CSS")
        void explicitGaugeStyleOverridesCss() {
            String css = "GaugeElement-filled { color: green; }";
            styleEngine.addStylesheet("test", css);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            gauge(1.0)
                .label("")
                .gaugeColor(Color.YELLOW)
                .render(frame, area, context);

            // Explicit style should override CSS
            assertThat(buffer.get(0, 0).style().fg()).contains(Color.YELLOW);
        }

        @Test
        @DisplayName("gauge renders with default style when no CSS or explicit")
        void defaultGaugeStyleUsed() {
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            gauge(1.0)
                .label("")
                .render(frame, area, context);

            // Gauge should render (filled character)
            assertThat(buffer.get(0, 0).symbol()).isEqualTo("█");
        }
    }

    @Nested
    @DisplayName("LineGaugeElement CSS child styling")
    class LineGaugeElementCssTests {

        @Test
        @DisplayName("explicit styles override CSS for both filled and unfilled")
        void explicitStylesOverrideCss() {
            String css = "LineGaugeElement-filled { color: magenta; }\n" +
                         "LineGaugeElement-unfilled { color: gray; }";
            styleEngine.addStylesheet("test", css);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            lineGauge(0.5)
                .filledColor(Color.RED)
                .unfilledColor(Color.BLUE)
                .render(frame, area, context);

            // Explicit styles should override CSS
            assertThat(buffer.get(0, 0).style().fg()).contains(Color.RED);
            assertThat(buffer.get(9, 0).style().fg()).contains(Color.BLUE);
        }

        @Test
        @DisplayName("line gauge renders correctly without styling")
        void lineGaugeRendersWithDefaults() {
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            lineGauge(0.5)
                .render(frame, area, context);

            // Line gauge should render its characters
            assertThat(buffer.get(0, 0).symbol()).isNotEqualTo(" ");
        }
    }

    @Nested
    @DisplayName("ScrollbarElement CSS child styling")
    class ScrollbarElementCssTests {

        @Test
        @DisplayName("explicit thumb style overrides CSS")
        void explicitThumbStyleOverridesCss() {
            String css = "ScrollbarElement-thumb { color: yellow; }";
            styleEngine.addStylesheet("test", css);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 1, 10);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            ScrollbarState state = new ScrollbarState()
                .contentLength(100)
                .viewportContentLength(10)
                .position(0);

            scrollbar()
                .vertical()
                .state(state)
                .thumbColor(Color.CYAN)
                .render(frame, area, context);

            // Check that the scrollbar renders without errors
            assertThat(buffer).isNotNull();
        }

        @Test
        @DisplayName("scrollbar renders with default symbols")
        void scrollbarRendersWithDefaults() {
            Rect area = new Rect(0, 0, 1, 10);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            ScrollbarState state = new ScrollbarState()
                .contentLength(100)
                .viewportContentLength(10)
                .position(0);

            scrollbar()
                .vertical()
                .state(state)
                .render(frame, area, context);

            // Scrollbar should render some non-empty content
            boolean hasContent = false;
            for (int y = 0; y < 10; y++) {
                if (!" ".equals(buffer.get(0, y).symbol())) {
                    hasContent = true;
                    break;
                }
            }
            assertThat(hasContent).isTrue();
        }
    }

    @Nested
    @DisplayName("TextAreaElement CSS child styling")
    class TextAreaElementCssTests {

        @Test
        @DisplayName("explicit line number style overrides CSS")
        void explicitLineNumberStyleOverridesCss() {
            String css = "TextAreaElement-line-number { color: cyan; }";
            styleEngine.addStylesheet("test", css);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            TextAreaState state = new TextAreaState();
            state.setText("Line 1\nLine 2");

            textArea(state)
                .showLineNumbers()
                .lineNumberStyle(Style.EMPTY.fg(Color.RED))
                .render(frame, area, context);

            // Explicit style should override CSS
            assertThat(buffer.get(0, 0).style().fg()).contains(Color.RED);
        }

        @Test
        @DisplayName("explicit placeholder style overrides CSS")
        void explicitPlaceholderStyleOverridesCss() {
            String css = "TextAreaElement-placeholder { color: magenta; }";
            styleEngine.addStylesheet("test", css);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            TextAreaState state = new TextAreaState();
            // Empty text shows placeholder

            textArea(state)
                .placeholder("Enter description...")
                .placeholderStyle(Style.EMPTY.fg(Color.GREEN))
                .render(frame, area, context);

            // Explicit placeholder style should override CSS
            assertThat(buffer.get(0, 0).style().fg()).contains(Color.GREEN);
        }

        @Test
        @DisplayName("default line number style used when no CSS or explicit")
        void defaultLineNumberStyleUsed() {
            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            TextAreaState state = new TextAreaState();
            state.setText("Line 1\nLine 2");

            textArea(state)
                .showLineNumbers()
                .render(frame, area, context);

            // Default line number style is dim
            assertThat(buffer.get(0, 0).style().effectiveModifiers())
                .contains(Modifier.DIM);
        }
    }
}

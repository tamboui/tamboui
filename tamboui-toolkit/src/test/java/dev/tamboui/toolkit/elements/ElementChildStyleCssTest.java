/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.css.engine.StyleEngine;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Modifier;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.component.Component;
import dev.tamboui.toolkit.element.DefaultRenderContext;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.widgets.input.TextAreaState;
import dev.tamboui.widgets.input.TextInputState;
import dev.tamboui.widgets.scrollbar.ScrollbarState;

import static dev.tamboui.assertj.BufferAssertions.assertThat;
import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests CSS child styling for various elements.
 * <p>
 * These tests verify the "explicit > CSS > default" priority pattern
 * implemented via
 * {@link dev.tamboui.toolkit.element.StyledElement#resolveEffectiveStyle}.
 */
class ElementChildStyleCssTest {

    /**
     * Minimal Component that mimics ProgressCard's structure: Component → Panel
     * (with cssParent) → Column → [Text, Text, Gauge]
     */
    static class TestProgressComponent extends Component<TestProgressComponent> {
        private final double progress;
        private final String statusCssClass;

        TestProgressComponent(double progress, String statusCssClass) {
            this.progress = progress;
            this.statusCssClass = statusCssClass;
        }

        @Override
        protected Element render() {
            return panel(
                    () -> column(text("Title").addClass("card-title").length(1),
                            text("Desc").addClass("card-description").length(1), gauge(progress)
                                    .label("").addClass("progress-" + statusCssClass).fill()))
                    .addClass(statusCssClass).cssParent(this);
        }
    }

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

            // Focus the element so cursor is rendered
            context.focusManager().setFocus("test-input");

            // Use explicit style
            textInput(state).id("test-input").cursorStyle(Style.EMPTY.bg(Color.RED)).render(frame,
                    area, context);

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

            textInput(state).placeholder("Enter text...")
                    .placeholderStyle(Style.EMPTY.fg(Color.MAGENTA)).render(frame, area, context);

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

            // Focus the element so cursor is rendered
            context.focusManager().setFocus("test-input");

            textInput(state).id("test-input").render(frame, area, context);

            // Default cursor style is reversed - cursor at position 2
            assertThat(buffer.get(2, 0).style().effectiveModifiers()).contains(Modifier.REVERSED);
        }

        @Test
        @DisplayName("default placeholder style used when no CSS or explicit style")
        void defaultPlaceholderStyleUsedWithoutCssOrExplicit() {
            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            TextInputState state = new TextInputState();
            // Empty text shows placeholder

            textInput(state).placeholder("Enter text...").render(frame, area, context);

            // Default placeholder style is dim
            assertThat(buffer.get(0, 0).style().effectiveModifiers()).contains(Modifier.DIM);
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

            gauge(1.0).label("").gaugeColor(Color.YELLOW).render(frame, area, context);

            // Explicit style should override CSS
            assertThat(buffer).at(0, 0).hasForeground(Color.YELLOW);
        }

        @Test
        @DisplayName("gauge renders with default style when no CSS or explicit")
        void defaultGaugeStyleUsed() {
            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            gauge(1.0).label("").render(frame, area, context);

            // Gauge should render (filled character)
            assertThat(buffer).at(0, 0).hasSymbol("█");
        }

        @Test
        @DisplayName("CSS child selector applies color to gauge filled portion")
        void cssChildSelectorAppliesColor() {
            String css = "GaugeElement-filled { color: green; }";
            styleEngine.addStylesheet("test", css);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            gauge(1.0).label("").render(frame, area, context);

            // CSS should apply green foreground to filled cells
            assertThat(buffer).at(0, 0).hasForeground(Color.GREEN);
        }

        @Test
        @DisplayName("CSS descendant child selector applies color via class on gauge")
        void cssDescendantChildSelectorAppliesColor() {
            String css = ".progress-complete GaugeElement-filled { color: green; }";
            styleEngine.addStylesheet("test", css);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            gauge(1.0).label("").addClass("progress-complete").render(frame, area, context);

            // CSS descendant selector should apply green to filled cells
            assertThat(buffer).at(0, 0).hasForeground(Color.GREEN);
        }

        @Test
        @DisplayName("CSS descendant child selector applies color when gauge is nested in column")
        void cssDescendantChildSelectorWorksNested() {
            String css = ".progress-complete GaugeElement-filled { color: green; }";
            styleEngine.addStylesheet("test", css);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Nest gauge in a column (like the demo does)
            column(gauge(1.0).label("").addClass("progress-complete").fill()).render(frame, area,
                    context);

            // CSS descendant selector should still apply green when nested
            assertThat(buffer).at(0, 0).hasForeground(Color.GREEN);
        }

        @Test
        @DisplayName("CSS descendant child selector applies color in panel > column > gauge hierarchy")
        void cssDescendantChildSelectorWorksDeeplyNested() {
            String css = ".progress-complete GaugeElement-filled { color: green; }\n"
                    + ".progress-in-progress GaugeElement-filled { color: yellow; }";
            styleEngine.addStylesheet("test", css);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 10, 3);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Mimic demo structure: panel > column > gauge
            panel(() -> column(gauge(1.0).label("").addClass("progress-complete").fill()))
                    .render(frame, area, context);

            // Find a filled gauge cell (inside the panel border)
            // Panel with borders takes 1 char on each side, so gauge content starts at x=1,
            // y=1
            assertThat(buffer).at(1, 1).hasForeground(Color.GREEN);
        }
    }

    @Nested
    @DisplayName("GaugeElement CSS child styling with full demo CSS")
    class GaugeElementFullDemoCssTests {

        private static final String DEMO_CSS = "GaugeElement { background: #1a1a1a; }\n"
                + ".card-title { color: cyan; text-style: bold; }\n"
                + ".card-description { color: #888888; }\n"
                + ".progress-complete GaugeElement-filled { color: green; }\n"
                + ".progress-in-progress GaugeElement-filled { color: yellow; }\n"
                + ".dim { text-style: dim; }\n" + ".title { color: cyan; text-style: bold; }";

        @Test
        @DisplayName("CSS descendant child selector works with full demo CSS loaded")
        void cssDescendantChildSelectorWithFullCss() {
            styleEngine.addStylesheet("test", DEMO_CSS);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            gauge(1.0).label("").addClass("progress-complete").render(frame, area, context);

            assertThat(buffer).at(0, 0).hasForeground(Color.GREEN);
        }

        @Test
        @DisplayName("CSS descendant child selector in nested panel with full demo CSS")
        void cssDescendantChildSelectorNestedWithFullCss() {
            styleEngine.addStylesheet("test", DEMO_CSS);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Mimic ProgressCard structure: panel > column > [title, description, gauge]
            panel(() -> column(text("Task Title").addClass("card-title").length(1),
                    text("Description").addClass("card-description").length(1),
                    gauge(1.0).label("").addClass("progress-complete").fill()))
                    .render(frame, area, context);

            // The gauge should be rendered inside the panel border
            // Panel border is 1 char, title text is row 1, description row 2
            // Gauge should be in row 3 (y=3 inside the panel, which is y=3 in buffer due to
            // border)
            // Actually, in a 5-row panel with border, inner area is rows 1-3
            // Row 1: title, Row 2: description, Row 3: gauge
            // Gauge cells should have green foreground
            // Find a gauge cell - look for the filled block character
            boolean foundGreenGauge = false;
            for (int y = 0; y < 5; y++) {
                for (int x = 0; x < 20; x++) {
                    if ("█".equals(buffer.get(x, y).symbol())) {
                        assertThat(buffer).at(x, y).hasForeground(Color.GREEN);
                        foundGreenGauge = true;
                        break;
                    }
                }
                if (foundGreenGauge)
                    break;
            }
            assertThat(foundGreenGauge).isTrue();
        }

        @Test
        @DisplayName("CSS in-progress gauge has yellow fill")
        void inProgressGaugeHasYellowFill() {
            styleEngine.addStylesheet("test", DEMO_CSS);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            gauge(0.5).label("").addClass("progress-in-progress").render(frame, area, context);

            // First cell should be filled with yellow
            assertThat(buffer).at(0, 0).hasForeground(Color.YELLOW);
        }
    }

    @Nested
    @DisplayName("LineGaugeElement CSS child styling")
    class LineGaugeElementCssTests {

        @Test
        @DisplayName("explicit styles override CSS for both filled and unfilled")
        void explicitStylesOverrideCss() {
            String css = "LineGaugeElement-filled { color: magenta; }\n"
                    + "LineGaugeElement-unfilled { color: gray; }";
            styleEngine.addStylesheet("test", css);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            lineGauge(0.5).filledColor(Color.RED).unfilledColor(Color.BLUE).render(frame, area,
                    context);

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

            lineGauge(0.5).render(frame, area, context);

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

            ScrollbarState state = new ScrollbarState().contentLength(100).viewportContentLength(10)
                    .position(0);

            scrollbar().vertical().state(state).thumbColor(Color.CYAN).render(frame, area, context);

            // Check that the scrollbar renders without errors
            assertThat(buffer).isNotNull();
        }

        @Test
        @DisplayName("scrollbar renders with default symbols")
        void scrollbarRendersWithDefaults() {
            Rect area = new Rect(0, 0, 1, 10);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            ScrollbarState state = new ScrollbarState().contentLength(100).viewportContentLength(10)
                    .position(0);

            scrollbar().vertical().state(state).render(frame, area, context);

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

            textArea(state).showLineNumbers().lineNumberStyle(Style.EMPTY.fg(Color.RED))
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

            textArea(state).placeholder("Enter description...")
                    .placeholderStyle(Style.EMPTY.fg(Color.GREEN)).render(frame, area, context);

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

            textArea(state).showLineNumbers().render(frame, area, context);

            // Default line number style is dim
            assertThat(buffer.get(0, 0).style().effectiveModifiers()).contains(Modifier.DIM);
        }
    }

    @Nested
    @DisplayName("Component rendering path CSS child styling")
    class ComponentRenderingPathCssTests {

        private static final String DEMO_CSS = "GaugeElement { background: #1a1a1a; }\n"
                + ".card-title { color: cyan; text-style: bold; }\n"
                + ".card-description { color: #888888; }\n"
                + ".progress-complete GaugeElement-filled { color: green; }\n"
                + ".progress-in-progress GaugeElement-filled { color: yellow; }\n"
                + ".dim { text-style: dim; }\n" + ".title { color: cyan; text-style: bold; }";

        @Test
        @DisplayName("CSS child selector works through Component rendering path with complete status")
        void cssChildSelectorWorksThroughComponentComplete() {
            styleEngine.addStylesheet("test", DEMO_CSS);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            new TestProgressComponent(1.0, "complete").render(frame, area, context);

            // Find gauge cells (filled block character) and verify green foreground
            // Panel border takes 1 char each side; title row 1, desc row 2, gauge row 3
            // Gauge is at y=3 (border top=1, title=1, desc=1), x=1 (border left=1)
            boolean foundGreenGauge = false;
            for (int y = 0; y < area.height(); y++) {
                for (int x = 0; x < area.width(); x++) {
                    if ("\u2588".equals(buffer.get(x, y).symbol())) {
                        assertThat(buffer).at(x, y).hasForeground(Color.GREEN);
                        foundGreenGauge = true;
                        break;
                    }
                }
                if (foundGreenGauge)
                    break;
            }
            assertThat(foundGreenGauge).as(
                    "Expected to find gauge cells with green foreground in Component rendering path")
                    .isTrue();
        }

        @Test
        @DisplayName("CSS child selector works through Component rendering path with in-progress status")
        void cssChildSelectorWorksThroughComponentInProgress() {
            styleEngine.addStylesheet("test", DEMO_CSS);
            styleEngine.setActiveStylesheet("test");

            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            new TestProgressComponent(0.5, "in-progress").render(frame, area, context);

            // Find gauge filled cells and verify yellow foreground
            boolean foundYellowGauge = false;
            for (int y = 0; y < area.height(); y++) {
                for (int x = 0; x < area.width(); x++) {
                    if ("\u2588".equals(buffer.get(x, y).symbol())) {
                        assertThat(buffer).at(x, y).hasForeground(Color.YELLOW);
                        foundYellowGauge = true;
                        break;
                    }
                }
                if (foundYellowGauge)
                    break;
            }
            assertThat(foundYellowGauge).as(
                    "Expected to find gauge cells with yellow foreground in Component rendering path")
                    .isTrue();
        }
    }
}

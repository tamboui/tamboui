/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.elements;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Constraint;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.Color;
import dev.tamboui.style.Style;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.element.RenderContext;
import dev.tamboui.widget.Widget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static dev.tamboui.toolkit.Toolkit.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for GenericWidgetElement.
 */
class GenericWidgetElementTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("GenericWidgetElement.of() creates element")
        void ofCreatesElement() {
            Widget testWidget = (area, buffer) -> buffer.setString(0, 0, "Test", Style.EMPTY);

            GenericWidgetElement<?> element = GenericWidgetElement.of(testWidget);

            assertThat(element).isNotNull();
            assertThat(element.widget()).isSameAs(testWidget);
        }

        @Test
        @DisplayName("GenericWidgetElement.widget() creates element (alias)")
        void widgetCreatesElement() {
            Widget testWidget = (area, buffer) -> buffer.setString(0, 0, "Test", Style.EMPTY);

            GenericWidgetElement<?> element = GenericWidgetElement.widget(testWidget);

            assertThat(element).isNotNull();
            assertThat(element.widget()).isSameAs(testWidget);
        }

        @Test
        @DisplayName("Toolkit.widget() creates element")
        void toolkitWidgetCreatesElement() {
            Widget testWidget = (area, buffer) -> buffer.setString(0, 0, "Test", Style.EMPTY);

            GenericWidgetElement<?> element = widget(testWidget);

            assertThat(element).isNotNull();
            assertThat(element.widget()).isSameAs(testWidget);
        }

        @Test
        @DisplayName("of() throws on null widget")
        void ofThrowsOnNull() {
            assertThatThrownBy(() -> GenericWidgetElement.of(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Widget cannot be null");
        }

        @Test
        @DisplayName("widget() throws on null widget")
        void widgetThrowsOnNull() {
            assertThatThrownBy(() -> GenericWidgetElement.widget(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Widget cannot be null");
        }
    }

    @Nested
    @DisplayName("Fluent API")
    class FluentApiTests {

        @Test
        @DisplayName("Fluent API chains correctly")
        void fluentApiChaining() {
            Widget testWidget = (area, buffer) -> {};

            GenericWidgetElement<?> element = widget(testWidget)
                    .bold()
                    .italic()
                    .fg(Color.CYAN)
                    .bg(Color.BLACK)
                    .id("my-element")
                    .addClass("custom-class")
                    .fill();

            assertThat(element).isInstanceOf(GenericWidgetElement.class);
            assertThat(element.id()).isEqualTo("my-element");
            assertThat(element.cssClasses()).contains("custom-class");
            assertThat(element.constraint()).isEqualTo(Constraint.fill());
        }

        @Test
        @DisplayName("Constraint methods work")
        void constraintMethods() {
            Widget testWidget = (area, buffer) -> {};

            assertThat(widget(testWidget).length(10).constraint())
                    .isEqualTo(Constraint.length(10));
            assertThat(widget(testWidget).percent(50).constraint())
                    .isEqualTo(Constraint.percentage(50));
            assertThat(widget(testWidget).fill().constraint())
                    .isEqualTo(Constraint.fill());
            assertThat(widget(testWidget).fill(2).constraint())
                    .isEqualTo(Constraint.fill(2));
            assertThat(widget(testWidget).min(5).constraint())
                    .isEqualTo(Constraint.min(5));
            assertThat(widget(testWidget).max(20).constraint())
                    .isEqualTo(Constraint.max(20));
        }

        @Test
        @DisplayName("Focusable and event handlers work")
        void focusableAndEventHandlers() {
            Widget testWidget = (area, buffer) -> {};

            GenericWidgetElement<?> element = widget(testWidget)
                    .focusable()
                    .onKeyEvent(event -> dev.tamboui.toolkit.event.EventResult.HANDLED)
                    .onMouseEvent(event -> dev.tamboui.toolkit.event.EventResult.HANDLED);

            assertThat(element.isFocusable()).isTrue();
            assertThat(element.keyEventHandler()).isNotNull();
            assertThat(element.mouseEventHandler()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Rendering")
    class RenderingTests {

        @Test
        @DisplayName("Renders wrapped widget to buffer")
        void rendersWidgetToBuffer() {
            Widget testWidget = (area, buffer) -> {
                buffer.setString(area.x(), area.y(), "Hello", Style.EMPTY);
            };

            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            widget(testWidget).render(frame, area, RenderContext.empty());

            assertThat(buffer.get(0, 0).symbol()).isEqualTo("H");
            assertThat(buffer.get(1, 0).symbol()).isEqualTo("e");
            assertThat(buffer.get(2, 0).symbol()).isEqualTo("l");
            assertThat(buffer.get(3, 0).symbol()).isEqualTo("l");
            assertThat(buffer.get(4, 0).symbol()).isEqualTo("o");
        }

        @Test
        @DisplayName("Widget receives correct area")
        void widgetReceivesCorrectArea() {
            Rect expectedArea = new Rect(5, 3, 10, 5);
            Rect[] capturedArea = new Rect[1];

            Widget testWidget = (area, buffer) -> {
                capturedArea[0] = area;
            };

            Buffer buffer = Buffer.empty(new Rect(0, 0, 20, 10));
            Frame frame = Frame.forTesting(buffer);

            widget(testWidget).render(frame, expectedArea, RenderContext.empty());

            assertThat(capturedArea[0]).isEqualTo(expectedArea);
        }

        @Test
        @DisplayName("Empty area does not render")
        void emptyAreaNoRender() {
            boolean[] rendered = {false};
            Widget testWidget = (area, buffer) -> rendered[0] = true;

            Rect emptyArea = new Rect(0, 0, 0, 0);
            Buffer buffer = Buffer.empty(new Rect(0, 0, 10, 1));
            Frame frame = Frame.forTesting(buffer);

            widget(testWidget).render(frame, emptyArea, RenderContext.empty());

            assertThat(rendered[0]).isFalse();
        }

        @Test
        @DisplayName("Widget with custom styling renders")
        void widgetWithCustomStyling() {
            Widget testWidget = (area, buffer) -> {
                buffer.setString(area.x(), area.y(), "X", Style.EMPTY.fg(Color.GREEN));
            };

            Rect area = new Rect(0, 0, 10, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            // Element styling doesn't override widget's internal styling
            widget(testWidget)
                    .fg(Color.RED)  // This won't affect widget's internal rendering
                    .render(frame, area, RenderContext.empty());

            // Widget renders with its own style (GREEN)
            assertThat(buffer.get(0, 0).symbol()).isEqualTo("X");
            assertThat(buffer.get(0, 0).style().fg()).contains(Color.GREEN);
        }
    }

    @Nested
    @DisplayName("Layout integration")
    class LayoutIntegrationTests {

        @Test
        @DisplayName("Works inside row")
        void worksInsideRow() {
            Widget leftWidget = (area, buffer) -> buffer.setString(area.x(), area.y(), "L", Style.EMPTY);
            Widget rightWidget = (area, buffer) -> buffer.setString(area.x(), area.y(), "R", Style.EMPTY);

            Rect area = new Rect(0, 0, 20, 1);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            row(
                    widget(leftWidget).fill(),
                    widget(rightWidget).fill()
            ).render(frame, area, RenderContext.empty());

            assertThat(buffer.get(0, 0).symbol()).isEqualTo("L");
            assertThat(buffer.get(10, 0).symbol()).isEqualTo("R");
        }

        @Test
        @DisplayName("Works inside column")
        void worksInsideColumn() {
            Widget topWidget = (area, buffer) -> buffer.setString(area.x(), area.y(), "T", Style.EMPTY);
            Widget bottomWidget = (area, buffer) -> buffer.setString(area.x(), area.y(), "B", Style.EMPTY);

            Rect area = new Rect(0, 0, 10, 4);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            column(
                    widget(topWidget).length(2),
                    widget(bottomWidget).length(2)
            ).render(frame, area, RenderContext.empty());

            assertThat(buffer.get(0, 0).symbol()).isEqualTo("T");
            assertThat(buffer.get(0, 2).symbol()).isEqualTo("B");
        }

        @Test
        @DisplayName("Works inside panel")
        void worksInsidePanel() {
            Widget contentWidget = (area, buffer) ->
                    buffer.setString(area.x(), area.y(), "Content", Style.EMPTY);

            Rect area = new Rect(0, 0, 20, 5);
            Buffer buffer = Buffer.empty(area);
            Frame frame = Frame.forTesting(buffer);

            panel("Title", widget(contentWidget))
                    .render(frame, area, RenderContext.empty());

            // Content should be inside the panel (after border)
            assertThat(buffer.get(1, 1).symbol()).isEqualTo("C");
        }

        @Test
        @DisplayName("Default constraint is null")
        void defaultConstraintIsNull() {
            Widget testWidget = (area, buffer) -> {};

            assertThat(widget(testWidget).constraint()).isNull();
        }
    }

    @Nested
    @DisplayName("CSS support")
    class CssSupportTests {

        @Test
        @DisplayName("Style type is GenericWidgetElement")
        void styleType() {
            Widget testWidget = (area, buffer) -> {};

            GenericWidgetElement<?> element = widget(testWidget);

            assertThat(element.styleType()).isEqualTo("GenericWidgetElement");
        }

        @Test
        @DisplayName("CSS classes can be added")
        void cssClasses() {
            Widget testWidget = (area, buffer) -> {};

            GenericWidgetElement<?> element = widget(testWidget)
                    .addClass("primary", "highlighted");

            assertThat(element.cssClasses()).containsExactlyInAnyOrder("primary", "highlighted");
        }

        @Test
        @DisplayName("CSS ID can be set")
        void cssId() {
            Widget testWidget = (area, buffer) -> {};

            GenericWidgetElement<?> element = widget(testWidget).id("my-widget");

            assertThat(element.cssId()).contains("my-widget");
        }

        @Test
        @DisplayName("Style attributes can be set")
        void styleAttributes() {
            Widget testWidget = (area, buffer) -> {};

            GenericWidgetElement<?> element = widget(testWidget)
                    .attr("data-type", "custom")
                    .attr("data-index", "5");

            assertThat(element.styleAttributes())
                    .containsEntry("data-type", "custom")
                    .containsEntry("data-index", "5");
        }
    }
}
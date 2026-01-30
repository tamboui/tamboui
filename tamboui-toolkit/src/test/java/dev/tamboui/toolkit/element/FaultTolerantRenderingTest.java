/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.element;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.style.StyledAreaInfo;
import dev.tamboui.style.StyledAreaRegistry;
import dev.tamboui.style.Tags;
import dev.tamboui.terminal.Frame;
import dev.tamboui.toolkit.event.EventRouter;
import dev.tamboui.toolkit.focus.FocusManager;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for fault-tolerant rendering in DefaultRenderContext.
 */
class FaultTolerantRenderingTest {

    private DefaultRenderContext context;
    private Frame frame;
    private Rect area;

    @BeforeEach
    void setUp() {
        FocusManager focusManager = new FocusManager();
        ElementRegistry registry = new ElementRegistry();
        context = new DefaultRenderContext(focusManager, new EventRouter(focusManager, registry));
        Buffer buffer = Buffer.empty(new Rect(0, 0, 40, 10));
        frame = Frame.forTesting(buffer);
        area = new Rect(0, 0, 40, 10);
    }

    @Nested
    @DisplayName("When fault-tolerant mode is disabled")
    class FaultTolerantDisabled {

        @BeforeEach
        void setUp() {
            context.setFaultTolerant(false);
        }

        @Test
        @DisplayName("exceptions propagate from renderChild")
        void exceptionsPropagateFromRenderChild() {
            Element faultyElement = createFaultyElement();

            assertThatThrownBy(() -> context.renderChild(faultyElement, frame, area))
                    .isInstanceOf(RuntimeException.class).hasMessage("Intentional render failure");
        }

        @Test
        @DisplayName("successful renders work normally")
        void successfulRendersWorkNormally() {
            AtomicBoolean rendered = new AtomicBoolean(false);
            Element element = createSuccessfulElement(rendered);

            context.renderChild(element, frame, area);

            assertThat(rendered.get()).isTrue();
        }
    }

    @Nested
    @DisplayName("When fault-tolerant mode is enabled")
    class FaultTolerantEnabled {

        @BeforeEach
        void setUp() {
            context.setFaultTolerant(true);
        }

        @Test
        @DisplayName("exceptions are caught and don't propagate")
        void exceptionsAreCaughtAndDontPropagate() {
            Element faultyElement = createFaultyElement();

            // Should not throw
            assertThatCode(() -> context.renderChild(faultyElement, frame, area))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("error placeholder is rendered for faulty elements")
        void errorPlaceholderIsRenderedForFaultyElements() {
            Element faultyElement = createFaultyElement();

            context.renderChild(faultyElement, frame, area);

            // Check that something was rendered (error placeholder renders a border)
            Buffer buffer = frame.buffer();
            // The error placeholder should have rendered something
            // Check for the "Error" title or border characters
            String topLeft = buffer.get(0, 0).symbol();
            assertThat(topLeft).isIn("┌", "╭", "+", "─");
        }

        @Test
        @DisplayName("error placeholder styled output is not attributed to the failed child context key")
        void errorPlaceholderStyledOutputNotAttributedToFailedChild() {
            StyledAreaRegistry styledAreaRegistry = StyledAreaRegistry.create();
            frame.setStyledAreaRegistry(styledAreaRegistry);

            Element faultyElement = createFaultyElement();
            context.renderChild(faultyElement, frame, area);

            assertThat(styledAreaRegistry.size()).isGreaterThan(0);

            for (StyledAreaInfo info : styledAreaRegistry.all()) {
                Tags tags = info.tags();
                if (tags.contains("error")) {
                    assertThat(info.contextKey()).isNotEqualTo(faultyElement.id());
                }
            }
        }

        @Test
        @DisplayName("successful renders work normally")
        void successfulRendersWorkNormally() {
            AtomicBoolean rendered = new AtomicBoolean(false);
            Element element = createSuccessfulElement(rendered);

            context.renderChild(element, frame, area);

            assertThat(rendered.get()).isTrue();
        }

        @Test
        @DisplayName("isFaultTolerant returns true")
        void isFaultTolerantReturnsTrue() {
            assertThat(context.isFaultTolerant()).isTrue();
        }
    }

    @Nested
    @DisplayName("Default behavior")
    class DefaultBehavior {

        @Test
        @DisplayName("fault-tolerant is disabled by default")
        void faultTolerantDisabledByDefault() {
            FocusManager fm = new FocusManager();
            ElementRegistry registry = new ElementRegistry();
            DefaultRenderContext newContext = new DefaultRenderContext(fm,
                    new EventRouter(fm, registry));

            assertThat(newContext.isFaultTolerant()).isFalse();
        }
    }

    private Element createFaultyElement() {
        return new Element() {
            @Override
            public void render(Frame frame, Rect area, RenderContext context) {
                throw new RuntimeException("Intentional render failure");
            }

            @Override
            public String id() {
                return "faulty-element";
            }
        };
    }

    private Element createSuccessfulElement(AtomicBoolean rendered) {
        return new Element() {
            @Override
            public void render(Frame frame, Rect area, RenderContext context) {
                rendered.set(true);
            }

            @Override
            public String id() {
                return "successful-element";
            }
        };
    }
}

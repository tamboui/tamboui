/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import dev.tamboui.layout.Rect;
import dev.tamboui.toolkit.test.TestElement;
import dev.tamboui.toolkit.test.TestToolkitApp;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EventRouter} covering key routing, mouse routing,
 * global handlers, and drag handling.
 */
@DisplayName("EventRouter")
class EventRouterTest {

    private TestToolkitApp app;

    @BeforeEach
    void setUp() {
        app = TestToolkitApp.create();
    }

    @Nested
    @DisplayName("Key event routing")
    class KeyEventRouting {

        @Test
        @DisplayName("routes key events to focused element first")
        void routesToFocusedElementFirst() {
            TestElement input1 = TestElement.create("input1").focusable().handlesAllKeys();
            TestElement input2 = TestElement.create("input2").focusable().handlesAllKeys();

            app.withElement(input1, new Rect(0, 0, 10, 1))
               .withElement(input2, new Rect(0, 1, 10, 1))
               .withFocus("input1");

            app.send(KeyEvent.ofChar('a'))
               .assertHandled()
               .assertFocusIs("input1");

            assertThat(input1.keyEvents()).hasSize(1);
            assertThat(input1.receivedFocusedKeyEvent()).isTrue();
            assertThat(input2.keyEvents()).isEmpty();
        }

        @Test
        @DisplayName("passes key events to unfocused elements if focused element doesn't handle")
        void passesToUnfocusedIfNotHandled() {
            TestElement input1 = TestElement.create("input1").focusable(); // Does NOT handle keys
            TestElement input2 = TestElement.create("input2").focusable().handlesAllKeys();

            app.withElement(input1, new Rect(0, 0, 10, 1))
               .withElement(input2, new Rect(0, 1, 10, 1))
               .withFocus("input1");

            app.send(KeyEvent.ofChar('a'))
               .assertUnhandled();  // Neither handles in unfocused mode

            assertThat(input1.keyEvents()).hasSize(1);  // Got event (focused=true)
            assertThat(input2.keyEvents()).hasSize(1);  // Got event (focused=false)
        }

        @Test
        @DisplayName("focused element receives focused=true, others receive focused=false")
        void focusedStateIsCorrect() {
            TestElement input1 = TestElement.create("input1").focusable();
            TestElement input2 = TestElement.create("input2").focusable();

            app.withElement(input1, new Rect(0, 0, 10, 1))
               .withElement(input2, new Rect(0, 1, 10, 1))
               .withFocus("input1");

            app.send(KeyEvent.ofChar('x'));

            assertThat(input1.focusedStates()).containsExactly(true);
            assertThat(input2.focusedStates()).containsExactly(false);
        }
    }

    @Nested
    @DisplayName("Mouse event routing")
    class MouseEventRouting {

        @Test
        @DisplayName("routes mouse press to element at position")
        void routesToElementAtPosition() {
            TestElement input1 = TestElement.create("input1").focusable().handlesAllMouse();
            TestElement input2 = TestElement.create("input2").focusable().handlesAllMouse();

            app.withElement(input1, new Rect(0, 0, 10, 1))
               .withElement(input2, new Rect(0, 1, 10, 1));

            // Click on input2 (y=1)
            app.send(MouseEvent.press(MouseButton.LEFT, 5, 1))
               .assertHandled()
               .assertFocusIs("input2");

            assertThat(input1.mouseEvents()).isEmpty();
            assertThat(input2.mouseEvents()).hasSize(1);
        }

        @Test
        @DisplayName("click focuses element even without mouse handler")
        void clickFocusesElement() {
            TestElement input1 = TestElement.create("input1").focusable();

            app.withElement(input1, new Rect(0, 0, 10, 1));

            app.send(MouseEvent.press(MouseButton.LEFT, 5, 0))
               .assertHandled()
               .assertFocusIs("input1");
        }

        @Test
        @DisplayName("click outside all elements clears focus")
        void clickOutsideClearsFocus() {
            TestElement input1 = TestElement.create("input1").focusable();

            app.withElement(input1, new Rect(0, 0, 10, 1))
               .withFocus("input1");

            // Click outside (y=5, no element there)
            app.send(MouseEvent.press(MouseButton.LEFT, 5, 5))
               .assertNoFocus();
        }

        @Test
        @DisplayName("top element (last registered) receives events first in z-order")
        void zOrderRespected() {
            TestElement bottom = TestElement.create("bottom").focusable().handlesAllMouse();
            TestElement top = TestElement.create("top").focusable().handlesAllMouse();

            // Both elements at same position - top is registered last
            app.withElement(bottom, new Rect(0, 0, 10, 5))
               .withElement(top, new Rect(0, 0, 10, 5));

            app.send(MouseEvent.press(MouseButton.LEFT, 5, 2))
               .assertHandled()
               .assertFocusIs("top");

            // Top element gets the event, bottom doesn't
            assertThat(top.mouseEvents()).hasSize(1);
            assertThat(bottom.mouseEvents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Global event handlers")
    class GlobalHandlers {

        @Test
        @DisplayName("global handlers are called for non-key events before elements")
        void globalHandlersCalledFirst() {
            boolean[] handlerCalled = {false};

            app.withGlobalHandler(event -> {
                handlerCalled[0] = true;
                return EventResult.HANDLED;
            });

            TestElement input1 = TestElement.create("input1").focusable().handlesAllMouse();
            app.withElement(input1, new Rect(0, 0, 10, 1));

            app.send(MouseEvent.press(MouseButton.LEFT, 5, 0))
               .assertHandled();

            assertThat(handlerCalled[0]).isTrue();
            // Element doesn't receive event because global handler consumed it
            assertThat(input1.mouseEvents()).isEmpty();
        }

        @Test
        @DisplayName("global handlers are called after focused element for key events")
        void globalHandlersAfterFocusedForKeys() {
            int[] callOrder = {0};
            int[] globalOrder = {0};
            int[] elementOrder = {0};

            app.withGlobalHandler(event -> {
                globalOrder[0] = ++callOrder[0];
                return EventResult.UNHANDLED;
            });

            TestElement input1 = TestElement.create("input1").focusable()
                .onKeyEvent(event -> {
                    elementOrder[0] = ++callOrder[0];
                    return EventResult.UNHANDLED;
                });
            app.withElement(input1, new Rect(0, 0, 10, 1))
               .withFocus("input1");

            app.send(KeyEvent.ofChar('a'));

            // Element (focused) is called first, then global handler
            assertThat(elementOrder[0]).isEqualTo(1);
            assertThat(globalOrder[0]).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Element count and registration")
    class Registration {

        @Test
        @DisplayName("reports correct element count")
        void reportsElementCount() {
            TestElement e1 = TestElement.create("e1");
            TestElement e2 = TestElement.create("e2");
            TestElement e3 = TestElement.create("e3");

            app.withElement(e1, new Rect(0, 0, 10, 1))
               .withElement(e2, new Rect(0, 1, 10, 1))
               .withElement(e3, new Rect(0, 2, 10, 1));

            assertThat(app.eventRouter().elementCount()).isEqualTo(3);
        }
    }
}

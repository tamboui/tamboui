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
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for focus navigation (Tab/Shift+Tab, Escape, click-to-focus).
 */
@DisplayName("Focus Navigation")
class FocusNavigationTest {

    private TestToolkitApp app;

    @BeforeEach
    void setUp() {
        app = TestToolkitApp.create();
    }

    @Nested
    @DisplayName("Tab navigation")
    class TabNavigation {

        @Test
        @DisplayName("Tab moves focus to next element")
        void tabMovesToNext() {
            TestElement input1 = TestElement.create("input1").focusable();
            TestElement input2 = TestElement.create("input2").focusable();
            TestElement input3 = TestElement.create("input3").focusable();

            app.withElement(input1, new Rect(0, 0, 10, 1))
               .withElement(input2, new Rect(0, 1, 10, 1))
               .withElement(input3, new Rect(0, 2, 10, 1))
               .withFocus("input1");

            app.send(KeyEvent.ofKey(KeyCode.TAB))
               .assertHandled()
               .assertFocusMovedTo("input2");
        }

        @Test
        @DisplayName("Tab wraps from last to first element")
        void tabWrapsAround() {
            TestElement input1 = TestElement.create("input1").focusable();
            TestElement input2 = TestElement.create("input2").focusable();

            app.withElement(input1, new Rect(0, 0, 10, 1))
               .withElement(input2, new Rect(0, 1, 10, 1))
               .withFocus("input2");  // Start at last

            app.send(KeyEvent.ofKey(KeyCode.TAB))
               .assertHandled()
               .assertFocusMovedTo("input1");
        }

        @Test
        @DisplayName("Tab with single element keeps focus")
        void tabWithSingleElement() {
            TestElement input1 = TestElement.create("input1").focusable();

            app.withElement(input1, new Rect(0, 0, 10, 1))
               .withFocus("input1");

            app.send(KeyEvent.ofKey(KeyCode.TAB))
               .assertUnhandled()  // No change, returns unhandled
               .assertFocusIs("input1");
        }
    }

    @Nested
    @DisplayName("Shift+Tab navigation")
    class ShiftTabNavigation {

        @Test
        @DisplayName("Shift+Tab moves focus to previous element")
        void shiftTabMovesToPrevious() {
            TestElement input1 = TestElement.create("input1").focusable();
            TestElement input2 = TestElement.create("input2").focusable();
            TestElement input3 = TestElement.create("input3").focusable();

            app.withElement(input1, new Rect(0, 0, 10, 1))
               .withElement(input2, new Rect(0, 1, 10, 1))
               .withElement(input3, new Rect(0, 2, 10, 1))
               .withFocus("input2");

            app.send(KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT))
               .assertHandled()
               .assertFocusMovedTo("input1");
        }

        @Test
        @DisplayName("Shift+Tab wraps from first to last element")
        void shiftTabWrapsAround() {
            TestElement input1 = TestElement.create("input1").focusable();
            TestElement input2 = TestElement.create("input2").focusable();

            app.withElement(input1, new Rect(0, 0, 10, 1))
               .withElement(input2, new Rect(0, 1, 10, 1))
               .withFocus("input1");  // Start at first

            app.send(KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT))
               .assertHandled()
               .assertFocusMovedTo("input2");
        }
    }

    @Nested
    @DisplayName("Escape clears focus")
    class EscapeClearsFocus {

        @Test
        @DisplayName("Escape clears focus when no element handles it")
        void escapeClearsFocus() {
            TestElement input1 = TestElement.create("input1").focusable();

            app.withElement(input1, new Rect(0, 0, 10, 1))
               .withFocus("input1");

            app.send(KeyEvent.ofKey(KeyCode.ESCAPE))
               .assertHandled()
               .assertFocusCleared();
        }

        @Test
        @DisplayName("Escape does not clear focus if element handles it")
        void escapeRespectedIfHandled() {
            TestElement input1 = TestElement.create("input1").focusable()
                .onKeyEvent(event -> {
                    if (event.isKey(KeyCode.ESCAPE)) {
                        return EventResult.HANDLED;
                    }
                    return EventResult.UNHANDLED;
                });

            app.withElement(input1, new Rect(0, 0, 10, 1))
               .withFocus("input1");

            app.send(KeyEvent.ofKey(KeyCode.ESCAPE))
               .assertHandled()
               .assertFocusIs("input1");  // Focus not cleared because element handled it
        }
    }

    @Nested
    @DisplayName("Click-to-focus")
    class ClickToFocus {

        @Test
        @DisplayName("clicking focusable element focuses it")
        void clickFocuses() {
            TestElement input1 = TestElement.create("input1").focusable();
            TestElement input2 = TestElement.create("input2").focusable();

            app.withElement(input1, new Rect(0, 0, 10, 1))
               .withElement(input2, new Rect(0, 1, 10, 1))
               .withFocus("input1");

            app.send(MouseEvent.press(MouseButton.LEFT, 5, 1))  // Click on input2
               .assertHandled()
               .assertFocusMovedTo("input2");
        }

        @Test
        @DisplayName("clicking non-focusable element that handles click keeps focus unchanged")
        void clickNonFocusableWithHandlerKeepsFocus() {
            TestElement input1 = TestElement.create("input1").focusable();
            TestElement label = TestElement.create("label").handlesAllMouse();  // Handles click

            app.withElement(input1, new Rect(0, 0, 10, 1))
               .withElement(label, new Rect(0, 1, 10, 1))
               .withFocus("input1");

            app.send(MouseEvent.press(MouseButton.LEFT, 5, 1))  // Click on label
               .assertHandled()  // Handled by label
               .assertFocusIs("input1");  // Focus unchanged because label is not focusable
        }

        @Test
        @DisplayName("clicking outside all elements clears focus")
        void clickOutsideClearsFocus() {
            TestElement input1 = TestElement.create("input1").focusable();

            app.withElement(input1, new Rect(0, 0, 10, 1))
               .withFocus("input1");

            app.send(MouseEvent.press(MouseButton.LEFT, 5, 5))  // Click outside
               .assertNoFocus();
        }
    }

    @Nested
    @DisplayName("Focus order")
    class FocusOrder {

        @Test
        @DisplayName("focus order matches registration order")
        void focusOrderMatchesRegistration() {
            TestElement a = TestElement.create("a").focusable();
            TestElement b = TestElement.create("b").focusable();
            TestElement c = TestElement.create("c").focusable();

            app.withElement(a, new Rect(0, 0, 10, 1))
               .withElement(b, new Rect(0, 1, 10, 1))
               .withElement(c, new Rect(0, 2, 10, 1))
               .withFocus("a");

            // Tab through all elements
            assertThat(app.focusedId()).isEqualTo("a");

            app.send(KeyEvent.ofKey(KeyCode.TAB));
            assertThat(app.focusedId()).isEqualTo("b");

            app.send(KeyEvent.ofKey(KeyCode.TAB));
            assertThat(app.focusedId()).isEqualTo("c");

            app.send(KeyEvent.ofKey(KeyCode.TAB));
            assertThat(app.focusedId()).isEqualTo("a");  // Wrapped around
        }

        @Test
        @DisplayName("non-focusable elements are skipped in focus order")
        void nonFocusableSkipped() {
            TestElement a = TestElement.create("a").focusable();
            TestElement b = TestElement.create("b");  // Not focusable
            TestElement c = TestElement.create("c").focusable();

            app.withElement(a, new Rect(0, 0, 10, 1))
               .withElement(b, new Rect(0, 1, 10, 1))
               .withElement(c, new Rect(0, 2, 10, 1))
               .withFocus("a");

            // Tab from a should go directly to c (skipping b)
            app.send(KeyEvent.ofKey(KeyCode.TAB))
               .assertHandled()
               .assertFocusMovedTo("c");
        }
    }
}

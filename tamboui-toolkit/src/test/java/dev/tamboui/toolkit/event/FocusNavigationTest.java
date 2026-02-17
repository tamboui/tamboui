/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import dev.tamboui.layout.Rect;
import dev.tamboui.toolkit.app.EventRouterTestHarness;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests focus navigation using EventRouterTestHarness, TestElement, and EventRouterResult.
 */
class FocusNavigationTest {

    @Nested
    @DisplayName("Tab / Shift+Tab")
    class TabNavigation {

        @Test
        @DisplayName("Tab moves focus to next element")
        void tabMovesFocusNext() {
            TestElement a = new TestElement("a").focusable();
            TestElement b = new TestElement("b").focusable();
            TestElement c = new TestElement("c").focusable();

            EventRouterTestHarness harness = EventRouterTestHarness.create()
                    .withElement(a, new Rect(0, 0, 10, 1))
                    .withElement(b, new Rect(0, 1, 10, 1))
                    .withElement(c, new Rect(0, 2, 10, 1));

            harness.send(KeyEvent.ofKey(KeyCode.TAB))
                    .assertHandled()
                    .assertFocusMovedTo("b");

            harness.send(KeyEvent.ofKey(KeyCode.TAB))
                    .assertHandled()
                    .assertFocusMovedTo("c");

            harness.send(KeyEvent.ofKey(KeyCode.TAB))
                    .assertHandled()
                    .assertFocusMovedTo("a");
        }

        @Test
        @DisplayName("Shift+Tab moves focus to previous element")
        void shiftTabMovesFocusPrevious() {
            TestElement a = new TestElement("a").focusable();
            TestElement b = new TestElement("b").focusable();
            TestElement c = new TestElement("c").focusable();

            EventRouterTestHarness harness = EventRouterTestHarness.create()
                    .withElement(a, new Rect(0, 0, 10, 1))
                    .withElement(b, new Rect(0, 1, 10, 1))
                    .withElement(c, new Rect(0, 2, 10, 1))
                    .withFocus("c");

            harness.send(KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT))
                    .assertHandled()
                    .assertFocusMovedTo("b");

            harness.send(KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT))
                    .assertHandled()
                    .assertFocusMovedTo("a");

            harness.send(KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT))
                    .assertHandled()
                    .assertFocusMovedTo("c");
        }

        @Test
        @DisplayName("Tab with single focusable keeps focus")
        void tabSingleElementKeepsFocus() {
            TestElement a = new TestElement("a").focusable();

            EventRouterTestHarness harness = EventRouterTestHarness.create()
                    .withElement(a, new Rect(0, 0, 10, 1));

            harness.send(KeyEvent.ofKey(KeyCode.TAB))
                    .assertFocusIs("a");
        }
    }

    @Nested
    @DisplayName("Escape")
    class Escape {

        @Test
        @DisplayName("Escape clears focus")
        void escapeClearsFocus() {
            TestElement a = new TestElement("a").focusable();

            EventRouterTestHarness harness = EventRouterTestHarness.create()
                    .withElement(a, new Rect(0, 0, 10, 1));

            harness.send(KeyEvent.ofKey(KeyCode.ESCAPE))
                    .assertHandled()
                    .assertNoFocus();
        }

        @Test
        @DisplayName("Escape when nothing focused is unhandled")
        void escapeWhenNoFocusUnhandled() {
            TestElement a = new TestElement("a").focusable();

            EventRouterTestHarness harness = EventRouterTestHarness.create()
                    .withElement(a, new Rect(0, 0, 10, 1))
                    .withFocus(null);

            harness.send(KeyEvent.ofKey(KeyCode.ESCAPE))
                    .assertUnhandled();
        }
    }

    @Nested
    @DisplayName("Click to focus")
    class ClickToFocus {

        @Test
        @DisplayName("Click on focusable element sets focus")
        void clickSetsFocus() {
            TestElement a = new TestElement("a").focusable();
            TestElement b = new TestElement("b").focusable();

            EventRouterTestHarness harness = EventRouterTestHarness.create()
                    .withElement(a, new Rect(0, 0, 10, 1))
                    .withElement(b, new Rect(0, 1, 10, 1));

            // Click in b's area (center: 5, 1)
            harness.send(MouseEvent.press(MouseButton.LEFT, 5, 1))
                    .assertFocusMovedTo("b");
        }

        @Test
        @DisplayName("Click outside elements clears focus")
        void clickOutsideClearsFocus() {
            TestElement a = new TestElement("a").focusable();

            EventRouterTestHarness harness = EventRouterTestHarness.create()
                    .withElement(a, new Rect(0, 0, 10, 1));

            harness.send(MouseEvent.press(MouseButton.LEFT, 20, 20))
                    .assertNoFocus();
        }
    }

    @Nested
    @DisplayName("Focus order")
    class FocusOrder {

        @Test
        @DisplayName("First focusable gets auto-focus when none set")
        void firstFocusableAutoFocused() {
            TestElement a = new TestElement("a").focusable();
            TestElement b = new TestElement("b").focusable();

            EventRouterTestHarness harness = EventRouterTestHarness.create()
                    .withElement(a, new Rect(0, 0, 10, 1))
                    .withElement(b, new Rect(0, 1, 10, 1));

            // First send: auto-focus gives focus to a
            harness.send(KeyEvent.ofKey(KeyCode.CHAR, KeyModifiers.NONE)).assertFocusIs("a");
            // Tab then moves to b
            harness.send(KeyEvent.ofKey(KeyCode.TAB)).assertFocusMovedTo("b");
        }

        @Test
        @DisplayName("withFocus sets initial focus")
        void withFocusSetsInitial() {
            TestElement a = new TestElement("a").focusable();
            TestElement b = new TestElement("b").focusable();

            EventRouterTestHarness harness = EventRouterTestHarness.create()
                    .withElement(a, new Rect(0, 0, 10, 1))
                    .withElement(b, new Rect(0, 1, 10, 1))
                    .withFocus("b");

            harness.send(KeyEvent.ofKey(KeyCode.TAB))
                    .assertFocusMovedTo("a");
        }
    }

    @Nested
    @DisplayName("Event handling")
    class EventHandling {

        @Test
        @DisplayName("Focused element receives key event")
        void focusedElementReceivesKey() {
            TestElement a = new TestElement("a").focusable();

            EventRouterTestHarness harness = EventRouterTestHarness.create()
                    .withElement(a, new Rect(0, 0, 10, 1));

            harness.send(KeyEvent.ofKey(KeyCode.ENTER));
            assertThat(a.keyEvents()).hasSize(1);
        }

        @Test
        @DisplayName("Element that handles key consumes event")
        void handlerConsumesKey() {
            TestElement a = new TestElement("a").focusable().handlesAllKeys();
            TestElement b = new TestElement("b").focusable();

            EventRouterTestHarness harness = EventRouterTestHarness.create()
                    .withElement(a, new Rect(0, 0, 10, 1))
                    .withElement(b, new Rect(0, 1, 10, 1));

            harness.send(KeyEvent.ofChar('x'))
                    .assertHandled();

            assertThat(a.keyEvents()).hasSize(1);
            assertThat(b.keyEvents()).isEmpty();
        }
    }
}

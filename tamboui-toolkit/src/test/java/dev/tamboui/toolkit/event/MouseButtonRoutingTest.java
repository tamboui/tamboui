/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.toolkit.event;

import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.toolkit.app.ToolkitTestRunner;
import dev.tamboui.toolkit.element.Element;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEventKind;

import static dev.tamboui.toolkit.Toolkit.column;
import static dev.tamboui.toolkit.Toolkit.text;
import static org.assertj.core.api.Assertions.assertThat;

class MouseButtonRoutingTest {

    private static Supplier<Element> capturing(MouseButton[] seen) {
        return () -> column(text("click me"))
            .id("root")
            .focusable()
            .onMouseEvent(e -> {
                if (e.kind() == MouseEventKind.PRESS) {
                    seen[0] = e.button();
                    return EventResult.HANDLED;
                }
                return EventResult.UNHANDLED;
            });
    }

    @Test
    @DisplayName("right-button press reaches the element handler (#621)")
    void rightButtonPressIsRouted() throws Exception {
        MouseButton[] seen = { null };
        try (ToolkitTestRunner test = ToolkitTestRunner.runTest(capturing(seen))) {
            test.pilot().mousePress(MouseButton.RIGHT, 2, 0);
            test.pilot().pause();
        }
        assertThat(seen[0]).isEqualTo(MouseButton.RIGHT);
    }

    @Test
    @DisplayName("middle-button press reaches the element handler (#621)")
    void middleButtonPressIsRouted() throws Exception {
        MouseButton[] seen = { null };
        try (ToolkitTestRunner test = ToolkitTestRunner.runTest(capturing(seen))) {
            test.pilot().mousePress(MouseButton.MIDDLE, 2, 0);
            test.pilot().pause();
        }
        assertThat(seen[0]).isEqualTo(MouseButton.MIDDLE);
    }

    @Test
    @DisplayName("left-button press still reaches the element handler")
    void leftButtonPressIsRouted() throws Exception {
        MouseButton[] seen = { null };
        try (ToolkitTestRunner test = ToolkitTestRunner.runTest(capturing(seen))) {
            test.pilot().mousePress(MouseButton.LEFT, 2, 0);
            test.pilot().pause();
        }
        assertThat(seen[0]).isEqualTo(MouseButton.LEFT);
    }
}

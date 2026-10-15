/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import dev.tamboui.buffer.Buffer;
import dev.tamboui.layout.Rect;
import dev.tamboui.terminal.Frame;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import dev.tamboui.tui.event.ResizeEvent;
import dev.tamboui.tui.event.TickEvent;

import static org.assertj.core.api.Assertions.assertThat;

class TuiDemoTest {

    @Test
    @DisplayName("Initial state has default values")
    void initialState() {
        TuiDemo demo = new TuiDemo();

        assertThat(demo.counter()).isEqualTo(0);
        assertThat(demo.selectedPanel()).isEqualTo(0);
        assertThat(demo.mouseX()).isEqualTo(-1);
        assertThat(demo.mouseY()).isEqualTo(-1);
        assertThat(demo.isLeftDown()).isFalse();
        assertThat(demo.isMiddleDown()).isFalse();
        assertThat(demo.isRightDown()).isFalse();
        assertThat(demo.mouseMotionEnabled()).isTrue();
        assertThat(demo.eventLog()).isEmpty();
    }

    @Test
    @DisplayName("Key navigation moves selected panel across panels 0, 1, 2")
    void keyNavigation() {
        TuiDemo demo = new TuiDemo();

        // Left from 0 stays 0
        demo.handleEvent(KeyEvent.ofKey(KeyCode.LEFT), null);
        assertThat(demo.selectedPanel()).isEqualTo(0);

        // Right moves to panel 1
        demo.handleEvent(KeyEvent.ofKey(KeyCode.RIGHT), null);
        assertThat(demo.selectedPanel()).isEqualTo(1);

        // Right moves to panel 2
        demo.handleEvent(KeyEvent.ofKey(KeyCode.RIGHT), null);
        assertThat(demo.selectedPanel()).isEqualTo(2);

        // Right stays at 2
        demo.handleEvent(KeyEvent.ofKey(KeyCode.RIGHT), null);
        assertThat(demo.selectedPanel()).isEqualTo(2);

        // Up/Left moves back to 1 and 0
        demo.handleEvent(KeyEvent.ofKey(KeyCode.UP), null);
        assertThat(demo.selectedPanel()).isEqualTo(1);
        demo.handleEvent(KeyEvent.ofKey(KeyCode.LEFT), null);
        assertThat(demo.selectedPanel()).isEqualTo(0);
    }

    @Test
    @DisplayName("Select keys increment counter")
    void selectIncrementsCounter() {
        TuiDemo demo = new TuiDemo();

        demo.handleEvent(KeyEvent.ofKey(KeyCode.ENTER), null);
        assertThat(demo.counter()).isEqualTo(1);

        demo.handleEvent(KeyEvent.ofChar(' '), null);
        assertThat(demo.counter()).isEqualTo(2);
    }

    @Test
    @DisplayName("Key 'm' toggles motion tracking")
    void toggleMotionKey() {
        TuiDemo demo = new TuiDemo();
        assertThat(demo.mouseMotionEnabled()).isTrue();

        demo.handleEvent(KeyEvent.ofChar('m'), null);
        assertThat(demo.mouseMotionEnabled()).isFalse();

        demo.handleEvent(KeyEvent.ofChar('M'), null);
        assertThat(demo.mouseMotionEnabled()).isTrue();
    }

    @Test
    @DisplayName("Key 'c' clears the event stream")
    void clearEventLogKey() {
        TuiDemo demo = new TuiDemo();

        demo.handleEvent(KeyEvent.ofChar('a'), null);
        demo.handleEvent(KeyEvent.ofChar('b'), null);
        assertThat(demo.eventLog()).hasSize(2);

        demo.handleEvent(KeyEvent.ofChar('c'), null);
        // Event log cleared and system clear message logged
        assertThat(demo.eventLog()).hasSize(1);
        assertThat(demo.eventLog().get(0).badge()).contains("SYSTEM");
    }

    @Test
    @DisplayName("Mouse press, drag, and release update button state and hover ticks")
    void mouseButtonLifecycle() {
        TuiDemo demo = new TuiDemo();

        // Left press
        demo.handleEvent(new MouseEvent(MouseEventKind.PRESS, MouseButton.LEFT, 15, 20, KeyModifiers.NONE), null);
        assertThat(demo.isLeftDown()).isTrue();
        assertThat(demo.mouseX()).isEqualTo(15);
        assertThat(demo.mouseY()).isEqualTo(20);

        // Drag
        demo.handleEvent(new MouseEvent(MouseEventKind.DRAG, MouseButton.LEFT, 16, 20, KeyModifiers.NONE), null);
        assertThat(demo.isLeftDown()).isTrue();
        assertThat(demo.hoverTicks()).isGreaterThan(0);

        // Left release
        demo.handleEvent(new MouseEvent(MouseEventKind.RELEASE, MouseButton.LEFT, 16, 20, KeyModifiers.NONE), null);
        assertThat(demo.isLeftDown()).isFalse();

        // Right press and release
        demo.handleEvent(new MouseEvent(MouseEventKind.PRESS, MouseButton.RIGHT, 10, 10, KeyModifiers.NONE), null);
        assertThat(demo.isRightDown()).isTrue();
        demo.handleEvent(new MouseEvent(MouseEventKind.RELEASE, MouseButton.RIGHT, 10, 10, KeyModifiers.NONE), null);
        assertThat(demo.isRightDown()).isFalse();

        // Middle press and release
        demo.handleEvent(new MouseEvent(MouseEventKind.PRESS, MouseButton.MIDDLE, 10, 10, KeyModifiers.NONE), null);
        assertThat(demo.isMiddleDown()).isTrue();
        demo.handleEvent(new MouseEvent(MouseEventKind.RELEASE, MouseButton.MIDDLE, 10, 10, KeyModifiers.NONE), null);
        assertThat(demo.isMiddleDown()).isFalse();
    }

    @Test
    @DisplayName("Vertical scroll events change counter and trigger decay animation")
    void verticalScrollEvents() {
        TuiDemo demo = new TuiDemo();

        // Scroll up
        demo.handleEvent(new MouseEvent(MouseEventKind.SCROLL_UP, MouseButton.NONE, 5, 5, KeyModifiers.NONE), null);
        assertThat(demo.counter()).isEqualTo(1);
        assertThat(demo.scrollUpTicks()).isGreaterThan(0);

        // Scroll down
        demo.handleEvent(new MouseEvent(MouseEventKind.SCROLL_DOWN, MouseButton.NONE, 5, 5, KeyModifiers.NONE), null);
        assertThat(demo.counter()).isEqualTo(0);
        assertThat(demo.scrollDownTicks()).isGreaterThan(0);

        // Scroll down does not go below 0
        demo.handleEvent(new MouseEvent(MouseEventKind.SCROLL_DOWN, MouseButton.NONE, 5, 5, KeyModifiers.NONE), null);
        assertThat(demo.counter()).isEqualTo(0);
    }

    @Test
    @DisplayName("Horizontal scroll events (#399) trigger decay indicators")
    void horizontalScrollEvents() {
        TuiDemo demo = new TuiDemo();

        // Scroll left
        demo.handleEvent(new MouseEvent(MouseEventKind.SCROLL_LEFT, MouseButton.NONE, 12, 14, KeyModifiers.NONE), null);
        assertThat(demo.scrollLeftTicks()).isGreaterThan(0);
        assertThat(demo.eventLog().get(0).badge()).contains("SCR-LT");

        // Scroll right
        demo.handleEvent(new MouseEvent(MouseEventKind.SCROLL_RIGHT, MouseButton.NONE, 12, 14, KeyModifiers.NONE), null);
        assertThat(demo.scrollRightTicks()).isGreaterThan(0);
        assertThat(demo.eventLog().get(0).badge()).contains("SCR-RT");
    }

    @Test
    @DisplayName("Hover motion (#390) updates pointer and triggers hover ticks")
    void hoverMotionEvents() {
        TuiDemo demo = new TuiDemo();

        demo.handleEvent(new MouseEvent(MouseEventKind.MOVE, MouseButton.NONE, 42, 18, KeyModifiers.NONE), null);
        assertThat(demo.mouseX()).isEqualTo(42);
        assertThat(demo.mouseY()).isEqualTo(18);
        assertThat(demo.hoverTicks()).isGreaterThan(0);
        assertThat(demo.eventLog().get(0).badge()).contains("MOVE");
    }

    @Test
    @DisplayName("Tick event smoothly decrements decay counters")
    void tickDecay() {
        TuiDemo demo = new TuiDemo();

        demo.handleEvent(new MouseEvent(MouseEventKind.SCROLL_UP, MouseButton.NONE, 0, 0, KeyModifiers.NONE), null);
        int initialTicks = demo.scrollUpTicks();
        assertThat(initialTicks).isGreaterThan(0);

        demo.handleEvent(new TickEvent(1), null);
        assertThat(demo.scrollUpTicks()).isEqualTo(initialTicks - 1);
    }

    @Test
    @DisplayName("Resize event updates terminal dimensions in event log")
    void resizeEvent() {
        TuiDemo demo = new TuiDemo();

        demo.handleEvent(new ResizeEvent(120, 45), null);
        assertThat(demo.eventLog().get(0).badge()).contains("RESIZE");
        assertThat(demo.eventLog().get(0).summary()).contains("120x45");
    }

    @Test
    @DisplayName("Render renders without throwing in standard and narrow dimensions")
    void renderHeadless() {
        TuiDemo demo = new TuiDemo();

        // Simulate some events
        demo.handleEvent(new MouseEvent(MouseEventKind.PRESS, MouseButton.LEFT, 10, 5, KeyModifiers.CTRL), null);
        demo.handleEvent(KeyEvent.ofChar('z', KeyModifiers.ALT), null);

        // Standard 100x30
        Buffer bufferStandard = Buffer.empty(new Rect(0, 0, 100, 30));
        Frame frameStandard = Frame.forTesting(bufferStandard);
        demo.render(frameStandard);

        // Wide 140x50
        Buffer bufferWide = Buffer.empty(new Rect(0, 0, 140, 50));
        Frame frameWide = Frame.forTesting(bufferWide);
        demo.render(frameWide);

        // Narrow 60x20
        Buffer bufferNarrow = Buffer.empty(new Rect(0, 0, 60, 20));
        Frame frameNarrow = Frame.forTesting(bufferNarrow);
        demo.render(frameNarrow);

        // Verify that title and borders were rendered into the buffer
        assertThat(bufferStandard.get(0, 0).symbol()).isNotEmpty();
        assertThat(bufferWide.get(0, 0).symbol()).isNotEmpty();
        assertThat(bufferNarrow.get(0, 0).symbol()).isNotEmpty();
    }
}

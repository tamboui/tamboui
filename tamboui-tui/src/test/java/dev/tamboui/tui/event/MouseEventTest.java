/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class MouseEventTest {

    @Test
    @DisplayName("press creates press event")
    void pressCreatesPressEvent() {
        MouseEvent event = MouseEvent.press(MouseButton.LEFT, 10, 20);
        assertThat(event.kind()).isEqualTo(MouseEventKind.PRESS);
        assertThat(event.button()).isEqualTo(MouseButton.LEFT);
        assertThat(event.x()).isEqualTo(10);
        assertThat(event.y()).isEqualTo(20);
        assertThat(event.modifiers()).isEqualTo(KeyModifiers.NONE);
    }

    @Test
    @DisplayName("release creates release event")
    void releaseCreatesReleaseEvent() {
        MouseEvent event = MouseEvent.release(MouseButton.RIGHT, 5, 15);
        assertThat(event.kind()).isEqualTo(MouseEventKind.RELEASE);
        assertThat(event.button()).isEqualTo(MouseButton.RIGHT);
        assertThat(event.x()).isEqualTo(5);
        assertThat(event.y()).isEqualTo(15);
    }

    @Test
    @DisplayName("drag creates drag event")
    void dragCreatesDragEvent() {
        MouseEvent event = MouseEvent.drag(MouseButton.LEFT, 30, 40);
        assertThat(event.kind()).isEqualTo(MouseEventKind.DRAG);
        assertThat(event.button()).isEqualTo(MouseButton.LEFT);
        assertThat(event.x()).isEqualTo(30);
        assertThat(event.y()).isEqualTo(40);
    }

    @Test
    @DisplayName("scrollUp creates scroll up event")
    void scrollUpCreatesScrollUpEvent() {
        MouseEvent event = MouseEvent.scrollUp(7, 8);
        assertThat(event.kind()).isEqualTo(MouseEventKind.SCROLL_UP);
        assertThat(event.button()).isEqualTo(MouseButton.NONE);
        assertThat(event.x()).isEqualTo(7);
        assertThat(event.y()).isEqualTo(8);
    }

    @Test
    @DisplayName("scrollDown creates scroll down event")
    void scrollDownCreatesScrollDownEvent() {
        MouseEvent event = MouseEvent.scrollDown(12, 24);
        assertThat(event.kind()).isEqualTo(MouseEventKind.SCROLL_DOWN);
        assertThat(event.button()).isEqualTo(MouseButton.NONE);
        assertThat(event.x()).isEqualTo(12);
        assertThat(event.y()).isEqualTo(24);
    }

    @Test
    @DisplayName("move creates move event")
    void moveCreatesMoveEvent() {
        MouseEvent event = MouseEvent.move(50, 60);
        assertThat(event.kind()).isEqualTo(MouseEventKind.MOVE);
        assertThat(event.button()).isEqualTo(MouseButton.NONE);
        assertThat(event.x()).isEqualTo(50);
        assertThat(event.y()).isEqualTo(60);
    }

    @Test
    @DisplayName("scrollLeft creates scroll left event")
    void scrollLeftCreatesScrollLeftEvent() {
        MouseEvent event = MouseEvent.scrollLeft(3, 4);
        assertThat(event.kind()).isEqualTo(MouseEventKind.SCROLL_LEFT);
        assertThat(event.button()).isEqualTo(MouseButton.NONE);
        assertThat(event.x()).isEqualTo(3);
        assertThat(event.y()).isEqualTo(4);
    }

    @Test
    @DisplayName("scrollRight creates scroll right event")
    void scrollRightCreatesScrollRightEvent() {
        MouseEvent event = MouseEvent.scrollRight(9, 10);
        assertThat(event.kind()).isEqualTo(MouseEventKind.SCROLL_RIGHT);
        assertThat(event.button()).isEqualTo(MouseButton.NONE);
        assertThat(event.x()).isEqualTo(9);
        assertThat(event.y()).isEqualTo(10);
    }

    @Test
    @DisplayName("isScroll returns true for all scroll directions")
    void isScrollReturnsTrueForAllScrollDirections() {
        assertThat(MouseEvent.scrollUp(0, 0).isScroll()).isTrue();
        assertThat(MouseEvent.scrollDown(0, 0).isScroll()).isTrue();
        assertThat(MouseEvent.scrollLeft(0, 0).isScroll()).isTrue();
        assertThat(MouseEvent.scrollRight(0, 0).isScroll()).isTrue();
    }

    @Test
    @DisplayName("isScroll returns false for non-scroll events")
    void isScrollReturnsFalseForNonScrollEvents() {
        assertThat(MouseEvent.press(MouseButton.LEFT, 0, 0).isScroll()).isFalse();
        assertThat(MouseEvent.release(MouseButton.LEFT, 0, 0).isScroll()).isFalse();
        assertThat(MouseEvent.move(0, 0).isScroll()).isFalse();
        assertThat(MouseEvent.drag(MouseButton.LEFT, 0, 0).isScroll()).isFalse();
    }

    @Test
    @DisplayName("all mouse buttons supported")
    void allButtonsSupported() {
        assertThat(MouseEvent.press(MouseButton.LEFT, 0, 0).button()).isEqualTo(MouseButton.LEFT);
        assertThat(MouseEvent.press(MouseButton.RIGHT, 0, 0).button()).isEqualTo(MouseButton.RIGHT);
        assertThat(MouseEvent.press(MouseButton.MIDDLE, 0, 0).button()).isEqualTo(MouseButton.MIDDLE);
    }
}

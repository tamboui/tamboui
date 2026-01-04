/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

class KeyEventTest {

    @Test
    @DisplayName("ofChar creates character event")
    void ofCharCreatesCharacterEvent() {
        KeyEvent event = KeyEvent.ofChar('a');
        assertThat(event.code()).isEqualTo(KeyCode.CHAR);
        assertThat(event.character()).isEqualTo('a');
        assertThat(event.modifiers()).isEqualTo(KeyModifiers.NONE);
    }

    @Test
    @DisplayName("ofChar with modifiers creates character event with modifiers")
    void ofCharWithModifiers() {
        KeyEvent event = KeyEvent.ofChar('c', KeyModifiers.CTRL);
        assertThat(event.code()).isEqualTo(KeyCode.CHAR);
        assertThat(event.character()).isEqualTo('c');
        assertThat(event.modifiers()).isEqualTo(KeyModifiers.CTRL);
    }

    @Test
    @DisplayName("ofKey creates key event")
    void ofKeyCreatesKeyEvent() {
        KeyEvent event = KeyEvent.ofKey(KeyCode.ENTER);
        assertThat(event.code()).isEqualTo(KeyCode.ENTER);
        assertThat(event.character()).isEqualTo('\0');
        assertThat(event.modifiers()).isEqualTo(KeyModifiers.NONE);
    }

    @Test
    @DisplayName("ofKey with modifiers creates key event with modifiers")
    void ofKeyWithModifiers() {
        KeyEvent event = KeyEvent.ofKey(KeyCode.UP, KeyModifiers.SHIFT);
        assertThat(event.code()).isEqualTo(KeyCode.UP);
        assertThat(event.modifiers()).isEqualTo(KeyModifiers.SHIFT);
    }

    @Test
    @DisplayName("isChar returns true for matching character")
    void isCharMatchesCharacter() {
        KeyEvent event = KeyEvent.ofChar('x');
        assertThat(event.isChar('x')).isTrue();
        assertThat(event.isChar('y')).isFalse();
    }

    @Test
    @DisplayName("isChar returns false for non-CHAR key codes")
    void isCharFalseForNonChar() {
        KeyEvent event = KeyEvent.ofKey(KeyCode.ENTER);
        assertThat(event.isChar('\n')).isFalse();
    }

    @Test
    @DisplayName("isKey returns true for matching key code")
    void isKeyMatchesKeyCode() {
        KeyEvent event = KeyEvent.ofKey(KeyCode.ESCAPE);
        assertThat(event.isKey(KeyCode.ESCAPE)).isTrue();
        assertThat(event.isKey(KeyCode.ENTER)).isFalse();
    }

    @Test
    @DisplayName("hasCtrl returns true when ctrl is pressed")
    void hasCtrlReturnsTrue() {
        KeyEvent event = KeyEvent.ofChar('c', KeyModifiers.CTRL);
        assertThat(event.hasCtrl()).isTrue();
        assertThat(event.hasAlt()).isFalse();
        assertThat(event.hasShift()).isFalse();
    }

    @Test
    @DisplayName("hasAlt returns true when alt is pressed")
    void hasAltReturnsTrue() {
        KeyEvent event = KeyEvent.ofChar('x', KeyModifiers.ALT);
        assertThat(event.hasAlt()).isTrue();
        assertThat(event.hasCtrl()).isFalse();
        assertThat(event.hasShift()).isFalse();
    }

    @Test
    @DisplayName("hasShift returns true when shift is pressed")
    void hasShiftReturnsTrue() {
        KeyEvent event = KeyEvent.ofKey(KeyCode.UP, KeyModifiers.SHIFT);
        assertThat(event.hasShift()).isTrue();
        assertThat(event.hasCtrl()).isFalse();
        assertThat(event.hasAlt()).isFalse();
    }

    @Test
    @DisplayName("isCtrlC returns true for Ctrl+C")
    void isCtrlCReturnsTrue() {
        KeyEvent event = KeyEvent.ofChar('c', KeyModifiers.CTRL);
        assertThat(event.isCtrlC()).isTrue();
    }

    @Test
    @DisplayName("isCtrlC returns false for other keys")
    void isCtrlCReturnsFalse() {
        assertThat(KeyEvent.ofChar('c').isCtrlC()).isFalse();
        assertThat(KeyEvent.ofChar('x', KeyModifiers.CTRL).isCtrlC()).isFalse();
    }

    @Test
    @DisplayName("isCharIgnoreCase matches both cases")
    void isCharIgnoreCaseMatchesBothCases() {
        KeyEvent lower = KeyEvent.ofChar('a');
        KeyEvent upper = KeyEvent.ofChar('A');

        assertThat(lower.isCharIgnoreCase('a')).isTrue();
        assertThat(lower.isCharIgnoreCase('A')).isTrue();
        assertThat(upper.isCharIgnoreCase('a')).isTrue();
        assertThat(upper.isCharIgnoreCase('A')).isTrue();
        assertThat(lower.isCharIgnoreCase('b')).isFalse();
    }

    @Test
    @DisplayName("isCharIgnoreCase returns false for non-CHAR key codes")
    void isCharIgnoreCaseFalseForNonChar() {
        KeyEvent event = KeyEvent.ofKey(KeyCode.ENTER);
        assertThat(event.isCharIgnoreCase('a')).isFalse();
    }
}

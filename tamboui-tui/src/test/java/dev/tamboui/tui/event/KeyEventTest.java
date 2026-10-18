/*
 * Copyright TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("deprecation")
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

    @Test
    @DisplayName("builder creates full-fidelity KeyEvent")
    void builderCreatesFullKeyEvent() {
        KeyEvent event = KeyEvent.builder()
                .code(KeyCode.CHAR)
                .modifiers(KeyModifiers.SUPER)
                .character('a')
                .kind(KeyEventKind.REPEAT)
                .state(new KeyEventState(true, false, true))
                .keyCodePoint('a')
                .shiftedKeyCodePoint('A')
                .baseLayoutKeyCodePoint('a')
                .text("A")
                .build();

        assertThat(event.code()).isEqualTo(KeyCode.CHAR);
        assertThat(event.modifiers().superKey()).isTrue();
        assertThat(event.kind()).isEqualTo(KeyEventKind.REPEAT);
        assertThat(event.isRepeat()).isTrue();
        assertThat(event.isPressOrRepeat()).isTrue();
        assertThat(event.isPress()).isFalse();
        assertThat(event.isRelease()).isFalse();
        assertThat(event.hasSuper()).isTrue();
        assertThat(event.hasHyper()).isFalse();
        assertThat(event.hasMeta()).isFalse();
        assertThat(event.isKeypad()).isTrue();
        assertThat(event.hasCapsLock()).isTrue();
        assertThat(event.hasNumLock()).isFalse();
        assertThat(event.keyCodePoint()).isEqualTo('a');
        assertThat(event.shiftedKeyCodePoint()).hasValue('A');
        assertThat(event.baseLayoutKeyCodePoint()).hasValue('a');
        assertThat(event.text()).isEqualTo("A");
        assertThat(event.string()).isEqualTo("A");
        assertThat(event.isChar('A')).isTrue();
        assertThat(event.isChar('a')).isTrue();
    }

    @Test
    @DisplayName("event kind predicates work as expected")
    void eventKindPredicates() {
        KeyEvent press = KeyEvent.builder().code(KeyCode.CHAR).character('x').kind(KeyEventKind.PRESS).build();
        KeyEvent repeat = KeyEvent.builder().code(KeyCode.CHAR).character('x').kind(KeyEventKind.REPEAT).build();
        KeyEvent release = KeyEvent.builder().code(KeyCode.CHAR).character('x').kind(KeyEventKind.RELEASE).build();

        assertThat(press.isPress()).isTrue();
        assertThat(press.isRepeat()).isFalse();
        assertThat(press.isRelease()).isFalse();
        assertThat(press.isPressOrRepeat()).isTrue();

        assertThat(repeat.isPress()).isFalse();
        assertThat(repeat.isRepeat()).isTrue();
        assertThat(repeat.isRelease()).isFalse();
        assertThat(repeat.isPressOrRepeat()).isTrue();

        assertThat(release.isPress()).isFalse();
        assertThat(release.isRepeat()).isFalse();
        assertThat(release.isRelease()).isTrue();
        assertThat(release.isPressOrRepeat()).isFalse();
    }

    @Test
    @DisplayName("isChar matches shifted key or text")
    void isCharMatchesShiftedOrText() {
        KeyEvent event = KeyEvent.builder()
                .code(KeyCode.CHAR)
                .modifiers(KeyModifiers.SHIFT)
                .keyCodePoint('=')
                .shiftedKeyCodePoint('+')
                .text("+")
                .build();

        assertThat(event.isChar('+')).isTrue();
        assertThat(event.isChar('=')).isTrue();
        assertThat(event.isChar('-')).isFalse();
    }
}

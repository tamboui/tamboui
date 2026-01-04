/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.keymap;

import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.*;

class KeyMapTest {

    @Test
    @DisplayName("Standard keymap matches arrow keys for navigation")
    void standardKeymapMatchesArrowKeys() {
        KeyMap keyMap = KeyMaps.standard();

        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, keyMap);
        KeyEvent down = KeyEvent.ofKey(KeyCode.DOWN, keyMap);
        KeyEvent left = KeyEvent.ofKey(KeyCode.LEFT, keyMap);
        KeyEvent right = KeyEvent.ofKey(KeyCode.RIGHT, keyMap);

        assertThat(up.isUp()).isTrue();
        assertThat(down.isDown()).isTrue();
        assertThat(left.isLeft()).isTrue();
        assertThat(right.isRight()).isTrue();
    }

    @Test
    @DisplayName("Vim keymap matches hjkl for navigation")
    void vimKeymapMatchesHjkl() {
        KeyMap keyMap = KeyMaps.vim();

        KeyEvent h = KeyEvent.ofChar('h', keyMap);
        KeyEvent j = KeyEvent.ofChar('j', keyMap);
        KeyEvent k = KeyEvent.ofChar('k', keyMap);
        KeyEvent l = KeyEvent.ofChar('l', keyMap);

        assertThat(h.isLeft()).isTrue();
        assertThat(j.isDown()).isTrue();
        assertThat(k.isUp()).isTrue();
        assertThat(l.isRight()).isTrue();
    }

    @Test
    @DisplayName("Vim keymap also matches arrow keys")
    void vimKeymapMatchesArrowKeys() {
        KeyMap keyMap = KeyMaps.vim();

        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, keyMap);
        KeyEvent down = KeyEvent.ofKey(KeyCode.DOWN, keyMap);

        assertThat(up.isUp()).isTrue();
        assertThat(down.isDown()).isTrue();
    }

    @Test
    @DisplayName("Standard keymap does not match vim keys for navigation")
    void standardKeymapDoesNotMatchVimKeys() {
        KeyMap keyMap = KeyMaps.standard();

        KeyEvent h = KeyEvent.ofChar('h', keyMap);
        KeyEvent j = KeyEvent.ofChar('j', keyMap);

        assertThat(h.isLeft()).isFalse();
        assertThat(j.isDown()).isFalse();
    }

    @Test
    @DisplayName("Enter key triggers confirm action")
    void enterKeyTriggersConfirm() {
        KeyMap keyMap = KeyMaps.standard();
        KeyEvent enter = KeyEvent.ofKey(KeyCode.ENTER, keyMap);

        assertThat(enter.isConfirm()).isTrue();
    }

    @Test
    @DisplayName("Escape key triggers cancel action")
    void escapeKeyTriggersCancel() {
        KeyMap keyMap = KeyMaps.standard();
        KeyEvent escape = KeyEvent.ofKey(KeyCode.ESCAPE, keyMap);

        assertThat(escape.isCancel()).isTrue();
    }

    @Test
    @DisplayName("Tab key triggers focus next action")
    void tabKeyTriggersFocusNext() {
        KeyMap keyMap = KeyMaps.standard();
        KeyEvent tab = KeyEvent.ofKey(KeyCode.TAB, keyMap);

        assertThat(tab.isFocusNext()).isTrue();
    }

    @Test
    @DisplayName("Shift+Tab triggers focus previous action")
    void shiftTabTriggersFocusPrevious() {
        KeyMap keyMap = KeyMaps.standard();
        KeyEvent shiftTab = KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT, keyMap);

        assertThat(shiftTab.isFocusPrevious()).isTrue();
    }

    @Test
    @DisplayName("q or Ctrl+C triggers quit action")
    void qOrCtrlCTriggersQuit() {
        KeyMap keyMap = KeyMaps.standard();

        KeyEvent q = KeyEvent.ofChar('q', keyMap);
        KeyEvent ctrlC = KeyEvent.ofChar('c', KeyModifiers.CTRL, keyMap);

        assertThat(q.isQuit()).isTrue();
        assertThat(ctrlC.isQuit()).isTrue();
    }

    @Test
    @DisplayName("Page up and page down work")
    void pageUpAndPageDownWork() {
        KeyMap keyMap = KeyMaps.standard();

        KeyEvent pageUp = KeyEvent.ofKey(KeyCode.PAGE_UP, keyMap);
        KeyEvent pageDown = KeyEvent.ofKey(KeyCode.PAGE_DOWN, keyMap);

        assertThat(pageUp.isPageUp()).isTrue();
        assertThat(pageDown.isPageDown()).isTrue();
    }

    @Test
    @DisplayName("Home and End work")
    void homeAndEndWork() {
        KeyMap keyMap = KeyMaps.standard();

        KeyEvent home = KeyEvent.ofKey(KeyCode.HOME, keyMap);
        KeyEvent end = KeyEvent.ofKey(KeyCode.END, keyMap);

        assertThat(home.isHome()).isTrue();
        assertThat(end.isEnd()).isTrue();
    }

    @Test
    @DisplayName("Vim keymap has g and G for home/end")
    void vimKeymapHasGForHomeEnd() {
        KeyMap keyMap = KeyMaps.vim();

        KeyEvent g = KeyEvent.ofChar('g', keyMap);
        KeyEvent G = KeyEvent.ofChar('G', keyMap);

        assertThat(g.isHome()).isTrue();
        assertThat(G.isEnd()).isTrue();
    }

    @Test
    @DisplayName("matches method returns true for matching action")
    void matchesMethodWorks() {
        KeyMap keyMap = KeyMaps.standard();
        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, keyMap);

        assertThat(up.matches(Action.MOVE_UP)).isTrue();
        assertThat(up.matches(Action.MOVE_DOWN)).isFalse();
    }

    @Test
    @DisplayName("action method returns the action for a key event")
    void actionMethodWorks() {
        KeyMap keyMap = KeyMaps.standard();
        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, keyMap);

        assertThat(up.action()).contains(Action.MOVE_UP);
    }

    @Test
    @DisplayName("action method returns empty for unbound key")
    void actionMethodReturnsEmptyForUnboundKey() {
        KeyMap keyMap = KeyMaps.standard();
        KeyEvent x = KeyEvent.ofChar('x', keyMap);

        assertThat(x.action()).isEmpty();
    }

    @Test
    @DisplayName("Custom keymap can be built")
    void customKeymapCanBeBuilt() {
        KeyMap custom = KeyMaps.standard()
            .toBuilder()
            .bind(Action.QUIT, KeyBinding.ch('x'))
            .build();

        KeyEvent x = KeyEvent.ofChar('x', custom);
        assertThat(x.isQuit()).isTrue();
    }

    @Test
    @DisplayName("Emacs keymap has Ctrl+N/P for navigation")
    void emacsKeymapHasCtrlNP() {
        KeyMap keyMap = KeyMaps.emacs();

        KeyEvent ctrlN = KeyEvent.ofChar('n', KeyModifiers.CTRL, keyMap);
        KeyEvent ctrlP = KeyEvent.ofChar('p', KeyModifiers.CTRL, keyMap);

        assertThat(ctrlN.isDown()).isTrue();
        assertThat(ctrlP.isUp()).isTrue();
    }

    @Test
    @DisplayName("Select action with space and enter")
    void selectActionWithSpaceAndEnter() {
        KeyMap keyMap = KeyMaps.standard();

        KeyEvent space = KeyEvent.ofChar(' ', keyMap);
        KeyEvent enter = KeyEvent.ofKey(KeyCode.ENTER, keyMap);

        assertThat(space.isSelect()).isTrue();
        assertThat(enter.isSelect()).isTrue();
    }
}

/*
 * Copyright (c) 2025 TamboUI Contributors
 * SPDX-License-Identifier: MIT
 */
package dev.tamboui.tui.bindings;

import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.KeyEvent;
import dev.tamboui.tui.event.KeyModifiers;
import dev.tamboui.tui.event.MouseButton;
import dev.tamboui.tui.event.MouseEvent;
import dev.tamboui.tui.event.MouseEventKind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class BindingsTest {

    @Test
    @DisplayName("Standard bindings match arrow keys for navigation")
    void standardBindingsMatchArrowKeys() {
        Bindings bindings = BindingSets.standard();

        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, bindings);
        KeyEvent down = KeyEvent.ofKey(KeyCode.DOWN, bindings);
        KeyEvent left = KeyEvent.ofKey(KeyCode.LEFT, bindings);
        KeyEvent right = KeyEvent.ofKey(KeyCode.RIGHT, bindings);

        assertThat(up.isUp()).isTrue();
        assertThat(down.isDown()).isTrue();
        assertThat(left.isLeft()).isTrue();
        assertThat(right.isRight()).isTrue();
    }

    @Test
    @DisplayName("Vim bindings match hjkl for navigation")
    void vimBindingsMatchHjkl() {
        Bindings bindings = BindingSets.vim();

        KeyEvent h = KeyEvent.ofChar('h', bindings);
        KeyEvent j = KeyEvent.ofChar('j', bindings);
        KeyEvent k = KeyEvent.ofChar('k', bindings);
        KeyEvent l = KeyEvent.ofChar('l', bindings);

        assertThat(h.isLeft()).isTrue();
        assertThat(j.isDown()).isTrue();
        assertThat(k.isUp()).isTrue();
        assertThat(l.isRight()).isTrue();
    }

    @Test
    @DisplayName("Vim bindings also match arrow keys")
    void vimBindingsMatchArrowKeys() {
        Bindings bindings = BindingSets.vim();

        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, bindings);
        KeyEvent down = KeyEvent.ofKey(KeyCode.DOWN, bindings);

        assertThat(up.isUp()).isTrue();
        assertThat(down.isDown()).isTrue();
    }

    @Test
    @DisplayName("Standard bindings do not match vim keys for navigation")
    void standardBindingsDoNotMatchVimKeys() {
        Bindings bindings = BindingSets.standard();

        KeyEvent h = KeyEvent.ofChar('h', bindings);
        KeyEvent j = KeyEvent.ofChar('j', bindings);

        assertThat(h.isLeft()).isFalse();
        assertThat(j.isDown()).isFalse();
    }

    @Test
    @DisplayName("Enter key triggers confirm action")
    void enterKeyTriggersConfirm() {
        Bindings bindings = BindingSets.standard();
        KeyEvent enter = KeyEvent.ofKey(KeyCode.ENTER, bindings);

        assertThat(enter.isConfirm()).isTrue();
    }

    @Test
    @DisplayName("Escape key triggers cancel action")
    void escapeKeyTriggersCancel() {
        Bindings bindings = BindingSets.standard();
        KeyEvent escape = KeyEvent.ofKey(KeyCode.ESCAPE, bindings);

        assertThat(escape.isCancel()).isTrue();
    }

    @Test
    @DisplayName("Tab key triggers focus next action")
    void tabKeyTriggersFocusNext() {
        Bindings bindings = BindingSets.standard();
        KeyEvent tab = KeyEvent.ofKey(KeyCode.TAB, bindings);

        assertThat(tab.isFocusNext()).isTrue();
    }

    @Test
    @DisplayName("Shift+Tab triggers focus previous action")
    void shiftTabTriggersFocusPrevious() {
        Bindings bindings = BindingSets.standard();
        KeyEvent shiftTab = KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT, bindings);

        assertThat(shiftTab.isFocusPrevious()).isTrue();
    }

    @Test
    @DisplayName("q or Ctrl+C triggers quit action")
    void qOrCtrlCTriggersQuit() {
        Bindings bindings = BindingSets.standard();

        KeyEvent q = KeyEvent.ofChar('q', bindings);
        KeyEvent ctrlC = KeyEvent.ofChar('c', KeyModifiers.CTRL, bindings);

        assertThat(q.isQuit()).isTrue();
        assertThat(ctrlC.isQuit()).isTrue();
    }

    @Test
    @DisplayName("Page up and page down work")
    void pageUpAndPageDownWork() {
        Bindings bindings = BindingSets.standard();

        KeyEvent pageUp = KeyEvent.ofKey(KeyCode.PAGE_UP, bindings);
        KeyEvent pageDown = KeyEvent.ofKey(KeyCode.PAGE_DOWN, bindings);

        assertThat(pageUp.isPageUp()).isTrue();
        assertThat(pageDown.isPageDown()).isTrue();
    }

    @Test
    @DisplayName("Home and End work")
    void homeAndEndWork() {
        Bindings bindings = BindingSets.standard();

        KeyEvent home = KeyEvent.ofKey(KeyCode.HOME, bindings);
        KeyEvent end = KeyEvent.ofKey(KeyCode.END, bindings);

        assertThat(home.isHome()).isTrue();
        assertThat(end.isEnd()).isTrue();
    }

    @Test
    @DisplayName("Vim bindings have g and G for home/end")
    void vimBindingsHaveGForHomeEnd() {
        Bindings bindings = BindingSets.vim();

        KeyEvent g = KeyEvent.ofChar('g', bindings);
        KeyEvent G = KeyEvent.ofChar('G', bindings);

        assertThat(g.isHome()).isTrue();
        assertThat(G.isEnd()).isTrue();
    }

    @Test
    @DisplayName("matches method returns true for matching action")
    void matchesMethodWorks() {
        Bindings bindings = BindingSets.standard();
        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, bindings);

        assertThat(up.matches(Actions.MOVE_UP)).isTrue();
        assertThat(up.matches(Actions.MOVE_DOWN)).isFalse();
    }

    @Test
    @DisplayName("action method returns the action for a key event")
    void actionMethodWorks() {
        Bindings bindings = BindingSets.standard();
        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, bindings);

        assertThat(up.action()).contains(Actions.MOVE_UP);
    }

    @Test
    @DisplayName("action method returns empty for unbound key")
    void actionMethodReturnsEmptyForUnboundKey() {
        Bindings bindings = BindingSets.standard();
        KeyEvent x = KeyEvent.ofChar('x', bindings);

        assertThat(x.action()).isEmpty();
    }

    @Test
    @DisplayName("Custom bindings can be built")
    void customBindingsCanBeBuilt() {
        Bindings custom = BindingSets.standard()
            .toBuilder()
            .bind(Actions.QUIT, KeyTrigger.ch('x'))
            .build();

        KeyEvent x = KeyEvent.ofChar('x', custom);
        assertThat(x.isQuit()).isTrue();
    }

    @Test
    @DisplayName("Emacs bindings have Ctrl+N/P for navigation")
    void emacsBindingsHaveCtrlNP() {
        Bindings bindings = BindingSets.emacs();

        KeyEvent ctrlN = KeyEvent.ofChar('n', KeyModifiers.CTRL, bindings);
        KeyEvent ctrlP = KeyEvent.ofChar('p', KeyModifiers.CTRL, bindings);

        assertThat(ctrlN.isDown()).isTrue();
        assertThat(ctrlP.isUp()).isTrue();
    }

    @Test
    @DisplayName("Select action with space and enter")
    void selectActionWithSpaceAndEnter() {
        Bindings bindings = BindingSets.standard();

        KeyEvent space = KeyEvent.ofChar(' ', bindings);
        KeyEvent enter = KeyEvent.ofKey(KeyCode.ENTER, bindings);

        assertThat(space.isSelect()).isTrue();
        assertThat(enter.isSelect()).isTrue();
    }

    // ========== Mouse Event Tests ==========

    @Test
    @DisplayName("Left mouse press matches click action")
    void leftMousePressMatchesClickAction() {
        Bindings bindings = BindingSets.standard();
        MouseEvent click = MouseEvent.press(MouseButton.LEFT, 10, 20, bindings);

        assertThat(click.isClick()).isTrue();
        assertThat(click.matches(Actions.CLICK)).isTrue();
    }

    @Test
    @DisplayName("Right mouse press matches right click action")
    void rightMousePressMatchesRightClickAction() {
        Bindings bindings = BindingSets.standard();
        MouseEvent rightClick = MouseEvent.press(MouseButton.RIGHT, 10, 20, bindings);

        assertThat(rightClick.isRightClick()).isTrue();
        assertThat(rightClick.matches(Actions.RIGHT_CLICK)).isTrue();
    }

    @Test
    @DisplayName("Custom bindings can include mouse triggers")
    void customBindingsCanIncludeMouseTriggers() {
        Bindings custom = BindingSets.standard()
            .toBuilder()
            .bind("customAction", MouseTrigger.click())
            .build();

        MouseEvent click = MouseEvent.press(MouseButton.LEFT, 0, 0, custom);
        assertThat(click.matches("customAction")).isTrue();
    }

    @Test
    @DisplayName("Mouse event action method works")
    void mouseEventActionMethodWorks() {
        Bindings bindings = BindingSets.standard();
        MouseEvent click = MouseEvent.press(MouseButton.LEFT, 5, 5, bindings);

        assertThat(click.action()).contains(Actions.CLICK);
    }

    // ========== MouseTrigger Tests ==========

    @Test
    @DisplayName("Ctrl+click matches Ctrl+left press")
    void ctrlClickMatchesCtrlLeftPress() {
        MouseTrigger trigger = MouseTrigger.ctrlClick();
        MouseEvent ctrlClick = new MouseEvent(
            MouseEventKind.PRESS, MouseButton.LEFT, 10, 20, KeyModifiers.CTRL);

        assertThat(trigger.matches(ctrlClick)).isTrue();
    }

    @Test
    @DisplayName("Ctrl+click does not match plain click")
    void ctrlClickDoesNotMatchPlainClick() {
        MouseTrigger trigger = MouseTrigger.ctrlClick();
        MouseEvent plainClick = new MouseEvent(
            MouseEventKind.PRESS, MouseButton.LEFT, 10, 20, KeyModifiers.NONE);

        assertThat(trigger.matches(plainClick)).isFalse();
    }

    @Test
    @DisplayName("Shift+click matches Shift+left press")
    void shiftClickMatchesShiftLeftPress() {
        MouseTrigger trigger = MouseTrigger.shiftClick();
        MouseEvent shiftClick = new MouseEvent(
            MouseEventKind.PRESS, MouseButton.LEFT, 10, 20, KeyModifiers.SHIFT);

        assertThat(trigger.matches(shiftClick)).isTrue();
    }

    @Test
    @DisplayName("Alt+click matches Alt+left press")
    void altClickMatchesAltLeftPress() {
        MouseTrigger trigger = MouseTrigger.altClick();
        MouseEvent altClick = new MouseEvent(
            MouseEventKind.PRESS, MouseButton.LEFT, 10, 20, KeyModifiers.ALT);

        assertThat(trigger.matches(altClick)).isTrue();
    }

    @Test
    @DisplayName("Middle click matches middle button press")
    void middleClickMatchesMiddlePress() {
        MouseTrigger trigger = MouseTrigger.middleClick();
        MouseEvent middleClick = new MouseEvent(
            MouseEventKind.PRESS, MouseButton.MIDDLE, 10, 20, KeyModifiers.NONE);

        assertThat(trigger.matches(middleClick)).isTrue();
    }

    @Test
    @DisplayName("Release trigger matches release events")
    void releaseTriggerMatchesReleaseEvents() {
        MouseTrigger trigger = MouseTrigger.release(MouseButton.LEFT);
        MouseEvent release = new MouseEvent(
            MouseEventKind.RELEASE, MouseButton.LEFT, 10, 20, KeyModifiers.NONE);

        assertThat(trigger.matches(release)).isTrue();
    }

    @Test
    @DisplayName("Release trigger does not match press events")
    void releaseTriggerDoesNotMatchPressEvents() {
        MouseTrigger trigger = MouseTrigger.release(MouseButton.LEFT);
        MouseEvent press = new MouseEvent(
            MouseEventKind.PRESS, MouseButton.LEFT, 10, 20, KeyModifiers.NONE);

        assertThat(trigger.matches(press)).isFalse();
    }

    @Test
    @DisplayName("Scroll up trigger matches scroll up event")
    void scrollUpTriggerMatchesScrollUpEvent() {
        MouseTrigger trigger = MouseTrigger.scrollUp();
        MouseEvent scrollUp = new MouseEvent(
            MouseEventKind.SCROLL_UP, MouseButton.NONE, 10, 20, KeyModifiers.NONE);

        assertThat(trigger.matches(scrollUp)).isTrue();
    }

    @Test
    @DisplayName("Scroll down trigger matches scroll down event")
    void scrollDownTriggerMatchesScrollDownEvent() {
        MouseTrigger trigger = MouseTrigger.scrollDown();
        MouseEvent scrollDown = new MouseEvent(
            MouseEventKind.SCROLL_DOWN, MouseButton.NONE, 10, 20, KeyModifiers.NONE);

        assertThat(trigger.matches(scrollDown)).isTrue();
    }

    @Test
    @DisplayName("Drag trigger matches drag events")
    void dragTriggerMatchesDragEvents() {
        MouseTrigger trigger = MouseTrigger.drag(MouseButton.LEFT);
        MouseEvent drag = new MouseEvent(
            MouseEventKind.DRAG, MouseButton.LEFT, 10, 20, KeyModifiers.NONE);

        assertThat(trigger.matches(drag)).isTrue();
    }

    @Test
    @DisplayName("MouseTrigger.of creates custom trigger")
    void mouseTriggerOfCreatesCustomTrigger() {
        MouseTrigger trigger = MouseTrigger.of(
            MouseEventKind.PRESS, MouseButton.RIGHT, true, true, false);
        MouseEvent event = new MouseEvent(
            MouseEventKind.PRESS, MouseButton.RIGHT, 0, 0,
            KeyModifiers.of(true, true, false));

        assertThat(trigger.matches(event)).isTrue();
    }

    @Test
    @DisplayName("MouseTrigger describe returns readable description")
    void mouseTriggerDescribeReturnsReadableDescription() {
        assertThat(MouseTrigger.click().describe()).isEqualTo("Mouse.LEFT.PRESS");
        assertThat(MouseTrigger.ctrlClick().describe()).isEqualTo("Ctrl+Mouse.LEFT.PRESS");
        assertThat(MouseTrigger.scrollUp().describe()).isEqualTo("Mouse.SCROLL_UP");
    }

    // ========== KeyTrigger Tests ==========

    @Test
    @DisplayName("chIgnoreCase matches both uppercase and lowercase")
    void chIgnoreCaseMatchesBothCases() {
        KeyTrigger trigger = KeyTrigger.chIgnoreCase('k');

        KeyEvent lowercase = KeyEvent.ofChar('k');
        KeyEvent uppercase = KeyEvent.ofChar('K');

        assertThat(trigger.matches(lowercase)).isTrue();
        assertThat(trigger.matches(uppercase)).isTrue();
    }

    @Test
    @DisplayName("ch (case-sensitive) only matches exact case")
    void chCaseSensitiveOnlyMatchesExactCase() {
        KeyTrigger trigger = KeyTrigger.ch('k');

        KeyEvent lowercase = KeyEvent.ofChar('k');
        KeyEvent uppercase = KeyEvent.ofChar('K');

        assertThat(trigger.matches(lowercase)).isTrue();
        assertThat(trigger.matches(uppercase)).isFalse();
    }

    @Test
    @DisplayName("ctrl() creates Ctrl+char trigger")
    void ctrlCreatesCtrlCharTrigger() {
        KeyTrigger trigger = KeyTrigger.ctrl('u');
        KeyEvent ctrlU = KeyEvent.ofChar('u', KeyModifiers.CTRL);

        assertThat(trigger.matches(ctrlU)).isTrue();
    }

    @Test
    @DisplayName("ctrl() does not match without Ctrl modifier")
    void ctrlDoesNotMatchWithoutModifier() {
        KeyTrigger trigger = KeyTrigger.ctrl('u');
        KeyEvent plainU = KeyEvent.ofChar('u');

        assertThat(trigger.matches(plainU)).isFalse();
    }

    @Test
    @DisplayName("alt() creates Alt+char trigger")
    void altCreatesAltCharTrigger() {
        KeyTrigger trigger = KeyTrigger.alt('v');
        KeyEvent altV = KeyEvent.ofChar('v', KeyModifiers.ALT);

        assertThat(trigger.matches(altV)).isTrue();
    }

    @Test
    @DisplayName("key with modifiers matches correctly")
    void keyWithModifiersMatchesCorrectly() {
        KeyTrigger trigger = KeyTrigger.key(KeyCode.TAB, false, false, true);
        KeyEvent shiftTab = KeyEvent.ofKey(KeyCode.TAB, KeyModifiers.SHIFT);

        assertThat(trigger.matches(shiftTab)).isTrue();
    }

    @Test
    @DisplayName("KeyTrigger describe returns readable description")
    void keyTriggerDescribeReturnsReadableDescription() {
        assertThat(KeyTrigger.key(KeyCode.UP).describe()).isEqualTo("Up");
        assertThat(KeyTrigger.ctrl('c').describe()).isEqualTo("Ctrl+c");
        assertThat(KeyTrigger.alt('v').describe()).isEqualTo("Alt+v");
        assertThat(KeyTrigger.ch(' ').describe()).isEqualTo("Space");
        assertThat(KeyTrigger.key(KeyCode.TAB, false, false, true).describe())
            .isEqualTo("Shift+Tab");
    }

    // ========== Bindings Interface Tests ==========

    @Test
    @DisplayName("triggersFor returns all triggers for an action")
    void triggersForReturnsAllTriggersForAction() {
        Bindings bindings = BindingSets.standard();
        List<InputTrigger> triggers = bindings.triggersFor(Actions.QUIT);

        assertThat(triggers).isNotEmpty();
        assertThat(triggers.size()).isGreaterThanOrEqualTo(2); // 'q' and Ctrl+C
    }

    @Test
    @DisplayName("triggersFor returns empty list for unknown action")
    void triggersForReturnsEmptyListForUnknownAction() {
        Bindings bindings = BindingSets.standard();
        List<InputTrigger> triggers = bindings.triggersFor("unknownAction");

        assertThat(triggers).isEmpty();
    }

    @Test
    @DisplayName("describeBindings returns formatted description")
    void describeBindingsReturnsFormattedDescription() {
        Bindings bindings = BindingSets.standard();
        String description = bindings.describeBindings(Actions.MOVE_UP);

        assertThat(description).isNotEmpty();
        assertThat(description).contains("Up");
    }

    // ========== BindingSets Tests ==========

    @Test
    @DisplayName("IntelliJ bindings work")
    void intellijBindingsWork() {
        Bindings bindings = BindingSets.intellij();

        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, bindings);
        KeyEvent down = KeyEvent.ofKey(KeyCode.DOWN, bindings);

        assertThat(up.isUp()).isTrue();
        assertThat(down.isDown()).isTrue();
    }

    @Test
    @DisplayName("VSCode bindings work")
    void vscodeBindingsWork() {
        Bindings bindings = BindingSets.vscode();

        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, bindings);
        KeyEvent down = KeyEvent.ofKey(KeyCode.DOWN, bindings);

        assertThat(up.isUp()).isTrue();
        assertThat(down.isDown()).isTrue();
    }

    @Test
    @DisplayName("defaults() returns standard bindings")
    void defaultsReturnsStandardBindings() {
        Bindings bindings = BindingSets.defaults();

        KeyEvent up = KeyEvent.ofKey(KeyCode.UP, bindings);
        assertThat(up.isUp()).isTrue();
    }

    @Test
    @DisplayName("MouseTrigger does not match KeyEvent")
    void mouseTriggerDoesNotMatchKeyEvent() {
        MouseTrigger trigger = MouseTrigger.click();
        KeyEvent keyEvent = KeyEvent.ofKey(KeyCode.ENTER);

        assertThat(trigger.matches(keyEvent)).isFalse();
    }

    @Test
    @DisplayName("KeyTrigger does not match MouseEvent")
    void keyTriggerDoesNotMatchMouseEvent() {
        KeyTrigger trigger = KeyTrigger.key(KeyCode.ENTER);
        MouseEvent mouseEvent = MouseEvent.press(MouseButton.LEFT, 0, 0);

        assertThat(trigger.matches(mouseEvent)).isFalse();
    }
}
